package edu.sdsc.inca;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.log4j.Logger;

import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class ManagerClient extends Client {
  private static final int MAX_TIMEOUT_TRIES = 60; // 5 mins
  private static final byte[] LF = "\n".getBytes();
  private static Logger logger = Logger.getLogger( ManagerClient.class );
  public String resourceName = "manager";

  /**
   * Send a ping to the server and return the response data.  Differs from
   * the Component's commandPing by doing a readSocketWithSocketTimeout().
   *
   * @param data the ping message data
   * @return the ping response data
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  @Override
  public String commandPing(String data) throws IOException, ProtocolException {
   logger.info( "Sending ping '" + data + "' to " + resourceName);
    if ( this.writer == null ) {
      throw new IOException( "Connection to reporter manager is lost" );
    }
    this.writer.write( new Statement( Protocol.PING_COMMAND, data ) );
    Statement statement = this.readStatementWithSocketTimeout();
    if ( statement == null ) {
      throw new IOException
        ( "Lost connection to reporter manager " + resourceName );
    }
    String response = new String( statement.getCmd() );
    String responseData = new String( statement.getData() );
    if ( response.equals(Protocol.FAILURE_COMMAND) ) {
      throw new ProtocolException
        ("Received error from reporter manager " + resourceName +
          " during ping: " + responseData );
    } else if ( ! response.equals(Protocol.SUCCESS_COMMAND) ) {
      throw new ProtocolException
        ( "Unexpected response '" + statement.toString() +
          "' from reporter manager " + resourceName + " during ping" );
    } else if ( ! responseData.equals(data)) {
      throw new ProtocolException
        ( "Ping response from " + resourceName + " expected '" + data +
          "'; received '" + responseData + "'");
    }
    return responseData;
  }


  /**
   * Return the reader connected to the remote reporter manager.
   *
   * @return The reader for the manager client.
   */
  public ProtocolReader getReader() {
    return this.reader;
  }

  /**
   * Return the name of the resource this manager client is attached to.
   *
   * @return The name of the resource the remote reporter manager is hosted on.
   */
  public String getResourceName() {
    return resourceName;
  }

  /**
   * Return the writer connected to the remote reporter manager.
   *
   * @return The writer for the manager client.
   */
  public ProtocolWriter getWriter() {
    return this.writer;
  }

  /**
   * Send the provided package information and content to the manager.
   *
   * @param filename       The name of the file to store this package as.
   *
   * @param installPath    The path relative to the package cache directory to
   *                       store this package at.
   *
   * @param name           The repository name for this package.
   *
   * @param packageContent The package content
   *
   * @param permissions    The permissions to set on the package once installed
   *                       or null to use default permissions.
   *
   * @param dependencies   The dependencies (space delimited) on this package or
   *                       null if no dependencies.
   *
   * @param version        The version of this package.
   *
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public void sendPackage(
    String filename, String installPath, String name, byte[] packageContent,
    String permissions, String dependencies, String version )
    throws IOException, ProtocolException {

    // send package to RM
    this.writer.write( new Statement(Protocol.PACKAGE_COMMAND, name) );
    this.writer.write(
      new Statement(Protocol.PACKAGE_FILENAME_COMMAND, filename )
    );
    this.writer.write(
      new Statement(Protocol.PACKAGE_VERSION_COMMAND, version)
    );
    this.writer.write(
      new Statement(Protocol.PACKAGE_INSTALLPATH_COMMAND, installPath)
    );
    if ( permissions != null ) {
      this.writer.write(
        new Statement(Protocol.PACKAGE_PERMISSIONS_COMMAND, permissions)
      );
    }
    if ( dependencies != null ) {
      this.writer.write(
        new Statement(Protocol.PACKAGE_DEPENDENCIES_COMMAND, dependencies)
      );
    }
    String packageText;
    if ( filename.endsWith(".tar.gz") ) {
      // we encode a tar.gz before sending because we send data as character
      // stream (not as bytes)
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      OutputStream encoder = new Base64OutputStream(result, true, 76, LF);

      try {
        encoder.write(packageContent);
      }
      finally {
        encoder.close();
      }

      packageText = result.toString();
    } else {
      packageText = new String( packageContent );
    }
    this.writer.write(
      new Statement(Protocol.PACKAGE_CONTENT_COMMAND, packageText)
    );
    Statement statement = this.readStatementWithSocketTimeout();
    if ( statement == null ) {
      throw new IOException
        ( "Lost connection to reporter manager " + resourceName );
    }
    String manager_response = new String( statement.getCmd() );
    if ( manager_response.equals(Protocol.FAILURE_COMMAND) ) {
      throw new ProtocolException(
        "Received error from reporter manager " + resourceName + ": " +
        new String(statement.getData()) + "'"
      );
    } else if ( ! manager_response.equals(Protocol.SUCCESS_COMMAND) ) {
      throw new ProtocolException(
        "Unexpected response '" + statement.toString() +
        "' from reporter manager " + resourceName
      );
    }
  }

  /**
   * Send the provided proxy renewal information to the manager.
   *
   * @param dn       The DN of the MyProxy server (use null if standard DN)
   *
   * @param hostname The host where the MyProxy server is running on
   *
   * @param lifetime The lifetime of the new proxy credential
   *
   * @param password The password to authenticate to the MyProxy server
   *                 [default: 24 hours]
   * @param port     The port where the MyProxy server is running on
   *                 [default: 7512]
   * @param username The username the proxy credential is stored under
   *
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public void sendProxyRenewInfo( String dn, String hostname, int lifetime,
                                     String password, int port,
                                     String username )
    throws IOException, ProtocolException {

    writer.write( new Statement(Protocol.PROXY_HOSTNAME_COMMAND, hostname) );
    if ( dn != null ) {
      writer.write( new Statement(Protocol.PROXY_DN_COMMAND, dn) );
    }
    writer.write( new Statement(
      Protocol.PROXY_PORT_COMMAND, Integer.toString(port)
    ) );
    writer.write( new Statement(Protocol.PROXY_USERNAME_COMMAND, username) );
    writer.write( new Statement(Protocol.PROXY_PASSWORD_COMMAND, password) );
    writer.write( new Statement(
      Protocol.PROXY_LIFETIME_COMMAND, Integer.toString(lifetime)
    ) );

    Statement ack = this.readStatementWithSocketTimeout();
    if ( ack == null ) {
      throw new IOException
        ( "Lost connection to reporter manager " + resourceName );
    }
    String response = new String( ack.getCmd() );
    if ( response.equals("OK") ) {
      logger.info( "Proxy renewal info successfully sent to " + resourceName );
    } else {
      throw new ProtocolException
        ( "Problem sending proxy renewal info to " + resourceName + ": " +
          new String(ack.getData()) );
    }
  }

  /**
   * Send the specified suite document to the remote reporter manager.
   *
   * @param resource The name of the resource the suite is being sent to.
   * @param suite      A suite to send to the reporter manager.
   *
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public void sendSuite( String resource, SuiteDocument suite )
    throws IOException, ProtocolException {

    logger.info
      ( "Sending suite '" + suite.getSuite().getName() + "' to " + resource );
    this.writer.write(
      new Statement( Protocol.SUITE_UPDATE_COMMAND, suite.toString() )
    );
    Statement statement = this.readStatementWithSocketTimeout();
    if ( statement == null ) {
      throw new IOException
        ( "Lost connection to reporter manager " + resourceName );
    }
    String manager_response = new String( statement.getCmd() );
    if ( manager_response.equals(Protocol.FAILURE_COMMAND) ) {
      throw new ProtocolException
        ( "Received error from reporter manager " + resourceName +
          " during send of suite: " + new String(statement.getData()) + "'" );
    } else if ( ! manager_response.equals(Protocol.SUCCESS_COMMAND) ) {
      throw new ProtocolException
        ( "Unexpected response '" + statement.toString() +
          "' from reporter manager " + resourceName + " during send of suite" );
    }
  }

  /**
   * Set the reader connected to the remote reporter manager.
   *
   * @param reader  The reader for the manager client
   */
  public void setReader( ProtocolReader reader ) {
    this.reader = reader;
  }


  /**
   * Set the name of the resource the remote reporter manager is hosted on.
   *
   * @param resourceName The name of the remote reporter manager.
   */
  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  /**
   * Set the writer connected to the remote reporter manager.
   *
   * @param writer  The reader for the manager client
   */
  public void setWriter( ProtocolWriter writer ) {
    this.writer = writer;
  }

  /**
   * Read a statement from the reporter manager but allow for up to 20
   * socket timeouts (i.e., 60 seconds) in case the reporter manager is just
   * slow to respond.  Will throw ProtocolException if more than 20 socket
   * timeouts are received.
   *
   * @return The successfully read statement from reporter manager.
   *
   * @throws IOException if cannot get read statement
   * @throws ProtocolException if unexpected format in response
   */
  private Statement readStatementWithSocketTimeout()
    throws IOException, ProtocolException {

    Statement response = null;
    int i = 0;
    for ( ; i < MAX_TIMEOUT_TRIES; i++ ) {
      try {
        if ( reader == null ) {
          throw new IOException( "Connection to reporter manager is lost" );
        }
        response = reader.readStatement();
        return response;
      } catch ( SocketTimeoutException e ) {
        // do nothing (i.e., try read again)
        logger.debug
          ( "Received socket timeout from manager " + resourceName +
            " during read: " + i );
      }
    }
    if ( i >= MAX_TIMEOUT_TRIES ) {
      throw new IOException
        ( "Giving up read statement to manager " + resourceName +
          " after " + MAX_TIMEOUT_TRIES + " tries" );
    }
    return response;
  }
}
