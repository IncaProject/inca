/*
 * Insert.java
 */
package edu.sdsc.inca.depot.commands;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.apache.xmlbeans.XmlOptions;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.Depot;
import edu.sdsc.inca.dataModel.report.ReportDocument;
import edu.sdsc.inca.depot.DelayedWork;
import edu.sdsc.inca.depot.DepotPeerClient;
import edu.sdsc.inca.depot.persistent.*;
import edu.sdsc.inca.depot.util.ExprComparitor;
import edu.sdsc.inca.depot.util.HibernateMessageHandler;
import edu.sdsc.inca.depot.util.ReportFilter;
import edu.sdsc.inca.depot.util.ReportNotifier;
import edu.sdsc.inca.depot.persistent.Row;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.util.WorkItem;
import edu.sdsc.inca.util.Worker;


/**
 * The task that will take a report and insert it to the database. The
 * REPORT, RESEND, and NOTIFYREPORT commands are supported by this class.
 *
 * @author Jim Hayes
 * @author Paul Hoover
 */
public class Insert extends HibernateMessageHandler implements DelayedWork {

  /**
   *
   */
  private static class NotifyPeer implements WorkItem<Worker> {

    private final Properties peerConfig;
    private final String resourceContextHostname;
    private final String stdErr;
    private final String stdOut;
    private final String usage;


    /**
     *
     * @param pc
     * @param rch
     * @param se
     * @param so
     * @param u
     */
    public NotifyPeer(Properties pc, String rch, String se, String so, String u)
    {
      peerConfig = pc;
      resourceContextHostname = rch;
      stdErr = se;
      stdOut = so;
      usage = u;
    }


    /**
     *
     * @param context
     * @throws ConfigurationException
     */
    @Override
    public void doWork(Worker context) throws ConfigurationException
    {
      DepotPeerClient peer = new DepotPeerClient(peerConfig);

      try {
        peer.connect();
        peer.sendNotifyInsert(resourceContextHostname, stdErr, stdOut, usage);
      }
      catch (Exception err) {
        logger.warn("Unable to send " + Protocol.NOTIFY_INSERT_COMMAND + " command to " + peer.getUri() + ": " + err.getMessage());
      }
      finally {
        if (peer.isConnected())
          peer.close();
      }
    }
  }


  private static final Logger logger = Logger.getLogger(Insert.class);
  private String command;
  private String resource;
  private String context;
  private String hostname;
  private String stdErr;
  private String sysUsage;
  private edu.sdsc.inca.dataModel.util.Report report;
  private Properties timings = new Properties();


  /**
   * Execute the insert task.
   *
   * @param reader
   * @param writer
   * @param dn
   * @throws Exception
   */
  @Override
  public void executeHibernateAction(
    ProtocolReader reader, ProtocolWriter writer, String dn) throws Exception {

    Statement stmt = null;
    long before = System.currentTimeMillis();

    // read everything needed from the socket
    stmt = reader.readStatement();
    command = new String(stmt.getCmd());

    String resourceContextHostname = new String(stmt.getData());
    String[] pieces = resourceContextHostname.split("\n",3); // don't discard empty string

    if(pieces.length == 3) {
      resource = pieces[0];
      context = pieces[1];
      hostname = pieces[2];

      if ( hostname.equals("") ) hostname = null; // no target resource
    }
    else {
      // backwards compatibility with version 2.5 reporters
      pieces = resourceContextHostname.split("\\s+", 2);

      resource = pieces[0];
      context = pieces[1];
      hostname = null;
    }

    String subCommand = reader.peekCommand();
    String origStdErr = null;

    if(subCommand != null && subCommand.equals(Protocol.INSERT_STDERR_COMMAND)) {
      stmt = reader.readStatement();
      origStdErr = new String(stmt.getData());
      stdErr = origStdErr;
      subCommand = reader.peekCommand();
    }

    if(subCommand == null || !subCommand.equals(Protocol.INSERT_STDOUT_COMMAND)) {
      throw new ProtocolException("STDOUT section missing from REPORT message");
    }
    stmt = reader.readStatement();

    String origStdOut = new String(stmt.getData());
    String stdOut = origStdOut;

    subCommand = reader.peekCommand();

    if(subCommand == null || !subCommand.equals(Protocol.INSERT_SYSUSAGE_COMMAND)) {
      throw new ProtocolException
        ("SYSUSAGE section missing from REPORT message");
    }
    stmt = reader.readStatement();

    String origSysUsage = new String(stmt.getData());

    sysUsage = origSysUsage;
    timings.setProperty("Read", (System.currentTimeMillis()-before) + "");
    before = System.currentTimeMillis();

    String[] reportFilterNames = Depot.getRunningDepot().getReportFilters();
    if(reportFilterNames != null) {
      ClassLoader cl = ClassLoader.getSystemClassLoader();
      String origContext = context;
      ReportFilter rf = null;
      for(int i = 0; i < reportFilterNames.length; i++) {
        try {
          rf = (ReportFilter)(cl.loadClass(reportFilterNames[i]).getDeclaredConstructor().newInstance());
        } catch(Exception e) {
          logger.warn
            ("Unable to load ReportFilter '" + reportFilterNames[i] + "' " + e);
          continue;
        }
        rf.setContext(context);
        rf.setResource(resource);
        rf.setStderr(stdErr);
        rf.setStdout(stdOut);
        rf.setSysusage(sysUsage);
        rf.setTargetHostname(hostname);
        context = rf.getContext();
        resource = rf.getResource();
        stdErr = rf.getStderr();
        stdOut = rf.getStdout();
        sysUsage = rf.getSysusage();
        hostname = rf.getTargetHostname();
        if(context==null || resource==null || stdOut==null || sysUsage==null ) {
          logger.warn("Report '" + context + "' from " + resource +
                      " suppressed by filter " + reportFilterNames[i]);
          writer.write(Statement.getOkStatement(origContext));
          return;
        }
      }
    }
    timings.setProperty("Filter", (System.currentTimeMillis()-before) + "");
    before = System.currentTimeMillis();

    try {
      report = parseReportXml(stdOut);
    } catch(XmlException e) {
      throw new ProtocolException("Unable to parse report XML: " + e);
    }
    timings.setProperty("Parse", (System.currentTimeMillis()-before) + "");

    if(!MessageHandler.isPermitted(dn, Protocol.INSERT_ACTION+" "+resource)) {
      throw new ProtocolException
        (Protocol.INSERT_ACTION +  " " + resource + " not allowed by " + dn);
    }

    // Let the client know everything went well.
    writer.write(Statement.getOkStatement(context));

    boolean notify = command.equals(Protocol.INSERT_COMMAND) || command.equals(Protocol.RESEND_COMMAND);
    Depot depot = Depot.getRunningDepot();

    if (notify) {
      for (Properties config : depot.getPeerConfigs())
        depot.addWork(new NotifyPeer(config, resourceContextHostname, origStdErr, origStdOut, origSysUsage));
    }

    if (depot.syncInProgress()) {
      if (depot.addDelayedWork(this))
        return;
    }

    insertReport(notify);
  }

