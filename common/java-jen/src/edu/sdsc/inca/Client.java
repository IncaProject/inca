package edu.sdsc.inca;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.util.ConfigProperties;
import edu.sdsc.inca.util.StringMethods;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * This class implements the functionality common to all Inca client classes.
 * Along with defining an API that understands the Server protocol, it defines
 * a main method that allows access to a Server via a telnet-style interface.
 */
public class Client extends Component {

  // Command-line options--see main method
  public static final String CLIENT_OPTS =
    ConfigProperties.mergeValidOptions(
      COMPONENT_OPTS,
      "  S|server str Server specification; host:port\n",
      true
    );

  // Protected class vars
  protected static Logger logger = Logger.getLogger(Client.class);

  // Other instance variables
  protected Socket socket = null;
  protected ProtocolReader reader = null;
  protected ProtocolWriter writer = null;
  private String tempPath = null;

  /**
   * Close the client socket to the server.  If not called explicitly, this
   * method will be called when the Client instance is destroyed.
   */
  public void close() {
    if(!this.socket.isClosed()) {
      try {
        this.socket.close();
        logger.debug("Closed connection to server");
      } catch(IOException e) {
        logger.error("Unable to close socket", e);
      }
    }
  }

  /**
   * Retrieve the text in the server's log file, if any.
   *
   * @return the contents of the server's log, an empty string if none
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String commandGetLog() throws IOException, ProtocolException {
    return this.dialog(Protocol.GET_LOG_COMMAND, "");
  }

  /**
   * Send a log config message to the server.
   *
   * @param data a log4j setting of the form property=value
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public void commandLogConfig(String data)
    throws IOException, ProtocolException {
    this.dialog(Protocol.LOG_CONFIG_COMMAND, data);
  }

  /**
   * Send a ping to the server and return the response data.
   *
   * @param data the ping message data
   * @return the ping response data
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String commandPing(String data) throws IOException, ProtocolException {
    String result = this.dialog(Protocol.PING_COMMAND, data);
    if(!result.equals(data)) {
      throw new ProtocolException("Unexpected reply '" + result + "'");
    }
    return result;
  }

  /**
   * Send a permit command to the server.
   *
   * @param dn the DN of the entity allowed to perform an action
   * @param action the action the entity is allowed to perform
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public void commandPermit(String dn, String action)
    throws IOException, ProtocolException {
    char quote = dn.indexOf('"') < 0 ? '"' : '\'';
    this.dialog(Protocol.PERMIT_COMMAND, quote + dn + quote + " " + action);
  }

  /**
   * Establish a connection to the server.
   *
   * @throws ConfigurationException if the configuration settings are invalid
   * @throws IOException if the attempt to contact the server fails
   */
  public void connect() throws ConfigurationException, IOException {
    logger.debug("Connecting to " + this.getUri());
    this.socket =
      (Socket)super.createSocket(false, this.getHostname(), this.getPort());
    this.reader =
      new ProtocolReader(new InputStreamReader(this.socket.getInputStream()));
    this.writer =
      new ProtocolWriter(new OutputStreamWriter(this.socket.getOutputStream()));
    // To detect an authentication mismatch (authenticating server/plaintext
    // client or vice-versa) we need to send a message--this will cause the SSL
    // layer to initiate a handshake and throw an exception on mismatch.
    try {
      this.commandPing("TEST");
    } catch(Exception e) {
      try {
        this.socket.close();
      } catch(IOException e2) {
        // empty
      }
      logger.error
        ("Client communication failed; suspect authorization mismatch:" + e);
      throw new ConfigurationException
        ("Client communication failed; suspect authorization mismatch:" + e);
    }
  }

  /**
   * Returns the DN associated with this connection.
   *
   * @param peer indicates whether the local or peer DN is desired
   * @return the desired DN; null if this connection is not secure
   */
  public String getDn(boolean peer) {
    return Component.getDn(this.socket, peer);
  }

