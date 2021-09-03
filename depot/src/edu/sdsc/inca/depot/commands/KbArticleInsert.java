/*
 * KbArticleInsert.java
 */
package edu.sdsc.inca.depot.commands;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.Depot;
import edu.sdsc.inca.dataModel.article.KbArticleDocument;
import edu.sdsc.inca.depot.DelayedWork;
import edu.sdsc.inca.depot.DepotPeerClient;
import edu.sdsc.inca.depot.persistent.KbArticle;
import edu.sdsc.inca.depot.util.HibernateMessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.util.WorkItem;
import edu.sdsc.inca.util.Worker;


/**
 *
 * @author Paul Hoover
 *
 */
public class KbArticleInsert extends HibernateMessageHandler implements DelayedWork {

  /**
   *
   */
  private static class NotifyKbArticleInsert implements WorkItem<Worker> {

    private final Properties m_peerConfig;
    private final String m_xml;


    /**
     *
     * @param config
     * @param xml
     */
    public NotifyKbArticleInsert(Properties config, String xml)
    {
      m_peerConfig = config;
      m_xml = xml;
    }


    /**
     *
     * @param context
     * @throws ConfigurationException
     */
    @Override
    public void doWork(Worker context) throws ConfigurationException
    {
      DepotPeerClient peer = new DepotPeerClient(m_peerConfig);

      try {
        peer.connect();
        peer.sendNotifyKbArticleInsert(m_xml);
      }
      catch (Exception err) {
        m_logger.warn("Unable to send " + Protocol.NOTIFY_KBARTICLE_INSERT_COMMAND + " command to " + peer.getUri() + ": " + err.getMessage());
      }
      finally {
        if (peer.isConnected())
          peer.close();
      }
    }
  }


  private static final Logger m_logger = Logger.getLogger(KbArticleInsert.class);
  private String m_xml;


  /**
   *
   * @param reader
   * @param writer
   * @param dn
   * @throws Exception
   */
  @Override
  public void executeHibernateAction(ProtocolReader reader, ProtocolWriter writer, String dn) throws Exception
  {
    Statement stmt = reader.readStatement();
    KbArticle article = null;

    m_xml = new String(stmt.getData());

    try {
      article = parseArticleXml(m_xml);
    }
    catch(XmlException xmlErr) {
      throw new ProtocolException("Unable to parse article XML: " + xmlErr);
    }

    writer.write(Statement.getOkStatement(""));

    if ((new String(stmt.getCmd())).equals(Protocol.KBARTICLE_INSERT_COMMAND)) {
      for (Properties config : Depot.getRunningDepot().getPeerConfigs())
        Depot.getRunningDepot().addWork(new NotifyKbArticleInsert(config, m_xml));
    }

    if (Depot.getRunningDepot().syncInProgress()) {
      if (Depot.getRunningDepot().addDelayedWork(this))
        return;
    }

    article.save();
  }

  /**
   *
   * @param state
   */
  @Override
  public void loadState(String state)
  {
    m_xml = state;
  }

  /**
   *
   * @return
   */
  @Override
  public String getState()
  {
    return m_xml;
  }

  /**
   *
   * @param context
   */
  @Override
  public void doWork(Worker context)
  {
    try {
      KbArticle article = parseArticleXml(m_xml);

      article.save();
    }
    catch (Exception err) {
      ByteArrayOutputStream logMessage = new ByteArrayOutputStream();

      err.printStackTrace(new PrintStream(logMessage));

      m_logger.error(logMessage.toString());
    }
  }


  /**
   *
   * @param xml
   * @return
   * @throws XmlException
   */
  private KbArticle parseArticleXml(String xml) throws XmlException
  {
    KbArticleDocument doc = KbArticleDocument.Factory.parse(xml, (new XmlOptions()).setLoadStripWhitespace());

    if(!doc.validate())
      throw new XmlException("Invalid article XML '" + xml + "'");

    return (new KbArticle()).fromBean(doc.getKbArticle());
  }
}
