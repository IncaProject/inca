package edu.sdsc.inca;

import org.apache.log4j.Logger;
import edu.sdsc.inca.util.ConfigProperties;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.xml.XmlConfiguration;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Starts up a Jetty server to server our Inca webapp.  Reads configuration
 * params from standard properties, waits for webapp to start, and then adds
 * the configuration params to the webapp.  This allows the password to be
 * passed via stdin.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class Consumer extends Component {
  final static public String AGENT_BEAN_ID = "agentBean";
  final static public String CONFIG_ID = "config";
  final static public String DEPOT_BEAN_ID = "depotBean";
  final static private String JETTY_CONFIG = "jetty.xml";
  final static private String JETTY_TEMPDIR = "work";

  private static Logger logger = Logger.getLogger(Consumer.class);

  // Configuration options
  public static final String CONSUMER_OPTS = ConfigProperties.mergeValidOptions(
    Component.COMPONENT_OPTS,
      "a|agent       str  URI to the Inca agent\n" +
      "d|depot       str  URI to the Inca depot\n" +
      "m|maxWait     int  Max wait time a JSP tag should wait on a cached item\n"+
      "r|reload      int  Reload period for cached objects (e.g., suites)\n" +
      "R|reloadTime  str  Reload time for cached histories\n" +
      "v|var         path Path to temporary directory\n",
    true
  );

  private Properties clientConfig = null;
  private org.mortbay.jetty.Server server = null;
  private String tempPath = null;

  /**
   * Returns configuration properties for clients (e.g., agent and depot).
   *
   * @return a list of configuration properties for clients
   */
  public Properties getClientConfiguration() {
    return clientConfig;
  }

  /**
   * Returns the path where the server stores temporary files.
   *
   * @return the temp directory path
   */
  public String getTempPath() {
    return this.tempPath;
  }


  /**
   * Determine if Jetty server is running.
   *
   * @return  True if Jetty server is running and false otherwise.
   */
  public synchronized boolean isRunning() {
    return this.server != null && this.server.isStarted();
  }


  /**
   * Sets the configuration properties for clients (e.g., agent and depot).
   *
   * @param props   a list of configuration properties for clients
   */
  public void setClientConfiguration( Properties props ) {
    logger.debug( "Setting client configuration" );
    clientConfig = props;
  }

  /**
   * Read in the configuration for the consumer from the specified properties
   * list.
   *
   * @param config  A list of configuration options for the consumer
   *
   * @throws ConfigurationException
   */
  public void setConfiguration(Properties config) throws ConfigurationException{
    super.setConfiguration( config );
    String prop;

    // check that agent and depot are specified and save the configuratioin
    if( config.getProperty("agent") == null ) {
      throw new ConfigurationException( "No agent URI specified" );
    }
    if( config.getProperty("depot") == null ) {
      throw new ConfigurationException( "No depot URI specified" );
    }
    config.remove( "logfile" ); // so log does not get initialized in client
    this.setClientConfiguration( config );

    if((prop = config.getProperty("var")) != null) {
      this.setTempPath(prop);
    }

  }

  /**
   * Sets the directory path where the Server stores temporary files.
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
    File jettyTempPath =
      new File( this.tempPath + File.separator + JETTY_TEMPDIR );
    if ( !jettyTempPath.exists() && !jettyTempPath.mkdir() ) {
      throw new ConfigurationException
        ("Unable to create jetty temporary dir '" + jettyTempPath + "'");
    }
    logger.info("Placing jetty webapps in " + jettyTempPath );
    System.setProperty( "jetty.home", this.tempPath );
  }

  public synchronized void shutdown() {
    if ( server.isStarted() ) {
      try {
        server.stop();
      } catch ( Exception e ) {
        logger.error( "Shutdown of Jetty interrupted", e );
      }
    }
    logger.info( "Shutdown of consumer complete" );
  }

  /**
   * Runs a Jetty server.
   *
   * @throws Exception if unable to start up Jetty server and consumer threads
   */
  public void startConsumer() throws Exception  {

    // get jetty config file
    URL jettyUrl = ClassLoader.getSystemClassLoader().getResource(JETTY_CONFIG);
    if(jettyUrl == null) {
      logger.error( JETTY_CONFIG + " not found in classpath" );
      return;
    }

    // configure and start jetty
    XmlConfiguration jettyConfig =  new XmlConfiguration( jettyUrl );
    this.server = new org.mortbay.jetty.Server();
    jettyConfig.configure(server);
    server.start();

    // find inca context and set the client configuration
    WebAppContext incaContext = findIncaContext();
    if ( incaContext == null ) {
      throw new IOException( "Cannot find inca webapp" );
    }
    incaContext.setAttribute(Consumer.CONFIG_ID, this.getClientConfiguration());

    // print out some info to the logs
    for ( Connector c : this.server.getConnectors() ) {
      logger.info( "Starting Jetty server: " +  c.getName() );
    }

    // For cewolf generated graphs
    System.setProperty( "java.awt.headless", "true" );
  }


  private WebAppContext findIncaContext() {
    WebAppContext incaContext = null;
    for ( Handler h : server.getHandlers() ) {
      if ( h.getClass().equals(ContextHandlerCollection.class) ) {
        ContextHandlerCollection context = (ContextHandlerCollection)h;
        for ( Handler c : context.getChildHandlers() ) {
          if ( c.getClass().equals(WebAppContext.class) ) {
            WebAppContext webapp = (WebAppContext)c;
            logger.info( "Context " + webapp.getContextPath() );
            if ( webapp.getContextPath().equals("/inca") ) {
              incaContext = webapp;
              break;
            }
          }
        }
      }
    }
    return incaContext;
  }

  /**
   * Start up the consumer server.
   *
   * @param args  array of command line arguments to configure the consumer.
   */
  public static void main(final String[] args) {

    Consumer consumer = new Consumer();
    try {
      configComponent(
        consumer, args, CONSUMER_OPTS, "inca.consumer.", "edu.sdsc.inca.Consumer",
        "inca-consumers-version"
      );
      consumer.startConsumer();
      while(consumer.isRunning()) {
        Thread.sleep(2000);
      }
    } catch(Exception e) {
      logger.fatal("Configuration error: ", e);
      System.err.println("Configuration error: " + e);
      System.exit(1);
    }
  }
}
