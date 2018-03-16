package edu.sdsc.inca;

import junit.framework.TestCase;

import java.net.URL;
import java.net.URLConnection;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Properties;

import org.apache.log4j.Logger;
import edu.sdsc.inca.util.ConfigProperties;
import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.dataModel.inca.IncaDocument;
import edu.sdsc.inca.dataModel.resourceConfig.ResourceConfig;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.dataModel.suite.Suite;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.protocol.Protocol;

import javax.net.ServerSocketFactory;

/**
 * Test Consumer class
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class ConsumerTest extends TestCase {
  public static Logger logger = Logger.getLogger( ConsumerTest.class );

  public static class ConsumerTester {
    public Consumer consumer;
    public MockAgent agent;
    public MockDepot depot;

    public void start()
      throws Exception {
      agent.start();
      while( ! agent.isReady) {
        Thread.sleep( 1000 );
      }
      depot.start();
      while( ! depot.isReady) {
        Thread.sleep( 1000 );
      }

      consumer.startConsumer();
      Properties config = consumer.getClientConfiguration();
      config.setProperty( "agent", "inca://localhost:" + agent.port );
      config.setProperty( "depot", "inca://localhost:" + depot.port );
      consumer.setClientConfiguration(config);
      logger.debug( "Setting consumer onfiguration" );
    }

    public void stop()
      throws Exception{

      logger.debug( "Shutting down consumer" );
      consumer.shutdown();
      logger.debug( "Shutting down mock depot" );
      depot.interrupt();
      depot.join();
      logger.debug( "Shut down of mock depot complete" );
      logger.debug( "Shutting down mock agent" );
      agent.interrupt();
      agent.join();
      logger.debug( "Shut down of mock agent complete" );
    }

    public ConsumerTester( ) throws Exception {
      ConfigProperties config = new ConfigProperties();
      config.putAllTrimmed(System.getProperties(), "inca.consumer.");
      config.loadFromResource("inca.properties", "inca.consumer.");

      int agentPort = 8899;
      agent = new MockAgent( agentPort );

      int depotPort = 8898;
      depot = new MockDepot( depotPort );
      consumer = new Consumer();
      config.setProperty( "agent", "inca://localhost:" + agentPort );
      config.setProperty( "depot", "inca://localhost:" + depotPort );
      config.setProperty( "auth", "false" );
      if ( config.containsKey( "logfile" )) config.remove( "logfile" );
      consumer.setConfiguration( config );
    }
  }

  /**
   * Test the ability to load the index page for the Inca consumer. Note, uses
   * mock agent and depot servers to emulate the real agent and depot.
   *
   * @throws Exception if unable to run test
   */
  public void testRun() throws Exception {
    ConfigProperties config = new ConfigProperties();
    config.putAllTrimmed(System.getProperties(), "inca.consumer.");
    config.loadFromResource("inca.properties", "inca.consumer.");

    // start up a mock depot and agent server
    int depotPort = 8998;
    int agentPort = 8999;
    MockDepot depot = new MockDepot( depotPort );
    MockAgent agent = new MockAgent( agentPort );
    depot.start();
    agent.start();

    // start up the inca consumer
    Consumer consumer = new Consumer();
    consumer.setConfiguration( config );
    consumer.startConsumer();
    logger.info( "Consumer started" );
    Thread.sleep( 5000 );
    config.setProperty( "depot", "inca://localhost:" + depot.port );
    config.setProperty( "agent", "inca://localhost:" + agent.port );


    // load the index page for the Inca consumer.  Since, it may take a little
    // bit of time for the consumer to load our webapp we try every 5 seconds
    // till we can load it successfully.
    try {
      InputStream inputStream = null;
      while ( inputStream == null ) {
        logger.info( "Sleeping 5 seconds" );
        Thread.sleep(5000);
        logger.info( "Trying to load page" );
        URL catalogUrl = new URL("http://localhost:8080/inca/index.jsp");
        URLConnection conn = catalogUrl.openConnection();
        try {
          inputStream = conn.getInputStream();
        } catch ( ConnectException e ) {
          logger.info( "Jetty server not up yet" );
        }
      }
      BufferedReader input = new BufferedReader(
        new InputStreamReader(inputStream)
      );
      String s;
      boolean pageLoaded = false;
      while ( (s = input.readLine() ) != null ) {
        if ( Pattern.matches( ".*Inca Consumer web pages!.*", s)) {
          pageLoaded = true;
          break;
        }
      }
      input.close();
      logger.info( "Page loaded successfully" );

      // did we see the welcome message indicating a successful load?
      assertTrue( "Page loaded", pageLoaded );

    } catch ( Exception e ) {
      logger.error( "Error testing consumer", e );
    }

    consumer.shutdown();
    agent.interrupt();
    agent.join();
    depot.interrupt();
    depot.join();

  }

  /**
   * Use to emulate a real agent.  Can be customized to serve a specific
   * resources file from the GETCONFIG command with a certain amount of
   * delay.
   */
  static public class MockAgent extends Thread {
    static private Logger logger = Logger.getLogger( MockAgent.class ) ;
    private int delay = 0;  // delay the return of the config file if desired
    private int port = -1; // port to listen on
    private ResourceConfig resourceConfig = null; // return when GETCONFIG
    private String[] suites = new String[0];
    private String[] repositories = new String[] {
      "http://inca.sdsc.edu/2.0/repository",
      "http://inca.sdsc.edu/2.0/ctssv3"
    };
    public boolean isReady = false;

    /**
     * Create a new mock agent.
     *
     * @param port            The port to start the mock depot on.
     */
    public MockAgent( int port ) {
      this.port = port;
    }

    /**
     * Return the uri of the mock agent
     *
     * @return A string containing the uri of the mock agent.
     */
    public String getUri() {
      try {
        String localhost = InetAddress.getLocalHost().getHostName();
        return "inca://" + localhost + ":" + port;
      } catch ( UnknownHostException e ) {
        logger.error( "Unable to get local hostname");
        return null;
      }
    }

    /**
     * Functionality of mock server.  Recognizes GETCONFIG and PING.
     */
    public void run() {

      // regular socket (i.e., no ssl)
      ServerSocketFactory factory =  ServerSocketFactory.getDefault();
      ServerSocket agentSocket = null;
      for ( int i = 0; i < 10 && agentSocket == null; i++ ) {
        try {
          agentSocket = factory.createServerSocket( port );
        } catch ( IOException e ) {
          port++;
          logger.warn( "agent port " + port + " not available" );
        }
      }
      if ( agentSocket == null ) {
        logger.error( "Unable to find open agent port after 10 tries" );
        return;
      }
      try {
        agentSocket.setSoTimeout( 5000 );
      } catch ( IOException e ) {
        // empty
      }
      logger.info( "Starting on port " + port );
      isReady = true;
      Socket agent = null;
      try {
        while(!this.isInterrupted()) {
          try {
            agent = agentSocket.accept();
          } catch ( SocketTimeoutException e ) {
            continue; // allows periodic interrupt check
          }
          logger.info( "Receieved connection " );
          ProtocolReader agentReader = new ProtocolReader(
            new InputStreamReader(agent.getInputStream())
          );
          ProtocolWriter agentWriter = new ProtocolWriter(
            new OutputStreamWriter(agent.getOutputStream())
          );
          while ( true ) {
            logger.info( "Waiting to receive command" );
            Statement stmt = agentReader.readStatement();
            if ( stmt == null ) break;
            logger.info( "Received: " + stmt.toString() );
            Statement reply;
            String cmd = new String( stmt.getCmd() );
            if ( cmd.equals(Protocol.CONFIG_GET_COMMAND)) {
              Thread.sleep( this.delay );
              IncaDocument config = IncaDocument.Factory.newInstance();
              config.addNewInca();
              if ( resourceConfig != null ) {
                config.getInca().setResourceConfig( resourceConfig );
              }
              if ( suites != null && suites.length > 0 ) {
                config.getInca().addNewSuites();
                Suite[] suiteChunks = new Suite[suites.length];
                for ( int j = 0; j < suites.length; j++ ) {
                  SuiteDocument suiteDoc =
                    SuiteDocument.Factory.parse(suites[j]);
                  suiteChunks[j] = suiteDoc.getSuite();
                }
                config.getInca().getSuites().setSuiteArray( suiteChunks );
              }
              if ( repositories != null ) {
                IncaDocument.Inca.Repositories repos =
                  config.getInca().addNewRepositories();
                repos.setRepositoryArray( repositories );
              }
              reply = Statement.getOkStatement(  config.toString() );
            } else if ( cmd.equals(Protocol.CATALOG_GET_COMMAND)) {
              String catalog =
                "file: bin/cluster.java.sun.version\n" +
                "name: cluster.java.sun.version\n" +
                "url: http://java.sun.com";
              reply = Statement.getOkStatement( catalog );
            } else if ( cmd.equals(Protocol.PING_COMMAND)) {
              reply = Statement.getOkStatement( new String(stmt.getData()) );
            } else {
              logger.error( "Received wrong command" );
              reply = Statement.getErrorStatement(
                "Expecting ping command; got " + cmd
              );
            }
            agentWriter.write(reply);
          }
        }
      } catch ( Exception e ) {
        try {
          agent.close();
        } catch ( IOException e1 ) {
          logger.error( "Error closing agent", e1 );
        }
        logger.error( "Caught exception", e );
      }

    }

    /**
     * Set an artifical delay in the return of the GETCONFIG request.
     *
     * @param delay The number of milliseconds to wait before returning the
     * Inca configuration document.
     */
    public void setDelay( int delay ) {
      this.delay = delay;
    }

    /**
     * Set the resources that should be returned in the Inca configuration XML
     * in response to GETCONFIG command.
     *
     * @param resourceConfig   The resources to return to the agent client.
     */
    public void setResourceConfig( ResourceConfig resourceConfig ) {
      this.resourceConfig = resourceConfig;
    }

    /**
     * Set the suites that should be returned in the Inca configuration XML
     * in response to GETCONFIG command.
     *
     * @param suites   The suites to return to the agent client.
     */
    public void setSuites( String[] suites ) {
      this.suites = suites;
    }
  }

  /**
   * Functionality of mock depot server.  Recognizes GETSUITE and PING.
   */
  static public class MockDepot extends Thread {
    private Logger logger = Logger.getLogger( this.getClass().getName() ) ;
    private int port = -1;
    private Hashtable<String,String[]> suites = new Hashtable<String,String[]>();  // suites "stored" on server
    private Hashtable<String,Integer> delays = new Hashtable<String,Integer>();  // used to simulate retrieval time
    public boolean isReady = false;
    public int statusHistoryDelay = 40000;

    /**
     * Create a new mock depot.
     *
     * @param port        The port to start the mock depot on.
     */
    public MockDepot( int port ) {
      this.port = port;
    }

    /**
     * Add a suite to the mock depot.  This will get echoed back to a client
     * when they query for suites.
     *
     * @param name The name of the suite
     *
     * @param xml  An array of results to use as the suite contents
     *
     * @param retTime How long to wait before returning the results.
     */
    public synchronized void addSuite( String name, String[] xml, int retTime ) {
      logger.debug( "Adding suite " + name + " with wait " + retTime );
      suites.put( name, xml );
      delays.put( name, retTime );
    }

    /**
     * Remove a suite to the mock depot.
     *
     * @param name The name of the suite to delete
     */
    public synchronized void deleteSuite( String name ) {
      suites.remove( name );
      delays.remove( name);
    }

    /**
     * Functionality of mock depot server.  Recognizes GETCONFIG and PING.
     */
    public void run() {

      ServerSocketFactory factory =  ServerSocketFactory.getDefault();
      ServerSocket depotSocket = null;
      for( int i = 0; i < 10 && depotSocket == null; i++ ) {
        try {
          depotSocket = factory.createServerSocket( port );
        } catch ( IOException e ) {
          logger.warn( "depot port " + port + " is unavailable" );
          port++;
        }
      }
      if ( depotSocket == null ) {
        logger.error( "Unable to find open port after 10 tries" );
        return;
      }
      try {
        depotSocket.setSoTimeout( 5000 );
      } catch ( IOException e ) {
        // empty
      }
      logger.info( "Starting on port " + port );
      isReady = true;
      Socket depot = null;
      try {
        while( !this.isInterrupted() ) {
          logger.debug( "Waiting for connection" );
          try {
            depot = depotSocket.accept();
          } catch ( SocketTimeoutException e ) {
            continue; // Allows periodic interrupt check
          }
          ProtocolReader depotReader = new ProtocolReader(
            new InputStreamReader(depot.getInputStream())
          );
          ProtocolWriter depotWriter = new ProtocolWriter(
            new OutputStreamWriter(depot.getOutputStream())
          );
          while( true ) {
            logger.info( "Waiting to receive command" );
            Statement suiteStmt = depotReader.readStatement();
            if ( suiteStmt == null ) break;
            Statement reply = null;
            String cmd = new String( suiteStmt.getCmd() );
            logger.info( "Received: " + suiteStmt.toString() );
            if ( cmd.equals(Protocol.QUERY_GUIDS_COMMAND) ) {
              logger.info( "Query guids" );
              String[] suiteNames = suites.keySet().toArray(
                new String[suites.keySet().size()]
              );
              Arrays.sort( suiteNames );
              reply = Statement.getOkStatement(
                StringMethods.join("\n", suiteNames)
              );
            } else if ( cmd.equals(Protocol.QUERY_LATEST_COMMAND) ) {
              if ( reply == null ) {
                String whereStmt = new String( suiteStmt.getData() );
                Pattern suitePattern = Pattern.compile( "suite.guid\\s+=\\s+'(\\w+)'" );
                Matcher suiteMatcher = suitePattern.matcher( whereStmt );
                suiteMatcher.find();
                String suiteName = suiteMatcher.group(1);
                logger.info( "Mock depot retrieving suite " + suiteName );
                String[] xml = suites.get( suiteName );
                int delay = delays.get( suiteName );
                logger.info( "Sleeping " + delay + " millis before responding");
                Thread.sleep( delay );
                for ( int j = 0; j < xml.length; j++ ) {
                  reply = new Statement();
                  reply.setCmd( Protocol.QUERY_RESULT.toCharArray() );
                  reply.setData( xml[j].toCharArray() );
                  depotWriter.write(reply);
                }
                reply = new Statement();
                reply.setCmd( Protocol.END_QUERY_RESULTS_COMMAND.toCharArray() );
              }
            } else if ( cmd.equals(Protocol.PING_COMMAND)) {
              logger.info( "ping" );
              reply = Statement.getOkStatement(
                new String(suiteStmt.getData())
              );
            } else if ( cmd.equals(Protocol.QUERY_HQL_COMMAND) ||
                        cmd.equals(Protocol.QUERY_INSTANCE_COMMAND) ||
                        cmd.equals(Protocol.QUERY_STATUS_COMMAND) ) {
              logger.info( "Received '" + cmd + "'" );
              if ( cmd.equals(Protocol.QUERY_STATUS_COMMAND) ) {
                Thread.sleep( statusHistoryDelay );
              }
              reply = new Statement();
              reply.setCmd( Protocol.QUERY_RESULT.toCharArray() );
              String xml = " <time>" + Calendar.getInstance().getTimeInMillis() + "</time>";
              reply.setData( xml.toCharArray() );
              depotWriter.write(reply);
              reply = new Statement();
              reply.setCmd( Protocol.END_QUERY_RESULTS_COMMAND.toCharArray() );

            } else {
              logger.error( "Received wrong command" );
              reply = Statement.getErrorStatement(
                "Received wrong command " + cmd
              );
            }
            logger.info( "Writing " + reply.toString() );
            depotWriter.write(reply);
          }
        }
      } catch ( Exception e ) {
        logger.error( "Caught exception", e );
        try {
          depot.close();
        } catch (IOException e1 ) {
          logger.error( "Caught exception", e1 );
        }
      }
    }

  }

}
