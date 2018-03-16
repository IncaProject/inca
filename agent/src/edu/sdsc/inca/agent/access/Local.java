package edu.sdsc.inca.agent.access;

import org.apache.log4j.Logger;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.agent.AccessMethod;
import edu.sdsc.inca.agent.AccessMethodOutput;
import edu.sdsc.inca.agent.AccessMethodException;

import java.io.*;

/**
 * A class that implements AccessMethod using the Java Runtime and IO API
 */
public class Local extends AccessMethod {
  private Logger logger = Logger.getLogger(this.getClass().toString());
  private Process activeProcess = null;


  /**
   * Create a local process controlling it via Java Runtime class. The given
   * resource should exist in the resource configuration file. This constructor
   * doesn't do anything but is needed because the interface defines it.
   *
   * @param resource  The name of the resource to start the process on
   * @param resources The resource configuration information
   */
  public Local( String resource, ResourcesWrapper resources ) {

  }

  /**
   * Copy a list of local files to a local directory on this machine.
   *
   * @param localFiles  List of paths to local files that will be transfered
   * @param localDir  Path to the local directory where the remote files will be
   *                  placed
   *
   * @throws AccessMethodException
   */
  public void get( String[] localFiles, String localDir )
    throws AccessMethodException {

    transfer( localFiles, localDir );
  }

  /**
   * Copy a list of local files to a local directory on this machine.
   *
   * @param localFiles  List of paths to local files that will be transfered
   * @param localDir  Path to the local directory where the remote files will be
   *                  placed
   *
   * @throws AccessMethodException
   */
  public void put( String[] localFiles, String localDir )
    throws AccessMethodException {

    transfer( localFiles, localDir );
  } 

  /**
   * Copy a list of local files to a directory on this machine.
   *
   * @param localFiles  List of paths to local files that will be transfered
   * @param localDir  Path to the directory where the local file will be placed
   *
   * @throws AccessMethodException  If trouble fetching files
   */
  public void transfer( String[] localFiles, String localDir )
    throws AccessMethodException {

    File localDirHandle = new File( localDir );
    if ( ! localDirHandle.exists() ) {
      if ( ! localDirHandle.mkdirs() ) {
        throw new AccessMethodException(
          "Failed to create directory '" + localDir + "'"
        );
      }
    }

    for ( int i = 0; i < localFiles.length; i++ ) {
      File fileToCopy = new File( localFiles[i] );
      File dest = new File(
        localDirHandle.getAbsolutePath() + File.separator + fileToCopy.getName()
      );
      logger.info( "Copying " + localFiles[i] + " to " + localDir );
      try {
        copy( fileToCopy, dest );
      } catch ( IOException e ) {
        throw new AccessMethodException(
          "Copy of " + fileToCopy + " to " + dest + " failed", e
        );
      }
    }
  }

  /**
   * Given a path relative to the home directory, prepend the home directory
   * to the path and return the new string.
   *
   * @param path   A path relative to the user's home directory
   *
   * @return  A new string that contains the home directory prepended to the
   * provided path.
   */
  public String prependHome( String path ) {
    return System.getProperty("user.home") + File.separator +
           path.replaceFirst( "^~/", "");
  }

