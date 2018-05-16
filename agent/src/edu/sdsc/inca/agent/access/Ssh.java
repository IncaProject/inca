package edu.sdsc.inca.agent.access;


import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.io.BufferedReader;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import com.sshtools.sftp.SftpClient;
import com.sshtools.sftp.SftpStatusException;
import com.sshtools.ssh.ChannelOpenException;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.SshSession;

import edu.sdsc.inca.agent.AccessMethodOutput;
import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.agent.AccessMethod;
import edu.sdsc.inca.agent.AccessMethodException;


/**
 * A class that implements AccessMethod using the SSH protocol for transferring
 * files and running processes on remote resources.
 *
 * Important note: stop() does not kill the remote process currently
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public abstract class Ssh extends AccessMethod {
  // Member variables
  protected String sshServer;
  protected int sshPort = 22;
  protected Logger logger = Logger.getLogger(this.getClass().toString());
  protected SshClient activeSsh = null;
  protected SshSession activeSession = null;

  // Public Methods

  /**
   * Transfer a list of remote files to a directory on the local machine using
   * SSH.
   *
   * @param remoteFiles  List of paths to remote files that will be transfered
   * @param localDir   Path to the directory on the local machine where
   *                   the remote files will be placed
   * @throws AccessMethodException
   */
  @Override
  public void get( String[] remoteFiles, String localDir )
    throws AccessMethodException {

    SshClient ssh;
    try {
      ssh = connect(true);
    } catch ( Exception e ) {
      throw new AccessMethodException( "Unable to connect to " + sshServer, e );
    }
    if ( ssh == null ) {
      throw new AccessMethodException( "Unable to connect to " + sshServer );
    }
    try {
      File localDirF = new File( localDir );
      if ( ! localDirF.exists() ) localDirF.mkdirs();
      SftpClient sftp = new SftpClient(ssh);
      sftp.lcd( localDir );
      for ( int i = 0; i < remoteFiles.length; i++ ) {
        String remoteFile = remoteFiles[i].replaceFirst( "\\$\\{HOME\\}/", "" );
        logger.info( "Getting " + remoteFile + " from " + sshServer );
        sftp.get( remoteFile );
      }
      sftp.quit();
      ssh.disconnect();
    } catch ( Exception e ) {
      throw new AccessMethodException( "Unable to put files", e );
    }
  }

  /**
   * Checks to see if the current ssh session is active.  Does not indicate
   * whether the remote process is alive (for now).
   *
   * @return true if the SSH session is alive; false otherwise.
   */
  @Override
  public boolean isActive() throws AccessMethodException {
    return activeSsh.isConnected() && !activeSession.isClosed();
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
  @Override
  public String prependHome( String path ) {
    return "${HOME}/" + path.replaceFirst( "^~/", "" );
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
   * AccessMethodOutput
   */
  @Override
  public AccessMethodOutput run( String executable, String[] arguments,
                                 String stdin, String directory)
    throws AccessMethodException, InterruptedException {

    String stdout = null;
    String stderr = null;
    try {
      SshClient ssh = connect(false);
      if ( ssh == null ) {
        throw new IOException( "Could not create ssh connection" );
      }
      SshSession session = sendCommand( ssh, executable, arguments,
                                                  directory );
      if ( stdin != null ) {
        session.getOutputStream().write( stdin.getBytes() );
        session.getOutputStream().close();
      }

      // read stdout/stderr
      BufferedReader stdoutReader = new BufferedReader(
        new InputStreamReader( session.getInputStream() )
      );
      BufferedReader stderrReader = new BufferedReader(
        new InputStreamReader( session.getStderrInputStream() )
      );
      stdout = "";
      stderr = "";
      String line;
      while( (line = stdoutReader.readLine()) != null ) {
        stdout += line;
      }
      while( (line = stderrReader.readLine()) != null ) {
        stderr += line;
      }
      session.close();
      ssh.disconnect();
    } catch ( IOException e ) {
      // throws general IOException if interrupted, we want the interrupt
      if ( Pattern.matches("(?i)^.*interrupt.*$", e.getMessage()) ){
        throw new InterruptedException( "Received interrupt" );
      }
      throw new AccessMethodException( "SSH remote run failed", e );
    } catch ( SshException | OperatorCreationException | ChannelOpenException | GeneralSecurityException | ConfigurationException e ) {
      throw new AccessMethodException( "SSH remote run failed", e );
    }

    AccessMethodOutput result = new AccessMethodOutput();
    result.setStdout( stdout );
    result.setStderr( stderr );
    return result;
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
   * @throws AccessMethodException
   */
  @Override
  public void start( String executable, String[] arguments, String stdin,
                     String directory ) throws AccessMethodException {

    try {
      activeSsh = connect(false);
      activeSession = sendCommand( activeSsh, executable, arguments,
                                   directory );
      if ( stdin != null ) {
        activeSession.getOutputStream().write( stdin.getBytes() );
        activeSession.getOutputStream().close();
      }
    } catch ( Exception e ) {
      throw new AccessMethodException( "Unable to start remote process", e );
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
  @Override
  public void stop() throws AccessMethodException {
    activeSession.close();
    activeSsh.disconnect();
  }

  /**
   * Transfer a list of local files to a directory on a remote machine using
   * SSH.
   *
   * @param localFiles  List of paths to local files that will be transfered
   * @param remoteDir   Path to the directory on the remote machine where
   *                   the local files will be placed
   * @throws AccessMethodException
   */
  @Override
  public void put( String[] localFiles, String remoteDir )
    throws AccessMethodException {

    SshClient ssh;
    try {
      ssh = connect(true);
    } catch ( Exception e ) {
      throw new AccessMethodException( "Unable to connect to " + sshServer, e );
    }
    if ( ssh == null ) {
      throw new AccessMethodException( "Unable to connect to " + sshServer );
    }
    try {
      SftpClient sftp = new SftpClient(ssh);
      remoteDir = remoteDir.replaceFirst( "\\$\\{HOME\\}/", "" );
      try {
        sftp.stat(remoteDir);
      } catch ( SftpStatusException e ) {
        sftp.mkdirs(remoteDir);
      }
      sftp.cd( remoteDir );
      String cwd = System.getProperty( "user.dir" );
      sftp.lcd( cwd );
      for ( int i = 0; i < localFiles.length; i++ ) {
        logger.info(
          "Tranferring " + localFiles[i] + " to " + sshServer + " - " + remoteDir
        );
        sftp.put( localFiles[i] );
      }
      sftp.quit();
      ssh.disconnect();
    } catch ( Exception e ) {
      throw new AccessMethodException( "Unable to put files", e );
    }
  }

  // Protected Methods

  /**
   * Connect and authenticate to the remote resource.
   *
   * @param noDelay
   *
   * @return An active SshClient connection to the remote resource.
   *
   * @throws IOException  if trouble connecting to ssh server
   * @throws SshException
   * @throws OperatorCreationException
   * @throws GeneralSecurityException
   * @throws ConfigurationException
   */
  protected abstract SshClient connect(boolean noDelay) throws IOException, SshException, OperatorCreationException, GeneralSecurityException, ConfigurationException;

  // Private Methods

  /**
   * Send the specified command to the open ssh client and return the active
   * session.
   *
   * @param ssh        An open SshClient connection to the remote resource.
   * @param exec       Path to the remote executable.
   * @param args       Contains the arguments that should be passed to the
   *                   executable
   * @param remoteDir  Path to the directory where the process will be executed
   * @return active SessionChannelClient that has just sent a command.
   *
   * @throws IOException  if trouble executing remote command
   * @throws SshException
   * @throws ChannelOpenException
   */
  private SshSession sendCommand(
    SshClient ssh, String exec, String[] args, String remoteDir
    ) throws IOException, SshException, ChannelOpenException {

    String command = "";
    if ( remoteDir != null ) {
      command += "cd " + remoteDir + ";";
    }
    command += exec + " ";
    for ( int i = 0; i < args.length; i++ ) {
      command += "\"" + args[i] + "\" ";
    }
    SshSession session = ssh.openSessionChannel();
    logger.info( "Sending SSH command to " + sshServer + ": " + command );
    session.executeCommand( command );
    return session;
  }
}
