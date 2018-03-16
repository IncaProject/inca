package edu.sdsc.inca.agent;

import edu.sdsc.inca.agent.util.MyProxy;
import edu.sdsc.inca.protocol.*;
import edu.sdsc.inca.ManagerClient;
import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.util.Constants;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

/**
 * Class for managing Grid proxy credentials.  This class is used for 2
 * purposes:
 *
 * 1) When using remoteInvocationMethod 'globus', we use the Globus CoG API
 * to transfer files and launch processes on a remote resource.  The CoG API
 * uses Globus services on the remote resource which needs a valid credential
 * to authenticate.  A proxy credential is a limited lifetime credential
 * which allows you to authenticate to Globus services without typing in a
 * password.  Since a proxy has a limited lifetime and we anticipate the
 * Reporter Agent and Reporter Manager to be long living processes, we can
 * either create a long running proxy credential (not so good) or provide
 * the ability to retrieve new proxy credentials from a MyProxy server.   This
 * class stores all the information to retrieve proxy credentials automatically
 * from a MyProxy server.
 *
 * 2) Most likely the Reporter Managers will be executing reporters that need
 * a valid proxy credential as part of their test, benchmark, or whatever.
 * As with above, we need a way to refresh a proxy credential automatically.
 * The information in this class will be passed to a remote reporter manager
 * so that a process can be forked to retrieve new proxy credentials
 * periodically.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class ReporterManagerProxy {
  private String hostname = null;
  private int port = 7512;
  private String username = null;
  private String password = null;
  private int lifetime = 12 * Constants.SECONDS_TO_HOUR;  // seconds
  private String dn = null;

  private Logger logger = Logger.getLogger(this.getClass().toString());

  /**
   * Create a new Reporter Manager proxy object.
   *
   * @param resource   The name of the resource to send proxies to.
   * @param resources  The resource configuration information.
   * @throws ConfigurationException
   */
  public ReporterManagerProxy( String resource, ResourcesWrapper resources )
    throws ConfigurationException{

    String myproxyHost = resources.getValue(resource, Protocol.MYPROXY_HOST_MACRO);
    if ( myproxyHost != null ) {
      setHostname( myproxyHost );
    } else {
      throw new ConfigurationException(
        "Missing macro " + Protocol.MYPROXY_HOST_MACRO
      );
    }
    String myproxyPort = resources.getValue(resource,Protocol.MYPROXY_PORT_MACRO);
    if ( myproxyPort != null ) {
      setPort( Integer.valueOf(myproxyPort).intValue() );
    }
    String myproxyDn = resources.getValue( resource, Protocol.MYPROXY_DN_MACRO );
    setDN( myproxyDn ); // null is okay
    String myproxyUsername = resources.getValue(
      resource, Protocol.MYPROXY_USERNAME_MACRO
    );
    if ( myproxyUsername != null ) {
      setUsername( myproxyUsername );
    } else {
      throw new ConfigurationException(
        "Missing macro " + Protocol.MYPROXY_USERNAME_MACRO
      );
    }
    String myproxyPassword = resources.getValue(
      resource, Protocol.MYPROXY_PASSWORD_MACRO
    );
    if ( myproxyPassword != null ) {
      setPassword( myproxyPassword );
    } else {
      throw new ConfigurationException(
        "Missing macro " + Protocol.MYPROXY_PASSWORD_MACRO
      );
    }
    String myproxyLifetime = resources.getValue(
      resource, Protocol.MYPROXY_LIFETIME_MACRO
    );
    if ( myproxyLifetime != null ) {
      setLifetime( Integer.valueOf(myproxyLifetime).intValue() );
    }
  }

  /**
   * Create a new Reporter Manager proxy object with the following fields
   * already set:
   *
   * @param hostname The host where the MyProxy server is running on
   * @param port     The port where the MyProxy server is running on
   *                 [default: 7512]
   * @param username The username the proxy credential is stored under
   * @param password The password to authenticate to the MyProxy server
   * @param dn       The DN of the MyProxy server (use null if standard DN)
   * @param lifetime The lifetime of the new proxy credential
   *                 [default: 12 hours]
   */
  public ReporterManagerProxy(
    String hostname, int port, String username, String password, String dn,
    int lifetime ){

    if ( hostname != null ) this.setHostname( hostname );
    if ( lifetime > 0 ) this.setLifetime( lifetime );
    if ( password != null ) this.setPassword( password );
    if ( port > 0 ) this.setPort( port );
    if ( username != null ) this.setUsername( username );
    if ( dn != null ) this.setDN( dn );
  }

  /**
   * Decide whether the two proxies are equivalent
   *
   * @param newProxy  A proxy object
   *
   * @return True if the proxies are equivalent and false otherwise.
   */
  public boolean equals( ReporterManagerProxy newProxy ) {
    if ( ! this.hostname.equals(newProxy.hostname) ) return false;
    if ( this.port != newProxy.port ) return false;
    if ( ! this.username.equals(newProxy.username) ) return false;
    if ( ! this.password.equals(newProxy.password) ) return false;
    if ( this.lifetime != newProxy.lifetime ) return false;
    if ( this.dn == null && newProxy.dn != null ) return false;
    if ( this.dn != null && newProxy.dn == null ) return false;
    return this.dn != null && newProxy.dn != null &&
           ! this.dn.equals(newProxy.dn);
  }

  /**
   * Return the subject DN of the MyProxy server.
   *
   * @return the subject DN of the MyProxy server or null if not set
   */
  public String getDN() {
    return dn;
  }

  /**
   * Return the hostname of the MyProxy server.
   *
   * @return the hostname of the MyProxy server or null if not set.
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * Return the lifetime of which we will request proxy credentials from the
   * MyProxy server.
   *
   * @return Lifetime we'll request for proxy certificates.
   */
  public int getLifetime() {
    return lifetime;
  }

  /**
   * The password to use to authenticate to the MyProxy server.
   *
   * @return  Password or null if not set.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Retrieve a new proxy credential from the MyProxy server and return it.
   *
   * @return  New proxy credential
   * @throws GeneralSecurityException
   * @throws IOException
   * @throws OperatorCreationException
   * @throws MyProxyException  if problem retrieving proxy credential
   */
  public String getProxy() throws OperatorCreationException, IOException, GeneralSecurityException {
    logger.info(
      "Retrieving '" + username + "' proxy credential from MyProxy server " +
      hostname + ":" + port + " for " + lifetime + " seconds"
    );
    MyProxy myproxy = new MyProxy( hostname, port, username, password );

    return myproxy.getCredential( lifetime );
  }

  /**
   * Return port of MyProxy server.
   *
   * @return Port of MyProxy server.
   */
  public int getPort() {
    return port;
  }

  /**
   * Return username that the credential is stored under.
   *
   * @return  Username to retrieve proxy credential from MyProxy server.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Send the MyProxy information stored in this object to the specified
   * writer.
   *
   * @param writer  Writer to remote process to send MyProxy information.
   * @param reader  Reader to remote process to read confirmation from.
   *
   * @throws IOException
   * @throws ProtocolException
   */
  public void send( ProtocolWriter writer, ProtocolReader reader )
    throws IOException, ProtocolException {

    ManagerClient mc = new ManagerClient();
    mc.setWriter( writer );
    mc.setReader( reader );
    mc.sendProxyRenewInfo( dn, hostname, lifetime, password, port, username );
  }

  /**
   * Set the subject DN for the MyProxy server if needed (i.e., if Globus
   * will not accept host DN)
   *
   * @param dn  Subject DN of MyProxy server
   */
  public void setDN( String dn ) {
    this.dn = dn;
  }

  /**
   * Set the hostname of the MyProxy server.
   *
   * @param hostname  hostname of the MyProxy server.
   */
  public void setHostname( String hostname ) {
    this.hostname = hostname;
  }

  /**
   * Set the lifetime for new proxy credentials.  Default value is 12 hours.
   *
   * @param lifetime  The period of validity for new proxy credentials in hours.
   */
  public void setLifetime( int lifetime ) {
    this.lifetime = lifetime * Constants.SECONDS_TO_HOUR;
  }

  /**
   * Set the password for retrieving proxy credentials from the MyProxy server.
   *
   * @param password  A password of at least 6 characters.
   */
  public void setPassword( String password ) {
    this.password = password;
  }

  /**
   * Set the port of the MyProxy server if needed.  Default value is 7512.
   *
   * @param port   Port number.
   */
  public void setPort( int port ) {
    this.port = port;
  }

  /**
   * Set the username for retrieving proxy credentials from the MyProxy server.
   *
   * @param username  A username.
   */
  public void setUsername( String username ) {
    this.username = username;
  }
}
