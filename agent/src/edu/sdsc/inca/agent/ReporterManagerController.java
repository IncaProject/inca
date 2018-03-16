package edu.sdsc.inca.agent;


import java.util.Vector;
import java.util.Calendar;
import java.util.HashMap;
import java.io.*;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.Agent;
import edu.sdsc.inca.util.WorkQueue;
import edu.sdsc.inca.util.Constants;
import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.util.CrypterException;
import edu.sdsc.inca.util.SuiteWrapper;
import edu.sdsc.inca.util.SuiteStagesWrapper;
import edu.sdsc.inca.dataModel.util.Series;
import edu.sdsc.inca.dataModel.util.SeriesConfig;
import edu.sdsc.inca.dataModel.catalog.PackageType;
import edu.sdsc.inca.dataModel.catalog.CatalogDocument;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.dataModel.suite.Suite;
import edu.sdsc.inca.dataModel.inca.IncaDocument;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.ManagerClient;
import edu.sdsc.inca.util.WorkItem;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlException;


/**
 * Manages a reporter manager instance on a remote machine. It maintains
 * a persistent connection to the manager and provides the
 * ability to send packages and suites to the remote reporter manager.  It
 * also has the ability to detect a fault of the reporter manager by regularly
 * pinging it and and restarting it if necessary.
 *
 * @author Shava Smallen
 */
public class ReporterManagerController  {

  // Constants
  final static public String APPROVED_DIR = "approved";
  final static public String BATCH_REPORTER = "cluster.batch.wrapper";
  final static public String COMMITTED_FILE = "committed.xml";
  final static public String DEPOT_DIR = "depot";
  final static public int ISREADY_PERIOD = 5 * Constants.MILLIS_TO_SECOND;
  final static public String PROPOSED_DIR = "proposed";
  final static private String PING_DATA = "manager";
  final static public int SUITE_CHECK_PERIOD = 5 * Constants.MILLIS_TO_SECOND;
  final static public String SUITE_DIR = "suites";

  private static Logger logger = Logger.getLogger(ReporterManagerController.class);

  // Member variables
  protected Agent                agent = null;
  private String                 approvalEmail = null;
  private File                   approvedSuites = null;
  private CatalogDocument        catalog = null;
  private File                   catalogFile = null;
  private File                   committedFile = null;
  private File                   depotSuites = null;
  private boolean                isRunning = false;
  private long                   lastDepotSendAttempt = 0;
  private boolean                needToShutdown = false;
  private ManagerClient          managerClient = new ManagerClient();
  private File                   proposedSuites = null;
  private ReporterManagerProxy   proxy = null;
  private String                 resource = null;
  private ReporterManagerStarter rmStarter = null;  
  private boolean                shutdownDetected = false;
  private WorkQueue<ReporterManagerController> work = new WorkQueue<ReporterManagerController>();
  private String                 tempDir = null;


  /**
   * Create a reporter manager object which will manage a  instance of a
   * reporter manager on a remote resource.
   *
   * @param resource   Name of the resource (from the resource configuration
   *                   file) to start the reporter manager on.
   * @param agent      The agent the reporter manager is registered with
   *
   * @throws ConfigurationException  if there is missing configuration infor
   * for the resource.
   */
  public ReporterManagerController( String resource, Agent agent )
    throws ConfigurationException {

    // properties read in from agent configuration
    this.resource = resource;
    this.agent = agent;
    this.setTempDir( agent.getTempPath() );
    this.rmStarter = new ReporterManagerStarter( resource, agent );

    // e.g., var/rm/resource
    catalogFile = new File( this.tempDir + File.separator + "catalog.xml" );
    if ( catalogFile.exists() ) {
      try {
        catalog = CatalogDocument.Factory.parse( catalogFile );
        logger.info(
          "Read reporter manager packages list from '" + catalogFile + "'"
        );
      } catch ( Exception e ) {
        logger.error("Unable to read existing packages file for " + resource,e);
      }
    } else {
      try {
        createBlankCatalog();
      } catch ( IOException e ) {
        throw new ConfigurationException
          ( "Cannot create packages file " + catalogFile.getAbsolutePath(), e );
      }
    }

    // find any approved (but unsent) and proposed suite changes
    String suitesDir = getTemporaryDirectory() + File.separator + SUITE_DIR;
    this.approvedSuites = new File( suitesDir + File.separator + APPROVED_DIR );
    this.committedFile = new File( suitesDir + File.separator + COMMITTED_FILE);
    this.depotSuites = new File( suitesDir + File.separator + DEPOT_DIR );
    this.proposedSuites = new File( suitesDir + File.separator + PROPOSED_DIR );
    for ( File dir : new File[]{ approvedSuites, depotSuites, proposedSuites }){
      if ( ! dir.exists() ) dir.mkdirs();
    }
    for ( File unsentSuiteFile : this.approvedSuites.listFiles() ) {
      try {
        this.addSuite( SuiteDocument.Factory.parse(unsentSuiteFile) );
      } catch (Exception e) {
        logger.error( "Error reading approved suite " +
                      unsentSuiteFile.getAbsolutePath(), e);
      }
    }

    this.updateConfig();
  }

