package edu.sdsc.inca.depot.commands;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.Depot;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.depot.DelayedWork;
import edu.sdsc.inca.depot.DepotPeerClient;
import edu.sdsc.inca.depot.persistent.*;
import edu.sdsc.inca.depot.util.HibernateMessageHandler;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.util.WorkItem;
import edu.sdsc.inca.util.Worker;


/**
 * @author cmills
 */
public class SuiteUpdate extends HibernateMessageHandler implements DelayedWork {

  /**
   *
   */
  private static class NotifySuiteUpdate implements WorkItem<Worker> {

    private final Properties peerConfig;
    private final String xml;


    public NotifySuiteUpdate(Properties pc, String x)
    {
      peerConfig = pc;
      xml = x;
    }


    @Override
    public void doWork(Worker context) throws ConfigurationException
    {
      DepotPeerClient peer = new DepotPeerClient(peerConfig);

      try {
        peer.connect();
        peer.sendNotifySuiteUpdate(xml);
      }
      catch (Exception err) {
        logger.warn("Unable to send " + Protocol.NOTIFY_SUITE_UPDATE_COMMAND + " command to " + peer.getUri() + ": " + err.getMessage());
      }
      finally {
        if (peer.isConnected())
          peer.close();
      }
    }
  }


  public static final String ADD = "add";
  public static final String DELETE = "delete";

  private static final Logger logger = Logger.getLogger(SuiteUpdate.class);
  private String xml;


  @Override
  public void executeHibernateAction(
    ProtocolReader reader, ProtocolWriter writer, String dn) throws Exception {
    Statement stmt = reader.readStatement();
    boolean notifyPeers = (new String(stmt.getCmd())).equals(Protocol.SUITE_UPDATE_COMMAND);
    xml = new String(stmt.getData());
    SuiteDocument doc = null;
    Suite suite = null;
    try {
      doc = parseSuiteXml(xml);
      suite = (new Suite()).fromBean(doc.getSuite());
    } catch(XmlException e) {
      throw new ProtocolException("Unable to parse suite XML: " + e);
    }
    if(!MessageHandler.isPermitted(dn, Protocol.SUITE_ACTION)) {
      throw new ProtocolException
        (Protocol.SUITE_ACTION + " not allowed by " + dn);
    }

    int newVersion;

    if (Depot.getRunningDepot().syncInProgress()) {
      newVersion = suite.getVersion() + 1;

      if (!Depot.getRunningDepot().addDelayedWork(this))
        newVersion = updateSuite(suite, doc);
    }
    else
      newVersion = updateSuite(suite, doc);

    writer.write(Statement.getOkStatement(String.valueOf(newVersion)));

    if (notifyPeers) {
      for (Properties config : Depot.getRunningDepot().getPeerConfigs())
        Depot.getRunningDepot().addWork(new NotifySuiteUpdate(config, xml));
    }
  }

  /**
   *
   * @param state
   */
  @Override
  public void loadState(String state)
  {
    xml = state;
  }

  /**
   *
   * @return
   */
  @Override
  public String getState()
  {
    return xml;
  }

  /**
   *
   * @param context
   */
  @Override
  public void doWork(Worker context)
  {
    try {
      SuiteDocument doc = parseSuiteXml(xml);
      Suite suite = (new Suite()).fromBean(doc.getSuite());

      updateSuite(suite, doc);
    }
    catch (Exception err) {
      ByteArrayOutputStream logMessage = new ByteArrayOutputStream();

      err.printStackTrace(new PrintStream(logMessage));

      logger.error(logMessage.toString());
    }
  }


  /**
   *
   * @param xml
   * @return
   * @throws XmlException
   */
  private SuiteDocument parseSuiteXml(String xml) throws XmlException
  {
    SuiteDocument doc = SuiteDocument.Factory.parse(xml, (new XmlOptions()).setLoadStripWhitespace());

    if(!doc.validate())
      throw new XmlException("Invalid suite XML '" + xml + "'");

    return doc;
  }

  /**
   *
   * @param suite
   * @param doc
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  private int updateSuite(Suite suite, SuiteDocument doc) throws IOException, SQLException, PersistenceException
  {
    suite.save();

    Suite dbSuite = suite;
    dbSuite.save();
    dbSuite.incrementVersion();
    dbSuite.setDescription(suite.getDescription());
    // Perform the actions specified in the report series configs.  Note that
    // we have to iterate through the SeriesConfigs in the SuiteDocument,
    // rather than those in dbSuite, to preserve ordering.  The SeriesConfig
    // set in dbConfig is unordered.
    edu.sdsc.inca.dataModel.util.SeriesConfig[] configs =
      doc.getSuite().getSeriesConfigs().getSeriesConfigArray();
    for(int i = 0; i < configs.length; i++) {
      SeriesConfig config = new SeriesConfig().fromBean(configs[i]);
      SeriesConfig dbConfig = null;
      // Check to see if the DB version contains an equivalent series.
      for(Iterator<SeriesConfig> j = dbSuite.getSeriesConfigs().iterator(); j.hasNext(); ) {
        SeriesConfig existingConfig = j.next();
        if(existingConfig.equals(config)) {
          dbConfig = existingConfig;
          break;
        }
      }

      Series dbSeries = config.getSeries();

      dbSeries.save();

      if(dbConfig == null) {
        if(DELETE.equals(config.getAction())) {
          logger.warn("No deletion match found for config " + config);
          // Note: even though we're deactivating the series, we'll add it to
          // the DB.  This might be useful for documentary purposes, and it
          // should be extremely rare in any case.
        }
        config.setSeries(dbSeries);
        dbSuite.getSeriesConfigs().add(config);
        config.save();
        dbConfig = config;
      } else if(dbSuite != suite) {
        // if this is not a new suite, but the DB version contains an
        // equivalent series, adjust the config to conform to the newer
        // version
        if(ADD.equals(config.getAction()) && dbConfig.getDeactivated() == null) {
          logger.warn("Add of already-active config " + config);
        }

        // only update non-unique identifier values on a series
        dbSeries.setNice(config.getSeries().getNice());
        dbSeries.setTargetHostname(config.getSeries().getTargetHostname());
        dbSeries.save();

        dbConfig.setSeries(dbSeries);
        dbConfig.setAcceptedOutput(config.getAcceptedOutput());
        dbConfig.setLimits(config.getLimits());
        dbConfig.setNickname(config.getNickname());
        dbConfig.setSchedule(config.getSchedule());
        dbConfig.setTags(config.getTags());
      }
      if(ADD.equals(config.getAction())) {
        dbConfig.setActivated(Calendar.getInstance().getTime());
        dbConfig.setDeactivated(null);
      } else if(DELETE.equals(config.getAction())) {
        dbConfig.setDeactivated(Calendar.getInstance().getTime());
      }
      dbConfig.save();
    }
    dbSuite.save();

    return dbSuite.getVersion();
  }
}
