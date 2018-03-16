package edu.sdsc.inca;

import junit.framework.TestCase;
import edu.sdsc.inca.protocol.Protocol;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class WrapConfigTest extends TestCase {

  private static final String[] SUITES = {
    "<suite>\n" +
    "  <seriesConfigs>\n" +
    "    <seriesConfig>\n" +
    "      <series>\n" +
    "        <name>cluster.accounting.tgusage.version</name>\n" +
    "        <args>\n" +
    "          <arg>\n" +
    "            <name>version</name>\n" +
    "            <value>no</value>\n" +
    "          </arg>\n" +
    "          <arg>\n" +
    "            <name>verbose</name>\n" +
    "            <value>1</value>\n" +
    "          </arg>\n" +
    "          <arg>\n" +
    "            <name>help</name>\n" +
    "            <value>no</value>\n" +
    "          </arg>\n" +
    "          <arg>\n" +
    "            <name>log</name>\n" +
    "            <value>0</value>\n" +
    "          </arg>\n" +
    "        </args>\n" +
    "        <limits/>\n" +
    "        <context>@@</context>\n" +
    "      </series>\n" +
    "      <resourceSetName>defaultvo</resourceSetName>\n" +
    "      <schedule>\n" +
    "        <cron>\n" +
    "          <min>*</min>\n" +
    "          <hour>*</hour>\n" +
    "          <mday>*</mday>\n" +
    "          <wday>*</wday>\n" +
    "          <month>*</month>\n" +
    "        </cron>\n" +
    "      </schedule>\n" +
    "      <action>none</action>\n" +
    "    </seriesConfig>\n" +
    "  </seriesConfigs>\n" +
    "  <name xmlns=\"\">FirstSuite</name>\n" +
    "  <guid xmlns=\"\">client64-51.sdsc.edu:6323/FirstSuite</guid>\n" +
    "  <description xmlns=\"\"/>\n" +
    "</suite>\n",

    "<suite>\n" +
    "  <seriesConfigs>\n" +
    "    <seriesConfig>\n" +
    "      <series>\n" +
    "        <name>cluster.lang.any.unit</name>\n" +
    "        <args>\n" +
    "          <arg>\n" +
    "            <name>version</name>\n" +
    "            <value>no</value>\n" +
    "          </arg>\n" +
    "          <arg>\n" +
    "            <name>verbose</name>\n" +
    "            <value>1</value>\n" +
    "          </arg>\n" +
    "          <arg>\n" +
    "            <name>help</name>\n" +
    "            <value>no</value>\n" +
    "          </arg>\n" +
    "          <arg>\n" +
    "            <name>log</name>\n" +
    "            <value>0</value>\n" +
    "          </arg>\n" +
    "        </args>\n" +
    "        <limits/>\n" +
    "        <context>@@</context>\n" +
    "      </series>\n" +
    "      <resourceSetName>defaultvo</resourceSetName>\n" +
    "      <schedule>\n" +
    "        <cron>\n" +
    "          <min>*</min>\n" +
    "          <hour>*</hour>\n" +
    "          <mday>*</mday>\n" +
    "          <wday>*</wday>\n" +
    "          <month>*</month>\n" +
    "        </cron>\n" +
    "      </schedule>\n" +
    "      <action>none</action>\n" +
    "    </seriesConfig>\n" +
    "  </seriesConfigs>\n" +
    "  <name xmlns=\"\">SecondSuite</name>\n" +
    "  <guid xmlns=\"\">client64-51.sdsc.edu:6323/SecondSuite</guid>\n" +
    "  <description xmlns=\"\"/>\n" +
    "</suite>\n",

    "<suite>\n" +
    "  <seriesConfigs>\n" +
    "    <seriesConfig>\n" +
    "      <series>\n" +
    "        <name>cluster.math.atlas.version</name>\n" +
    "        <args>\n" +
    "          <arg>\n" +
    "            <name>cc</name>\n" +
    "            <value>cc</value>\n" +
    "          </arg>\n" +
    "          <arg>\n" +
    "            <name>dir</name>\n" +
    "            <value>ATLAS_HOME</value>\n" +
    "          </arg>\n" +
    "          <arg>\n" +
    "            <name>help</name>\n" +
    "            <value>no</value>\n" +
    "          </arg>\n" +
    "          <arg>\n" +
    "            <name>log</name>\n" +
    "            <value>3</value>\n" +
    "          </arg>\n" +
    "          <arg>\n" +
    "            <name>verbose</name>\n" +
    "            <value>1</value>\n" +
    "          </arg>\n" +
    "          <arg>\n" +
    "            <name>version</name>\n" +
    "            <value>no</value>\n" +
    "          </arg>\n" +
    "        </args>\n" +
    "        <context>@@</context>\n" +
    "      </series>\n" +
    "      <resourceSetName>defaultvo</resourceSetName>\n" +
    "      <schedule>\n" +
    "        <cron>\n" +
    "          <min>1-59/10</min>\n" +
    "          <hour>*</hour>\n" +
    "          <mday>*</mday>\n" +
    "          <wday>*</wday>\n" +
    "          <month>*</month>\n" +
    "        </cron>\n" +
    "      </schedule>\n" +
    "      <action>none</action>\n" +
    "    </seriesConfig>\n" +
    "  </seriesConfigs>\n" +
    "  <name>ThirdSuite</name>\n" +
    "  <guid>client64-51.sdsc.edu:6323/ThirdSuite</guid>\n" +
    "  <description/>\n" +
    "</suite>\n"

  };

  /**
   * Tests the WrapConfig constructor.
   */
  public void testConstructor() throws Exception {

    WrapConfig[] configs = {
      createConfig(SUITES[0] + SUITES[1]),
      createConfig(SUITES[0]),
      createConfig(SUITES[1]),
      createConfig(SUITES[0] + SUITES[1] + SUITES[0])
    };
    int[] suiteCount = { 2, 1, 1, 3 };
    for(int i = 0; i < configs.length; i++) {
      assertEquals(suiteCount[i], configs[i].getSuites().length);
    }

  }

  /**
   * Tests the WrapConfig differences method w/no suites.
   */
  public void testDifferencesNoSuites() throws Exception {
    WrapConfig c1 = createConfig("");
    WrapConfig c2 = createConfig("");
    WrapConfig diff = c1.differences(c2);
    assertEquals(0, diff.getSuites().length);
    assertNotNull(c1.getRepositories());
    assertNotNull(c1.getResources());
    assertNull(diff.getRepositories());
    assertNull(diff.getResources());
  }

  /**
   * Tests the WrapConfig differences method when adding suites.
   */
  public void testDifferencesAddSuites() throws Exception {
    WrapConfig c1 = createConfig("");
    WrapConfig c2 = createConfig(SUITES[0] + SUITES[1]);
    WrapConfig diff = c1.differences(c2);
    assertEquals(2, diff.getSuites().length);
  }

  /**
   * Tests the WrapConfig differences method when removing suites.
   */
  public void testDifferencesDeleteSeries() throws Exception {
    WrapConfig c1 = createConfig(SUITES[0] + SUITES[1]);
    WrapConfig c2 = createConfig("");
    WrapConfig diff = c1.differences(c2);
    assertEquals(2, diff.getSuites().length);
  }

  /**
   * Tests the WrapConfig differences method on equivalent configs.
   */
  public void testDifferencesEqual() throws Exception {
    WrapConfig c1 = createConfig(SUITES[0] + SUITES[1]);
    WrapConfig c2 = createConfig(SUITES[0] + SUITES[1]);
    WrapConfig diff = c1.differences(c2);
    assertEquals(0, diff.getSuites().length);
  }

  /**
   * Tests the WrapConfig differences method on rearranged suites.
   */
  public void testDifferencesRearranged() throws Exception {
    WrapConfig c1 = createConfig(SUITES[0] + SUITES[1]);
    WrapConfig c2 = createConfig(SUITES[1] + SUITES[0]);
    WrapConfig diff = c1.differences(c2);
    assertEquals(0, diff.getSuites().length);
  }

  /**
   * Tests the WrapConfig differences method w/both additions and deletions.
   */
  public void testDifferencesMultiple() throws Exception {
    WrapConfig c1 = createConfig(SUITES[0] + SUITES[1]);
    WrapConfig c2 = createConfig(SUITES[1] + SUITES[2]);
    WrapConfig diff = c1.differences(c2);
    assertEquals(2, diff.getSuites().length);
  }

  /**
   * Tests the WrapConfig differences where suites have been changed.
   */
  public void testDifferencesModifications() throws Exception {
    WrapConfig c1 = createConfig(SUITES[0] + SUITES[1] + SUITES[2]);
    WrapConfig c2 = createConfig(SUITES[1] + SUITES[2] + SUITES[0]);
    WrapSuite suite = c2.getSuites()[0];
    WrapSeries series = suite.addNewSeries();
    series.setReporter("any.reporter");
    suite = c2.getSuites()[2];
    suite.removeSeries(suite.getSeriesAt(0));
    WrapConfig diff = c1.differences(c2);
    assertEquals(2, diff.getSuites().length);
  }
  /**
   * Returns an Inca configuration that contains specified suites.
   *
   * @param suites the Suite XML to include in the configuration
   * @return an Inca configuration
   */
  private WrapConfig createConfig(String suites) throws Exception {
    return new WrapConfig(
      "<inca:inca xmlns:inca='http://inca.sdsc.edu/dataModel/inca_2.0'>\n" +
      "<repositories>\n" +
      "  <repository>http://inca.sdsc.edu/2.0/repository</repository>\n" +
      "</repositories>\n" +
      "<resourceConfig>\n" +
      "  <resources xmlns=\"\">\n" +
      "    <resource>\n" +
      "      <name>client64-51.sdsc.edu</name>\n" +
      "    </resource>\n" +
      "  </resources>\n" +
      "</resourceConfig>\n" +
      "<suites>\n" +
      suites +
      "</suites>\n" +
      "</inca:inca>"
    );
  }

}
