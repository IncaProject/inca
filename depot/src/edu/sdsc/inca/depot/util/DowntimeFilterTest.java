package edu.sdsc.inca.depot.util;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import java.util.regex.Pattern;

import java.io.File;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * Test DowntimeFilter class
 *
 * @author Kate Kaya &lt;kate@sdsc.edu&gt;
 */

/**
 * Test the class used to append downtimes
 */
public class DowntimeFilterTest extends TestCase {
  private static Logger logger = Logger.getLogger( DowntimeFilterTest.class );
  private static File downProp = new File( "etc/downtime.properties" );

  /**
   * Delete any pre-existing files.  Create default properties files and set
   * cache refresh to one minute.
   */
  public void setUp() throws Exception  {
    if ( downProp.exists() ) downProp.delete();
    downProp.createNewFile();
    System.setProperty("inca.depot.downtimeRefresh", "1");
  }

  /**
   * Create a basic TeraGridFilter
   */
  public DowntimeFilter getFilter(){
    DowntimeFilter filter = new DowntimeFilter();
    filter.setContext("orig context");
    filter.setResource("orig-resource");
    filter.setStdout("<errorMessage>orig stdout");
    return filter;
  }

  /**
   * Test filter
   *
   * @throws Exception
   */
  public void testFilter() throws Exception {

    // original context, nothing in downtime or filter files
    String out = getFilter().getStdout();
    logger.debug( "stdout: "+ out );
    assertTrue( "returns original stdout", Pattern.matches(
        "^<errorMessage>orig stdout$", out) );

    // set a resource as down
    Writer writeDowntime = new BufferedWriter(new FileWriter(downProp));
    writeDowntime.write("orig-resource=123\n");
    writeDowntime.close();
    logger.debug( "Sleeping 65 seconds" );
    Thread.sleep(65000);
    String out2 = getFilter().getStdout();
    logger.debug( "stdout: "+ out2);
    assertTrue( "down orig resource", Pattern.matches(
        "^<errorMessage>DOWNTIME:123: orig stdout$", out2) );


    // clear files, back to original output
    downProp.delete();
    downProp.createNewFile();
    logger.debug( "Sleeping 60 seconds" );
    Thread.sleep(60000);
    String out5 = getFilter().getStdout();
    logger.debug( "stdout: "+ out5 );
    assertTrue( "returns original stdout", Pattern.matches(
        "^<errorMessage>orig stdout$",  out5) );
  }
}
