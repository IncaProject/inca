package edu.sdsc.inca.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;
import junit.framework.TestCase;

/**
 *
 */
public class RepositoryPackageTest extends TestCase {

  private static final String CWD = System.getProperty("user.dir");
  private static final String SEP = File.separator;

  private static final String[] REPORTER_ATTRS = {
    "name", "kernel", "version", "1.7",
    "name", "gpfs", "version", "1.2",
    "name", "java", "version", "1.5",
    "name", "gsi", "version", "0.1"
  };
  private final int ATTRIBUTE_COUNT = 2;
  private static final String[] REPORTER_FILENAMES = {
    "cluster.os.kernel.version.rpt",
    "cluster.filesystem.gpfs.version.rpt",
    "cluster.java.sun.version.rpt",
    "grid.security.gsi.version.rpt"
  };
  private static final String[] REPORTER_BODIES = {

    "#!/usr/bin/perl\n" +
    "use strict;\n" +
    "use warnings;\n" +
    "use Inca::Reporter::Version;\n" +
    "my $reporter = new Inca::Reporter::Version(\n" +
    "  version => 1.7,\n" +
    "  description => 'Reports the os kernel release info',\n" +
    "  url => 'http://www.linux.org',\n" +
    "  package_name => 'kernel'\n" +
    ");\n" +
    "$reporter->processArgv(@ARGV);\n" +
    "$reporter->setVersionByExecutable('uname -r');\n" +
    "$reporter->print();\n",

    "#!/usr/bin/perl\n" +
    "use strict;\n" +
    "use warnings;\n" +
    "use Inca::Reporter::Version;\n" +
    "my $reporter = new Inca::Reporter::Version(\n" +
    "  version => 1.2,\n" +
    "  description => 'Reports the version of GPFS',\n" +
    "  url => 'http://www.almaden.ibm.com/cs/gpfs.html',\n" +
    "  package_name => 'gpfs'\n" +
    ");\n" +
    "$reporter->processArgv(@ARGV);\n" +
    "$reporter->setVersionByRpmQuery('gpfs');\n" +
    "$reporter->print();\n",

    "#!/usr/bin/perl\n" +
    "use strict;\n" +
    "use warnings;\n" +
    "use Inca::Reporter::Version;\n" +
    "my $reporter = new Inca::Reporter::Version(\n" +
    "  version => 1.5,\n" +
    "  description => 'Reports the version of java',\n" +
    "  url => 'http://java.sun.com',\n" +
    "  package_name => 'java'\n" +
    ");\n" +
    "$reporter->processArgv(@ARGV);\n" +
    "$reporter->setVersionByExecutable('java -version', 'java version \"(.+)\"');\n" +
    "$reporter->print();\n",

    "#!/usr/bin/perl\n" +
    "use strict;\n" +
    "use warnings;\n" +
    "use Inca::Reporter::Version;\n" +
    "my $reporter = new Inca::Reporter::Version(\n" +
    "  version => 0.1,\n" +
    "  description => 'Reports the version of GSI client tools',\n" +
    "  url => 'http://www.globus.org/gsi',\n" +
    "  package_name => 'gsi'\n" +
    ");\n" +
    "$reporter->processArgv(@ARGV);\n" +
    "$reporter->setVersionByGptQuery('globus_gsi_cert_utils');\n" +
    "$reporter->print();\n"

  };


  /**
   * Test that an instantiation with a non-existent URL throws an exception.
   */
  public void testNonexistentRepository() {
    Repository r = null;
    try {
      r = new Repository(new URL("file:///notthere"));
    } catch(MalformedURLException e) {
      fail("Unexpected URL exception " + e);
    } catch(IOException e) {
      return;
    }
    fail("IOException not raised for missing repository");
  }

