/*
 * Depot.java
 */
package edu.sdsc.inca;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import edu.sdsc.inca.util.StringMethods;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import edu.sdsc.inca.depot.DelayedWork;
import edu.sdsc.inca.depot.DelayedWorkQueue;
import edu.sdsc.inca.depot.DepotPeerClient;
import edu.sdsc.inca.depot.PurgeDatabase;
import edu.sdsc.inca.depot.ScheduledPurge;
import edu.sdsc.inca.depot.SyncResponseParser;
import edu.sdsc.inca.depot.UpdateDBSchema;
import edu.sdsc.inca.depot.persistent.DatabaseTools;
import edu.sdsc.inca.depot.util.AmqpNotifier;
import edu.sdsc.inca.depot.util.ReportNotifier;
import edu.sdsc.inca.depot.util.ScriptNotifier;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.MessageHandlerFactory;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.ConfigProperties;
import edu.sdsc.inca.util.WorkItem;
import edu.sdsc.inca.util.Worker;


/**
 * The main depot object.
 * <p/>
 * This object is responsible for retrieving configuration information,
 * starting and cleaning up after the server as well as shutdown.
 * <p/>
 * Configuration is handled by default using inca.properties in the classpath.
 */
public class Depot extends Server {

  static {
    MessageHandlerFactory.registerMessageHandler
      (Protocol.INSERT_COMMAND, "edu.sdsc.inca.depot.commands.Insert");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.NOTIFY_INSERT_COMMAND, "edu.sdsc.inca.depot.commands.Insert");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.QUERY_DB_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.QUERY_GUIDS_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.QUERY_HQL_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.QUERY_INSTANCE_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler
    (Protocol.QUERY_INSTANCE_BY_ID_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.QUERY_LATEST_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.QUERY_PERIOD_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.QUERY_SQL_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.QUERY_STATUS_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.QUERY_USAGE_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.RESEND_COMMAND, "edu.sdsc.inca.depot.commands.Insert");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.SUITE_UPDATE_COMMAND, "edu.sdsc.inca.depot.commands.SuiteUpdate");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.NOTIFY_SUITE_UPDATE_COMMAND, "edu.sdsc.inca.depot.commands.SuiteUpdate");
    MessageHandlerFactory.registerMessageHandler(Protocol.KBARTICLE_INSERT_COMMAND, "edu.sdsc.inca.depot.commands.KbArticleInsert");
    MessageHandlerFactory.registerMessageHandler(Protocol.NOTIFY_KBARTICLE_INSERT_COMMAND, "edu.sdsc.inca.depot.commands.KbArticleInsert");
    MessageHandlerFactory.registerMessageHandler(Protocol.KBARTICLE_DELETE_COMMAND, "edu.sdsc.inca.depot.commands.KbArticleDelete");
    MessageHandlerFactory.registerMessageHandler(Protocol.NOTIFY_KBARTICLE_DELETE_COMMAND, "edu.sdsc.inca.depot.commands.KbArticleDelete");
    MessageHandlerFactory.registerMessageHandler(Protocol.REGISTER_PEER_COMMAND, "edu.sdsc.inca.depot.commands.RegisterPeer");
    MessageHandlerFactory.registerMessageHandler(Protocol.SYNC_COMMAND, "edu.sdsc.inca.depot.commands.SyncResponse");
    MessageHandlerFactory.registerMessageHandler(Protocol.SYNC_DUMP_COMMAND, "edu.sdsc.inca.depot.commands.SyncResponse");
    MessageHandlerFactory.registerMessageHandler(Protocol.QUERY_DEPOT_PEERS_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler(Protocol.QUERY_REPORTERS_COMMAND, "edu.sdsc.inca.depot.commands.Query");
    MessageHandlerFactory.registerMessageHandler(Protocol.QUERY_REPORTERS_DETAIL_COMMAND, "edu.sdsc.inca.depot.commands.Query");
  }


  private enum DepotSyncState {
    NOT_SYNCHRONIZING,
    REQUESTING_SYNC_LOCKED,
    REQUESTING_SYNC_UNLOCKED,
    RESPONDING_TO_SYNC
  }


  // Protected class variables
  protected static final Logger logger = Logger.getLogger(Depot.class);

  // Command-line options
  protected static final String DEPOT_OPTS =
    ConfigProperties.mergeValidOptions(
      SERVER_OPTS,
      "d|dbinit null init depot DB tables\n" +
      "e|expunge str delete old DB instance records\n" +
      "r|remove null remove depot DB tables\n" +
      "R|recover null recover from a crash\n" +
      "S|sync null synchronize DB data with peer depot\n" +
      "M|dump null download DB synchronization data from peer depot\n" +
      "U|update null update database schema\n",
      true
    );

  private static Depot m_runningDepot = null;

  // Instance configuration variables
  private boolean initDb = false;
  private boolean rmDb = false;
  private boolean updateDb = false;
  private boolean requestDump = false;
  private boolean requestSync = false;
  private boolean recover = false;
  private DepotSyncState syncState = DepotSyncState.NOT_SYNCHRONIZING;
  private String[] reportFilters = null;
  private Date purgeCutoff = null;
  private final List<ReportNotifier> reportNotifiers = new ArrayList<ReportNotifier>();
  private final List<Properties> peerConfigs = new ArrayList<Properties>();
  private DelayedWorkQueue delayedWork;
  private Thread purgeThread = null;

  /**
   * Default Constructor.
   */
  public Depot() {
    super();

    MessageHandlerFactory.registerMessageHandler(Protocol.PERMIT_COMMAND, "edu.sdsc.inca.depot.commands.Permit");
    MessageHandlerFactory.registerMessageHandler(Protocol.NOTIFY_PERMIT_COMMAND, "edu.sdsc.inca.depot.commands.Permit");
  }

  /**
   * A convenience function for setting multiple configuration properties at
   * once.  In addition to the keys recognized by the superclass, recognizes:
   * "dbinit", a command to initialize the database; "remove", a command to
   * delete the DB.
   *
   * @param config contains client configuration values
   * @throws ConfigurationException on a faulty configuration property value
   */
  public void setConfiguration(Properties config) throws ConfigurationException
  {
    super.setConfiguration(config);

    try {
      if(config.getProperty("dbinit") != null) {
        this.initDb = true;
      }
      if(config.getProperty("remove") != null) {
        this.rmDb = true;
      }
      if(config.getProperty("sync") != null) {
        this.requestSync = true;
      }
      if(config.getProperty("update") != null) {
        this.updateDb = true;
      }
      if(config.getProperty("dump") != null) {
        this.requestDump = true;
      }
      if(config.getProperty("recover") != null) {
        this.recover = true;
      }

      String prop = config.getProperty("peers");

      if (prop != null) {
        String[] uris = prop.split("[\\s,;]+");

        for (int i = 0 ; i < uris.length ; i += 1)
          addPeerConfig(uris[i]);
      }

      prop = config.getProperty("reportFilter");

      if (prop != null)
        reportFilters = prop.split("[\\s,;]+");

      reportNotifiers.add(new ScriptNotifier());

      prop = config.getProperty("amqp.hosts");

      if (prop != null) {
        String[] hosts = prop.split("[\\s,;]+");
        String exchange = config.getProperty("amqp.exchange");
        String keyBase = config.getProperty("amqp.keyBase");

        String routing_key = "@inca.result@.@inca.nickname@";
        if (config.getProperty("amqp.routing_key") != null ) {
          routing_key = config.getProperty("amqp.routing_key");
        }

        String json_template = "";
        String json_template_name = config.getProperty("amqp.json_template");
        if (json_template_name != null && ! json_template_name.equals("")) {
          json_template = StringMethods.fileContentsFromClasspath(json_template_name);
        }
        String json_report_field = "";
        String json_report_fieldname = config.getProperty("amqp.json_report_fieldname");
        if (json_report_fieldname != null && ! json_report_fieldname.equals("")) {
          json_report_field = json_report_fieldname;
        }
        AmqpNotifier newNotifier = new AmqpNotifier(hosts, exchange, routing_key, json_template, json_report_field);

        prop = config.getProperty("amqp.userCert");

        if (prop != null) {
          String userCert = prop;
          String userKey = config.getProperty("amqp.userKey");
          String passPhrase = config.getProperty("amqp.passPhrase");

          prop = config.getProperty("amqp.hostCerts");

          String[] hostCerts = prop.split("[\\s,;]+");

          newNotifier.setSslContext(userCert, userKey, passPhrase, hostCerts);
        }

        reportNotifiers.add(newNotifier);
      }

      prop = config.getProperty("deleteHour");

      if (prop != null) {
        int hour = Integer.parseInt(prop);

        prop = config.getProperty("deletePeriod");

        int period = Integer.parseInt(prop);

        prop = config.getProperty("deleteOlderThan");

        int cutoff = Integer.parseInt(prop);

        purgeThread = new ScheduledPurge(hour, period, cutoff);
      }

      prop = config.getProperty("expunge");

      if (prop != null) {
        try {
          int numDays = Integer.parseInt(prop);
          Calendar cutoff = Calendar.getInstance();

          cutoff.add(Calendar.DAY_OF_YEAR, -numDays);

          purgeCutoff = cutoff.getTime();
        }
        catch (NumberFormatException formatErr) {
          purgeCutoff = (new SimpleDateFormat("yyyy-MM-dd")).parse(prop);
        }
      }
    }
    catch (Exception err) {
      throw new ConfigurationException(err);
    }
  }

  /**
   * Start the depot from the command line.
   *
   * @param args
   */
  public static void main(String[] args) {
    Depot d = new Depot();

    Depot.setRunningDepot(d);

    try {
      configComponent
        (d, args, DEPOT_OPTS, "inca.depot.", "edu.sdsc.inca.Depot",
         "inca-depot-version");
    } catch(Exception e) {
      logger.fatal("Configuration error: " + e, e);
      System.err.println("Configuration error: " + e);
      System.exit(1);
    }

    // process special commands that are run before/in lieu of the sever
    if (d.requestDump) {
      try {
        d.dumpSyncData();

        return;
      }
      catch (Exception err) {
        logger.fatal("DB synchronization data dump failed: " + err);

        System.exit(1);
      }
    }

    try {
      if(d.rmDb) {
        logger.info("Removing Inca database");
        DatabaseTools.removeDatabase();
        if(!d.initDb) {
          return;
        }
      }
      if(d.initDb) {
        logger.info("Creating Inca database");
        DatabaseTools.initializeDatabase();
        return;
      }
    } catch(Exception e) {
      logger.fatal("Database command failed: " + e);
      System.exit(1);
    }

    if (d.updateDb) {
      try {
        boolean updated = UpdateDBSchema.update();

        logger.info("DB schema " + (updated ? "" : "not ") + "updated");
      }
      catch (SQLException sqlErr) {
        ByteArrayOutputStream logMessage = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(logMessage);

        output.print("DB schema update failed: ");
        sqlErr.printStackTrace(output);

        logger.fatal(logMessage.toString());

        System.exit(1);
      }
    }

    if (d.purgeCutoff != null) {
      try {
        (new PurgeDatabase()).purge(d.purgeCutoff);

        return;
      }
      catch (SQLException sqlErr) {
        sqlErr.printStackTrace();

        System.exit(1);
      }
    }

    try {
      String queueFileName = d.getTempPath() + System.getProperty("file.separator") + "depot.work";

      d.delayedWork = new DelayedWorkQueue(queueFileName);

      File queueFile = new File(queueFileName);

      if (queueFile.exists()) {
        if (!d.recover)
          queueFile.delete();
        else {
          List<DelayedWork> work = d.delayedWork.removeAll();

          for (DelayedWork item : work)
            d.workQueue.addWork(item);

          logger.info("Recovered " + work.size() + " items from the delayed work queue");
        }
      }

      if (d.requestSync)
        d.startSyncRequest();
    }
    catch (Exception err) {
      logger.fatal(err);

      System.exit(1);
    }

    try {
      d.runServer();
      d.registerWithPeers();

      if (d.purgeThread != null)
        d.purgeThread.start();

      if (d.requestingSync()) {
        try {
          d.syncDatabase();
          d.endSync();

          logger.info("DB synchronization succeeded");
        }
        catch (Exception err) {
          ByteArrayOutputStream logMessage = new ByteArrayOutputStream();
          PrintStream output = new PrintStream(logMessage);

          output.print("DB synchronization failed: ");
          err.printStackTrace(output);

          logger.fatal(logMessage.toString());

          d.shutdown();
        }
      }

      while(d.isRunning()) {
        Thread.sleep(1000);
      }
    } catch(Exception e) {
      logger.fatal("Server error: ", e);
      System.err.println("Server error: " + e);
      System.exit(1);
    }
  }

  /**
   *
   * @return
   */
  public String[] getReportFilters()
  {
    return reportFilters;
  }

  /**
   *
   * @return
   */
  public List<ReportNotifier> getReportNotifiers()
  {
    return reportNotifiers;
  }

  /**
   *
   * @return
   */
  public synchronized List<Properties> getPeerConfigs()
  {
    return peerConfigs;
  }

  /**
   *
   * @param uri
   */
  public synchronized void addPeerConfig(String uri)
  {
    String localUri = getLocalUri();

    if (localUri.equalsIgnoreCase(uri)) {
      logger.warn("Refusing to add self to list of peer depots");

      return;
    }

    Properties config = new Properties();
    String value = getCertificatePath();

    if (value != null)
      config.setProperty("cert", value);

    value = getKeyPath();

    if (value != null)
      config.setProperty("key", value);

    value = getPassword();

    if (value != null)
      config.setProperty("password", value);

    value = getTrustedPath();

    if (value != null)
      config.setProperty("trusted", value);

    value = getTempPath();

    if (value != null)
      config.setProperty("var", value);

    config.setProperty("peer", uri);

    peerConfigs.add(config);

    logger.info("Added configuration for peer depot " + uri);
  }

  /**
   *
   * @return
   */
  public String getLocalUri()
  {
    String name;

    try {
      name = InetAddress.getLocalHost().getCanonicalHostName();

      if (name.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$"))
        logger.warn("Unable to get external hostname; fallback to IP");
    }
    catch (Exception err) {
      logger.warn("Unable to get local IP address");

      name = "localhost";
    }

    return (getAuthenticate() ? "incas://" : "inca://") + name + ":" + getPort();
  }

  /**
   *
   * @throws IOException
   */
  public synchronized void startSyncRequest() throws IOException
  {
    logger.debug("Starting synchronization request");

    syncState = DepotSyncState.REQUESTING_SYNC_LOCKED;

    delayedWork.createRequestQueue();
  }

  /**
   * @throws IOException
   *
   */
  public synchronized void startSyncResponse() throws IOException
  {
    logger.debug("Starting synchronization response");

    syncState = DepotSyncState.RESPONDING_TO_SYNC;

    delayedWork.createResponseQueue();
  }

  /**
   *
   * @return
   */
  public synchronized boolean requestingSync()
  {
    return syncState == DepotSyncState.REQUESTING_SYNC_LOCKED || syncState == DepotSyncState.REQUESTING_SYNC_UNLOCKED;
  }

  /**
   *
   * @return
   */
  public synchronized boolean respondingToSync()
  {
    return syncState == DepotSyncState.RESPONDING_TO_SYNC;
  }

  /**
   *
   * @return
   */
  public synchronized boolean syncInProgress()
  {
    return requestingSync() || respondingToSync();
  }

  /**
   *
   * @return
   */
  public synchronized boolean workQueueIsLocked()
  {
    return syncState == DepotSyncState.RESPONDING_TO_SYNC || syncState == DepotSyncState.REQUESTING_SYNC_LOCKED;
  }

  /**
   *
   * @param item
   * @return
   * @throws IOException
   */
  public synchronized boolean addDelayedWork(DelayedWork item) throws Exception
  {
    if (!workQueueIsLocked())
      return false;

    logger.debug("Adding a WorkItem to the delayed work queue");

    delayedWork.add(item);

    return true;
  }

  /**
   *
   * @throws Exception
   */
  public void syncDatabase() throws Exception
  {
    assert requestingSync();

    String fileName = readSyncData(false);

    if (fileName == null)
      throw new Exception("Failed to receive DB data");

    DatabaseTools.removeDatabase();
    DatabaseTools.initializeDatabase();

    parseSyncData(fileName);

    if (!(new File(fileName)).delete())
      logger.warn("Couldn't delete " + fileName);
  }

  /**
   *
   * @throws Exception
   */
  public void dumpSyncData() throws Exception
  {
    String fileName = readSyncData(true);

    if (fileName == null)
      throw new Exception("Failed to receive DB data");

    logger.info("Dumped DB synchronization data to " + fileName);
  }

  /**
   *
   * @throws SAXException
   * @throws IOException
   */
  public synchronized void endSync() throws IOException, SAXException
  {
    if (syncInProgress()) {
      logger.debug("Ending synchronization");

      removeSyncLock();

      syncState = DepotSyncState.NOT_SYNCHRONIZING;
    }
  }

  /**
   *
   * @throws SAXException
   * @throws IOException
   */
  public synchronized void removeSyncLock() throws IOException, SAXException
  {
    if (workQueueIsLocked()) {
      logger.debug("Removing synchronization work queue lock");

      List<DelayedWork> work = delayedWork.removeAll();

      for (DelayedWork item : work)
        workQueue.addWork(item);

      if (syncState == DepotSyncState.REQUESTING_SYNC_LOCKED)
        syncState = DepotSyncState.REQUESTING_SYNC_UNLOCKED;

      logger.debug("Added " + work.size() + " delayed work items to the work queue");
    }
  }

  /**
   *
   * @param item
   */
  public void addWork(WorkItem<Worker> item)
  {
    workQueue.addWork(item);
  }

  /**
   *
   * @throws InterruptedException
   */
  @Override
  public synchronized void shutdown() throws InterruptedException
  {
    if (purgeThread != null)
      purgeThread.interrupt();

    super.shutdown();

    if (purgeThread.isAlive())
      logger.warn("Shutdown for " + purgeThread.getName() + " failed");
  }

  /**
   *
   * @param depot
   */
  public static void setRunningDepot(Depot depot)
  {
    m_runningDepot = depot;
  }

  /**
   *
   * @return
   */
  public static Depot getRunningDepot()
  {
    return m_runningDepot;
  }


  // private methods


  /**
   *
   * @throws ConfigurationException
   */
  private void registerWithPeers() throws ConfigurationException
  {
    String uri = getLocalUri();

    for (Properties config : getPeerConfigs()) {
      DepotPeerClient client = new DepotPeerClient(config);

      try {
        client.connect();

        String data = client.sendRegisterPeer(uri);

        MessageHandler.setPermissionsFromXml(data, client.getDn(false));
        MessageHandler.grantPeerPermission(client.getDn(true));
      }
      catch (Exception err) {
        String errMessage = err.getMessage();
        StringBuilder logMessage = new StringBuilder();

        logMessage.append("Failed to register with peer depot ");
        logMessage.append(client.getUri());

        if (errMessage != null && errMessage.length() > 0) {
          logMessage.append(": ");
          logMessage.append(errMessage);
        }

        logger.warn(logMessage.toString());
      }
      finally {
        if (client.isConnected())
          client.close();
      }
    }
  }

  /**
   *
   * @param dump
   * @return
   * @throws ConfigurationException
   */
  private String readSyncData(boolean dump) throws ConfigurationException
  {
    String fileName = null;

    for (Properties config : getPeerConfigs()) {
      DepotPeerClient client = new DepotPeerClient(config);

      try {
        client.connect();

        fileName = client.requestSync(dump);

        logger.info("Received DB data from peer depot " + client.getUri());

        break;
      }
      catch (Exception err) {
        String errMessage = err.getMessage();
        StringBuilder logMessage = new StringBuilder();

        logMessage.append("Failed to receive DB data from peer depot ");
        logMessage.append(client.getUri());

        if (errMessage != null && errMessage.length() > 0) {
          logMessage.append(": ");
          logMessage.append(errMessage);
        }

        logger.warn(logMessage.toString());
      }
      finally {
        if (client.isConnected())
          client.close();
      }
    }

    return fileName;
  }

  /**
   *
   * @param fileName
   * @throws SQLException
   * @throws IOException
   * @throws SAXException
   */
  private void parseSyncData(String fileName) throws SQLException, IOException, SAXException
  {
    logger.debug("Parsing peer DB data from " + fileName);

    InputStream inStream = new BufferedInputStream(new GZIPInputStream(new FileInputStream(fileName)));

    try {
      SyncResponseParser.parseResponse(this, inStream);
    }
    finally {
      inStream.close();
    }
  }
}
