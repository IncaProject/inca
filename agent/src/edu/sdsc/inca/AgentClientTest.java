package edu.sdsc.inca;

import junit.framework.TestCase;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import edu.sdsc.inca.dataModel.suite.Suite;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.dataModel.inca.IncaDocument;
import edu.sdsc.inca.dataModel.resourceConfig.ResourceConfigDocument;
import edu.sdsc.inca.dataModel.util.Macro;
import edu.sdsc.inca.dataModel.util.Resource;
import edu.sdsc.inca.dataModel.util.Resources;
import edu.sdsc.inca.agent.ReporterManagerControllerTest;
import edu.sdsc.inca.agent.ReporterManagerController;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.util.ConfigProperties;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.util.ResourcesWrapperTest;
import edu.sdsc.inca.util.Constants;
import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.util.XmlWrapper;
import edu.sdsc.inca.util.CrypterException;
import edu.sdsc.inca.repository.Repositories;
import edu.sdsc.inca.repository.RepositoriesTest;
import edu.sdsc.inca.repository.Repository;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

/**
 * A unit test driver for the Agent and AgentClient classes.
 */
public class AgentClientTest extends TestCase {

  private static final String CWD = System.getProperty("user.dir");
  private static final String SEP = File.separator;
  private static final String REPOSITORY_PATH1 = CWD + SEP + "rep1";
  private static final String REPOSITORY_PATH2 = CWD + SEP + "rep2";
  private static final String REPOSITORY_URL1 = "file://" + REPOSITORY_PATH1;
  private static final String REPOSITORY_URL2 = "file://" + REPOSITORY_PATH2;
  static private Logger logger = Logger.getLogger( AgentClientTest.class );

  private AgentClient client = null;
  private Agent server = null;
  private AgentTest.MockDepot depot = null;
  private static int port = 3456;
  private static Repositories sampleRepository = null;
  private static ResourcesWrapper sampleResources = null;

  private static final String[] REPORTER_FILENAMES = {
    "cluster.os.kernel.version.rpt",
    "cluster.filesystem.gpfs.version.rpt",
    "cluster.java.sun.version.rpt",
    "grid.security.gsi.version.rpt",
    "cluster.compiler.gcc.version",
    "Reporter.pm",
    "cluster.security.openssl.version"
  };
  private static final String[] REPORTER_ATTRS = {
    "name", "kernel", "version", "1.7", "file", REPORTER_FILENAMES[0], "dependencies", "",
    "name", "gpfs", "version", "1.2", "file", REPORTER_FILENAMES[1], "dependencies", "",
    "name", "java", "version", "1.5", "file", REPORTER_FILENAMES[2],"dependencies", "",
    "name", "gsi", "version", "0.1", "file", REPORTER_FILENAMES[3], "dependencies", "",
    "name", "gcc", "version", "1.5", "file", REPORTER_FILENAMES[4], "dependencies", "Test::Inca::Reporter",
    "name", "Test::Inca::Reporter", "version", "1.0", "file", REPORTER_FILENAMES[5], "dependencies", "",
    "name", "openssl", "version", "1.3", "file", REPORTER_FILENAMES[6], "dependencies", ""
  };
  public static final int ATTRIBUTE_COUNT = 4;
  public static final String[] REPORTER_BODIES = {

    "#!/usr/bin/perl\n" +
    "use strict;\n" +
    "use warnings;\n" +
    "use Inca::Reporter::Version;\n" +
    "my $reporter = new Inca::Reporter::Version(\n" +
    "  version => 1.7,\n" +
    "  description => 'Reports the os kernel release info',\n" +
    "  url => 'http://www.linux.org',\n" +
    "  package_name => 'kernel'\n" +
    ");\n" +
    "$reporter->processArgv(@ARGV);\n" +
    "$reporter->setVersionByExecutable('uname -r');\n" +
    "$reporter->print();\n",

    "#!/usr/bin/perl\n" +
    "use strict;\n" +
    "use warnings;\n" +
    "use Inca::Reporter::Version;\n" +
    "my $reporter = new Inca::Reporter::Version(\n" +
    "  version => 1.2,\n" +
    "  description => 'Reports the version of GPFS',\n" +
    "  url => 'http://www.almaden.ibm.com/cs/gpfs.html',\n" +
    "  package_name => 'gpfs'\n" +
    ");\n" +
    "$reporter->processArgv(@ARGV);\n" +
    "$reporter->setVersionByRpmQuery('gpfs');\n" +
    "$reporter->print();\n",

    "#!/usr/bin/perl\n" +
    "use strict;\n" +
    "use warnings;\n" +
    "use Inca::Reporter::Version;\n" +
    "my $reporter = new Inca::Reporter::Version(\n" +
    "  version => 1.5,\n" +
    "  description => 'Reports the version of java',\n" +
    "  url => 'http://java.sun.com',\n" +
    "  package_name => 'java'\n" +
    ");\n" +
    "$reporter->processArgv(@ARGV);\n" +
    "$reporter->setVersionByExecutable('java -version', 'java version \"(.+)\"');\n" +
    "$reporter->print();\n",

    "#!/usr/bin/perl\n" +
    "use strict;\n" +
    "use warnings;\n" +
    "use Inca::Reporter::Version;\n" +
    "my $reporter = new Inca::Reporter::Version(\n" +
    "  version => 0.1,\n" +
    "  description => 'Reports the version of GSI client tools',\n" +
    "  url => 'http://www.globus.org/gsi',\n" +
    "  package_name => 'gsi'\n" +
    ");\n" +
    "$reporter->processArgv(@ARGV);\n" +
    "$reporter->setVersionByGptQuery('globus_gsi_cert_utils');\n" +
    "$reporter->print();\n",

    "#!/usr/bin/perl\n" +
    "\n" +
    "use strict;\n" +
    "use warnings;\n" +
    "use Inca::Reporter::Version;\n" +
    "\n" +
    "my $reporter = new Inca::Reporter::Version(\n" +
    "  version => 1.5,\n" +
    "  description => 'Reports the version of gcc',\n" +
    "  url => 'http://gcc.gnu.org',\n" +
    "  package_name => 'gcc'\n" +
    ");\n" +
    "$reporter->processArgv(@ARGV);\n" +
    "\n" +
    "$reporter->setVersionByExecutable('gcc -dumpversion');\n" +
    "$reporter->print();",

    "package Inca::Reporter;" + "\n",
    "# empty",

    "#!/usr/bin/perl\n" +
    "\n" +
    "use strict;\n" +
    "use warnings;\n" +
    "use Inca::Reporter::Version;\n" +
    "\n" +
    "my $reporter = new Inca::Reporter::Version(\n" +
    "  version => 1.3,\n" +
    "  description => 'Reports the version of openssl',\n" +
    "  url => 'http://www.openssl.org',\n" +
    "  package_name => 'openssl'\n" +
    ");\n" +
    "$reporter->processArgv(@ARGV);\n" +
    "\n" +
    "$reporter->setVersionByExecutable('echo version | openssl', 'OpenSSL (\\S+)');\n" +
    "$reporter->print();"

  };

