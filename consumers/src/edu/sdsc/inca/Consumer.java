package edu.sdsc.inca;


import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.PropertiesConfigurationManager;
import org.eclipse.jetty.deploy.bindings.DebugListenerBinding;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.AsyncRequestLogWriter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.DebugListener;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnectionStatistics;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.RolloverFileOutputStream;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

import edu.sdsc.inca.util.ConfigProperties;


/**
 * Starts up a Jetty server to server our Inca webapp.  Reads configuration
 * params from standard properties, waits for webapp to start, and then adds
 * the configuration params to the webapp.  This allows the password to be
 * passed via stdin.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class Consumer extends Component {

  @SuppressWarnings("serial")
  private static class JettyProperties extends Properties {

    public Integer getPropertyAsInt(String key, int defaultValue)
    {
      String value = getProperty(key);

      if (value == null)
        return defaultValue;

      return Integer.valueOf(value);
    }
  }


  final static public String AGENT_BEAN_ID = "agentBean";
  final static public String CONFIG_ID = "config";
  final static public String DEPOT_BEAN_ID = "depotBean";
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
  private org.eclipse.jetty.server.Server server = null;
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
  @Override
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

    this.server = createServer();
    this.server.start();

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

  private org.eclipse.jetty.server.Server createServer() throws IOException {

    JettyProperties jettyProps = readJettyProps();
    QueuedThreadPool threadPool = new QueuedThreadPool();

    threadPool.setMinThreads(jettyProps.getPropertyAsInt("jetty.threadpool.min", 10));
    threadPool.setMaxThreads(jettyProps.getPropertyAsInt("jetty.threadpool.max", 250));
    threadPool.setDetailedDump(false);

    org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(threadPool);

    int httpPort = jettyProps.getPropertyAsInt("jetty.port", 8080);
    int sslPort = jettyProps.getPropertyAsInt("jetty.ssl.port", 8443);
    int securePort = jettyProps.getPropertyAsInt("jetty.securePort", sslPort);
    HttpConfiguration httpConfig = new HttpConfiguration();

    httpConfig.setSecureScheme("https");
    httpConfig.setSecurePort(securePort);
    httpConfig.setOutputBufferSize(32768);
    httpConfig.setOutputAggregationSize(8192);
    httpConfig.setRequestHeaderSize(8192);
    httpConfig.setResponseHeaderSize(8192);
    httpConfig.setSendServerVersion(true);
    httpConfig.setSendDateHeader(false);
    httpConfig.setHeaderCacheSize(4096);
    httpConfig.setDelayDispatchUntilContent(true);
    httpConfig.setMaxErrorDispatches(10);
    httpConfig.setPersistentConnectionsEnabled(true);
    httpConfig.setRequestCookieCompliance(CookieCompliance.RFC6265);
    httpConfig.setResponseCookieCompliance(CookieCompliance.RFC6265);
    httpConfig.setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);

    HandlerCollection handlers = new HandlerCollection();
    ContextHandlerCollection contexts = new ContextHandlerCollection();

    handlers.setHandlers(new Handler[] {contexts, new DefaultHandler()});
    server.setHandler(handlers);

    server.setStopAtShutdown(true);
    server.setStopTimeout(5000);
    server.setDumpAfterStart(false);
    server.setDumpBeforeStop(false);

    MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());

    server.addBean(mbContainer);

    ServerConnector httpConn = new ServerConnector(server, new HttpConnectionFactory(httpConfig));

    httpConn.setName("HTTP");
    httpConn.setPort(httpPort);
    httpConn.setIdleTimeout(30000);

    server.addConnector(httpConn);

    SslContextFactory sslContextFactory = new SslContextFactory.Server();

    sslContextFactory.setKeyStorePath(jettyProps.getProperty("jetty.keystore.file"));
    sslContextFactory.setKeyStorePassword(jettyProps.getProperty("jetty.keystore.password"));
    sslContextFactory.setKeyManagerPassword(jettyProps.getProperty("jetty.keystore.manager.password"));
    sslContextFactory.setTrustStorePath(jettyProps.getProperty("jetty.truststore.file"));
    sslContextFactory.setTrustStorePassword(jettyProps.getProperty("jetty.truststore.password"));

    HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);

    httpsConfig.addCustomizer(new SecureRequestCustomizer());

    ServerConnector sslConn = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(httpsConfig));

    sslConn.setName("SSL");
    sslConn.setPort(sslPort);
    server.addConnector(sslConn);

    DeploymentManager deployer = new DeploymentManager();

    if (Log.getLog().isDebugEnabled()) {
      RolloverFileOutputStream debugOut = new RolloverFileOutputStream(getTempPath() + "/jetty.debug.yyyy_mm_dd.log", true, 14);
      DebugListener debug = new DebugListener(debugOut, false, true, true);

      server.addBean(debug);
      deployer.addLifeCycleBinding(new DebugListenerBinding(debug));
    }

    deployer.setContexts(contexts);
    deployer.setContextAttribute(
        "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
        ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/org.apache.taglibs.taglibs-standard-impl-.*\\.jar$"
    );

    WebAppProvider webappProvider = new WebAppProvider();

    webappProvider.setMonitoredDirName("webapps");
    webappProvider.setDefaultsDescriptor("etc/webdefault.xml");
    webappProvider.setScanInterval(1);
    webappProvider.setExtractWars(true);
    webappProvider.setTempDir(new File(getTempPath() + "/work"));
    webappProvider.setConfigurationManager(new PropertiesConfigurationManager());
    deployer.addAppProvider(webappProvider);
    server.addBean(deployer);

    Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(server);

    classlist.addAfter(
        "org.eclipse.jetty.webapp.FragmentConfiguration",
        "org.eclipse.jetty.plus.webapp.EnvConfiguration",
        "org.eclipse.jetty.plus.webapp.PlusConfiguration"
    );
    classlist.addBefore(
        "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
        "org.eclipse.jetty.annotations.AnnotationConfiguration"
    );

    StatisticsHandler stats = new StatisticsHandler();

    stats.setHandler(server.getHandler());
    server.setHandler(stats);
    ServerConnectionStatistics.addToAllConnectors(server);

    RewriteHandler rewrite = new RewriteHandler();

    rewrite.setHandler(server.getHandler());
    server.setHandler(rewrite);

    AsyncRequestLogWriter logWriter = new AsyncRequestLogWriter(getTempPath() + "/jetty.request.yyyy_mm_dd.log");
    CustomRequestLog requestLog = new CustomRequestLog(logWriter, CustomRequestLog.EXTENDED_NCSA_FORMAT + " \"%C\"");

    logWriter.setFilenameDateFormat("yyyy_MM_dd");
    logWriter.setRetainDays(90);
    logWriter.setTimeZone("GMT");
    server.setRequestLog(requestLog);

    LowResourceMonitor lowResourcesMonitor=new LowResourceMonitor(server);

    lowResourcesMonitor.setPeriod(1000);
    lowResourcesMonitor.setLowResourcesIdleTimeout(1000);
    lowResourcesMonitor.setMonitorThreads(true);
    lowResourcesMonitor.setMaxMemory(0);
    lowResourcesMonitor.setMaxLowResourcesTime(5000);
    lowResourcesMonitor.setAcceptingInLowResources(true);
    server.addBean(lowResourcesMonitor);

    String realmConfig = jettyProps.getProperty("jetty.realm.file");

    if (realmConfig != null && !realmConfig.isEmpty()) {
      HashLoginService login = new HashLoginService();

      login.setName(jettyProps.getProperty("jetty.realm.name"));
      login.setConfig(realmConfig);
      login.setHotReload(false);
      server.addBean(login);
    }

    return server;
  }

  private JettyProperties readJettyProps() throws IOException
  {
    JettyProperties result = new JettyProperties();
    InputStream inStream = openResourceStream("jetty.properties");

    if (inStream != null) {
      try {
        result.load(inStream);
      }
      finally {
        inStream.close();
      }
    }

    return result;
  }

  private WebAppContext findIncaContext() {
    WebAppContext incaContext = null;
    for ( Handler h : server.getChildHandlers() ) {
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
