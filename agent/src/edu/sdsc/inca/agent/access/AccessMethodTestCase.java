package edu.sdsc.inca.agent.access;

import junit.framework.TestCase;

import java.util.regex.Pattern;
import java.util.Calendar;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;

import edu.sdsc.inca.agent.AccessMethodOutput;
import edu.sdsc.inca.agent.AccessMethod;
import edu.sdsc.inca.agent.AccessMethodException;
import edu.sdsc.inca.util.StringMethods;
import org.apache.log4j.Logger;

/**
 * Base class for implementing access method tests (i.e., run the same tests
 * consistently on each method).  To use,
 *
 * 1) implement hasRequirements which can be used to check for specific
 *    prerequisites before a test is run (default is to always run test)
 *
 * 2) set checkResult to specify whether results should be checked or not
 *    (e.g., we don't for Manual).  Default is to always check.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public abstract class AccessMethodTestCase extends TestCase {
  protected static Logger logger = Logger.getLogger(AccessMethodTestCase.class);

  protected AccessMethod[] procs = new AccessMethod[0];
  protected boolean checkResult = true;

  public class TmpPasswordFilter implements FilenameFilter {
    public boolean accept( File dir, String name ) {
      return Pattern.matches( "inca.*tmp", name );
    }
  }


  /**
   * Check for any requirements for running a test using the specific access
   * method.
   *
   * @return  True if all requirements are met and it's okay to run the test;
   * false otherwise.
   */
  public boolean hasRequirements() {
    return true;
  }

  /**
   * Run thru a set of transfer tests using the specified access method
   *
   * @throws Exception  if problem executing test
   */
  public void testTransfer() throws Exception {
    if ( ! hasRequirements() ) return;

    for ( AccessMethod proc : procs ) {
      execTransfers( proc );
    }
  }

  /**
   * Run thru a set of execution tests using the specified access method
   *
   * @throws Exception if problem executing test
   */
  public void testRun() throws Exception {
    if ( ! hasRequirements() ) return;

    // clean up from any previous runs
    deleteTempFiles();
    
    for ( AccessMethod proc : procs ) {
      execRun( proc );
    }
  }

  /**
   * Need to make sure access methods are throwing InterruptedExceptions
   * when they get interrupted
   *
   * @throws Exception  if problem executing test
   */
  public void testRunInterrupt() throws Exception {
    if ( ! hasRequirements() || ! checkResult ) return;

    class RunSleepCommand extends Thread {
      public AccessMethod a;
      public boolean done = false;
      public boolean amException = false;
      public boolean iException = false;

      public void run() {
        AccessMethodOutput out = null;
        try {
          logger.info( "Starting sleep" );
          out = a.run( "/bin/sleep", new String[]{ "60"}, null, null );
          logger.info( "Finished sleep" );
        } catch (AccessMethodException e) {
          logger.warn( "Caught access method exception", e );
          amException = true;
        } catch (InterruptedException e) {
          logger.warn( "Caught interrupted method exception" + e );
          iException = true;
        } finally {
          logger.info( "Thread done" );
          if ( out != null ) {
            logger.info( "STDERR: " + out.getStderr() );
            logger.info( "STDOUT: " + out.getStdout() );
          }
          done = true;
        }
      }
    }
    for ( AccessMethod proc : procs ) {
      RunSleepCommand a = new RunSleepCommand();
      a.a = proc;
      a.start();
      try {
        logger.info( "Waiting for thread to start" );
        Thread.sleep( 5000 );
        logger.info( "Interrupting thread" );
        a.interrupt();
        a.join();
        logger.info( "Thread joined" );
        assertTrue( "Thread caught interrupted exception", a.iException );
      } catch ( InterruptedException e ) {
        // empty
      }
    }

  }

  /**
   * Test that we can get the remote home directory
   * 
   * @throws Exception  if problem executing test
   */
  public void testHome() throws Exception {
    if ( ! hasRequirements() ) return;    
    if ( ! new File( System.getProperty("user.home") ).exists() ) return;

    String testDir = ".inca.test";
    for ( AccessMethod proc : procs ) {
      // ~ gets replaced
      String homedir = "~/somefile";
      assertFalse( Pattern.matches("^~\\/", homedir) );

      File file = File.createTempFile( "incatest", "tmp" );
      FileWriter writer = new FileWriter( file );
      writer.write(
        "#!/bin/sh\n" +
        "\n" +
        "echo hello\n"
      );
      writer.close();
      logger.info( "temp file " + file.getAbsolutePath() );
      StringMethods.deleteDirectory(
        new File( System.getProperty("user.home") + File.separator + testDir )
      );
      proc.put(file.getAbsolutePath(), proc.prependHome(testDir));
      file.delete();
      AccessMethodOutput result = proc.run(
        "/bin/bash",
        new String[]{
          "-l",
          "-c",
          "sh " + proc.prependHome(testDir) + File.separator + file.getName()
        }
      );
      logger.debug( "STDOUT: " + result.getStdout() );
      logger.debug( "STDERR: " + result.getStderr() );
      if ( checkResult ) {
        assertTrue(
          proc.getClass().getName() + " home works",
          Pattern.matches( "(?m)(?s)hello\n?", result.getStdout())
        );
      }
      result = proc.run(
        "/bin/sh",
        new String[]{ file.getName() },
        null,
        proc.prependHome( testDir )
      );
      logger.debug( "STDOUT: " + result.getStdout() );
      logger.debug( "STDERR: " + result.getStderr() );      
      if ( checkResult ) {
        assertTrue(
          proc.getClass().getName() + " home works",
          Pattern.matches( "(?m)(?s)hello\n?", result.getStdout())
        );
      }

    }
  }

  /**
   * Run thru a set of start/stop process tests using the specified access
   * method
   *
   * @throws Exception  if problem executing test
   */
  public void testStartStop() throws Exception {
    if ( ! hasRequirements() ) return;

    // clean up from any previous runs
    deleteTempFiles();

    for ( AccessMethod proc : procs ) {
      execStartStop( proc );
    }
  }

  /**
   * Check to see if there are any temporary files leftover
   */
  private void checkForTempFiles() {
    File tmpdir = new File( "/tmp" );
    File [] tmpFilesLeftover = tmpdir.listFiles( new TmpPasswordFilter() );
    String leftOvers = "";
    for( File leftover : tmpFilesLeftover ) {
      leftOvers += " " + leftover;
    }
    assertEquals( "no tempfiles leftover", "", leftOvers );
  }

  /**
   * Test a simple executable 'echo hello' with and without directory specified.
   *
   * @param proc  A configured access method object
   *
   * @throws Exception   if problem executing run tests
   */
  private void execRun( AccessMethod proc )
    throws Exception {

    // test bad exec and see if files are cleaned up
    try {
      proc.run( "/bin/cat38323", new String[]{}, "hello", null );
    } catch ( AccessMethodException e ) {
      // empty
    }
    checkForTempFiles();

    // without stdin
    String [] args = new String[] { "-c", "echo hello" };
    AccessMethodOutput result = proc.run( "/bin/sh", args );
    if ( checkResult ) {
      assertTrue(
        proc.getClass().getName() + " run works",
        Pattern.matches( "(?m)(?s)hello\n?", result.getStdout())
      );
    }
    checkForTempFiles();

    // with special chars
    String[] quotedargs = new String[] { "-c", "echo 'load1>5'" };
    result = proc.run( "/bin/sh", quotedargs );
    if ( checkResult ) {
      assertTrue(
        proc.getClass().getName() + " quoted arg run works",
        Pattern.matches( "(?m)(?s)load1>5\n?", result.getStdout())
      );
    }
    checkForTempFiles();


    // with stdin
    result = proc.run( "/bin/cat", new String[]{}, "hello", null );
    if ( checkResult ) {
      assertTrue(
        proc.getClass().getName() + " run works with stdin",
        Pattern.matches( "(?m)(?s)hello\n?", result.getStdout())
      );
    }
    checkForTempFiles();

    result = proc.run( "/bin/sh", args, null, "/tmp" );
    if ( checkResult ) {
      assertTrue(
        proc.getClass().getName() + " run works with dir specified",
        Pattern.matches( "(?m)(?s)hello\n?", result.getStdout())
      );
    }
    checkForTempFiles();
  }

  /**
   * Test the start of a process (very simple sleep for a minute) and then
   * stop it.  Also test stdin.
   *
   * @param proc  A configured access method object
   *
   * @throws Exception   if problem running non-blocking remote command
   */
  private void execStartStop( AccessMethod proc )
    throws Exception {

    // no stdin
    String [] args = { "60" };
    proc.start( "/bin/sleep", args, null );
    if ( checkResult ) {
      assertEquals( "local start works", true, proc.isActive() );
    }
    Thread.sleep(5000);
    proc.stop();
    Thread.sleep(2000);
    if ( checkResult ) {
      assertEquals( "local stop works", false, proc.isActive() );
    }
    checkForTempFiles();

    // with stdin
    File tmpFile = File.createTempFile( "inca", "tmp" );
    proc.start( "/usr/bin/tee", new String[]{ tmpFile.getAbsolutePath()},
                "test" );
    Thread.sleep(5000);

    if ( checkResult ) {
      assertEquals( "tee no longer working", false, proc.isActive() );
      assertTrue( "tmpFile created", tmpFile.exists() );
      BufferedReader reader = new BufferedReader( new FileReader(tmpFile) );
      String data = reader.readLine();
      reader.close();
      assertTrue( "test in file", data.matches( "test") );
    }
    tmpFile.delete();
    checkForTempFiles();
  }

  /**
   * Create and return a temporary file for data transfer.
   *
   * @param prefix  A string to be prefixed to the temp file name
   *
   * @return  A regular file handle to a temporary file
   *
   * @throws Exception if trouble creating file
   */
  private File createTempDataFile(String prefix) throws Exception {
    File f = File.createTempFile
      ( prefix, "tmp", new File ( System.getProperty( "user.dir" ) ) );
    FileWriter writer = new FileWriter( f );
    writer.write( "test data for file transfer " +
                  Calendar.getInstance().getTimeInMillis()  + "\n" );
    writer.close();
    return f;
  }

  /**
   * Check to see if there are any temporary files leftover
   */
  private void deleteTempFiles() {
    File tmpdir = new File( "/tmp" );
    File [] tmpFilesLeftover = tmpdir.listFiles( new TmpPasswordFilter() );
    for( File tmpFile : tmpFilesLeftover ) {
      tmpFile.delete();
    }
  }

  /**
   * Test the transfer of one and multiple files
   *
   * @param proc  A configured access method object
   *
   * @throws Exception  if problem executing transfers
   */
  private void execTransfers( AccessMethod proc )
    throws Exception {

    File dest1, dest2, source1, source2, verify1, verify2;
    boolean tranfersWorked;

    // check transfers of single file
    source1 = createTempDataFile("incatest1");
    dest1 = new File( "/tmp/" + source1.getName() );
    proc.put( source1.getAbsolutePath(), dest1.getParent() );

    // now get it back
    source2 = new File( source1.getAbsolutePath() + ".copy" );
    proc.get( dest1.getAbsolutePath(), source2.getAbsolutePath() );
    tranfersWorked = source2.exists();
    try {
      source1.delete();
      StringMethods.deleteDirectory( source2 );
      // if using local globus server, delete
      if ( dest1.exists() ) dest1.delete();
    } catch ( Exception e ) {
      // empty
    }
    if ( checkResult ) {
      assertTrue( "single-file get works", tranfersWorked );
    }

    // check transfers of multiple files
    source1 = createTempDataFile("incatest2");
    source2 = createTempDataFile("incatest3");
    dest1 = new File( "/tmp/" + source1.getName() );
    dest2 = new File( "/tmp/" + source2.getName() );
    File scratchDir = createTempDataFile("incatestScratch");
    scratchDir.delete();
    scratchDir.mkdirs();
    verify1 = new File( scratchDir + File.separator + source1.getName() );
    verify2 = new File( scratchDir + File.separator + source2.getName() );
    proc.put( new String[] {source1.getAbsolutePath(), source2.getAbsolutePath()},
              dest1.getParent() );
    proc.get( new String[] {dest1.getAbsolutePath(), dest2.getAbsolutePath()},
              scratchDir.getAbsolutePath() );
    if ( checkResult ) {
      assertTrue( "multi put/get file1 works",
        StringMethods.fileContents(source1.toString()).
          equals(StringMethods.fileContents(verify1.toString()) ) );
      assertTrue( "multi put/get file2 works",
        StringMethods.fileContents(source2.toString()).
          equals(StringMethods.fileContents(verify2.toString()) ) );
    }
    try {  // if using local globus server, delete
      source1.delete();
      source2.delete();
      if ( dest1.exists() ) dest1.delete();
      if ( dest2.exists() ) dest2.delete();
      verify1.delete();
      verify2.delete();
      StringMethods.deleteDirectory( scratchDir );
    } catch ( Exception e ) {
      // empty
    }

    // test ability to transfer files to non-yet created directories
    source1 = createTempDataFile("incatest4");
    dest1 = new File( "/tmp/tmpa/tmpb/" + source1.getName() );
    proc.put( source1.getAbsolutePath(), dest1.getParent() );
    verify1 = new File( "/tmp/tmpc/tmpd/" + source1.getName() );
    proc.get( dest1.getAbsolutePath(), verify1.getParent() );
    if ( checkResult ) {
      assertTrue( "create dir and transfer works",
        StringMethods.fileContents(source1.toString()).
          equals(StringMethods.fileContents(verify1.toString()) ) );
    }
    try {
      File remoteDir = new File( "/tmp/tmpa" );
      if ( remoteDir.exists() ) StringMethods.deleteDirectory( remoteDir );
      StringMethods.deleteDirectory( new File( "/tmp/tmpc" ) );
      source1.delete();
    } catch ( Exception e ) {
      // empty
    }   

    // test ability to put files to user's home directory
    if ( ! new File( System.getProperty("user.home") ).exists() ) return;
    source1 = createTempDataFile("incatest5");
    dest1 = new File ( System.getProperty("user.home") + File.separator +
                       source1.getName() );
    verify1 = new File( "/tmp/" + source1.getName() );
    proc.put( source1.getAbsolutePath(), proc.prependHome("") );
    proc.get( proc.prependHome(source1.getName()), "/tmp");
    if ( checkResult ) {
      assertTrue( "home dir intepretations works",
        StringMethods.fileContents(source1.toString()).
          equals(StringMethods.fileContents(verify1.toString()) ) );
    }
    try {
      source1.delete();
      if ( dest1.exists() ) dest1.delete();
      verify1.delete();
    } catch ( Exception e ) {
      // empty
    }

  }

}