  /**
   * Test that an instantiation with a valid URL succeeds.
   */
  public void testValidRepository() {
    String failMsg = null;
    int i;
    String path = CWD + SEP + "rep1";
    Properties[] ps = null;
    Repository r = null;
    try {
      makeRepository(path, 0, 2);
      r = new Repository(new File(path).toURL());
      ps = r.getCatalog();
      assertTrue( "exists returns correctly",
                  r.exists( REPORTER_ATTRS[0], REPORTER_ATTRS[1]) );
      assertFalse( "exists returns correctly for false",
                  r.exists( "name", "bogus") );
    } catch(MalformedURLException e) {
      failMsg = "Unexpected URL exception " + e;
    } catch(IOException e) {
      failMsg = "IOException raised for valid repository";
    }
    if(failMsg == null) {
      for(i = 0;
          i < ps.length &&
          !ps[i].getProperty("file").equals(REPORTER_FILENAMES[0]);
          i++) {
        // empty
      }
      if(i >= ps.length) {
        failMsg = "Cannot find package in test repository";
      }
    }
    deleteRepository(path);
    if(failMsg != null) {
      fail(failMsg);
    }

  }

  /**
   * Tests the getPropertyValues class method.
   */
  public void testGetPropertyValues() {
    String[] values = Repository.getPropertyValues("");
    assertEquals("split empty string", 0, values.length);
    values = Repository.getPropertyValues("    ");
    assertEquals("split blank string", 0, values.length);
    values = Repository.getPropertyValues("abcde");
    assertEquals("split single value length", 1, values.length);
    assertEquals("split single value", "abcde", values[0]);
    values = Repository.getPropertyValues("abcde;");
    assertEquals("split with empty value length", 2, values.length);
    assertEquals("split with empty value", "abcde", values[0]);
    assertEquals("split with empty value", "", values[1]);
    values = Repository.getPropertyValues("abcd;e");
    assertEquals("split with two values length", 2, values.length);
    assertEquals("split with two values", "abcd", values[0]);
    assertEquals("split with two values", "e", values[1]);
    values = Repository.getPropertyValues("\n  abcd;e");
    assertEquals("newline single value length", 1, values.length);
    assertEquals("newline single value", "abcd;e", values[0]);
    values = Repository.getPropertyValues("\n  ab\n  cd\n  ;e");
    assertEquals("newline multivalue length", 3, values.length);
    assertEquals("newline multivalue", "ab", values[0]);
    assertEquals("newline multivalue", "cd", values[1]);
    assertEquals("newline multivalue", ";e", values[2]);
  }

  public void testReporterFetch() throws IOException {
    String failMsg = null;
    String path = CWD + SEP + "rep1";
    Repository r = null;
    try {
      makeRepository(path, 0, 2);
      r = new Repository(new File(path).toURL());
      String body = new String(r.getReporter(REPORTER_FILENAMES[0]));
      if(!body.equals(REPORTER_BODIES[0])) {
        failMsg = "bad read of reporter; fetched \"" + body +
                  "\" instead of \"" + REPORTER_BODIES[0] + "\"";;
      }
    } catch(Exception e) {
      failMsg = "unexpected exception " + e;
    }
    deleteRepository(path);
    if(failMsg != null) {
      fail(failMsg);
    }
  }

  private void deleteRepository(String path) {
    File dir = new File(path);
    String[] contents = dir.list();
    for(int i = 0; i < contents.length; i++) {
      new File(path + SEP + contents[i]).delete();
    } 
    dir.delete();
  } 
  
  private void makeRepository(String path, int first, int last)
    throws IOException {
    new File(path).mkdir();
    String catalogPath = path + SEP + "Packages.gz";
    OutputStreamWriter output = new OutputStreamWriter(
      new GZIPOutputStream(new FileOutputStream(catalogPath))
    );
    for(int i = first, offset = first * ATTRIBUTE_COUNT * 2; i <= last; i++) {
      for(int j = 0; j < ATTRIBUTE_COUNT; j++) {
        String attribute = REPORTER_ATTRS[offset++];
        String value = REPORTER_ATTRS[offset++];
        output.write(attribute + ": " + value + "\n");
      } 
      output.write("file: " + REPORTER_FILENAMES[i] + "\n");
      OutputStreamWriter fout = new OutputStreamWriter(
        new FileOutputStream(path + SEP + REPORTER_FILENAMES[i])
      );
      fout.write(REPORTER_BODIES[i]);
      fout.close();
      output.write("\n");
    } 
    output.close();
  }

}