  /**
   * Create a new reporter manager object, whose configuration is identical to
   * rm, which will manage an instance of a reporter manager on a remote resource.
   *
   * @param rm  A reporter manager object that will be used to configure this
   * reporter manager object.
   */
  public ReporterManagerController( ReporterManagerController rm ) {

    this.approvalEmail = rm.approvalEmail;
    this.agent = rm.agent;
    this.approvedSuites = rm.approvedSuites;
    this.catalog = rm.catalog;
    this.catalogFile = rm.catalogFile;
    this.proposedSuites = rm.proposedSuites;
    this.proxy = rm.proxy;
    this.resource = rm.resource;
    this.rmStarter = new ReporterManagerStarter(rm.getReporterManagerStarter());
    this.tempDir = rm.tempDir;
  }

  /**
   * Add the specified package to the work queue so that the register
   * thread can pick it up and send it to the reporter manager.
   *
   * @param packageName  The name of the package to send to the remote reporter
   * manager.
   */
  public void addPackage( String packageName ) {
    logger.info(
      "Adding package " + packageName + " to the queue for " + resource
    );
    this.work.addWork( new SendPackage(packageName) );
    logger.info( "Package " + packageName + " added to queue for " + resource );
  }

  /**
   * Add proposed suite changes to the manager.  These suite changes get written
   * to a special directory and get approved and sent to the manager by
   * a separate thread (Approve command) therefore we synchronize access to
   * the directory where they are stored.
   *
   * @param suite  A suite document containing proposed changes to the manager
   *
   * @throws IOException if cannot write proposed suite changes to disk
   * @throws XmlException if unable to read proposed changes from disk
   *
   * note: intellij complains about use of proposedSuites to do synchronization
   * because it thinks it will not be unique among separate threads. But this ok
   * because in our case, the two threads will have access to the same object
   */
  public int addProposedSuite( SuiteDocument suite )
    throws IOException, XmlException {

    SeriesConfig[] proposedConfigs =
      suite.getSuite().getSeriesConfigs().getSeriesConfigArray();

    // if a suite is a run now suite, we check to see if series are allowed
    // to be executed (i.e., approved series) and send them on thru
    if ( suite.getSuite().getName().equals(Protocol.IMMEDIATE_SUITE_NAME) ) {
      logger.info( "Run now suite added to proposed" );
      SuiteWrapper approvedRunNows = new SuiteWrapper();
      approvedRunNows.copySuiteAttributes(suite);
      SuiteWrapper committedSuite =
        new SuiteWrapper( this.committedFile.getAbsolutePath() );
      for ( SeriesConfig sc :
        suite.getSuite().getSeriesConfigs().getSeriesConfigArray() ) {
        if ( committedSuite.hasRunNowMatch( sc ) ) {
          approvedRunNows.appendSeriesConfig(sc);
        }
      }
      if ( approvedRunNows.getSeriesConfigCount() > 0 ) {
      logger.debug( "Approving " + approvedRunNows.getSeriesConfigCount() +
                    " run nows for execution" );
      this.addSuite( approvedRunNows.getSuiteDocument() );
      } else {
        logger.warn
          ( "No series in run now suite have been approved...discarding" );
      }
      return 0;
    }

    // otherwise we add them to the queue
    logger.debug( "Adding " + proposedConfigs.length + " proposed changes to " +
                  suite.getSuite().getName() );
    String proposedSuiteFilename =
      this.proposedSuites+File.separator+suite.getSuite().getName() + ".xml";
    File proposedSuite = new File( proposedSuiteFilename );
    int totalProposed = 0;
    synchronized( this.proposedSuites ) {
      if ( proposedSuite.exists() ) {
        SuiteWrapper existingProposed = new SuiteWrapper(proposedSuiteFilename);
        for (SeriesConfig proposedConfig : proposedConfigs ) {
          // if 1 less, it was merged -- if 1 more new change
          int count = existingProposed.mergeSeriesConfig( proposedConfig );
          if ( count > 0 ) totalProposed += count;
        }
        existingProposed.save( proposedSuiteFilename );
      } else {
        totalProposed += proposedConfigs.length;
        suite.save( proposedSuite );
      }
    }
    return totalProposed;
  }

  /**
   * Add the specified suite to the work queue so that the register
   * thread can pick it up and send it to the reporter manager. Back it up
   * to the manager's approved suite changes directory in case the agent gets
   * restarted before it has a chance to send the suite to the manager.
   *
   * @param suite  A suite document to send to the reporter manager.
   *
   * @throws IOException if cannot backup suite changes to disk
   */
  public void addSuite( SuiteDocument suite ) throws IOException {
    if ( ! suite.getSuite().getName().contains("@") ) {
      suite.getSuite().setName( suite.getSuite().getName() + "@" +
                                Calendar.getInstance().getTimeInMillis());
    }
    this.work.addWork( new SendSuite(suite) );
    File approvedSuite = new File
      ( this.approvedSuites + File.separator + suite.getSuite().getName() +
        "-" + suite.getSuite().getVersion() + ".xml" );
    if ( ! approvedSuite.exists() ) suite.save( approvedSuite );
    logger.debug( "Added suite changes " + suite.getSuite().getName() + " v" +
      suite.getSuite().getVersion() + " to queue for " + resource );
  }