  /**
    * Generate a sample reporter repository using the global reporters in this
    * file.
    *
    * @param path    The destination of the reporter repository.
    *
    * @param first   The start index of the reporters to include in the
    * repository
    *
    * @param last    The end index of the reporters to include in the repository
    *
    * @throws IOException  if problem creating repository
    */
   public static void makeRepository(String path, int first, int last)
     throws IOException {
     new File(path).mkdir();
     String catalogPath = path + SEP + "Packages.gz";
     OutputStreamWriter output = new OutputStreamWriter(
       new GZIPOutputStream(new FileOutputStream(catalogPath))
     );
     for(int i = first, offset = first * ATTRIBUTE_COUNT * 2; i <= last; i++) {
       for(int j = 0; j < ATTRIBUTE_COUNT; j++) {
         String attribute = REPORTER_ATTRS[offset++];
         String value = REPORTER_ATTRS[offset++];
         output.write(attribute + ": " + value + "\n");
       }
       OutputStreamWriter fout = new OutputStreamWriter(
         new FileOutputStream(path + SEP + REPORTER_FILENAMES[i])
       );
       fout.write(REPORTER_BODIES[i]);
       fout.close();
       output.write("\n");
     }
     output.close();
   }

  /**
   * Start up an Inca agent server and a client for testing
   *
   * @throws Exception
   */
  public void setUp() throws Exception {
    StringMethods.deleteDirectory(new File("var"));
    StringMethods.deleteDirectory(new File("/tmp/inca2install2"));
    makeRepository(REPOSITORY_PATH1, 0, 2);
    makeRepository(REPOSITORY_PATH2, 2, 3);
    depot = AgentTest.startDepot(true);
    server = startAgent(true);
    server.getRepositoryCache().setUpdatePeriod(
      30 * Constants.MILLIS_TO_SECOND
    );
    assertFalse( "agent shutdown correct", server.ranShutdown );
    client = connectAgentClient
      ("localhost",server.getPort(), AgentTest.INCAT_CERT, AgentTest.INCAT_KEY);
  }

  /**
   * Shutdown the global client and agent server.
   *
   * @throws Exception
   */
  public void tearDown() throws Exception {
    client.close();
    AgentTest.stopAgent();
    AgentTest.stopDepot();
    StringMethods.deleteDirectory(new File(REPOSITORY_PATH1));
    StringMethods.deleteDirectory(new File(REPOSITORY_PATH2));
    StringMethods.deleteDirectory(new File("var"));
    StringMethods.deleteDirectory(new File("/tmp/inca2install2"));
  }


  /**
   * Tests whether the agent can send a catalog w/no reporters.
   *
   * @throws Exception if problem running test
   */
  public void testCatalogEmpty() throws Exception {
    Properties[] props = client.getCatalog(null);
    if(props.length != 0) {
      fail("Non-empty catalog with " + props.length + " entries");
    }
    String cat = client.getCatalogAsXml(null);
    cat = cat.replaceAll("\\s", "");
    assertEquals("<catalog></catalog>", cat);
  }

  /**
   * Tests whether the agent can sent a catalog from a single repository.
   */
  public void testCatalogSingle() throws Exception {
    setConfig(new String[] {REPOSITORY_URL1}, null, null);
    Properties[] props = client.getCatalog(null);
    assertEquals(3, props.length);
    String cat = client.getCatalogAsXml(null).replaceAll("\\s", "");
    int endIndex = 3 * ATTRIBUTE_COUNT * 2;
    for(int i = 0; i < props.length; i++) {
      Enumeration e = props[i].propertyNames();
      while(e.hasMoreElements()) {
        String attr = (String)e.nextElement();
        String value = props[i].getProperty(attr);
        String xml = "<name>" + attr + "</name><value>" + value + "</value>";
        assertTrue(cat.indexOf(xml) >= 0);
        int j;
        for(j = 0;
            j < endIndex &&
            (!attr.equals(REPORTER_ATTRS[j]) ||
             !value.equals(REPORTER_ATTRS[j + 1]));
            j += 2) {
          // empty
        }
        if(j >= endIndex) {
          fail("Spurious attribute '" + attr + "=" + value + "'");
        }
      }
    }
  }

