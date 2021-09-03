package edu.sdsc.inca.consumer;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.DepotClient;
import edu.sdsc.inca.dataModel.queryResults.ObjectDocument;
import edu.sdsc.inca.dataModel.queryResults.Row;
import edu.sdsc.inca.dataModel.queryResults.Rows;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.util.StringMethods;


/**
 * Handles the prefetching and caching of HQL queries to the depot.  The HQL
 * queries allow users to request customized data from the depot.  In the
 * event that a query takes a long time to load, this class can be used
 * to continuously query results from the depot and store them.  The advantage
 * of this is that whenever requests are made on the consumer for the data,
 * the latest cached data can be returned immediately rather than waiting for
 * the lengthy query to complete.  Furthermore, it will reduce the load on the
 * consumer and depot.  If a cached query has parameters, it is a template
 * for cached queries (e.g., get latest suite results for suite X)
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class DepotQuery extends TimerTask {
  public final static String QUERYDIR = "queries";
  private static Properties depotConfig = null;
  private static Logger logger = Logger.getLogger( DepotQuery.class );

  private File resultFile = null;
  private Object[] params = new Object[0];
  private String command = null;
  private String name = null;
  private int period = 0; // millis
  private String reloadAt = "*";
  private String tempDir = null;

  /**
   * Create a new cache query thread for the specified query.
   *
   * @param period  The frequency of which the consumer should prefetch the
   * query
   * @param reloadAt Should be "*" if we don't care about the time the query
   *                 is started otherwise should be WW:HH:MM.
   * @param tempDir A temporary directory where queries can be cached
   * @param name   The name of the query (may contain whitespace)
   * @param command  The depot query command
   * @param params The parameters for the query command
   */
  public DepotQuery
    (int period, String reloadAt, String tempDir,
     String name, String command, Object... params){

    this.tempDir = tempDir;
    this.setCommand( command );
    this.setQueryName( name );
    if ( params != null ) this.setParams( params );
    this.setReloadAt( reloadAt );
    this.setPeriod( period );
  }

  /**
   * Return the handle of the file used to store the query result.
   *
   * @return The file handle to the query result.
   */
  public File getCacheFile() {
    return resultFile;
  }

  /**
   * Return the depot command being used to fetch data from the depot.
   *
   * @return A string containing a depot command.
   */
  public String getCommand() {
    return this.command;
  }

  /**
   * Returns a set of depot client objects to use to connect to a depot
   *
   * @return an array of depot client objects.
   */
  public static synchronized DepotClient[] getDepotClients() {

    Properties clientConfig = new Properties();
	String[] uris = depotConfig.getProperty("depot").split("[\\s,;]+");
    DepotClient[] clients = new DepotClient[uris.length];

    clientConfig.putAll(depotConfig);

    for (int i = 0 ; i < uris.length ; i += 1) {
      clients[i] = new DepotClient();

      clientConfig.setProperty("depot", uris[i]);

      try {
        clients[i].setConfiguration(clientConfig);
      } catch (ConfigurationException configErr) {
        logger.error("Unable to configure depot clients: " + configErr.getMessage());

        return null;
      }
    }

    return clients;
  }

  /**
   * Check the age of the cached file and return the number of milliseconds
   * before it should be refreshed again.
   *
   * @return Number of milliseconds before next refresh
   */
  public long getNextRefresh() {
    if ( resultFile.exists() && resultFile.length() > 0 ) {
      long age = Util.getTimeNow() - resultFile.lastModified();
      if ( age < period ) {
        return period - age;
      }
    }
    return 0;
  }

  /**
   * Return the depot command params being used to fetch data from the depot.
   *
   * @return A string array containing depot command parameters.
   */
  public Object[] getParams() {
    return this.params;
  }

  /**
   * Returns the frequency of queries to the depot.
   *
   * @return The number of seconds used to wait before querying the depot again.
   */
  public int getPeriod() {
    return period / 1000;
  }

  /**
   * Returns the name of the query.
   *
   * @return  A string containing the name of the HQL query.
   */
  public String getQueryName() {
    return name;
  }

  /**
   * Set the reload start time.
   *
   * @return Should be "*" if we don't care about the time the query is started
   * otherwise should be WW:HH:MM.
   */
  public String getReloadAt() {
    return reloadAt;
  }

  /**
   * Returns the latest result of the query stored in the file cache.  Does
   * a synchronized read of the data from file and returns it.
   *
   * @return  An XML document containing the results of the query or null if
   * nothing has been cached yet or there are problems reading the file from
   * disk.
   */
  public synchronized String getStoredResult() {
    logger.debug( "Fetching results from " + resultFile.getAbsolutePath() );
    if ( ! resultFile.exists() ) {
      return null;
    }
    try {
      return StringMethods.fileContents( resultFile.getAbsolutePath() );
    } catch ( IOException e ) {
      logger.error( "Problem reading cached query " + name + " from disk", e );
      return null;
    }
  }

  /**
   * Query the depot and return the results (rows) as an XmlBean
   *
   * @return  An XmlBean containing the query results.
   *
   * @throws ConfigurationException if complete or incorrect depot parameters
   * @throws IOException if trouble connecting to depot
   * @throws ProtocolException if trouble talking to depot
   */
  public String getFreshResult()
    throws ConfigurationException, IOException, ProtocolException {

    return this.query();
  }

  private static String wrapResults(String[] queryresult) {
    ObjectDocument doc = ObjectDocument.Factory.newInstance();
    ObjectDocument.Object object = doc.addNewObject();
    if ( queryresult != null && queryresult.length > 0 ) {
      if ( queryresult.length == 1 ) {
        try {
          object.set(XmlObject.Factory.parse( queryresult[0].trim(), (new XmlOptions()).setLoadStripWhitespace() ) );
        } catch ( XmlException e ) {
          logger.error( "Unable to parse query result", e );
        }
      } else {
        Rows rows = Rows.Factory.newInstance();
        for ( int i = 0; i < queryresult.length; i++ ) {
          Row row = rows.addNewRow();
          try {
            row.set( XmlObject.Factory.parse( queryresult[i].trim(), (new XmlOptions()).setLoadStripWhitespace() ) );
          } catch ( XmlException e ) {
            logger.error( "Unable to parse query result " + i, e );
          }
        }
        object.set( rows );
      }
    }
    return doc.xmlText();
  }

  /**
   * Send a query to the depot.
   *
   * @return  An XML document complying to the queryResults schema
   *
   * @throws ConfigurationException if problem with depot contact info
   * @throws IOException iif trouble connecting to depot
   */
  public String query() throws ConfigurationException, IOException {
    return DepotQuery.query( this.getCommand(), this.getParams() );
  }


  /**
   * Send a query to the depot.
   *
   * @param command The query command to call on the depot client
   * (e.g., querySuite, queryHql, etc.)
   *
   * @param params The parameters for the query command
   *
   * @return  An XML document complying to the queryResults schema
   *
   * @throws ConfigurationException if problem with depot contact info
   * @throws IOException iif trouble connecting to depot
   */
  public static String query( String command, Object... params )
    throws ConfigurationException, IOException {

    DepotClient[] clients = getDepotClients();
    Object results = null;
    boolean methodFound = false;

    for (int i = 0 ; i < clients.length ; i += 1) {
      logger.info( "Contacting depot " + clients[i].getUri() );
      long startTime = Util.getTimeNow();
      try {
        clients[i].connect();
        for( Method m : DepotClient.class.getMethods() ) {
          if ( m.getName().equals( command ) && m.getParameterTypes().length == params.length ) {
            methodFound = true;
            try {
              logger.info( "Fetching results for " + command );
              results = m.invoke( clients[i], params );
              Util.printElapsedTime( startTime, command + " query" );
            } catch (Exception e) {
              logger.error( "Problem invoking method " + command, e );
              int j = 0;
              for( Class<?> c : m.getParameterTypes() ) {
                if ( params[j].getClass().isAssignableFrom(c) ) {
                  logger.info( "param " + j + " is of type " + c );
                } else {
                  logger.error( "param " + j + " not of type " + c + "; type is " + params[j].getClass() );
                }
                j++;
              }
            }
            break;
          }
        }
        break;
      } catch (Exception e) {
        logger.error( "Unable to connect to depot " + clients[i].getUri(), e );
      }
      finally {
        if (clients[i].isConnected())
          clients[i].close();
      }
    }

    if ( ! methodFound ) logger.warn( "Unknown depot command: " + command );
    if ( results == null ) return "";
    if ( results.getClass().isArray() ) {
      return DepotQuery.wrapResults( (String[])results );
    } else if ( XmlObject.class.isInstance(results) ) {
      return DepotQuery.wrapResults( new String[] { ((XmlObject)results).xmlText() } );
    } else {
      return DepotQuery.wrapResults( new String[] { ((String)results) } );
    }
  }

  /**
   * Will first check to see if there is a result already stored on disk and
   * if so, will check to see how fresh it is.  If the result is younger than
   * the refreshSuiteCaching period, we will wait until it has expired before querying
   * the depot.  Once the first query is made, this thread will continously
   * query until it receives an interrupt.
   */
  @Override
  public void run() {
    try {
      refresh();
    } catch ( Exception e ) {
      logger.error( "Error retrieving query " + name, e );
    }
  }

  /**
   * Query the depot for a new result and store it in the cache
   *
   * @throws ConfigurationException if unable to read depot config params
   * @throws IOException if unable to contact depot
   * @throws ProtocolException if trouble talking to depot
   */
  public void refresh() throws ConfigurationException, IOException,
                               ProtocolException {
    if ( period < 1 ) {
      logger.warn( "Non-cached query cannot be refreshed" );
      return;
    }
    logger.info( "Refreshing query " + this.getQueryName() );
    long queryStart = Util.getTimeNow();
    String result = this.getFreshResult();
    long lastCacheTime = (Util.getTimeNow() - queryStart)/1000;
    logger.info
          ( "Query time for " + this + " = " + lastCacheTime + " secs" );
    if ( result != null && ! result.equals("") ) {
      synchronized (this) {
        FileWriter writer = new FileWriter( resultFile );
        writer.write( result );
        writer.close();
        this.notifyAll();
      }
    }
    logger.info( "New query result for " + name + " stored in " +
                 resultFile.getAbsolutePath() );
  }

  /**
   * Set the depot query command being used to fetch data from the depot.
   *
   * @param command   A string containing a depot query command
   */
  public void setCommand( String command ) {
    this.command = command;
  }

  /**
   * Set the configuration for connecting to the depot.
   *
   * @param depotConfig  A properties list containing config parameters for
   * connecting to the depot.
   */
  public static synchronized void setDepotConfig( Properties depotConfig ) {
    DepotQuery.depotConfig = depotConfig;
  }

  /**
   * Set the depot query command parameters being used to fetch data from the
   * depot.
   *
   * @param params A string array containing parameters for a depot query
   * command
   */
  public void setParams( Object[] params ) {
    this.params = params;
  }

  /**
   * Set the frequency of queries to the depot.
   *
   * @param period Set the number of seconds used to wait before querying the
   * depot again.
   */
  public void setPeriod( int period ) {
    this.period = period * 1000;
  }

  /**
   * Set the name of the query.
   *
   * @param name  A string containing the name of the HQL query.
   */
  public void setQueryName( String name ) {
    this.name = name;
    Pattern space = Pattern.compile( "\\s" );
    Matcher matcher = space.matcher( name );
    String filename = matcher.replaceAll( "_" );
    File queryDir = new File( tempDir + File.separator + QUERYDIR );
    if ( ! queryDir.exists() ) queryDir.mkdirs();
    this.resultFile = new File
      ( queryDir.getAbsolutePath() + File.separator + filename + ".xml" );
    logger.debug
      ( "Query results for " + name + " at " + resultFile.getAbsolutePath() );
  }

  /**
   * Set the reload start time
   *
   * @param reloadAt  Should be "*" if we don't care about the time the query
   *                 is started otherwise should be WW:HH:MM.
   */
  public void setReloadAt( String reloadAt ) {
    this.reloadAt = reloadAt;
  }

  /**
   * Return the query information as a string
   *
   * @return  A string representation of the query
   */
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer( this.getCommand() );
    buf.append( "(" );
    for( Object o : this.getParams() ) {
      buf.append( o );
      buf.append( "," );
    }
    buf.append( ") - " );
    buf.append( this.period );
    buf.append( " @ " );
    buf.append( this.reloadAt );
    return buf.toString();
  }
}
