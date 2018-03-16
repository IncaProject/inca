package edu.sdsc.inca.consumer;

import junit.framework.TestCase;
import org.apache.log4j.Logger;


import java.io.File;

import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.util.ConfigProperties;
import edu.sdsc.inca.ConsumerTest;

/**
 * Test the class used to store queries.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class DepotBeanTest extends TestCase {
  private static Logger logger = Logger.getLogger( DepotBeanTest.class );
  private static File qsFile = new File( "var/queryStore.xml" );
  private static File qDir = new File( "var/queries" );


  /**
   * Delete any pre-existing files.
   */
  public void setUp() {
    if ( qsFile.exists() ) qsFile.delete();
    if ( qDir.exists() ) StringMethods.deleteDirectory( qDir );
    assertFalse( "No query file yet", qsFile.exists() );
    try {
      logger.debug( "\n\n--------------New Test -----------------" );
      Thread.sleep( 5000 );
    } catch (InterruptedException e) {
      logger.warn( "Interrupted while sleeping" );
    }
  }

  /**
   * Verify persistence by adding a few queries and then reading them back
   *
   * @throws Exception if trouble running test
   */
  public void testPersistence() throws Exception {

    // bean
    String[] sql = {
      "select id from suite",
      "select id from seriesconfig"
    };
    String[] names = { "q1", "q2" };

    // add bean
    DepotBean bean = new DepotBean( qsFile.getAbsolutePath(), "var" );
    assertTrue( "file exists", qsFile.exists() );
    assertFalse( "no bean yet", bean.hasQuery( "q1") );
    assertEquals( "0 bean yet", 0, bean.list().length );
    for( int i = 0; i < names.length; i++ ) {
      bean.add( names[i], "hql", sql[i] );
    }
    assertEquals( "query length correct", names.length, bean.list().length );


    // read the bean back and check entries
    DepotBean checkBean = new DepotBean( qsFile.getAbsolutePath(), "var" );
    for( int i = 0; i < names.length; i++ ) {
      assertTrue
        ("has query " + i, checkBean.hasQuery( names[i]) );
      assertEquals
        ("correct query type " + i, "queryHql", checkBean.getQueryType( names[i]) );
      assertEquals
        ("got correct hql for " + i, sql[i],
          checkBean.getQueryParams( names[i])[0] );
    }

    String[] queryList = checkBean.list();
    assertEquals( "correct list count", sql.length, queryList.length );
    for( int i = 0; i < sql.length; i++ ) {
      assertEquals( "list returned right for " + i, names[i], queryList[i] );
    }

    for( int i = 0; i < sql.length; i++ ) {
      checkBean.delete( names[i] );
    }
    assertEquals( "correct list count", 0, checkBean.list().length );
   }

  /**
   * Verify the ability to retrieve query results from non-cached and
   * cached queries.
   *
   * @throws Exception if trouble running test
   */
  public void testQueryCache() throws Exception {

    ConsumerTest.MockDepot tester = new ConsumerTest.MockDepot(8344);
    ConfigProperties config = new ConfigProperties();
    config.putAllTrimmed(System.getProperties(), "inca.consumer.");
    config.loadFromResource("inca.properties", "inca.consumer.");
    config.setProperty( "reload", "30" );
    config.setProperty( "depot", "inca://localhost:8344" );
    config.setProperty( "auth", "false" );
    if ( config.containsKey( "logfile" )) config.remove( "logfile" );
    tester.start();

    //  verify ability to get fresh query
    DepotBean bean = new DepotBean();
    bean.setBeanConfig( config  );
    bean.add( "q3", "hql", "select id from suite" );
    long firstResult = DepotQueryTest.getAndVerifyResult
      ( bean.getQueryResult( "q3") );
    long secondResult = DepotQueryTest.getAndVerifyResult
      ( bean.getQueryResult( "q3") );
    assertTrue( "Results successfully retrieved", secondResult > firstResult );

    // verify ability to get cached query (fresh and not)
    bean = new DepotBean();
    bean.setBeanConfig( config  );
    bean.startQueries();
    bean.add( 5, "*", "q4", "hql", "select id from suite" );
    Thread.sleep( 2000 );
    String r = bean.getQueryResult( "q4" );
    firstResult = DepotQueryTest.getAndVerifyResult( r );
    logger.info( "Got freshcached result " + r );

    Thread.sleep(1000);
    r = bean.getQueryResult( "q4" );
    logger.info( "Got cached result 2 "  + r );
    secondResult = DepotQueryTest.getAndVerifyResult( r );
    assertEquals( "Results successfully retrieved", secondResult, firstResult );
    Thread.sleep(5000);
    long thirdResult = DepotQueryTest.getAndVerifyResult
      ( bean.getQueryResult( "q4") );
    assertTrue( "Results successfully retrieved", thirdResult > firstResult );

    assertTrue( "query was deleted", bean.delete( "q4" ) );
    assertFalse( "verify query gone", bean.hasQuery("q4") );

    bean.stopQueries();
    tester.interrupt();
    tester.join();
  }

  public void testQueries() throws Exception {
    DepotBean depotBean = new DepotBean( qsFile.getAbsolutePath(), "var" );
    depotBean.add( "db", "database" );
    depotBean.add( "guids", "guids" );
    depotBean.add( "hql", "hql", "select s from Suite s" );
    depotBean.add( "instance", "instance", "5", "6" );
    depotBean.add( "latest", "latest", "where suite = 'x'" );
    depotBean.add
      ( "status", "statusHistory", "WEEK", "2008-01-05", "2008-05-05",
        "where suite = 'x'" );
    depotBean.save();

    new DepotBean(qsFile.getAbsolutePath(), "var");
  }

   public void testSuiteCaching() throws Exception {
    ConsumerTest.MockDepot tester = new ConsumerTest.MockDepot(8343);
    tester.statusHistoryDelay = 0;
    ConfigProperties config = new ConfigProperties();
    config.putAllTrimmed(System.getProperties(), "inca.consumer.");
    config.loadFromResource("inca.properties", "inca.consumer.");
    config.setProperty( "reload", "30" );
    config.setProperty( "depot", "inca://localhost:8343" );
    config.setProperty( "auth", "false" );
    if ( config.containsKey( "logfile" )) config.remove( "logfile" );
    tester.start();

    DepotBean depotBean = new DepotBean();

    depotBean.setBeanConfig( config );
    tester.addSuite
      ( "suiteA", new String[] {"<reportSummary><a/></reportSummary>"}, 0 );
    tester.addSuite
      ( "suiteB", new String[] {"<reportSummary><b/></reportSummary>"}, 15000 );

    depotBean.start();

    Thread.sleep(20000);
    for( String s : new String[]{ "suiteA", "suiteB" } ) {
      assertTrue
        ("query now in cache",depotBean.hasQuery(DepotBean.suiteCachePrefix+s));
    }
    depotBean.interrupt();
    depotBean.join();

    logger.info( "\n\n-----------Restart-------------\n\n" );

    tester.deleteSuite( "suiteA" );
    depotBean =  new DepotBean();
    depotBean.setBeanConfig( config );
    depotBean.setCacheReloadPeriod(5);
    depotBean.start();
    Thread.sleep(10000);
     logger.debug( "checking" );
    assertFalse
        ( "query deleted in cache",
          depotBean.hasQuery(DepotBean.suiteCachePrefix+"suiteA") );
    assertTrue
         ( "query still in cache",
           depotBean.hasQuery(DepotBean.suiteCachePrefix+"suiteB") );

    logger.info( "\n\n-----------Restart-------------\n\n" );
    tester.deleteSuite( "suiteB" );
    tester.addSuite
      ( "suiteC", new String[] {"<reportSummary><c/></reportSummary>"}, 0 );
    Thread.sleep( 10000 );
    assertFalse
        ( "query deleted in cache",
          depotBean.hasQuery(DepotBean.suiteCachePrefix+"suiteB") );
    assertTrue
         ( "query now in cache",
           depotBean.hasQuery(DepotBean.suiteCachePrefix+"suiteC") );

    logger.info( "Shutting down tester" );
    tester.interrupt();
    tester.join();
  }

}
