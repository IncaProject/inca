package edu.sdsc.inca.util;

import edu.sdsc.inca.dataModel.util.AcceptedOutput;
import edu.sdsc.inca.dataModel.util.Notifications;
import junit.framework.TestCase;
import edu.sdsc.inca.dataModel.util.SeriesConfig;
import edu.sdsc.inca.dataModel.util.Series;
import edu.sdsc.inca.dataModel.util.Args;
import edu.sdsc.inca.dataModel.util.Resource;
import edu.sdsc.inca.dataModel.util.Limits;
import edu.sdsc.inca.dataModel.util.Macro;
import edu.sdsc.inca.dataModel.util.Schedule;
import edu.sdsc.inca.dataModel.resourceConfig.ResourceConfigDocument;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.repository.Repositories;
import edu.sdsc.inca.repository.RepositoriesTest;

import java.io.File;
import java.util.Vector;
import java.util.HashMap;
import java.util.Calendar;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class SuiteStagesWrapperTest extends TestCase {
  private static Logger logger = Logger.getLogger(SuiteStagesWrapperTest.class);
  final static String[] resources = {
    "tg-login.sdsc.teragrid.org",
    "localhost",
    "tg-login.ncsa.teragrid.org",
    "dslogin.sdsc.edu" };

  /**
   * Uncomment to test out a real incat change file.
   *
   * @throws Exception
   *
  public void testRealFiles() throws Exception {
    IncaDocument incaDoc = IncaDocument.Factory.parse( new File("incat2.xml") );
    ResourceConfigDocument rcDoc = ResourceConfigDocument.Factory.newInstance();
    rcDoc.setResourceConfig( incaDoc.getInca().getResourceConfig() );
    ResourcesWrapper resources = new ResourcesWrapper( rcDoc  );
    SuiteDocument suiteDoc = SuiteDocument.Factory.newInstance();
    Suite[] suites = incaDoc.getInca().getSuites().getSuiteArray();
    for ( int i = 0; i < suites.length; i++ ) {
      if ( suites[i].getName().equals( "ctss-v3") ) {
        suiteDoc.setSuite(  suites[i] );
      }
    }
    File suiteFile = File.createTempFile( "inca", ".xml" );
    if ( suiteFile.exists() ) suiteFile.delete();
    SuiteStagesWrapper suite = new SuiteStagesWrapper(
      suiteFile.getAbsolutePath(), resources
    );
    SuiteWrapper suiteChange = new SuiteWrapper( suiteDoc );
    suite.modify( suiteChange );
    suite.save();
  }*/

  /**
   * Get some stats on the reporters configured for each resource
   *
  public void testGetStats() throws Exception {
    FileWriter writer = new FileWriter( "statFile.txt" );
    SuiteStagesWrapper suite = new SuiteStagesWrapper
      ( "ctss-v3.xml", new ResourcesWrapper() );
    HashMap resourceSuites = suite.getResourceSuites();
    Iterator suiteIterator = resourceSuites.keySet().iterator();
    while( suiteIterator.hasNext() ) {
      String resource = (String)suiteIterator.next();
      SuiteWrapper resourceSuite = (SuiteWrapper)resourceSuites.get( resource
      );
      int[] counts = resourceSuite.getSeriesConfigCountByPattern(
        new String[] { "all2all:gram", "all2all:gridftp", "all2all:ssh" },
        new String[] { "grid", "cluster", "data", "user" }      );
      writer.write( resource );
      for ( int j = 0; j < counts.length; j++ ) {
        writer.write( " " + counts[j] );
      }      writer.write( "\n" );
    }
    writer.close();
  }*/

  /**
   * Test the ability to get the cross product of a report series args
   *
   * @throws Exception
   */
  public void testArgCrossProduct() throws Exception {
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();

    /************************************************************************/
    Args orig_args = Args.Factory.newInstance();
    Args.Arg argVerbose = orig_args.addNewArg();
    argVerbose.setName( "verbose");
    argVerbose.setValue( "0" );

    Args.Arg argHelp = orig_args.addNewArg();
    argHelp.setName( "help");
    argHelp.setValue( "no" );

    Vector expanded_args = resources.expand(
      orig_args.toString(),
      "tg-login.sdsc.teragrid.org",
      null,
      new Vector<String>()
    );
    String[][][] expected_expanded_args = new String[][][]{
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"}
      }
    };
    argVectorEquals( "no expansion", expected_expanded_args, expanded_args );

    /**********************************************************************/
    // first expansion
    Args.Arg argTgcp = orig_args.addNewArg();
    argTgcp.setName( "server");
    argTgcp.setValue( "@tgcpServers@");

    expanded_args = resources.expand(
      orig_args.toString(),
      "tg-login.sdsc.teragrid.org",
      null,
      new Vector<String>()
    );
    expected_expanded_args = new String[][][]{
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.sdsc.teragrid.org -P 2812"}
      },
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.ncsa.teragrid.org -P 2812"}
      },
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.uc.teragrid.org -P 2812"}
      }
    };
    argVectorEquals( "one expansion", expected_expanded_args, expanded_args );

    /**********************************************************************/
    // single value substitution
    Args.Arg argRM = orig_args.addNewArg();
    argRM.setName( "rmLoc");
    argRM.setValue( "@" + Protocol.WORKING_DIR_MACRO + "@");

    expanded_args = resources.expand(
      orig_args.toString(),
      "tg-login.sdsc.teragrid.org",
      null,
      new Vector<String>()
    );
    expected_expanded_args = new String[][][]{
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.sdsc.teragrid.org -P 2812"},
        new String[]{"rmLoc", "/users/ssmallen/inca2"}
      },
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.ncsa.teragrid.org -P 2812"},
        new String[]{"rmLoc", "/users/ssmallen/inca2"}
      },
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.uc.teragrid.org -P 2812"},
        new String[]{"rmLoc", "/users/ssmallen/inca2"}
      }
    };
    argVectorEquals( "substitution", expected_expanded_args, expanded_args );

    /**********************************************************************/
    // another multi value substitution

    Args.Arg argFlavors = orig_args.addNewArg();
    argFlavors.setName( "flavor");
    argFlavors.setValue( "@globusFlavors@");

    expanded_args = resources.expand(
      orig_args.toString(),
      "tg-login.sdsc.teragrid.org",
      null,
      new Vector<String>()
    );
    expected_expanded_args = new String[][][]{
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.sdsc.teragrid.org -P 2812"},
        new String[]{"rmLoc", "/users/ssmallen/inca2"},
        new String[]{"flavor", "gcc64dbg"}
      },
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.sdsc.teragrid.org -P 2812"},
        new String[]{"rmLoc", "/users/ssmallen/inca2"},
        new String[]{"flavor", "vendorcc64dbg"}
      },
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.ncsa.teragrid.org -P 2812"},
        new String[]{"rmLoc", "/users/ssmallen/inca2"},
        new String[]{"flavor", "gcc64dbg"}
      },
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.ncsa.teragrid.org -P 2812"},
        new String[]{"rmLoc", "/users/ssmallen/inca2"},
        new String[]{"flavor", "vendorcc64dbg"}
      },
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.uc.teragrid.org -P 2812"},
        new String[]{"rmLoc", "/users/ssmallen/inca2"},
        new String[]{"flavor", "gcc64dbg"}
      },
      new String[][]{
        new String[]{"verbose", "0"},
        new String[]{"help", "no"},
        new String[]{"server", "tg-gridftp.uc.teragrid.org -P 2812"},
        new String[]{"rmLoc", "/users/ssmallen/inca2"},
        new String[]{"flavor", "vendorcc64dbg"}
      },
    };
    argVectorEquals( "substitution", expected_expanded_args, expanded_args );

  }

  /**
   * Test good behavior when dealing with a blank suite
   *
   * @throws Exception
   */
  public void testBlankSuite() throws Exception {
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    File suiteFile = File.createTempFile( "inca", "xml" );
    if ( suiteFile.exists() ) suiteFile.delete();
    SuiteStagesWrapper suiteStagesWrapper = new SuiteStagesWrapper( suiteFile.getAbsolutePath(), resources );
    assertTrue( "blank file saved to disk", suiteFile.exists() );

    assertEquals(
      "got 0 configs in expanded", 0, suiteStagesWrapper.getSeriesConfigCount() );
    assertEquals(
      "got 0 configs in expanded", 0, suiteStagesWrapper.getResourceSeriesConfigCount() );
    assertEquals( "got 0 subscriptions", 0, suiteStagesWrapper.getResourceSuites().size());
  }

  /**
   * Test proper substitution of all cron fields
   *
   * @throws Exception
   */
  public void testChooseCronFieldValue( ) throws Exception {
    assertEquals(
      "normal values pass thru",
      "5",
      SuiteStagesWrapper.chooseCronFieldValue( "5", false, 24 )
    );
    assertEquals(
      "normal values pass thru",
      "3-23/4",
      SuiteStagesWrapper.chooseCronFieldValue( "3-23/4", false, 24 )
    );
    for( int i = 0; i < 1000; i++ ) { // since it's random, try a bunch of times
      // minutes example
      String fieldValue = SuiteStagesWrapper.chooseCronFieldValue(
        "?", false, 60
      );
      assertTrue(
        "properly formatted",
        Pattern.matches( "\\?=\\d+", fieldValue)
      );
      int value = Integer.valueOf( fieldValue.substring( 2 )).intValue();
      assertTrue( "any value gets resolved mins", value >= 0 && value < 60 );

      // hours example
      fieldValue = SuiteStagesWrapper.chooseCronFieldValue( "?", false, 24 );
      assertTrue(
        "properly formatted",
        Pattern.matches( "\\?=\\d+", fieldValue)
      );
      value = Integer.valueOf(fieldValue.substring(2)).intValue();
      assertTrue( "any value gets resolved hour", value >= 0 && value <= 23 );

      // days example
      fieldValue = SuiteStagesWrapper.chooseCronFieldValue( "?", true, 31 );
      assertTrue(
        "properly formatted",
        Pattern.matches( "\\?=\\d+", fieldValue)
      );
      value = Integer.valueOf(fieldValue.substring(2)).intValue();
      assertTrue( "any value gets resolved day", value >= 1 && value <= 31 );

      // wday example
      fieldValue = SuiteStagesWrapper.chooseCronFieldValue( "?", false, 7 );
      assertTrue(
        "properly formatted",
        Pattern.matches( "\\?=\\d+", fieldValue)
      );
      value = Integer.valueOf(fieldValue.substring(2)).intValue();
      assertTrue( "any value gets resolved day", value >= 0 && value <= 6 );

      // month example
      fieldValue = SuiteStagesWrapper.chooseCronFieldValue( "?", true, 12 );
      assertTrue(
        "properly formatted",
        Pattern.matches( "\\?=\\d+", fieldValue)
      );
      value = Integer.valueOf(fieldValue.substring(2)).intValue();
      assertTrue( "any value gets resolved month", value >= 1 && value <= 12 );


      // minutes every example
      String cron_field = SuiteStagesWrapper.chooseCronFieldValue(
        "?-59/5", false, 60
      );
      assertTrue( "any every matches pattern",
                  Pattern.matches("\\?=\\d+-59/5", cron_field) );
      int randomBit = Integer.valueOf(
        Pattern.compile("-").split(cron_field)[0].substring(2) ).intValue();
      assertTrue( "any every range is good", randomBit >= 0 && randomBit < 5 );

      // another every example
      cron_field = SuiteStagesWrapper.chooseCronFieldValue(
        "?-20/5", false, 60
      );
      assertTrue( "any every matches pattern",
                  Pattern.matches("\\?=\\d+-20/5", cron_field) );
      randomBit = Integer.valueOf(
        Pattern.compile("-").split(cron_field)[0].substring(2) ).intValue();
      assertTrue( "any every range is good", randomBit >= 0 && randomBit < 5 );

      // old style every example
      cron_field = SuiteStagesWrapper.chooseCronFieldValue( "?/5", false, 60 );
      assertTrue( "any every matches pattern",
                  Pattern.matches("\\?=\\d+-59/5", cron_field) );
      randomBit = Integer.valueOf(
        Pattern.compile("-").split(cron_field)[0].substring(2) ).intValue();
      assertTrue( "any every range is good", randomBit >= 0 && randomBit < 5 );

      // days every example
      cron_field = SuiteStagesWrapper.chooseCronFieldValue("?-31/5", true, 31);
      assertTrue( "any every matches pattern",
                  Pattern.matches("\\?=\\d+-31/5", cron_field) );
      randomBit = Integer.valueOf(
        Pattern.compile("-").split(cron_field)[0].substring(2) ).intValue();
      assertTrue( "any every range is good", randomBit >= 1 && randomBit <= 5 );

      // days every example - (? - 31)
      cron_field = SuiteStagesWrapper.chooseCronFieldValue("?-31", true, 31);
      assertTrue( "any every matches pattern",
                  Pattern.matches("\\?=\\d+-31/1", cron_field) );
      randomBit = Integer.valueOf(
        Pattern.compile("-").split(cron_field)[0].substring(2) ).intValue();
      assertTrue( "any every range is good", randomBit >= 1 && randomBit <= 31 );

      // days every example - (?-31)/1
      cron_field = SuiteStagesWrapper.chooseCronFieldValue("?-31/1", true, 31);
      assertTrue( "any every matches pattern",
                  Pattern.matches("\\?=\\d+-31/1", cron_field) );
      randomBit = Integer.valueOf(
        Pattern.compile("-").split(cron_field)[0].substring(2) ).intValue();
      assertTrue( "any every range is good", randomBit >= 1 && randomBit <= 31 );     
    }
    try {
      SuiteStagesWrapper.chooseCronFieldValue( "?/25", true, 24 );
      fail( "invalid every should cause exception" );
    } catch( ConfigurationException e ) {
      assertTrue(
        "correct fail msg",
        Pattern.matches
        ( "Cron field out of range; 25 should be less than 24", e.getMessage() )
      );
    }
    try {
      SuiteStagesWrapper.chooseCronFieldValue( "?-25/10", true, 24 );
      fail( "invalid every should cause exception" );
    } catch( ConfigurationException e ) {
      assertEquals(
        "correct fail msg",
        e.getMessage(),
        "Cron field out of range; max 25 should be less than 24"
      );
    }

    try {
      SuiteStagesWrapper.chooseCronFieldValue( "?-10/12", true, 24 );
      fail( "invalid every should cause exception" );
    } catch( ConfigurationException e ) {
      assertEquals(
        "correct fail msg",
        e.getMessage(),
        "Cron field out of range; 12 should be less than 10"
      );
    }

    try {
      SuiteStagesWrapper.chooseCronFieldValue( "?-10/", true, 24 );
      fail( "invalid no every should cause exception" );
    } catch( ConfigurationException e ) {
      assertEquals(
        "correct fail msg",
        e.getMessage(),
        "Invalid cron syntax for field value '?-10/'"
      );
    }

    assertEquals(
      "borderline case",
      "?=0-59/1",
      SuiteStagesWrapper.chooseCronFieldValue( "?/1", false, 60 )
    );
    assertEquals(
      "borderline case",
      "?=1-31/1",
      SuiteStagesWrapper.chooseCronFieldValue( "?/1", true, 31 )
    );
  }

  /**
   * Test ability to substitute '?'s in schedules
   *
   * @throws Exception
   */
  public void testChooseSchedule() throws Exception {
    assertTrue( "matching", Pattern.matches("\\?", "?" ));
    String[] anySchedules = {
      "  <cron>\n" +
      "    <min>?</min>\n" +
      "    <hour>*</hour>\n" +
      "    <mday>*</mday>\n" +
      "    <wday>*</wday>\n" +
      "    <month>*</month>\n" +
      "  </cron>\n",
      "  <cron>\n" +
      "    <min>?</min>\n" +
      "    <hour>?</hour>\n" +
      "    <mday>?</mday>\n" +
      "    <wday>?</wday>\n" +
      "    <month>?</month>\n" +
      "  </cron>\n",
      "  <cron>\n" +
      "    <min>?</min>\n" +
      "    <hour>?</hour>\n" +
      "    <mday>?/3</mday>\n" +
      "    <wday>*</wday>\n" +
      "    <month>*</month>\n" +
      "  </cron>\n",
      "  <crontime>? ? ?/3 * *</crontime>\n",
      "  <crontime>? ? * * *</crontime>\n",
    };
    String[][] anyScheduleResults = {
      { "\\?=\\d+", "\\*", "\\*", "\\*", "\\*"},
      { "\\?=\\d+", "\\?=\\d+", "\\?=\\d+", "\\?=\\d+", "\\?=\\d+" },
      { "\\?=\\d+", "\\?=\\d+", "\\?=\\d+-\\d+/3", "\\*", "\\*" },
      { "\\?=\\d+", "\\?=\\d+", "\\?=\\d+-\\d+/3", "\\*", "\\*" },
      { "\\?=\\d+", "\\?=\\d+", "\\*", "\\*", "\\*" }      
    } ;
    for ( int i = 0; i < anySchedules.length; i++ ) {
      Schedule anySchedule = Schedule.Factory.parse( anySchedules[i] );
      Schedule resolvedSchedule = SuiteStagesWrapper.chooseSchedule( anySchedule );

      assertNotNull( "got a new schedule", resolvedSchedule.getCron() );
      logger.debug( "Schedule: " + resolvedSchedule );
      assertTrue( "minutes resolved", Pattern.matches
        ( anyScheduleResults[i][0], resolvedSchedule.getCron().getMin() ) );
      assertTrue( "hour resolved", Pattern.matches
        ( anyScheduleResults[i][1], resolvedSchedule.getCron().getHour() ) );
            assertTrue( "mday resolved", Pattern.matches
        ( anyScheduleResults[i][2], resolvedSchedule.getCron().getMday() ) );
      assertTrue( "wday resolved", Pattern.matches
        ( anyScheduleResults[i][3], resolvedSchedule.getCron().getWday() ) );
      assertTrue( "month resolved",Pattern.matches
        ( anyScheduleResults[i][4], resolvedSchedule.getCron().getMonth() ) );
    }
  }

  /**
   * Test ability to extract resource suites
   *
   * @throws Exception
   */
  public void testGetResourceSuites() throws Exception {
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    Repositories repositories = RepositoriesTest.createSampleRepository(null);
    File suiteFile = File.createTempFile( "inca", ".xml" );
    if ( suiteFile.exists() ) suiteFile.delete();
    SuiteStagesWrapper suiteStagesWrapper = new SuiteStagesWrapper( suiteFile.getAbsolutePath(), resources );
    SuiteWrapper sampleSuite = SuiteWrapperTest.createSampleSuite(repositories);
    suiteStagesWrapper.modify( sampleSuite );

    HashMap suites = suiteStagesWrapper.getResourceSuites();
    assertEquals( "Correct number of suites", 4, suites.size() );
    for ( int i = 0; i < SuiteStagesWrapperTest.resources.length; i++ ) {
      assertTrue(
        "Has "+SuiteStagesWrapperTest.resources[i],
        suites.containsKey(SuiteStagesWrapperTest.resources[i])
      );
      SuiteWrapper suite = (SuiteWrapper)suites.get(
        SuiteStagesWrapperTest.resources[i]
      );
      assertNotNull(
        SuiteStagesWrapperTest.resources[i] + "'s suite not null",
        suite
      );
      assertEquals(
        "Right # of series configs for " + SuiteStagesWrapperTest.resources[i],
        6,
        suite.getSeriesConfigCount()
      );
      assertEquals(
        "Right guid for" + SuiteStagesWrapperTest.resources[i],
        "edu.sdsc.inca.testsuite",
        suite.getSuiteDocument().getSuite().getGuid()
      );
      assertEquals( "right version " + i,
                    sampleSuite.getSuiteDocument().getSuite().getVersion(),
                    suite.getSuiteDocument().getSuite().getVersion() );
    }

    SuiteWrapper suite = (SuiteWrapper)suites.get( "localhost" );

    // test some stuff about myproxy
    SeriesConfig myproxyConfig = suite.getSeriesConfig(1);
    Series myproxyReporter = myproxyConfig.getSeries();
    assertTrue(
      "myproxy reporter okay",
      Pattern.matches("^.*myproxy.*$" , myproxyReporter.getUri() )
    );
    assertEquals( "cron is set for myproxy", "54",
                  myproxyConfig.getSchedule().getCron().getMin() );
    assertEquals(
      "report series looks good for myproxy",
      "myproxy.teragrid.org",
      myproxyReporter.getArgs().getArgArray( 2 ).getValue()
    );

    // test that ?= are removed from schedule
    SeriesConfig gccConfig = suite.getSeriesConfig(0);
    Schedule schedule = gccConfig.getSchedule();
    int mins = Integer.valueOf( schedule.getCron().getMin() ).intValue();
    assertTrue(
      "mins looks good",
      mins >= 0 && mins <=60
    );  
  }

  /**
   * Test expansion
   *
   * @throws Exception
   */
  public void testModify() throws Exception {
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    Repositories repositories = RepositoriesTest.createSampleRepository(null);
    File suiteFile = File.createTempFile( "inca", ".xml" );
    if ( suiteFile.exists() ) suiteFile.delete();
    SuiteStagesWrapper suiteStagesWrapper = new SuiteStagesWrapper( suiteFile.getAbsolutePath(), resources );
    SuiteWrapper sampleSuite = SuiteWrapperTest.createSampleSuite(repositories);
    suiteStagesWrapper.modify( sampleSuite );

    // test the original document is stored and retrievable
    assertEquals(
      "correct number of configs", 4, suiteStagesWrapper.getSeriesConfigCount()
    );
    String suiteXmlOrig = XmlWrapper.prettyPrint
      (sampleSuite.getSuiteDocument().getSuite(), "  ");
    String suiteXml = XmlWrapper.prettyPrint
      (suiteStagesWrapper.getSuiteDocument().getSuite(), "  ");
    suiteXmlOrig = suiteXmlOrig.replaceAll( "<xml-fragment.*>", "" );
    suiteXml = suiteXml.replaceAll( "<xml-fragment.*>", "" );
    suiteXml = suiteXml.replaceAll( "<seriesConfig .*>", "<seriesConfig>" );
    suiteXmlOrig = suiteXmlOrig.replaceAll( "<version>\\?=[\\d\\.]+</version>\\s*", "" );
    assertEquals( "matches original", suiteXmlOrig, suiteXml );

    // test the per resource series configs are stored correctly
    assertEquals(
      "correct number of configs", 24, suiteStagesWrapper.getResourceSeriesConfigCount()
    );
    assertEquals(
      "correct number of configs",
      24,
      suiteStagesWrapper.getPerResourceSuiteDocument().getSuite().getSeriesConfigs().
      sizeOfSeriesConfigArray()
    );

    // GCC Reporter - contains no macros - 4  reporters should be listed
    suiteStagesWrapper.getDocument().save( File.createTempFile("suiteStagesWrapper", "xml") );
    XmlObject[] objects = suiteStagesWrapper.getDocument().selectPath(
      "//perResourceConfigs/config/series[matches(uri, '^.*gcc.*$')]"
    );
    assertEquals( "Basic resource expansion worked", 4, objects.length );

    // MYPROXY Reporter - contains one macro which maps to one value
    // 4 myproxy reporters should be listed
    objects = suiteStagesWrapper.getDocument().selectPath(
      "//perResourceConfigs/config/series[matches(uri, '^.*myproxy.*$')]"
    );
    assertEquals( "Basic resource expansion worked (2)", 4, objects.length );

    // test that gcc config expanded the resources and arguments
    objects = suiteStagesWrapper.getDocument().selectPath(
      "//perResourceConfigs/config[resourceHostname='tg-login.sdsc.teragrid.org']/series[matches(uri, '^.*myproxy.*$')]"
    );
    assertEquals( "Got gcc for sdsc config", 1, objects.length );
    Series series = (Series)objects[0];
    assertEquals(
      "Simple macro expansion - global",
      "myproxy.teragrid.org",
      series.getArgs().getArgArray( 2 ).getValue()
    );
    assertEquals(
      "context set",
      "bash --login -c 'export X509_USER_PROXY=@.incaProxy@;" +
      "grid.interactive_access.myproxy.unit" +
      " -certpw=\"MyProxy_test\" -help=\"no\" -server=\"myproxy.teragrid.org\" -verbose=\"0\"'",
      series.getContext()
    );

    // TGCP Reporter - contains one macro containing 3 values
    // 12 reporters should be found
    objects = suiteStagesWrapper.getDocument().selectPath(
      "//perResourceConfigs/config/series[matches(uri, '^.*tgcp.*$')]"
    );
    assertEquals( "Multi value macro expansion worked", 12, objects.length );

    sampleSuite = SuiteWrapperTest.createSampleSuiteModification(repositories);
    SuiteStagesWrapper changes = suiteStagesWrapper.modify( sampleSuite );
    // deleted non-sdsc gcc's -- should have 2
    objects = suiteStagesWrapper.getDocument().selectPath(
      "//perResourceConfigs/config/series[matches(uri, '^.*gcc.*$')]"
    );
    assertEquals( "resource change worked", 2, objects.length );
    // changes should include 4 deletes and 2 adds
    objects = changes.getDocument().selectPath(
      "//perResourceConfigs/config[action='delete']/series[matches(uri, '^.*gcc.*$')]"
    );
    assertEquals( "4 deletes", 4, objects.length );
    objects = changes.getDocument().selectPath(
      "//perResourceConfigs/config[action='add']/series[matches(uri, '^.*gcc.*$')]"
    );
    assertEquals( "2 deletes", 2, objects.length );


    // deleted complelete - none left
    objects = suiteStagesWrapper.getDocument().selectPath(
      "//perResourceConfigs/config/series[matches(uri, '^.*myproxy.*$')]"
    );
    assertEquals( "myproxy deleted", 0, objects.length );
    objects = changes.getDocument().selectPath(
      "//perResourceConfigs/config/series[matches(uri, '^.*myproxy.*$')]"
    );
    assertEquals( "myproxy deleted", 4, objects.length );


  }

  /**
   * Test suite dao persistence.  Also tests constructors with suite document
   * and suite file.
   *
   * @throws Exception
   */
  public void testPersistenceAndModify() throws Exception {
    Repositories repositories = RepositoriesTest.createSampleRepository(null);
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    File suiteFile = File.createTempFile( "inca", ".xml" );
    if ( suiteFile.exists() ) suiteFile.delete();
    SuiteStagesWrapper suiteStagesWrapper = new SuiteStagesWrapper( suiteFile.getAbsolutePath(), resources );

    // no configs to start with
    assertEquals( "no configs now", 0, suiteStagesWrapper.getSeriesConfigCount() );

    SuiteWrapper suiteChange = SuiteWrapperTest.createSampleSuite(repositories);
    suiteStagesWrapper.modify( suiteChange );
    assertEquals( "suiteStagesWrapper has guid", "edu.sdsc.inca.testsuite",
                  suiteStagesWrapper.getDocument().getSuiteStages().getGuid() );
    assertEquals( "suiteStagesWrapper has guid", "edu.sdsc.inca.testsuite",
                  suiteStagesWrapper.getSuiteDocument().getSuite().getGuid() );

    assertEquals( "have 4 series configs", 4, suiteStagesWrapper.getSeriesConfigCount() );
    assertEquals(
      "have 24 resource series configs", 24,
      suiteStagesWrapper.getResourceSeriesConfigCount()
    );

    suiteChange = SuiteWrapperTest.createSampleSuiteModification(repositories);
    suiteStagesWrapper.modify( suiteChange );
    suiteStagesWrapper.save();
    assertEquals("still have 3 series configs", 3,suiteStagesWrapper.getSeriesConfigCount());
    assertEquals(
      "have 18 resource series configs now", 18,
      suiteStagesWrapper.getResourceSeriesConfigCount()
    );

    // check doc attributes
    assertEquals(
      "has suiteStagesWrapper name",
      suiteChange.getSuiteDocument().getSuite().getName(),
      suiteStagesWrapper.getSuiteDocument().getSuite().getName()
    );
    // check doc attributes
    assertEquals(
      "has suiteStagesWrapper GUID",
      suiteChange.getSuiteDocument().getSuite().getGuid(),
      suiteStagesWrapper.getSuiteDocument().getSuite().getGuid()
    );
    // check doc attributes
    assertEquals(
      "has suiteStagesWrapper description",
      suiteChange.getSuiteDocument().getSuite().getDescription(),
      suiteStagesWrapper.getSuiteDocument().getSuite().getDescription()
    );

    // try to reload it
    SuiteStagesWrapper savedSuiteStagesWrapper = new SuiteStagesWrapper(
      suiteFile.getAbsolutePath(), resources
    );
    assertEquals(
      "have 18 resource series configs now", 18,
      savedSuiteStagesWrapper.getResourceSeriesConfigCount()
    );
    savedSuiteStagesWrapper.remove();
    assertFalse( "suite file gone", suiteFile.exists() );

    // with encryption
    logger.debug(  "Testing encryption" );
    String passphrase = "SsshSecret";
    File encryptedFile = File.createTempFile( "suiteStages", ".xml" );
    encryptedFile.delete();
    SuiteStagesWrapper encryptedSuiteStagesWrapper = new SuiteStagesWrapper(
      encryptedFile.getAbsolutePath(), passphrase, resources
    );
    encryptedSuiteStagesWrapper.setDocument(
      savedSuiteStagesWrapper.getDocument()
    );
    encryptedSuiteStagesWrapper.save();
    SuiteStagesWrapper encryptedSuiteStagesWrapper2 = new SuiteStagesWrapper(
      encryptedFile.getAbsolutePath(), passphrase, resources
    );
    assertEquals(
      "reloaded resources",
      encryptedSuiteStagesWrapper.getDocument().xmlText(),
      encryptedSuiteStagesWrapper2.getDocument().xmlText()
    );
    encryptedSuiteStagesWrapper2.remove();
    assertFalse( "encrypted file deleted", encryptedFile.exists() );
  }

  /**
   * Test set ResourceCOnfigDAO function
   *
   * @throws Exception
   */
  public void testSetResourcesWrapper() throws Exception {

    long startTime = Calendar.getInstance().getTimeInMillis();
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    Repositories repositories = RepositoriesTest.createSampleRepository(null);
    File suiteFile = File.createTempFile( "inca", ".xml" );
    if ( suiteFile.exists() ) suiteFile.delete();
    SuiteStagesWrapper suiteStagesWrapper = new SuiteStagesWrapper( suiteFile.getAbsolutePath(), resources );
    SuiteWrapper suiteChange = SuiteWrapperTest.createSampleSuite(repositories);
    suiteStagesWrapper.modify( suiteChange );
    suiteStagesWrapper.getDocument().save(File.createTempFile("samplesuite", "xml"),new XmlOptions());

    SuiteDocument suite = suiteStagesWrapper.getPerResourceSuiteDocument();
    logger.debug(
      "Suite contains " +
      suite.getSuite().getSeriesConfigs().sizeOfSeriesConfigArray()
    );
    SeriesConfig gcc =
      suite.getSuite().getSeriesConfigs().getSeriesConfigArray(0);

    // make another resource document with a different resource name: dslogin
    // changes to dslogin2
    ResourceConfigDocument newResourcesDoc =
      (ResourceConfigDocument)resources.getResourceConfigDocument().copy();

    Object[] result = newResourcesDoc.selectPath(
      "//resource[name='dslogin.sdsc.edu']"
    );
    assertEquals( "got dslogin.sdsc.edu", 1, result.length );
    Resource dslogin = (Resource)result[0];
    dslogin.setName( "dslogin2.sdsc.edu" );
    result = newResourcesDoc.selectPath( "//macro[name='myproxyServer']" );
    for ( int i = 0; i < result.length; i++) {
      Macro myproxyServer = (Macro)result[i];
      myproxyServer.setValueArray( 0, "myproxy.sdsc.edu" );
    }
    result = newResourcesDoc.selectPath( "//macro[name='myproxyCertPassword']");
    for ( int i = 0; i < result.length; i++) {
      Macro myproxyPw = (Macro)result[i];
      myproxyPw.setValueArray( 0, "changeIt!" );
    }
    result = newResourcesDoc.selectPath( "//macro[name='tgcpServers']" );
    for ( int i = 0; i < result.length; i++) {
      Macro tgcpServers = (Macro)result[i];
      tgcpServers.addValue( "tg-gridftp.iu.teragrid.org -P 2812");
    }

    // add a host to sdsc
    result = newResourcesDoc.selectPath(
      "//resource[name='tg-login2.sdsc.teragrid.org']"
    );
    Resource tglogin = (Resource)result[0];
    tglogin.setName( "tg-login4.sdsc.teragrid.org" );
    newResourcesDoc.save( File.createTempFile("resourcesNew", "xml") );
    ResourcesWrapper newResources = new ResourcesWrapper( newResourcesDoc );

    String contentsBefore =
      StringMethods.fileContents( suiteFile.getAbsolutePath() );
    SuiteDocument changes = suiteStagesWrapper.setResources( newResources );
    suiteStagesWrapper.getDocument().save
      ( File.createTempFile("samplesuitechanges", "xml"), new XmlOptions() );
    SuiteDocument newsuite = suiteStagesWrapper.getPerResourceSuiteDocument();
    SeriesConfig gccnew = newsuite.getSuite().getSeriesConfigs().getSeriesConfigArray( 0);
    assertEquals( "gcc not modified",
      XmlWrapper.prettyPrint(gcc,"  "), XmlWrapper.prettyPrint(gccnew,"  "));

    Thread.sleep(2);
    assertFalse("file modified", contentsBefore.equals(
                StringMethods.fileContents(suiteFile.getAbsolutePath())));


    SuiteWrapper resourceChanges = new SuiteWrapper(
      changes
    );
    // 1 resource change (dslogin) on 6 reporters (12 adds&deletes) and
    // 1 config changes to myproxy on 4 resources (8 adds/deletes) and
    // 1 extra macro expansion on tgcp -- 4 adds --
    // -2 (resource and myproxy add/delete overlap)
    // TOTAL = 22
    assertEquals(
      "got 22 changes", 22,
      resourceChanges.getSeriesConfigCount()
    );
    assertEquals(
      "guid set",
      "edu.sdsc.inca.testsuite",
      resourceChanges.getSuiteDocument().getSuite().getGuid()
    );

    // should be 0 changes
    changes = suiteStagesWrapper.setResources( newResources );
    resourceChanges = new SuiteWrapper(
      changes
    );
    assertEquals(
      "got 0 changes", 0, resourceChanges.getSeriesConfigCount()
    );

    long endTime = Calendar.getInstance().getTimeInMillis();
    logger.info(
      "Initialized resources in " +
      ((endTime - startTime)/Constants.MILLIS_TO_SECOND) + " secs"
    );
  }

  /**
   * Test ability to expand report series
   *
   * @throws Exception
   */
  public void testSeriesConfigCrossProduct() throws Exception {

    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    File suiteFile = File.createTempFile( "inca", ".xml" );

    //----------------------------------------------------------------------
    // test macros
    if ( suiteFile.exists() ) suiteFile.delete();
    SuiteStagesWrapper suiteStagesWrapper = new SuiteStagesWrapper( suiteFile.getAbsolutePath(), resources );

    SeriesConfig seriesConfig = SeriesConfig.Factory.newInstance();
    Series series = seriesConfig.addNewSeries();

    Args args = Args.Factory.newInstance();

    Args.Arg argTgcp = args.addNewArg();
    argTgcp.setName( "server");
    argTgcp.setValue( "@tgcpServers@");
    series.setArgs( args );

    series.setContext( "soft add +@globusFlavors@@@@globusFlavors@'" );
    seriesConfig.setNickname(
      "@tgcpServers@ testing @globusFlavors@ @globusFlavors@"
    );
    Limits l = Limits.Factory.newInstance();
    l.setCpuTime( "60.0"  );
    series.setLimits( l );
    series.setUri( "file:///tmp/blah");

    Vector cp = suiteStagesWrapper.seriesConfigCrossProduct(
      seriesConfig, "tg-login.sdsc.teragrid.org"
    );
    assertEquals( "Series expansion count correct", 6, cp.size() );
    int gccFlavorCount = 0;
    int vendorFlavorCount = 0;
    int tgcpSdscCount = 0;
    for( int i = 0; i < cp.size(); i++ ) {
      SeriesConfig eSeriesConfig = (SeriesConfig)cp.get(i);
      logger.debug( eSeriesConfig );
      String tgcpValue =
        eSeriesConfig.getSeries().getArgs().getArgArray(0).getValue();
      String flavor1 = eSeriesConfig.getSeries().getContext().replaceFirst(
        "'$", ""
      );
      flavor1 = flavor1.replaceFirst(".*@@", "");
      String flavor2 =
        eSeriesConfig.getSeries().getContext().replaceFirst("@@.*", "");
      flavor2 = flavor2.substring("soft add +".length());
      logger.debug( "Verifying " + i );
      assertEquals( "description expanded",
                    tgcpValue + " testing " + flavor1 + " " + flavor2,
                    eSeriesConfig.getNickname() + "" );
      if ( eSeriesConfig.getSeries().getContext().indexOf( "gcc64dbg'") >= 0) {
        gccFlavorCount++;
      }
      if ( eSeriesConfig.getSeries().getContext().indexOf( "vendorcc64dbg'") >= 0) {
        vendorFlavorCount++;
      }
      if ( eSeriesConfig.getSeries().getArgs().getArgArray( 0).getValue().
        equals("tg-gridftp.sdsc.teragrid.org -P 2812")) {
        tgcpSdscCount++;
      }
      assertFalse( "no target hostname", eSeriesConfig.isSetTargetHostname() );
    }
    assertEquals( "gcc cleanup instances", 3, gccFlavorCount );
    assertEquals( "vendor cleanup instances", 3, vendorFlavorCount );
    assertEquals( "tgcp instances", 2, tgcpSdscCount );

    //----------------------------------------------------------------------
    // test macros from other resources
    if ( suiteFile.exists() ) suiteFile.delete();
    suiteStagesWrapper = new SuiteStagesWrapper( suiteFile.getAbsolutePath(), resources );

    seriesConfig = SeriesConfig.Factory.newInstance();
    series = seriesConfig.addNewSeries();

    args = Args.Factory.newInstance();

    Args.Arg hosts = args.addNewArg();
    hosts.setName( "server");
    hosts.setValue( "@teragrid->__incaHosts__@");
    series.setArgs( args );

    series.setContext( "@@'" );
    seriesConfig.setNickname( "@abcMacro@test_to_@teragrid->__incaHosts__@");
    series.setUri( "file:///tmp/blah");

    AcceptedOutput out = seriesConfig.addNewAcceptedOutput();
    Notifications.Notification notification =
      out.addNewNotifications().addNewNotification();
    notification.setNotifier("EmailNotifier");
    notification.setTarget("@teragrid->emailContact@");

    cp = suiteStagesWrapper.seriesConfigCrossProduct(
      seriesConfig, "tg-login.sdsc.teragrid.org"
    );
    assertEquals( "Series expansion count correct", 6, cp.size() );
    String[] emailContacts = {
      "admin@sdsc.edu",
      "admin@sdsc.edu",
      "admin@sdsc.edu",
      "admin@sdsc.edu",
      "admin@ncsa.uiuc.edu",
      "admin@localhost"
    };
    String[] nicknames = {
      "deftest_to_tg-login1.sdsc.teragrid.org",
      "deftest_to_tg-login2.sdsc.teragrid.org",
      "deftest_to_tg-login3.sdsc.teragrid.org",
      "deftest_to_dslogin.sdsc.edu",
      "deftest_to_tg-login.ncsa.teragrid.org",
      "deftest_to_localhost"
    };
    String[] servers = {
      "tg-login1.sdsc.teragrid.org",
      "tg-login2.sdsc.teragrid.org",
      "tg-login3.sdsc.teragrid.org",
      "dslogin.sdsc.edu",
      "tg-login.ncsa.teragrid.org",
      "localhost"
    };
    String[] targets = {
      "tg-login.sdsc.teragrid.org",
      "tg-login.sdsc.teragrid.org",
      "tg-login.sdsc.teragrid.org",
      "dslogin.sdsc.edu",
      "tg-login.ncsa.teragrid.org",
      "localhost"
    };

    for( int i = 0; i < cp.size(); i++ ) {
      SeriesConfig eSeriesConfig = (SeriesConfig)cp.get(i);
      logger.debug( eSeriesConfig );

      assertEquals("email contact correct " + i, emailContacts[i],
        eSeriesConfig.getAcceptedOutput().getNotifications().getNotificationArray(0).getTarget());
      assertEquals("nickname correct " + i, nicknames[i],
        eSeriesConfig.getNickname());
      assertEquals("server arg correct " + i, servers[i],
        eSeriesConfig.getSeries().getArgs().getArgArray(0).getValue());
      assertEquals("target correct " + i, targets[i],
        eSeriesConfig.getTargetHostname());
    }
  }

  /**
   * test updatePackage function
   *
   * @throws Exception
   */
  public void testUpdatePackage() throws Exception {
    File tempFile = File.createTempFile( "inca", "tmp" );
    tempFile.delete();

    SuiteWrapper suiteDoc = SuiteWrapperTest.createSampleSuite(
      RepositoriesTest.createSampleRepository( null )
    );
    SuiteStagesWrapper suite = new SuiteStagesWrapper(
      tempFile.getAbsolutePath(),
      ResourcesWrapperTest.createSampleResources()
    );
    logger.debug( suiteDoc.getSuiteDocument().toString() );
    suite.modify( suiteDoc );
    suite.save();
    XmlObject[] result = suite.getDocument().selectPath(
      "//multiResourceConfig[latestVersion='true']"
    );
    assertEquals( "4 set to latestVersion", 4, result.length );
    SuiteWrapper matches = suite.updatePackage(
      "cluster.compiler.gcc.version", "1.6", "newuri"
    );
    assertEquals( "1 add and 1 delete", 2, matches.getSeriesConfigCount() );
    XmlObject[] results = matches.getSuiteDocument().selectPath(
      "//seriesConfig[action='delete' and " +
      "series[name='cluster.compiler.gcc.version' and version='1.5']]"
    );
    assertEquals( "delete found", 1, results.length );
    results = matches.getSuiteDocument().selectPath(
      "//seriesConfig[action='add' and " +
      "series[name='cluster.compiler.gcc.version' and version='1.6'" +
      " and uri='newuri']]"
    );
    assertEquals( "add found", 1, results.length );



  }

  // PRIVATE FUNCTIONS

  private void argVectorEquals(
    String msg, String[][][] expectedArgs, Vector expandedArgs )
    throws XmlException {

    assertEquals(
      msg + ": same length",
      expectedArgs.length,
      expandedArgs.size()
    );
    for( int i = 0; i < expectedArgs.length; i++ ) { // for each set of args
      String argsetString = (String)expandedArgs.get(i);
      Args argset = Args.Factory.parse(argsetString);
      for ( int j = 0; j < expectedArgs[i].length; j++ ) { // each arg in set
        Args.Arg arg = argset.getArgArray(j);
        assertEquals(
          msg + ": " + expectedArgs[i][j][0],
          expectedArgs[i][j][0],
          arg.getName()
        );
        assertEquals(
          msg + ": " + expectedArgs[i][j][1],
          expectedArgs[i][j][1],
          arg.getValue()
        );
      }

    }
  }
}
