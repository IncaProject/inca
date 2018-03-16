package edu.sdsc.inca.util;

import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.util.regex.Pattern;
import java.io.File;

import edu.sdsc.inca.dataModel.util.SeriesConfig;
import edu.sdsc.inca.dataModel.util.Schedule;
import edu.sdsc.inca.dataModel.util.Args;

/**
 * Test XMLWrapper abstract class functions
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class XmlWrapperTest extends TestCase {
  private static Logger logger = Logger.getLogger( XmlWrapperTest.class );
  String password = "superSecret";
  String[] samples = {
    "<name>passphrase</name>\n" +
    "<value>" + password + "</value>\n",

    "<name>aPassphrase</name>\n" +
    "<value>" + password + "</value>\n",

    "<name>password</name>\n" +
    "<value>" + password + "</value>\n",

    "<name>aPassword</name>\n" +
    "<value>" + password + "</value>\n",

    "<name>passphrase</name>\n" +
    "<value>" + password + "</value>\n" +
    "<name>server</name>\n" +
    "<value>localhost</value>\n" +
    "<name>password</name>\n" +
    "<value>" + password + "</value>\n",

    "<name>server</name>\n" +
    "<value>localhost</value>\n" +
    "<name>password</name>\n" +
    "<value>" + password + "</value>\n" +
    "<name>passphrase</name>\n" +
    "<value>" + password + "</value>\n" +
    "<name>passphrase</name>\n" +
    "<value>" + password + "</value>\n" +
    "<name>server</name>\n" +
    "<value>localhost</value>\n" +
    "<name>password</name>\n" +
    "<value>" + password + "</value>\n" +
    "<name>server</name>\n" +
    "<value>localhost</value>\n",

    "<name>server</name>\n" +
    "<value>localhost</value>\n" +
    "<name>password</name>\n" +
    "<value>" + password + "</value>\n" +
    "<name>server</name>\n" +
    "<value>localhost</value>\n"
  };

  public void testConfigEquals() throws Exception {
    SeriesConfig config = SeriesConfig.Factory.newInstance();
    config.addNewSeries().addNewArgs();
    Args.Arg arg = config.getSeries().getArgs().addNewArg();
    arg.setName( "arg1" );
    arg.setValue( "value1" );
    arg = config.getSeries().getArgs().addNewArg();
    arg.setName( "arg2" );
    arg.setValue( "value2" );
    Schedule schedule = config.addNewSchedule();
    schedule.addNewCron().setMin( "?" );
    schedule.getCron().setHour( "?/5" );
    schedule.getCron().setMday( "*" );
    schedule.getCron().setWday( "*" );
    schedule.getCron().setMonth( "*" );
    assertTrue(
      "configEquals workd for true case",
      XmlWrapper.configEqual( config, config )
    );

    SeriesConfig config2 = (SeriesConfig)config.copy();
    assertTrue(
      "configEquals worked when exactly the same",
      XmlWrapper.configEqual( config, config2 )
    );
    assertTrue(
      "configEquals worked when exactly the same",
      XmlWrapper.configEqual( config2, config )
    );
    config2.getSeries().getArgs().setArgArray( new Args.Arg[0] );
    arg = config2.getSeries().getArgs().addNewArg();
    arg.setName( "arg2" );
    arg.setValue( "value2" );
    arg = config2.getSeries().getArgs().addNewArg();
    arg.setName( "arg1" );
    arg.setValue( "value1" );
    logger.debug( ">> config2" + config2 );
    assertTrue(
      "configEquals works when args out of order",
      XmlWrapper.configEqual( config, config2 )
    );
    config2.setSchedule(
      SuiteStagesWrapper.chooseSchedule(config2.getSchedule())
    );
    assertTrue(
      "configEquals workd for true case",
      XmlWrapper.configEqual( config, config2 )
    );
    config.getSchedule().getCron().setMin( "5");
    assertFalse(
      "configEquals workd for false case",
      XmlWrapper.configEqual( config, config2 )
    );

    config2.getSchedule().unsetCron();
    config2.getSchedule().setCrontime( "5 ?=0-23/5 * * *" );
    logger.debug( config2 );
    logger.debug( config );     
    assertTrue(
      "configEquals worked when cron and crontime used",
      XmlWrapper.configEqual( config2, config )
    );

    // check to see if chooseSchedule will not pick new schedule if already
    // selected
    SeriesConfig config3 = (SeriesConfig)config2.copy();
    config3.setSchedule(
      SuiteStagesWrapper.chooseSchedule(config3.getSchedule())
    );
    assertTrue(
      "configEquals workd for true case",
      XmlWrapper.configEqual( config2, config3 )
    );


  }

  /**
   * Test crypSensitive function
   *
   * @throws Exception
   */
  public void testCryptSensitive() throws Exception {
    for ( int i = 0; i < samples.length; i++) {
      logger.debug( "\nORIG " + samples[i] );
      String encrypted = XmlWrapper.cryptSensitive(
        samples[i], "topSecret!", false
      );
      assertFalse(
        "password not found",
        Pattern.compile(password).matcher(encrypted).find()
      );
      String decrypted = XmlWrapper.cryptSensitive(
        encrypted, "topSecret!", true
      );
      assertEquals( "got back the original text", decrypted, samples[i] );
    }
  }

  /**
   * Test read and save functions
   *
   * @throws Exception
   */
  public void testReadSave() throws Exception {
    File tempFile = File.createTempFile( "inca", ".xml" );
    XmlWrapper.save( samples[6], tempFile.getAbsolutePath(), "topSecret!" );
    String xml = XmlWrapper.read( tempFile.getAbsolutePath(), "topSecret!" );
    assertEquals( "restored text", samples[6], xml );
    xml = XmlWrapper.read( tempFile.getAbsolutePath(), "" );
    assertFalse(
        "password not found",
        Pattern.compile(password).matcher(xml).find()
    );
    tempFile.delete();
  }

  /**
   * Test prettyPrint method.
   *
   * @throws Exception
   */
  public void testPrettyPrint() throws Exception {
    String cdata = "<b>ab<![CDATA[<any>\n\n<garbage]]>cd</b>";
    String comment = "<!-- this is\na comment\n-->";
    String pi = "<?xml version='1.0' encoding='UTF-8'?>";
    String pretty;
    pretty = XmlWrapper.prettyPrint(comment, "  ");
    assertEquals(comment + "\n", pretty);
    pretty = XmlWrapper.prettyPrint(pi, "  ");
    assertEquals(pi + "\n", pretty);
    pretty = XmlWrapper.prettyPrint("   \n    " + pi + "\n\n\n   \n", "  ");
    assertEquals(pi + "\n", pretty);
    String element = "<b>content</b>";
    pretty = XmlWrapper.prettyPrint(element, "  ");
    assertEquals(element + "\n", pretty);
    element = "<a><b>content</b></a>";
    pretty = XmlWrapper.prettyPrint(element, "  ");
    assertEquals("<a>\n  <b>content</b>\n</a>\n", pretty);
    element = "<a><b><c> content </c><c>more\ncontent</c></b><b>also</b></a>";
    pretty = XmlWrapper.prettyPrint(element, "  ");
    assertEquals("<a>\n  <b>\n    <c> content </c>\n    <c>more\ncontent</c>\n  </b>\n  <b>also</b>\n</a>\n", pretty);
    pretty = XmlWrapper.prettyPrint(cdata, "  ");
    assertEquals(cdata + "\n", pretty);
    pretty = XmlWrapper.prettyPrint(element, " ");
    assertEquals("<a>\n <b>\n  <c> content </c>\n  <c>more\ncontent</c>\n </b>\n <b>also</b>\n</a>\n", pretty);

  }

}