  /**
   * Execute the specified process on this resource.  This call will block
   * until the process has completed.
   *
   * @param executable Path to the executable.
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
    throws AccessMethodException, InterruptedException {

    File directoryHandle = null;
    if ( directory != null ) {
      directoryHandle = new File( directory );
    }

    String[] cmdarray = new String[arguments.length + 1];
    String cmdline = executable;
    cmdarray[0] = executable;
    for ( int i = 0; i < arguments.length; i++ ) {
      cmdarray[i+1] = arguments[i];
      cmdline += " " + arguments[i];
    }
    String stdout = "";
    String stderr = "";
    logger.info( "Runtime exec: " + cmdline );
    try {
      Process proc = Runtime.getRuntime().exec(cmdarray, null, directoryHandle);
      if ( stdin != null ) {
        proc.getOutputStream().write( stdin.getBytes() );
        proc.getOutputStream().close();
      }
      proc.waitFor();
      InputStream in = proc.getInputStream();
      int c;
      while ((c = in.read()) != -1) {
        stdout += (char)c;
      }
      in.close();
      InputStream err = proc.getErrorStream();
      while ((c = err.read()) != -1) {
        stderr += (char)c;
      }
      err.close();
    } catch ( IOException e ) {
      throw new AccessMethodException( "Local execution failed", e );
    }

    AccessMethodOutput result = new AccessMethodOutput();
    logger.debug( "Stdout: " + stdout);
    logger.debug( "Stderr: " + stderr );    
    result.setStdout( stdout );
    result.setStderr( stderr );
    return result;
  }

  /**
   * Start a process on this machine.  This is a non-blocking call.
   *
   * Important note:  this process will not be killed on call to stop()
   *
   * @param executable  Path to the remote executable.
   * @param arguments   Contains the arguments that should be passed to the
   *                    executable
   * @param directory  Path to the directory where the process will be executed
   * @throws AccessMethodException
   */
  public void start( String executable, String[] arguments, String stdin,
                     String directory ) throws AccessMethodException {

   File directoryHandle = null;
    if ( directory != null ) {
      directoryHandle = new File( directory );
    }

    // Runtime.exec accepts either a string or an array.  If we use string
    // cmd, exec separates the arguments using space despite escaped quotes
    // (e.g., "echo hello" is tokanized as '"echo' and 'hello"'. So, we use the
    // cmdarray option.  However, the array option will interprete quotes
    // literally.  So, we strip them off the arguments if they exist.
    String[] cmdarray = new String[arguments.length + 1];
    String cmdline = executable;
    cmdarray[0] = executable;
    for ( int i = 0; i < arguments.length; i++ ) {
      String arg = arguments[i].replaceFirst( "^\"", "" );
      arg = arg.replaceFirst( "\"$", "" );
      cmdarray[i+1] = arg;
      cmdline += " " + arg;
    }
    logger.info( "Runtime exec: " + cmdline );
    try {
      activeProcess = Runtime.getRuntime().exec(cmdarray, null, directoryHandle);
      if ( stdin != null ) {
        activeProcess.getOutputStream().write( stdin.getBytes() );
        activeProcess.getOutputStream().close();
      }
    } catch ( IOException e ) {
      throw new AccessMethodException( "Unable to start local process", e );
    }
  }

  /**
   * This call should kill the remote process that was started by the start()
   * call.  However, since a remote process won't be killed when the ssh client
   * session is closed, the remote process will persist.  Will try to figure
   * out if there is a SSH configuration mechanism that can be turned on so
   * that the process will be killed upon disconnect.  This call does
   * disconnect the active SSH session.
   *
   * @throws AccessMethodException
   */
  public void stop() throws AccessMethodException {
    activeProcess.destroy();
  }

  /**
   * Checks to see if the current ssh session is active.  Does not indicate
   * whether the remote process is alive (for now).
   *
   * @return true if the SSH session is alive; false otherwise.
   */
  public boolean isActive() throws AccessMethodException {
    try {
      activeProcess.exitValue();
      return false;
    } catch( IllegalThreadStateException e ) {
      return true;
    }
  }

  /**
   * Copies a local file since Java does not seem to have a copy file function.
   * Code comes from the Java Almanac.
   *
   * http://javaalmanac.com/egs/java.io/CopyFile.html?l=new
   *
   *  @param src  File to copy
   * @param dst  Destination of file
   *
   * @throws IOException   If trouble copying a file
   */
  public static void copy(File src, File dst) throws IOException {
    InputStream in = new FileInputStream(src);
    OutputStream out = new FileOutputStream(dst);

    // Transfer bytes from in to out
    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }

}
