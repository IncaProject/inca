package edu.sdsc.inca.repository;

import edu.sdsc.inca.util.StringMethods;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

/**
 * Some testing of Repositories.  More testing resides in AgentClientTest and
 * some of those should be moved over eventually.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class RepositoriesTest extends TestCase {
  private static Logger logger = Logger.getLogger(RepositoriesTest.class);

  final static private String INCA_SVN_URL =
    "http://capac.sdsc.edu:9080/localdisk/inca/subversion/inca/trunk/devel";
  final static private String[] REPORTER_PACKAGES = {
    INCA_SVN_URL + "/reporters/lib/perl/Inca/Reporter.pm",
    INCA_SVN_URL + "/reporters/lib/perl/Inca/Reporter/Version.pm" ,
    INCA_SVN_URL + "/reporters/lib/perl/Inca/Reporter/SimpleUnit.pm",
    INCA_SVN_URL + "/reporters/lib/perl/Inca/Reporter/GridProxy.pm",
    INCA_SVN_URL + "/reporters/bin/cluster.compiler.gcc.version",
    INCA_SVN_URL + "/reporters/bin/cluster.security.openssl.version",
    INCA_SVN_URL + "/reporters/bin/data.transfer.tgcp.unit",
    INCA_SVN_URL + "/reporters/bin/grid.interactive_access.myproxy.unit",
    INCA_SVN_URL + "/reporters/bin/grid.middleware.globus.unit.gatekeeper",
    INCA_SVN_URL + "/reporters/bin/user.search.output.unit",
    INCA_SVN_URL + "/reporters/bin/grid.middleware.globus.unit.proxy",
    INCA_SVN_URL + "/reporters/bin/cluster.devel.gmake.version",    
    INCA_SVN_URL + "/manager/t/makedist.tar.gz"
  };



  /**
   * Create a small repository which can be used in testing.  Provides
   * the basic libraries, Reporter.pm, Version.pm, and SimpleUnit, along with
   * 5 reporters:  gcc version reporter, openssl version reporter, tgcp
   * unit reporter, myproxy unit reporter, and globus 2 gatekeeper unit test.
   * It will download the packages directly from the Inca SVN repository.
   * Note this directory will be cleaned up upon program exit.
   *
   * @param dirPath  Specify a directory for the repository.  If null a
   * temporary directory will be used.
   *
   * @return  The url for the local repository.
   *
   * @throws Exception  if unable to create repository
   */
  public static Repositories createSampleRepository( String dirPath )
    throws Exception {

    String packages =
      "dependencies: \n" +
      "description: Inca reporter API base class\n" +
      "file: Reporter.pm\n" +
      "name: Inca::Reporter\n" +
      "url: http://inca.sdsc.edu\n" +
      "version: 1.0\n" +
      "\n" +
      "dependencies: \n" +
      "description: pseudo-module for reporters that require proxy creds\n" +
      "file: GridProxy.pm\n" +
      "name: Inca::Reporter::GridProxy\n" +
      "url: http://inca.sdsc.edu\n" +
      "version: 1.0\n" +
      "\n" +
      "dependencies:\n Inca::Reporter\n" +
      "description: Inca reporter API class for unit test reporters\n" +
      "file: SimpleUnit.pm\n" +
      "name: Inca::Reporter::SimpleUnit\n" +
      "url: http://inca.sdsc.edu\n" +
      "version: 1.0\n" +
      "\n" +
      "dependencies:\n Inca::Reporter\n" +
      "description: Inca reporter API class for version reporters\n" +
      "file: Version.pm\n" +
      "name: Inca::Reporter::Version\n" +
      "url: http://inca.sdsc.edu\n" +
      "version: 1.0\n" +
      "\n" +
      "arguments: help no|yes no;log [01234]|debug|error|info|system|warn ;verbose [012] 0;version no|yes no\n" +
      "dependencies:\n Inca::Reporter\n Inca::Reporter::Version\n" +
      "description: Reports the version of gcc\n" +
      "file: cluster.compiler.gcc.version\n" +
      "name: cluster.compiler.gcc.version\n" +
      "url: http://gcc.gnu.org\n" +
      "version: 1.5\n" +
      "\n" +
      "arguments: help no|yes no;log [01234]|debug|error|info|system|warn ;verbose [012] 0;version no|yes no\n" +
      "dependencies:\n Inca::Reporter\n Inca::Reporter::Version\n" +
      "description: Reports the version of openssl\n" +
      "file: cluster.security.openssl.version\n" +
      "name: cluster.security.openssl.version\n" +
      "url: http://www.openssl.org\n" +
      "version: 1.3\n" +
      "\n" +
      "arguments: help no|yes no;log [01234]|debug|error|info|system|warn ;verbose [012] 0;version no|yes no\n" +
      "dependencies:\n Inca::Reporter\n Inca::Reporter::SimpleUnit\n" +
      "description: Check if tgcp can connect to all teragrid sites and translate url correctly\n" +
      "file: data.transfer.tgcp.unit\n" +
      "name: data.transfer.tgcp.unit\n" +
      "url: http://www.teragrid.org/userinfo/guide_data_gridftp.html#tgcp\n" +
      "version: 0.1\n" +
      "\n" +
      "arguments: cert .+ path to x509 certificate;help no|yes no;key .+ path to x509 key;log [01234]|debug|error|info|system|warn ;proxypw .+ server proxy password;timeout .+ number of seconds to wait between cmds;user .+ myproxy user;verbose [012] 0;version no|yes no\n" +
      "dependencies:\n Inca::Reporter\n Inca::Reporter::SimpleUnit\n" +
      "description: 1) store dummy proxy on myproxy server (myproxy-init) 2) retrieve proxy from server (myproxy-get-delegation) 3) destroy proxy on server (myproxy-destroy)\n" +
      "file: grid.interactive_access.myproxy.unit\n" +
      "name: grid.interactive_access.myproxy.unit\n" +
      "url: http://grid.ncsa.uiuc.edu/myproxy/\n" +
      "version: 1.8\n" +
      "\n" +
      "arguments: help no|yes no;log [01234]|debug|error|info|system|warn ;verbose [012] 0;version no|yes no\n" +
      "dependencies:\n Inca::Reporter\n Inca::Reporter::SimpleUnit\n" +
      "description: Checks that the gatekeeper at a host is accessible from the local machine\n" +
      "file: grid.middleware.globus.unit.gatekeeper\n" +
      "name: grid.middleware.globus.unit.gatekeeper\n" +
      "url: http://www.ncsa.uiuc.edu/People/jbasney/teragrid-setup-test.html\n" +
      "version: 1.4\n" +
      "\n" +
      "arguments: com .+;delim .+ The deliminator to use to separate regular expressions;help no|yes no;log [01234]|debug|error|info|system|warn 0;search .+;verbose [012] 1;version no|yes no\n" +
      "dependencies:\n Inca::Reporter\n Inca::Reporter::SimpleUnit\n makedist\n" +
      "description: Search result of supplied command for regular expressions\n" +
      "file: user.search.output.unit\n" +
      "name: user.search.output.unit\n" +
      "version: 1.1\n" +
      "\n" +
      "arguments: help no|yes no;log [01234]|debug|error|info|system|warn 0;verbose [012] 1;version no|yes no\n" +
      "dependencies:\n Inca::Reporter\n Inca::Reporter::SimpleUnit\n Inca::Reporter::GridProxy\n" +
      "description: Verifies that user has valid proxy\n" +
      "file: grid.middleware.globus.unit.proxy\n" +
      "name: grid.middleware.globus.unit.proxy\n" +
      "url: http://www.globus.org/security/proxy.html\n" +
      "version: 1.6\n" +
      "\n" +
      "dependencies:\n" +
      "description: test tar.gz\n" +
      "file: makedist.tar.gz\n" +
      "name: makedist\n" +
      "url: http://inca.sdsc.edu\n" +
      "version: 1.0\n" +
      "\n" +
      "arguments:\n" +
      "  help no|yes no\n" +
      "  log [012345]|debug|error|info|system|warn 0\n" +
      "  verbose [012] 1\n" +
      "  version no|yes no\n" +
      "dependencies:\n" +
      "  inca.Reporter\n" +
      "  inca.VersionReporter\n" +
      "description: Reports the version of gmake\n" +
      "file: cluster.devel.gmake.version\n" +
      "name: cluster.devel.gmake.version\n" +
      "url: http://gnu.org\n" +
      "version: 1"
      ;

    File repositoryDir;
    if ( dirPath == null ) {
      repositoryDir = File.createTempFile( "repository", "" );
      repositoryDir.delete();
      repositoryDir.mkdirs();
    } else {
      repositoryDir = new File( dirPath );
      if ( ! repositoryDir.exists() ) repositoryDir.mkdirs();
    }
    logger.debug( "Creating repository " + repositoryDir );    
    repositoryDir.deleteOnExit();
    File packagesGz = new File(
      repositoryDir.getAbsolutePath() + File.separator + "Packages.gz"
    );
    packagesGz.deleteOnExit();
    OutputStreamWriter output = new OutputStreamWriter(
      new GZIPOutputStream(new FileOutputStream(packagesGz))
    );
    output.write(packages);
    output.close();
    for ( String r : REPORTER_PACKAGES ) {
      URL catalogUrl = new URL( r );
      URLConnection conn = catalogUrl.openConnection();
      File packageFile = new File(
        repositoryDir.getAbsolutePath() + File.separator +
        new File(catalogUrl.getPath()).getName()
      );
      logger.debug( "Writing " + r + " to "+ packageFile );
      FileOutputStream out = new FileOutputStream(packageFile);
      packageFile.deleteOnExit();
      InputStream is = conn.getInputStream();
      byte[] block = new byte[1024];
      int bytesRead;
      while((bytesRead = is.read(block)) > 0) {
        out.write(block, 0, bytesRead);
      }
      is.close();
    }
    File repositoryXml = File.createTempFile( "repository", ".xml" );
    repositoryXml.deleteOnExit();
    Repositories repositories = new Repositories(
      repositoryXml.getAbsolutePath()
    );
    repositories.setRepositories(
      new Repository[] {
        new Repository( new URL("file://" + repositoryDir.getAbsolutePath()) )
      }
    );
    return repositories;
  }

  /**
   * Test function getRepositoryForPackage
   *
   * @throws Exception  if trouble running test
   */
  public void testGetRepositoryForPackage() throws Exception {
    Repositories repositories = createSampleRepository( null );
    Repository repo = createSampleRepository( null ).getRepositories()[0];
    String pkgName = "cluster.compiler.gcc.version";
    repositories.setRepositories
      ( new Repository[] { repositories.getRepositories()[0], repo } );
    for ( int i = 0; i < repo.catalog.length; i++ ) {
      if ( repo.catalog[i].containsValue(pkgName) ) {
        repo.catalog[i].setProperty( Repositories.VERSION_ATTR, "2.0" );
      }
    }   

    // latest version of package
    Repository foundRepo = repositories.getRepositoryForPackage(pkgName, null);
    assertNotNull( "returns repository for gcc version", foundRepo );
    assertEquals(
      "got latest version in repositories",
      "2.0",
      foundRepo.getLatestVersionOfPackage( pkgName )
    ) ;
    assertTrue(
      "repository url properly fetched",
      Pattern.matches(
        "^file://.*" + pkgName + "$",
        repositories.getRepositoryUrlForPackage(pkgName,null)
      )
    );

    // specific version
    assertNotNull(
      "returns repository for gcc version",
      repositories.getRepositoryForPackage( pkgName, "1.5" )
    );
    assertTrue(
      "repository url properly fetched",
      Pattern.matches(
        "^file://.*" + pkgName + "$",
        repositories.getRepositoryUrlForPackage( pkgName, "1.5" )
      )
    );

    // non-existent version
    assertNull(
      "returns repository for gcc version",
      repositories.getRepositoryForPackage(pkgName, "1.3")
    );
    assertNull(
      "repository url properly fetched",
        repositories.getRepositoryUrlForPackage(pkgName, "1.3")
    );

    assertFalse(
      "package update returns false",
      repositories.hasPackageUpdated( pkgName, "2.0")
    );
    assertTrue(
      "package update returns false",
      repositories.hasPackageUpdated( pkgName, "1.3")
    );

    // non-existent package
    assertNull( "returns null for bogus",
                repositories.getRepositoryForPackage( "bogus", "1.1") );
  }

  /**
   * Test ability to compare versions
   *
   * @throws Exception  if trouble running test
   */
  public void testVersionCompare() throws Exception {
    assertTrue( "1.3 is newer than 1.2",
    StringMethods.compareTo( "1.3", "1.2" ) > 0);
    assertFalse( "1.3 is not newer than 1.3",
    StringMethods.compareTo( "1.3", "1.3") > 0 );
    assertTrue( "2.5.1 is newer than 2.5",
    StringMethods.compareTo( "2.5.1", "2.5" ) > 0);
    assertFalse( "2.5 is not newer than 2.5.1",
    StringMethods.compareTo( "2.5", "2.5.1" ) > 0 );
    assertTrue( "2.5a newer than 2.5",
                StringMethods.compareTo( "2.5a", "2.5" ) > 0 );
  }

  /**
   * Test constructor and save
   * 
   * @throws Exception if trouble running test
   */
  public void testPersistence() throws Exception {
    Repositories repos = new Repositories( "/tmp/repos.txt" );
    File reposFile = new File( "/tmp/repos.txt" );
    reposFile.deleteOnExit();
    assertEquals( "blank repos loaded", 0, repos.getRepositories().length );
    Repositories repo1 = createSampleRepository( "/tmp/repo1" );
    Repositories repo2 = createSampleRepository( "/tmp/repo2" );
    Repository[] new_repos = new Repository[] {
     repo1.getRepositories()[0],
     repo2.getRepositories()[0]
    };
    repos.setRepositories( new_repos );
    Repositories repos2 = new Repositories( "/tmp/repos.txt" );
    assertEquals( "2 loaded", 2, repos2.getRepositories().length );
  }
}
