package edu.sdsc.inca.depot.util;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import java.util.regex.Pattern;
import java.io.*;

import edu.sdsc.inca.depot.util.All2AllFilter;

/**
 * Test All2AllFilter class
 *
 * @author Kate Ericson &lt;kericson@sdsc.edu&gt;
 */

/**
 * Test the class used to filter All2All reports
 */
public class All2AllFilterTest extends TestCase {
  private static Logger logger = Logger.getLogger( All2AllFilterTest.class );
  private static File summaryProp;

  /**
   * Delete any pre-existing files.  Create default properties file
   */
  public void setUp() throws Exception  {
    summaryProp = File.createTempFile( "summary", "pro" );
    System.setProperty("inca.summaryFile", summaryProp.getAbsolutePath());
  }

  /**
   * Create a basic All2AllFilter
   */
  public All2AllFilter getFilter(String context, String report, String resource){
    All2AllFilter filter = new All2AllFilter();
    filter.setContext(context);
    filter.setStdout(report);
    filter.setResource(resource);
    return filter;
  }

  /**
   * Test filter
   *
   * @throws Exception
   */
  public void testFilter() throws Exception {
    All2AllFilter filter = getFilter(
        "nickname=all2all:gsissh_to_remoteResource " +
            "summary.successpct.performance", downErrorReport(), "repo");
    // determine this is a summary all2all related reporter
    // but no action because the report error was during downtime
    filter.getStdout();
    String prop = readFile(summaryProp.toString());
    logger.debug( "Prop: " + prop);
    assertTrue( "nothing in summary properties", Pattern.matches("", prop) );

    All2AllFilter filter2 = getFilter(
        "nickname=all2all:gram_to_remoteResource " +
            "summary.successpct.performance", errorReport(), "repo");
    // determine this is a summary all2all related reporter
    // check error was written to file
    filter2.getStdout();
    String prop2 = readFile(summaryProp.toString());
    logger.debug( "Prop2: " + prop2);
    Pattern p2 = Pattern.compile("^gram_to_remoteResource=1$",
        Pattern.MULTILINE);
    assertTrue( "error in summary properties", p2.matcher(prop2).find() );

    All2AllFilter filter3 = getFilter(
        "nickname=all2all:gsissh_to_remoteResource " +
            "summary.successpct.performance", passReport(), "repo");
    // determine this is a summary all2all related reporter
    // check success was written to file and old error is still in file
    filter3.getStdout();
    String prop3 = readFile(summaryProp.toString());
    logger.debug( "Prop3: " + prop3);
    Pattern p3 = Pattern.compile("^gsissh_to_remoteResource=19$" +
        "\n^gram_to_remoteResource=1$",
        Pattern.MULTILINE);
    assertTrue( "success in summary properties", p3.matcher(prop3).find() );

    All2AllFilter filter4 = getFilter(
        "nickname=all2all:gram_to_remoteResource",
        rmErrorReport(), "someResource");
    // determine this is a regular all2all related reporter
    // check error message modified to be not at fault since less than
    // two resources can use gram to remoteResource
    String out4 = filter4.getStdout();
    logger.debug( "Out4: " + out4);
    Pattern p4 = Pattern.compile(".*<errorMessage>NOT_AT_FAULT: all2all error.*",
        Pattern.MULTILINE);
    assertTrue( "modified err message", p4.matcher(out4).find() );

    All2AllFilter filter5 = getFilter(
        "nickname=all2all:gsissh_to_remoteResource",
        rmErrorReport(), "someResource");
    // determine this is a regular all2all related reporter
    // check error message NOT modified since at least two resources can
    // gsissh to remoteResource
    String out5 = filter5.getStdout();
    logger.debug( "Out5: " + out5);
    Pattern p5 = Pattern.compile(".*<errorMessage>all2all error.*",
        Pattern.MULTILINE);
    assertTrue( "err message not modified", p5.matcher(out5).find() );

  }