  /**
   * Tests whether the agent refreshes its repository cache when asked for its
   * combined catalog.
   */
  public void testCatalogRefresh() throws Exception {
    setConfig(new String[] {REPOSITORY_URL1, REPOSITORY_URL2}, null, null);
    Properties[] props = client.getCatalog(null);
    for(int i = 0; i < props.length; i++) {
      logger.info("Reporter #" + i + " has name " + props[i].getProperty("name"));
    }
    assertEquals(5, props.length);
    makeRepository(REPOSITORY_PATH1, 0, 2);
    makeRepository(REPOSITORY_PATH2, 2, 4);
    props = client.getCatalog(null);
    for(int i = 0; i < props.length; i++) {
      logger.info("Reporter #" + i + " has name " + props[i].getProperty("name"));
    }
    assertEquals(6, props.length);
  }

  /**
   * Test agent resource transmission.
   *
   * @throws Exception if problem running test
   */
   public void testConfigCommand() throws Exception {
     Resource[] sent = ResourcesWrapperTest.createSampleResources().
       getResourceConfigDocument().getResourceConfig().getResources().
       getResourceArray();
     // Dump any password macros to avoid en/decryption problems.
     for(int i = 0; i < sent.length; i++) {
       Resource r = sent[i];
       if(r.getMacros() == null) {
         continue;
       }
       Macro[] macros = r.getMacros().getMacroArray();
       for(int j = macros.length - 1; j >= 0; j--) {
         if(macros[j].getName().matches(".*Password.*")) {
           r.getMacros().removeMacro(j);
         }
       }
     }
     setConfig(null, sent, null);
     IncaDocument doc = getIncaDocument();
     assertNotNull(doc);
     assertNotNull(doc.getInca());
     assertNotNull(doc.getInca().getResourceConfig());
     assertNotNull(doc.getInca().getResourceConfig().getResources());
     Resource[] received =
       doc.getInca().getResourceConfig().getResources().getResourceArray();
     assertNotNull(received);
     assertEquals(sent.length, received.length);
     for(int i = 0; i < sent.length; i++) {
       boolean found = false;
       for(int j = 0; j < received.length; j++) {
         if(sent[i].getName().equals(received[j].getName())) {
           found = true;
         }
       }
       assertTrue(found);
     }
   }

  /**
   * Tests that the agent can report and process an empty configuration.
   */
  public void testConfigEmpty() throws Exception {

    // Check the initial configuration
    IncaDocument doc = getIncaDocument();
    assertNotNull(doc);
    IncaDocument.Inca inca = doc.getInca();
    assertNotNull(inca);
    assertNotNull(inca.getRepositories());
    assertEquals(0, inca.getRepositories().getRepositoryArray().length);
    assertNotNull(inca.getResourceConfig());
    assertNotNull(inca.getResourceConfig().getResources());
    assertEquals
      (0, inca.getResourceConfig().getResources().getResourceArray().length);
    assertNotNull(inca.getSuites());
    assertEquals(0, inca.getSuites().getSuiteArray().length);

    // Send an empty configuration
    setConfig(null, null, null);

    // Check the new configuration
    doc = getIncaDocument();
    assertNotNull(doc);
    inca = doc.getInca();
    assertNotNull(inca);
    assertNotNull(inca.getRepositories());
    assertEquals(0, inca.getRepositories().getRepositoryArray().length);
    assertNotNull(inca.getResourceConfig());
    assertNotNull(inca.getResourceConfig().getResources());
    assertEquals
      (0, inca.getResourceConfig().getResources().getResourceArray().length);
    assertNotNull(inca.getSuites());
    assertEquals(0, inca.getSuites().getSuiteArray().length);

  }

  /**
   * Tests whether the agent can shorten its repository list.
   */
  public void testRepositoryDelete() throws Exception {
    String[] repos = {REPOSITORY_URL1, REPOSITORY_URL2};
    setConfig(repos, null, null);
    repos = new String[] {REPOSITORY_URL2};
    setConfig(repos, null, null);
    IncaDocument doc = getIncaDocument();
    assertNotNull(doc);
    assertNotNull(doc.getInca());
    assertNotNull(doc.getInca().getRepositories());
    repos = doc.getInca().getRepositories().getRepositoryArray();
    if(repos.length == 0) {
      fail("Empty repository list");
    } else if(repos.length > 1) {
      fail("Long repository list");
    } else if(repos[0].indexOf(REPOSITORY_PATH2) < 0) {
      fail("Invalid repository list '" + repos[0] + "'");
    }
  }

  /**
   * Tests whether the agent can process a valid repository.
   */
  public void testRepositorySingle() throws Exception {
    String[] repos = {REPOSITORY_URL1};
    setConfig(repos, null, null);
    IncaDocument doc = getIncaDocument();
    assertNotNull(doc);
    assertNotNull(doc.getInca());
    assertNotNull(doc.getInca().getRepositories());
    repos = doc.getInca().getRepositories().getRepositoryArray();
    if(repos.length == 0) {
      fail("Empty repository list");
    } else if(repos.length > 1) {
      fail("Long repository list");
    } else if(repos[0].indexOf(REPOSITORY_PATH1) < 0) {
      fail("Invalid repository list '" + repos[0] + "'");
    }
  }

