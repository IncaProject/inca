package edu.sdsc.inca.agent;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import java.util.Hashtable;
import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.util.SuiteStagesWrapper;
import edu.sdsc.inca.util.CrypterException;

/**
 * Stores the suites currently stored at the reporter agent and provides
 * methods for accessing them. Suites are stored in a directory under their
 * name and will be persistent.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class SuiteTable {
  private Hashtable<String, SuiteStagesWrapper> suites =
    new Hashtable<String, SuiteStagesWrapper>();
  private String directory = null;
  Logger logger = Logger.getLogger(this.getClass().toString());

  /**
   * Create new suite table and read in any existing suites from file.
   *
   * @param directory  Path to the directory where suite files can be stored.
   * @param resources  The resource configuration information.
   *
   */
  public SuiteTable( String directory, ResourcesWrapper resources ) {
    this.directory = directory;
    File suiteDir = new File( directory );
    if ( ! suiteDir.exists() ) {
      logger.info( "Creating suite directory '" + directory + "'" );
      suiteDir.mkdirs();
    }
    String[] suitePaths = suiteDir.list(
      new FilenameFilter() {
        public boolean accept( File dir, String name ) {
          return name.endsWith(".xml");
        }
      }
    );
    for( String suitePath : suitePaths ) {
      String suiteName = suitePath.replaceAll( "\\.xml", "" );
      try {
        SuiteStagesWrapper suiteStages = new SuiteStagesWrapper
          ( directory + File.separator + suitePath, resources );
        suites.put( suiteName, suiteStages );
      } catch ( Exception e ) {
        logger.error(
          "Unable to read existing suite '" + suiteName + "' from disk", e
        );
      }
    }
  }

  /**
   * Traverses thru all of its suites and applies the new resources file to
   * each suite.  If there are differences in the suite expanded file produced
   * by the new resources from the old suite expanded, they are returned as
   * adds/deletes in a suite expanded document.  This function returns an
   * array of suite expanded documents, one for each suite.
   *
   * @param newResources  The new resource configuration information.
   *
   * @return An array of expanded suite documents
   */
  public SuiteDocument[] applyResourceChanges( ResourcesWrapper newResources ){

    String[] suiteNames = getNames();
    logger.debug( "Applying resource changes to "+suiteNames.length+" suites" );
    SuiteDocument[] changes = new SuiteDocument[suiteNames.length];
    for ( int i = 0; i < suiteNames.length; i++ ) {
      logger.info( "Applying resource changes to " + suiteNames[i] );
      try {
        SuiteStagesWrapper suiteStages = getSuite( suiteNames[i] );
        changes[i] = suiteStages.setResources( newResources );
        suites.put( suiteNames[i], suiteStages );
      } catch ( Exception e ) {
        logger.error(
          "Unable to apply resource changes to suite " + suiteNames[i], e
        );
      }
    }
    return changes;
  }

  /**
   * Get the directory where the suite table stores the suites to disk.
   *
   * @return A string containing the path to the directory where suites are
   * stored.
   */
  public String getDirectory() {
    return directory;
  }

  /**
   * Returns true if the named suite is currently stored in the object.
   *
   * @param suiteName The name of the suite we're looking for.
   *
   * @return true if the named suite is currently stored in the object;
   * otherwise false.
   */
  public synchronized boolean hasSuite( String suiteName ) {
    return suites.containsKey( suiteName );
  }

    /**
   * Return the contents of the named suite.  If there is not suite by that
   * name, null is returned.
   *
   * @param suiteName  The name of the suite to be retrieved.
   *
   * @return A suite data access object that contains the suite.
   */
  public synchronized SuiteStagesWrapper getSuite( String suiteName ) {
    if ( suites.containsKey(suiteName) ) {
      logger.info( "Retrieving suite '" + suiteName + "'" );
      return suites.get( suiteName );
    } else {
      logger.info( "SuiteStages '" + suiteName + "' not found" );
      return null;
    }
  }

  /**
   * Return the contents of the named suite.  If there is not suite by that
   * name, a blank suite is created and returned.
   *
   * @param suiteName  The name of the suite to be retrieved.
   * @param resources  The resource configuration information to be applied
   * to new suites.
   *
   * @return A suite data access object that contains the suite.
   *
   * @throws ConfigurationException if trouble reading resources
   * @throws CrypterException if unable to decrypt suite
   * @throws IOException if unable to open suite
   * @throws XmlException if unable to parse suite
   */
  public synchronized SuiteStagesWrapper getSuite( String suiteName,
                                      ResourcesWrapper resources )
    throws ConfigurationException, CrypterException, IOException, XmlException {

    if ( suites.containsKey(suiteName) ) {
      logger.info( "Retrieving suite '" + suiteName + "'" );
      return suites.get( suiteName );
    } else {
      String suitePath = directory + File.separator + suiteName +
                         ".xml";
      logger.info( "Creating blank suite at '" + suitePath + "' for suite '" +
                   suiteName + "'" );
      return new SuiteStagesWrapper(suitePath, resources );
    }
  }

  /**
   * Store the named suite contained in the suiteStages object.  If
   * there is an existing suite in the table already, this suite will be
   * applied to it.  This change will be persistent.
   *
   * @param suiteStages The suiteStages object which contains the expanded
   * suite.
   *
   * @throws CrypterException if trouble encryping suite
   * @throws IOException if unable to save suite to disk
   */
  public synchronized void putSuite( SuiteStagesWrapper suiteStages )
    throws CrypterException, IOException {
    String suiteName = suiteStages.getDocument().getSuiteStages().getName();
    if ( suiteStages.getSeriesConfigCount() > 0 ) {
      logger.info( "Putting suite " + suiteName + " in suite table" );
      suites.put( suiteName, suiteStages );
      suiteStages.save();
    } else {
      logger.info( "0 configs found in " + suiteName + "; deleting" );
      suites.remove( suiteName );
      suiteStages.remove();
    }
  }

  /**
   * Delete the named suite from storage.
   *
   * @param suiteName The name of the suite to be deleted.
   */
  public synchronized void remove( String suiteName ) {
    suites.remove( suiteName );
  }

  /**
   * Return the names of the suites currently being stored.
   *
   * @return  An array of suite names currently stored in object.
   */
  public String[] getNames() {
    String[] stringArrayType = new String[suites.size()];
    return suites.keySet().toArray( stringArrayType );
  }

  /**
   * Set the directory where the suites can be stored to disk.
   *
   * @param directory A string containing the path to a writeable directory.
   */
  public void setDirectory( String directory ) {
    this.directory = directory;
  }

}
