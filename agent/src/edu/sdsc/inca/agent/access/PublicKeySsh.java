package edu.sdsc.inca.agent.access;


import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;

import com.sshtools.net.SocketTransport;
import com.sshtools.publickey.SshPrivateKeyFile;
import com.sshtools.publickey.SshPrivateKeyFileFactory;
import com.sshtools.ssh.HostKeyVerification;
import com.sshtools.ssh.PublicKeyAuthentication;
import com.sshtools.ssh.SshAuthentication;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshConnector;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.components.SshKeyPair;
import com.sshtools.ssh.components.SshPublicKey;
import com.sshtools.ssh2.Ssh2Context;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.ResourcesWrapper;


/**
 * A class that implements AccessMethod using the SSH protocol for transferring
 * files and running processes on remote resources.
 *
 * Important note: stop() does not kill the remote process currently
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class PublicKeySsh extends Ssh {
  // Constants
  public static final int CHECK_PERIOD = 5000;
  public static final String[] SSH_IDENTITY_FILENAMES = {
      ".ssh/id_rsa", ".ssh/id_dsa"
  };

  // Member variables
  private String sshUsername = null;
  private String[] sshKeyFiles;
  private String sshPassword = null;

  // Public Methods

  /**
   * Create a new remote process controlling it via SSH. The given
   * resource should exist in the resource configuration file and  the
   * following fields can be optionally defined:
   *
   * sshServer - the remote hostname [default: resource name]
   * sshPort - alternative port of SSH server [default: SSHClient's default]
   * sshUsername - username on remote machine [default: user.home from Java]
   * sshPrivateKeyFile - identity to use to authenticate
   *                     [default: ~/.ssh/id_dsa, ~/.ssh/id_rsa]
   *
   * @param resource  The name of the resource to start the process on
   * @param resources The resource configuration information.
   *
   * @throws ConfigurationException  if problem configuring ssh connection
   */
  public PublicKeySsh( String resource, ResourcesWrapper resources )
    throws ConfigurationException {

    sshServer = resources.getValue( resource, Protocol.COMPUTE_SERVER_MACRO );
    logger.debug( "sshServer is '" + sshServer + "'" );
    if ( sshServer == null || sshServer.equals("") ) {
      sshServer = resource;
    }
    logger.debug( "sshServer is '" + sshServer + "'" );

    String sshPortString =
      resources.getValue( resource, Protocol.COMPUTE_PORT_MACRO );
    if ( sshPortString != null ) {
      sshPort = Integer.parseInt( sshPortString );
    }

    sshUsername = resources.getValue( resource, Protocol.LOGIN_ID_MACRO );
    if ( sshUsername == null ) {
      sshUsername = System.getProperty( "user.name" );
    }

    String sshIdentity = resources.getValue(resource, Protocol.SSH_IDENTITY_MACRO );
    if ( sshIdentity != null ) {
      sshKeyFiles = new String[]{ sshIdentity };
    } else {
      String userhome = System.getProperty( "user.home" );
      sshKeyFiles = new String[SSH_IDENTITY_FILENAMES.length];
      for ( int i = 0; i < SSH_IDENTITY_FILENAMES.length; i++ ) {
        sshKeyFiles[i] = userhome + File.separator + SSH_IDENTITY_FILENAMES[i];
      }
    }

    sshPassword = resources.getValue(resource, Protocol.SSH_PASSWORD_MACRO );

  }

  // Protected Methods

  /**
   * Connect and authenticate to the remote resource.
   *
   * @param noDelay
   * @return An active SshClient connection to the remote resource.
   *
   * @throws IOException  if trouble connecting to ssh server
   * @throws SshException
   */
  @Override
  protected SshClient connect(boolean noDelay) throws IOException, SshException {

    SshConnector conn = SshConnector.createInstance();
    Ssh2Context context = conn.getContext();

    context.setPreferredKeyExchange(Ssh2Context.KEX_DIFFIE_HELLMAN_ECDH_NISTP_256);
    context.setHostKeyVerification(new HostKeyVerification() {
      @Override
      public boolean verifyHost(String host, SshPublicKey key)
      {
        return true;
      }
    });

    SocketTransport transport = new SocketTransport(sshServer, sshPort);

    if (noDelay)
      transport.setTcpNoDelay(true);

    logger.debug( "Connecting to ssh server " + sshServer + ":" + sshPort + " using username " + sshUsername );

    SshClient ssh = conn.connect(transport, sshUsername, true);

    PublicKeyAuthentication pk = new PublicKeyAuthentication();

    for ( String fileName : sshKeyFiles ) {
      try {
        logger.debug( "Trying ssh key file " + fileName );

        SshPrivateKeyFile keyFile = SshPrivateKeyFileFactory.parse(new FileInputStream(fileName));
        SshKeyPair pair = keyFile.toKeyPair( sshPassword );

        pk.setPrivateKey(pair.getPrivateKey());
        pk.setPublicKey(pair.getPublicKey());

        if ( ssh.authenticate(pk) == SshAuthentication.COMPLETE ) {
          logger.debug( "Authentication succeeded" );
          break;
        }
      } catch ( Exception e ) {
        logger.debug( "Ssh key file " + fileName + " failed" );
        // do nothing and hope a subsequent file works
      }
    }

    if ( !ssh.isAuthenticated() ) {
      logger.error( "Unable to create ssh connection to " + sshServer );
      return null;
    } else {
      return ssh;
    }
  }
}