  private static String readFile(String filename) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    String nextLine;
    StringBuffer sb = new StringBuffer();
    while ((nextLine = br.readLine()) != null) {
      sb.append(nextLine);
      sb.append("\n");
    }
    return sb.toString();
  }

  private static String downErrorReport(){
    return "<rep:report xmlns:rep='http://inca.sdsc.edu/dataModel/report_2.1'>\n" +
        "    <gmt>2008-07-28T16:25:05.000-07:00</gmt>\n" +
        "    <hostname>tg-login1.uc.teragrid.org</hostname>\n" +
        "    <name>summary.successpct.performance</name>\n" +
        "    <version>10</version>\n" +
        "    <workingDir>/home/inca/inca2install-ia64</workingDir>\n" +
        "    <reporterPath>/path/summary.successpct.performance</reporterPath>\n" +
        "    <args>\n" +
        "      <arg>\n" +
        "        <name>host</name>\n" +
        "        <value>gatekeeper.bigred.iu.teragrid.org</value>\n" +
        "      </arg>\n" +
        "    </args>\n" +
        "    <body>\n" +
        "      <unitTest xmlns:rep=\"http://inca.sdsc.edu/dataModel/report_2.1\">\n" +
        "        <ID>remoteLogin</ID>\n" +
        "      </unitTest>\n" +
        "    </body>\n" +
        "    <exitStatus>\n" +
        "      <completed>false</completed>\n" +
        "      <errorMessage>DOWNTIME: resource out</errorMessage>\n" +
        "    </exitStatus>\n" +
        "  </rep:report>";
  }

    private static String rmErrorReport(){
    return "<rep:report xmlns:rep='http://inca.sdsc.edu/dataModel/report_2.1'>\n" +
        "    <gmt>2008-07-28T16:25:05.000-07:00</gmt>\n" +
        "    <hostname>tg-login1.uc.teragrid.org</hostname>\n" +
        "    <name>summary.successpct.performance</name>\n" +
        "    <version>10</version>\n" +
        "    <workingDir>/home/inca/inca2install-ia64</workingDir>\n" +
        "    <reporterPath>/path/summary.successpct.performance</reporterPath>\n" +
        "    <args>\n" +
        "      <arg>\n" +
        "        <name>host</name>\n" +
        "        <value>gatekeeper.bigred.iu.teragrid.org</value>\n" +
        "      </arg>\n" +
        "    </args>\n" +
        "    <body>\n" +
        "      <unitTest xmlns:rep=\"http://inca.sdsc.edu/dataModel/report_2.1\">\n" +
        "        <ID>remoteLogin</ID>\n" +
        "      </unitTest>\n" +
        "    </body>\n" +
        "    <exitStatus>\n" +
        "      <completed>false</completed>\n" +
        "      <errorMessage>all2all error</errorMessage>\n" +
        "    </exitStatus>\n" +
        "  </rep:report>";
  }

  private static String errorReport(){
    return "<rep:report xmlns:rep='http://inca.sdsc.edu/dataModel/report_2.1'>\n" +
        "    <gmt>2008-07-29T23:48:51.000-07:00</gmt>\n" +
        "    <hostname>tg-login1.sdsc.teragrid.org</hostname>\n" +
        "    <name>summary.successpct.performance</name>\n" +
        "    <version>1</version>\n" +
        "    <workingDir>/users/inca/inca2install</workingDir>\n" +
        "    <reporterPath>/users/inca/inca2install/var/reporter-packages/bin/summary.successpct.performance-1</reporterPath>\n" +
        "    <args>\n" +
        "      <arg>\n" +
        "        <name>agent</name>\n" +
        "        <value/>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>filter</name>\n" +
        "        <value>all2all:gsissh_to_tg-login1.sdsc.teragrid.org</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>suite</name>\n" +
        "        <value>ctss</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>version</name>\n" +
        "        <value>no</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>type</name>\n" +
        "        <value>consumer</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>log</name>\n" +
        "        <value>3</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>help</name>\n" +
        "        <value>no</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>server</name>\n" +
        "        <value>sapa.sdsc.edu</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>verbose</name>\n" +
        "        <value>1</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>selector</name>\n" +
        "        <value/>\n" +
        "      </arg>\n" +
        "    </args>\n" +
        "    <log>\n" +
        "      <system xmlns:rep=\"http://inca.sdsc.edu/dataModel/report_2.1\">\n" +
        "        <gmt>2008-07-30T06:48:50Z</gmt>\n" +
        "        <message>wget -O - 'http://sapa.sdsc.edu:8080/inca/jsp/query.jsp?action=View&amp;qname=incaQueryLatest%2Bincas:__sapa.sdsc.edu:6323_ctss&amp;escapeXml=false'</message>\n" +
        "      </system>\n" +
        "    </log>\n" +
        "    <body>\n" +
        "      <performance xmlns:rep=\"http://inca.sdsc.edu/dataModel/report_2.1\">\n" +
        "        <ID>successpct</ID>\n" +
        "        <benchmark>\n" +
        "          <ID>successpct</ID>\n" +
        "          <parameters>\n" +
        "            <parameter>\n" +
        "              <ID>filter</ID>\n" +
        "              <value>all2all:gsissh_to_tg-login1.sdsc.teragrid.org</value>\n" +
        "            </parameter>\n" +
        "            <parameter>\n" +
        "              <ID>selector</ID>\n" +
        "              <value>incaQueryLatest%2Bincas:__sapa.sdsc.edu:6323_ctss</value>\n" +
        "            </parameter>\n" +
        "            <parameter>\n" +
        "              <ID>server</ID>\n" +
        "              <value>sapa.sdsc.edu:8080</value>\n" +
        "            </parameter>\n" +
        "          </parameters>\n" +
        "          <statistics>\n" +
        "            <statistic>\n" +
        "              <ID>all2all:gram_to_remoteResource-fail</ID>\n" +
        "              <value>4</value>\n" +
        "            </statistic>\n" +
        "            <statistic>\n" +
        "              <ID>all2all:gram_to_remoteResource-pct</ID>\n" +
        "              <value>100</value>\n" +
        "            </statistic>\n" +
        "            <statistic>\n" +
        "              <ID>all2all:gram_to_remoteResource-success</ID>\n" +
        "              <value>1</value>\n" +
        "            </statistic>\n" +
        "          </statistics>\n" +
        "        </benchmark>\n" +
        "      </performance>\n" +
        "    </body>\n" +
        "    <exitStatus>\n" +
        "      <completed>true</completed>\n" +
        "      <errorMessage/>\n" +
        "    </exitStatus>\n" +
        "  </rep:report>";
  }

  private static String passReport(){
    return "<rep:report xmlns:rep='http://inca.sdsc.edu/dataModel/report_2.1'>\n" +
        "    <gmt>2008-07-29T23:48:51.000-07:00</gmt>\n" +
        "    <hostname>tg-login1.sdsc.teragrid.org</hostname>\n" +
        "    <name>summary.successpct.performance</name>\n" +
        "    <version>1</version>\n" +
        "    <workingDir>/users/inca/inca2install</workingDir>\n" +
        "    <reporterPath>/users/inca/inca2install/var/reporter-packages/bin/summary.successpct.performance-1</reporterPath>\n" +
        "    <args>\n" +
        "      <arg>\n" +
        "        <name>agent</name>\n" +
        "        <value/>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>filter</name>\n" +
        "        <value>all2all:gsissh_to_tg-login1.sdsc.teragrid.org</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>suite</name>\n" +
        "        <value>ctss</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>version</name>\n" +
        "        <value>no</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>type</name>\n" +
        "        <value>consumer</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>log</name>\n" +
        "        <value>3</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>help</name>\n" +
        "        <value>no</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>server</name>\n" +
        "        <value>sapa.sdsc.edu</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>verbose</name>\n" +
        "        <value>1</value>\n" +
        "      </arg>\n" +
        "      <arg>\n" +
        "        <name>selector</name>\n" +
        "        <value/>\n" +
        "      </arg>\n" +
        "    </args>\n" +
        "    <log>\n" +
        "      <system xmlns:rep=\"http://inca.sdsc.edu/dataModel/report_2.1\">\n" +
        "        <gmt>2008-07-30T06:48:50Z</gmt>\n" +
        "        <message>wget -O - 'http://sapa.sdsc.edu:8080/inca/jsp/query.jsp?action=View&amp;qname=incaQueryLatest%2Bincas:__sapa.sdsc.edu:6323_ctss&amp;escapeXml=false'</message>\n" +
        "      </system>\n" +
        "    </log>\n" +
        "    <body>\n" +
        "      <performance xmlns:rep=\"http://inca.sdsc.edu/dataModel/report_2.1\">\n" +
        "        <ID>successpct</ID>\n" +
        "        <benchmark>\n" +
        "          <ID>successpct</ID>\n" +
        "          <parameters>\n" +
        "            <parameter>\n" +
        "              <ID>filter</ID>\n" +
        "              <value>all2all:gsissh_to_tg-login1.sdsc.teragrid.org</value>\n" +
        "            </parameter>\n" +
        "            <parameter>\n" +
        "              <ID>selector</ID>\n" +
        "              <value>incaQueryLatest%2Bincas:__sapa.sdsc.edu:6323_ctss</value>\n" +
        "            </parameter>\n" +
        "            <parameter>\n" +
        "              <ID>server</ID>\n" +
        "              <value>sapa.sdsc.edu:8080</value>\n" +
        "            </parameter>\n" +
        "          </parameters>\n" +
        "          <statistics>\n" +
        "            <statistic>\n" +
        "              <ID>all2all:gsissh_to_remoteResource-fail</ID>\n" +
        "              <value>0</value>\n" +
        "            </statistic>\n" +
        "            <statistic>\n" +
        "              <ID>all2all:gsissh_to_remoteResource-pct</ID>\n" +
        "              <value>100</value>\n" +
        "            </statistic>\n" +
        "            <statistic>\n" +
        "              <ID>all2all:gsissh_to_remoteResource-success</ID>\n" +
        "              <value>19</value>\n" +
        "            </statistic>\n" +
        "          </statistics>\n" +
        "        </benchmark>\n" +
        "      </performance>\n" +
        "    </body>\n" +
        "    <exitStatus>\n" +
        "      <completed>true</completed>\n" +
        "      <errorMessage/>\n" +
        "    </exitStatus>\n" +
        "  </rep:report>";
  }


}