  /**
   * Tests whether the agent can process multiple valid repositories.
   */
  public void testRepositoryMultiple() throws Exception {
    String[] repos = {REPOSITORY_URL1, REPOSITORY_URL2};
    setConfig(repos, null, null);
    IncaDocument doc = getIncaDocument();
    assertNotNull(doc);
    assertNotNull(doc.getInca());
    assertNotNull(doc.getInca().getRepositories());
    repos = doc.getInca().getRepositories().getRepositoryArray();
    if(repos.length == 0) {
      fail("Empty repository list");
    } else if(repos.length < 2) {
      fail("Short repository list");
    } else if(repos.length > 2) {
      fail("Long repository list");
    } else if((repos[0].indexOf(REPOSITORY_PATH1) < 0 &&
               repos[1].indexOf(REPOSITORY_PATH1) < 0) ||
              (repos[0].indexOf(REPOSITORY_PATH2) < 0 &&
               repos[1].indexOf(REPOSITORY_PATH2) < 0)) {
      fail("Invalid repository list '"+repos[0]+"' '"+repos[1]+"'");
    }
  }

  /**
   * Test ability to do run now
   *
   * @throws Exception if problem running test
   */
  public void testRunNow() throws Exception {
    String suiteXml = "<st:suite xmlns:st = \"http://inca.sdsc.edu/dataModel/suite_2.0\" xmlns:xsi = \"http://www.w3.org/2001/XMLSchema-instance\" >\n" +
      "  <seriesConfigs>\n" +
      "    <seriesConfig>\n" +
      "      <series>\n" +
      "        <name>cluster.compiler.gcc.version</name>\n" +
      "          <args>\n" +
      "            <arg>\n" +
      "               <name>help</name>\n" +
      "               <value>no</value>\n" +
      "             </arg>\n" +
      "             <arg>\n" +
      "               <name>log</name>\n" +
      "               <value>0</value>\n" +
      "             </arg>\n" +
      "             <arg>\n" +
      "               <name>verbose</name>\n" +
      "               <value>1</value>\n" +
      "             </arg>\n" +
      "             <arg>\n" +
      "               <name>version</name>\n" +
      "               <value>no</value>\n" +
      "             </arg>\n" +
      "          </args>\n" +
      "          <limits>\n" +
      "            <cpuTime>60</cpuTime>\n" +
      "          </limits>\n" +
      "          <context>@@</context>\n" +
      "      </series>\n" +
      "      <resourceSetName>localhost</resourceSetName>\n" +
      "      <schedule/>\n" +
      "      <action>add</action>\n" +
      "    </seriesConfig>\n" +
      "  </seriesConfigs>\n" +
      "  <name>_runNow</name>\n" +
      "  <guid>_runNow</guid>\n" +
      "</st:suite>";

    sampleRepository = RepositoriesTest.createSampleRepository(null);
    sampleResources = ResourcesWrapperTest.createSampleResources();

    // set resources and repository
    setConfig(
      new String[] {sampleRepository.getRepositories()[0].getURL().toString()},
      sampleResources.getResourceConfigDocument().getResourceConfig().getResources().getResourceArray(),
      new Suite[] {}
    );

    SuiteDocument runNow = SuiteDocument.Factory.parse(suiteXml);
    // start up RM by executing a run now request...request will get thrown
    // away because the RM will be started as a fresh install
    client.runNow( Protocol.RUNNOW_TYPE_INCAT, runNow.xmlText() );

    while( ! server.getReporterManager("localhost").isRunning() ){
      logger.debug( "Waiting 5 seconds for reporter manager to check in" );
      Thread.sleep( 5000 );
    }

    // execute run now request
    client.runNow( Protocol.RUNNOW_TYPE_INCAT, runNow.xmlText() );

    waitForReports( new String[]{ "gcc" } );

    // try to fake it out with a non-run now suite
    runNow.getSuite().setName("anIncaSuite");
    try {
      client.runNow( Protocol.RUNNOW_TYPE_INCAT, runNow.xmlText() );
      fail( "request should have failed");
    } catch (Exception e) { /* do nothing */ }


  }

