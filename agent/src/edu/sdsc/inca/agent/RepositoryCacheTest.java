package edu.sdsc.inca.agent;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import edu.sdsc.inca.AgentClientTest;
import edu.sdsc.inca.Agent;
import edu.sdsc.inca.util.StringMethods;

import edu.sdsc.inca.dataModel.catalog.PackageType;
import edu.sdsc.inca.repository.Repository;
import edu.sdsc.inca.repository.Repositories;
import edu.sdsc.inca.repository.RepositoriesTest;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class RepositoryCacheTest extends TestCase {
  final public static String repositoryLocation = "/tmp/repositorycache";
  final public static String cacheLocation = "var/reporter";
  private static Logger logger = Logger.getLogger(RepositoryCacheTest.class);

  /**
   * Create a local repository cache using selected reporters from the
   * supplied repository
   *
   * @param repositories  A Inca package repository
   *
   * @return  A repository cache object to a local repository cache
   *
   * @throws IOException if unable to create repository cache
   */
  public static RepositoryCache createSampleRepositoryCache(
    Repositories repositories )
    throws IOException {

    File repositoryDir = File.createTempFile( "repository-cache", "" );
    repositoryDir.delete();
    repositoryDir.mkdirs();
    repositoryDir.deleteOnExit();
    logger.info(
      "Creating sample repository cache at " + repositoryDir.getAbsolutePath()
    );

    RepositoryCache cache = new RepositoryCache(
      repositoryDir.getAbsolutePath(), repositories
    );
    String[] reporters = {
      "cluster.security.openssl.version",
      "cluster.compiler.gcc.version",
      "user.search.output.unit",
      "grid.interactive_access.myproxy.unit",
      "grid.middleware.globus.unit.gatekeeper",
      "data.transfer.tgcp.unit",
      "cluster.devel.gmake.version"
    };
    for( int i = 0; i < reporters.length; i++ ) {
      cache.getPackageContent( reporters[i], null );
      PackageType pkg = cache.getPackage( reporters[i], null );
      File reporter = new File(
        repositoryDir + File.separator + reporters[i] + "-" + pkg.getVersion()
      );
      if ( ! reporter.exists() ) {
        throw new IOException( reporters[i] + " not found" );
      }
    }
    File[] files = repositoryDir.listFiles();
    for ( int i = 0; i < files.length; i++ ) {
      files[i].deleteOnExit();
    }
    return cache;

  }

  /**
   * Setup a repositories object to a local repository
   *
   * @return A repositories object
   *
   * @throws Exception if trouble setting up repository
   */
  public static Repositories setupRepository( )
    throws Exception {

    // set up repository
    AgentClientTest.makeRepository( repositoryLocation, 0, 6 );
    File repositoryDir = new File( repositoryLocation );
    String url = "file://" + repositoryDir.getCanonicalPath();
    Repository repo = new Repository( new URL(url) );
    Repositories repositories = new Repositories();
    repositories.setRepositories( new Repository[]{ repo } );
    return repositories;
  }

  /**
   * Test ability to fetch packages from a repository
   *
   * @throws Exception if trouble running test
   */
  public void testFetch() throws Exception {
    Repositories repositories = setupRepository();
    StringMethods.deleteDirectory( new File(cacheLocation) );
    RepositoryCache cache = new RepositoryCache( cacheLocation, repositories );
    assertFalse( "gpfs not in cache", cache.existsLocally("gpfs", "1.2") );
    assertTrue( "Fetched gpfs from cache", cache.fetchPackage("gpfs", "1.2") );
    RepositoryCache cache2 = new RepositoryCache( cacheLocation, repositories );
    assertTrue( "cache2 has gpfs reporter", cache2.existsLocally("gpfs", "1.2") );
    assertTrue( "Fetched gcc from cache", cache.fetchPackage("gcc", "1.5") );
    assertTrue( "Cache has gpfs reporter", cache.existsLocally("gpfs", "1.2"));
    PackageType pkg = cache.getPackage( "gpfs", "1.2");
    assertNotNull( "getPackage()", pkg );
    assertEquals( "getFilename()", "cluster.filesystem.gpfs.version.rpt-1.2",
                  pkg.getFilename() );
    assertEquals( "getVersion()", "1.2", pkg.getVersion() );
    assertEquals( "getInstallPath()", "bin", pkg.getInstallPath() );
    assertEquals( "getPermissions()", "755", pkg.getPermissions() );
    assertTrue( "Cache has gcc reporter", cache.existsLocally("gcc", "1.5"));
    assertTrue("Cache has Inca::Reporter",
               cache.existsLocally("Test::Inca::Reporter", "1.0"));
    assertTrue( "Removed gpfs from cache", cache.removePackage("gpfs") );
    assertFalse( "gpfs not in cache", cache.existsLocally("gpfs", "1.2") );
    assertTrue( "Removed gcc from cache", cache.removePackage("gcc") );
    assertTrue( "Removed Inca::Reporter from cache",
                cache.removePackage("Test::Inca::Reporter") );
  }

  /**
   * Test ability to get package info and content once fetched
   *
   * @throws Exception if trouble running test
   */
  public void testGetPackage() throws Exception {
    StringMethods.deleteDirectory( new File(cacheLocation) );
    Repositories repositories = setupRepository();
    RepositoryCache cache = new RepositoryCache( cacheLocation, repositories );
    assertFalse( "gpfs not in cache", cache.existsLocally("gpfs", "1.2") );
    assertNotNull(
      "Fetched gpfs from cache",
      cache.getPackageContent("gpfs", "1.2")
    );
    assertTrue( "gpfs in cache", cache.existsLocally("gpfs", "1.2") );
    assertTrue( "gpfs in cache", cache.existsLocally("gpfs", null) );
    // no repositories so the get will fail if it doesn't get it from cache
    Repositories emptyRepository = new Repositories();
    cache.setRepositories( emptyRepository );
    assertNotNull(
      "Fetched gpfs from cache",
      cache.getPackageContent("gpfs", "1.2")
    );
    assertTrue( "Removed gpfs from cache", cache.removePackage("gpfs") );
  }

  /**
   * Test ability to convert package name into file
   *
   * @throws Exception if trouble running test
   */
  public void testParsePackage() throws Exception {
    File repositoryFile = new File(
      "Inca::Reporter".replaceAll( "::", "/") + ".pm"
    );
    assertEquals( "parse package file name", "Reporter.pm",
                  repositoryFile.getName() );
    assertEquals( "parse package install path", "Inca",
                  repositoryFile.getParent() );
    repositoryFile = new File(
      "Inca".replaceAll( "::", "/") + ".pm"
    );
    assertEquals( "parse package file name", "Inca.pm",
                  repositoryFile.getName() );
    assertNull( "parse package install path",
                  repositoryFile.getParent() );
    repositoryFile = new File(
      "Inca::Reporter::Version".replaceAll( "::", "/") + ".pm"
    );
    assertEquals( "parse package file name", "Version.pm",
                  repositoryFile.getName() );
    assertEquals( "parse package install path", "Inca/Reporter",
                  repositoryFile.getParent() );
  }

  /**
   * Test python module detection method
   *
   * @throws Exception  if trouble running test
   */
  public void testPython() throws Exception {
    assertTrue
      ( "python module .py", RepositoryCache.isPythonModule("lib/Module.py") );
    assertTrue( "python module .py",
                RepositoryCache.isPythonModule("python/lib/Module.py") );
    assertFalse( "python reporter .py",
                 RepositoryCache.isPythonModule("pythonreporter.py") );
    assertFalse( "python reporter .py",
                 RepositoryCache.isPythonModule("mylib/reporter.py") ); 
  }

  /**
   * Test ability to create a repository cache from a repository
   * @throws Exception
   */
  public void testRepo() throws Exception {
    logger.debug( "Creating repository" );
    Repositories repos = RepositoriesTest.createSampleRepository(null);
    createSampleRepositoryCache( repos );
  }

  /**
   * Test ability to update packages in repository cache
   *
   * @throws Exception if trouble running test
   */
  public void testUpdate() throws Exception {
    StringMethods.deleteDirectory( new File(cacheLocation) );
    Repositories repositories = setupRepository();
    RepositoryCache cache = new RepositoryCache( cacheLocation, repositories );
    Agent.getGlobalAgent().setRepositoryCache( cache );
    Agent.getGlobalAgent().setSuites( "var/suites" );
    assertFalse( "gpfs not in cache", cache.existsLocally("gpfs", null) );
    assertNotNull(
      "Fetched gpfs from cache",
      cache.getPackageContent("gpfs",null)
    );

    PackageType pkg = cache.getPackage( "gpfs", "1.2" );
    increaseVersion( cache, "gpfs", "1.3" );
    assertTrue( "package updated", cache.checkForPackageUpdate( pkg ) );
    String gpfsText = new String( cache.readPackageFromDisk("gpfs", "1.2") );
    assertTrue( "gpfs text fetched", gpfsText.matches( "(?m)(?s).*use.*") );
    pkg = cache.getPackage( "gpfs", null );
    assertEquals( "current gpfs version", "1.3", pkg.getVersion()  );    

    increaseVersion( cache, "gpfs", "1.4");
    cache.setUpdatePeriod( 2000 );
    cache.start();
    Thread.sleep(5000);
    pkg = cache.getPackage( "gpfs", null );
    assertEquals( "current gpfs version", "1.4", pkg.getVersion()  );
    assertTrue( "Removed gpfs from cache", cache.removePackage("gpfs") );
    cache.interrupt();
    cache.join();
  }

  // Private functions

  /**
   * Increase the version number of a package
   *
   * @param cache  repository cache to update the package
   *
   * @param name  The name of the package to update
   *
   * @param newVersion  The new version of the package to add
   *
   * @throws IOException if trouble writing to catalog
   */
  private void increaseVersion( RepositoryCache cache, String name,
                                String newVersion )
    throws IOException {

    AgentClientTest.updateVersionInCatalog
      ( cache.getRepositories(), name, newVersion );
    cache.getRepositories().refresh();
  }

}
