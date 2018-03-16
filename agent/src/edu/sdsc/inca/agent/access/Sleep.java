package edu.sdsc.inca.agent.access;

import org.apache.log4j.Logger;

import java.io.File;

import edu.sdsc.inca.agent.AccessMethod;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.agent.AccessMethodOutput;
import edu.sdsc.inca.agent.AccessMethodException;

/**
 * Used for testing to make sure the ReporterManagerStarter class handles
 * interrupts correctly. Can be configured to have the run method sleep on
 * the nth call so it can be interrupted and tested to see if the run method
 * handles the interrupt correctly.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class Sleep extends AccessMethod {
  // Constants
  private Logger logger = Logger.getLogger(this.getClass().toString());
  private String resource = null;
  private String command = null;
  public int sleepPeriod = 5000;
  public int sleepOnNthTime = 0;
  public int numSkips = 0;
  public String lastCommand = null;

  /**
   * Create a interrupt manual process.
   *
   * @param resource  The name of the resource to start the process on
   * @param resources The resource configuration information.
   */
  public Sleep( String resource, ResourcesWrapper resources ) {
    logger.info( "Initializing interrupt access method" );
    String sleepString = System.getProperty( "inca.interrupt.sleep" );
    sleepPeriod = Integer.parseInt( sleepString );
    String sleepOn = System.getProperty( "inca.interrupt.sleepOn" );
    sleepOnNthTime = Integer.parseInt( sleepOn );
    this.resource = resource;
  }

  /**
   * Log the remote file that would be transferred to a directory on the
   * local machine.
   *
   * @param remoteFile  Path to the remote file that will be transfered
   * @param localDir  Path to the directory on the local machine where
   *                   the remote file will be placed
   */
  public void get( String remoteFile, String localDir )
    throws AccessMethodException {

    logger.warn( "Get remote file '" + remoteFile + "' to '" + localDir + "'" );
  }

  /**
   * Log the file that would be transferred to a directory on a
   * remote machine .
   *
   * @param localFile  Path to the local file that will be transfered
   * @param remoteDir  Path to the directory on the remote machine where
   *                   the local file will be placed
   */
  public void put( String localFile, String remoteDir )
    throws AccessMethodException {

    File file = new File( localFile );
    if ( ! file.exists() ) {
      throw new AccessMethodException("File '" + localFile +"' does not exist");
    }
    logger.warn( "Put file '" + localFile + "' to '" + remoteDir + "'" );
  }

  /**
   * Log the command that should be executed on the remote machine.
   *
   * @param executable Path to the remote executable.
   * @param arguments  Contains the arguments that should be passed to the
   *                   executable
   * @param stdin      A string that will be passedd in as stdin to the process
   *                   when it is started
   * @param directory  Path to the directory where the process will be executed
   *                   from
   * @return The stdout and stderr of the executed process in
   * AccessMethodOutput
   */
  public AccessMethodOutput run( String executable, String[] arguments,
                                 String stdin, String directory)
    throws InterruptedException {


    int sleeptime = sleepPeriod;
    if ( numSkips < sleepOnNthTime ) {
      sleeptime = 0;
      numSkips++;      
    }
    lastCommand = getCommand( executable, arguments, directory );
    logger.warn( "[sleep " + sleeptime + "] cmd '" + lastCommand + "'" );
    Thread.sleep( sleeptime );
    return new AccessMethodOutput();
  }

  /**
   * Start a process on a remote machine.  This is a non-blocking call.
   *
   * Important note:  this process will not be killed on call to stop()
   *
   * @param executable  Path to the remote executable.
   * @param arguments   Contains the arguments that should be passed to the
   *                    executable
   * @param directory  Path to the directory where the process will be executed
   */
  public void start( String executable, String[] arguments, String stdin,
                     String directory ) {
    command = getCommand( executable, arguments, directory );
    logger.warn( "Start command '" + command + "'" );
  }

  /**
   * Just log that the process needs to be stopped.
   *
   * @throws edu.sdsc.inca.agent.AccessMethodException
   */
  public void stop() throws AccessMethodException {
    logger.warn( "Stop '" + command + "'" );
    command = null;
  }

  /**
   * Checks to see if the current ssh session is active.  Does not indicate
   * whether the remote process is alive (for now).
   *
   * @return true if the SSH session is alive; false otherwise.
   */
  public boolean isActive() throws AccessMethodException {
    return command != null;
  }

  /**
   * Log the specified command.
   *
   * @param exec       Path to the remote executable.
   * @param args       Contains the arguments that should be passed to the
   *                   executable
   * @param remoteDir  Path to the directory where the process will be executed
   *
   * @returns the remote command in string form
   */
  private String getCommand( String exec, String[] args, String remoteDir ) {
    String command = "";
    if ( remoteDir != null ) {
      command += "cd " + remoteDir + ";";
    }
    command += exec + " ";
    for ( int i = 0; i < args.length; i++ ) {
      command += args[i] + " ";
    }
    return command;
  }
}