  /**
   * Tell the agent that the resource administrators have accepted specific
   * proposed changes.
   *
   * @param inca  An Inca config document containing a list of suites with
   * approved series config changes
   *
   * @throws IOException if cannot write proposed suite changes to disk
   * @throws XmlException if unable to read proposed changes from disk
   *
   * note: intellij complains about use of proposedSuites to do synchronization
   * because it thinks it will not be unique among separate threads. But this ok
   * because in our case, the two threads will have access to the same object
   */
  public void approveSuites( IncaDocument inca )
    throws IOException, XmlException {

    synchronized( this.proposedSuites ) {
      for ( Suite approvedSuite : inca.getInca().getSuites().getSuiteArray() ) {
        String proposedSuiteFilename =
          this.proposedSuites+File.separator + approvedSuite.getName() + ".xml";
        File proposedSuite = new File( proposedSuiteFilename );
        if ( proposedSuite.exists() ) {
          SuiteWrapper proposed = new SuiteWrapper( proposedSuiteFilename );
          SuiteWrapper approved = new SuiteWrapper( approvedSuite );
          SuiteWrapper committed = new SuiteWrapper();
          committed.copySuiteAttributes( approved.getSuiteDocument() );

          // for each config we are able to find in saved proposed changes,
          // remove from proposed list, remove from manager's approved list,
          // and add to a list of configs to commit.  Leftovers will be sent
          // back to manager
          for( SeriesConfig approvedConfig :
               approvedSuite.getSeriesConfigs().getSeriesConfigArray() ) {
            // if able to successfully remove from agent, remove from doc
            if ( proposed.removeSeriesConfig( approvedConfig ) ) {
              committed.appendSeriesConfig( approvedConfig );
            } else {
              logger.warn( "Unable to find approved config in proposed " +
                           approvedConfig.getNickname() );
            }
          }
          if ( committed.getSeriesConfigCount() > 0 ) {
            logger.debug
              ( "Successfully approved " + committed.getSeriesConfigCount() +
                " changes to " + approvedSuite.getName() );
            this.addSuite( committed.getSuiteDocument() );
          }
          if ( proposed.getSeriesConfigCount() > 0 ) {
            proposed.save( proposedSuiteFilename );
          } else {
            proposedSuite.delete(); // clean it out if none left
          }
        } else {
          logger.warn( "Unable to find any proposed changes to suite " +
                       approvedSuite.getName() );
        }
      }
    }
  }

  /**
   * Reinitialize the manager state on the agent in preparation for a new
   * manager registration.  First clear the queue and then clear out all
   * proposed and approved suites.  I.e., reset state
   */
  public void reinitialize() {
    logger.info( "Reinitializing state" );

    // clear anything in the queue
    logger.debug( "Clearing work queue" );
    this.work.clear();

    // clear packages
    try {
      this.createBlankCatalog();
    } catch (IOException e) {
      logger.warn( "Unable to reinitialize catalog" );
    }

    // clear the proposed and approved state
    logger.debug( "Clearing approved, committed, proposed, and depot suites" );
    File[] dirs = new File[]{ proposedSuites, approvedSuites, depotSuites };
    for ( File dir : dirs ) {
      synchronized( dir ) {
        for ( File suite : dir.listFiles() ) {
          suite.delete();
        }
      }
    }
    if ( committedFile.exists() ) committedFile.delete();

    // add all the suites to the manager
    SuiteTable suites = this.agent.getSuites();
    for ( String name : suites.getNames() ) {
      SuiteStagesWrapper suite = suites.getSuite( name );
      HashMap<String, SuiteWrapper> resourceSuites = suite.getResourceSuites();
      if ( resourceSuites.containsKey(resource) ) {
        logger.info( "Add suite " + name + " to queue for " + resource );
        SuiteDocument suiteDoc=resourceSuites.get(resource).getSuiteDocument();
        try {
          if ( this.hasApprovalEmail() ) {
            this.addProposedSuite( suiteDoc );
          } else {
            this.addSuite( suiteDoc );
          }
        } catch (Exception e) {
          logger.error( "Unable to add suite " + name + " to " + resource );
        }
      }
    }

    logger.debug( "Work queue reinitialized for resource " + resource );

}

