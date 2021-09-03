package edu.sdsc.inca;


import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import edu.sdsc.inca.dataModel.inca.IncaDocument;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.util.ConfigProperties;
import edu.sdsc.inca.util.StringMethods;


/**
 * This class allows access to the services provided by an Inca Agent.  Along
 * with defining an API that understands the Agent protocol, it defines a main
 * method that allows access to an Agent via a telnet-style interface and
 * provides a means for configuring an Inca installation from the command line.
 */
public class AgentClient extends Client {

  // Command-line options--see main method
  public static final String AGENT_CLIENT_OPTS =
    ConfigProperties.mergeValidOptions
      (CLIENT_OPTS.replaceFirst(" *S\\|server[^\\n]*\\n", ""),
       "  A|agent  str  Agent specification; host:port\n" +
       "  f|file  path  Inca installation configuration file path\n",
       true);

  // Protected class vars
  protected static final Logger logger = Logger.getLogger(AgentClient.class);

  // Configuration instance variables
  protected String deployPath = null;


  /**
   * Approve the enclosed series config changes for the specified resource.
   *
   * @param resource The resource the changes are being approved for.
   *
   * @param approved IncaDocument XML specifying changes in the configuration
   *
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public void approve(String resource, IncaDocument approved)
    throws IOException, ProtocolException {

    String doc = resource + " " + approved.xmlText();
    this.dialog(Protocol.APPROVE_COMMAND, doc );
  }

  /**
   * Returns properties for all reporters contained within one repository or
   * all repositories known to the Agent.
   *
   * @param url the url of the repository to query; if null, a merged catalog
   *            from all repositories known to the Agent is returned
   * @return an array of Properties, each of which represents the attributes of
   *         a single repository package
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public Properties[] getCatalog(String url)
    throws IOException, ProtocolException {
    String data =
      this.dialog(Protocol.CATALOG_GET_COMMAND, url == null ? "" : url);
    if(data == null) {
      throw new ProtocolException
        ("No data in reply to " + Protocol.CATALOG_GET_COMMAND);
    }
    if(data.length() == 0) {
      logger.debug("Returning attributes for 0 reporters");
      return new Properties[0];
    }
    // Attribute lines in the data have the form "attribute: value", blank
    // lines indicate the start of a new package, and lines that begin with a
    // space indicate a continuation of the value of the preceding attribute.
    String[] lines = data.split("\n");
    String lastProp = null;
    Vector<Properties> result = new Vector<Properties>();
    Properties p = new Properties();
    for(int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if(line.length() == 0) {
        // Don't add an empty Properties for leading or adjacent blank lines
        if(p.size() > 0) {
          result.add(p);
          p = new Properties();
        }
      } else if(line.charAt(0) != ' ') {
        String[] attrAndValue = line.split(":", 2);
        if(attrAndValue.length == 2) {
          lastProp = attrAndValue[0].trim();
          p.setProperty(lastProp, attrAndValue[1].trim());
        } else {
          logger.warn("Invalid line '" + line + "' in catalog");
        }
      } else if(lastProp != null) {
        p.setProperty
          (lastProp, p.getProperty(lastProp) + "\n" + line.substring(1));
      }
    }
    // The data may or may not end with a blank line
    if(p.size() > 0) {
      result.add(p);
    }
    logger.debug("Returning attributes for " + result.size() + " reporters");
    return result.toArray(new Properties[result.size()]);
  }

  /**
   * Like getCatlog, but returns wraps the catalog information into XML
   * instead of an array of Properties.  The outer tag of the XML is
   * &lt;catalog&gt;, which returns 0 or more &lt;reporter&gt; subtags.  Each
   * of these contains zero or more &lt;property&gt; subtags, which list the
   * property name and value in subtags.
   *
   * @param uri the url of the repository to query; if null, a merged catalog
   *            from all repositories known to the Agent is returned
   * @return XML, described above, that gives the contents of the repository
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String getCatalogAsXml(String uri)
    throws IOException, ProtocolException {
    Properties[] catalog = this.getCatalog(uri);
    StringBuffer result = new StringBuffer();
    result.append("<catalog>\n");
    for(int i = 0; i < catalog.length; i++) {
      Properties p = catalog[i];
      result.append("  <reporter>\n");
      for(Enumeration<?> e = p.propertyNames(); e.hasMoreElements(); ) {
        String name = (String)e.nextElement();
        String value = p.getProperty(name);
        result.append("    <property>\n")
              .append("      <name>").append(name).append("</name>\n")
              .append("      <value>").append(value).append("</value>\n")
              .append("    </property>\n");
      }
      result.append("  </reporter>\n");
    }
    result.append("</catalog>\n");
    return result.toString();
  }

  /**
   * Returns IncaDocument XML for the Inca deployment configuration.
   *
   * @return IncaDocument XML for the deployment configuration
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String getConfig() throws IOException, ProtocolException {
    return this.dialog(Protocol.CONFIG_GET_COMMAND, "");
  }

  /**
   * Returns the path to the IncaDocument XML deployment file.
   *
   * @return the path to the deployment file
   */
  public String getDeployPath() {
    return this.deployPath;
  }

