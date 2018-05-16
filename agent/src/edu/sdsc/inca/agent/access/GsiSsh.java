/*
 * GsiSsh.java
 */
package edu.sdsc.inca.agent.access;


import java.io.IOException;
import java.security.GeneralSecurityException;

import org.bouncycastle.operator.OperatorCreationException;

import com.sshtools.net.SocketTransport;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshConnector;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh2.GssApiKeyexAuthentication;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.agent.util.MyProxy;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.ResourcesWrapper;


/**
 * A class that implements AccessMethod using the GSI SSH protocol for
 * transferring files and running processes on remote resources.
 *
 * @author Paul Hoover
 *
 */
public class GsiSsh extends Ssh {

  // data fields


  private final String myProxyUsername;
  private final String myProxyPassword;
  private final String myProxyHost;
  private final int myProxyPort;
  private final int myProxyLifetime;


  // constructors


  public GsiSsh(String resource, ResourcesWrapper resources) throws ConfigurationException
  {
    sshServer = resources.getValue(resource, Protocol.COMPUTE_SERVER_MACRO);

    if (sshServer == null || sshServer.isEmpty())
      sshServer = resource;

    String port = resources.getValue(resource, Protocol.COMPUTE_PORT_MACRO);

    if (port != null)
      sshPort = Integer.parseInt(port);

    myProxyHost = resources.getValue(resource, Protocol.MYPROXY_HOST_MACRO);

    if (myProxyHost == null)
      throw new ConfigurationException("Missing macro " + Protocol.MYPROXY_HOST_MACRO);

    port = resources.getValue(resource, Protocol.MYPROXY_PORT_MACRO);

    if (port != null)
      myProxyPort = Integer.parseInt(port);
    else
      myProxyPort = MyProxy.DEFAULT_PORT;

    String username = resources.getValue(resource, Protocol.MYPROXY_USERNAME_MACRO);

    if (username != null)
      myProxyUsername = username;
    else
      myProxyUsername = System.getProperty("user.name");

    myProxyPassword = resources.getValue(resource, Protocol.MYPROXY_PASSWORD_MACRO);

    if (myProxyPassword == null)
      throw new ConfigurationException("Missing macro " + Protocol.MYPROXY_PASSWORD_MACRO);

    String lifetime = resources.getValue(resource, Protocol.MYPROXY_LIFETIME_MACRO);

    if (lifetime != null)
      myProxyLifetime = Integer.parseInt(lifetime);
    else
      myProxyLifetime = MyProxy.DEFAULT_LIFETIME;

    try {
      renewCredential(myProxyHost, myProxyPort, myProxyUsername, myProxyPassword, myProxyLifetime);
    }
    catch (IOException | OperatorCreationException | GeneralSecurityException err) {
      logger.error("Error retrieving GSS credential: ", err);

      throw new ConfigurationException(err);
    }
  }


  // protected methods


  @Override
  protected SshClient connect(boolean noDelay) throws IOException, SshException, OperatorCreationException, GeneralSecurityException
  {
    SshConnector conn = SshConnector.createInstance();
    SocketTransport transport = new SocketTransport(sshServer, sshPort);

    if (noDelay)
      transport.setTcpNoDelay(true);

    String username = System.getProperty("user.name");
    SshClient client = null;

    try {
      logger.debug("Connecting to GSI SSH server " + sshServer + ":" + sshPort);

      client = conn.connect(transport, username, true);
    }
    catch (SshException sshErr) {
      if (!renewCredential(myProxyHost, myProxyPort, myProxyUsername, myProxyPassword, myProxyLifetime))
        throw sshErr;

      logger.debug("GSS credential was invalid, retrieved new credential");

      client = conn.connect(transport, username, true);
    }

    client.authenticate(new GssApiKeyexAuthentication());

    if (!client.isAuthenticated()) {
      logger.error("Unable to create GSI SSH connection to " + sshServer + ":" + sshPort);

      client.disconnect();

      return null;
    }

    return client;
  }


  // private methods


  private static synchronized boolean renewCredential(String server, int port, String username, String passphrase, int lifetime) throws IOException, OperatorCreationException, GeneralSecurityException
  {
    if (MyProxy.checkCredential())
      return false;

    MyProxy myProxyServer = new MyProxy(server, port, username, passphrase);

    myProxyServer.writeCredential(lifetime);

    return true;
  }
}