  /**
   * Extract the reporters from the specified suite document
   * which do not already exist on the remote reporter manager.
   *
   * @param s  A suite document that will be sent to the reporter manager
   *
   * @return  An array of reporter packages that exist in the suite and
   * do not exist on the remote reporter manager.
   */
  public PackageType[] extractReportersFromSuite( SuiteDocument s ){
    Vector<PackageType> packages = new Vector<PackageType>();
    for ( XmlObject result : s.selectPath( "//series" ) ) {
      Series series = (Series)result;
      if ( ! this.hasPackage(series.getName(), series.getVersion()) ) {
        // don't add the same package to packages twice
        int j = 0;
        for ( ; j < packages.size(); j++ ) {
          PackageType pkgToAdd = packages.get(j);
          if ( series.getName().equals(pkgToAdd.getName()) ) {
            if ( series.isSetVersion() ) {
              if ( series.getVersion().equals(pkgToAdd.getVersion()) ) break;
            } else {
              if ( pkgToAdd.isSetLatestVersion() ) break;
            }
          }
        }
        if ( j >= packages.size() ) {
          PackageType pkgToSend = this.agent.getRepositoryCache().getPackage(
            series.getName(), series.getVersion()
          );
          if ( pkgToSend != null ) {
            packages.add( pkgToSend );
          } else {
            logger.error
              ( "Unable to find reporter " + series.getName() + ", version='" +
                series.getVersion() + "' in reporter cache");
          }
        }
      }
    }
    return packages.toArray( new PackageType[packages.size()] );
  }

  /**
   * Return the email of the admin to send approval requests too
   *
   * @return An email address for the admin
   */
  public String getApprovalEmail() {
    return approvalEmail;
  }

  /**
   * Return the directory handle to where suites that have not yet been sent
   * to the reporter manager has been sent.
   *
   * @return directory handle to suite directory
   */
  public File getApprovedSuiteDirectory() {
    return approvedSuites;
  }

  /**
    * Return the file handle to where committed series configs are stored
    *
    * @return directory handle to committed file
    */
   public File getCommittedFile() {
     return this.committedFile;
   }

  /**
   * Return the directory handle to where suites that have not yet been sent
   * to the depot has been sent.
   *
   * @return directory handle to depot suite directory
   */
  public File getDepotSuiteDirectory() {
    return depotSuites;
  }

  /**
   * Return the client connection to the remote reporter manager.
   *
   * @return  The manager client object connected to the remote reporter
   * manager.
   */
  public ManagerClient getManagerClient() {
    return managerClient;
  }

  /**
   * Return the packages that are installed on the reporter manager.
   *
   * @return A property list of packages that are installed on the reporter
   * manager where the package names are the "name" and package versions are
   * the "value".
   */
  public PackageType[] getPackages() {
    return catalog.getCatalog().getPackageArray();
  }

  /**
   * Return the proposed changes for this reporter manager.
   *
   * @return  An Inca document containing all of the suites with proposed
   * changes
   */
  public IncaDocument getProposedChanges() {
    IncaDocument sendProposed = IncaDocument.Factory.newInstance();
    IncaDocument.Inca.Suites sendProposedSuites =
      sendProposed.addNewInca().addNewSuites();
    int sendProposedCount = 0;
    synchronized( this.proposedSuites ) {
      for ( File proposedFile : this.proposedSuites.listFiles() ) {
        try {
          SuiteDocument proposed = SuiteDocument.Factory.parse(proposedFile);
          sendProposedSuites.addNewSuite();
          sendProposedSuites.setSuiteArray
            ( sendProposedCount, proposed.getSuite() );
          sendProposedCount++;
        } catch (Exception e) {
          logger.error( "Unable to open proposed suite changes in " +
                        proposedFile.getAbsolutePath() );
        }
      }
    }
    return sendProposed;
  }

  /**
    * Return the directory handle to where proposed suites changes are stored
    *
    * @return directory handle to suite directory
    */
   public File getProposedSuiteDirectory() {
     return proposedSuites;
   }

  /**
   * Return the proxy object for this resource.
   *
   * @return A ReporterManagerProxy object containing the proxy information
   * for this resource or null if it does not exist.
   */
  public ReporterManagerProxy getProxy() {
    return this.proxy;
  }

  /**
   * Return the name of this resource.
   *
   * @return The name of the resource the reporter manager will execute on.
   */
  public String getResource() {
    return resource;
  }

  /**
   * Return true if the reporter manager is currently registered and running.
   *
   * @return  True if the reporter manager is currently registered and running
   * and false otherwise.
   */
  public boolean isRunning() {
    return isRunning;
  }

  public ReporterManagerStarter getReporterManagerStarter() {
    return rmStarter;
  }

  /**
   * Gets the directory path where the temporary files can be stored.
   *
   * @return  A path to a local directory.
   */
  public String getTemporaryDirectory() {
    return tempDir;
  }

  /**
   * Return whether changes should be approved for a resource.
   *
   * @return True if changes require approval and false otherwise
   */
  public boolean hasApprovalEmail() {
    return this.approvalEmail != null;
  }