  /**
   * Test Agent suite manipulation in approve mode.
   * This test starts up a MockDepot and submits 2 proposed changes.  Approves
   * one and then waits for report to be sent to it.  This test could take a
   * couple minutes to complete.
   *
   * @throws Exception if problem running test
   */
  public void testSuiteApproveCommands() throws Exception {
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
      "        <name>" + Protocol.EMAIL_MACRO + "</name>\n" +
      "        <value>ssmallen@mac.com</value>\n" +
      "      </macro>\n" +
      "      <macro>\n" +
      "        <name>verbose</name>\n" +
      "        <value>1</value>\n" +
      "      </macro>\n" +
      "    </macros>\n" +
      "  </resource>\n" +
      "</resources>\n" +
      "</rc:resourceConfig>";
    sampleResources = new ResourcesWrapper
      (ResourceConfigDocument.Factory.parse( resources ) );
    submitSuite
      ( new String[]{"test/samplesuitelocal.xml"}, 0, new String[0] );
    logger.info( "----------------- suite submitted -----------------------" );
    while ( server.getReporterManagerTable().get( "localhost" ) == null ) {
      logger.debug( "Sleeping few seconds for RM object to get created" );
      Thread.sleep( 2000 );
    }
    ReporterManagerController rmc =
      server.getReporterManagerTable().get( "localhost" );
    while ( ! rmc.isRunning() ) {
      logger.debug( "Sleeping few seconds for RM object to register" );
      Thread.sleep( 2000 );
    }
    logger.info( "----------------- RM registered -----------------------" );
    Thread.sleep( 5000 );

    // in this case our client is the reporter manager
    String rmCert = StringMethods.fileContents
      ( ReporterManagerControllerTest.TEMP_RM_DIR + "/etc/rmcert.pem" );
    String rmKey = StringMethods.fileContents
      ( ReporterManagerControllerTest.TEMP_RM_DIR  + "/etc/rmkey.pem" );
    client = connectAgentClient("localhost", server.getPort(), rmCert, rmKey);

    IncaDocument proposed = client.getProposedChanges( "localhost" );
    assertEquals( "correct num of suites", 1,
                  proposed.getInca().getSuites().sizeOfSuiteArray() );
    Suite proposedSuite = proposed.getInca().getSuites().getSuiteArray(0);
    assertEquals( "correct num of series", 3,
                  proposedSuite.getSeriesConfigs().sizeOfSeriesConfigArray() );
    logger.info( "----------------- verify proposed -----------------------" );

    proposedSuite.getSeriesConfigs().removeSeriesConfig(1);
    proposedSuite.getSeriesConfigs().removeSeriesConfig(1);
    client.approve( "localhost", proposed );
    logger.info( "----------------- approved 1 change ----------------------" );

    waitForReports( new String[]{"gcc"} );
    logger.info( "----------------- received report  ----------------------" );

    proposed = client.getProposedChanges( "localhost" );
    assertEquals( "correct num of suites", 1,
                  proposed.getInca().getSuites().sizeOfSuiteArray() );
    proposedSuite = proposed.getInca().getSuites().getSuiteArray(0);
    assertEquals( "correct num of series", 2,
                  proposedSuite.getSeriesConfigs().sizeOfSeriesConfigArray() );

    AgentTest.stopAgent();
    depot.clearReports();
    startAgent( false );
    logger.info( "----------------- restarted agent ----------------------" );

    client = connectAgentClient("localhost", server.getPort(), rmCert, rmKey);
    proposed = client.getProposedChanges( "localhost" );
    proposedSuite = proposed.getInca().getSuites().getSuiteArray(0);
    proposedSuite.getSeriesConfigs().removeSeriesConfig(1);
    client.approve( "localhost", proposed );
    logger.info( "----------------- approve 1 change ----------------------" );

    waitForReports( new String[]{"openssl", "gcc"} );
    logger.info( "----------------- received reports  ----------------------" );

    proposed = client.getProposedChanges( "localhost" );
    proposedSuite = proposed.getInca().getSuites().getSuiteArray(0);
    assertEquals( "correct num of series", 1,
                  proposedSuite.getSeriesConfigs().sizeOfSeriesConfigArray() );
    sampleResources = null;
  }

  /**
   * Test Agent suite manipulation.
   * This test starts up a MockDepot and waits for 2 reports to be sent
   * to it before moving on.  This test could take a couple minutes to
   * complete.
   *
   * @throws Exception if problem running test
   */
  public void testSuiteCommands() throws Exception {
    submitSuite(
      new String[]{"test/samplesuitelocal.xml"},
      2, new String[]{ "gcc", "openssl"}
    );
    submitSuite(
      new String[]{"test/delete_samplesuitelocal.xml"},
      1, new String[]{"openssl"}
    );

    Thread.sleep(10 * Constants.MILLIS_TO_SECOND );
    depot.clearReports();
    logger.info( "Restarting agent" );
    AgentTest.stopAgent( );
    startAgent( false );
    waitForReports( new String[]{"openssl"} );
  }


  /**
   * Test the setConfig and getConfig commands with a mock depot.
   */
  public void testSuitesMultiple() throws Exception {
    submitSuite(
      new String[]{"test/samplesuitelocal.xml", "test/samplesuitelocal2.xml"},
      3, new String[]{ "gcc", "openssl", "user"}
    );

    waitForReports
      ( new String[]{ "gcc", "openssl", "user"} );
    depot.clearReports();

    // changes a macro in the localhost resource
    ResourceConfigDocument rcd = sampleResources.getResourceConfigDocument();
    Object[] result = rcd.selectPath(
      "//resource[name='localhost']//macro[name='verbose']"
    );
    assertEquals( "got verbose macro", 1, result.length );
    ((Macro)result[0]).setValueArray( 0, "25" );
    setConfig(
      new String[] {sampleRepository.getRepositories()[0].getURL().toString()},
      rcd.getResourceConfig().getResources().getResourceArray(),
      null
    );
    waitForReports
      ( new String[]{ "openssl", "user", "<value>25</value>"} );

  }

