package edu.sdsc.inca.agent;

import junit.framework.TestCase;

import java.util.Vector;
import java.util.Calendar;
import java.util.regex.Pattern;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.security.KeyStoreException;
import java.security.Security;
import java.security.PrivateKey;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;

import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.*;
import edu.sdsc.inca.agent.access.Sleep;
import edu.sdsc.inca.agent.access.Local;
import edu.sdsc.inca.util.ConfigProperties;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.util.ResourcesWrapperTest;
import edu.sdsc.inca.util.SuiteWrapper;
import edu.sdsc.inca.util.SuiteStagesWrapper;
import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.repository.Repositories;
import edu.sdsc.inca.repository.RepositoriesTest;
import edu.sdsc.inca.dataModel.resourceConfig.ResourceConfigDocument;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.dataModel.suite.Suite;
import edu.sdsc.inca.dataModel.catalog.PackageType;
import edu.sdsc.inca.dataModel.util.SeriesConfig;
import edu.sdsc.inca.dataModel.util.Macro;
import edu.sdsc.inca.dataModel.util.Macros;
import edu.sdsc.inca.dataModel.util.Cron;
import edu.sdsc.inca.dataModel.inca.IncaDocument;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLServerSocket;

/**
 * Tests for ReporterManagerController (and ReporterManagerStarter).
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class ReporterManagerControllerTest extends TestCase {
  private ConfigProperties config = null;
  private String resource = null;
  private static Logger logger = Logger.getLogger(ReporterManagerControllerTest.class);
  ReporterManagerController rmc = null;
  MockAgent mockAgent = null;
  private String testPassword = "topSecret!";
  static public File TEMP_RM_DIR = new File( "/tmp/inca2install2");
  static public File RM_SCHEDULE = new File
    ( TEMP_RM_DIR.getAbsolutePath() + File.separator + "var" + File.separator + "schedule.xml");


  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  /**
   * Emulates the agent's tasks
   */
  static public class MockAgent extends Thread {
    public ProtocolWriter writer = null;
    public ProtocolReader reader = null;
    public boolean freshInstall = true;
    public int port = 8934;
    public int numConnections = 1;
    public int numConnectionsReceived = 0;
    private ServerSocket serverSocket = null;
    private Socket server = null;
    private boolean auth = false;
    private ReporterManagerController rmc = null;

    public MockAgent( boolean auth, ReporterManagerController rmc )
      throws Exception {

      this.setName( "MockAgent" );
      this.auth = auth;
      this.rmc = rmc;
    }

    /**
     * Allows this mock agent to be run as a thread
     */
    @Override
    public void run() {
      try {
        if ( auth ) {
          logger.info("Mock Agent with auth");
          serverSocket = createSSLSocket( port );
        } else {
          logger.info("Mock Agent without auth");
          ServerSocketFactory factory =  ServerSocketFactory.getDefault();
          serverSocket = factory.createServerSocket( port );
          //serverSocket.setSoTimeout(Server.ACCEPT_TIMEOUT);
        }
        for ( int i = 0; i < numConnections; i++ ) {
          String resourceName = null;
          try {
            resourceName = mockAgentRegister();
            logger.debug( resourceName + " registered" );
          } catch ( Exception e ) {
            logger.error( "Error in registration", e );
            continue;
          }
          numConnectionsReceived++;
          if ( rmc == null ) {
            Agent.getGlobalAgent().registerReporterManager(
              resourceName, reader, writer, null, this.freshInstall
            );
          } else {
            rmc.register( reader, writer, this.freshInstall );
            logger.debug( "end register method" );
          }
        }
      } catch ( InterruptedException e ) {
        logger.debug( "Interrupting mock agent" );
      } catch ( Throwable e ) {
        logger.debug( "Exception detected in MockAgent" );
        e.printStackTrace();
      } finally {
        logger.info( "Exitting" );
        cleanup();
      }
    }

    /**
     * Mimics the agent's register task
     *
     * @return  The name of the resource that registered.
     *
     * @throws Exception if test fails
     */
    public String mockAgentRegister() throws Exception {
      logger.info( "Waiting for connection");
      server = serverSocket.accept();
      //server.setSoTimeout(Server.CLIENT_TIMEOUT);
      logger.info( "Approve connection");
      reader = new ProtocolReader(
        new InputStreamReader(server.getInputStream())
      );
      writer = new ProtocolWriter(
        new OutputStreamWriter(server.getOutputStream())
      );

      Statement stmt = reader.readStatement();
      String response = new String( stmt.getCmd() );
      if ( response.equals("START") ) {
        logger.info( "Mock agent received start" );
        writer.write( Statement.getOkStatement(Statement.getVersion()) );
        stmt = reader.readStatement();
      }

      response = new String( stmt.getCmd() );
      char[] pingdata = stmt.getData();

      assertTrue(
        "Received reporter manager ping",
        response.equals("PING")
      );

      writer.write(
        new Statement(
          "OK".toCharArray(),
          pingdata
        )
      );
      logger.info( "Mock agent received ping" );

      Statement register = reader.readStatement();
      response = new String( register.getCmd() );
      assertTrue(
        "Received reporter manager registration",
        response.equals("REGISTER")
      );
      String data = new String( register.getData() );
      freshInstall = data.split( " " )[0].equals("NEW");
      logger.debug( "REGISTER is fresh install: " + freshInstall );
      String resource = data.split( " " )[1];
      writer.write(
        new Statement( Protocol.SUCCESS_COMMAND.toCharArray())
      );
      logger.info( "Mock agent received register" );
      return resource;
    }

    /**
     * Ensure's correct cleanup. Shutdown the reporter manager and close the
     * streams and sockets.
     *
     * @throws java.io.IOException if problem cleaning up
     */
    public void cleanup() {
      logger.info( "Running MockAgent finalize");
      try {
        if ( reader != null ) {
          reader.close();
        }
        if ( writer != null ) {
          writer.close();
        }
        if ( server != null ) {
          server.close();
        }
        if ( serverSocket != null ) {
          serverSocket.close();
        }
      } catch (IOException e) {
        logger.warn( "Exception detected while closing connections" );
      }
    }
  }

  /**
   * Create a SSL server socket
   *
   * @param port  The port to open the socket on.
   *
   * @return A configured SSL ServerSocket.
   *
   * @throws Exception  if unable to create ssl socket
   */
  static public ServerSocket createSSLSocket( int port ) throws Exception {

      X509Certificate cert = readCertData(AgentTest.AGENT_CERT);
      PrivateKey key = readKeyData(AgentTest.AGENT_KEY, AgentTest.PASSWORD);
    /*  Certificate cert = readCertData(AgentTest.CA_CERT); */


    // Init the keystore w/the server cert and the trusted certificates,
    // making sure we also trust our own certificate.
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null, null);
    ks.setKeyEntry
      ("serverkey", key,
       "".toCharArray(), new Certificate[]{cert});
    ks.setCertificateEntry
      ("trusted0", cert);

    // Use an SSLContext inited from the keystore to open a server socket.
    SSLContext context = SSLContext.getInstance("SSL");
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    kmf.init(ks, "".toCharArray());
    tmf.init(ks);
    context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    ServerSocket ssocket =context.getServerSocketFactory().createServerSocket();
    ((SSLServerSocket) ssocket).setNeedClientAuth(true);
    // Allow for immediate server restart w/out a wait for port release.
    ssocket.setReuseAddress(true);
    // After this call the port is actually in business.
    ssocket.bind(new InetSocketAddress(port));

    return ssocket;
  }

 /**
   * Create a sample suite document and create the reporter uris
   * relative to the specified repository path.
   *
   * @param resource   The name of a resource to schedule the suite to.
   *
   * @return  A suite document.
   *
   * @throws XmlException  if unable to parse sample suite
   */
  public static SuiteWrapper createSampleSuite( String resource )
    throws XmlException {

    if ( resource == null ) resource = "localhost";

    String suiteDoc =
      "<st:suite xmlns:st = \"http://inca.sdsc.edu/dataModel/suite_2.0\" xmlns:xsi = \"http://www.w3.org/2001/XMLSchema-instance\" >\n" +
      "  <seriesConfigs>\n" +
      "  <seriesConfig>\n" +
      "    <series>\n" +
      "      <name>cluster.compiler.gcc.version</name>\n" +
      "      <version>1.5</version>\n" +
      "      <args>\n" +
      "        <arg>\n" +
      "          <name>verbose</name>\n" +
      "          <value>1</value>\n" +
      "        </arg>\n" +
      "        <arg>\n" +
      "          <name>help</name>\n" +
      "          <value>no</value>\n" +
      "        </arg>\n" +
      "      </args>\n" +
      "      <limits>\n" +
      "        <wallClockTime>300</wallClockTime>\n" +
      "      </limits>\n" +
      "      <context>cluster.compiler.gcc.version -verbose=\"1\" -help=\"no\"</context>\n" +
      "    </series>\n" +
      "    <nickname>cluster.compiler.gcc.version</nickname>\n" +
      "    <resourceSetName>" + resource + "</resourceSetName>\n" +
      "    <schedule>\n" +
      "      <cron>\n" +
      "        <min>0-59/1</min>\n" +
      "        <hour>*</hour>\n" +
      "        <mday>*</mday>\n" +
      "        <wday>*</wday>\n" +
      "        <month>*</month>\n" +
      "      </cron>\n" +
      "    </schedule>\n" +
      "    <action>add</action>\n" +
      "  </seriesConfig>\n" +
      "  <seriesConfig>\n" +
       "    <series>\n" +
      "      <name>cluster.security.openssl.version</name>\n" +
      "      <version>1.3</version>\n" +
      "      <args>\n" +
      "        <arg>\n" +
      "          <name>verbose</name>\n" +
      "          <value>1</value>\n" +
      "        </arg>\n" +
      "        <arg>\n" +
      "          <name>help</name>\n" +
      "          <value>no</value>\n" +
      "        </arg>\n" +
      "      </args>\n" +
      "      <limits>\n" +
      "        <wallClockTime>300</wallClockTime>\n" +
      "      </limits>\n" +
      "      <context>cluster.security.openssl.version -verbose=\"1\" -help=\"no\"</context>\n" +
      "    </series>\n" +
      "    <nickname>cluster.security.openssl.version</nickname>\n" +
      "    <resourceSetName>" + resource + "</resourceSetName>\n" +
      "    <schedule/>\n" +
      "    <action>add</action>\n" +
      "  </seriesConfig>\n" +
      "  <seriesConfig>\n" +
      "    <series>\n" +
      "      <name>user.search.output.unit</name>\n" +
      "      <version>1.1</version>\n" +
      "      <args>\n" +
      "             <arg>\n" +
      "                <name>version</name>\n" +
      "                <value>no</value>\n" +
      "              </arg>\n" +
      "              <arg>\n" +
      "                <name>verbose</name>\n" +
      "                <value>1</value>\n" +
      "              </arg>\n" +
      "              <arg>\n" +
      "                <name>com</name>\n" +
      "                <value>ls var/reporter-packages/include</value>\n" +
      "              </arg>\n" +
      "              <arg>\n" +
      "                <name>help</name>\n" +
      "                <value>no</value>\n" +
      "              </arg>\n" +
      "              <arg>\n" +
      "                <name>search</name>\n" +
      "                <value>somefile</value>\n" +
      "              </arg>\n" +
      "              <arg>\n" +
      "                <name>log</name>\n" +
      "                <value>3</value>\n" +
      "              </arg>\n" +
      "              <arg>\n" +
      "                <name>delim</name>\n" +
      "                <value>\\|</value>\n" +
      "              </arg>\n" +
      "      </args>\n" +
      "      <limits>\n" +
      "        <wallClockTime>300</wallClockTime>\n" +
      "      </limits>\n" +
      "      <context>user.search.output.unit -version=\"no\" -verbose=\"1\" -com=\"ls var/reporter-packages/include\" -help=\"no\" -search=\"somefile\" -log=\"3\" -delim=\"\\|\"</context>\n" +
      "    </series>\n" +
      "    <nickname>user.search.output.unit</nickname>\n" +
      "    <resourceSetName>" + resource + "</resourceSetName>\n" +
      "    <schedule/>\n" +
      "    <action>add</action>\n" +
      "  </seriesConfig>\n" +
      "  </seriesConfigs>\n" +
      "  <name>TestSuiteLocal</name>\n" +
      "  <guid>inca://localhost:6323/TestSuiteLocal</guid>\n" +
      "  <version>1</version>\n" +
      "</st:suite>";
    return new SuiteWrapper( SuiteDocument.Factory.parse(suiteDoc) );
  }

  /**
   * Create a reporter manager object using the specified uri as the contact
   * to the agent.
   *
   * @param secure      Whether the mock agent server should be secure or not
   * @param repository  Whether or not we need to setup a temporary repository
   * @param resources   The resources the agent should be configured with
   * @param resource    The name of the resource in resources we should use to
   *                    start the reporter manager on.
   *
   * @return a new reporter mananger controller object
   *
   * @throws Exception  if trouble creating test reporter manager object
   */
  public ReporterManagerController createRM(
    boolean secure, boolean repository,
    ResourcesWrapper resources, String resource )
    throws Exception {

    if ( resource == null ) resource = this.resource;
    Agent agent = new Agent() {
      @Override
      public void readCredentials() throws ConfigurationException, IOException {
        try {
          this.cert = readCertData(AgentTest.AGENT_CERT);
          this.key = readKeyData(AgentTest.AGENT_KEY, AgentTest.PASSWORD);
        }
        catch (Exception err) {
          throw new ConfigurationException(err);
        }
        this.trusted = new Vector();
        this.trusted.add(readPEMData(AgentTest.AGENT_CERT));
        this.trusted.add(readPEMData(AgentTest.CA_CERT));
      }
    };
    Agent.setGlobalAgent( agent );
    if ( AgentTest.MockDepot.getDepot() != null ) {
      config.setProperty( "depot", AgentTest.MockDepot.getDepot().getUri() );
    }
    agent.setConfiguration( config );
    agent.setAuthenticate( secure );
    if ( secure ) {
      agent.readCredentials();
    }
    agent.setPort( 8934 );
    if ( resources == null ) {
      resources = ResourcesWrapperTest.createSampleResources();
    }
    agent.setResources( resources );
    agent.setStartAttemptWaitPeriod( 240000 );
    ReporterManagerController rm = new ReporterManagerController(
      resource, agent
    );
    if ( repository ) {
      File resourcePackages = new File( "var/rm/localhost/packages" );
      resourcePackages.deleteOnExit();
      Repositories repositories = RepositoriesTest.createSampleRepository(null);
      RepositoryCache cache = RepositoryCacheTest.createSampleRepositoryCache(
        repositories
      );
      agent.setRepositories( repositories.getRepositories() );
      agent.setRepositoryCache( cache );
    }

    return rm;
  }

  /**
   * Stop the reporter manager process externally
   */
  public void killReporterManagerProcess() {
    try {
      Local proc = new Local( "", null );
      proc.run( TEMP_RM_DIR.getAbsolutePath()+"/sbin/inca",
        new String[] {"stop", "reporter-manager"}, "\n",
        TEMP_RM_DIR.getAbsolutePath() );
    } catch (Throwable t) {
      logger.error( "exception detected", t);
    }
  }

  /**
   * Read and verify reports from samplesuite
   *
   * @param depot A mock depot object
   */
  public void receiveStdSuiteReports(AgentTest.MockDepot depot) {
    String[] reports = depot.waitForReports( 3 );
    boolean gccFound = false, opensslFound = false, searchFound = false;
    for ( int i = 0; i < reports.length; i++ ) {
      logger.debug( "REPORT " + i + " " + reports[i] );
      if ( Pattern.compile( "gcc").matcher( reports[i] ).find() ) {
        gccFound = true;
      }
      if ( Pattern.compile( "openssl").matcher( reports[i] ).find() ) {
        opensslFound = true;
      }
      if ( Pattern.compile("search").matcher(reports[i]).find() &&
           Pattern.compile("<completed>true").matcher(reports[i]).find()) {
        searchFound = true;
      }
    }
    assertTrue( "gcc reporter found ", gccFound );
    assertTrue( "openssl reporter found ", opensslFound );
    assertTrue( "search reporter found ", searchFound );
  }

  /**
   * Test the reporter manager's create and sendSuite functions.
   *
   * @param auth  whether to test with credentials or not
   *
   * @throws Exception  if error during send of sample subscription
   *
   * @return depot
   */
  public AgentTest.MockDepot runCreateAndSubscribe( boolean auth )
    throws Exception {

    AgentTest.MockDepot depot = AgentTest.startDepot( false );
    logger.info( "Starting Mock depot at " + depot.getUri() );
    startAndRegisterRMC( auth, true, null, 1, null );

    SuiteWrapper suite = createSampleSuite( null );
    SuiteStagesWrapper ss = new SuiteStagesWrapper( rmc.agent.getResources() );
    ss.modify(suite);

    try {
      rmc.agent.getSuites().putSuite(ss);
      PackageType[] reps = rmc.extractReportersFromSuite(
        suite.getSuiteDocument()
      );
      assertEquals( "3 reporters extracted", 3, reps.length );
      logger.info( "Sending suite" );
      rmc.sendSuite( suite.getSuiteDocument() );
    } catch( Exception e ) {
      logger.error( "Sending suite failed: ", e );
      fail( "Sending suite failed: " + e );
    }
    receiveStdSuiteReports(depot);

    assertTrue( "committed file exists", rmc.getCommittedFile().exists() );
    assertTrue
      ( "schedule file created on RM: " + RM_SCHEDULE.getAbsolutePath(),
        RM_SCHEDULE.exists() );
    String rmSched = StringMethods.fileContents(RM_SCHEDULE.getAbsolutePath());
    logger.debug( rmSched );
    SuiteWrapper agentSuite = new SuiteWrapper
      (rmc.getCommittedFile().getAbsolutePath() );
    assertEquals( "committed and manager have same series #",
                  agentSuite.getSeriesConfigCount(), rmSched.split("<seriesConfig>").length-1 );

    return depot;
  }

  /**
    * Read properties, initialize member variables, and delete var directory
    *
    * @throws Exception
    */
   @Override
  public void setUp() throws Exception {
     config = new ConfigProperties();
     config.putAllTrimmed(System.getProperties(), "inca.agent.");
     config.loadFromResource("inca.properties", "inca.agent.");
     config.remove( "logfile" );
     resource = AgentTest.TEST_RESOURCE;

     StringMethods.deleteDirectory( new File("var") );
     StringMethods.deleteDirectory(TEMP_RM_DIR);
   }

  /**
   * Start up a remote reporter manager and a mock agent.  Return when the
   * remote reporter manager has checked in and registered.
   *
   * @param secure      Whether the mock agent server should be secure or not
   * @param repository  Whether or not we need to setup a temporary repository
   * @param resources   The resources the agent should be configured with
   * @param numConnections The number of connections the mock agent should
   *                       expect before exitting.
   * @param resource    The name of the resource to start this reporter manager
   *                    on.
   *
   * @throws Exception  If trouble starting up a remote reporter managers
   */
  public void startAndRegisterRMC(
    boolean secure, boolean repository, ResourcesWrapper resources,
    int numConnections, String resource )
    throws Exception {

    if ( resource == null ) resource = this.resource;
    rmc = createRM( secure, repository, resources, resource );
    Agent.getGlobalAgent().getReporterManagerTable().put( resource, rmc );
    if ( secure ) {
      mockAgent = new MockAgent( true, rmc );
    } else {
      mockAgent = new MockAgent( false, rmc );
    }
    if ( mockAgent == null ) throw new IOException( "Unable to start server" );
    mockAgent.numConnections = numConnections;
    rmc.getReporterManagerStarter().start();
    mockAgent.start();
    rmc.getReporterManagerStarter().waitForReporterManager();
    logger.info( "Reporter manager checked in" );
  }

   /**
   * Close reporter manager controller and mock agent if used in test
   *
   * @throws Exception
   */
  @Override
  public void tearDown() throws Exception {
    if ( rmc != null && rmc.isRunning() ) rmc.shutdown();
    if ( mockAgent != null ) mockAgent.join();
    if ( AgentTest.MockDepot.getDepot() != null ) {
      try {
        logger.debug( "Trying to shutdown agent" );
        AgentTest.MockDepot.getDepot().shutdown();
        logger.debug( "Shutdown complete" );
      } catch(InterruptedException e) {
        // empty
      }
    }
    StringMethods.deleteDirectory(TEMP_RM_DIR);
    StringMethods.deleteDirectory(new File("var"));
  }

  /**
    * Test the reporter manager's approval mode.
    *
    * @throws Exception  if trouble executing test
    */
   public void testApproval() throws Exception {
     // create RM
     AgentTest.MockDepot depot = AgentTest.startDepot( false );
     logger.info( "Starting Mock depot at " + depot.getUri() );
     startAndRegisterRMC( false, true, null, 1, null );
     logger.info( "----------------- RM registered -----------------------" );


     // add 3 proposed suite changes
     SuiteWrapper suite = createSampleSuite( null );
     // first series has a cron schedule -- copy it to others
     Cron cron = null;
     for ( SeriesConfig sc : suite.getSeriesConfigs() ) {
       if ( sc.getSchedule().isSetCron() ) {
         cron = sc.getSchedule().getCron();
       } else {
         sc.getSchedule().setCron(cron);
       }
     }
     SuiteStagesWrapper ss = new SuiteStagesWrapper( rmc.agent.getResources() );
     ss.modify(suite);
     rmc.agent.getSuites().putSuite(ss);
     rmc.addProposedSuite( suite.getSuiteDocument() );
     logger.info( "----------------- add proposed changes -------------------" );


     // check proposed files written
     File proposedFile = new File (
       rmc.getProposedSuiteDirectory() + File.separator +
       suite.getSuiteDocument().getSuite().getName() + ".xml" );
     assertTrue( "proposed changes written", proposedFile.exists() );
     IncaDocument changes = rmc.getProposedChanges();
     assertEquals( "correct num of suite changes", 1,
                   changes.getInca().getSuites().sizeOfSuiteArray() );
     Suite suiteChange = changes.getInca().getSuites().getSuiteArray(0);
     assertEquals( "correct num of series changes", 3,
                   suiteChange.getSeriesConfigs().sizeOfSeriesConfigArray() );
     logger.info( "----------------- check proposed -------------------" );


     // remove the first config from suite and then approve the changes
     suite.removeSeriesConfig( 0 );
     SuiteDocument approvedSuite=(SuiteDocument)suite.getSuiteDocument().copy();
     IncaDocument approved = IncaDocument.Factory.newInstance();
     IncaDocument.Inca.Suites approvedSuites =
       approved.addNewInca().addNewSuites();
     approvedSuites.addNewSuite();
     approvedSuites.setSuiteArray(0, suite.getSuiteDocument().getSuite() );
     rmc.approveSuites( approved );
     logger.info( "----------------- approved changes -------------------" );

     changes = rmc.getProposedChanges();
     assertEquals( "correct num of suite changes", 1,
                   changes.getInca().getSuites().sizeOfSuiteArray() );
     suiteChange = changes.getInca().getSuites().getSuiteArray(0);
     assertEquals( "correct num of series changes", 1,
                   suiteChange.getSeriesConfigs().sizeOfSeriesConfigArray() );
     logger.info( "----------------- check proposed again -------------------" );


     depot.waitForReports(2);
     assertTrue( "expected reports found",
                 depot.checkReportsForPatterns( new String[]{"openssl", "user.search"}));
     depot.clearReports();
     logger.info( "------------ received accepted reports -------------------" );


     // check that run nows that are approved are passed thru
     for ( SeriesConfig sc :
           approvedSuite.getSuite().getSeriesConfigs().getSeriesConfigArray() ) {
       sc.getSchedule().unsetCron(); // convert them to run now
     }
     approvedSuite.getSuite().setName(Protocol.IMMEDIATE_SUITE_NAME);
     rmc.addProposedSuite( approvedSuite );
     long startTimestamp = Calendar.getInstance().getTimeInMillis();
     depot.waitForReports(2);
     long timeDiff = Calendar.getInstance().getTimeInMillis() - startTimestamp;
     depot.checkReportsForPatterns( new String[]{ "openssl", "user.search" } );
     assertTrue( "time diff is not a minute", timeDiff < 50000 );
     logger.info( "------------ approved run nows go thru -------------------" );


     // check that run nows that have not yet been approved are not passed thru
     SuiteWrapper unapproved =
       new SuiteWrapper( changes.getInca().getSuites().getSuiteArray(0) );
     unapproved.getSeriesConfig(0).getSchedule().unsetCron();
     unapproved.getSuiteDocument().getSuite().setName
       (Protocol.IMMEDIATE_SUITE_NAME);
     rmc.addProposedSuite( unapproved.getSuiteDocument() );
     Thread.sleep(30000);
     String sched = StringMethods.fileContents(RM_SCHEDULE.getAbsolutePath());
     assertFalse( "schedule doesn't contain gcc", sched.contains("gcc") );
     logger.info( "------------ non-approved thrown away -------------------" );

     // make sure file cleans out
     changes = rmc.getProposedChanges();
     rmc.approveSuites( changes );
     assertFalse( "file cleaned out after none left", proposedFile.exists() );

   }

  /**
   * Test the reporter manager's create and sendSuite functions.
   * The test emulates the real reporter agent's
   * responses to PING, REGISTER and then sends the END statement to
   * shutdown the reporter manager.
   *
   * @throws Exception if trouble executing test
   */
  public void testCreateAndSuite() throws Exception {
    runCreateAndSubscribe( false );
  }

  /**
   * Same as testCreateAndSuite but with authentication turned on
   *
   * @throws Exception if trouble executing test
   */
  public void testCreateAndSuiteAuth() throws Exception {
    runCreateAndSubscribe( true );
  }

  /**
   * Test persistency in reporter manager.
   *
   * @throws Exception if trouble executing test
   */
  public void testDownDepot() throws Exception {
    // execute runcreateand subscribe w/ no depot started up
    startAndRegisterRMC( false, true, null, 1, null );
    rmc.agent.setPingPeriod(20000);
    SuiteWrapper suite = createSampleSuite( null );
    SuiteStagesWrapper ss = new SuiteStagesWrapper( rmc.agent.getResources() );
    ss.modify(suite);
    rmc.agent.getSuites().putSuite(ss);
    assertEquals( "depot dir empty", 0,
                  rmc.getDepotSuiteDirectory().listFiles().length);
    rmc.addSuite( suite.getSuiteDocument() );

    // check changes are sent to RMs -- var/archive
    File archiveDir = new File( TEMP_RM_DIR.getAbsolutePath() + File.separator +
                                "var" + File.separator + "archive" );

    while( ! archiveDir.exists() || archiveDir.listFiles().length < 1 ) {
      logger.debug( "Sleeping 10 seconds for reports to be archived" );
      Thread.sleep(10000);
    }
    for( File reportFile : archiveDir.listFiles() ) {
      String report = StringMethods.fileContents(reportFile.getAbsolutePath());
      logger.info( report );
      assertTrue( "report is correct",
        report.contains("search_output") || report.contains("openssl") ||
          report.contains("gcc"));
    }
    assertEquals( "depot file written", 1,
                  rmc.getDepotSuiteDirectory().listFiles().length);

    // start depot
    AgentTest.MockDepot depot = AgentTest.startDepot( false );
    rmc.agent.setDepotUris( new String[] {depot.getUri()} );
    while( depot.getSuites().length < 1 ) {
      logger.debug( "Sleeping 10 seconds till suite sent to depot" );
      Thread.sleep( 10000);
    }
    assertEquals( "got suite", 1, depot.getSuites().length );

    assertEquals( "depot file cleaned up", 0,
                  rmc.getDepotSuiteDirectory().listFiles().length);

  }
  /**
   * Test persistency in reporter manager.
   *
   * @throws Exception if trouble executing test
   */
  public void testExistingInstall() throws Exception {
    AgentTest.MockDepot depot = runCreateAndSubscribe( false );
    rmc.shutdown();
    startAndRegisterRMC( false, false, null, 1, null );
    receiveStdSuiteReports( depot );
  }

  /**
   * Test the ability to get URIs from a suite
   *
   * @throws Exception  if trouble executing test
   */
  public void testGetUris() throws Exception {

    ReporterManagerController rm = createRM( false, true, null, null );
    SuiteWrapper suite = createSampleSuite( null );

    // make numRepeats duplicates of the suite so that we can check
    // that if a reporter is listed in a suite more than once,
    // extractReportersFromSuite only lists it once
    int numRepeats = 2;
    int numConfigs = suite.getSeriesConfigCount();
    for ( int i = 0; i < numRepeats; i++ ) {
      for ( int j = 0; j < numConfigs; j++ ) {
        suite.appendSeriesConfig((SeriesConfig)suite.getSeriesConfig(j).copy());
      }
    }
    assertEquals(
      "size right",
      numRepeats*numConfigs+numConfigs,
      suite.getSeriesConfigCount()
    );

    PackageType[] reporters = rm.extractReportersFromSuite(
      suite.getSuiteDocument()
    );
    assertEquals( "3 uris extracted", 3, reporters.length );
  }

  /**
   * Test that interrupts at different stages in the run function will
   * cause the run thread to exit appropriately.
   *
   * @throws Exception  if trouble executing test
   */
  public void testInterrupt() throws Exception {
    String resourcesXml =
      "<rc:resourceConfig xmlns:rc=\"http://inca.sdsc.edu/dataModel/resourceConfig_2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
      "  <resources>" +
      "    <resource>\n" +
      "      <name>localhost-interrupt</name>\n" +
      "      <macros>\n" +
      "        <macro>\n" +
      "          <name>__remoteInvocationMethod__</name>\n" +
      "          <value>sleep</value>\n" +
      "        </macro>\n" +
      "        <macro>\n" +
      "          <name>__incaComputeServer__</name>\n" +
      "          <value>localhost</value>\n" +
      "        </macro>\n" +
     "      </macros>\n" +
      "    </resource>" +
      "  </resources>\n" +
      "</rc:resourceConfig>";
    ResourceConfigDocument doc=ResourceConfigDocument.Factory.parse(resourcesXml);
    ResourcesWrapper resources =  new ResourcesWrapper(doc);

    System.setProperty( "inca.interrupt.sleep", "60000" );
    String[] interruptedAt = new String[] {
      "/bin/bash -l -c echo inca ",
      "/bin/bash --login -c echo inca ",
      "^.*buildRM\\.sh.*$",
      "^.*-T -P false $",
      "^.*tail -4.*$"
    };
    for ( int i = 0; i < interruptedAt.length; i++ ) {
      logger.info( "Testing interrupt number " + i );
      System.setProperty( "inca.interrupt.sleepOn", String.valueOf(i) );
      ReporterManagerController rm = createRM
        ( true, false, resources, "localhost-interrupt" );
      ReporterManagerStarter starter = rm.getReporterManagerStarter();
      if ( i > 1 ) {
        starter.setBashLoginOption( "-l" );
      }
      starter.start();
      Thread.sleep(5000);
      starter.interrupt();
      starter.join();
      Sleep interrupt = (Sleep)starter.getProcessHandle();
      assertEquals( "interrupted at " + i, i, interrupt.numSkips );
      assertFalse( "thread exitted at " + i, starter.isAlive() );
      assertTrue( "Interrupted at right command " + i,
        Pattern.matches(interruptedAt[i],interrupt.lastCommand)
      );
    }
  }

  /**
   * Test the reporter manager's isRemoteManagerAlive function
   *
   * @throws Exception if trouble executing test
   */
  public void testIsRemoteManagerAlive() throws Exception {
    startAndRegisterRMC( true, false, null, 1, null );
    rmc.isRemoteManagerAlive();
    assertTrue( "is alive returns true", rmc.isRemoteManagerAlive() );
    rmc.shutdown();
    mockAgent.join();
    assertFalse( "is alive returns false", rmc.isRemoteManagerAlive() );
    rmc = null;
    mockAgent = null;
  }

  /**
   * Test the reporter manager's stage function which installs a reporter
   * manager distribution on a remote resource.  Tested with auth on.
   *
   * @throws Exception  if trouble executing test
   */
  public void testManual() throws Exception {
    // build a RM
    AgentTest.MockDepot depot = AgentTest.startDepot( false );
    startAndRegisterRMC( false, true, null, 3, resource );
    logger.info( "----------------- RM registered -----------------------" );
    rmc.shutdown();
    logger.info( "----------------- RM shutdown -----------------------" );

    rmc = createRM( false, true, null, resource + "-manual" );
    Agent.getGlobalAgent().setPingPeriod( 20000 );
    mockAgent.rmc = rmc;
    Agent.getGlobalAgent().getReporterManagerTable().put( resource + "-manual", rmc );
    String[] cmd = new String[]
      {"-c", rmc.getReporterManagerStarter().getRmCmd() + " -P false" };
    Local proc = new Local( resource + "-manual", rmc.agent.getResources() );
    proc.run("/bin/bash", cmd, "\n", "/tmp");
    logger.info( "----------------- RM started manually --------------------" );

    // add 3 suite changes
    SuiteWrapper suite = createSampleSuite( null );
    for( SeriesConfig sc : suite.getSeriesConfigs() ) {
      sc.setResourceSetName("localhost-manual");
    }
    SuiteStagesWrapper ss = new SuiteStagesWrapper( rmc.agent.getResources() );
    ss.modify(suite);
    rmc.agent.getSuites().putSuite(ss);
    rmc.addSuite( suite.getSuiteDocument() );
    logger.info( "----------------- add suite -------------------" );

    depot.waitForReports(3);
    assertTrue( "both reports found",
      depot.checkReportsForPatterns(new String[]{"openssl", "search", "gcc"}) );

    logger.info( "----------------- kill RM -------------------" );
    this.killReporterManagerProcess();
    Thread.sleep(30000);
    suite.removeSeriesConfig(0);
    ss.modify(suite);
    rmc.agent.getSuites().putSuite(ss);
    rmc.addSuite( suite.getSuiteDocument() );
    logger.info( "----------------- add suite while down -------------------" );

    depot.clearReports();

    proc = new Local( resource + "-manual", rmc.agent.getResources() );
    proc.run("/bin/bash", cmd, "\n", "/tmp");
    logger.info( "----------------- RM restarted manually ------------------" );

    depot.waitForReports(3);
    assertTrue( "both reports found",
      depot.checkReportsForPatterns(new String[]{"openssl", "search", "gcc"}) );

    Agent.getGlobalAgent().setPingPeriod( 120000 );
    Thread.sleep(25000);

    depot.clearReports();
    proc = new Local( resource + "-manual", rmc.agent.getResources() );
    proc.run("/bin/bash", cmd, "\n", "/tmp");
    logger.info( "----------------- RM restarted short ------------------" );
    depot.clearReports();
    depot.waitForReports(1);
    assertTrue( "gcc report found",
      depot.checkReportsForPatterns(new String[]{"gcc"}) );
    logger.info( "----------------- RM test over ------------------" );
  }

  /**
   * Test the reporter manager's addPackage function
   *
   * @throws Exception  if trouble executing test
   */
  public void testPackage() throws Exception {
    startAndRegisterRMC( true , true, null, 1, null );

    Thread.sleep( 5000 );
    assertFalse(
      "Inca::Reporter does not exist",
      rmc.hasPackage( "Inca::Reporter", null)
    );
    rmc.addPackage( "Inca::Reporter" );
    while( ! rmc.hasPackage( "Inca::Reporter",null) ) {
      logger.debug( "package not yet updated" );
      Thread.sleep(2000);
    }
    logger.debug( "package updated" );
  }

  /**
   * Test the ability to parse the rm working dir correctly
   *
   * @throws Exception if trouble executing test
   */
  public void testParseWorkingDir() throws Exception {
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    Local method = new Local( this.resource, resources );

    String homedir = System.getProperty( "user.home" ) + File.separator;
    String[] homeDirChoices = new String[]
      { null, "", "~/incaReporterManager", "incaReporterManager", "~" };
    for ( int i = 0; i < homeDirChoices.length; i++ ) {
      assertEquals
        ( homeDirChoices[i] + " resolves to incaReporterManager",
          homedir+"incaReporterManager",
          ReporterManagerStarter.parseWorkingDir(homeDirChoices[i],method));
    }
    assertEquals
      ( "~/rm" + " resolves to rm",
        homedir+"rm",
        ReporterManagerStarter.parseWorkingDir("~/rm",method));
  }

  /**
   * Test the reporter manager's register function when the reporter manager
   * connects using a password
   *
   * @throws Exception  if trouble executing test
   */
  public void testPasswordRegister() throws Exception {
    startAndRegisterRMC( true, false, null, 1, null );
    Thread.sleep(5000);
  }

  /**
   * Test the specified pattern to make sure it can parse out the string "inca"
   * from a glob of text.
   */
  public void testPattern() {
    String[] strings = {
      "***********************************************************************\n" +
      "* Ceci est le serveur de calcul d'IDRIS. Tout acces au systeme doit   *\n" +
      "* etre specifiquement autorise par le proprietaire du compte. Si vous *\n" +
      "* tentez de continuer a acceder cette machine alors que vous n'y etes *\n" +
      "* pas autorise, vous vous exposez a des poursuites judiciaires.       *\n" +
      "***********************************************************************\n" +
      "*                  CNRS / IDRIS  -   zahir.idris.fr                   *\n" +
      "*                  IBM 120 noeuds (1024 processeurs)                  *\n" +
      "***********************************************************************\n" +
      "inca",
      "inca",
      "inca\n"
    };
    for ( int i = 0; i < strings.length; i++ ) {
      assertTrue( "pattern " + i, Pattern.matches( "(?m)(?s).*inca.*", strings[i]) );
    }
  }

  /**
   * Test the reporter manager's register function
   *
   * @throws Exception if trouble executing test
   */
  public void testRegister() throws Exception {
    startAndRegisterRMC( false, false, null, 1, null );
    Thread.sleep(5000);
  }

  /**
   * Test the reporter manager's restart function.  When manager checks in
   * the first time, we kill it and expect the isReporterManagerAlive function
   * to detect the kill.  The manager is restarted and this time is sent a
   * suite.  During the sending of the suite, we kill the reporter
   * manager again and expect those functions to detect the kill.  When the
   * manager is restarted for the second time, it should pick up a suite
   * and execute it. It will start up 3 reporters which we wait for.  Then we
   * pick the manager 3 times and then properly shut it down.
   *
   * @throws Exception  if trouble executing test
   */
  public void testRestart() throws Exception {
    // (1) reporter manager should be killed after 5 seconds and detected from
    // ping and restarted
    startAndRegisterRMC( false, true, null, 3, null );
    RepositoryCache cache = rmc.agent.getRepositoryCache();
    rmc.agent.setPingPeriod( 10000 );
    logger.info( "received first connection -- initial start " );
    logger.info( "-------------------------------------------------------" );
    if ( ! rmc.isRunning() ) {
      rmc.getReporterManagerStarter().waitForReporterManager();
    }
    logger.debug( "RM checked in" );

    // reporter manager should be killed after 5 seconds and detected from ping
    Thread.sleep(5000);
    mockAgent.writer.close(); // kill rm
    assertTrue( "registration returned -- detected shutdown", true );

    AgentTest.MockDepot depot = AgentTest.startDepot( false );
    rmc.agent.setDepotUris( new String[] { depot.getUri() });
    // (2) kill reporter manager but detected when trying to send suite
    while( mockAgent.numConnectionsReceived < 2 ) Thread.sleep( 2000 );
    logger.info( "received first connection -- 1st restart " );
    logger.info( "-------------------------------------------------------" );
    Thread.sleep(5000);
    SuiteWrapper suiteWrapper = createSampleSuite( null );
    SuiteStagesWrapper suite = new SuiteStagesWrapper(
      "var/suites/TestSuiteLocal.xml",
      ResourcesWrapperTest.createSampleResources()
    );
    cache.resolveReporters( suiteWrapper );
    suite.modify( suiteWrapper );
    suite.save();
    rmc.agent.setSuites( "var" );
    rmc.addSuite( suiteWrapper.getSuiteDocument() );
    // wait long enough for something to get sent and then kill
    File indicatorFile = new File
      ( rmc.getReporterManagerStarter().getRmRootPath() +
        "/var/reporter-packages/lib/perl/Inca/Reporter.pm" );
    while ( ! indicatorFile.exists() ) {
      logger.debug( "Waiting half a second for makedist to be installed" );
      Thread.sleep( 500 );
    }
    logger.debug( "Mock agent closing connection to RM" );

    assertTrue( "RM dir exists", TEMP_RM_DIR.exists() );
    killReporterManagerProcess();
    logger.debug( "closed connection to RM" );
    depot.clearReports();

    while( mockAgent.numConnectionsReceived < 3 ) Thread.sleep( 2000 );
    logger.info( "received second connection -- 2nd restart" );
    logger.info( "-------------------------------------------------------" );
    for( int i = 0; i < 100 & depot.getReports().length < 3; i++ ) {
      logger.debug( "Sleeping 10 seconds" );
      Thread.sleep( 10000 );
    }
    logger.info( "Waiting for reports" );
    logger.info( "Reports received" );
    rmc.agent.setPingPeriod( 4000 ); // at least 3 pings
    Thread.sleep( 15000 );
  }

  /**
   * Test the ability to send reporters from a suite to a RM
   *
   * @throws Exception   if trouble executing test
   */
  public void testSendReporters() throws Exception {
    startAndRegisterRMC( false, true, null, 1, null );

    SuiteWrapper suite = createSampleSuite( null );

    try {
      PackageType[] reps = rmc.extractReportersFromSuite(
        suite.getSuiteDocument()
      );
      logger.info( "Sending " + reps.length + " reporters" );
      assertEquals( "3 reporters extracted", 3, reps.length );
      rmc.sendReporters( reps );
      assertTrue(
        "openssl in rm cache",
        rmc.hasPackage("cluster.security.openssl.version", null) );
      assertTrue(
        "gcc in rm cache",
        rmc.hasPackage("cluster.compiler.gcc.version", null)
      );
      reps = rmc.extractReportersFromSuite( suite.getSuiteDocument() );
      assertEquals( "sending suite again, no uris", 0, reps.length );
      ReporterManagerController rm2 = createRM( false, false, null, null );
      rm2.agent.setRepositoryCache( rmc.agent.getRepositoryCache() );
      assertTrue(
        "openssl in rm cache",
        rm2.hasPackage("cluster.security.openssl.version", null) );
      assertTrue(
        "gcc in rm cache",
        rm2.hasPackage("cluster.compiler.gcc.version", null)
      );
    } catch( Exception e ) {
      fail( "Sending suite failed: " + e );
    }
  }

  /**
   * Test the reporter manager's stage function which installs a reporter
   * manager distribution on a remote resource.  Tested with auth on.
   *
   * @throws Exception  if trouble executing test
   */
  public void testStage() throws Exception {
    /*
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    ReporterManagerController rm = createRM( true, false, null, null );
    File installDir = new File(
      resources.getValue( resource, Protocol.WORKING_DIR_MACRO)
    );


    // first test if credentials with a passphrase work
    StringMethods.deleteDirectory(installDir);
    ReporterManagerStarter starter = rm.getReporterManagerStarter();
    starter.findBashLoginShellOption();
    assertFalse( "isStaged returns false", starter.isStaged());
    starter.stage( false );
    assertTrue( "isStaged returns true for cred+pass", starter.isStaged());

    // now just test credentials with no passphrase
    StringMethods.deleteDirectory(installDir);
    rm = createRM( true, false, null, null );
    rm.agent.setPassword( "" );
    starter = rm.getReporterManagerStarter();
    starter.findBashLoginShellOption();
    assertFalse( "isStaged returns false", starter.isStaged());
    starter.stage( false );
    assertTrue( "isStaged returns true for cred", starter.isStaged());

    // and finally no credentials
    StringMethods.deleteDirectory(installDir);
    rm = createRM( true, false, null, null );
    rm.agent.setPassword( "" );
    rm.agent.setAuthenticate( false );
    starter = rm.getReporterManagerStarter();
    starter.findBashLoginShellOption();
    assertFalse( "isStaged returns false", starter.isStaged());
    starter.stage( false );
    assertTrue( "isStaged returns true", starter.isStaged());  */

  }

  /**
   * Test the reporter manager's start function.  Given a resource with 2
   * bad hosts and 1 good one
   *
   * @throws Exception if trouble executing test
   */
  public void testStart() throws Exception {
    String resources =
      "<?xml version = \"1.0\"?>\n" +
      "<!--Generated by XML Authority.-->\n" +
      "<rc:resourceConfig xmlns:rc = \"http://inca.sdsc.edu/dataModel/resourceConfig_2.0\" xmlns:xsi = \"http://www.w3.org/2001/XMLSchema-instance\" >\n" +
      "<resources>\n" +
      "  <resource>\n" +
      "    <name>localhost</name>\n" +
      "    <xpath>//resource[matches(name, 'localhost.+')]</xpath>\n" +
      "    <macros>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.COMPUTE_METHOD_MACRO + "</name>\n" +
      "        <value>local</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.COMPUTE_SERVER_MACRO + "</name>\n" +
      "        <value>localhost</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.WORKING_DIR_MACRO + "</name>\n" +
      "        <value>/tmp/inca2install2</value>\n" +
      "      </macro>\n" +
      "    </macros>\n" +
      "  </resource>\n" +
      "  <resource>\n" +
      "    <name>localhostBad1</name>\n" +
      "    <macros>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.WORKING_DIR_MACRO + "</name>\n" +
      "        <value>/var/inca2install</value>\n" +
      "      </macro>\n" +
      "    </macros>\n" +
      "  </resource>\n" +
      "  <resource>\n" +
      "    <name>localhostBad2</name>\n" +
      "    <macros>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.WORKING_DIR_MACRO + "</name>\n" +
      "        <value>/var/inca2install2</value>\n" +
      "      </macro>\n" +
      "    </macros>\n" +
      "  </resource>\n" +
      "  <resource>\n" +
      "    <name>localhostGood</name>\n" +
      "  </resource>\n" +
      "</resources>\n" +
      "</rc:resourceConfig>";
    ResourceConfigDocument rcDoc = ResourceConfigDocument.Factory.parse(
      resources
    );
    startAndRegisterRMC( false, false, new ResourcesWrapper(rcDoc), 1, null );
    assertEquals("got right rm dir", "/tmp/inca2install2", rmc.getReporterManagerStarter().getRmRootPath());
    assertEquals("got right host", "localhostGood", rmc.getReporterManagerStarter().getCurrentHost());
    Thread.sleep(10000);
  }

  /**
   * Test the reporter manager's start function.  Given a resource which doesn't
   * work.  We then change the resource to work 10 seconds and expect start to
   * pick it up after it's next iteration.
   *
   * @throws Exception  if trouble executing test
   */
  public void testStartOnSecondTime() throws Exception {
    String resourcesBad =
      "<?xml version = \"1.0\"?>\n" +
      "<!--Generated by XML Authority.-->\n" +
      "<rc:resourceConfig xmlns:rc = \"http://inca.sdsc.edu/dataModel/resourceConfig_2.0\" xmlns:xsi = \"http://www.w3.org/2001/XMLSchema-instance\" >\n" +
      "<resources>\n" +
      "  <resource>\n" +
      "    <name>localhost</name>\n" +
      "    <macros>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.COMPUTE_METHOD_MACRO + "</name>\n" +
      "        <value>local</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.COMPUTE_SERVER_MACRO + "</name>\n" +
      "        <value>localhost</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.WORKING_DIR_MACRO + "</name>\n" +
      "        <value>/var/inca2install2</value>\n" +
      "      </macro>\n" +
      "    </macros>\n" +
      "  </resource>\n" +
      "</resources>\n" +
      "</rc:resourceConfig>";

    String resourcesGood =
      "<?xml version = \"1.0\"?>\n" +
      "<!--Generated by XML Authority.-->\n" +
      "<rc:resourceConfig xmlns:rc = \"http://inca.sdsc.edu/dataModel/resourceConfig_2.0\" xmlns:xsi = \"http://www.w3.org/2001/XMLSchema-instance\" >\n" +
      "<resources>\n" +
      "  <resource>\n" +
      "    <name>localhost</name>\n" +
      "    <macros>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.COMPUTE_METHOD_MACRO + "</name>\n" +
      "        <value>local</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.COMPUTE_SERVER_MACRO + "</name>\n" +
      "        <value>localhost</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.WORKING_DIR_MACRO + "</name>\n" +
      "        <value>/tmp/inca2install2</value>\n" +
      "      </macro>\n" +
      "    </macros>\n" +
      "  </resource>\n" +
      "</resources>\n" +
      "</rc:resourceConfig>";
    ResourceConfigDocument rcDoc = ResourceConfigDocument.Factory.parse(
      resourcesBad
    );
    startAndRegisterRMC( false, false, new ResourcesWrapper(rcDoc), 1, null );
    Thread.sleep(10000);
    logger.info( "Setting new resource document" );
    rcDoc = ResourceConfigDocument.Factory.parse( resourcesGood );
    rmc.agent.setResources( new ResourcesWrapper(rcDoc) );
    rmc.getReporterManagerStarter().refreshHosts();
    logger.debug( "Refreshed hosts");
    Thread.sleep(10000);
    while( ! rmc.isRunning() ) {
      logger.debug( "Waiting 5 seconds for RM to check in" );
      Thread.sleep( 5000 );
    }
    logger.debug( "reporter manager checked in" );
    assertEquals("got right rm dir", "/tmp/inca2install2", rmc.getReporterManagerStarter().getRmRootPath());
    assertEquals("got right host", "localhost", rmc.getReporterManagerStarter().getCurrentHost());
  }

  /**
   * Test the reporter manager's start function.  Given a resource that doesn't
   * start the first time, and then the user realizes it's the wrong resources
   * and decides to delete it.  This thread should exit accordingly.
   *
   * @throws Exception  if trouble executing test
   */
  public void testStartStops() throws Exception {
    String noResources =
      "<?xml version = \"1.0\"?>\n" +
      "<!--Generated by XML Authority.-->\n" +
      "<rc:resourceConfig xmlns:rc = \"http://inca.sdsc.edu/dataModel/resourceConfig_2.0\" xmlns:xsi = \"http://www.w3.org/2001/XMLSchema-instance\" >\n" +
      "<resources>\n" +
      "</resources>\n" +
      "</rc:resourceConfig>";

    String resourcesBad =
      "<?xml version = \"1.0\"?>\n" +
      "<!--Generated by XML Authority.-->\n" +
      "<rc:resourceConfig xmlns:rc = \"http://inca.sdsc.edu/dataModel/resourceConfig_2.0\" xmlns:xsi = \"http://www.w3.org/2001/XMLSchema-instance\" >\n" +
      "<resources>\n" +
      "  <resource>\n" +
      "    <name>localhost</name>\n" +
      "    <macros>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.COMPUTE_METHOD_MACRO + "</name>\n" +
      "        <value>local</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.COMPUTE_SERVER_MACRO + "</name>\n" +
      "        <value>localhost</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.WORKING_DIR_MACRO + "</name>\n" +
      "        <value>/tmp/inca2install2</value>\n" +
      "      </macro>\n" +
      "    </macros>\n" +
      "  </resource>\n" +
      "</resources>\n" +
      "</rc:resourceConfig>";
    ResourceConfigDocument rcDoc = ResourceConfigDocument.Factory.parse(
      resourcesBad
    );
    startAndRegisterRMC( false, false, new ResourcesWrapper(rcDoc), 1, null );
    Thread.sleep(10000);
    logger.info( "Setting new resource document" );
    rcDoc = ResourceConfigDocument.Factory.parse( noResources );
    rmc.agent.setResources( new ResourcesWrapper(rcDoc) );
    rmc.getReporterManagerStarter().refreshHosts();
    Thread.sleep(10000);
  }

  /**
   * Test that suite changes are persisted if unable to send to manager and
   * agent is restarted
   *
   * @throws Exception if trouble executing test
   */
  public void testUnsentSuites() throws Exception {
    AgentTest.MockDepot depot = AgentTest.startDepot( false );
    logger.info( "Starting Mock depot at " + depot.getUri() );

    // RM launched and registers to agent
    startAndRegisterRMC( false, true, null, 1, null );
    rmc.shutdown();
    mockAgent.join();
    logger.info( "Shutting down manager" );
    logger.info( "-------------------------------------------------------" );

    // create suite files
    SuiteWrapper suite = createSampleSuite( null );
    SuiteStagesWrapper ss = new SuiteStagesWrapper( rmc.agent.getResources() );
    ss.modify(suite);
    SuiteWrapper suite2 =
      new SuiteWrapper( (SuiteDocument)suite.getSuiteDocument().copy() );
    for ( SeriesConfig sc : suite2.getSeriesConfigs() ) {
      sc.setNickname( sc.getNickname() + "2" );
    }

    // add to RM even though it's not connected to the agent now
    rmc.extractReportersFromSuite( suite.getSuiteDocument() );
    rmc.agent.getSuites().putSuite(ss);
    rmc.addSuite( suite.getSuiteDocument() );
    rmc.addSuite( suite2.getSuiteDocument() );
    Suite suiteObj = suite.getSuiteDocument().getSuite();
    assertTrue( "suite name changed", suiteObj.getName().contains("@") );
    assertEquals( "2 suites written", 2,
                  rmc.getApprovedSuiteDirectory().listFiles().length );
    File backupSuite = new File
      ( rmc.getApprovedSuiteDirectory().getAbsolutePath() + File.separator +
        suiteObj.getName() + "-" + suiteObj.getVersion() + ".xml" );
    assertTrue( "backup suite file written", backupSuite.exists() );
    logger.info( "------------------ Added suite ----------------------" );

    // make the manager think it's an existing install
    File varDir = new File( rmc.getReporterManagerStarter().getRmRootPath() + File.separator + "var" );
    IncaDocument emptySched = IncaDocument.Factory.newInstance();
    varDir.mkdirs();
    File scheduleFile = new File
      ( varDir.getAbsolutePath() + File.separator + "schedule.xml" );
    emptySched.save( scheduleFile );
    logger.debug("Wrote new schedule file: " + scheduleFile.getAbsolutePath());
    Thread.sleep(10000);

    // restart reporter manager
    logger.info( "------------ Restarting manager -----------------------" );
    startAndRegisterRMC( false, true, null, 1, null );
    rmc.agent.getSuites().putSuite(ss);

    logger.info( "------------ Waiting for reports -----------------------" );
    depot.waitForReports(6);
    receiveStdSuiteReports( depot );
  }

  public void testUpdateConfig() throws Exception {

    ReporterManagerController rm = createRM( false, false, null, null );
    Agent.getGlobalAgent().getReporterManagerTable().put( "localhost", rm );
    String resources =
      "<?xml version = \"1.0\"?>\n" +
      "<rc:resourceConfig xmlns:rc = \"http://inca.sdsc.edu/dataModel/resourceConfig_2.0\" xmlns:xsi = \"http://www.w3.org/2001/XMLSchema-instance\" >\n" +
      "<resources>\n" +
      "  <resource>\n" +
      "    <name>localhost</name>\n" +
      "    <macros>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.COMPUTE_METHOD_MACRO + "</name>\n" +
      "        <value>local</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.COMPUTE_SERVER_MACRO + "</name>\n" +
      "        <value>localhost</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>" + Protocol.WORKING_DIR_MACRO + "</name>\n" +
      "        <value>/tmp/inca2install2</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>verbose</name>\n" +
      "        <value>1</value>\n" +
      "      </macro>\n" +
      "    </macros>\n" +
      "  </resource>\n" +
      "</resources>\n" +
      "</rc:resourceConfig>";

    ResourceConfigDocument r = ResourceConfigDocument.Factory.parse(resources);
    Agent.getGlobalAgent().updateResources( new ResourcesWrapper(r) );
    assertFalse( "approval not set", rm.hasApprovalEmail() );
    assertFalse( "proxy not set", rm.hasProxy() );

    r = ResourceConfigDocument.Factory.parse(resources);
    Macro m = r.getResourceConfig().getResources().getResourceArray(0).getMacros().addNewMacro();
    m.setName( Protocol.EMAIL_MACRO );
    m.addValue( "ssmallen@sdsc.edu" );
    Agent.getGlobalAgent().updateResources( new ResourcesWrapper(r) );
    assertTrue( "approval set", rm.hasApprovalEmail() );

    r = ResourceConfigDocument.Factory.parse(resources);
    m = r.getResourceConfig().getResources().getResourceArray(0).getMacros().addNewMacro();
    m.setName( Protocol.EMAIL_MACRO );
    m.addValue( "inca@sdsc.edu" );
    Agent.getGlobalAgent().updateResources( new ResourcesWrapper(r) );
    assertEquals("approval email reset", "inca@sdsc.edu",rm.getApprovalEmail());

    r = ResourceConfigDocument.Factory.parse(resources);
    Agent.getGlobalAgent().updateResources( new ResourcesWrapper(r) );
    assertFalse( "approval unset", rm.hasApprovalEmail() );

    ReporterManagerProxy p =
      new ReporterManagerProxy( "host", 222, "username", "password", "dn", 0 );
    rm.setProxy(p);
    assertTrue( "proxy  set", rm.hasProxy() );
    r.getResourceConfig().getResources().addNewResource();
    r = ResourceConfigDocument.Factory.parse(resources);
    Agent.getGlobalAgent().updateResources( new ResourcesWrapper(r) );
    assertFalse( "proxy unset", rm.hasProxy() );

    r = ResourceConfigDocument.Factory.parse(resources);
    Macros macros = r.getResourceConfig().getResources().getResourceArray(0).getMacros();
    m = macros.addNewMacro();
    m.setName( Protocol.MYPROXY_HOST_MACRO );
    m.addValue( "myhost" );
    m = macros.addNewMacro();
    m.setName( Protocol.MYPROXY_USERNAME_MACRO );
    m.addValue( "myuser" );
    m = macros.addNewMacro();
    m.setName( Protocol.MYPROXY_PASSWORD_MACRO );
    m.addValue( "pass" );
    Agent.getGlobalAgent().updateResources( new ResourcesWrapper(r) );
    assertTrue( "proxy set", rm.hasProxy() );

    r = (ResourceConfigDocument)r.copy();
    macros = r.getResourceConfig().getResources().getResourceArray(0).getMacros();
    m = macros.getMacroArray(5);
    m.setValueArray(0, "change" );
    logger.info( r.xmlText() );
    Agent.getGlobalAgent().updateResources( new ResourcesWrapper(r) );
    assertEquals( "proxy set", "change", rm.getProxy().getUsername() );

    r = (ResourceConfigDocument)r.copy();
    macros = r.getResourceConfig().getResources().getResourceArray(0).getMacros();
    macros.removeMacro(5);
    Agent.getGlobalAgent().updateResources( new ResourcesWrapper(r) );
    assertFalse( "proxy unset", rm.hasProxy() );
  }

  /**
   * Test the reporter manager's stage function which installs a reporter
   * manager distribution on a remote resource.  Tested with auth on.
   *
   * @throws Exception  if trouble executing test
   */
  public void testUpgrade() throws Exception {
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    File installDir = new File(
      resources.getValue( resource, Protocol.WORKING_DIR_MACRO)
    );


    // first test it errors
    Agent agent = new Agent();
    agent.setConfiguration( config );
    agent.setResources( resources );
    agent.readCredentials();
    agent.setPassword( testPassword );
    ReporterManagerStarter rm = new ReporterManagerStarter(
      "localhost-noinstallation", agent
    );
    StringMethods.deleteDirectory(installDir);

    try {
      rm.stage( "install" );
      fail( "Upgrade when no previous installation should fail" );
    } catch ( Exception e ) {     }

    // now expect it to succeed
    rm = new ReporterManagerStarter( "localhost", agent );

    rm.findBashLoginShellOption();

    rm.stage( null );  // just in case it's not there
    File keyFile = new File(
      installDir.getAbsolutePath() + File.separator + "etc" + File.separator +
      "rmkey.pem"
    );
    assertTrue( "rm key exists", keyFile.exists() );
    long beforeUpdateTimestamp = keyFile.lastModified();
    assertTrue( "isStaged returns true", rm.isStaged());
    rm.stage( "install" );
    assertTrue( "isStaged returns true", rm.isStaged());
    Thread.sleep(2);
    logger.info("Stamps " + keyFile.lastModified() + " > " + beforeUpdateTimestamp );
    assertTrue( "rm key modified",
                keyFile.lastModified() > beforeUpdateTimestamp  );

    // test lib upgrades
    File lib = new File(
      installDir.getAbsolutePath() + File.separator + "lib" + File.separator +
      "perl" + File.separator + "Inca" + File.separator + "GridProxy.pm"
    );
    lib.delete();
    rm.stage( "install XML-Simple-2.14" );
    assertTrue( "isStaged returns true", rm.isStaged());
    assertTrue( "lib reinstalled", lib.exists() );
  }

  /**
   *
   * @param data
   * @return
   * @throws IOException
   */
  private static Object readPEMData(String data) throws IOException
  {
    PEMParser parser = new PEMParser(new StringReader(data));

    try {
      return parser.readObject();
    }
    finally {
      parser.close();
    }
  }

  /**
   *
   * @param data
   * @return
   * @throws IOException
   * @throws CertificateException
   */
  private static X509Certificate readCertData(String data) throws IOException, CertificateException
  {
    X509CertificateHolder certHolder = (X509CertificateHolder)readPEMData(data);
    JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();

    certConverter.setProvider("BC");

    return certConverter.getCertificate(certHolder);
  }

  /**
   *
   * @param data
   * @param password
   * @return
   * @throws IOException
   * @throws KeyStoreException
   * @throws OperatorCreationException
   * @throws PKCSException
   */
  private static PrivateKey readKeyData(String data, final String password) throws IOException, KeyStoreException, OperatorCreationException, PKCSException
  {
    Object pemObject = readPEMData(data);
    PrivateKeyInfo keyInfo;

    if (pemObject instanceof PEMKeyPair)
      keyInfo = ((PEMKeyPair)pemObject).getPrivateKeyInfo();
    else if (pemObject instanceof PEMEncryptedKeyPair) {
      PEMEncryptedKeyPair encryptedKey = (PEMEncryptedKeyPair)pemObject;
      JcePEMDecryptorProviderBuilder decryptBuilder = new JcePEMDecryptorProviderBuilder();

      decryptBuilder.setProvider("BC");

      PEMDecryptorProvider decryptor = decryptBuilder.build(password.toCharArray());
      PEMKeyPair pemKey = encryptedKey.decryptKeyPair(decryptor);

      keyInfo = pemKey.getPrivateKeyInfo();
    }
    else if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo) {
      PKCS8EncryptedPrivateKeyInfo encryptedInfo = (PKCS8EncryptedPrivateKeyInfo)pemObject;
      JceOpenSSLPKCS8DecryptorProviderBuilder decryptBuilder = new JceOpenSSLPKCS8DecryptorProviderBuilder();

      decryptBuilder.setProvider("BC");

      InputDecryptorProvider decryptor = decryptBuilder.build(password.toCharArray());

      keyInfo = encryptedInfo.decryptPrivateKeyInfo(decryptor);
    }
    else
      throw new KeyStoreException(data + " does not contain a key pair");

    JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();

    keyConverter.setProvider("BC");

    return keyConverter.getPrivateKey(keyInfo);
  }
}
