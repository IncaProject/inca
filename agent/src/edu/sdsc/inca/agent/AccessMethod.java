package edu.sdsc.inca.agent;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.agent.access.Sleep;
import edu.sdsc.inca.agent.access.Local;
import edu.sdsc.inca.agent.access.Manual;
import edu.sdsc.inca.agent.access.GsiSsh;
import edu.sdsc.inca.agent.access.PublicKeySsh;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.ResourcesWrapper;
import org.apache.log4j.Logger;

/**
 * An abstract class used to represent methods for transferring files and
 * running processes on resources.  Supported methods include
 * GSISSH, Local (exec), Manual, and SSH currently. The classes that implement
 * each of these methods exist in the 'access' directory.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
abstract public class AccessMethod  {
  static final private Logger logger = Logger.getLogger( AccessMethod.class );

  /**
   * Scan the resource configuration specified in resources and use the
   * remoteInvocationMethod value for the resource to create the appropriate
   * subclass of AccessMethod.
   *
   * @param resource  The name of the resource to start the process on
   *
   * @param resources  The resource configuration information
   *
   * @param temp      Path to a directory for temporary files to be stored

   * @return  An object of a AccessMethod subclass
   *
   * @throws ConfigurationException   unknown access method specified
   */
  static public AccessMethod create( String resource,
                                     ResourcesWrapper resources,
                                     String temp )
    throws ConfigurationException {

    String method = resources.getValue(resource, Protocol.COMPUTE_METHOD_MACRO);
    AccessMethod process = null;
    if ( method == null ) {
      method = Protocol.COMPUTE_METHOD_MACRO_DEFAULT;
    }
    logger.info(
      "Using invocation method '" + method + "' for resource '" + resource + "'"
    );
    if ( method.equals("ssh") ) {
      process = new PublicKeySsh( resource, resources );
    } else if ( method.equals("gsissh") ) {
      process = new GsiSsh( resource, resources );
    } else if ( method.equals("local") ) {
      process = new Local( resource, resources );
    } else if ( method.equals("manual") ) {
      process = new Manual( resource, resources );
    } else if ( method.equals("sleep") ) {
      process = new Sleep( resource, resources );
    } else {
      throw new ConfigurationException(
        "Uknown remote process invocation method '" + method +
        "' specified for reporter manager"
      );
    }
    return process;
  }

  /**
   * Transfer a file from a remote machine to a local directory.
   *
   * @param remoteFile   contains a path to the file on the remote file system
   * @param localDirPath contains a path to a directory/file on the local
   *        machine
   *
   * @throws AccessMethodException if unable to fetch remote file
   */
  public void get ( String remoteFile, String localDirPath )
    throws AccessMethodException {

    /* this assumes the implementing class has implemented the other get
    method */
    get( new String[] { remoteFile }, localDirPath );
  }

  /**
   * Transfer a list of remote files to a directory on the local machine.
   *
   * @param remoteFiles  list of paths to files on the remote file system
   * @param localDirPath contains a path to a directory/file on the local
   *        machine
   *
   * @throws AccessMethodException if unable to fetch remote file
   */
  public void get ( String[] remoteFiles, String localDirPath )
    throws AccessMethodException {

    /* this assumes the implementing class has implemented the other get
    method */
    for ( String remoteFile : remoteFiles ) {
      get( remoteFile, localDirPath );
    }
  }

  /**
   * Checks to see if the process started by start() is active.
   *
   * @return true if the process is running; false otherwise.
   *
   * @throws AccessMethodException if unable to determine if process is active
   */
  abstract public boolean isActive() throws AccessMethodException;

  /**
   * Given a path relative to the home directory, prepend the home signifier
   * for the given access method to the path and return the new string.
   *
   * @param path   A path relative to the user's home directory
   *
   * @return  A new string that contains the home signifier prepended to the
   * provided path.
   */
  public String prependHome( String path ) {
    // most access methods recognize a path w/o a starting / as being relative
    // to the home dir.
    return path;
  }

  /**
   * Transfer a file to a directory on a remote machine.
   *
   * @param localFile      contains a path to the file on the local file system
   * @param remoteDirPath contains a path to a directory/file on a remote
   *        machine
   *
   * @throws AccessMethodException if unable to transfer remote file
   */
  public void put ( String localFile, String remoteDirPath )
    throws AccessMethodException {

    /* this assumes the implementing class has implemented the other put
    method */
    put( new String[] { localFile }, remoteDirPath );
  }

  /**
   * Transfer a list of files to a directory on a remote machine.
   *
   * @param localFiles    list of paths to files on the local file system
   * @param remoteDirPath contains a path to a directory/file on a remote
   *        machine
   *
   * @throws AccessMethodException if unable to transfer remote file
   */
  public void put ( String[] localFiles, String remoteDirPath )
    throws AccessMethodException {

    /* this assumes the implementing class has implemented the other put
    method */
    for ( String localFile : localFiles ) {
      put( localFile, remoteDirPath );
    }
  }

  /**
   * Run a process on a remote machine.
   *
   * @param executable  Path to the remote executable.
   * @param arguments   Contains the arguments that should be passed to the
   *                    executable
   * @return The stdout and stderr of the executed process in
   * RemoteProcessObject.
   *
   * @throws AccessMethodException if unable to execute remote process
   * @throws InterruptedException if interrupted while running remote process
   */
  public AccessMethodOutput run(String executable, String[] arguments)
    throws AccessMethodException, InterruptedException {

    return run(executable, arguments, null);
  }

  /**
   * Run a process on a remote machine.
   *
   * @param executable  Path to the remote executable.
   * @param arguments   Contains the arguments that should be passed to the
   *                    executable
   * @param stdin       A string that will be passedd in as stdin to the process
   *                    when it is started
   * @return The stdout and stderr of the executed process in
   * RemoteProcessObject.
   *
   * @throws AccessMethodException if unable to execute remote process
   * @throws InterruptedException if interrupted while running remote process
   */
  public AccessMethodOutput run(String executable, String[] arguments,
                                String stdin )
    throws AccessMethodException, InterruptedException {

    return run(executable, arguments, stdin, null);
  }

  /**
   * Execute the specified process on the remote resource.  This call will block
   * until the process has completed.
   *
   * @param executable Path to the remote executable.
   * @param arguments  Contains the arguments that should be passed to the
   *                   executable
   * @param stdin      A string that will be passedd in as stdin to the process
   *                   when it is started
   * @param directory  Path to the directory where the process will be executed
   *                   from
   * @return The stdout and stderr of the executed process in
   * RemoteProcessObject.
   *
   * @throws AccessMethodException if unable to execute remote process
   * @throws InterruptedException if interrupted while running remote process
   */
  abstract public AccessMethodOutput run( String executable,
                                          String[] arguments,
                                          String stdin,
                                          String directory )
    throws AccessMethodException, InterruptedException;

  /**
   * Start a process on a remote machine.  This is a non-blocking call.
   *
   * @param executable  Path to the remote executable.
   * @param arguments   Contains the arguments that should be passed to the
   *                    executable
   *
   * @throws AccessMethodException if unable to start remote process
   */
  public void start(String executable, String[] arguments )
    throws AccessMethodException {
    start(executable, arguments, null, null);
  }

  /**
   * Start a process on a remote machine.  This is a non-blocking call.
   *
   * @param executable  Path to the remote executable.
   * @param arguments   Contains the arguments that should be passed to the
   *                    executable
   * @param stdin       A string that will be passedd in as stdin to the process
   *                    when it is started
   *
   * @throws AccessMethodException if unable to start remote process
   */
  public void start(String executable, String[] arguments, String stdin)
    throws AccessMethodException {
    start(executable, arguments, stdin, null);
  }

  /**
   * Start a process on a remote machine.  This is a non-blocking call.
   *
   * @param executable  Path to the remote executable.
   * @param arguments   Contains the arguments that should be passed to the
   *                    executable
   * @param in          A string that will be passedd in as stdin to the process
   *                    when it is started
   * @param directory  Path to the directory where the process will be executed
   *                   from
   *
   * @throws AccessMethodException if unable to start remote process
   */
  abstract public void start(String executable, String[] arguments, String in,
                             String directory)
    throws AccessMethodException;

  /**
   * Stop the currently running process started by start().
   *
   * @throws AccessMethodException  if unable to stop remote process
   */
  abstract public void stop() throws AccessMethodException;

}
