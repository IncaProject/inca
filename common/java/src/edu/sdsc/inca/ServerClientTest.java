package edu.sdsc.inca;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.util.ConfigProperties;
import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

/**
 * @author Jim Hayes
 */
public class ServerClientTest extends TestCase {

  public static final String PING_DATA = "TEST";
  public static final Statement PING_STATEMENT =
    new Statement(Protocol.PING_COMMAND, PING_DATA);
  public static final Statement PING_REPLY =
    Statement.getOkStatement(new String(PING_STATEMENT.getData()));
  public static final String SERVER_LOGFILE_PATH = "/tmp/sctestlog.txt";

  protected static Logger logger = Logger.getLogger(ServerClientTest.class);

  /**
   * Test a start message with no content.
   */
  public void testStartNoError() {
    Client client = null;
    String failure = null;
    Server server = null;
    try {
      server = startServer();
      client = connectClient("localhost", server.getPort());
      client.write(Statement.getStartStatement());
      client.read();
    } catch (Exception e) {
      failure = "Unexpected exception " + e;
    }
    stopServer(server);
    if(failure != null) {
      fail(failure);
    }
  }

  /**
   * Make sure that a ping statement works.
   */
  public void testPingStatement() {
    Client client = null;
    String failure = null;
    Server server = null;
    try {
      server = startServer();
      client = connectClient("localhost", server.getPort());
      client.write(PING_STATEMENT);
      Statement reply = client.read();
      if(reply == null) {
        failure = "Null reply from PING";
      } else if(!reply.equals(PING_REPLY)) {
        failure = "Unexpected reply from PING '" + reply + "'";
      }
    } catch (Exception e) {
      failure = "Unexpected exception " + e;
    }
    stopServer(server);
    if(failure != null) {
      fail(failure);
    }
  }

  /**
   * Make sure the Client ping command works.
   */
  public void testPingCommand() {
    Client client = null;
    String failure = null;
    Server server = null;
    try {
      server = startServer();
      client = connectClient("localhost", server.getPort());
      String reply = client.commandPing(PING_DATA);
      if(reply == null) {
        failure = "Null reply from PING";
      } else if(!reply.equals(PING_DATA)) {
        failure = "Unexpected reply from PING '" + reply + "'";
      }
    } catch (Exception e) {
      failure = "Unexpected exception " + e;
    }
    stopServer(server);
    if(failure != null) {
      fail(failure);
    }
  }

  /**
   * Make sure the Client getLog and logConfig commands work.
   */
  public void testLogCommands() {
    Client client = null;
    String failure = null;
    Server server = null;
    try {
      server = startServer();
      client = connectClient("localhost", server.getPort());
      String log1 = client.commandGetLog();
      if(log1 == null) {
        throw new Exception("Null returned from GETLOG command");
      }
      client.commandLogConfig("log4j.logger.edu.sdsc.inca=debug");
      String log2 = client.commandGetLog();
      if(log2 == null) {
        throw new Exception("Null returned from GETLOG command");
      }
      if(log2.length() <= log1.length()) {
        failure = "No new text in second getlog output";
      } else if(log2.lastIndexOf("DEBUG") <= log1.length()) {
        failure = "No new debug text in second getlog output";
      }
    } catch (Exception e) {
      failure = "Unexpected exception " + e;
    }
    stopServer(server);
    if(failure != null) {
      fail(failure);
    }
  }

  /**
   * Make sure we can resolve localhost
   */
  public void testLocalhostResolution() {
    Client client = new Client();
    client.setHostname( "localhost" );
    assertFalse( "no localhost resolution",
                 client.getHostname().equals("localhost") );
  }

  private Client connectClient(String server, int port)
    throws ConfigurationException, IOException {
    Client result = new Client();
    ConfigProperties config = new ConfigProperties();
    config.putAllTrimmed(System.getProperties(), "inca.component.");
    config.loadFromResource("inca.properties", "inca.component.");
    result.setConfiguration(config);
    result.setServer(server, port);
    result.connect();
    return result;
  }

  private Server startServer() throws Exception {
    Server result = new Server();
    ConfigProperties config = new ConfigProperties();
    config.putAllTrimmed(System.getProperties(), "inca.component.");
    try {
      config.loadFromResource("inca.properties", "inca.component.");
    } catch(IOException e) {
      throw new ConfigurationException(e.toString());
    }
    config.setProperty("logfile", SERVER_LOGFILE_PATH);
    result.setConfiguration(config);
    result.runServer();
    return result;
  }

  private void stopServer(Server s) {
    if(s == null) {
      return;
    }
    try {
      s.shutdown();
    } catch(InterruptedException e) {
      // empty
    }
    try {
      new File(SERVER_LOGFILE_PATH).delete();
    } catch(Exception e) {
      // empty
    }
  }

}
