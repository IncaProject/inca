package edu.sdsc.inca.util;

import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.dataModel.suite.Suite;
import edu.sdsc.inca.dataModel.suiteStages.SuiteStagesDocument;
import edu.sdsc.inca.dataModel.suiteStages.MultiResourceConfig;
import edu.sdsc.inca.dataModel.util.SeriesConfig;
import edu.sdsc.inca.dataModel.util.Schedule;
import edu.sdsc.inca.dataModel.util.Cron;
import edu.sdsc.inca.dataModel.util.Args;
import edu.sdsc.inca.dataModel.inca.IncaDocument;
import edu.sdsc.inca.dataModel.resourceConfig.ResourceConfigDocument;
import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.repository.Repositories;
import edu.sdsc.inca.repository.Repository;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlObject;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;
import java.util.Random;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;

/**
 * Convenience object for accessing suite stages documents and persisting
 * changes to them. A suite stages document stores series configs from a
 * suite document in expanded form (i.e., macros are expanded and schedules
 * selected). There are 3 main stages of a suite's series configs.
 *
 * 1) the series configs as specified by the user.  These are known in the suite
 * stages document as a "MultiResourceConfigs" meaning that each series config
 * can potentially expand to run on multiple resources.
 *
 * 2) the expanded series configs per resource.  These are known in the suite
 * stages document as "PerResourceConfigs" meaning that the series config
 * applies to a single resource.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class SuiteStagesWrapper extends XmlWrapper {
  final public static String DIFF_OPTS =
    "   a|all-diffs       boolean Print all diffs\n" +
    "  it|ignore-target   boolean Ignore targetHostname in comparisons\n" +
    "  iu|ignore-uri      boolean Ignore uri in comparisons\n" +
    "  s1|suite-dir1      string  A directory containing suite files\n" +
    "  s2|suite-dir2      string  A directory containing suite files\n";

  final public static String EXPAND_OPTS =
    "  i|inca           string Inca configuration file\n" +
    "  s|suite-dir      string  A directory to place generated suite files\n";

  private static Pattern ANY = Pattern.compile( "\\?" );
  private static Pattern ANY_EVERY = Pattern.compile( "\\?\\/(\\d+)" );
  private static Pattern ANY_EVERY_NEW =
    Pattern.compile( "\\?-(\\d+)(\\/(\\d+))?" );

  private SuiteStagesDocument suiteStages = null;
  private ResourcesWrapper resources = null;
  private String filePath = null;
  private String passphrase = null;
  private static Logger logger = Logger.getLogger(SuiteStagesWrapper.class);

    /**
   * Create a SuiteStagesWrapper object with an existing SuiteStages document.
   *
   * @param doc  A suite stages document
   *
   * @throws InstantiationException if unable to create  document
   */
  public SuiteStagesWrapper( SuiteStagesDocument doc )
    throws InstantiationException  {

    this.resources = null;
    this.suiteStages = doc;
    try {
      validate( this.suiteStages );
    } catch ( Exception e ) {
      throw new InstantiationException(
        "Unable to create blank SuiteStagesWrapper object"
      );
    }
  }

  /**
   * Create a SuiteStagesWrapper object with a blank SuiteStages document.
   *
   * @param resources   The resource configuration information.
   *
   * @throws InstantiationException if unable to create empty document
   */
  public SuiteStagesWrapper( ResourcesWrapper resources )
    throws InstantiationException  {

    this.resources = resources;
    this.suiteStages = SuiteStagesDocument.Factory.newInstance();
    this.suiteStages.addNewSuiteStages().addNewMultiResourceConfigs();
    this.suiteStages.getSuiteStages().setGuid( "UnnamedSuite" );
    this.suiteStages.getSuiteStages().setName( "UnnamedSuite" );
    try {
      validate( this.suiteStages );
    } catch ( Exception e ) {
      throw new InstantiationException(
        "Unable to create blank SuiteStagesWrapper object"
      );
    }
  }

  /**
   * Create a new SuiteStagesWrapper object from a SuiteStages file.
   *
   * @param filename  Path to a file containing suiteStages compliant XML.
   * @param resources The resource configuration information.
   *
   * @throws CrypterException if unable to decrypt file
   * @throws IOException if unable to read from disk
   * @throws XmlException if unable to parse suite from disk
   */
  public SuiteStagesWrapper( String filename, ResourcesWrapper resources )
    throws IOException, XmlException, CrypterException {
    this( filename, "", resources );
  }

  /**
   * Create a new SuiteStagesWrapper object from a SuiteStages file.
   *
   * @param filename  Path to a file containing suiteStages compliant XML.
   * @param passphrase  Secret string used to encrypt/decrypt file to disk
   * @param resources The resource configuration information.
   *
   * @throws CrypterException if trouble encrypting/decrypting file
   * @throws IOException if problem reading in file
   * @throws XmlException if problem parsing suite
   */
  public SuiteStagesWrapper( String filename, String passphrase,
                             ResourcesWrapper resources )
    throws IOException, XmlException, CrypterException {

    this.resources = resources;
    this.filePath = filename;
    this.passphrase = passphrase;
    File suiteFile  = new File( filename );
    if ( suiteFile.exists() ) {
      String xml = read( filename, passphrase );
      this.suiteStages = SuiteStagesDocument.Factory.parse( xml );
      logger.debug(
        "Loading suite '" + this.suiteStages.getSuiteStages().getName() +
        "' at " + filename
      );
    } else {
      this.suiteStages = SuiteStagesDocument.Factory.newInstance();
      this.suiteStages.addNewSuiteStages().addNewMultiResourceConfigs();
      this.suiteStages.getSuiteStages().setGuid( "UnnamedSuite" );
      this.suiteStages.getSuiteStages().setName( "UnnamedSuite" );
      logger.debug(
        "SuiteStagesWrapper '" + filename + "' does not exist...creating blank suite"
      );
      this.save();
    }
    validate( this.suiteStages );
  }

  /**
   * Given a field in a cron line (e.g., hour or day), check it for instances
   * of '?' meaning anytime.  For example, '?' in the hour field would indicate
   * to run at any hour of the day.  Similarly, '?' in the minute field would
   * indicate to run at any hour of the minute.  '?-[max]/[number]' indicates to
   * run every [number] seconds at any offset with an upper bound of max.
   * For example, '?-23/8' in the hour
   * field indicates to run every 8 hours beginning at any offset.  In this
   * latter example, this function could return ?=4-23/8 meaning run at hours
   * 4,12, and 20.
   *
   * @param field  The field from a cron line which may or may not contain '?'
   * @param startAtOne True if the minimum value of the field is 1.  Otherwise
   * 0 is assumed.  E.g., it would be false for minute and true for days.
   * @param count The count for the field.  E.g., 24 for hours, 60 for minutes,
   * etc.
   *
   * @return A string where values are chose for the '?'.  For, example '?=52'
   * or '?=2-59/4'.
   * @throws edu.sdsc.inca.ConfigurationException if fields are out of range
   */
  public static String chooseCronFieldValue( String field, boolean startAtOne,
                                             int count )
    throws ConfigurationException {

    // pass it back if it doesn't contain '?'
    if ( ! ANY.matcher(field).find() ) {
      return field;
    }

    // if it's already been expanded, return it
    if ( Pattern.matches( "^\\?=\\d+-?\\d*/?\\d*", field) ) {
      return field;
    }

    // otherwise we need to modify it
    // if it matches '?' exactly we can pick anytime within the interval
    Random pickAny = new Random();
    if ( ANY.matcher(field).matches() ) {
      int randomTime = pickAny.nextInt( count - 1 );
      if ( startAtOne ) randomTime++;
      return "?=" + String.valueOf( randomTime );
    }
    // or it is ?-X/Y and we have to pick sometime between that offset
    Matcher anyEveryMatcher = ANY_EVERY_NEW.matcher(field);
    if ( anyEveryMatcher.matches() ) {
      int max = Integer.valueOf( anyEveryMatcher.group(1) );
      if ( max > count ) {
        throw new ConfigurationException
          ( "Cron field out of range; max "+max+" should be less than "+count );
      }
      String everyString = anyEveryMatcher.group(3);
      if ( everyString == null) {
        everyString = "1";
      }
      int every = Integer.valueOf( everyString );
      if ( every > count || every > max ) {
        throw new ConfigurationException(
          "Cron field out of range; " + every + " should be less than " +
          (max < count ? max : count)
        );
      }
      int randomTime;
      if ( every <= 1 ) {
        randomTime = 0;
      } else {
        randomTime = pickAny.nextInt( every - 1 );
      }
      if ( startAtOne ) randomTime++;
      return "?=" + randomTime + "-" + max + "/" + every;
    }

    // or it is ?/Y and we have to pick sometime between that offset
    anyEveryMatcher = ANY_EVERY.matcher(field);
    if ( anyEveryMatcher.matches() ) {
      int every = Integer.valueOf(anyEveryMatcher.group(1));
      if ( every > count ) {
        throw new ConfigurationException(
          "Cron field out of range; " + every + " should be less than " + count
        );
      }
      int randomTime;
      if ( every <= 1 ) {
        randomTime = 0;
      } else {
        randomTime = pickAny.nextInt( every - 1 );
      }
      int max = count - 1;
      if ( startAtOne ) {
        randomTime++;
        max++;
      }
      return "?=" + randomTime + "-" + max + "/" + every;
    }
    // can't handle anything else at this point
    throw new ConfigurationException(
      "Invalid cron syntax for field value '" + field + "'"
    );
  }


  /**
   * A schedule may contain '?' in its cron fields meaning run at any time in
   * the field.  E.g., ? ?/5 * * * means run every 5th hour at any minute in the
   * day.  The agent will resolve these '?' to concrete running times.  In the
   * example, the agent could decide to run on the 35th minute of the hour
   * starting at hour 2 (i.e., 35 2-23/5 * * *). The agent will choose running
   * times randomly.
   *
   * @param schedule  A schedule that may or may not contain '?'
   *
   * @return A schedule with all instances of '?' resolved to concrete running
   * times.
   *
   * @throws ConfigurationException if fields are out of range
   */
  public static Schedule chooseSchedule( Schedule schedule )
    throws ConfigurationException {

    Cron resolved_cron = null;
    
    if ( schedule.isSetCron() ) {
      resolved_cron = (Cron)schedule.getCron().copy();
    }
    if ( schedule.isSetCrontime() ) {
      resolved_cron = createCron( schedule.getCrontime() );
    }
    
    if ( resolved_cron == null ) { // '?' only appear in cron
      return schedule;
    }
    
    Schedule resolved = Schedule.Factory.newInstance();
    if ( resolved_cron.getMin() != null ) {
      resolved_cron.setMin( chooseCronFieldValue
        ( resolved_cron.getMin(), false, Constants.MINUTES_TO_HOUR) );
    }
    if ( resolved_cron.getHour() != null ) {
      resolved_cron.setHour( chooseCronFieldValue
        ( resolved_cron.getHour(), false, Constants.HOURS_TO_DAY) );
    }
    if ( resolved_cron.getMday() != null ) {
      resolved_cron.setMday( chooseCronFieldValue
        ( resolved_cron.getMday(), true, Constants.DAYS_TO_MONTH) );
    }
    if ( resolved_cron.getMonth() != null ) {
      resolved_cron.setMonth( chooseCronFieldValue
        (resolved_cron.getMonth(), true, Constants.MONTHS_TO_YEAR) );
    }
    if ( resolved_cron.getWday() != null ) {
      resolved_cron.setWday( chooseCronFieldValue
        ( resolved_cron.getWday(), false, Constants.DAYS_TO_WEEK) );
    }
    resolved.setCron( resolved_cron );
    return resolved;
  }
  /**
   * Compare two suite stages wrapper documents by per resource configs.  That
   * is if all per resource configs in s1 exist in s2, the documents are
   * identical and diff returns false.  Otherwise returns true.
   *
   * @param s1  A suite stages xml document
   *
   * @param s2  A suite stages xml document
   *
   * @param ignoreTarget Ignore the target hostnames in per resource config
   *                     comparison.  Good for comparing series conversion.
   *
   * @param ignoreUri Ignore the uri in per resource config which can sometimes
   *                  be present and other times not.
   *
   * @param allDiffs True if all diffs should be printed and false to stop
   *                  on first diff.
   *
   * @return Returns true if documents differ and false otherwise.
   *
   * @throws SuiteModificationException if unable to merge changes
   */
  public static boolean diff( SuiteStagesWrapper s1, SuiteStagesWrapper s2,
                              boolean ignoreTarget, boolean ignoreUri,
                              boolean allDiffs )
    throws SuiteModificationException {

    // read all per resource configs into single vectors for comparison
    Vector<SeriesConfig> configs1 = new Vector<SeriesConfig>();
    for( int i = 0; i < s1.getSeriesConfigCount(); i++ ) {
      MultiResourceConfig c = s1.getMultiResourceConfig(i);
      configs1.addAll
        (Arrays.asList(c.getPerResourceConfigs().getConfigArray()));
    }

    Vector<SeriesConfig> configs2 = new Vector<SeriesConfig>();
    for( int i = 0; i < s2.getSeriesConfigCount(); i++ ) {
      MultiResourceConfig c = s2.getMultiResourceConfig(i);
      configs2.addAll
        (Arrays.asList(c.getPerResourceConfigs().getConfigArray()));
    }

    // look for each config from s1 in s2 and remove them as found
    for( SeriesConfig config1 : configs1 ) {
      String resource1 = config1.getResourceHostname();
      String nickname1 = config1.getNickname();
      String seriesDesc1 = "(" + nickname1 + ", " + resource1;
      if (config1.isSetTargetHostname() )
        seriesDesc1 += ", " + config1.getTargetHostname();
      seriesDesc1 += ")";
      logger.debug( "Looking for match for resource " + seriesDesc1 );

      SeriesConfig config2 = null;
      for( int j = 0; j <configs2.size(); j++ ) {
        String resource2 = configs2.get(j).getResourceHostname();
        String nickname2 = configs2.get(j).getNickname();
        String seriesDesc2 = "(" + nickname2 + ", " + resource2;
        if (configs2.get(j).isSetTargetHostname() )
          seriesDesc2 += ", " + configs2.get(j).getTargetHostname();
        seriesDesc2 += ")";
        if ( seriesDesc1.equals(seriesDesc2) ) {
          config2 = configs2.remove(j);
          break;
        }
      }
      if ( config2 == null ) {
        logger.error( "Cannot find match for config " + seriesDesc1 );
        if ( ! allDiffs ) {
          return true;
        } else {
          continue;
        }
      }
      logger.info( "Match found for config " + seriesDesc1 );
      if ( ignoreTarget ) {
        config1.setTargetHostname("");
        config2.setTargetHostname("");
      }
      if ( ignoreUri ) {
        config1.getSeries().setUri("");
        config2.getSeries().setUri("");
      }
      if ( ! configEqual(config1, config2) ) {
        logger.error( "Config not equal" );
        logger.error( "s1:" + config1.toString() );
        logger.error("s2:" + config2.toString());
        if ( ! allDiffs ) return true;
      }
    }

    // if there are configs left, than the documents are different
    if (configs2.size() > 0)
      logger.error( "Found " + configs2.size() +
        " series left in config2 for suite " +
        s2.getSuiteDocument().getSuite().getName() );
    for( SeriesConfig config : configs2 ) {
      String seriesDesc = "(" + config.getNickname() + ", " +
                            config.getResourceHostname();
      if (config.isSetTargetHostname() )
        seriesDesc += ", " + config.getTargetHostname();
      seriesDesc += ")";
      logger.error( "No match found for " + seriesDesc );
    }

    return ( configs2.size() > 0 );
  }

  public SuiteWrapper updatePackage( String name, String newVersion,
                                     String newUri ){
    SuiteWrapper suite = new SuiteWrapper();
    copySuiteAttributes( suite );
    XmlObject[] results = this.suiteStages.selectPath(
      "//multiResourceConfig[latestVersion='true']/" +
      "config[series[name='" + name + "']]"
    );
    for( XmlObject result : results ) {
      SeriesConfig deletedConfig = (SeriesConfig)result.copy();
      deletedConfig.setAction( "delete" );
      suite.appendSeriesConfig( deletedConfig );
      SeriesConfig addedConfig = (SeriesConfig)result.copy();
      addedConfig.getSeries().setVersion( newVersion );
      addedConfig.getSeries().setUri( newUri );      
      addedConfig.setAction( "add" );
      suite.appendSeriesConfig( addedConfig );
    }
    return suite;
  }

  /**
   * Return the SuiteStagesDocument contained in this SuiteStagesWrapper object.
   *
   * @return The SuiteStagesDocument stored in this object.
   */
  public SuiteStagesDocument getDocument() {
    return this.suiteStages;
  }

  /**
   * Extract a suite document with all resource series config objects contained
   * in this SuiteStages document.  This document would be appropriate for
   * sending to the agent's reporter manager controller.
   *
   * @return A suite document containing all resource series configs in this
   * SuiteStages document.
   */
  public SuiteDocument getPerResourceSuiteDocument() {
    return createSuiteDocument( "//perResourceConfigs/config" );
  }

  /**
   * Return the number of per resource series configs stored in this SuiteStages
   * document.
   *
   * @return  The number of resource series configs stored in this SuiteStages
   * document.
   */
  public int getResourceSeriesConfigCount() {
    return this.suiteStages.selectPath( "//perResourceConfigs/config" ).length;
  }

  /**
   * Return the number of multi-resource series configs stored in this
   * SuiteStages document.
   *
   * @return  The number of multi-resource configs stored in this SuiteStages
   * document.
   */
  public int getSeriesConfigCount() {
    return this.suiteStages.getSuiteStages().getMultiResourceConfigs().
      sizeOfMultiResourceConfigArray();
  }

  /**
   * Iterate through the per resource series configs and extract suites
   * for each of the resources.
   *
   * @return A HashMap where the keys are resource names and the entries are
   * the suite wrapper objects.
   *
   */
  public HashMap<String, SuiteWrapper> getResourceSuites( ) {
    return getResourceSuites( this.getPerResourceSuiteDocument() );
  }


  /**
   * Extract suites for each of the resources from the provided SuiteDocument
   *
   * @param suiteDoc  A suite document with resource series configs.
   *
   * @return A HashMap where the keys are resource names and the entries are
   * the suites documents.
   *
   */
  public static HashMap<String, SuiteWrapper> getResourceSuites
    ( SuiteDocument suiteDoc ) {

    HashMap<String, SuiteWrapper> suites = new HashMap<String, SuiteWrapper>();
    for ( SeriesConfig seriesConfig :
          suiteDoc.getSuite().getSeriesConfigs().getSeriesConfigArray() ) {
      SeriesConfig newConfig = (SeriesConfig)seriesConfig.copy();
      String resource = newConfig.getResourceHostname();
      if ( ! suites.containsKey( resource ) ) {
        SuiteWrapper suite = new SuiteWrapper();
        suite.copySuiteAttributes( suiteDoc );
        suites.put( resource, suite );
      }
      Schedule sched = newConfig.getSchedule();
      if(sched.getCron() != null) {
        sched.getCron().setMin( sched.getCron().getMin().replaceAll("\\?=", "") );
        sched.getCron().setHour(
          sched.getCron().getHour().replaceAll("\\?=", "")
        );
        sched.getCron().setMday(
          sched.getCron().getMday().replaceAll("\\?=", "")
        );
        sched.getCron().setWday(
          sched.getCron().getWday().replaceAll("\\?=", "")
        );
        sched.getCron().setMonth(
          sched.getCron().getMonth().replaceAll("\\?=", "")
        );    
      }
      suites.get(resource).appendSeriesConfig( newConfig );
    }
    return suites;
  }

  /**
   * Extract a suite document with all multi-resource series config objects
   * contained in this SuiteStages document.  This document is appropriate
   * for sending to the user.
   *
   * @return  A suite document containing all multi-resource series configs in
   * this SuiteStages document.
   */
  public SuiteDocument getSuiteDocument() {
    SuiteWrapper suite = new SuiteWrapper();
    copySuiteAttributes( suite );
    for ( int i = 0; i < this.getSeriesConfigCount(); i++ ) {
      MultiResourceConfig multiConfig = this.getMultiResourceConfig(i);
      SeriesConfig config = (SeriesConfig)multiConfig.getConfig().copy();
      if ( multiConfig.isSetLatestVersion() ) {
        config.getSeries().unsetVersion();
      }
      suite.appendSeriesConfig( config );
    }

    return suite.getSuiteDocument();
  }

  /**
   * Extract a suite document with all multi-resource series config objects
   * contained in this SuiteStages document.  This document is appropriate
   * for sending to the user. This document is identical to the user's
   * config except that latest version is filled in with concrete version
   * numbers.
   *
   * @return A suite document containing all multi-resource series configs in
   * this SuiteStages document.
   */
  public SuiteDocument getSuiteDocumentWithVersions() {
    return createSuiteDocument( "//multiResourceConfig/config" );
  }
  
  /**
   * Apply multi-resource series config changes in the provided suite to the
   * SuiteStages document.
   *
   * @param suiteChanges  A suite document containing multi-resource series
   * config adds/deletes to be applied to the existing suiteStages document.
   *
   * @return A SuiteStages document containing the expanded multi-resource
   * series config adds/deletes that were applied to the SuiteStages document.
   *
   * @throws SuiteModificationException if unable to merge changes
   */
  public SuiteStagesWrapper modify( SuiteWrapper suiteChanges )
    throws SuiteModificationException {

    SuiteStagesWrapper committedChanges;
    try {
      committedChanges = new SuiteStagesWrapper( this.resources );
    } catch ( InstantiationException e ) {
      throw new SuiteModificationException(
        "Unable to expand the multi-resource series config changes: " + e
      );
    }
    String suitename = suiteChanges.getSuiteDocument().getSuite().getName();
    logger.debug( "Applying committedChanges to suite '" + suitename + "'" );

    // apply top level document committedChanges
    if ( suiteChanges.getSuiteDocument().getSuite().isSetName() ) {
      this.suiteStages.getSuiteStages().setName( suitename );
      committedChanges.getDocument().getSuiteStages().setName( suitename );
    }
    if ( suiteChanges.getSuiteDocument().getSuite().isSetDescription() ) {
      suiteStages.getSuiteStages().setDescription(
        suiteChanges.getSuiteDocument().getSuite().getDescription()
      );
      committedChanges.getDocument().getSuiteStages().setDescription(
        suiteChanges.getSuiteDocument().getSuite().getDescription()
      );
    }
    suiteStages.getSuiteStages().setGuid(
      suiteChanges.getSuiteDocument().getSuite().getGuid()
    );
    committedChanges.getDocument().getSuiteStages().setGuid(
      suiteChanges.getSuiteDocument().getSuite().getGuid()
    );
    if ( suiteChanges.getSuiteDocument().getSuite().isSetVersion() ) {
      suiteStages.getSuiteStages().setVersion(
        suiteChanges.getSuiteDocument().getSuite().getVersion()
      );
      committedChanges.getDocument().getSuiteStages().setVersion(
        suiteChanges.getSuiteDocument().getSuite().getVersion()
      );
    }  

    // look through configs and apply add or delete
    SeriesConfig[] configs = suiteChanges.getSeriesConfigs();
    for( int i = 0; i < configs.length; i++ ) {
      MultiResourceConfig committedConfigChange;
      if ( configs[i].getAction().equals("add") ) {
        try {
          committedConfigChange = createMultiResourceConfig( configs[i] );
          this.appendMultiResourceConfig( committedConfigChange );
        } catch ( ConfigurationException e ) {
          throw new SuiteModificationException(
            "Problem adding series config " + i + ": ", e
          );
        }
      } else if ( configs[i].getAction().equals("delete") ) {
        committedConfigChange = this.deleteMultiResourceConfig( configs[i] );
      } else {
        continue;
      }
      committedChanges.appendMultiResourceConfig( committedConfigChange );
    }

    return committedChanges;
  }

  /**
   * Remove the suite document from disk.
   *
   */
  public void remove() {

    if ( this.filePath == null ) return;

    File file = new File( this.filePath );
    if ( file.delete() ) {
      logger.info( "Deleted " + this.filePath );
    } else {
      logger.error( "Unable to delete file " + this.filePath );
    }
  }

  /**
   * Save the suite document to disk.
   *
   * @throws CrypterException if unable to encrypt document
   * @throws IOException if unable to write to disk
   */
  public void save() throws CrypterException, IOException {

    if ( this.filePath == null ) return;

    String xml = XmlWrapper.prettyPrint(this.suiteStages, "  ");
    save( xml, this.filePath, this.passphrase );
    logger.debug(
      "Saved suite '" + suiteStages.getSuiteStages().getName() +
      "' to file '" + filePath + "'"
    );

  }

  /**
   * Expand the macros contained in the suite's seriesConfig using the
   * definitions provided in resources and create multiple seriesConfig for
   * suiteExpanded.
   *
   * @param seriesConfig  The seriesConfig from a SeriesConfig
   *                      object from the suite that may contain macros that
   *                      need to be expanded.
   * @param resource      The name of the resource this seriesConfig is being
   *                      expanded for.
   * @return A vector or seriesConfig objects that can be inserted into a
   * suite expanded document.
   *
   * @throws ConfigurationException if unable to expand series config
   */
  public Vector<SeriesConfig> seriesConfigCrossProduct(
    SeriesConfig seriesConfig, String resource
    ) throws ConfigurationException {

    Vector<String> targetResources = new Vector<String>();
    Vector<String> seriesConfigCPText = this.resources.expand(
      seriesConfig.toString(), resource, null, targetResources
    );
    Vector<SeriesConfig> seriesConfigCP = new Vector<SeriesConfig>();
    for ( int i = 0; i < seriesConfigCPText.size(); i++ ) {
      try {
        SeriesConfig config = SeriesConfig.Factory.parse(seriesConfigCPText.get(i));
        if ( targetResources.size() > 0 ) {
          config.setTargetHostname(targetResources.get(i));
        }
        seriesConfigCP.add( config );
      } catch ( XmlException e ) {
        logger.error( "Unexpected xml exception parsing series config", e );
      }
    }
    return seriesConfigCP;
  }

  /**
   * Set a new SuiteStages document for this object.
   *
   * @param suiteStages  A SuiteStages document.
   */
  public void setDocument( SuiteStagesDocument suiteStages ) {
    this.suiteStages = suiteStages;
  }

  /**
   * Set the new resource configuration information and apply it to the
   * existing suite.  The changes will be returned.
   *
   * @param resources The new resource configuration information
   *
   * @return The resulting changes of the new resource configuration.
   *
   * @throws Exception if unable to apply to existing resources
   */
  public SuiteDocument setResources( ResourcesWrapper resources )
    throws Exception {

    // create document to store resource changes
    SuiteWrapper resourceConfigDiffs = new SuiteWrapper();
    copySuiteAttributes( resourceConfigDiffs );

    if ( this.resources.equals(resources) ) {
      logger.info( "No change in resource document" );
      return resourceConfigDiffs.getSuiteDocument();
    }

    // get the current suite document
    SuiteDocument currentSuiteDoc = this.getSuiteDocumentWithVersions();
    SuiteWrapper currentSuite = new SuiteWrapper( currentSuiteDoc );

    // create a new suite object with the new resources
    SuiteStagesWrapper newSuiteStagesWrapper = new SuiteStagesWrapper( resources );
    newSuiteStagesWrapper.modify( currentSuite );

    // set new resource document
    this.resources = resources;

    // iterate thru multi resource configs and propogate resource changes;
    // mark the changes in resource document so they can be returned
    for ( int i = 0; i < this.getSeriesConfigCount(); i++ ) {
      logger.debug( "Comparing series config " + i );
      this.recreateMultiResourceConfig(
        this.getMultiResourceConfig(i),
        resourceConfigDiffs
      );
    }

    // save new document
    this.save();

    // return changes
    return resourceConfigDiffs.getSuiteDocument();
  }

  // Private Functions

  /**
   * Append the given expanded multi-resource series config to the existing
   * SuiteStages document.
   *
   * @param seriesConfig   An expanded multi-resource series config object.
   */
  private void appendMultiResourceConfig( MultiResourceConfig seriesConfig ) {

    int lastIndex = this.getSeriesConfigCount();
    suiteStages.getSuiteStages().getMultiResourceConfigs().
      insertNewMultiResourceConfig( lastIndex );
    suiteStages.getSuiteStages().getMultiResourceConfigs().
      setMultiResourceConfigArray( lastIndex, seriesConfig );
  }

  /**
   * Append an expanded per-resource series config to the provided
   * multi-resource series config.
   *
   * @param mrConfig A multi-resource series config that is in the process
   * of being expanded.
   * @param prConfig A per-resource series config to add to the multi-resource
   * series config.
   */
  private void appendPerResourceConfig( MultiResourceConfig mrConfig,
                                        SeriesConfig prConfig ) {
    int numConfigs =
      mrConfig.getPerResourceConfigs().sizeOfConfigArray();
    logger.debug( "Adding per resource config " + numConfigs );
    mrConfig.getPerResourceConfigs().insertNewConfig( numConfigs );
    mrConfig.getPerResourceConfigs().setConfigArray(
      numConfigs, prConfig
    );
  }

  /**
   * Change the action in all of the series configs of the provided expanded
   * multi-resource series config.
   *
   * @param config  An expanded multi-resource series config.
   *
   * @param action  The action (add/delete) to change to.
   */
  private void changeMultiResourceConfigAction( MultiResourceConfig config,
                                                String action ) {
    config.getConfig().setAction( action );
    for (SeriesConfig rCfg : config.getPerResourceConfigs().getConfigArray()) {
      rCfg.setAction(action);
    }
  }

  /**
   * Copy the top level suite attributes: name, guid, and description to
   * supplied suite document.
   *
   * @param suite  A suite that where the top level suite attributes will be
   * copied to
   */
  private void copySuiteAttributes( SuiteWrapper suite ) {
    // apply top level document attributes to document
    if ( this.suiteStages.getSuiteStages().isSetName() ) {
      suite.getSuiteDocument().getSuite().setName(
        this.suiteStages.getSuiteStages().getName()
      );
    }
    if ( this.suiteStages.getSuiteStages().isSetDescription() ) {
      suite.getSuiteDocument().getSuite().setDescription(
        this.suiteStages.getSuiteStages().getDescription()
      );
    }
    if ( this.suiteStages.getSuiteStages().isSetVersion() ) {
      suite.getSuiteDocument().getSuite().setVersion(
        this.suiteStages.getSuiteStages().getVersion()
      );
    }
    suite.getSuiteDocument().getSuite().setGuid(
      this.suiteStages.getSuiteStages().getGuid()
    );
  }

  /**
   * Converts suite to a suiteExpanded document using the information in the
   * resource configuration object to expand the supplied resources
   * and macros. A resource is defined in the supplied resource configuration
   * object and can contain any number of macros.  A macro is defined
   * in the resource configuration document and can contain more than one value
   * in which case, the SeriesConfig will be expanded into multiple
   * SeriesConfigs equal to the  cross product of the macro values. A
   * macro is represented as '@\w+@' (a string wrapped by '@') and can appear
   * in the arg value, setup, or cleanup tags of the suite document. For
   * example, if a suite SeriesConfig has a resource set that contains 2
   * resources and a Series contains 2 arguments with macros mapping to
   * 1 and 2 values respectively, then 2 X 1 X 2 = 4 reportSetConfigs will
   * be created in suiteExpanded.

   * @param config a series config to expand
   *
   * @return  An expanded series config object
   *
   * @throws ConfigurationException if unable to expand
   */
  private MultiResourceConfig createMultiResourceConfig( SeriesConfig config )
    throws ConfigurationException {

    if ( config.isSetNickname() ) {
      logger.info( "Expanding series config '" + config.getNickname() + "'" );
    } else {
      logger.info(
        "Expanding series config for '" + config.getSeries().getName() + "'"
      );
    }

    // create multi resource config object and set its config (i.e., the orig)
    MultiResourceConfig multiResourceConfig =
      MultiResourceConfig.Factory.newInstance();
    multiResourceConfig.addNewPerResourceConfigs();
    SeriesConfig configCopy = (SeriesConfig)config.copy();
    Pattern latestPattern = Pattern.compile( "([^\\?=]+)" );
    logger.debug( "Version found " + configCopy.getSeries().getVersion() );
    Matcher matcher=latestPattern.matcher(configCopy.getSeries().getVersion());
    if ( matcher.find() ){
      logger.debug( "Setting latest version for " + config.getSeries().getName() );
      String version = matcher.group();
      multiResourceConfig.setLatestVersion( true );
      configCopy.getSeries().setVersion( version );
    }
    multiResourceConfig.setConfig( configCopy );

    // now need to populate multi resource config's per resource configs
    // We will get all the affected resources and expand series config for it
    String resources[];
    SeriesConfig resourceConfig = (SeriesConfig)configCopy.copy();
    if(resourceConfig.isSetResourceSetName()) {
      resources = this.resources.getResources(
        config.getResourceSetName(), true
      );
      resourceConfig.unsetResourceSetName();
    } else if(config.isSetResourceXpath()) {
      resources = this.resources.getResources(config.getResourceXpath(), true);
      resourceConfig.unsetResourceXpath();
    } else {
      resources = new String[] { config.getResourceHostname() };
    }
    for ( String resource : resources ) {
      resourceConfig.setResourceHostname( resource );
      Vector<SeriesConfig> seriesConfigCP = seriesConfigCrossProduct
        ( resourceConfig, resource );
      // for each expanded series config, select a schedule and create a per
      // resource series config
      for( SeriesConfig expandedConfig : seriesConfigCP ) {
        expandedConfig.setSchedule(
          chooseSchedule( expandedConfig.getSchedule() )
        );
        // expand @@ in context with: REPORTER_URL REPORTER_ARGS
        String context = expandedConfig.getSeries().getContext();
        String cmdline = expandedConfig.getSeries().getName();
        Args.Arg[] args = expandedConfig.getSeries().getArgs().getArgArray();
        String[] argStrings = new String[args.length];
        for( int k = 0; k < args.length; k++ ) {
          argStrings[k] = " -" + args[k].getName() + "=" +
                          "\"" + args[k].getValue() + "\"";
        }
        Arrays.sort(argStrings);
        for( String argString : argStrings ) {
          cmdline += argString;
        }
        logger.debug( "Context before: " + context );
        context = XmlWrapper.unescape( context.replaceAll(
          "@@", XmlWrapper.escape(XmlWrapper.unescape(cmdline))
        ) );
        logger.debug( "Context after: " + context );
        expandedConfig.getSeries().setContext( context );
        appendPerResourceConfig( multiResourceConfig, expandedConfig );
      }
    }
    return multiResourceConfig;
  }

  /**
    * Retrieve series configs using the specified xpath expression and return
    * as a suite document.
    *
    * @param xpath  A xpath expression that extracts series configs from the
    * suite stages document.
    *
    * @return A suite document containing the extract series config object.
    */
   private SuiteDocument createSuiteDocument( String xpath ) {

     SuiteWrapper suite = new SuiteWrapper();
     copySuiteAttributes( suite );
     XmlObject[] configs = this.suiteStages.selectPath( xpath );
     logger.debug( "Found " + configs.length + " configs" );
     for ( XmlObject config : configs ) {
       suite.appendSeriesConfig( (SeriesConfig)config.copy() );
     }
     return suite.getSuiteDocument();
   }

  /**
   * Delete the specified series config object from the suite stages document
   * and return it (the expanded -- multi resource config object).
   *
   * @param config  The config to search for in the suite document and delete
   *
   * @return The expanded version of the series config.
   *
   * @throws SuiteModificationException if unable to find config
   */
  private MultiResourceConfig deleteMultiResourceConfig( SeriesConfig config )
    throws SuiteModificationException {

    String id = config.getSeries().getName();
    logger.debug( "Attempting to delete series config " + id );

    MultiResourceConfig deletedMultiResourceConfig = null;
    Pattern latestPattern = Pattern.compile( "([^\\?=]+)" );
    Matcher matcher=latestPattern.matcher(config.getSeries().getVersion());
    if ( matcher.find() ){
      String version = matcher.group();
      config.getSeries().setVersion( version );
    }
    config.setAction( "add" ); // configs are stored as adds
    MultiResourceConfig[] multiResourceConfigs =
      this.suiteStages.getSuiteStages().getMultiResourceConfigs().
      getMultiResourceConfigArray();
    int i = 0;
    for( ; i < multiResourceConfigs.length; i++) {
      if ( configEqual( config, multiResourceConfigs[i].getConfig() )) {
        logger.debug( "Deleting series config " + id );
        deletedMultiResourceConfig =
        (MultiResourceConfig)multiResourceConfigs[i].copy();
        changeMultiResourceConfigAction( deletedMultiResourceConfig, "delete" );
        this.suiteStages.getSuiteStages().getMultiResourceConfigs().
          removeMultiResourceConfig(i);
        break;
      }
    }
    config.setAction( "delete" ); // switch it back to delete
    if ( i >= multiResourceConfigs.length ) {
      String error = "Unable to locate existing match for series config " + id;
      logger.error( error );
      throw new SuiteModificationException( error );
    }
    return deletedMultiResourceConfig;
  }

  /**
   * Return the specified multi resource config from this document.
   *
   * @param multiResourceId  The index of the multi resource config to retrieve
   *
   * @return A multi resource config
   */
  private MultiResourceConfig getMultiResourceConfig( int multiResourceId ){
    return this.getDocument().getSuiteStages().getMultiResourceConfigs().
        getMultiResourceConfigArray(multiResourceId);
  }

  /**
   * Retrieve the specified resource series config from the specified multi
   * resource config object.
   *
   * @param mrConfig  The multi resource config to retrieve the per resource
   * config from.
   *
   * @param resourceId The id of the per resource config to retrieve from
   * the multi resource config
   * @return  A series config for a resource.
   */
  private SeriesConfig getResourceConfig( MultiResourceConfig mrConfig,
                                          int resourceId ){
    return mrConfig.getPerResourceConfigs().getConfigArray(resourceId);
  }

  /**
   * Return the number of per resource configs in the specified multi resource
   * config object.
   *
   * @param mrConfig  The multi resource config object to retrieve the number
   * of per resource configs from.
   *
   * @return The number of per resource configs attached to the specified
   * multi resource config object.
   */
  private int getResourceConfigCount( MultiResourceConfig mrConfig ) {
    return mrConfig.getPerResourceConfigs().sizeOfConfigArray();
  }

  /**
   * Given a series config that has already been expanded, re-expand it
   * using the current resources document.  Compare newly-expanded multi
   * resource config to the original and add/delete per resource configs
   * as necessary in the original (series configs that are
   * not affected by the resource change will remain).  The changes that
   * are made are recorded in resourceConfigDiffs.
   *
   * @param current A multi resource config object that was expanded using a
   * different resources file and may need to be changed depending on the
   * current resource file.
   *
   * @param resourceConfigDiffs  A list of series configs that represent
   * resource diffs from the current multi resource config and the
   * newly-expanded multi resource config
   *  *
   * @throws ConfigurationException if unable to re-expand
   */
  private void recreateMultiResourceConfig( MultiResourceConfig current,
                                            SuiteWrapper resourceConfigDiffs )
  throws ConfigurationException {

    // re-expand the series config
    MultiResourceConfig update = createMultiResourceConfig(current.getConfig());

    // compare the current and newly-expanded series config; delete every
    // per resource config in current that does not exist in newly-expanded;
    // and add every per resource config in newly expanded that does not
    // appear in current
    logger.debug( "Looking for series configs to delete" );
    for( int i = 0; i < this.getResourceConfigCount(current); i++ ) {
      for( int j = 0; j < this.getResourceConfigCount(update); j++ ) {
        if ( configEqual(this.getResourceConfig(current,i),
                         this.getResourceConfig(update, j)) ) {
          break;
        }
        if ( j == this.getResourceConfigCount(update) - 1) {
          // record changes
          logger.debug( "Deleting per resource config " + i );
          SeriesConfig resourceConfig =
            (SeriesConfig)this.getResourceConfig(current,i).copy();
          resourceConfig.setAction( "delete" );
          resourceConfigDiffs.appendSeriesConfig( resourceConfig );
          current.getPerResourceConfigs().removeConfig(i);
          i--; // delete will shrink the array by 1
        }
      }
    }

    logger.debug( "Looking for new series config to add" );

    // compare the current and newly-expanded series config; add every
    // per resource config in newly-expanded that does not exist in current;
    // if 2 per resource configs are equivalent, we don't have to do anything
    // because they would be fixed in the upper loop
    for( int i = 0; i < this.getResourceConfigCount(update); i++ ) {
      int j = 0;
      for( ; j < this.getResourceConfigCount(current); j++ ) {
        if ( configEqual(this.getResourceConfig(update, i),
                         this.getResourceConfig(current,j)) ) {
          break;
        }
      }
      if ( j >= this.getResourceConfigCount(current) ) {
        // Copy others
        SeriesConfig resourceConfig =
          (SeriesConfig)this.getResourceConfig(update, i).copy();
        resourceConfig.setAction( "add" );
        resourceConfigDiffs.appendSeriesConfig( resourceConfig );
        // add per resource config to our config
        this.appendPerResourceConfig(
          current,
          (SeriesConfig)
          update.getPerResourceConfigs().getConfigArray(i).copy()
        );
      }
    }

  }

  /**
   * Provide a command line interface to examining existing suite stage
   * wrapper files or expanding them.
   *
   * SuiteStagesWrapper <suiteFile> <resourceFile> [<dir>]
   *
   * where suiteFile can be a suite stages doc or a plain suite doc. If dir
   * argument is present, will print out the suites for each host.
   * 
   * @param args   The command line arguments
   */
  public static void main(String[] args) {
    String usage =
      "Usage: \n" +
      "\n" +
      "SuiteStagesWrapper [expand | diff] <args> \n" +
      "\n" +
      "     expand -i <inca file> [-s <suite dir>]\n" +
      "     diff [-a yes|no] [-it yes|no] [-iu yes|no] -s1 <suite dir> -s2 <suite dir>\n";
    if ( args.length < 1 ) {
      System.err.println( usage );
      System.exit( 1 );
    }

    // Read command
    Vector<String> argsList = new Vector<String>(Arrays.asList(args));
    String command = argsList.remove(0);
    String[] commandArgs = argsList.toArray(new String[argsList.size()]);

    Logger.getRootLogger().setLevel(Level.INFO);
    BasicConfigurator.configure();

    String prop;
    if ( command.equals("expand") ) {
      ConfigProperties argProps = new ConfigProperties();
      try {
        argProps.setPropertiesFromArgs(EXPAND_OPTS, commandArgs);
      } catch (ConfigurationException e) {
        System.err.println("\nError parsing options: " + e );
        System.err.println( usage );
        System.exit(1);
      }

      if ( (prop = argProps.getProperty("inca")) == null ) {
        System.err.println( "\nError: missing inca file");
        System.err.println( usage );
        System.exit(1);
      }
      String incaFile = prop;
      String expandedDir = null;
      if ( (prop = argProps.getProperty("suite-dir")) != null ) {
        expandedDir = prop;
      }
      File dir = new File(expandedDir);
      if ( ! dir.exists() ) {
        if ( dir.mkdirs() ) {
          logger.info( "Directory " + expandedDir + " successfully created" );
        } else {
          System.err.println( "Unable to create " + expandedDir );
          System.exit(1);
        }
      }
      logger.info
        ("Expanding suites from " + incaFile);
      try {
        IncaDocument incaDoc = IncaDocument.Factory.parse(new File(incaFile));
        ResourceConfigDocument rcDoc = ResourceConfigDocument.Factory.newInstance();
        rcDoc.setResourceConfig( incaDoc.getInca().getResourceConfig() );
        Repositories repos = new Repositories();
        String[] repoArray = incaDoc.getInca().getRepositories().getRepositoryArray();
        for( String repoUrl : repoArray ) {
          logger.debug( "Adding repository " + repoUrl );
          repos.addRepository( new Repository(new URL(repoUrl)) );
        }
        ResourcesWrapper resources = new ResourcesWrapper( rcDoc );
        for ( Suite suite : incaDoc.getInca().getSuites().getSuiteArray() ) {
          for ( SeriesConfig sc : suite.getSeriesConfigs().getSeriesConfigArray() ) {
            Repository r = repos.getRepositoryForPackage
              (sc.getSeries().getName(), sc.getSeries().getVersion() );
            Properties pkg = r.getProperties
              (sc.getSeries().getName(), sc.getSeries().getVersion() );
            sc.getSeries().setVersion( pkg.getProperty("version") );
          }
          SuiteWrapper suiteWrapper = new SuiteWrapper( suite );
          SuiteStagesWrapper suiteStages = new SuiteStagesWrapper( resources );
          suiteStages.modify( suiteWrapper );
          if ( expandedDir != null ) {
            File suiteFile =
              new File( expandedDir + File.separator + suite.getName() + ".xml");
            suiteStages.getDocument().save( suiteFile );
            logger.info( "Saving suite " + suite.getName() + " to " + suiteFile);
          } else {
            System.out.println( suiteStages.getDocument().xmlText() );
          }
        }
      } catch (Exception e) {
        System.err.println( "Unable to expand " + incaFile + ": " + e );
        e.printStackTrace();
      }
    } else if ( command.equals("diff") ) {
      ConfigProperties argProps = new ConfigProperties();
      try {
        argProps.setPropertiesFromArgs(DIFF_OPTS, commandArgs);
      } catch (ConfigurationException e) {
        System.err.println("\nError parsing options: " + e );
        System.err.println( usage );
        System.exit(1);
      }

      if ( (prop = argProps.getProperty("suite-dir1")) == null ) {
        System.err.println( "\nError: missing suite directory 1");
        System.err.println( usage );
        System.exit(1);
      }
      File suiteDir1 = new File( prop );
      if ( (prop = argProps.getProperty("suite-dir2")) == null ) {
        System.err.println( "\nError: missing suite directory 2");
        System.err.println( usage );
        System.exit(1);
      }
      File suiteDir2 = new File( prop );
      if ( ! suiteDir1.exists() ) {
        logger.error( suiteDir1.getPath() + " does not exist");
        System.exit(1);
      }
      if ( ! suiteDir2.exists() ) {
        logger.error( suiteDir2.getPath() + " does not exist");
        System.exit(1);
      }

      File[] suiteFiles1 = suiteDir1.listFiles();
      for( File suiteFile1 : suiteFiles1 ) {
        logger.info( "Comparing file " + suiteFile1.getName() );
        File suiteFile2 = new File( suiteDir2 + File.separator + suiteFile1.getName() );
        if ( ! suiteFile2.exists() ) {
          logger.error("Suite file " + suiteFile1.getName() +
                       " does not exist in " + suiteDir2.getPath());
        } else {
          try {
            SuiteStagesDocument doc1 = SuiteStagesDocument.Factory.parse(suiteFile1);
            SuiteStagesDocument doc2 = SuiteStagesDocument.Factory.parse(suiteFile2);
            boolean ignoreTarget = false;
            boolean ignoreUri = false;
            boolean allDiffs = false;
            if ( (prop = argProps.getProperty("ignore-target")) != null ) {
              ignoreTarget = prop.equalsIgnoreCase("true") ||
                             prop.equalsIgnoreCase("yes");
            }
            if ( (prop = argProps.getProperty("ignore-uri")) != null ) {
              ignoreUri = prop.equalsIgnoreCase("true") ||
                          prop.equalsIgnoreCase("yes");
            }
            if ( (prop = argProps.getProperty("all-diffs")) != null ) {
              allDiffs = prop.equalsIgnoreCase("true") ||
                          prop.equalsIgnoreCase("yes");
            }
            if ( diff(new SuiteStagesWrapper(doc1),new SuiteStagesWrapper(doc2),
                      ignoreTarget, ignoreUri, allDiffs )) {
              logger.error( "Suite file " + suiteFile1.getName() + " differs" );
              if ( ! allDiffs ) break;
            }
          } catch (Exception e) {
            logger.error( "Problem parsing xml: " + e, e );
          }
        }
      }
    } else {
      logger.error( "Unknown command " + command );
      System.exit(1);
    }
    System.exit( 0 );
  }

}
