package edu.sdsc.inca.util;

import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.dataModel.suite.Suite;
import edu.sdsc.inca.dataModel.util.SeriesConfig;
import edu.sdsc.inca.repository.RepositoriesTest;
import edu.sdsc.inca.repository.Repositories;
import edu.sdsc.inca.ConfigurationException;

import junit.framework.TestCase;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import java.util.Properties;

/**
 * Tests the SuiteWrapper class
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class SuiteWrapperTest extends TestCase {

  /**
   * Creates a suite with the following reporters:
   *
   * gcc version reporter running on resource teragrid
   * myproxy unit reporter running on resource teragrid
   * gatekeeper unit reporter running on resource teragrid
   * tgcp unit reporter running on resource teragrid
   *
   * @param repositories The repository that contains listed reporters
   *
   * @return A SuiteWrapper object containing the above suite.
   *
   * @throws ConfigurationException if unable to resolve reporters
   * @throws XmlException if unable to create sample suite
   */
  public static SuiteWrapper createSampleSuite( Repositories repositories )
    throws XmlException, ConfigurationException {

    String suiteXml =
      "<?xml version = \"1.0\"?>\n" +
      "<st:suite xmlns:st = \"http://inca.sdsc.edu/dataModel/suite_2.0\" xmlns:xsi = \"http://www.w3.org/2001/XMLSchema-instance\" >\n" +
      "  <seriesConfigs>\n" +
      "\n" +
      "    <seriesConfig>\n" +
      "      <series>\n" +
      "        <name>cluster.compiler.gcc.version</name>\n" +
      "          <args>\n" +
      "            <arg>\n" +
      "              <name>verbose</name>\n" +
      "              <value>0</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>help</name>\n" +
      "              <value>no</value>\n" +
      "            </arg>\n" +
      "          </args>\n" +
      "          <limits>\n" +
      "            <cpuTime>60</cpuTime>\n" +
      "          </limits>\n" +
      "        <context>@@</context>\n" +
      "      </series>\n" +
      "      <resourceSetName>teragrid</resourceSetName>\n" +
      "      <schedule>\n" +
      "        <cron>\n" +
      "          <min>?</min>\n" +
      "          <hour>?/8</hour>\n" +
      "          <mday>*</mday>\n" +
      "          <wday>*</wday>\n" +
      "          <month>*</month>\n" +
      "        </cron>\n" +
      "      </schedule>\n" +
      "      <action>add</action>\n" +
      "    </seriesConfig>\n" +
      "\n" +
      "    <seriesConfig>\n" +
      "      <series>\n" +
      "        <name>grid.interactive_access.myproxy.unit</name>\n" +
      "          <args>\n" +
      "            <arg>\n" +
      "              <name>verbose</name>\n" +
      "              <value>0</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>help</name>\n" +
      "              <value>no</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>server</name>\n" +
      "              <value>@myproxyServer@</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>certpw</name>\n" +
      "              <value>@myproxyCertPassword@</value>\n" +
      "            </arg>\n" +
      "          </args>\n" +
      "          <limits>\n" +
      "            <cpuTime>60</cpuTime>\n" +
      "          </limits>\n" +
      "          <context>@pre@@@@post@</context>\n" +
      "      </series>\n" +
      "      <resourceSetName>teragrid</resourceSetName>\n" +
      "      <schedule>\n" +
      "        <cron>\n" +
      "          <min>54</min>\n" +
      "          <hour>10-23/24</hour>\n" +
      "          <mday>*</mday>\n" +
      "          <wday>*</wday>\n" +
      "          <month>*</month>\n" +
      "        </cron>\n" +
      "      </schedule>\n" +
      "      <action>add</action>\n" +
      "    </seriesConfig>\n" +
      "\n" +
      "    <seriesConfig>\n" +
      "      <series>\n" +
      "        <name>grid.middleware.globus.unit.gatekeeper</name>\n" +
      "          <args>\n" +
      "            <arg>\n" +
      "              <name>verbose</name>\n" +
      "              <value>0</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>help</name>\n" +
      "              <value>no</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>host</name>\n" +
      "              <value>@gridServicesNode@</value>\n" +
      "            </arg>\n" +
      "          </args>\n" +
      "          <limits>\n" +
      "            <cpuTime>60</cpuTime>\n" +
      "          </limits>\n" +
      "          <context>@@</context>\n" +
      "      </series>\n" +
      "      <resourceSetName>teragrid</resourceSetName>\n" +
      "      <schedule>\n" +
      "        <cron>\n" +
      "          <min>54</min>\n" +
      "          <hour>10-23/24</hour>\n" +
      "          <mday>*</mday>\n" +
      "          <wday>*</wday>\n" +
      "          <month>*</month>\n" +
      "        </cron>\n" +
      "      </schedule>\n" +
      "      <action>add</action>\n" +
      "    </seriesConfig>\n" +
      "\n" +
      "    <seriesConfig>\n" +
      "      <series>\n" +
      "        <name>data.transfer.tgcp.unit</name>\n" +
      "          <args>\n" +
      "            <arg>\n" +
      "              <name>verbose</name>\n" +
      "              <value>0</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>help</name>\n" +
      "              <value>no</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>srcURL</name>\n" +
      "              <value>$TG_CLUSTER_PFS</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>dstURL</name>\n" +
      "              <value>@tgcpServers@</value>\n" +
      "            </arg>\n" +
      "          </args>\n" +
      "          <limits>\n" +
      "            <cpuTime>60</cpuTime>\n" +
      "          </limits>\n" +
      "          <context>@@</context>\n" +
      "      </series>\n" +
      "      <resourceSetName>teragrid</resourceSetName>\n" +
      "      <schedule>\n" +
      "        <cron>\n" +
      "          <min>54</min>\n" +
      "          <hour>10-23/24</hour>\n" +
      "          <mday>*</mday>\n" +
      "          <wday>*</wday>\n" +
      "          <month>*</month>\n" +
      "        </cron>\n" +
      "      </schedule>\n" +
      "      <action>add</action>\n" +
      "    </seriesConfig>\n" +
      "  </seriesConfigs>\n" +
      "  <name>TestSuite</name>\n" +
      "  <guid>edu.sdsc.inca.testsuite</guid>\n" +
      "  <version>2</version>\n" +
      "</st:suite>";
    SuiteDocument suiteDoc = SuiteDocument.Factory.parse(suiteXml);
    SuiteWrapper suite = new SuiteWrapper( suiteDoc );
    resolveReporterNames( suite, repositories );
    return suite;
  }

  /**
   * Creates a suite with the following reporters:
   *
   * deletes gcc version reporter running on resource teragrid
   * adds gcc version reporter running on resource sdsc
   * deletes myproxy unit reporter running on resource teragrid
   *
   * @param repos The repository that contains listed reporters
   *
   * @return A SuiteWrapper object containing the above suite.
   *
   * @throws ConfigurationException if unable to resolve reporters
   * @throws XmlException if unable to create suite
   */
  public static SuiteWrapper createSampleSuiteModification( Repositories repos)
    throws XmlException, ConfigurationException {

    String suiteXml =
      "<?xml version = \"1.0\"?>\n" +
      "<st:suite xmlns:st = \"http://inca.sdsc.edu/dataModel/suite_2.0\" xmlns:xsi = \"http://www.w3.org/2001/XMLSchema-instance\" >\n" +
      "  <seriesConfigs>\n" +
      "\n" +
      "    <seriesConfig>\n" +
      "      <series>\n" +
      "        <name>cluster.compiler.gcc.version</name>\n" +
      "          <args>\n" +
       "            <arg>\n" +
      "              <name>help</name>\n" +
      "              <value>no</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>verbose</name>\n" +
      "              <value>0</value>\n" +
      "            </arg>\n" +
      "          </args>\n" +
      "          <limits>\n" +
      "            <cpuTime>60</cpuTime>\n" +
      "          </limits>\n" +
      "          <context>@@</context>\n" +
      "      </series>\n" +
      "      <resourceSetName>teragrid</resourceSetName>\n" +
      "      <schedule>\n" +
      "        <cron>\n" +
      "          <min>?</min>\n" +
      "          <hour>?/8</hour>\n" +
      "          <mday>*</mday>\n" +
      "          <wday>*</wday>\n" +
      "          <month>*</month>\n" +
      "        </cron>\n" +
      "      </schedule>\n" +
      "      <action>delete</action>\n" +
      "    </seriesConfig>\n" +
      "\n" +
      "    <seriesConfig>\n" +
      "      <series>\n" +
      "        <name>cluster.compiler.gcc.version</name>\n" +
      "          <args>\n" +
      "            <arg>\n" +
      "              <name>verbose</name>\n" +
      "              <value>0</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>help</name>\n" +
      "              <value>no</value>\n" +
      "            </arg>\n" +
      "          </args>\n" +
      "          <limits>\n" +
      "            <cpuTime>60</cpuTime>\n" +
      "          </limits>\n" +
      "          <context>@@</context>\n" +
      "      </series>\n" +
      "      <resourceSetName>sdsc</resourceSetName>\n" +
      "      <schedule>\n" +
      "        <cron>\n" +
      "          <min>?</min>\n" +
      "          <hour>?/8</hour>\n" +
      "          <mday>*</mday>\n" +
      "          <wday>*</wday>\n" +
      "          <month>*</month>\n" +
      "        </cron>\n" +
      "      </schedule>\n" +
      "      <action>add</action>\n" +
      "    </seriesConfig>\n" +
      "\n" +
      "    <seriesConfig>\n" +
      "      <series>\n" +
      "        <name>grid.interactive_access.myproxy.unit</name>\n" +
      "          <args>\n" +
      "            <arg>\n" +
      "              <name>verbose</name>\n" +
      "              <value>0</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>help</name>\n" +
      "              <value>no</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>server</name>\n" +
      "              <value>@myproxyServer@</value>\n" +
      "            </arg>\n" +
      "            <arg>\n" +
      "              <name>certpw</name>\n" +
      "              <value>@myproxyCertPassword@</value>\n" +
      "            </arg>\n" +
      "          </args>\n" +
      "          <limits>\n" +
      "            <cpuTime>60</cpuTime>\n" +
      "          </limits>\n" +
      "          <context>@pre@@@@post@</context>\n" +
      "      </series>\n" +
      "      <resourceSetName>teragrid</resourceSetName>\n" +
      "      <schedule>\n" +
      "        <cron>\n" +
      "          <min>54</min>\n" +
      "          <hour>10-23/24</hour>\n" +
      "          <mday>*</mday>\n" +
      "          <wday>*</wday>\n" +
      "          <month>*</month>\n" +
      "        </cron>\n" +
      "      </schedule>\n" +
      "      <action>delete</action>\n" +
      "    </seriesConfig>\n" +
      "  </seriesConfigs>\n" +
      "\n" +
      "  <name>TestSuite</name>\n" +
      "  <guid>edu.sdsc.inca.testsuite</guid>\n" +
      "</st:suite>";
    SuiteDocument suiteDoc = SuiteDocument.Factory.parse(suiteXml);
    SuiteWrapper suite = new SuiteWrapper( suiteDoc );
    resolveReporterNames( suite, repos );
    return suite;
  }

  /**
   * Test good behavior when dealing with a blank suite
   *
   * @throws Exception if problem running test
   */
  public void testBlankSuite() throws Exception {
    SuiteDocument suiteDoc = SuiteDocument.Factory.newInstance();
    Suite suite = suiteDoc.addNewSuite();
    suite.addNewSeriesConfigs();
    suite.setGuid( "blah");
    suite.setName( "test" );
    new SuiteWrapper( suiteDoc );
  }

  /**
   * test the changeAction function
   *
   * @throws Exception if problem running test
   */
  public void testChangeAction() throws Exception {
    SuiteWrapper sampleSuite = createSampleSuite( null );
    XmlObject[] results = sampleSuite.getSuiteDocument().selectPath(
      "//seriesConfig[action='add']"
    );
    assertEquals( "4 adds found", 4, results.length );
    sampleSuite.changeAction( "delete" );
    results = sampleSuite.getSuiteDocument().selectPath(
      "//seriesConfig[action='delete']"
    );
    assertEquals( "4 deletes found", 4, results.length );
  }

  /**
   * Test the diff function
   *
   * @throws Exception if problem running test
   */
  public void testDiff() throws Exception {
    // set up original
    Repositories repositories = RepositoriesTest.createSampleRepository(null);
    SuiteWrapper sampleSuite = createSampleSuite( repositories );

    // make a copy and change schedule in one of the series; this should
    // result in an add and a delete
    SuiteDocument newSuite =
      (SuiteDocument)sampleSuite.getSuiteDocument().copy();
    XmlObject[] objects = newSuite.selectPath(
      "//seriesConfig/series[matches(uri, '^.*gcc.*$')]/.."
    );
    assertEquals( "got gcc config", 1, objects.length );
    SeriesConfig seriesConfig = (SeriesConfig)objects[0];
    seriesConfig.getSchedule().getCron().setHour( "?/9" );
    SuiteDocument changes = sampleSuite.diff( newSuite );
    assertEquals(
      "guid set",
      "edu.sdsc.inca.testsuite",
      changes.getSuite().getGuid()
    );
    assertEquals(
      "got 2 changes", 2,
      changes.getSuite().getSeriesConfigs().sizeOfSeriesConfigArray()
    );
    objects = changes.selectPath(
      "//seriesConfig/series[matches(uri, '^.*gcc.*$')]/.."
    );
    assertEquals( "got gcc configs", 2, objects.length );
    seriesConfig = (SeriesConfig)objects[0];
    assertEquals(
      "got resource", "?/8", seriesConfig.getSchedule().getCron().getHour()
    );
    assertEquals( "got delete", "delete", seriesConfig.getAction() );
    seriesConfig = (SeriesConfig)objects[1];
    assertEquals(
      "got resource", "?/9", seriesConfig.getSchedule().getCron().getHour()
    );
    assertEquals( "got add", "add", seriesConfig.getAction() );
  }

  /**
   * Replace the reporter names with reporter uris.
   *
   * @param suite  A suite containing reporter names.
   *
   * @param repositories  Where to pick up reporter uris.
   *
   * @throws ConfigurationException if unable to resolve reporters
   */
  public static void resolveReporterNames( SuiteWrapper suite,
                                            Repositories repositories )
    throws ConfigurationException {

    if ( repositories == null ) return;

    for( SeriesConfig series : suite.getSeriesConfigs() ) {
      String name = series.getSeries().getName();
      String version = series.getSeries().getVersion();
      Properties pkg = repositories.getRepositoryForPackage( name, version ).
        getProperties( name, version );
      if ( pkg == null ) {
        throw new ConfigurationException( "unable to locate " + name );
      }
      series.getSeries().setUri(
        repositories.getRepositoryUrlForPackage( name, version )
      );
      String value = version == null ? "?=" : "";
      series.getSeries().setVersion(
        value + pkg.getProperty( Repositories.VERSION_ATTR)
      );
    }
  }

   /**
   * test the mergeSeriesConfig function
   *
   * @throws Exception if problem running test
   */
  public void testHasRunNowMatch() throws Exception {
    SuiteWrapper sampleSuite = createSampleSuite( null );
    int suiteLength = sampleSuite.getSeriesConfigCount();

    // test series is there
    SeriesConfig config =
      (SeriesConfig)sampleSuite.getSeriesConfig( suiteLength-1 ).copy();
    config.getSchedule().unsetCron();  // convert to run now
    assertTrue( "suite has match", sampleSuite.hasRunNowMatch(config) );

     // test series not in there
     config.setNickname( "shava" );
     assertFalse( "no match", sampleSuite.hasRunNowMatch(config) );     
   }

  /**
   * test the mergeSeriesConfig function
   *
   * @throws Exception if problem running test
   */
  public void testMergeSeriesConfig() throws Exception {
    SuiteWrapper sampleSuite = createSampleSuite( null );
    int suiteLength = sampleSuite.getSeriesConfigCount();

    // test normal add
    SeriesConfig config = (SeriesConfig)sampleSuite.getSeriesConfig( 0 ).copy();
    config.setNickname( "addedSeries" );
    int count = sampleSuite.mergeSeriesConfig( config );
    assertEquals("correct count", 1, count);
    suiteLength++;
    assertEquals
      ("added config", suiteLength, sampleSuite.getSeriesConfigCount());

    // test normal delete
    config = (SeriesConfig)sampleSuite.getSeriesConfig( 0 ).copy();
    config.setNickname( "deletedSeries" );
    config.setAction( "delete" );
    count = sampleSuite.mergeSeriesConfig( config );
    assertEquals("correct count", 1, count);
    suiteLength++;
    assertEquals
      ("added config delete", suiteLength, sampleSuite.getSeriesConfigCount());

    // test merge
    config = (SeriesConfig)config.copy();
    config.setAction( "add" );
    count = sampleSuite.mergeSeriesConfig( config );
    assertEquals("correct count", -1, count);
    suiteLength--;
      assertEquals
        ("merged config", suiteLength, sampleSuite.getSeriesConfigCount());
   }

    /**
   * test the removeSeriesConfig function
   *
   * @throws Exception if problem running test
   */
  public void testRemoveSeriesConfig() throws Exception {
    SuiteWrapper sampleSuite = createSampleSuite( null );
    int suiteLength = sampleSuite.getSeriesConfigCount();

    // test delete
    SeriesConfig config = (SeriesConfig)sampleSuite.getSeriesConfig( 0 ).copy();
    sampleSuite.removeSeriesConfig( config );
    suiteLength--;
    assertEquals
      ("removed config", suiteLength, sampleSuite.getSeriesConfigCount());

   }
}