  /**
   * The repository cache will check for package updates periodically or
   * whenever the Agent's setRepositories function is invoked.  This test
   * will first set the package check update period frequency to every 30
   * seconds and then update the version of the gcc version reporter to "10".
   * To verify, it will wait for the next report and check it's version.  Once
   * verified, we set the update frequency to every 4 hours (the default) and
   * then update the version of the gcc version reporter to "20".  Then we
   * invoke the Agent's setRepository function and verify the update was
   * made in the next report that is received.
   *
   * @throws Exception if problem running test
   */
  public void testSuiteWithPackageUpdate() throws Exception {
    sampleRepository = RepositoriesTest.createSampleRepository(null);

    String reporterName = "cluster.compiler.gcc.version";
    String reporterVersion = "1.5";
    assertEquals( "repository refresh correct", 30 * Constants.MILLIS_TO_SECOND,
      this.server.getRepositoryCache().getUpdatePeriod() );

    // No updates available for gcc reporter
    assertFalse(
      "package not updated",
      sampleRepository.hasPackageUpdated( reporterName, reporterVersion )
    );

    // Run the suite containing the gcc reporter
    submitSuite(
      new String[]{"test/samplesuitelocal.xml"},
      2, new String[]{ "gcc", "openssl" }
    );
    logger.info( "Received first set of reports --------" );

    // Update the version of the reporter to 10 and let the repository
    // auto-refresh
    updateVersionInCatalog( sampleRepository, reporterName, "10" );
    updateVersionInReporter( reporterName,"10" );

    // Wait for next reports to come in - should have updated gcc report
    waitForReports( new String[]{"openssl", "gcc", "<version>10</version>"});
    logger.info( "Received second set of reports --------" );

    // Make sure auto-refresh doesn't update the reporter
    logger.info( "Setting refresh frequency to 4 hours" );
    Agent.getGlobalAgent().getRepositoryCache().setUpdatePeriod(
      4 * Constants.MILLIS_TO_HOUR
    );

    // Update the version of the reporter to 20
    AgentTest.MockDepot.getDepot().clearReports();
    logger.info( "Updating repository" );
    updateVersionInCatalog( sampleRepository, reporterName, "20" );
    updateVersionInReporter( reporterName,"20" );

    // Force the agent to check for updates
    Agent.getGlobalAgent().setRepositories( sampleRepository.getRepositories() );
    Agent.getGlobalAgent().updateCachedPackages();

    // Wait for the next set of reports to come in - gcc should now contain 20
    waitForReports( new String[]{"openssl", "gcc", "<version>20</version>"});
  }

  /**
   * Test whether the Agent appropriately restricts certain actions to clients
   * that have been given permission to make them.
   *
   * @throws Exception
   */
  public void testPermit() throws Exception {

    String failMsg = null;
    String[] repos = {REPOSITORY_URL1, REPOSITORY_URL2};

    String dn = client.getDn(false);

    // Try updating config w/no permissions ...
    MessageHandler.resetPermissions();
    logger.debug( "SENDING CONFIG" );
    setConfig( repos, null, null );
    // ... with insufficient permission ...
    client.commandPermit(dn + "OTHER", Protocol.CONFIG_ACTION);
    try {
      logger.debug( "SENDING FORBIDDEN CONFIG" );
      setConfig( repos, null, null );
      fail("Forbidden config allowed");
    } catch(Exception e) {
      client = connectAgentClient
        ("localhost",server.getPort(),AgentTest.INCAT_CERT,AgentTest.INCAT_KEY);
    }
    // ... and with sufficient permission
    MessageHandler.resetPermissions();
    client.commandPermit(dn, Protocol.CONFIG_ACTION);
    logger.debug( "SENDING ALLOWED CONFIG" );
    setConfig( repos, null, null );

  }

  /**
   * Test that the reporter manager is restarted when it's configuration
   * is changed.
   *
   * @throws Exception  when problem with test
   */
  public void testUpdateConfigRestart() throws Exception {
    submitSuite(
      new String[]{"test/samplesuitelocal.xml"},
      2, new String[]{ "gcc", "openssl"}
    );

    ResourceConfigDocument rc =
      (ResourceConfigDocument)Agent.getGlobalAgent().getResources().getResourceConfigDocument().copy();
    Resource[] resources =
      rc.getResourceConfig().getResources().getResourceArray();
    for( Resource resource : resources ) {
      if ( resource.getName().equals("localhost") ) {
        Macro macro = resource.getMacros().addNewMacro();
        macro.setName( Protocol.SUSPEND_MACRO );
        macro.addValue( "load1>0");
      }
    }
    setConfig(
      new String[] {sampleRepository.getRepositories()[0].getURL().toString()},
      rc.getResourceConfig().getResources().getResourceArray(),
      null
    );

    depot.clearReports();

    waitForReports( new String[]{"high load"} );
   }


  /**
   * Change the version of a reporter in the reporter repository.
   *
   * @param repositories  A list of reporter repositories
   *
   * @param reporterName  The name of the reporter to change the version on
   *
   * @param version       The new version of the reporter.
   *
   * @throws IOException  if problem updating catalog
   */
  static public void updateVersionInCatalog(
    Repositories repositories, String reporterName, String version )
    throws IOException {

    Repository repo = repositories.getRepositoryForPackage
      ( reporterName, null );
    assertNotNull( "repository found for " + reporterName, repo );
    logger.info( "repository url is " + repo.getURL().toString() );
    Properties[] props = repo.getPropertiesByLookup( "name", reporterName );
    assertNotNull( "properties found for gcc reporter", props );

    File packagesGz = new File(
      repo.getURL().getPath() + File.separator + "Packages.gz"
    );
    assertTrue( "Package.gz found", packagesGz.exists() );
    BufferedReader input = new BufferedReader(
      new InputStreamReader(
        new GZIPInputStream(new FileInputStream(packagesGz))
      )
    );
    File packagesGzNew = new File(
      repo.getURL().getPath() + File.separator + "Packages.gz.new"
    );
    OutputStreamWriter output = new OutputStreamWriter(
      new GZIPOutputStream(new FileOutputStream(packagesGzNew))
    );
    String s;
    boolean nextVersionGcc = false;
    while( (s = input.readLine()) != null ) {
      if ( Pattern.compile("name: " + reporterName).matcher(s).find() ) {
        nextVersionGcc = true;
      }
      if ( nextVersionGcc && Pattern.compile("^version:").matcher(s).find() ) {
        nextVersionGcc = false;
        logger.debug( "Updating catalog entry '" + s + "' to " + version );
        output.write( "version: " + version + "\n");
      } else {
        output.write(s + "\n");
      }
    }
    output.close();
    input.close();
    packagesGzNew.renameTo( packagesGz );
    packagesGzNew.deleteOnExit();
    assertTrue( "Packages.gz exists", packagesGz.exists() );
    assertFalse( "Packages.gz.new does not exist", packagesGzNew.exists() );
  }

