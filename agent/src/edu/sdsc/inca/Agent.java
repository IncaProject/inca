package edu.sdsc.inca;

import java.io.IOException;
import java.io.File;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;

import edu.sdsc.inca.protocol.*;
import edu.sdsc.inca.repository.Repository;
import edu.sdsc.inca.repository.Repositories;
import edu.sdsc.inca.util.ConfigProperties;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.util.CrypterException;
import edu.sdsc.inca.util.SuiteWrapper;
import edu.sdsc.inca.util.SuiteStagesWrapper;
import edu.sdsc.inca.util.Constants;
import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.agent.*;
import edu.sdsc.inca.agent.util.MyProxy;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.dataModel.catalog.PackageType;
import edu.sdsc.inca.dataModel.util.SeriesConfig;
import org.apache.xmlbeans.XmlException;
import org.apache.log4j.Logger;

/**
 * The agent is the Inca component that provides centralized control
 * of the reporters running on the monitored resources.  A client of the
 * agent (usually thru incat) can
 * <ol>
 * <li>request specific reporters be executed on the monitored resources by
 * submitting suites</li>
 * <li>configure the agent with a list of reporter repositories where it can
 * find reporters requested in the suites</li>
 * <li>configure the agent with information about the resources it is monitoring
 * (e.g., the access method to use to start up a remote reporter manager
 * process)</li>
 * </ol>
 *
 * Note: The Agent is subclass of the Server component whose command threads
 * (which service client requests) do not have access to the server object.
 * Therefore, we have a static Agent object which can be accessed by the
 * command threads to perform actions requested by the clients.
 */
public class Agent extends Server {

  // Constants
  final static public int DEPOT_URI_REFRESH = 10 * Constants.MILLIS_TO_MINUTE;
  final static public int PING_PERIOD = 10 * Constants.MILLIS_TO_MINUTE;
  final public static String REPOSITORY_CACHE = "repository";
  final public static String REPOSITORIES_PATH = "repositories.xml";
  final public static String RESOURCES_PATH = "resources.xml";
  final static public String RMDIR = "rm";
  final static public int START_ATTEMPT_PERIOD = Constants.MILLIS_TO_HOUR;

  public static final String SUITEDIR_PATH = "suites";

  // Standard logger
  private static Logger logger = Logger.getLogger( Agent.class );

  // Static (i.e., global) agent that can be referenced from any thread
  private static Agent globalAgent = new Agent();

  // The agent's communicator to the Inca depot
  //protected DepotClient depotClient = new DepotClient();

  // Handle to the available Inca reporter repositories
  private Repositories repositories = null;

  // The agent's local copy of reporters and packages (only downloads
  // those reporters and dependencies that were requested in a suite).
  private RepositoryCache repositoryCache = null;

  // The current resource configuration on the agent
  private ResourcesWrapper resources = null;

  // Holds handles (i.e., ReporterManagerControllers) to the currently running
  // remote reporter managers
  private ReporterManagerTable rms = new ReporterManagerTable();

  // The suites current configured on the agent
  private SuiteTable suites;

  // Global agent state
  protected boolean ranShutdown = false;

  // Configuration options to specify alternative agent behavior
  protected String upgradeReporterManagers = null; // upgrade the remote RMs
  protected String upgradeTargets = "install"; // makefile targets in build RM
  protected String checkReporterManagers = null; // check the remote RMs

  // Configuration options to run agent -- note need static copies of
  // credentials because the credentials are needed from static functions
  private String adminEmail = null;
  protected Properties depotClientConfig;
  protected String[] depots;
  protected long lastDepotUriFetch = 0;
  private int pingPeriod = Agent.PING_PERIOD;
  private int startAttemptWaitPeriod = Agent.START_ATTEMPT_PERIOD;

  private String myProxyUsername;
  private String myProxyPassword;
  private String myProxyHost;
  private int myProxyPort;