  /**
   * Request all proposed changes to the reporter manager that have not yet
   * been committed to the manager.
   *
   * @param resource The resource name for the manager we are requesting for
   *
   * @return IncaDocument containing all of the proposed changes for the manager
   *
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public IncaDocument getProposedChanges( String resource )
    throws IOException, ProtocolException {

    String result = this.dialog(Protocol.PROPOSED_GET_COMMAND, resource );
    IncaDocument proposed;
    try {
      proposed = IncaDocument.Factory.parse( result, (new XmlOptions()).setLoadStripWhitespace() );
    } catch (XmlException e) {
      throw new ProtocolException( "Unable to parse result: " + e );
    }
    return proposed;
  }

  /**
   * Submits the following run now request to the agent
   *
   * @param suite SuiteDocument XML specifying changes in the configuration
   * @param type A string containing the type of run now request
   *
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public void runNow(String type, String suite) throws IOException, ProtocolException {
    this.dialog(Protocol.RUN_NOW_COMMAND+" "+type, suite);
  }

  /**
   * Replaces or updates the Inca deployment configuration.
   *
   * @param config IncaDocument XML specifying changes in the configuration
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public void setConfig(String config) throws IOException, ProtocolException {
    this.dialog(Protocol.CONFIG_SET_COMMAND, config);
  }

  /**
   * A convenience function for setting multiple configuration properties at
   * once.  In addition to the keys recognized by the superclass, recognizes:
   * "agent", the specification of the Agent; "file", a path to an IncaDocument
   * XML file.
   *
   * @param config contains AgentClient configuration values
   * @throws ConfigurationException on a faulty configuration property value
   */
  @Override
  public void setConfiguration(Properties config) throws ConfigurationException{
    super.setConfiguration(config);
    String prop;
    if((prop = config.getProperty("agent")) != null) {
      this.setServer(prop, 0);
    }
    if((prop = config.getProperty("file")) != null) {
      this.setDeployPath(prop);
    }
  }

  /**
   * Sets the path to the IncaDocument XML deployment file.
   *
   * @param path the path to the deployment file
   */
  public void setDeployPath(String path) {
    this.deployPath = path;
  }

  /**
   * A simple main method for exercising the AgentClient.
   * <br/>
   * <pre>
   * usage: <code>java inca.AgentClient</code><br/>
   *  -a|--auth     boolean Authenticated (secure) connection?
   *  -A|--agent    str     Agent specification; host:port
   *  -c|--cert     path    Path to the authentication certificate
   *  -f|--file     path    Inca installation configuration file path
   *  -h|--help     null    Print help/usage
   *  -i|--init     path    Path to properties file
   *  -k|--key      path    Path to the authentication key
   *  -P|--password str     Specify how to obtain encryption password
   *  -t|--trusted  path    Path to authentication trusted certificate dir
   *  -V|--version  null    Display program version
   * </pre>
   * <br/>
   * Each of these properties, other than --init, can also be specified in the
   * initialization file or in system properties defined via the -D switch.  In
   * each of these cases, the configuration name must be prefixed with
   * "inca.agent.", e.g., use "inca.agent.cert" to specify the cert value.
   * Command line arguments override any properties specified in this fashion.
   * If --file is specified, the main method reads the specified file and sends
   * it to the Agent in a setConfig message; otherwise, main allows the user to
   * communicate with the Agent via a telnet-style interface.  Supported values
   * for the --password argument are: "no" or "false" (no encryption); "yes",
   * "true", or "stdin:prompt" (read password from stdin after optionally
   * prompting); "pass:text" (specify the password directly--should only be
   * used for testing and, possibly, in the init file).
   * <br/>
   *
   * @param args command line arguments, as above.
   */
  public static void main(final String[] args) {
    AgentClient a = new AgentClient();
    try {
      Component.configComponent
        (a,args,AGENT_CLIENT_OPTS,"inca.agent.","edu.sdsc.inca.AgentClient",
         "inca-common-java-version");
      a.connect();
    } catch(Exception e) {
      logger.fatal("Client error: ", e);
      System.err.println("Client error: " + e);
      System.exit(1);
    }
    if ( a.getDeployPath() != null ) {
      try {
        a.setConfig( StringMethods.fileContents( a.getDeployPath() ) );
      } catch ( Exception e ) {
        System.err.println( "Problem deploying configuration" );
        e.printStackTrace();
        System.exit( 1 );
      }
    } else {
      Client.telnetDialog(a, "Agent> ");
    }
  }

}