  /**
   *
   * @param state
   * @throws Exception
   */
  @Override
  public void loadState(String state) throws Exception
  {
    try {
      XPath xpath = XPathFactory.newInstance().newXPath();
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document stateDoc = docBuilder.parse(new InputSource(new StringReader(state)));

      command = xpath.evaluate("/insertState/command", stateDoc);
      resource = xpath.evaluate("/insertState/resource", stateDoc);
      context = xpath.evaluate("/insertState/context", stateDoc);
      hostname = xpath.evaluate("/insertState/hostname", stateDoc);
      stdErr = xpath.evaluate("/insertState/stdErr", stateDoc);
      sysUsage = xpath.evaluate("/insertState/sysUsage", stateDoc);

      String text = xpath.evaluate("/insertState/report", stateDoc);

      report = parseReportXml(text);

      text = xpath.evaluate("/insertState/timings", stateDoc);

      timings.load(new StringReader(text));
    }
    catch (Exception err) {
      ByteArrayOutputStream logMessage = new ByteArrayOutputStream();
      PrintStream stream = new PrintStream(logMessage);

      err.printStackTrace(stream);
      stream.println("Thrown while parsing");
      stream.println(state);

      logger.error(logMessage.toString());

      throw err;
    }
  }

  /**
   *
   * @return
   * @throws IOException
   */
  @Override
  public String getState() throws IOException
  {
    ByteArrayOutputStream timingBytes = new ByteArrayOutputStream();

    timings.store(timingBytes, null);

    ReportDocument reportDoc = ReportDocument.Factory.newInstance();

    reportDoc.setReport(report);

    StringBuilder builder = new StringBuilder();

    builder.append("<insertState><command>");
    builder.append(command);
    builder.append("</command><resource>");
    builder.append(resource);
    builder.append("</resource><context>");
    builder.append(createCdataValue(context));
    builder.append("</context><hostname>");
    builder.append(hostname);
    builder.append("</hostname><stdErr>");
    builder.append(createCdataValue(stdErr));
    builder.append("</stdErr><sysUsage>");
    builder.append(sysUsage);
    builder.append("</sysUsage><report>");
    builder.append(createCdataValue(reportDoc.xmlText()));
    builder.append("</report><timings>");
    builder.append(timingBytes.toString());
    builder.append("</timings></insertState>");

    return builder.toString();
  }

  /**
   *
   * @param context
   */
  @Override
  public void doWork(Worker context)
  {
    try {
      boolean notify = command.equals(Protocol.INSERT_COMMAND) || command.equals(Protocol.RESEND_COMMAND);

      insertReport(notify);
    }
    catch (Exception err) {
      ByteArrayOutputStream logMessage = new ByteArrayOutputStream();

      err.printStackTrace(new PrintStream(logMessage));

      logger.error(logMessage.toString());
    }
  }


  // private methods


  /**
   *
   * @param xml
   * @return
   * @throws XmlException
   */
  private edu.sdsc.inca.dataModel.util.Report parseReportXml(String xml) throws XmlException
  {
    ReportDocument doc = ReportDocument.Factory.parse(xml, (new XmlOptions()).setLoadStripWhitespace());

    if (!doc.validate())
      throw new XmlException("Invalid report XML '" + xml + "'");

    return doc.getReport();
  }