  /**
   * Returns the path where the Client stores temporary files.
   *
   * @return the temp directory path
   */
  public String getTempPath() {
    return this.tempPath;
  }

  /**
   * Query whether the client is presently connected.
   *
   * @return true iff connected
   */
  public boolean isConnected() {
    if (this.socket == null)
      return false;

    return this.socket.isConnected();
  }

  /**
   * Read the next statement from the server.
   *
   * @return the next statement
   * @throws IOException on read error
   * @throws ProtocolException on an invalid message
   */
  public Statement read() throws IOException, ProtocolException {
    return this.reader.readStatement();
  }

  /**
   * A convenience function for setting multiple configuration properties at
   * once.  In addition to the keys recognized by the superclass, recognizes:
   * "server", the specification of the server.
   *
   * @param config contains client configuration values
   * @throws ConfigurationException on a faulty configuration property value
   */
  public void setConfiguration(Properties config) throws ConfigurationException{
    super.setConfiguration(config);
    String prop;
    if((prop = config.getProperty("server")) != null) {
      this.setServer(prop, 0);
    }
    if((prop = config.getProperty("var")) != null) {
      this.setTempPath(prop);
    }
  }

  /**
   * Sets the host and port of the server to contact.
   *
   * @param server contact spec of the form [protocol://]host[:port]
   * @param defaultPort port to contact if server contains none
   */
  public void setServer(String server, int defaultPort) {
    int colonPos = server.indexOf("://");
    if(colonPos >= 0) {
      this.setAuthenticate
        (server.startsWith("incas") || server.startsWith("https"));
      server = server.substring(colonPos + 3);
    }
    colonPos = server.indexOf(":");
    if(colonPos < 0) {
      this.setHostname(server);
      this.setPort(defaultPort);
    } else {
      this.setHostname(server.substring(0, colonPos));
      this.setPort(Integer.parseInt(server.substring(colonPos + 1)));
    }
  }

  /**
   * Sets the directory path where the Client stores temporary files.
   *
   * @param path the temporary directory path
   * @throws ConfigurationException if the path can't be read from the classpath
   */
  public void setTempPath(String path) throws ConfigurationException {
    File tempPath = new File(path);
    if (!tempPath.exists() && !tempPath.mkdir()) {
      throw new ConfigurationException
        ("Unable to create temporary dir '" + path + "'");
    }
    this.tempPath = tempPath.getAbsolutePath();
  }

  /**
   * Write a statement to the server.
   *
   * @param statement the statement to write
   * @throws IOException on write failure
   */
  public void write(Statement statement) throws IOException {
    writer.write(statement);
  }

  /**
   * Sends a statement to the server and returns the data from a successful
   * response.
   *
   * @param command the command to send
   * @param data the data to accompany the command
   * @return the data from a non-error response
   * @throws IOException on read failure
   * @throws ProtocolException if a failure message is received
   */
  protected String dialog(String command, String data)
    throws IOException, ProtocolException {
    this.write(new Statement(command, data));
    Statement reply = this.read();
    if(reply == null) {
      throw new IOException("Server closed connection");
    } 
    command = new String(reply.getCmd());
    data = new String(reply.getData());
    if(Protocol.FAILURE_COMMAND.equals(command)) {
      throw new ProtocolException(command + " command failed " + data);
    }
    return data;
  }