  // Private functions

  /**
   * Retrieve the configuration from the Agent server
   *
   * @return An Inca document showing the configuration of the agent server.
   *
   * @throws IOException if problem contacting agent
   * @throws ProtocolException if problem getting data from agent
   * @throws XmlException if problem if reading datat from agent
   */
  private IncaDocument getIncaDocument()
    throws IOException, ProtocolException, XmlException {
    String xml = client.getConfig();
    return IncaDocument.Factory.parse(xml);
  }

  /**
   * Send a new Inca configuration to the Inca agent server.
   *
   * @param repositories  The new list of reporter repositories.
   *
   * @param resources     The new resource configuration.
   *
   * @param suites        Changes to the suites.
   *
   * @throws IOException if problem connecting to agent
   * @throws ProtocolException if problem setting config in agent
   */
  private void setConfig
    (String[] repositories, Resource[] resources, Suite[] suites)
    throws CrypterException, IOException, ProtocolException {

    IncaDocument doc = IncaDocument.Factory.newInstance();
    IncaDocument.Inca inca = doc.addNewInca();
    IncaDocument.Inca.Suites docSuites = inca.addNewSuites();
    if(repositories != null) {
      IncaDocument.Inca.Repositories docRepositories=inca.addNewRepositories();
      for(int i = 0; i < repositories.length; i++) {
        docRepositories.addRepository(repositories[i]);
      }
    }
    if(resources != null) {
      Resources docResources = inca.addNewResourceConfig().addNewResources();
      for(int i = 0; i < resources.length; i++) {
        docResources.addNewResource();
        docResources.setResourceArray(i, resources[i]);
      }
    }
    if(suites != null) {
      for(int i = 0; i < suites.length; i++) {
        docSuites.addNewSuite();
        docSuites.setSuiteArray(i, suites[i]);
      }
    }
    String xml = XmlWrapper.cryptSensitive( doc.xmlText(), AgentTest.PASSWORD, false );
    client.setConfig(xml);
  }

  /**
   * Start a new agent server.
   *
   * @param fresh indicates whether to start an agent from scratch (i.e.,
   * with no existing state)
   *
   * @return A new Agent object.
   *
   * @throws Exception if problem starting agent
   */
  private Agent startAgent( boolean fresh ) throws Exception {
    if ( fresh ) {
      File tempFile = new File("var");
      StringMethods.deleteDirectory(tempFile);
    }
    return AgentTest.startAgent( true, false );
  }

  /**
   * Start up a new Inca agent client.
   *
   * @param server  The hostname of the agent server
   *
   * @param port    The port of the agent server
   *
   * @param certTxt
   *@param keyTxt @return A new AgentClient object
   *
   * @throws ConfigurationException  if problem reading configuration
   * @throws IOException if problem connecting to agent
   */
  private AgentClient connectAgentClient(
    String server, int port, final String certTxt, final String keyTxt)
    throws ConfigurationException, IOException {

    AgentClient result = new AgentClient() {
      public void readCredentials() throws ConfigurationException, IOException {
        try {
          this.cert = readCertData(certTxt);
          this.key = readKeyData(keyTxt, AgentTest.PASSWORD);
        }
        catch (Exception err) {
          throw new ConfigurationException(err);
        }
        this.trusted.add(readPEMData(AgentTest.AGENT_CERT));
        this.trusted.add(readPEMData(AgentTest.CA_CERT));
      }
    };
    ConfigProperties config = new ConfigProperties();
    config.putAllTrimmed(System.getProperties(), "inca.agent.");
    config.loadFromResource("inca.properties", "inca.agent.");
    config.setProperty( "auth", "true" );
    config.setProperty( "password", AgentTest.PASSWORD );
    config.remove( "logfile" );
    result.setConfiguration(config);
    result.setServer(server, port);
    result.connect();
    return result;
  }