  /**
   *
   * @param value
   * @return
   */
  private String createCdataValue(String value)
  {
    StringBuilder result = new StringBuilder();

    result.append("<![CDATA[");

    if (value != null)
      result.append(value.replaceAll("]]>", "]]]]><![CDATA[>"));

    result.append("]]>");

    return result.toString();
  }

  /**
   *
   * @param notify
   * @throws Exception
   */
  private void insertReport(boolean notify) throws Exception
  {
    // Construct the four database entities contained in the text of the
    // report--a Series, a Report, an InstanceInfo, and a RunInfo.
    Series s = new Series().fromBean(report);
    s.setResource(resource);
    s.setContext(context);
    s.setTargetHostname(hostname);
    Report r = new Report().fromBean(report);
    r.setStderr(stdErr);

    RunInfo ri = new RunInfo().fromBean(report);
    Report dbReport = null;
    RunInfo dbRunInfo = null;
    Series dbSeries = null;
    long before = System.currentTimeMillis();

    // Retrieve them from (existing) or store them in (new) the DB.
    if((dbSeries = Series.find(s)) == null) {
      logger.warn
        ("Report of " + s.getReporter() + " unattached to any DB series");
      logger.debug("Auto-add new series to DB");
      s.save();
      dbSeries = s;
    }
    ri.save();
    dbRunInfo = ri;
    r.setSeries(dbSeries);  // Report.load depends on these being set
    r.setRunInfo(dbRunInfo);
    if((dbReport = Report.find(r)) == null) {
      logger.debug("Auto-add new report to DB");
      r.save();
      dbReport = r;
    }
    dbReport.setSeries(dbSeries);
    dbSeries.save();
    dbReport.save();
    logger.debug("Add new instance to DB");
    InstanceInfo ii = new InstanceInfo(dbSeries, report.getGmt().getTime(), sysUsage);
    if(report.getLog() != null)
      ii.setLog(report.getLog().xmlText());
    ii.setReportId(dbReport.getId());
    ii.save();
    timings.setProperty("DB Records", (System.currentTimeMillis()-before) + "");
    before = System.currentTimeMillis();

    // Update all related SeriesConfigs
    if(dbSeries.getSeriesConfigs().size() < 1) {
      logger.warn
        ("Series of " + s.getReporter() + " unattached to any DB config");
    }

    if (notify) {
      Depot depot = Depot.getRunningDepot();

      for (ReportNotifier notifier : depot.getReportNotifiers())
        notifier.notify(command, report, dbSeries, ii);
    }

    int i = 0;

    for (SeriesConfig dbSc : dbSeries.getSeriesConfigs()) {
      i++;

      // If multiple configs feed into the same series, we could receive
      // reports attached to deactivated series.  Do no processing for these.
      if(dbSc.getDeactivated() != null) {
        continue;
      }

      ii.getSeriesConfigs().add(dbSc);
      ii.save();

      boolean isLatest =
        command.equals(Protocol.INSERT_COMMAND) ||
        dbSc.getLatestInstanceId() < 0 ||
        (new InstanceInfo(dbSeries, dbSc.getLatestInstanceId())).getCollected().
          before(ii.getCollected());
      if(isLatest) {
        // Update the SeriesConfig's latest instance field
        dbSc.setLatestInstanceId(ii.getId());
        try {
          dbSc.save();
        } catch(PersistenceException e) {
          logger.error("Error storing report series config:" + e);
        }
      }
      timings.setProperty("SC update "+i, (System.currentTimeMillis()-before) + "");
      before = System.currentTimeMillis();

      // Generate and save any comparison the SeriesConfig specifies
      AcceptedOutput ao = dbSc.getAcceptedOutput();
      if(ao == null) {
        continue;
      }
      String comparitor = ao.getComparitor();
      if(comparitor == null || comparitor.equals("") ||
         comparitor.equals(Row.DB_EMPTY_STRING)) {
        continue;
      }

      String result = new ExprComparitor().compare(ao, dbReport);
      ComparisonResult dbCr = null;
      try {
        dbCr = new ComparisonResult(result, dbReport.getId(), dbSc.getId());
        dbCr.save();
      } catch(Exception e) {
        logger.error("Error storing comparison result:" + e);
      }

      // Update the id in the SeriesConfig if the comparison result changed
      Long priorComparisonId = dbSc.getLatestComparisonId();
      if(!isLatest ||
         dbCr.getId() == priorComparisonId.longValue()) {
        continue;
      }
      try {
        dbSc.setLatestComparisonId(dbCr.getId());
        dbSc.save();
      } catch(Exception e) {
        logger.error("Error updating series config:" + e);
      }
      timings.setProperty("SC compare "+i, (System.currentTimeMillis()-before) + "");
    }

    StringBuffer timeString = new StringBuffer();
    for(Enumeration<?> e = timings.propertyNames(); e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      timeString.append(" ").append(key).append(" ")
                .append(timings.getProperty(key));
    }
    logger.debug("Insert times:" + timeString);
  }
}