  // Setup the agent configuration options
  private static final String AGENT_OPTS =
    ConfigProperties.mergeValidOptions(
      SERVER_OPTS,
      DepotClient.DEPOT_CLIENT_OPTS +
      "b|buildscript      str path to reporter manager build script\n" +
      "C|check            str check the reporter manager on resources\n" +
      "e|email            str email to send notices of manager restarts\n" +
      "r|rmdist           str path to reporter manager tarball distribution\n" +
      "R|refreshPkgs      int repository check period for package updates\n" +
      "s|stayAlive        int stay alive ping period for the manager\n" +
      "S|startAttempt     int re-start attempt period fpr the manager\n" +
      "u|upgradeResources str upgrade managers in specified resource group\n" +
      "U|upgradeTargets   str makefile targets to execute during upgrade\n",
      true
    );
  static {
    MessageHandlerFactory.registerMessageHandler
      (Protocol.APPROVE_COMMAND, "edu.sdsc.inca.agent.commands.Approve");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.CATALOG_GET_COMMAND,
       "edu.sdsc.inca.agent.commands.RepositoryCommands");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.CONFIG_GET_COMMAND,
       "edu.sdsc.inca.agent.commands.ConfigCommands");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.CONFIG_SET_COMMAND,
       "edu.sdsc.inca.agent.commands.ConfigCommands");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.PROPOSED_GET_COMMAND, "edu.sdsc.inca.agent.commands.Approve");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.PROXY_RENEW_INFO_GET_COMMAND,
       "edu.sdsc.inca.agent.commands.Proxy");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.REGISTER_COMMAND, "edu.sdsc.inca.agent.commands.Register");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.RUN_NOW_COMMAND, "edu.sdsc.inca.agent.commands.ConfigCommands");
  }

  /**
   * Check to see if the reporter manager has been staged to all resources in
   * the specified resource group
   *
   * @param resourcesToCheck  A resource or resource group to check
   *
   * @throws ConfigurationException if missing information about a resource
   * @throws InterruptedException if receives interrupt
   */
  public void checkReporterManagers( String resourcesToCheck )
    throws ConfigurationException, InterruptedException {

    for ( String resource :
          this.resources.getResources( resourcesToCheck, true ) ) {
      logger.info( "Checking reporter manager on resource " + resource );
      String desc = "Reporter manager stage on resource " + resource + ":";
      ReporterManagerStarter rmStarter =
        new ReporterManagerStarter( resource, this );
      for ( int j = 0; j < rmStarter.getEquivalentHosts().length; j++ ) {
        if ( rmStarter.isStaged() ) {
          logger.info( desc + " good" );
          return;
        }
        rmStarter.nextHost();
      }
      logger.error( desc + "' bad" );
    }
  }

  /**
   * Send a newer version of the specified package to all remote reporter
   * managers.
   *
   * @param packageName  The name of the package to update.
   */
  public void distributePackageUpdate( String packageName ) {
    for ( String resource : this.rms.getResourceNames() ) {
      ReporterManagerController managerController=getReporterManager(resource);
      if ( managerController == null ) {
        logger.error
          ( "Unable to retrieve controller for reporter manager " + resource );
        return;
      }
      if ( managerController.hasPackage(packageName, null) ) {
        logger.debug
          ("Adding update for package " + packageName + " to reporter manager "
            + resource );
        managerController.addPackage( packageName );
      }
    }
  }

  /**
   * Send the suites to all reporter managers.
   *
   * @param suites  A HashMap where the resources are the keys and
   * each entry is the SuiteDocument that should be distributed to the resource.
   *
   * @return a map where the resources with proposed changes are the keys and
   * the value is the number of changes.
   */
  public HashMap<String,Integer> distributeSuites
    ( HashMap<String,SuiteWrapper> suites ) {

    HashMap<String,Integer> proposedChangeCount = new HashMap<String,Integer>();
    for( String resource : suites.keySet() ) {
      logger.debug( "Attempting to distribute suite to " + resource );
      ReporterManagerController managerController =
        this.getReporterManager( resource );
      if ( managerController != null ) {
        SuiteDocument s = suites.get(resource).getSuiteDocument();
        logger.debug( "Adding suite "+s.getSuite().getGuid()+" to "+resource );
        try {
          if ( managerController.hasApprovalEmail() ) {
            logger.debug
              ( "Adding proposed suite " + s.getSuite().getGuid() + " to " +
                resource );
            proposedChangeCount.put
              ( resource, managerController.addProposedSuite( s ) );
          } else {
            logger.debug
              ( "Adding suite " + s.getSuite().getGuid() + " to " + resource );
            managerController.addSuite( s );
          }
        } catch (Exception e) {
          logger.debug( "Unable to add suite " + s.getSuite().getGuid()  );
        }
      } else {
        logger.error
          ( "Unable to retrieve controller for reporter manager " + resource );
      }
    }
    return proposedChangeCount;
  }

  /**
   * Return the email address used to send notifications upon reporter manager
   * restart.
   *
   * @return The email address of the administrator or null if no emails should
   * be sent.
   */
  public String getAdminEmail() {
    return adminEmail;
  }

  /**
   * Returns the depot client object to use to communicate to the depot
   * (currently to send suite expanded documents).
   *
   * @return a depot client object.
   */
  protected DepotClient[] getDepotClients() {

    DepotClient[] clients = new DepotClient[depots.length];

    for (int i = 0 ; i < depots.length ; i += 1) {
      clients[i] = new DepotClient();

      depotClientConfig.setProperty("depot", depots[i]);

      try {
        clients[i].setConfiguration(depotClientConfig);
      } catch (ConfigurationException configErr) {
        logger.error("Unable to configure depot clients: " + configErr.getMessage());

        return null;
      }
    }

    return clients;
  }

  /**
   * Get the URIs for the depots.  Fetches a fresh list directly from the
   * first responsive depot if more than 10 minutes have passed since the last
   * fetch.
   *
   * @return  A list of depot URIs in the format:  inca[s]://host:port
   */
  public String[] getDepotUris() {
    long age = Calendar.getInstance().getTimeInMillis() - lastDepotUriFetch;
    String[] depots = this.depots;
    if (  age > DEPOT_URI_REFRESH ) {
      logger.debug( "refreshing depots");
      System.out.println( "refreshing depots" );
      for ( DepotClient client : getDepotClients() ) {
        try {
          client.connect();
          String [] peerUris = client.queryDepotPeers();
          if ( peerUris != null && peerUris.length > 0 ) {
            depots = new String[peerUris.length+1];
            depots[0] = client.getUri();
            System.arraycopy(peerUris, 0, depots, 1, peerUris.length );
          }
          this.lastDepotUriFetch = Calendar.getInstance().getTimeInMillis();
          break;
        } catch ( Exception e ) {
          logger.error( "Unable to fetch depot uris from " + client.getUri(), e );
        } finally {
          if (client.isConnected())
            client.close();
        }
      }
    }
    return depots;
  }

  public static Agent getGlobalAgent() {
    return Agent.globalAgent;
  }

  /**
   * Return the current wait period between pinging of a reporter manager.
   *
   * @return the current ping wait period
   */
  public int getPingPeriod() {
    return this.pingPeriod;
  }

  /**
   * Get the controller for the remote reporter manager.  If the reporter
   * manager does not exist already, create it.
   *
   * @param resource  The name of the resource.
   *
   * @return A reporter manager controller for the remote reporter manager
   * process. If we have just created the reporter manager process, will not
   * wait for reporter manager to register.
   */
  public ReporterManagerController getReporterManager( String resource ) {
    ReporterManagerController managerController;
    if ( this.rms.containsResource(resource)) {
      managerController = this.rms.get( resource );
    } else {
      logger.info("No active reporter manager on resource '" + resource + "'");
      try {
        managerController = new ReporterManagerController( resource, this );
        managerController.getReporterManagerStarter().start();
        rms.put( resource, managerController );
        logger.info( "Reporter manager started on resource '" + resource );
      } catch ( Exception e ) {
        logger.error(
          "Unable to start reporter manager for resource '" + resource + "'", e
        );
        return null;
      }
    }
    return managerController;
  }

  public ReporterManagerTable getReporterManagerTable() {
    return rms;
  }

  /**
   * Returns the list of reporter repositories the agent knows about.
   *
   * @return A list of repository objects.
   */
  public Repositories getRepositories() {
    return this.repositories;
  }

  /**
   * Return the repository cache which provides access to packages cached
   * locally on the Agent as they are requested in suites.
   *
   * @return  The repository cache object.
   */
  public RepositoryCache getRepositoryCache() {
    return this.repositoryCache;
  }

  /**
   * Retrieves the current resource configuration stored on the agent.
   *
   * @return A ResourcesWrapper object which holds the current resource
   * configuration.
   */
  public ResourcesWrapper getResources() {
    return this.resources;
  }

  /**
   * Return the current wait period between start attempts.
   *
   * @return the current start attempt wait period
   */
  public int getStartAttemptWaitPeriod() {
    return this.startAttemptWaitPeriod;
  }

  /**
   * Returns the suites stored at the agent.
   *
   * @return  A table of suites where the keys are the names of the suites
   * and the entries are SuiteDocuments.
   */
  public SuiteTable getSuites() {
    return this.suites;
  }

  /**
   *
   * @return
   */
  public String getMyProxyHost() {
    return this.myProxyHost;
  }

  /**
   *
   * @return
   */
  public int getMyProxyPort() {
    return this.myProxyPort;
  }

  /**
   *
   * @return
   */
  public String getMyProxyPassword() {
    return this.myProxyPassword;
  }

  /**
   *
   * @return
   */
  public String getMyProxyUsername() {
    return this.myProxyUsername;
  }

  /**
   * Given a set of resource names and a count of how many changes are
   * available, email admins of resources that require approval for any changes
   *
   * @param proposedCounts A mapping of resources to number of new proposed
   *                       changes
   */
  public void notifyAdminsofChanges( HashMap<String,Integer> proposedCounts ){
    logger.debug( "Notifying any resource admins of proposed changes" );
    for( String resource : proposedCounts.keySet() ) {
      if ( proposedCounts.get(resource) < 1 ) continue;
      ReporterManagerController rmc = this.getReporterManager( resource );
      if ( rmc.hasApprovalEmail() ) {
        try {
          String emailContent =
            StringMethods.fileContentsFromClasspath("approveEmail.txt");
          HashMap<String,String> macros = new HashMap<String,String>();
          macros.put( "@AGENT@", this.getUri() );
          String adminEmail = this.getAdminEmail();
          if ( adminEmail == null ) {
            adminEmail = System.getProperty("user.name") + "@localhost";
          }
          macros.put( "@INCA_ADMIN@", adminEmail );
          macros.put( "@RESOURCE@", resource );
          macros.put( "@RM_INSTALL_PATH@",
                      rmc.getReporterManagerStarter().getRmRootPath() );
          for ( String macro : macros.keySet() ) {
            emailContent = emailContent.replaceAll( macro, macros.get(macro) );
          }
          String sbj =
            "[inca notification] new changes proposed for Inca resource " +
              resource;
          StringMethods.sendEmail( rmc.getApprovalEmail(), sbj, emailContent );
        } catch ( IOException e ) {
          logger.error( "Problem sending email to admin of " + resource );
        }
      }
    }
  }

  /**
   * Lets the Depot know that this agent is allowed to perform permit and
   * suite actions.
   *
   * @throws ConfigurationException if unable to register permissions
   */
  public void registerDepotPermissions() throws ConfigurationException {

    if( !this.getAuthenticate() ) {
       return;
    }

    boolean success = false;

    for ( DepotClient client : this.getDepotClients() ) {
      try {
        logger.info( "Registering agent permissions with depot " + client.getUri() );
        client.connect();
        String myDn = client.getDn( false );
        if( myDn != null ) {
          client.commandPermit(myDn, Protocol.SUITE_ACTION);
        }
        success = true;
        break;
      } catch (Exception e) {
        logger.error( "Unable to register agent permissions with depot " + client.getUri(), e );
      } finally {
        if (client.isConnected())
          client.close();
      }
    }

    if (!success)
      throw new ConfigurationException( "Unable to register agent permissions with depot; check depot is running" );
  }

  /**
   * Starts the Agent server.  This overrides the Server call in order to
   * do some post configuration.
   */
  public void runServer() throws Exception {
    class RestartSuiteThread extends Thread {
      Agent agent;
      RestartSuiteThread( Agent agent ) {
        this.agent = agent;
      }
      public void run() {
        ranShutdown = false;
        try {
          logger.info( "Waiting for agent to start up" );
          while( ! this.agent.isRunning() ) {
            Thread.sleep(1000);
          }
          logger.info
            ( "Agent is up...sending existing suites to reporter managers" );
        } catch ( InterruptedException e ) {
          logger.info("Received interrupt while waiting for agent to start up");
          logger.info( "Exitting suite start up" );
          return;
        }
        this.agent.getRepositoryCache().start();
        this.agent.getReporterManagerTable().start();
        this.agent.restartManagers();
      }
    }
    new RestartSuiteThread( this ).start();
    super.runServer();
  }

  /**
   * Overrides the server's setConfiguration function to configure the
   * agent specific properties.
   *
   * @param config contains configuration values
   * @throws ConfigurationException on a faulty configuration property value
   */
  public void setConfiguration(Properties config) throws ConfigurationException{
    logger.debug( "Configuring agent" );
    super.setConfiguration( config );

    if((config.getProperty("check")) != null) {
      this.checkReporterManagers = config.getProperty("check");
    }
    if ( config.getProperty("depot") == null ) {
      throw new ConfigurationException( "depot uri not defined" );
    }
    depotClientConfig = new Properties( config );
    this.setDepotUris( config.getProperty("depot").split("[\\s,;]+") );
    setAdminEmail( config.getProperty("email") );
    if((config.getProperty("stayAlive")) != null) {
      this.setPingPeriod(
        Integer.parseInt(config.getProperty("stayAlive")) *
        Constants.MILLIS_TO_SECOND
      );
    }
    if((config.getProperty("startAttempt")) != null) {
      this.setStartAttemptWaitPeriod(
        Integer.parseInt(config.getProperty("startAttempt")) *
        Constants.MILLIS_TO_SECOND
      );
    }

    logger.debug( "temp path is " + this.getTempPath() );
    try {
      setAgentTempPath
        ( this.getTempPath() == null ? "/tmp" : this.getTempPath() );
    } catch(IOException e) {
      throw new ConfigurationException("Unable to configure temp directory", e);
    }
    if((config.getProperty("refreshPkgs")) != null) {
      this.getRepositoryCache().setUpdatePeriod(
        Integer.parseInt(config.getProperty("refreshPkgs")) *
        Constants.MILLIS_TO_SECOND
      );
    }
    if((config.getProperty("upgradeResources")) != null) {
      this.upgradeReporterManagers = config.getProperty("upgradeResources");
      if((config.getProperty("upgradeTargets")) != null) {
        this.upgradeTargets = config.getProperty("upgradeTargets");
      }
    }

    if ((config.getProperty("myproxyHost")) != null) {
      String hostName = config.getProperty("myproxyHost");
      int index = hostName.indexOf(":");

      if (index < 0) {
        myProxyHost = hostName;
        myProxyPort = MyProxy.DEFAULT_PORT;
      } else {
        myProxyHost = hostName.substring(0, index);
        myProxyPort = Integer.parseInt(hostName.substring(index + 1));
      }

      if ((config.getProperty("myproxyUsername")) != null)
        myProxyUsername = config.getProperty("myproxyUsername");
      else
        myProxyUsername = System.getProperty("user.name");

      myProxyPassword = readPassword(config.getProperty("myproxyPassword"));
    }
  }

  /**
   * Set the URIs for the depots.  The first depot in the list will be used
   * unless it is unreachable. In that case, subsequent depots in the list
   * will be tried until one is reachable.
   *
   * @param uris  A list of depot URIs in the format:  inca[s]://host:port
   */
  public void setDepotUris( String[] uris ) {
    this.depots = uris;
  }

  /**
   * Set a new global agent.
   *
   * @param a  The new agent object
   */
  public static void setGlobalAgent( Agent a ) {
    logger.debug( "Setting global agent" );
    Agent.globalAgent = a;
  }

  /**
   * Set the period for how often to ping the reporter manager.
   *
   * @param pingPeriod  The period to wait in between pings to the reporter
   * manager.
   */
  public void setPingPeriod( int pingPeriod ) {
    this.pingPeriod = pingPeriod;
    this.rms.setCheckPeriod( pingPeriod );
  }

  /**
   * Set the repositories the agent should download reporters from upon
   * receiving a suite (if it hasn't downloaded them before).
   *
   * @param repos  A list of repositories.
   *
   * @throws IOException if trouble contacting repositories
   */
  public void setRepositories(Repository[] repos) throws IOException {
    this.repositories.setRepositories( repos );
    this.repositoryCache.setRepositories( this.repositories );
    String batchReporter = ReporterManagerController.BATCH_REPORTER;
    if ( ! repositoryCache.existsLocally(batchReporter, null) &&
         repositories.getRepositoryForPackage(batchReporter, null) != null ) {
      logger.debug( "Adding batch wrapper to repository" );
      repositoryCache.fetchPackage(batchReporter,null);
    }

  }

  /**
   * Set the repository cache which provides access to packages cached
   * locally on the Agent as they are requested in suites.

   * @param repositoryCache  A repository cache object.
   */
  public void setRepositoryCache( RepositoryCache repositoryCache ) {
    this.repositoryCache = repositoryCache;
  }

  /**
   * Sum the numbers in newCounts to counts and update
   *
   * @param counts a hashmap of counts
   * @param newCounts A hashmap of counts
   */
  public static void sumHashes
    ( HashMap<String, Integer> counts, HashMap<String, Integer> newCounts ) {

    for( String key : newCounts.keySet() ) {
      if ( ! counts.containsKey(key) ) {
        counts.put( key, 0 );
      }
      counts.put( key, counts.get(key) + newCounts.get(key) );
    }
  }

  /**
   * Check for updates on all packages in the repository cache including
   * reporters.  A series config is identified by the reporter context and
   * version of the reporter.  So, if a series config in a suite uses the
   * latest version of a package and there is a reporter update, we will need
   * to delete the existing series config (with its current version) and add
   * a new series config (with the updated package version).  This function
   * will iterate thru the agent's existing suites and perform the needed adds
   * and deletes.
   *
   * @return a map where the resources with proposed changes are the keys and
   * the value is the number of changes.
   */
  public synchronized HashMap<String,Integer> updateCachedPackages() {
    HashMap<String,Integer> proposedChangeCount = new HashMap<String,Integer>();
    for( String pkgName : this.repositoryCache.checkForPackageUpdates() ) {
      distributePackageUpdate( pkgName );
      for ( String suiteName : this.getSuites().getNames() ) {
        logger.info
          ( "Checking suite " + suiteName + " for package " + pkgName );
        SuiteStagesWrapper suite = this.getSuites().getSuite( suiteName );
        PackageType pkg = this.getRepositoryCache().getPackage(pkgName, null);
        SuiteWrapper suiteMods = suite.updatePackage
          ( pkgName, pkg.getVersion(), pkg.getUri() );
        if ( suiteMods.getSeriesConfigCount() < 1 ) {
          logger.info( "No changes found" );
          continue;
        }
        try {
          logger.info
            ( "Found " + suiteMods.getSeriesConfigCount() + " changes" );
          SuiteStagesWrapper changes = suite.modify( suiteMods );
          logger.info
            ("Sending package update for suite " + suiteName + " to managers");
          HashMap<String,Integer> thisProposedChangeCount =
            this.distributeSuites( changes.getResourceSuites() );
          sumHashes( proposedChangeCount, thisProposedChangeCount );
        } catch ( Exception e ) {
          logger.error
            ( "Error applying package update for " + pkgName + " to suite " +
            suiteName, e );
        }
      }
    }
    return proposedChangeCount;
  }

  /**
   * Updates the resource configuration on the agent.
   *
   * @param resources  A ResourcesWrapper object which contains the new
   * resource configuration for the agent to utilize.
   *
   * @return a map where the resources with proposed changes are the keys and
   * the value is the number of changes.
   *
   * @throws ConfigurationException if trouble reading info about resources
   * @throws CrypterException if trouble reading resource information
   * @throws IOException if trouble communicating with resources
   * @throws ProtocolException if problem communicating with resources
   * @throws XmlException if trouble updating resources
   */
  public synchronized HashMap<String,Integer> updateResources
    ( ResourcesWrapper resources )

    throws XmlException, ProtocolException, ConfigurationException,
           CrypterException, IOException {

    if ( this.resources == null ) {
      throw new ConfigurationException( "Current resources is null" );
    }
    if ( this.getResources().equals(resources) ) {
      logger.info( "No resource change found" );
      return new HashMap<String,Integer>();
    }
    logger.debug( "Resources has changed" );

    SuiteDocument[] changes =
      this.getSuites().applyResourceChanges( resources );
    this.setResources( resources );

    // update managers
    for ( String resource :this.rms.getResourceNames() ) {
      ReporterManagerController rm = this.rms.get( resource );
      if ( rm != null ) {
        logger.info( "Updating resources for " + resource );
        boolean restartNeeded = rm.updateConfig();
        if (restartNeeded) {
          rm.shutdown();
          rm.restart();
        } else {
          rm.getReporterManagerStarter().refreshHosts();
        }
      }
    }

    // send out affected changes
    HashMap<String,Integer> proposedChangeCount = new HashMap<String,Integer>();
    for ( int i = 0; i < changes.length; i++ ) {
      logger.info( "Queuing resource suites for suite " + i );
      try {
        HashMap<String,SuiteWrapper> resourceChanges =
          SuiteStagesWrapper.getResourceSuites(changes[i]);
        HashMap<String,Integer> thisProposedChangeCount =
          this.distributeSuites( resourceChanges );
        sumHashes( proposedChangeCount, thisProposedChangeCount );
      } catch ( Exception e ) {
        logger.error("Error distributing suites to reporter managers",e);
      }
      logger.info( "Suites queued for suite " + i );
    }
    return proposedChangeCount;
  }

  /**
   * Register the given reporter manager with the agent so that the agent
   * can begin to send it requests.
   *
   * @param resource  The resource the reporter manager is running on.
   * @param reader  the ProtocolReader for reading responses from the remote
   * reporter manager.
   * @param writer  the ProtocolWriter for sending commands to the remote
   * reporter manager.
   * @param dn  the connection DN
   * @param freshInstall indicates whether manager is new and needs to be
   *                     initialized
   *
   * @throws IOException if trouble writing response to reporter manager
   */
  public void registerReporterManager(
    String resource, ProtocolReader reader, ProtocolWriter writer, String dn,
    boolean freshInstall )
  throws IOException {

    // is this a resource we know about?
    if ( ! this.rms.containsResource(resource)) {
      String error = "Unknown resource " + resource + " registering";
      logger.error( error + "...ignoring" );
      writer.write(Statement.getErrorStatement(error));
      return;
    }

    // is this a duplicate resource?
    ReporterManagerController managerController = this.rms.get( resource );
    if ( managerController.isRunning() ) {
      String error = "Reporter manager already running for " + resource;
      logger.error( error );
      writer.write(Statement.getErrorStatement(error));
      return;
    }

    // Otherwise, reply it is OK and register this stream with the reporter
    // manager so the agent can continue to communicate with it.
    Statement response = new Statement(
      Protocol.SUCCESS_COMMAND.toCharArray()
    );
    writer.write(response);

    // name the thread to make it easier to view log messages
    Thread.currentThread().setName( resource );

    // Let the Depot know that this RM is allowed to insert reports
    if ( dn != null ) {
      for ( DepotClient client : getDepotClients() ) {
        try {
          client.connect();
          client.commandRevokeAll( Protocol.INSERT_ACTION + " " + resource );
          client.commandPermit( dn, Protocol.INSERT_ACTION + " " + resource );
          break;
        } catch ( Exception e ) {
          logger.error( "Unable to register Depot permissions for " + resource + " with depot " + client.getUri(), e );
        } finally {
          if (client.isConnected())
            client.close();
        }
      }
    }

    // add permission to approve
    if ( managerController.hasApprovalEmail() ) {
      MessageHandler.grantPermission( dn, Protocol.APPROVE_ACTION + " " + resource );
    }
    managerController.register( reader, writer, freshInstall );

  }

  /**
   * In order to figure out which managers to startup, we iterate thru each
   * suite and see if all resources specified are active.
   */
  public synchronized void restartManagers() {
    Properties activeManagers = new Properties();
    for( String existingSuite : this.suites.getNames() ) {
      try {
        SuiteStagesWrapper suite = suites.getSuite( existingSuite );
        for ( Object resourceObj : suite.getResourceSuites().keySet() ) {
          String resource = (String)resourceObj;
          logger.debug
            ( "Resource " + resource + " found in suite " + existingSuite );
          if ( ! activeManagers.containsKey(resource) ) {
            logger.debug( "Starting up reporter manager on " + resource );
            ReporterManagerController managerController =
              this.getReporterManager( resource );
            if ( managerController == null ) {
              logger.error
                ( "Unable to startup reporter manager for" + resource );
            }
            activeManagers.put( resource, "" );
          }
        }
      } catch ( Exception e ) {
        logger.error
          ( "Unable to restart managers in suite '" + existingSuite + "'", e );
      }
    }
  }

  /**
   * Set the email address of the Inca administrator who should be notified
   * when reporter managers are restarted.
   *
   * @param adminEmail  An email address of the Inca administrator.
   */
  public void setAdminEmail( String adminEmail ) {
    this.adminEmail = adminEmail;
  }

  /**
   * Sets the directory path where the Agent stores state files such as
   * its list of reporter repositories, reporter cache files, resources, and
   * suites.
   *
   * @param path the temporary directory path
   * @throws IOException if the path does not exist and cannot be created
   */
  public void setAgentTempPath( String path ) throws IOException {
    this.tempPath = path;
    this.repositories = new Repositories(
      path + File.separator + REPOSITORIES_PATH
    );

    RepositoryCache cache = new RepositoryCache(
      path + File.separator + REPOSITORY_CACHE , this.repositories
    );
    this.setRepositoryCache( cache );

    String rcPath = path + File.separator + RESOURCES_PATH;
    try {
      ResourcesWrapper resources;
      if ( this.password == null || this.password.equals("") ) {
        resources = new ResourcesWrapper( rcPath );
      } else {
        resources = new ResourcesWrapper( rcPath, this.password );
      }
      this.resources = resources;
    } catch ( Exception e ) {
      String error = "Unable to load resource config file '" + rcPath + "'";
      logger.error( error, e );
      throw new IOException( error + ": " + e );
    }
    if ( this.getResources() == null ) {
      throw new IOException( "resource config is null" );
    }
    this.setSuites( path );
  }

  /**
   * Set the current wait period between start attempts.
   *
   * @param startAttemptWaitPeriod  the new start attempt wait period
   */
  public void setStartAttemptWaitPeriod( int startAttemptWaitPeriod ) {
    this.startAttemptWaitPeriod = startAttemptWaitPeriod;
  }


  /**
   * Create a suite directory for storing the results of suites
   *
   * @param path  A string containing the path to store suites
   */
  public void setSuites( String path ) {
    this.suites = new SuiteTable(
      path + File.separator + SUITEDIR_PATH, this.getResources()
    );
  }

  /**
   * Overrides the server shutdown in order to shutdown the reporter manager's
   * before the general server shutdown.
   *
   * @throws InterruptedException  (see server javadoc)
   */
  public void shutdown() throws InterruptedException {
    if ( ! ranShutdown ) {
      logger.debug( "Shutting down reporter managers" );
      for ( String resource : rms.getResourceNames() ) {
        logger.info( "Shutting down reporter managerController on '" + resource + "'" );
        ReporterManagerController managerController = rms.get( resource );
        if ( managerController.isRunning() ) {
          managerController.shutdown();
        }
      }
      ranShutdown = true;
      logger.debug( "Shutting down agent" );
      if(this.getRepositoryCache() != null) {
        this.getRepositoryCache().interrupt();
        this.getRepositoryCache().join();
      }
      if(this.getReporterManagerTable() != null) {
        this.getReporterManagerTable().interrupt();
        this.getReporterManagerTable().join();
      }

      super.shutdown();
    }
  }

  /**
   * Sets the resource configuration for the agent.
   *
   * @param resources  The resource configuration information.
   *
   * @throws CrypterException if trouble encrypting/decrypting resources
   * @throws IOException if trouble communicating with reporter managers
   * @throws XmlException if trouble reading resources
   */
  public void setResources( ResourcesWrapper resources )
    throws XmlException, CrypterException, IOException {

    this.resources.setResourceConfigDocument(
      resources.getResourceConfigDocument()
    );
    this.resources.save();
  }

  /**
   * Receive a set suite request from client and validate the XML. Then:
   *
   * 1) resolve the reporter names into reporter uris using available repos
   * 2) set the guid of the suite
   * 3) apply the expanded suite to the existing suite and hold onto the applied
   *    changes
   * 4) create resource suites
   * 5) test the resources if no RM running now
   * 6) send the suite expanded to the depot
   * 7) return ok to the client
   * 8) load the reporters into our cache
   * 9) send the suites to the reporter managers
   *
   * @param suite An update to a suite or a new suite.
   *
   * @return a map where the resources with proposed changes are the keys and
   * the value is the number of changes.
   */
  public synchronized HashMap<String,Integer> updateSuite(SuiteWrapper suite) {
    HashMap<String,Integer> proposedChangeCount = new HashMap<String,Integer>();
    SuiteDocument sd = suite.getSuiteDocument();
    String suiteName = sd.getSuite().getName();
    if ( suite.getSeriesConfigCount() < 1 ) {
      logger.info( "No changes received for suite '" + suiteName + "'" );
      return proposedChangeCount;
    }
    logger.info( "Received update for suite '" + suiteName + "'" );

    HashMap<String, SuiteWrapper> suites;

    // 1) load the reporters into our cache
    logger.info( "Loading reporters for suite update '" + suiteName + "'" );
    for ( SeriesConfig config : suite.getSeriesConfigs() ) {
      String name = config.getSeries().getName();
      String version = config.getSeries().getVersion();
      if ( ! this.getRepositoryCache().existsLocally(name,version) ) {
        if ( ! this.getRepositoryCache().fetchPackage(name,version) ) {
          logger.error
            ( "Unable to retrieve package " + name + " version=" + version +
              "and/or its dependencies; rejecting suite update " + suiteName );
          return proposedChangeCount;
        }
      }
    }
    logger.info( "Reporters loaded for suite update '" + suiteName + "'" );

    // 2) resolve the reporter names into reporter uris using available repos
    logger.info( "Resolving reporter names for suite update " + suiteName );
    try {
      this.getRepositoryCache().resolveReporters( suite );
    } catch ( Exception e ) {
      logger.error( e.getMessage() );
      return proposedChangeCount;
    }
    logger.info( "All reporters found for update " + suiteName );

    // 3) set the guid of the suite
    String guid = this.getUri() + "/"  + suiteName;
    sd.getSuite().setGuid( guid );

    // 4) Apply the suiteExpandedChanges to the existing suite
    SuiteStagesWrapper existingSuiteStagesWrapper;
    SuiteStagesWrapper suiteChanges;
    try {
      existingSuiteStagesWrapper =
      this.getSuites().getSuite(suiteName, this.getResources());
      suiteChanges = existingSuiteStagesWrapper.modify( suite );
      if( suiteName.equals( Protocol.IMMEDIATE_SUITE_NAME ) ) {
        // Set action on all series to "delete" and redo modify.  This
        // assures that the stored suite files don't get cluttered by one-off
        // series and prevents us from re-running immediate series on restart.
        for( SeriesConfig immConfig : suite.getSeriesConfigs()) {
          immConfig.setAction( "delete" );
        }
        existingSuiteStagesWrapper.modify( suite );
      }
    } catch ( Exception e ) {
      logger.error( "Unable to apply changes to existing suite", e );
      return proposedChangeCount;
    }

    // 5) create resource suites
    logger.info( "Extracting resource suites for suite '" + suiteName + "'" );
    try {
      suites = suiteChanges.getResourceSuites();
    } catch ( Exception e ) {
      logger.error( "Error in preparing suite for distribution", e );
      return proposedChangeCount;
    }
    logger.info( "Suites extracted for suite '" + suiteName + "'" );

    // 6) queue the suites to the reporter managers
    logger.info("Queuing " + suites.size() + " suites for '" + suiteName + "'");
    try {
      HashMap<String,Integer> thisProposedChangeCount =
        this.distributeSuites( suites );
      sumHashes( proposedChangeCount, thisProposedChangeCount );
    } catch ( Exception e ) {
      logger.error( "Error distributing suites to reporter managers", e );
    }
    logger.info( "Resource suites queued for '" + suiteName + "'" );

    // 7) so far so good. store suite to disk
    logger.info( "Storing suite update for suite '" + suiteName + "'" );
    try {
      this.getSuites().putSuite( existingSuiteStagesWrapper );
    } catch ( Exception e ) {
      logger.error( "Error saving suite " + suiteName + " to disk", e );
      return proposedChangeCount;
    }
    logger.info( "Suite update stored for suite '" + suiteName + "'" );
    logger.info( "Suite update '" + suiteName + "' completed" );
    return proposedChangeCount;
  }

  /**
   * Send the suite changes to the depot, receives a new version number and
   * updates suite with new version.
   *
   * @param expanded   An expanded suite document that should be sent to depot
   *
   * @throws ConfigurationException if trouble reading config info
   * @throws CrypterException if trouble writing new version to suite
   * @throws IOException if trouble communicating with depot
   * @throws ProtocolException if unexpected responses from depot
   */
  public  void updateSuiteOnDepot( SuiteDocument expanded )
    throws ConfigurationException, CrypterException, IOException,
           ProtocolException {

    logger.debug
      ("Sending depot suite update for " + expanded.getSuite().getName() );

    String version = null;

    for ( DepotClient client : getDepotClients() ) {
      try {
        client.connect();
        version = client.updateSuite( expanded.xmlText() );
        break;
      } catch (Exception err) {
        logger.error("Unable to update suite with depot "+client.getUri(), err);
      } finally {
        if (client.isConnected())
          client.close();
      }
    }

    if (version == null)
      throw new IOException("Unable to update suite with depot");

    if ( this.getSuites().hasSuite(expanded.getSuite().getName()) ){
      SuiteStagesWrapper wrapper =
        this.getSuites().getSuite(expanded.getSuite().getName());
      wrapper.getDocument().getSuiteStages().setVersion(new BigInteger(version));
      wrapper.save();
    }
  }

  /**
   * Upgrade the reporter manager distributions on the specified resources
   * read upon configuration.
   *
   * @param resourcesToUpgrade A resource name or group to upgrade
   * @param targetsToExecute The makefile target to execute for the upgrade
   *
   * @throws ConfigurationException if trouble reading resource config info
   */
  public void upgradeReporterManagers( String resourcesToUpgrade,
                                       String targetsToExecute )
    throws ConfigurationException {

    int numSuccessfulUpgrades = 0;
    String[] resourceNames =
      this.resources.getResources(resourcesToUpgrade, true);
    for ( String resource : resourceNames ) {
      logger.info(
        "Upgrading reporter manager on resource '" + resource + "'"
      );
      ReporterManagerStarter rmStarter =
        new ReporterManagerStarter( resource, this );
      int j;
      for ( j = 0; j < rmStarter.getEquivalentHosts().length; j++ ) {
        try {
          rmStarter.findBashLoginShellOption();
          rmStarter.stage( targetsToExecute );
          break;
        } catch ( Exception e ) {
          logger.error(
            "Attempt to upgrade resource " + resource +
            " failed using host " + rmStarter.getCurrentHost(), e
          );
        }
        rmStarter.nextHost();
      }
      if ( j < rmStarter.getEquivalentHosts().length ) {
        logger.info(
          "Upgrade on resource '" + resource + "' completed"
        );
        numSuccessfulUpgrades++;
      } else {
        logger.error(
          "Upgrade on resource '" + resource + "' failed"
        );
      }
    }
    logger.info
      ( "Upgrade complete:  " + numSuccessfulUpgrades + " out of " +
        resourceNames.length + " resources were successfully upgraded" );
  }

  public boolean hasCredentials() {
    return this.authenticate &&
           this.getCertificate() != null &&
           this.getKey() != null &&
           this.getTrustedCertificates() != null;
  }

  public static void main(String[] args) {
    Agent a = new Agent();
    try {
      configComponent
        (a, args, AGENT_OPTS, "inca.agent.", "edu.sdsc.inca.Agent",
         "inca-agent-version");
    } catch(Exception e) {
      logger.fatal("Configuration error: ", e);
      System.err.println("Configuration error: " + e);
      System.exit(1);
    }
    try {
      Agent.setGlobalAgent( a );
      if ( a.upgradeReporterManagers != null ) {
        Agent.getGlobalAgent().readCredentials();
        Agent.getGlobalAgent().upgradeReporterManagers(
          a.upgradeReporterManagers, a.upgradeTargets
        );
      } else if ( a.checkReporterManagers != null ) {
        Agent.getGlobalAgent().readCredentials();
        Agent.getGlobalAgent().checkReporterManagers( a.checkReporterManagers );
      } else {
        a.registerDepotPermissions( );
        a.runServer();
        while ( a.isRunning() ) {
          Thread.sleep( 2000 );
        }
      }
    } catch (Exception e) {
      logger.fatal("Server error: ", e);
      System.err.println("Server error: " + e);
      System.exit(1);
    }
  }

}
