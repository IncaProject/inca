package edu.sdsc.inca.consumer;

import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Properties;
import java.util.Timer;

import edu.sdsc.inca.ConsumerTest;
import edu.sdsc.inca.util.Constants;

/**
 * Tests the DepotQuery class.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class DepotQueryTest extends TestCase {
  private static Logger logger = Logger.getLogger( DepotQueryTest.class );

  /**
   * Verify the result is not null and contains a timestamp and return it.
   *
   * @param result A string containing the result of a query.
   *
   * @return  The timestamp retrieved from the result.
   */
  public static long getAndVerifyResult( String result ) {
    logger.info( "Received " + result );
    assertNotNull( "Found first result", result );
    Pattern pattern = Pattern.compile( "<time>(\\d+)</time>" );
    Matcher matcher = pattern.matcher( result );
    assertTrue( "got time string from first query", matcher.find() );
    return Long.parseLong( matcher.group(1) );
  }

  public void setUp() {
    File qs = new File( "var/" + DepotBean.QUERYSTORE );
    if ( qs.exists() ) qs.delete();
  }
  /**
   * Test caching abilities.
   *
   * @throws Exception if problem running test
   */
  public void testCache() throws Exception {
    File qFile = new File( "var/queries/q1.xml" );
    if ( qFile.exists() ) qFile.delete();

    ConsumerTest.ConsumerTester tester = new ConsumerTest.ConsumerTester();
    tester.start();
    DepotQuery.setDepotConfig( tester.consumer.getClientConfiguration() );

    // Before we start the query thread, there should be no result in the cache
    DepotQuery q1 = new DepotQuery
      ( 5, "*", "var", "q1", "queryHql", "select guid from suite" );
    String r1 = q1.getStoredResult();
    assertNull( "No result in cache yet", r1 );

    // We start the cache thread and wait a few seconds and verify that the
    // thread queried the depot and stored the result
    Timer timer = new Timer();
    timer.schedule
      ( q1, q1.getNextRefresh(), q1.getPeriod() * Constants.MILLIS_TO_SECOND );
    logger.info ( "\nTest 1 started\n" );
    logger.info( "query thread started" );
    Thread.sleep( 3000 );
    long firstQueryTime = getAndVerifyResult( q1.getStoredResult() );


    // We do another query and verify the query result got updated by
    // comparing timestamps
    logger.info ( "\nTest 2 started\n" );
    Thread.sleep( 6000 );
    logger.debug( "Fetching test 2 result" );
    long secondQueryTime = getAndVerifyResult( q1.getStoredResult() );
    assertTrue( "second query time later than first",
                secondQueryTime > firstQueryTime );


    // We stop the current cache thread and start a new one to emulate a
    // a consumer restart -- when it first starts, it should return the
    // the results from file because they're still fresh
    logger.info ( "\nTest 3 started\n" );
    timer.cancel();
    timer = new Timer();
    DepotQuery q2 = new DepotQuery
      ( 10, "*", "var", "q1", "queryHql", "select guid from suite" );
    logger.debug( "Next refresh " + q2.getNextRefresh() );
    timer.schedule( q2, q2.getNextRefresh(), q2.getPeriod() * Constants.MILLIS_TO_SECOND );
    Thread.sleep( 1000 );
    long thirdQueryTime = getAndVerifyResult( q2.getStoredResult() );
    assertTrue( "3rd query time is same as 2nd",
                thirdQueryTime == secondQueryTime );

    // now let's make sure, it updates properly
    logger.info ( "\nTest 4 started\n" );
    q2.setPeriod( 5 );
    Thread.sleep( 8000 );
    long fourthQueryTime = getAndVerifyResult( q2.getStoredResult() );
    long fourthResultWrite = q2.getCacheFile().lastModified();
    assertTrue( "4th query time is greater than 3rd",
                fourthQueryTime > thirdQueryTime );

    /*
    logger.info ( "\nTest 5 started\n" );
    Properties depotConfig = DepotQuery.getDepotConfig();
    depotConfig.setProperty( "depot", "inca://localhost:11111" );
    Thread.sleep( 5000 );
    long fifthResultWrite = q2.getCacheFile().lastModified();
    assertTrue( "5th file timestamp is same as 4th",
                fifthResultWrite == fourthResultWrite );
    */
    timer.cancel();

    logger.info( "Shutting down now" );

    tester.stop();
    logger.info( "Shut down complete" );


  }

  /**
   * Test filename translation
   *
   * @throws Exception if problem running test
   */
  public void testFile() throws Exception {

    DepotQuery q1 = new DepotQuery
      ( 5000, "*", "var", "q1", "queryHQL", "select guid from suite" );
    assertEquals( "regular filename", "q1.xml", q1.getCacheFile().getName() );

    q1.setQueryName( "My query is cool" );
    assertEquals( "spaces in filename",
                  "My_query_is_cool.xml", q1.getCacheFile().getName() );

    q1.setQueryName( "My  query is cool" );
    assertEquals( "two spaces in filename",
                  "My__query_is_cool.xml", q1.getCacheFile().getName() );
  }

  /**
   * Test query casting function
   *
   * @throws Exception if problem running test
   */
  public void testQueryCast() throws Exception {

    ConsumerTest.ConsumerTester tester = new ConsumerTest.ConsumerTester();
    tester.start();
    DepotQuery.setDepotConfig( tester.consumer.getClientConfiguration() );

    String results = DepotQuery.query( "queryInstance", 6L, 5L );
    getAndVerifyResult(results);

    tester.stop();
    logger.info( "Shut down complete" );


  }

}