  /**
   * Allows the user to use a client via a telnet-style interface.  Allows
   * the contents of files to be substituted in commands via '&lt; path' and
   * output of commands to be redirected via '&gt; path'.
   *
   * @param c the client to use
   * @param prompt the user prompt
   */
  public static void telnetDialog(Client c,
                                  String prompt) {
    String command;
    BufferedReader keyboard =
      new BufferedReader(new InputStreamReader(System.in));
    OutputStreamWriter output = new OutputStreamWriter(System.out);
    ArrayList<String> toDo = new ArrayList<String>();
    while(true) {
      while(toDo.size() == 0) {
        command = "";
        while(true) {
          if(prompt != null) {
            System.out.print(prompt);
          }
          try {
            String line = keyboard.readLine();
            if(line == null) {
              return;
            }
            command += line;
          } catch(Exception e) {
            return;
          }
          if(!command.endsWith("\\")) {
            break;
          }
          command += "\n";
        }
        output = new OutputStreamWriter(System.out);
        int pos;
        String path;
        if(command.matches(".*>\\s*[\\w\\.\\-]+$")) {
          pos = command.lastIndexOf(">");
          path = command.substring(pos + 1).trim();
          command = command.substring(0, pos).trim();
          try {
            output = new FileWriter(path);
          } catch(IOException e) {
            System.err.println(e.toString());
            continue;
          }
        }
        if(command.matches(".*<\\s*[\\w\\.\\-]+$")) {
          pos = command.lastIndexOf("<");
          path = command.substring(pos + 1).trim();
          command = command.substring(0, pos);
          try {
            command += StringMethods.fileContents(path);
          } catch(IOException e) {
            System.err.println(e.toString());
            continue;
          }
        }
        command = command.replaceAll("(?m)^\\s*#.*(\n|$)", "");
        command = command.replaceAll("\\\\\n\\s*", "");
        String[] commands = command.split("\n");
        for(int i = 0; i < commands.length; i++) {
          command = commands[i];
          if(!command.matches("^\\s*$")) {
            toDo.add(command);
          }
        }
      }
      command = (String)toDo.remove(0);
      try {
        c.write(new Statement(command));
        while(true) {
          Statement reply = c.read();
          if(reply == null) {
            logger.debug("Connection closed by server");
            System.err.println("Connection closed by server");
            return;
          }
          output.write(reply.toString() + "\n");
          output.flush();
          if(!reply.toString().startsWith("QUERYRESULT")) {
            break;
          }
        }
      } catch(IOException e) {
        logger.error("IOException " + e);
        System.err.println("IOException " + e);
      } catch(ProtocolException e) {
        logger.error("ProtocolException " + e);
        System.err.println("ProtocolException " + e);
        e.printStackTrace();
      }
    }
  }

  /**
   * A simple main method for exercising the Client.
   * <br/>
   * <pre>
   * usage: <code>java inca.Client</code><br/>
   *  -a|--auth     boolean Authenticated (secure) connection?
   *  -c|--cert     path    Path to the authentication certificate
   *  -f|--file     path    Inca installation configuration file path
   *  -h|--help     null    Print help/usage
   *  -i|--init     path    Path to properties file
   *  -k|--key      path    Path to the authentication key
   *  -P|--password str     Specify how to obtain encryption password
   *  -S|--server   str     Server specification; host:port
   *  -t|--trusted  path    Path to authentication trusted certificate dir
   *  -V|--version  null    Display program version
   * </pre>
   * <br/>
   * Each of these properties, other than --init, can also be specified in the
   * initialization file or in system properties defined via the -D switch.  In
   * each of these cases, the configuration name must be prefixed with
   * "inca.client.", e.g., use "inca.client.cert" to specify the cert value.
   * Command line arguments override any properties specified in this fashion.
   * If --file is specified, the main method reads the specified file and sends
   * it to the Server in a setConfig message; otherwise, main allows the user
   * to communicate with the Server via a telnet-style interface.  Supported
   * values for the --password argument are: "no" or "false" (no encryption);
   * "yes", "true", or "stdin:prompt" (read password from stdin after optionally
   * prompting); "pass:text" (specify the password directly--should only be
   * used for testing and, possibly, in the init file).
   * <br/>
   *
   * @param args command line arguments, as above.
   */
  public static void main(String[] args) {
    Client c = new Client();
    try {
      configComponent
        (c, args, CLIENT_OPTS, "inca.client.", "edu.sdsc.inca.Client",
         "inca-common-java-version");
      c.connect();
    } catch(Exception e) {
      logger.fatal("Client error: ", e);
      System.err.println("Client error: " + e);
      System.exit(1);
    }
    telnetDialog(c, "Client> ");
  }

}
