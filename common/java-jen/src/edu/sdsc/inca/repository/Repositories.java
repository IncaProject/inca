package edu.sdsc.inca.repository;

import edu.sdsc.inca.util.StringMethods;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import org.apache.log4j.Logger;

/**
 * Provides convenience methods for dealing with a set of Inca reporter
 * repositories.  This object may keep its persistence by writing the list
 * of repositories to file.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class Repositories {

  public static final String DEFAULT_PATH = "/tmp/repositories.xml";
  public static final String DEPENDS_ATTR = "dependencies";
  public static final String FILE_ATTR = "file";
  public static final String NAME_ATTR = "name";
  public static final String VERSION_ATTR = "version";

  private Hashtable repositories = new Hashtable();
  private String filePath = null;
  static private Logger logger = Logger.getLogger(Repositories.class);

  /**
   * Creates a Repositories object using the default repositories file
   * to store its state.
   *
   * @throws IOException
   */
  public Repositories() throws IOException {
    this( DEFAULT_PATH );
  }

  /**
   * Creates a Repositories object using the provided filePath to read/save
   * the list of repositories.
   *
   * @param filePath A path to a file containing newline delimited uris
   * to Inca reporter repositories; null for none
   *
   * @throws IOException
   */
  public Repositories( String filePath ) throws IOException {
    this.filePath = filePath;
    if ( filePath != null ) {
      File file = new File( filePath );
      if ( file.exists() ) {
        try {
          logger.debug( "Reading repositories from '" + filePath + "'" );
          BufferedReader reader = new BufferedReader( new FileReader(file) );
          String line;
          while( ( line = reader.readLine() ) != null ) {
            line = line.trim();
            try {
              this.repositories.put( line, new Repository( new URL( line ) ) );
            } catch( IOException e ) {
              logger.error( "Unable to access repository " + line, e );
            }
            logger.debug( "Added repository '" + line + "'" );
          }
          reader.close();
        } catch ( IOException e ) {
          logger.error( "Unable to read existing repositories file '" +
                        filePath + "'", e );
        }
      }
    }
  }

  /**
   * Add a repository to the set.
   *
   * @param repo the repository to add
   */
  public void addRepository( Repository repo ) {
    this.repositories.put( repo.getURL().toString(), repo );
    try {
      this.saveRepositories( this.filePath );
    } catch ( IOException e ) {
      logger.error( "Unable to save repositories", e );
    }
  }

  /**
   * Returns an array of Properties, each of which specifies the attributes of
   * one reporter from the Agent's reporter repositories.
   *
   * @return a property set of every reporter in any repository
   */
  public Properties[] getCatalog() {
    int totalReporters = 0;
    for ( Enumeration e = this.repositories.elements(); e.hasMoreElements(); ) {
      Repository repo = (Repository)e.nextElement();
      totalReporters += repo.getCatalog().length;
    }
    Properties[] result = new Properties[totalReporters];
    totalReporters = 0;
    for ( Enumeration e = this.repositories.elements(); e.hasMoreElements(); ) {
      Properties[] catalog = ( (Repository)e.nextElement() ).getCatalog();
      System.arraycopy( catalog, 0, result, totalReporters, catalog.length );
      totalReporters += catalog.length;
    }
    return result;
  }

  /**
   * Retrieve the path to the repositories file.
   *
   * @return A string containing the path to the repositories file.
   */
  public String getFilePath() {
    return this.filePath;
  }

  /**
   * Return the latest version available for the named package in any of the
   * repositories.
   *
   * @param name  The name of the package to search for.
   * @return  The latest version of the named package or null if not found.
   */
  public String getLatestVersionOfPackage( String name ) {
    String result = null;
    for ( Enumeration e = this.repositories.elements(); e.hasMoreElements(); ) {
      Repository repo = (Repository)e.nextElement();
      String version = repo.getLatestVersionOfPackage(name);
      if ( version != null &&
           ( result == null ||
             StringMethods.compareTo( version, result ) > 0 ) ) {
        result = version;
      }
    }
    return result;
  }

  /**
   * Returns the list of reporter repositories stored in the object.
   *
   * @return A list of repository objects.
   */
  public Repository[] getRepositories() {
    return (Repository [])this.repositories.values().toArray
      ( new Repository[ this.repositories.size() ] );
  }

  /**
   * Return the repository that contains the specified package with the
   * specified version.  If version is null, we get the latest version in
   * all repositories.
   *
   * @param packageName The name of the package we are looking for.
   * @param version     The version of the package we are looking for.
   * @return  The repository that contains the specified package or null if
   * it does not exist in any repository.
   */
  public Repository getRepositoryForPackage( String packageName,
                                             String version ) {

    if ( version == null ) {
      version = this.getLatestVersionOfPackage( packageName );
      if ( version == null ) {
        logger.error
          ( "Package '" + packageName + "' not found in repositories" );
      }
    }
    for ( Enumeration e = this.repositories.elements(); e.hasMoreElements(); ) {
      Repository repo = (Repository)e.nextElement();
      Properties props = repo.getProperties( packageName, version );
      if ( props != null ) {
        return repo;
      }
    }
    logger.error( "Package '" + packageName + "' not found in repositories" );
    return null;

  }

  /**
   * Translate the package name into a repository url.  The first repository
   * that contains the specified package will be returned.
   *
   * @param packageName  A package name.
   * @return  A url to the specified package on a repository.
   */
  public String getRepositoryUrlForPackage(String packageName, String version ){
    Repository repository = getRepositoryForPackage( packageName, version );
    if ( repository == null ) {
      return null;
    }
    if ( version == null ) {
      version = repository.getLatestVersionOfPackage( packageName );
    }
    Properties props = repository.getProperties( packageName, version );
    if ( props != null ) {
      if ( props.getProperty(FILE_ATTR) == null ) {
        logger.error( "Corrupt package entry for package '" + packageName +
                      "', no " + FILE_ATTR + " attribute" );
        return null;
      }
      String repositoryUrl = repository.getURL().toString();
      if ( repositoryUrl.matches("^file:/\\w.*") ) {
        repositoryUrl = repositoryUrl.replaceFirst( "file:", "file://" );
      }
      return repositoryUrl + "/" + props.getProperty(FILE_ATTR);
    }
    logger.error( "found repo for " + packageName + " unable to retrieve url" );
    return null;
  }

  /**
   * Checks for a package update by iterating through all repositories
   * and returns true if it finds a version of the package more recent
   * than the specified one.
   *
   * @return True if the package was updated and false if it was not.
   */
  public boolean hasPackageUpdated( String name, String version ) {
    String latestVersion = this.getLatestVersionOfPackage( name );
    return latestVersion != null &&
           StringMethods.compareTo( latestVersion, version ) > 0;
  }

  /**
   * Refresh the current catalog contents of all repositories.
   *
   * @throws IOException
   */
  public void refresh() throws IOException {
    logger.info( "Refreshing repository catalogs" );
    for ( Enumeration e = this.repositories.elements(); e.hasMoreElements(); ) {
      Repository repo = (Repository)e.nextElement();
      logger.info( "Refreshing " + repo.getURL() );
      repo.refresh();
    }
  }

  /**
   * Removes the repository with a particular URL from the set.
   *
   * @param baseURL the URL of the repository to remove
   */
  public void removeRepository( String baseURL ) {
    this.repositories.remove( baseURL );
    try {
      this.saveRepositories( this.filePath );
    } catch ( IOException e ) {
      logger.error( "Error saving repositories", e );
    }
  }

  /**
   * Set the path to the repositories file.
   *
   * @param filePath A string containing the path to where the repositories
   * file can be read/stored.
   * @throws IOException on write error
   */
  public void setFilePath( String filePath ) throws IOException {
    this.saveRepositories( filePath );
    if ( this.filePath != null ) {
      new File( this.filePath ).delete();
    }
    this.filePath = filePath;
  }

  /**
   * Set the repositories the agent should download reporters from upon
   * receiving a suite (if it hasn't downloaded them before).
   *
   * @param repos  A list of repositories.
   */
  public void setRepositories(Repository[] repos) throws IOException {
    this.repositories = new Hashtable();
    for ( int i = 0; i < repos.length; i++ ) {
      this.repositories.put( repos[i].getURL().toString(), repos[i] );
      repos[i].refresh();
    }
    saveRepositories( this.filePath );
  }

  /**
   * Writes the set of repository URLs to a specified file.
   *
   * @param filePath the file to write
   * @throws IOException on error
   */
  protected void saveRepositories( String filePath ) throws IOException {
    if(filePath == null) {
      return;
    }
    logger.debug( "Saving repositories to " + filePath );
    File saveFile = new File( filePath );
    saveFile.getParentFile().mkdirs();
    FileWriter writer = new FileWriter( saveFile );
    for ( Enumeration e = this.repositories.keys(); e.hasMoreElements(); ) {
      String url = (String)e.nextElement();
      logger.debug( "Write " + url );
      writer.write( url + "\n" );
    }
    writer.close();
  }

}