  /**
   * Return true if the manager has been sent the given package; otherwise
   * return false.
   *
   * @param name      The name of the package to check for.
   *
   * @param version   The version of the package to check for.
   *
   * @return  true if the manager has been sent the given package; otherwise
   * return false.
   */
  public boolean hasPackage( String name, String version ) {
    for ( PackageType pkg : catalog.getCatalog().getPackageArray() ) {
      if ( name.equals(pkg.getName())  ) {
        if ( version == null ) {
          if ( pkg.isSetLatestVersion() ) {
            return true;
          }
        } else {
          if ( version.equals(pkg.getVersion()) ) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Return whether or not proxy information is listed for this resource.
   *
   * @return True if proxy information is available for this resource; otherwise
   * false.
   */
  public boolean hasProxy() {
    return this.proxy != null;
  }

  /**
   * Pings the reporter manager to check whether it is still running on the
   * remote resource and returns the status.
   *
   * @return  True if the reporter manager responded to the ping and false
   * otherwise.
   *
   */
  public boolean isRemoteManagerAlive() {
    try {
      this.managerClient.commandPing( PING_DATA  + " " + resource );
    } catch ( IOException e ) {
      logger.error(
        "Unable to ping reporter manager '" + resource + "' assuming down: " + e
      );
      return false;
    } catch ( ProtocolException e ) {
      logger.error(
        "Unexpected response to ping of reporter manager '" + resource +
        "': " +  e + "; continuing operation"
      );
      // let's see if we can still continue
      return true;
    }
    return true;
  }

  /**
   * Register the specified reporter manager with the agent so that the
   * agent can send it commands.  Accepts two types of registrations: NEW and
   * EXISTING.  If NEW, the agent will retrieve any existing suites for the
   * reporter manager and reinitialize its queue.  If EXISTING, will assume
   * manager has all of its suites and packages.  Holds a socket connection open
   * to the reporter manager and loops indefinitely (until a shutdown) to send
   * requests to the  reporter manager.  Currently requests are suite changes or
   * packages.  When either is found in the queue, it will propagate the changes
   * to the manager.
   *
   * @param reader  the ProtocolReader for reading responses from the remote
   * reporter manager.
   *
   * @param writer  the ProtocolWriter for sending commands to the remote
   *
   * @param freshInstall true if the reporter manager is being installed from
   * scratch and needs to be sent its schedule; false otherwise if reporter
   * manager is just being restarted (and already has its schedule)
   */
  public void register( ProtocolReader reader, ProtocolWriter writer,
                        boolean freshInstall ) {
    logger.debug( "Entering register for resource " + resource );

    this.managerClient.setReader( reader );
    this.managerClient.setWriter( writer );

    if ( freshInstall ) {
      this.reinitialize();
    }
    this.setRunning( true );    

    // temporary hack for batch reporter
    if ( agent.getRepositoryCache().existsLocally(BATCH_REPORTER, null) &&
      ! this.hasPackage(BATCH_REPORTER,null) ) {
      this.addPackage( BATCH_REPORTER );
    }

    // now wait for any changes to the schedule
    long lastPingTime = Calendar.getInstance().getTimeInMillis();
    while ( true ) {
      if ( isBeingShutdown() ) break;
      long now = Calendar.getInstance().getTimeInMillis();
      if ( (now - lastPingTime) >= this.agent.getPingPeriod() ) {
        lastPingTime = now;
        if ( ! isRemoteManagerAlive() ) {
          restart();
          break;
        }
      }
      this.sendUnsentSuitesToDepot();
      
      // wait around until an action needs to be sent to the remote reporter
      // manager
      WorkItem<ReporterManagerController> work = null;
      try {
        if ( this.work.isEmpty() ) { // we wait until there is
          Thread.sleep( SUITE_CHECK_PERIOD ); // something in the queue
          continue;
        }
        logger.info("Retrieving work for reporter manager '" + resource + "'" );
        work = this.work.getWork();
      } catch ( InterruptedException e ) {
        logger.info( "Received interrupt in register: " + e );
        break;
      }

      try {
    	  work.doWork(this);
      } catch ( InterruptedException e ) {
        logger.info(
          "Caught interrupt in while doing work in reporter manager " + resource
        );
        break;
      } catch ( Exception e ) {
        logger.error(
          "Error '" + e +
          "' detected while trying to send work to '" + resource +
          "' assuming down" );
        logger.info( "Adding work back to reporter manager " + resource );
        this.work.addWork( work );
        restart();
        break;
      }
    }
    this.setRunning( false );
    logger.debug( "Exitting register for resource " + resource );
  }

  /**
   * Send the specified suite and any needed reporters to the remote
   * reporter manager.  On success, send changes to the depot and then remove
   * the suite backup from disk.  Will pass thru an IOException so that it can
   * be addressed (most likely this means the reporter manager died and we need
   * to restart it).
   *
   * @param suiteDoc The suite to send to the remote reporter manager.
   *
   * @throws ConfigurationException if insufficient data to connect to depot
   * @throws CrypterException if trouble writing new version to suite on disk
   * @throws IOException if trouble sending suite
   * @throws InterruptedException if interrupted during send of suite
   * @throws ProtocolException if unexpected response to send request
   */
  public void sendSuite( SuiteDocument suiteDoc )
    throws ConfigurationException, CrypterException, IOException,
           InterruptedException, ProtocolException {

    // now an action is the sending of a suite -- first extract any
    // packages that may need to be sent ahead of time
    sendReporters( extractReportersFromSuite( suiteDoc ) );

    // Dependencies should be filled -- send the xml suite
    if ( ! this.isRemoteManagerAlive() ) {
      throw new IOException
        ( "Reporter manager " + resource +
          " appears to be down...aborting send of suite " +
          suiteDoc.getSuite().getName() );
    }

    // Suitename has been modified to be unique so that we can remove it from
    // file easily even when unable to send to RM --
    // contains suiteName@timestamp -- change it back to the original
    String suiteName, timestamp;
    if ( suiteDoc.getSuite().getName().contains("@") ) {
      String[] decrypt = suiteDoc.getSuite().getName().split("@");
      suiteName = decrypt[0];
      timestamp = "@" + decrypt[1];
      suiteDoc.getSuite().setName( suiteName );
    } else {
      suiteName = suiteDoc.getSuite().getName();
      timestamp = "";
    }
    this.managerClient.sendSuite( this.resource, suiteDoc );

    // Suite successfully sent to RM -- now store it in committed dir
    logger.debug( "Saving series to " + this.committedFile);
    try {
      SuiteWrapper committedSuite;
      if ( committedFile.exists() ) {
        committedSuite = new SuiteWrapper(committedFile.getAbsolutePath());
      } else {
        committedSuite = new SuiteWrapper();
      }
      committedSuite.copySuiteAttributes(suiteDoc);
      for (SeriesConfig committedConfig :
        suiteDoc.getSuite().getSeriesConfigs().getSeriesConfigArray() ) {
        if ( committedConfig.getSchedule().isSetCron() ) {
          committedSuite.mergeSeriesConfig( committedConfig );
        }
      }
      committedSuite.save( committedFile.getAbsolutePath() );
    } catch (XmlException e) {
      logger.error( "Unable to open committed suite " + committedFile, e );
      logger.error( "Writing suite to log " + suiteDoc.xmlText() );
    }

    // remove backup or move it to depot dir if unable to send to depot
    String postFix = File.separator + suiteName + timestamp + "-" +
                     suiteDoc.getSuite().getVersion() + ".xml";
    File suiteBackup = new File(this.approvedSuites.getAbsolutePath()+postFix);
    File depotBackup = new File(this.depotSuites.getAbsolutePath() + postFix );
    if ( ! suiteBackup.exists() ) {
      logger.error( "Unable to cleanup backup file " + suiteBackup.getAbsolutePath() );
    }
    try {
      this.agent.updateSuiteOnDepot( suiteDoc );
      // suite successfully sent to mananger and depot; remove backup
      synchronized( this.approvedSuites ) {
        suiteBackup.delete();
      }
    } catch (Exception e) {
      synchronized( this.approvedSuites ) {
        suiteDoc.save( suiteBackup );
        suiteBackup.renameTo( depotBackup );
      }
    }    
  }

  /**
   * Send the named package to the remote reporter manager along with all of
   * its dependencies.
   *
   * @param pkg  The package to send to the remote reporter manager
   *
   * @throws IOException if problem sending package
   * @throws InterruptedException if interrupted during send of package
   */
  public void sendPackage(String pkg) throws IOException, InterruptedException
  {
	  sendPackage(agent.getRepositoryCache().getPackage(pkg, null));
  }

  /**
   * Check to see if there are any suites that have not yet been sent to the
   * depot.  If there are, check the last time we attempted to send.  If it
   * exceeds the ping period, go ahead and make another attempt.
   */
  public void sendUnsentSuitesToDepot() {
    if ( this.depotSuites.listFiles().length > 0 && lastDepotSendAttempt > 0 ) {
      long timediff = Calendar.getInstance().getTimeInMillis() - lastDepotSendAttempt;
      if ( timediff < this.agent.getPingPeriod() ) {
        return;
      }
    }
    for ( File depotFile : this.depotSuites.listFiles() ) {
      try {
        lastDepotSendAttempt = Calendar.getInstance().getTimeInMillis();
        SuiteDocument proposed = SuiteDocument.Factory.parse(depotFile);
        this.agent.updateSuiteOnDepot(proposed);
        lastDepotSendAttempt = 0;
        depotFile.delete();
      } catch (Exception e) {
        logger.error( "Unable to send suite changes in " +
                      depotFile.getAbsolutePath() + " to depot", e );
      }
    }
  }
  
  /**
   * Require changes to the manager to be approved
   *
   * @param approvalEmail   An email to notify when proposed changes are added
   * to manager
   */
  public void setApprovalEmail(String approvalEmail) {
    this.approvalEmail = approvalEmail;
  }

  /**
   * Set the client connection to the remote reporter manager.
   *
   * @param managerClient  The manager client object connected to the remote reporter
   * manager.
   *
   */
  public void setManagerClient( ManagerClient managerClient ) {
    this.managerClient = managerClient;
  }

  /**
   * Set the proxy renewal information so it can be passed to the  remote
   * reporter manager on registration.
   *
   * @param proxy Contains MyProxy information that can be used to retrieve
   * a new proxy for the remote reporter manager.
   */
  public void setProxy( ReporterManagerProxy proxy ) {
    this.proxy = proxy;
  }


  /**
   * Set the status of the reporter manager to running and notify any
   * waiting threads.
   *
   * @param isRunning  True indicates the reporter manager is running on the
   * remote machine and available; false otherwise.
   */
  public void setRunning( boolean isRunning ) {
    synchronized(this) {
      logger.debug
        ("Set reporter manager " + resource + " status to running " +isRunning);
      this.isRunning = isRunning;
      this.getReporterManagerStarter().setRunning( isRunning );
      notifyAll(); // notify anybody who may be waiting on this
    }
  }

  /**
   * Set the location of the temporary directory.
   *
   * @param tempDir  A path to a directory where temporary files can be stored.
   */
  public void setTempDir( String tempDir ) {
    this.tempDir = tempDir + File.separator + Agent.RMDIR + File.separator +
                   resource;
    File dir = new File( this.tempDir );
    if ( ! dir.exists() ) {
      logger.info( "Creating temporary directory '" + this.tempDir + "'" );
      logger.info( "Directories created: " + dir.mkdirs() );
    }
  }

  /**
   * Start the reporter manager
   */
  public void start() {
    if ( ! this.getReporterManagerStarter().isManual() ) {
      this.getReporterManagerStarter().start();
    }
  }

  /**
   * Cleanly shutdown the reporter manager by signaling the register
   * thread to exit and sending an END statement to the remote reporter manager.
   */
  public synchronized void shutdown() {
    // close connection to reporter manager (i.e., shutdown)
    if ( isRunning ) {
      logger.debug( "Signaling '" + resource + "' to shutdown");
      try {
        this.managerClient.getWriter().close();
        this.managerClient.getReader().close();
      } catch ( IOException e ) {
        logger.error(
          "Problem sending shutdown to reporter manager '" + resource + "'", e
        );
      }
      needToShutdown = true;
    } else {
      logger.warn( "Shutdown requested but manager " + resource + " is not running" );

    }

    // signal register thread to exit.  Since register checks for shutdowns
    // periodically, we wait until we hear back from it before exitting.  That
    // way we'll be sure the task is recycled by the time the general shutdown
    // request is issued and it all can go cleanly.
    boolean shutdownComplete = false;
    do {
      try {
        logger.debug( "Agent waiting for reporter manager thread to exit" );
        wait();
        logger.debug( "Reporter manager thread for " + resource + " exitted" );
        needToShutdown = false;
      } catch ( InterruptedException e ) {
        logger.info( "waiting for reporter manager to shutdown", e );
      }
      shutdownComplete = shutdownDetected;
    } while( ! shutdownComplete );
  }

  /**
   * Tell the controller to re-read the macros from the agent's configuration
   *
   * @throws ConfigurationException if problem getting macro value
   */
  public boolean updateConfig() throws ConfigurationException {
    boolean restartNeeded = false;

    // is sys admin approval required before sending suite changes
    String approval = agent.getResources().getValue
      (resource, Protocol.EMAIL_MACRO );
    if ( approval != null &&
         (this.approvalEmail == null || ! this.approvalEmail.equals(approval)) ) {
      logger.debug( "Approval mode set using email: " + approval );
      approvalEmail =  approval;
    } else if ( approval == null && this.approvalEmail != null ) {
      logger.debug( "Turning approval mode off" );
      approvalEmail = null;       
    }

    // setup proxy information
    ReporterManagerProxy newProxy;
    try {
      newProxy = new ReporterManagerProxy(resource, agent.getResources());
      if ( this.proxy == null || ! this.getProxy().equals(newProxy) ) {
        logger.info( "Proxy renewal turned on for "  + resource );
        this.setProxy( newProxy );
      }
    } catch ( ConfigurationException e ) {
      if ( this.hasProxy() ) {
        logger.info( "Proxy renewal turned off for " + resource + ": " + e );        
        this.setProxy( null );
      }
    }
    String newSuspend =
      agent.getResources().getValue( this.resource, Protocol.SUSPEND_MACRO);
    String oldSuspend = this.getReporterManagerStarter().getSuspend();
    if ( (oldSuspend != null && newSuspend == null) ||
         (oldSuspend == null && newSuspend != null) ||
         (oldSuspend != null && newSuspend != null &&
           ! oldSuspend.equals(newSuspend)) ) {
      restartNeeded = true;
    }

    String newWaitCheckPeriod =
      agent.getResources().getValue( this.resource, Protocol.CHECK_PERIOD_MACRO);
    if ( newWaitCheckPeriod == null ) {
      newWaitCheckPeriod = Protocol.CHECK_PERIOD_MACRO_DEFAULT;
    }
    if ( this.getReporterManagerStarter().getWaitCheckPeriod() !=
      Integer.parseInt(newWaitCheckPeriod) ) {
      restartNeeded = true;
    }

    return restartNeeded;
  }

  /* Protected functions */


  /**
   * Send the specified reporters and their package dependencies to the remote
   * reporter manager.
   *
   * @param reporters  A list of reporters to send to the remote reporter
   * manager.
   *
   * @throws IOException if problem sending reporters
   * @throws InterruptedException if interrupted during send of reporters
   */
  protected void sendReporters( PackageType[] reporters )
    throws IOException, InterruptedException {

    logger.debug(
      "Sending " + reporters.length + " reporters to reporter manager "+resource
    );
    for ( PackageType reporter : reporters ) {
      sendPackage( reporter );
    }
  }

  /* Private functions */

  /**
   * Create a new catalog file for the reporter manager.
   *
   * @throws IOException if unable to create packages file
   */
  private void createBlankCatalog() throws IOException {
    this.catalog = CatalogDocument.Factory.newInstance();
    this.catalog.addNewCatalog();
    this.catalogFile.getParentFile().mkdirs();
    XmlOptions xmloptions = new XmlOptions();
    xmloptions.setSavePrettyPrint();
    catalog.save( catalogFile, xmloptions );
    logger.info( "Created new packages file" );
  }  

  /**
   * Check for a shutdown request and if true, notify any waiting threads.
   *
   * @return True if shutdown has been requested and false otherwise
   */
  private synchronized boolean isBeingShutdown() {
    if ( needToShutdown ) {
      logger.info( "Shutdown detected by reporter manager " + resource );
      shutdownDetected = true;
      this.notifyAll();
      return true;
    } else {
      return false;
    }
  }

  /**
   * Restart this reporter manager.  Since you cannot restart a thread we
   * create a duplicate reporter manager object and execute its start method.
   * We then replace the reporter manager in the reporter manager table.
   */
  public synchronized void restart() {
    if ( this.getReporterManagerStarter().isManual() ) return;
    logger.info( "Attempting reporter manager restart for " + resource );
    if ( isBeingShutdown() ) return;
    if ( this.agent.getAdminEmail() != null ) {
      StringMethods.sendEmail(
        this.agent.getAdminEmail(),
        "inca reporter manager '" + resource + "' restarted",
        "This is a notification from the Inca system that the reporter manager"
        + " executing on resource " + resource + " has been restarted"
      );
    }
    if ( this.rmStarter.isAlive() ) {
      logger.info( "Attempting to shutdown existing starter thread" );
      this.rmStarter.interrupt();
      try {
        this.rmStarter.join();
        logger.info( "Completed shutdown of existing starter thread" );
      } catch (InterruptedException e) {
        logger.warn( "Interrupted while trying to shut down starter thread" );
      }
    } else {
      logger.debug( "No existing starter thread to shut down" );      
    }
    this.rmStarter = new ReporterManagerStarter( resource, agent );
    this.start();
  }

  /**
   * Send the named package to the remote reporter manager along with all of
   * its dependencies.
   *
   * @param pkg  The package to send to the remote reporter manager
   *
   * @throws IOException if problem sending package
   * @throws InterruptedException if interrupted during send of package
   */
  private void sendPackage( PackageType pkg )
    throws IOException, InterruptedException {

    // this check may not be needed if I can figure out how null packages
    // are being sent via this function elsewhere -- problem originated from
    // David Spence's deployment
    if ( pkg == null ) {
      logger.error( "Received null package to send...skipping" );
      return;
    }
    String[] dependencies = new String[0];
    if ( pkg.getDependencies() == null ) {
      logger.warn( "package " + pkg.getName() + " has no dependencies" );
    } else {
      dependencies = pkg.getDependencies().getDependencyArray();
      for ( String dependency : dependencies ) {
        if ( ! this.hasPackage( dependency, null) ) {
          sendPackage
            ( this.agent.getRepositoryCache().getPackage(dependency, null) );
        }
      }
    }

    // send package to RM
    if ( ! this.isRemoteManagerAlive() ) {
      throw new IOException
        ( "Reporter manager " + resource +
          " appears to be down...aborting send of package " + pkg.getName() );
    }
    logger.info(
      "Sending package " + pkg.getName() + ", version=" + pkg.getVersion() +
      " to resource '" + resource +"'"
    );
    String dependsToSend = null;
    if ( dependencies.length > 0 ) {
      dependsToSend = StringMethods.join( " ", dependencies );
    }
    try {
      this.managerClient.sendPackage(
        pkg.getFilename(),
        pkg.getInstallPath(),
        pkg.getName(),
        this.agent.getRepositoryCache().getPackageContent(
          pkg.getName(), pkg.getVersion()
        ),
        pkg.getPermissions(),
        dependsToSend,
        pkg.getVersion()
      );
      //update our package listing for this reporter
      int newIndex = catalog.getCatalog().sizeOfPackageArray();
      catalog.getCatalog().insertNewPackage( newIndex );
      catalog.getCatalog().setPackageArray( newIndex, (PackageType)pkg.copy() );
      XmlOptions xmloptions = new XmlOptions();
      xmloptions.setSavePrettyPrint();
      catalog.save( catalogFile, xmloptions );
    } catch ( ProtocolException e ) {
      logger.error(
        "Unexpected result trying to send package " + pkg.getName() + " to '" + resource
        + "'; continuing operation: " + e
      );
    }

  }

 }
