package edu.sdsc.inca.agent;

import junit.framework.TestCase;

import java.io.File;

import edu.sdsc.inca.dataModel.util.Macro;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.util.ResourcesWrapperTest;
import edu.sdsc.inca.util.SuiteWrapper;
import edu.sdsc.inca.util.SuiteWrapperTest;
import edu.sdsc.inca.util.SuiteStagesWrapper;
import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.repository.RepositoriesTest;
import edu.sdsc.inca.repository.Repositories;
import org.apache.log4j.Logger;

/**
 * Test suite table
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class SuiteTableTest extends TestCase {
  static Logger logger = Logger.getLogger(SuiteTableTest.class);

  public void testApplyResourceChanges() throws Exception {
    // set up temp repository of reporters the suites can refer too
    Repositories repositories = RepositoriesTest.createSampleRepository(null);
    RepositoryCache cache = RepositoryCacheTest.createSampleRepositoryCache(
      repositories
    );

    // test no change first
    StringMethods.deleteDirectory( new File("/tmp/suites") );
    ResourcesWrapper resources1 = ResourcesWrapperTest.createSampleResources();
    SuiteTable suites = new SuiteTable( "/tmp/suites", resources1 );
    SuiteStagesWrapper SuiteStagesWrapper = suites.getSuite( "TestSuite", resources1 );
    SuiteWrapper sampleSuite = SuiteWrapperTest.createSampleSuite(null);

    cache.resolveReporters( sampleSuite );
    SuiteStagesWrapper.modify( sampleSuite );
    suites.putSuite( SuiteStagesWrapper );
    ResourcesWrapper resources2 = ResourcesWrapperTest.createSampleResources();
    SuiteDocument[] changes = suites.applyResourceChanges( resources2 );
    assertNotNull( "SuiteStagesWrapper expanded returned", changes );
    assertNotNull( "SuiteStagesWrapper expanded returned", changes[0] );
    assertEquals(
      "no changes", 0,
      new SuiteWrapper(changes[0]).getSeriesConfigCount()
    );

    // changes a macro in the resource group teragrid which should cause
    // 4 report series configs to change
    Macro[] macros = (Macro[])resources2.getResourceConfigDocument().selectPath(
      "//resource[name='teragrid']/macros/macro[name='myproxyServer']"
    );
    assertEquals( "got macro", 1, macros.length );
    macros[0].setValueArray( new String[] { "myproxy.sdsc.edu"} );
    changes = suites.applyResourceChanges( resources2 );
    assertNotNull( "SuiteStagesWrapper expanded returned", changes );
    assertNotNull( "SuiteStagesWrapper expanded returned", changes[0] );
    assertEquals(  // should be 4 adds and 4 deletes
      "some changes", 8, new SuiteWrapper(changes[0]).getSeriesConfigCount()
    );
    Object[] adds = changes[0].selectPath(
    "//seriesConfig[action='add']//arg[value='myproxy.sdsc.edu']"
    );
    assertEquals( "got 4 adds", 4, adds.length );
    Object[] deletes = changes[0].selectPath(
    "//seriesConfig[action='delete']//arg[value='myproxy.teragrid.org']"
    );
    assertEquals( "got 4 deletes", 4, deletes.length );
  }

  /**
   * Test persistence of the suites.
   *
   * @throws Exception if trouble running test
   */
  public void testPersistence() throws Exception {
    Repositories repositories = RepositoriesTest.createSampleRepository(null);
    RepositoryCache cache = RepositoryCacheTest.createSampleRepositoryCache(
      repositories
    );
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    SuiteWrapper suite = new SuiteWrapper("test/samplesuitelocal.xml" );
    cache.resolveReporters( suite );
    StringMethods.deleteDirectory( new File("/tmp/suites") );
    SuiteTable suites = new SuiteTable( "/tmp/suites", resources );
    SuiteStagesWrapper blankSuiteStagesWrapper = suites.getSuite( "TestSuiteLocal", resources );
    assertEquals( "blank suite has 0 configs", 0,
                  blankSuiteStagesWrapper.getSeriesConfigCount() );
    blankSuiteStagesWrapper.modify( suite );
    suites.putSuite( blankSuiteStagesWrapper );
    assertEquals( "1 suite found", 1, suites.getNames().length );
    suites = new SuiteTable( "/tmp/suites", resources );
    assertEquals( "1 suite found", 1, suites.getNames().length );
    SuiteWrapper change = new SuiteWrapper("test/delete_samplesuitelocal.xml" );
    cache.resolveReporters( change );
    SuiteStagesWrapper sampleSuiteStagesWrapper = suites.getSuite( "TestSuiteLocal" );
    sampleSuiteStagesWrapper.modify( change );
    suites.putSuite( sampleSuiteStagesWrapper );
    assertEquals( "1 suite found", 1, suites.getNames().length );
    String delete = sampleSuiteStagesWrapper.getSuiteDocument().toString();
    delete = delete.replaceAll( "add", "delete" );
    SuiteDocument newDelete = SuiteDocument.Factory.parse(delete);
    SuiteWrapper deleteChange = new SuiteWrapper( newDelete );
    cache.resolveReporters( deleteChange );    
    sampleSuiteStagesWrapper.modify( deleteChange );
    assertEquals(
      "0 configs now", 0, sampleSuiteStagesWrapper.getSeriesConfigCount()
    );
    suites.putSuite( sampleSuiteStagesWrapper );
    assertEquals( "0 suites found", 0, suites.getNames().length );
  }

}
