package edu.sdsc.inca.agent;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import edu.sdsc.inca.Agent;
import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.dataModel.catalog.CatalogDocument;
import edu.sdsc.inca.dataModel.catalog.DependenciesType;
import edu.sdsc.inca.dataModel.catalog.PackageType;
import edu.sdsc.inca.dataModel.util.SeriesConfig;
import edu.sdsc.inca.repository.Repositories;
import edu.sdsc.inca.repository.Repository;
import edu.sdsc.inca.util.Constants;
import edu.sdsc.inca.util.SuiteWrapper;


/**
 * The Agent will cache packages (reporter or library) locally when
 * a suite uses them and send them to the reporter managers.  This class
 * tracks the packages cached on disk and will periodically look for updates
 * and download them.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class RepositoryCache extends Thread {
  // Constants
/*  public static final String[] REPOSITORY_ATTRS = new String[] {
    VERSION_ATTR, DEPENDS_ATTR
  }; */
  private static final String REPOSITORY_CATALOG = "repository.xml";

  // Member variables
  private Logger logger = Logger.getLogger(this.getClass().toString());
  private String cacheLocation = null;
  private int updatePeriod = 4 * Constants.MILLIS_TO_HOUR;
  private File catalogFile = null;
  private CatalogDocument catalog = null;
  private Repositories repositories = null;

  /**
   * Create a new RepositoryCache object using cacheLocation as the directory
   * to cache packages. Will be able to read in any existing cached
   * packages.  By default, the repository contents will be checked for updates
   * every 4 hours.  Use RepositoryCache(cacheLocation,updatePeriod) or
   * setUpdatePeriod to change frequency.
   *
   * @param cacheLocation A directory where cached packages will be stored.
   *
   * @param repositories The available repositories where packages can be
   * downloaded from.
   */
  public RepositoryCache( String cacheLocation, Repositories repositories ) {
    this.cacheLocation = cacheLocation;
    this.repositories = repositories;
    File dir = new File( cacheLocation );
    dir.mkdirs();
    catalogFile = new File(
      cacheLocation + File.separator + REPOSITORY_CATALOG
    );
    catalog = CatalogDocument.Factory.newInstance();
    catalog.addNewCatalog();
    if ( catalogFile.exists() ) {
      try {
        logger.info( "Reading repository cache catalog " + catalogFile );
        catalog = CatalogDocument.Factory.parse( catalogFile, (new XmlOptions()).setLoadStripWhitespace() );
      } catch ( Exception e ) {
        logger.error( "Unable to read " + catalogFile.getAbsolutePath(), e );
        logger.info( "Creating new repository cache catalog" );
      }
    }
  }

  /**
   * Create a new RepositoryCache object using cacheLocation as the directory
   * to cache packages. Will be able to read in any existing cached
   * packages.  The repository contents will be checked for updates
   * every updatePeriod milliseconds.
   *
   * @param cacheLocation A directory where cached packages will be stored.
   * @param updatePeriod  Number of milliseconds to sleep before checking the
   * repositories for updates.
   */
  public RepositoryCache( String cacheLocation, int updatePeriod ) {
    super(cacheLocation);
    this.updatePeriod = updatePeriod;
  }

  /**
   * Checks for a package update using the following techniques:
   *
   * 1) If package's latestVersion field is true, checks for package update.
   * Otherwise, returns false meaning no package update (since we have the
   * version we need).
   *
   * 2) Checks the repository it was fetched from and fetches the package
   * information to see if the available version is newer than the cached
   * version. If so, it will fetch the new version of the package.
   *
   * 3) If the repository no longer has the package or the repository is
   * no longer registered with the Agent, then we check all available
   * repositories to see if the package exists there.  If we find a repository
   * that has the package we fetch it if its version is newer than our
   * cached version.
   *
   * 4) If the package is not available from any repositories, then we keep
   * the cached version and return false.
   *
   * @param aPackage   A package that will be checked for an update.
   *
   * @return True if the package was updated and false if it was not.
   */
  public boolean checkForPackageUpdate( PackageType aPackage ) {
    String pkgName = aPackage.getName();
    String pkgVersion = aPackage.getVersion();
    if ( aPackage.isSetLatestVersion() && aPackage.getLatestVersion() ) {
      logger.debug(
        "Checking for package update for " + pkgName + "; current version = " +
        pkgVersion
      );
      if ( this.repositories.hasPackageUpdated(pkgName, pkgVersion) ){
        String newVer = this.repositories.getLatestVersionOfPackage(pkgName);
        logger.info(
          "Fetching update version=" + newVer + " for package '" + pkgName + "'"
        );
        aPackage.setLatestVersion( false );
        this.saveCatalog();
        fetchPackage( pkgName, null );
        return true;
      } else {
        logger.debug( "Package "  + pkgName + " up to date" );
        return false;
      }
    } else {
      logger.debug(
        "Skipping package update for " + pkgName + ", version=" + pkgVersion
      );
      return false;
    }
  }

  /**
   * Check all packages contained in the cache and send any package updates
   * to the reporter managers.
   *
   * @return An array of strings containing package names that need to be
   * updated
   */
  public String[] checkForPackageUpdates() {
    logger.debug( "Checking for package updates" );

    Vector<String> updated = new Vector<String>();
    try {
      this.repositories.refresh();
    } catch ( IOException e ) {
      logger.error( "Unable to refresh agent repository catalogs", e );
      return new String[0];
    }
    for ( PackageType p : catalog.getCatalog().getPackageArray() ) {
      logger.debug( "Checking for update on package '" + p.getName() + "'" );
      if ( checkForPackageUpdate( p ) ) updated.add( p.getName() );
    }
    logger.debug( "Finished checking for package updates" );
    return updated.toArray( new String[updated.size()] );
  }

  /**
   * Retrieve the location of the repository cache.
   *
   * @return A string containing the path to the repository cache.
   */
  public String getCacheLocation() {
    return cacheLocation;
  }

  /**
   * Check the local repository cache for the specified package and version.
   *
   * @param name  Name of the package to look for.
   *
   * @param version Version of the package to look for or null if latest
   * version is desired.
   *
   * @return  True if reporter package exists in repository cache and false
   * otherwise.
   */
  public boolean existsLocally( String name, String version ) {
    return this.getPackage(name, version) != null;
  }

  /**
   * Fetch the specified package from the available repositories and
   * store locally on disk.  This function is synchronized so that multiple
   * processes can use this class without stepping on each other.
   *
   * @param name The name of the repository package to fetch.
   *
   * @param version The version of the package to fetch or null for latest
   *
   * @return True if the repository was successfully fetched and false
   * otherwise.
   */
  public synchronized boolean fetchPackage( String name, String version )  {

    boolean latestVersion = version == null;
    Repository repository = this.repositories.getRepositoryForPackage(
      name, version
    );
    if ( repository == null ) {
      return false;
    }
    if ( version == null ) {
      version = repository.getLatestVersionOfPackage( name );
    }
    Properties props = repository.getProperties(  name, version );
    try {
      logger.info(
        "Fetching package '" + name + "' from repository '" +
        repository.getURL() + "'"
      );
      byte[] fileText = repository.getReporter(
        props.getProperty( Repositories.FILE_ATTR )
      );
      savePackageToDisk( fileText, name, version );
    } catch ( IOException e ) {
      logger.error( "Unable to update package '"+ name +"'", e );
      return false;
    }
    PackageType pkg = updateCacheCatalog(
      name, repository.getURL().toString(), props, latestVersion
    );
    String[] dependencies = pkg.getDependencies().getDependencyArray();
    if ( dependencies.length < 1 ) {
      logger.debug( "Package '" + name + "' has no dependencies" );
      return true;
    }
    boolean all_fetched = true;
    // no way to specify version for dependencies currently so we assume
    // most current
    for ( String d : dependencies ) {
      if ( ! existsLocally(d, null) ) {
        if ( ! fetchPackage(d, null) ) {
          all_fetched = false;
        }
      } else {
        logger.debug( "Dependency '" + d + "' exists" );
      }
    }
    return all_fetched;
  }

  /**
   * Return the repository cache catalog.
   *
   * @return  the repository cache catalog (XMLbeans document)
   */
  public CatalogDocument getCatalog() {
    return catalog;
  }

  /**
   * Return the package dependencies for this particular package.
   *
   * @param uri  The uri of the package to return the dependencies on.
   *    *
   * @return The dependencies on this particular package.
   */
  public String[] getDependencies( String uri ) {
    PackageType pkg = getPackageByUri( uri );
    if ( pkg != null ) {
      return pkg.getDependencies().getDependencyArray();
    }
    return new String[]{};
  }

  /**
   * Return the filename to use on the reporter manager when installation a
   * parcticular package name and version.
   *
   * @param name   Name of the package to retrieve the install path for.
   *
   * @param version Version of the package to retrieve the install path for.
   *
   * @return  The filename to use when storing the package on the remote
   * reporter manager (in the installpath).
   */
  public String getFilename( String name, String version ) {
    PackageType pkg = getPackage( name, version );
    if ( pkg != null ) {
      return pkg.getFilename();
    }
    return null;
  }

  /**
   * Return the installation path to use on the reporter manager when installing
   * for a particular package name and version.
   *
   * @param name   Name of the package to retrieve the install path for.
   * @param version Version of the package to retrieve the install path for.
   *
   * @return The relative install path to use on the reporter manager when
   * installing this package.
   */
  public String getInstallPath( String name, String version ) {
   PackageType pkg = getPackage( name, version );
    if ( pkg != null ) {
      return pkg.getInstallPath();
    }
    return null;
  }

  public PackageType getPackage( String name, String version ) {
    String conditions = "name='" + name + "'";
    if ( version != null ) {
      conditions += " and version='" + version + "'";
    }
    XmlObject[] result = catalog.selectPath(
      "//package[" + conditions + "]"
    );
    if ( result.length < 1 ) {
      return null;
    }
    if ( result.length > 1 ) {
      if ( version == null ) {
        for ( XmlObject r : result ) {
          if ( ((PackageType)r).getLatestVersion() ) {
            return (PackageType)r;
          }
        }
      }
      logger.error(
        "Located more than one match in repository cache for package" +
        name + ", version=" + version + "; choosing first one"
      );
    }
    return (PackageType)result[0];
  }

  public PackageType getPackageByUri( String uri ) {
    XmlObject[] result = catalog.selectPath(
      "//package[uri='" + uri + "']"
    );
    if ( result.length < 1 ) {
      return null;
    }
    if ( result.length > 1 ) {
      logger.error(
        "Located more than one match in repository cache for package" +
        uri + "; choosing first one"
      );
    }
    logger.debug( result[0].toString() );
    return (PackageType)result[0];
  }

  /**
   * Return the specified package.  If the package is already cached on the
   * machine, it will be retrieved from the cache.  Otherwise, it will be
   * searched for in the available repositories and if it exists, will be
   * returned.
   *
   * @param name  The name of the package to retrieve.
   *
   * @param version The version of the package to fetch or null to get the
   * latest.
   *
   * @return  The contents of the specified package.
   *
   * @throws IOException If there is an error retrieving the package.
   */
  public byte[] getPackageContent( String name, String version )
    throws IOException {

    if ( ! existsLocally(name, version) ) {
      fetchPackage( name, version );
    }
    try {
      for ( PackageType p : catalog.getCatalog().getPackageArray() ) {
        if ( p.getName().equals(name) && version == null ) {
          checkForPackageUpdate( p );
        }
      }
      return readPackageFromDisk( name, version );
    } catch ( Exception e ) {
      logger.error( "Problem reading local reporter '" + name + "'", e );
      return null;
    }
  }

  public String getPackageNameByUri( String uri ) {
    if ( Pattern.matches( "^file:/\\w.*$", uri) ) {
      uri = uri.replaceFirst( "file:/", "file:///" );
    }
    XmlObject[] pkgs = catalog.selectPath( "//package[uri='" + uri + "']");
    if ( pkgs.length == 1 ) {
      return ((PackageType)pkgs[0]).getName();
    }
    logger.error( "Package '" + uri + "' not in repository cache" );
    return null;
  }

  /**
   * Look up a specify package and version in the repository cache and return
   * the permissions needed for installation.
   *
   * @param name   Name of the package to look up permissions on.
   * @param version Version of the package to look up permissions on.
   *
   * @return  Permissions for reporter manager to use on package when installing
   */
  public String getPermissions( String name, String version ) {
    PackageType pkg = getPackage( name, version );
    if ( pkg != null && pkg.isSetPermissions() ) {
      return pkg.getPermissions();
    }
    return null;
  }

  /**
   * Return the repositories that the repository cache references when
   * looking for reporter packages.
   *
   * @return The repositories object used in the repository cache.
   */
  public Repositories getRepositories() {
    return repositories;
  }

  /**
   * Return the current update period (i.e., amount of time used to sleep
   * in between updates).
   *
   * @return The update period in milliseconds.
   */
  public int getUpdatePeriod() {
    return updatePeriod;
  }

  public String getUri( String name, String version ) {
    PackageType pkg = getPackage( name, version );
    if ( pkg != null ) {
      return pkg.getUri();
    }
    return null;
  }

  /**
   * Return the contents of the specified package from our local cache.
   *
   * @param name  Name of the package to retrieve.
   *
   * @param version Version of the package to retrieve
   *
   * @return The  contents of the specified package.
   *
   * @throws IOException If there is an error reading the package from disk.
   */
  public byte[] readPackageFromDisk( String name, String version )
    throws IOException {

    if ( version == null ) {
      version = this.getPackage( name, null).getVersion();
    }
    FileInputStream is = new FileInputStream(
      getCachedPackageFileHandle(name, version)
    );
    byte[] result = new byte[0];
    byte[] block = new byte[1024];
    int readCount;
    while((readCount = is.read(block)) > 0) {
      byte[] expanded = new byte[result.length + readCount];
      System.arraycopy(result, 0, expanded, 0, result.length);
      System.arraycopy(block, 0, expanded, result.length, readCount);
      result = expanded;
    }
    is.close();
    return result;
  }

  /**
   * Remove the specified package from the repository cache.
   *
   * @param name  Name of the package to remove
   * @return  True upon successful deletion and false if error.
   */
  public synchronized boolean removePackage( String name ) {
    PackageType[] pkgs =
      catalog.getCatalog().getPackageArray();
    for( int i = 0; i < pkgs.length; i++ ) {
      if ( pkgs[i].getName().equals(name)) {
        logger.info( "Removing package '" + name + "' from repository" );
        catalog.getCatalog().removePackage( i );
        return saveCatalog();
      }
    }
    return false;
  }

  /**
   * Starts a thread to check for package updates from the available
   * repositories.
   */
  @Override
  public void run() {
    try {
      logger.info(
        "Repository cache will check for package updates every " +
        updatePeriod/Constants.MILLIS_TO_SECOND + " seconds"
      );
      while (!isInterrupted()) {
        Agent.getGlobalAgent().notifyAdminsofChanges
          ( Agent.getGlobalAgent().updateCachedPackages() );
        Thread.sleep( updatePeriod );
      }
    } catch (InterruptedException e) {
      logger.debug( "RepositoryCache interrupted while waiting on queue");
    } finally {
      logger.info( "RepositoryCache stopping updates");
    }
  }

  /**
   * Resolve all reporter names in the provided suite document to uris using
   * information provided in the repository cache.
   *
   * @param suite   A suite containing reporter names and possibly versions
   *
   * @throws edu.sdsc.inca.ConfigurationException if unable to locate reporter
   * in cache
   */
  public void resolveReporters( SuiteWrapper suite )
    throws ConfigurationException {

    for ( SeriesConfig s : suite.getSeriesConfigs() ) {
      String version = s.getSeries().getVersion();
      String name = s.getSeries().getName();
      PackageType pkg = this.getPackage( name, version );
      if ( pkg == null ) {
        throw new ConfigurationException(
          "Unable to locate reporter '" + name + ", version=" + version +
          "' in local repository cache"
        );
      }
      s.getSeries().setUri( pkg.getUri() );
      if ( ! s.getSeries().isSetVersion() ) {
        s.getSeries().setVersion( "?=" + pkg.getVersion() );
      }
    }
  }

  /**
   * Save the specified package to disk (i.e., cache it).
   *
   * @param fileContent  The package content as a text string.
   *
   * @param name         The name of the package
   *
   * @param version      The version of the package to save
   *
   * @throws IOException  If there is an error saving the package to disk.
   */
  public void savePackageToDisk(byte[] fileContent, String name, String version)
    throws IOException {

    File file = getCachedPackageFileHandle( name, version );
    logger.debug( "Writing package " + name + " to " + file.getAbsolutePath() );
    FileOutputStream out = new FileOutputStream( file );
    out.write( fileContent );
    out.close();
  }

  /**
   * Set the location of the where the agent will cache repository packages.
   *
   * @param cacheLocation  A string containing the path to a directory where
   * the repository cache can store files.
   */
  public void setCacheLocation( String cacheLocation ) {
    this.cacheLocation = cacheLocation;
  }

  /**
   * Set the repositories the repository cache will reference when looking
   * for packages.
   *
   * @param repositories  A set of repositories for which reporter packages
   * can be downloaded from.
   */
  public void setRepositories( Repositories repositories ) {
    this.repositories = repositories;
  }

  /**
   * Set the current update period (i.e., amount of time used to sleep
   * in between updates).
   *
   * @param updatePeriod  The update period in milliseconds.
   */
  public void setUpdatePeriod( int updatePeriod ) {
    this.updatePeriod = updatePeriod;
  }

  // Protected

  /**
   * Determine whether package file is a python module
   *
   * @param filename the name of the file in the repository
   *
   * @return  True if appears to be a python module and false otherwise
   */
  protected static boolean isPythonModule( String filename ) {
    return (filename.startsWith("lib/") || filename.contains("/lib/") ) &&
                filename.endsWith(".py");
  }


  /**
   * Return a File object for a package that will be cached locally.
   *
   * @param name  Name of the package to be read/stored.
   *
   * @param version Version of the package to read/stored
   *
   * @return A File object that can be used to read or store a package on disk.
   */
  private File getCachedPackageFileHandle( String name, String version ) {
    return new File(
     cacheLocation + File.separator + name + "-" + version
    );
  }

  /**
   * Save the repository cache catalog which contains information about the
   * packages that are being cached locally on this machine to disk.
   *
   * @return True if the catalog was successfully saved and false otherwise.
   */
  private boolean saveCatalog() {
    try {
      XmlOptions xmloptions = new XmlOptions();
      xmloptions.setSavePrettyPrint();
      catalog.save( catalogFile, xmloptions );
      return true;
    } catch ( IOException e ) {
      logger.error( "Unable to store reporter cache catalog", e );
      return false;
    }


  }

  /**
   * Update the local catalog of information for packages that are being
   * cached on the machine to include the specified package.
   *
   * @param name   Name of the package that is being added to the catalog.
   *
   * @param repoUrl  Url of the repository that the package came from.
   *
   * @param properties  The properties for the package.
   *
   * @param latestVersion  True if want to retrieve latest version of package
   *
   * @return  True if the update was successful and false otherwise.
   */
  private PackageType updateCacheCatalog(
    String name, String repoUrl, Properties properties, boolean latestVersion ){

    if ( repoUrl.matches("^file:/\\w.*") ) {
      repoUrl = repoUrl.replaceFirst( "file:", "file://" );
    }
    PackageType repPackage = catalog.getCatalog().addNewPackage();
    repPackage.setName( name );
    repPackage.setRepository( repoUrl );
    String filename = properties.getProperty(Repositories.FILE_ATTR );
    repPackage.setUri(repoUrl + "/" + filename );
    String version = properties.getProperty( Repositories.VERSION_ATTR );
    repPackage.setVersion( version );
    logger.debug( "Setting latestVersion to " + latestVersion );
    repPackage.setLatestVersion( latestVersion );
    if ( filename.endsWith(".pm") ) {
      // it is a perl Inca reporter library
      File repositoryFile = new File( name.replaceAll( "::", "/") + ".pm" );
      repPackage.setFilename( repositoryFile.getName() );
      repPackage.setInstallPath( "lib/perl/" + repositoryFile.getParent() );
      repPackage.setPermissions( "644" );
    } else if ( isPythonModule(filename) ) {
      // it is a python Inca reporter library
      File repositoryFile = new File( name.replaceAll( "\\.", "/") + ".py" );
      repPackage.setFilename( repositoryFile.getName() );
      repPackage.setInstallPath( "lib/python/" + repositoryFile.getParent() );
      repPackage.setPermissions( "644" );
    } else if ( filename.endsWith(".gz") ) {
      File repositoryFile = new File(filename);
      repPackage.setFilename( repositoryFile.getName() );
      repPackage.setInstallPath( "." );
      repPackage.setPermissions( "644" );
    } else if (
      repPackage.getName().equals(ReporterManagerController.BATCH_REPORTER) ) {
      // special hack for batch wrapper
      File repositoryFile = new File(filename);
      repPackage.setFilename( repositoryFile.getName() );
      repPackage.setInstallPath( "bin" );
      repPackage.setPermissions( "755" );
    } else { // it is a reporter
      File repositoryFile = new File(filename);
      repPackage.setFilename( repositoryFile.getName() + "-" + version );
      repPackage.setInstallPath( "bin" );
      repPackage.setPermissions( "755" );
    }
    String dependencies = properties.getProperty( Repositories.DEPENDS_ATTR );
    DependenciesType repDependencies = repPackage.addNewDependencies();
    if ( dependencies != null ) {
      for ( String d : Repository.getPropertyValues(dependencies) ) {
        if ( ! d.equals("") ) repDependencies.addDependency( d.trim() );
      }
    }
    if ( saveCatalog() ) {
      return repPackage;
    } else {
      return null;
    }
  }

}