  /**
   * Submit a specified number of suite documents to the Inca agent and then
   * wait for a specified number of reports to be received.  Also retrieves
   * the configuration on the agent server and checks the results.
   *
   * @param suiteFiles  The suite files to submit to the agent server.
   * @param numReports  The number of reports to expect from the suites.
   * @param patterns    The patterns that be searched for in the received
   *                    reports indicating successful submission.
   *
   * @throws Exception  if trouble submitting suites to agent
   */
  private void submitSuite(
    String[] suiteFiles, int numReports, String[] patterns )
    throws Exception {

    logger.debug
      ("------------ " + StringMethods.join(", ",suiteFiles) + " ----------- ");
    if ( sampleRepository == null ) {
      sampleRepository = RepositoriesTest.createSampleRepository(null);
    }

    // Read in resources
    if ( sampleResources == null ) {
      sampleResources = ResourcesWrapperTest.createSampleResources();
    }
    ResourceConfigDocument rcd = sampleResources.getResourceConfigDocument();

    // Read in suite
    Suite[] suites =  new Suite[suiteFiles.length];
    Hashtable suiteNames = new Hashtable();
    for ( int i = 0; i < suites.length; i++ ) {
      suites[i]=SuiteDocument.Factory.parse(new File(suiteFiles[i])).getSuite();
      suiteNames.put( suites[i].getName(), "" );
    }
    String[] uniqSuiteNames = (String[])suiteNames.keySet().toArray
      ( new String[suiteNames.size()]);

    // Submit to agent.
    setConfig(
      new String[] {sampleRepository.getRepositories()[0].getURL().toString()},
      rcd.getResourceConfig().getResources().getResourceArray(),
      suites
    );

    logger.debug( "Config sent...waiting for " + numReports + " reports" );
    waitForReports( patterns );

    // Read back the configuration from the agent and check the suite guid
    IncaDocument doc = getIncaDocument();
    assertNotNull( "Inca document retrieved from agent", doc );
    assertNotNull( "Inca document has content", doc.getInca() );
    assertNotNull( "Inca document has suites", doc.getInca().getSuites());
    suites = doc.getInca().getSuites().getSuiteArray();
    assertEquals(
      "Contains " + uniqSuiteNames.length + " suites",
      suites.length, uniqSuiteNames.length
    );
    String[] receivedSuiteNames = new String[suites.length];
    for ( int i = 0; i < suites.length; i++ ) {
      receivedSuiteNames[i] = suites[i].getName();
      assertEquals(
        "Guid set",
        suites[i].getGuid(), server.getUri() + "/" + receivedSuiteNames[i]
      );
    }
    Arrays.sort( receivedSuiteNames );
    Arrays.sort( uniqSuiteNames );
    for ( int i = 0; i < suites.length; i++ ) {
      assertEquals(
        "Suite " + uniqSuiteNames[i] + " exists on agent",
        receivedSuiteNames[i], uniqSuiteNames[i]
      );
    }
  }

  /**
   * Update the version of the reporter (i.e., code).
   *
   * @param reporterName  The name of the reporter to update.
   *
   * @param version       The new version of the reporter
   *
   * @throws IOException if trouble updating reporter
   */
  private void updateVersionInReporter( String reporterName, String version )
    throws IOException {

    Repository repo = sampleRepository.getRepositoryForPackage
      ( reporterName, null );
    assertNotNull( "repository found for " + reporterName, repo );
    logger.info( "repository url is " + repo.getURL().toString() );
    Properties[] props = repo.getPropertiesByLookup( "name", reporterName );
    assertNotNull( "properties found for gcc reporter", props );
    String reporterText = new String(repo.getReporter(reporterName));

    BufferedReader input;
    OutputStreamWriter output;
    String s;

    File reporter = new File(
      repo.getURL().getPath() + File.separator + reporterName
    );
    input = new BufferedReader(
      new InputStreamReader(new FileInputStream(reporter))
    );
    File reporterNew = new File(
      repo.getURL().getPath() + File.separator + reporterName + ".new"
    );
    output = new OutputStreamWriter(
      new FileOutputStream(reporterNew)
    );
    while( (s = input.readLine()) != null ) {
      if ( Pattern.compile("version =>").matcher(s).find() ) {
        output.write( "version => " + version + ",\n");
      } else {
        output.write(s + "\n");
      }
    }
    output.close();
    input.close();
    reporterNew.renameTo( reporter );
    reporterNew.deleteOnExit();
    assertFalse(
      "package updated",
      reporterText.equals( new String(repo.getReporter(reporterName)) )
    );

  }

  /**
   * Wait for the reports to be received by the mock depot server and check
   * that the specifed patterns have been found.
   *
   * @param patterns   Patterns that should be found in the received reports
   *                   indicating a successul configuration.
   *
   * @throws InterruptedException if interrupted
   */
  private void waitForReports( String[] patterns ) throws InterruptedException {
    depot.waitForReports( patterns.length );
    assertTrue(
      "all patterns found in reports",
      depot.checkReportsForPatterns( patterns )
    );
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
   */
  private static KeyPair readKeyData(String data, final String password) throws IOException, KeyStoreException
  {
    Object pemObject = readPEMData(data);
    PEMKeyPair pemKey;

    if (pemObject instanceof PEMKeyPair)
      pemKey = (PEMKeyPair)pemObject;
    else if (pemObject instanceof PEMEncryptedKeyPair) {
      PEMEncryptedKeyPair encryptedKey = (PEMEncryptedKeyPair)pemObject;
      JcePEMDecryptorProviderBuilder decryptBuilder = new JcePEMDecryptorProviderBuilder();

      decryptBuilder.setProvider("BC");

      PEMDecryptorProvider decryptor = decryptBuilder.build(password.toCharArray());

      pemKey = encryptedKey.decryptKeyPair(decryptor);
    }
    else
      throw new KeyStoreException(data + " does not contain a key pair");

    JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();

    keyConverter.setProvider("BC");

    return keyConverter.getKeyPair(pemKey);
  }
}
