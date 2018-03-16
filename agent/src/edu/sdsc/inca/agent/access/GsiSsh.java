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

  // constructors


  public GsiSsh(String resource, ResourcesWrapper resources) throws ConfigurationException
  {
    sshServer = resources.getValue(resource, Protocol.COMPUTE_SERVER_MACRO);

    if (sshServer == null || sshServer.isEmpty())
      sshServer = resource;

    String port = resources.getValue(resource, Protocol.COMPUTE_PORT_MACRO);

    if (port != null)
      sshPort = Integer.parseInt(port);

    try {
      renewCredential();
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
      if (!renewCredential())
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


  private static synchronized boolean renewCredential() throws IOException, OperatorCreationException, GeneralSecurityException
  {
    if (MyProxy.checkCredential())
      return false;

    Agent agent = Agent.getGlobalAgent();
    MyProxy myProxyServer = new MyProxy(agent.getMyProxyHost(), agent.getMyProxyPort(), agent.getMyProxyUsername(), agent.getMyProxyPassword());

    myProxyServer.writeCredential(MyProxy.DEFAULT_LIFETIME);

    return true;
  }
}
