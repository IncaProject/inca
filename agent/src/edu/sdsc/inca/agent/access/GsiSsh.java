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

import edu.sdsc.inca.Agent;
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


  private final String resourceName;


  // constructors


  public GsiSsh(String resource, ResourcesWrapper resources) throws ConfigurationException
  {
    sshServer = resources.getValue(resource, Protocol.COMPUTE_SERVER_MACRO);

    if (sshServer == null || sshServer.isEmpty())
      sshServer = resource;

    String port = resources.getValue(resource, Protocol.COMPUTE_PORT_MACRO);

    if (port != null)
      sshPort = Integer.parseInt(port);

    resourceName = resource;

    try {
      renewCredential(resourceName);
    }
    catch (IOException | OperatorCreationException | GeneralSecurityException err) {
      logger.error("Error retrieving GSS credential: ", err);

      throw new ConfigurationException(err);
    }
  }


  // protected methods


  @Override
  protected SshClient connect(boolean noDelay) throws IOException, SshException, OperatorCreationException, GeneralSecurityException, ConfigurationException
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
      if (!renewCredential(resourceName))
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


  private static synchronized boolean renewCredential(String resource) throws IOException, OperatorCreationException, GeneralSecurityException, ConfigurationException
  {
    if (MyProxy.checkCredential())
      return false;

    ResourcesWrapper resources = Agent.getGlobalAgent().getResources();

    String myProxyHost = resources.getValue(resource, Protocol.MYPROXY_HOST_MACRO);

    if (myProxyHost == null)
      throw new ConfigurationException("Missing macro " + Protocol.MYPROXY_HOST_MACRO);

    String port = resources.getValue(resource, Protocol.MYPROXY_PORT_MACRO);
    int myProxyPort;

    if (port != null)
      myProxyPort = Integer.parseInt(port);
    else
      myProxyPort = MyProxy.DEFAULT_PORT;

    String myProxyUsername = resources.getValue(resource, Protocol.MYPROXY_USERNAME_MACRO);

    if (myProxyUsername == null)
      myProxyUsername = System.getProperty("user.name");

    String myProxyPassword = resources.getValue(resource, Protocol.MYPROXY_PASSWORD_MACRO);

    if (myProxyPassword == null)
      throw new ConfigurationException("Missing macro " + Protocol.MYPROXY_PASSWORD_MACRO);

    String lifetime = resources.getValue(resource, Protocol.MYPROXY_LIFETIME_MACRO);
    int myProxyLifetime;

    if (lifetime != null)
      myProxyLifetime = Integer.parseInt(lifetime);
    else
      myProxyLifetime = MyProxy.DEFAULT_LIFETIME;

    MyProxy myProxyServer = new MyProxy(myProxyHost, myProxyPort, myProxyUsername, myProxyPassword);

    myProxyServer.writeCredential(myProxyLifetime);

    return true;
  }
}
