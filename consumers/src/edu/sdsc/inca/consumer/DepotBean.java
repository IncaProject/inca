package edu.sdsc.inca.consumer;


import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Timer;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlDate;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlLong;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlString;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.DepotClient;
import edu.sdsc.inca.dataModel.queryResults.ObjectDocument;
import edu.sdsc.inca.dataModel.queryStore.Cache;
import edu.sdsc.inca.dataModel.queryStore.Query;
import edu.sdsc.inca.dataModel.queryStore.QueryStoreDocument;
import edu.sdsc.inca.dataModel.queryStore.Type;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.Constants;
import edu.sdsc.inca.util.CronSchedule;
import edu.sdsc.inca.util.CrypterException;
import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.util.XmlWrapper;


/**
 * Manages a list of stored (persistent) depot queries, some of which are longer
 * queries that are cached to disk so that results can be prefetched.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class DepotBean extends Thread {
  public static final String QUERYSTORE = "queryStore.xml";
  public static final String suiteCachePrefix = "incaQueryLatest+";
  public static final String queryStatus = "incaQueryStatus";
  private static Logger logger = Logger.getLogger( DepotBean.class );

  private String cacheReloadAt = "*:23:0";
  private int cacheReloadPeriod = 5 * Constants.SECONDS_TO_MINUTE;
  private String ignoreErrorPattern = "(^DOWNTIME:.*|^NOT_AT_FAULT.*|.*Inca error.*|.*Unable to fetch proxy for reporter execution.*)";
  private File qsFile = null;
  private QueryStoreDocument queryStore = null;
  private String tempDir = "/tmp";
  Hashtable<String,DepotQuery> cachedQueries =
    new Hashtable<String,DepotQuery>();
  private Timer periodTimer = new Timer( "DepotBeanPeriodTimer" );
  private Timer periodAtTimer = new Timer( "DepotBeanPeriodAtTimer" );


  public DepotBean() {
    /* empty */
  }

  /**
   * Read in or create a new list of stored queries from the provided file.
   *
   * @param filePath  A string containing the path to an XML file.
   *
   * @param queryDir A directory where cached queries can be stored
   *
   * @throws edu.sdsc.inca.ConfigurationException if unable to open queries
   */
  public DepotBean( String filePath, String queryDir )
    throws ConfigurationException {

    init(filePath, queryDir);
  }

  /**
   * Add a query to the query store.  The results of this query will always
   * be fetched directly from the depot.  Use this function for light to medium
   * weight queries.
   *
   * @param name  The name of the query to store the hql under.
   * @param type  The depot query command
   * @param params The parameters for the query command
   *
   * @return True if the query was successfully added to the store and false
   * if not.
   */
  public synchronized boolean add(String name, String type, String... params) {
    return add( 0, "*", name, type, params );
  }

  /**
   * Add a query to the query store.  The results of this query will be stored
   * to disk and continously fetched (via a thread) using the frequency
   * provided.  Use this function for heavy-duty queries.
   *
   * @param cachePeriod  The frequency in seconds to fetch the results of the
   *                     query periodically from the depot.
   * @param reloadAt  The start time for the cache in the form WW:HH:MM
   * @param name  The name of the query to store the hql under.
   * @param type  The depot query command
   * @param params The parameters for the query command
   *
   * @return True if the query was successfully added to the store and false
   * if not.
   */
  public synchronized boolean add(int cachePeriod, String reloadAt, String name,
                                   String type, String... params) {

    Query queryXml = queryStore.getQueryStore().addNewQuery();
    Query cache = this.createQuery(cachePeriod, reloadAt, name,type,params);
    queryXml.set( cache );
    DepotQuery  query = this.readQuery(cache);
    logger.debug( "Adding query " + query );
    cachedQueries.put( name, query );
    if ( cachePeriod > 0 ) {
      if ( reloadAt.equals("*") || reloadAt.equals("*:*") ||
           reloadAt.equals("*:*:*") ) {
        periodTimer.schedule
          (query,query.getNextRefresh(),cachePeriod*Constants.MILLIS_TO_SECOND);
      } else {
        try {
          periodAtTimer.schedule
            (query, CronSchedule.parseWHM( reloadAt ).nextEvent(),
              cachePeriod * Constants.MILLIS_TO_SECOND);
        } catch (ParseException e) {
          logger.error( "Unable to add query " + name, e );
          return false;
        }
      }
      logger.debug( "Adding query " + query.getQueryName() + " as timer task");
    }
    try {
      this.save();
      return true;
    } catch ( IOException e ) {
      logger.error
        ( "Problem saving query store to " + qsFile.getAbsolutePath(), e );
      return false;
    }
  }

  /**
   * Delete the specified query from the query store.
   *
   * @param name  The name of the query to remove.
   *
   * @return True if the query was successfully deleted and false if not.
   */
  public boolean delete( String name ) {
    Query[] queries = queryStore.getQueryStore().getQueryArray();
    for ( int i = 0; i < queries.length; i++ ) {
      if ( queries[i].getName().equals(name) ) {
        DepotQuery query = cachedQueries.remove( queries[i].getName() );
        logger.info( "Deleted query " + query );
        query.cancel();
        queryStore.getQueryStore().removeQuery(i);
        try {
          this.save();
          return true;
        } catch ( IOException e ) {
          logger.error( "Problem saving updated query store to disk", e );
          return false;
        }
      }
    }
    return false;
  }

  /**
   * Return the period of refreshSuiteCaching for the suite and resources in the
   * Consumer's cache.
   *
   * @return  the number of milliseconds in between reloads of cached
   * objects.
   */
  public int getCacheReloadPeriod() {
    return cacheReloadPeriod;
  }

  /**
   * Return a handle to the query file.
   *
   * @return  A File object.
   */
  public File getFile() {
    return qsFile;
  }

  /**
   * Get the regular expression pattern to mark neutrally if an error matches it
   *
   * @return ignoreErrorPattern  The pattern to mark neutrally in error msgs
   */
  public String getIgnoreErrorPattern() {
    return ignoreErrorPattern;
  }

  /**
   * Return the query store xml as a string
   *
   * @return An xml document containing the query store info
   */
  public synchronized String getQueriesXml() {
    return queryStore.xmlText((new XmlOptions()).setSavePrettyPrint());
  }

  /**
   * Return the stored query names.
   *
   * @return  An array of stored query names.
   */
  public String[] getQueryNames() {
    return cachedQueries.keySet().toArray( new String[cachedQueries.size()] );
  }

  /**
   * Return the query params for the named query
   *
   * @param name  The name of a query
   *
   * @return  The query params or null if query not found
   */
  public Object[] getQueryParams( String name ) {
    return cachedQueries.containsKey(name) ?
      cachedQueries.get(name).getParams() : null;
  }


  /**
   * Retrieve a result for the named query.  The result will either be
   * fetched directly from the depot or from disk (if it is cached).
   * This is a blocking function.
   *
   * @param name  The name of the query to retrieve the results for
   *
   * @return  A string containing the result of the query.
   */
  public String getQueryResult( String name ) {
    if ( cachedQueries.containsKey(name) ) {
      DepotQuery query = cachedQueries.get(name);
      if ( query.getPeriod() > 0 ) {
        logger.debug( "Fetching query results " + name + " from cache" );
        String result = query.getStoredResult();
        if ( result == null ) { // no result yet
          result = ObjectDocument.Factory.newInstance().xmlText();
        }
        return result;
      } else {
        try {
          logger.debug( "Fetching query results from depot" );
          return query.getFreshResult();
        } catch ( Exception e ) {
          logger.error( "Problem fetching result for cached query " + name, e );
          return null;
        }
      }
    } else {
      return null;
    }
  }

  /**
   * Return the query type for the named query
   *
   * @param name  The name of a query
   *
   * @return  The type of query or null if query not found
   */
  public String getQueryType( String name ) {
    return cachedQueries.containsKey(name) ?
      cachedQueries.get(name).getCommand() : null;
  }

  /**
   * Check to see if the query exists in the store already.
   *
   * @param name   The name of the query to search for.
   *
   * @return True if the query exists in the store and false if not.
   */
  public synchronized boolean hasQuery( String name ) {
    return cachedQueries.containsKey( name );
  }

  /**
   * Fetch the list of stored query names.
   *
   * @return A list containing the names of the stored queries.
   */
  public synchronized String[] list() {
    Query[] queries = queryStore.getQueryStore().getQueryArray();
    String[] names = new String[queries.length];
    for ( int i = 0; i < queries.length; i++ ) {
      names[i] = queries[i].getName();
    }
    return names;
  }

  /**
   * Refresh the cached result for the named query.
   *
   * @param name  The name of the query to retrieve the results for
   *
   * @return true if query was refreshed and false if not or is not a cached
   * query.
   */
  public boolean refresh( String name ) {
    if ( cachedQueries.containsKey(name) ) {
      DepotQuery query = cachedQueries.get(name);
      if ( query.getPeriod() > 0 ) {
        logger.debug( "Fetching query results " + name + " from cache" );
        try {
          query.refresh();
          return true;
        } catch (Exception e) {
          logger.error( "Unable to refresh query " + name, e );
          return false;
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * Query the depot and make sure there is a cache entry for each suite
   * and remove those that are no longer around
   */
  public void refreshSuiteCaching() {
    logger.info( "Refreshing list of cached suites" );
    DepotClient[] clients = DepotQuery.getDepotClients();
    for (int i = 0 ; i < clients.length ; i += 1) {
      try {
        logger.debug( "Query depot " + clients[i].getUri() + " for suite guids" );
        clients[i].connect();
        String[] guids = clients[i].queryGuids();
        clients[i].close();

        // add queries that are not yet in the stored depot queries
        Properties propGuids = new Properties();
        for( String guid : guids ) {
          if ( guid.endsWith(Protocol.IMMEDIATE_SUITE_NAME) ) {
            continue; // discard run now results
          }
          String cacheName = getCacheName(guid);
          propGuids.setProperty( cacheName, "" );
          if ( ! this.hasQuery(cacheName) ) {
            boolean result = this.add
              ( this.cacheReloadPeriod, "*", cacheName, "latest",
                "suite.guid = '" + guid + "'" );
            logger.debug("Adding guid to '" + guid + "' to the cache: " + result);
          } else {
            logger.debug( "Query '" + guid + "' already added" );
          }
        }
        // delete queries that are no longer relevant
        for( String s : this.getQueryNames() ) {
          if (s.startsWith(suiteCachePrefix) && propGuids.getProperty(s) == null){
            logger.info( "Deleting cached suite query " + s );
            this.delete( s );
          }
        }

        // add series history query
        if ( ! this.hasQuery(queryStatus) && guids.length > 0 ) {
          logger.info( "Adding status history query" );
          this.add
            ( Constants.SECONDS_TO_HOUR * Constants.HOURS_TO_DAY, cacheReloadAt,
              queryStatus, "statusHistory", "WEEK", "28", "1 = 1" );
        }
        break;
      } catch (Exception e) {
        logger.error( "Trouble getting suite guids from depot", e );
      }
    }
  }


  /**
   * Periodically query depot and refresh caching of suites
   */
  @Override
  public void run() {
    this.refreshSuiteCaching();
    this.startQueries();
    long sleepTime = this.cacheReloadPeriod * Constants.MILLIS_TO_SECOND;

    try {
      while ( true ) {
        logger.debug( "Suite cache sleeping " + sleepTime + " millis" );
        Thread.sleep( sleepTime );
        logger.debug( "Refreshing suite cache list" );
        this.refreshSuiteCaching();
      }
    } catch ( InterruptedException e ) {
      logger.info( "Suite cache received interrupt" );
      this.stopQueries();
    }
  }

  /**
   * Save the query store configuration to disk.
   *
   * @throws java.io.IOException if trouble while writing to disk
   */
  public void save() throws IOException {
    try {
      XmlWrapper.save(queryStore.xmlText((new XmlOptions()).setSavePrettyPrint()), qsFile.getAbsolutePath(), null);
    } catch (CrypterException e) {
      throw new IOException("Unexpected crypt error");
    }
  }

  /**
   * Set the attributes for connecting to the agent and also the maxWait
   * and reload frequency.
   *
   * @param beanConfig  A properties object containing the config options for
   * this object
   *
   * @throws ConfigurationException if problem with config params
   */
  public synchronized void setBeanConfig(Properties beanConfig)
    throws ConfigurationException {

    logger.debug( "Setting depot configuration" );

    //tmpdir
    String tmpDir = beanConfig.getProperty("var");
    if ( tmpDir == null ) System.getProperty( "javax.servlet.context.tempdir" );
    if ( tmpDir == null ) tmpDir = "/tmp";
    init( tmpDir + File.separator + QUERYSTORE, tmpDir );

    // caching
    String prop;
    if((prop = beanConfig.getProperty("reload")) != null) {
      cacheReloadPeriod = Integer.parseInt(prop);
    }
    if((prop = beanConfig.getProperty("reloadAt")) != null) {
      cacheReloadAt = prop;
    }

    // errors to mark neutrally
    if((prop = beanConfig.getProperty("ignoreErrors")) != null) {
      ignoreErrorPattern = prop;
    }


    // depot query
    DepotQuery.setDepotConfig( beanConfig );

  }

  /**
   * Set the period of refreshSuiteCaching for the suite and resources in the
   * Consumer's cache.
   *
   * @param period the number of milliseconds in between reloads of cached
   * objects
   */
  public void setCacheReloadPeriod( int period ) {
    cacheReloadPeriod = period;
  }

  /**
   * Set the path to the file containing stored query information.
   *
   * @param filePath  A path to an XML file.
   */
  public void setFile( String filePath ) {
    this.qsFile = new File(filePath);
  }

  /**
   * Set a regular expression pattern to mark neutrally if an error matches it
   *
   * @param ignoreErrors  The pattern to mark neutrally in error msgs
   */
  public void setIgnoreErrorPattern(String ignoreErrors) {
    ignoreErrorPattern = ignoreErrors;
  }

  /**
   * Starts the cached query threads so that results are prefetched from the
   * depot and continuously updated.
   */
  public void startQueries()  {
    logger.debug( "Starting queries" );
    for( DepotQuery q : cachedQueries.values() ) {
      if ( q.getPeriod() > 0 && q.scheduledExecutionTime() <= 0 ) {
        logger.debug( "Adding query " + q + " to timer");
        if ( q.getReloadAt() == null || q.getReloadAt().equals("*") ||
             q.getReloadAt().equals("*:*") || q.getReloadAt().equals("*:*:*") ){
          periodTimer.schedule
            (q, q.getNextRefresh(), q.getPeriod() * Constants.MILLIS_TO_SECOND);
        } else {
          try {
            periodAtTimer.schedule
            ( q, CronSchedule.parseWHM(q.getReloadAt()).nextEvent(),
                q.getPeriod() * Constants.MILLIS_TO_SECOND );
          } catch (ParseException e) {
            logger.error( "Unable to start query " + q, e );
          }
        }
      }
    }
  }

  /**
   * Stop prefetching query results from the depot.
   */
  public void stopQueries() {
    periodTimer.cancel();
    periodAtTimer.cancel();
  }

  // Private Functions

  /**
   * Create a new XML file for persisting stored queries and save it to disk.
   *
   * @throws edu.sdsc.inca.ConfigurationException if unable to create query
   * store xml.
   */
  public void clear() throws ConfigurationException {
    logger.info
      ( "Initializing query store document at " + qsFile.getAbsolutePath() );
    queryStore = QueryStoreDocument.Factory.newInstance();
    queryStore.addNewQueryStore();

    try {
      this.save();
    } catch ( IOException e ) {
      throw new ConfigurationException( "Unable to save new file", e );
    }
  }

  /**
   * Make a distinct name for the cached suites that we add
   *
   * @param guid  The guid of the suite
   *
   * @return  A distinct name for a suite guid that is likely not to clash
   * with a name a user would pick
   */
  public static String getCacheName( String guid ) {
    return suiteCachePrefix + guid.replaceAll( "/", "_" );
  }

  /**
   * Read a query from the provided xml object and parse it into a DepotQuery
   * object.
   *
   * @param cachePeriod  The frequency in seconds to fetch the results of the
   *                     query periodically from the depot.
   *
   * @param reloadAt Should be "*" if we don't care about the time the query
   *                 is started otherwise should be WW:HH:MM.
   * @param name  The name of the query to store the hql under.
   * @param type  The depot query command
   * @param params The parameters for the query command @return A new DepotQuery object that can be used to fetch results
   *
   * @return a query object from the queryStore schema
   */
  public Query createQuery(int cachePeriod, String reloadAt, String name,
                           String type, String... params) {

    Query query = Query.Factory.newInstance();

    if ( cachePeriod > 0 ) {
      Cache cache = query.addNewCache();
      cache.setReloadAt( reloadAt );
      cache.setReloadPeriod( BigInteger.valueOf(cachePeriod) );
    }
    query.setName( name );
    Type commandType = query.addNewType();
    commandType.setCommand( type );
    if ( type.equals("hql") ) {
      Type.Params commandParams = commandType.addNewParams();
      XmlString hqlXml = XmlString.Factory.newInstance();
      hqlXml.setStringValue(params[0]);
      XmlAnySimpleType param = commandParams.addNewParam();
      param.set( hqlXml );
    } else if ( type.equals("instance") ) {
      Type.Params commandParams = commandType.addNewParams();
      XmlString nickname = XmlString.Factory.newInstance();
      nickname.setStringValue(params[0]);
      XmlAnySimpleType paramNickname = commandParams.addNewParam();
      paramNickname.set(nickname);
      XmlString resource = XmlString.Factory.newInstance();
      resource.setStringValue(params[1]);
      XmlAnySimpleType paramResource = commandParams.addNewParam();
      paramResource.set(resource);
      XmlString target = XmlString.Factory.newInstance();
      target.setStringValue(params[2]);
      XmlAnySimpleType paramTarget = commandParams.addNewParam();
      paramTarget.set(target);
      String timePattern =  "yyyy-MM-dd HH:mm:ss";
      XmlDate collected = XmlDate.Factory.newInstance();
      collected.setDateValue(StringMethods.convertDateString(params[3], timePattern));
      XmlAnySimpleType paramCollected = commandParams.addNewParam();
      paramCollected.set(collected);
    } else if ( type.equals("latest")) {
      Type.Params commandParams = commandType.addNewParams();
      XmlString whereXml = XmlString.Factory.newInstance();
      whereXml.setStringValue(params[0]);
      XmlAnySimpleType param = commandParams.addNewParam();
      param.set( whereXml );
    } else if ( type.equals("period")) {
      Type.Params commandParams = commandType.addNewParams();
      String whereValue;
      if ( params.length == 2 ) {
        XmlInteger numDaysXml = XmlInteger.Factory.newInstance();
        numDaysXml.setBigIntegerValue( new BigInteger(params[0]) );
        XmlAnySimpleType paramNumDays = commandParams.addNewParam();
        paramNumDays.set( numDaysXml );
        whereValue = params[1];
      } else {
        String timePattern =  "yyyy-MM-dd";
        XmlDate startDateXml = XmlDate.Factory.newInstance();
        startDateXml.setDateValue
          ( StringMethods.convertDateString(params[0], timePattern) );
        XmlAnySimpleType paramStartDate = commandParams.addNewParam();
        paramStartDate.set( startDateXml );

        XmlDate endDateXml = XmlDate.Factory.newInstance();
        endDateXml.setDateValue
          ( StringMethods.convertDateString(params[1], timePattern) );
        XmlAnySimpleType paramEndDate = commandParams.addNewParam();
        paramEndDate.set( endDateXml );
        whereValue = params[2];
      }
      XmlString whereXml = XmlString.Factory.newInstance();
      whereXml.setStringValue(whereValue);
      XmlAnySimpleType paramWhere = commandParams.addNewParam();
      paramWhere.set( whereXml );
    } else if ( type.equals("statusHistory") ) {
      Type.Params commandParams = commandType.addNewParams();
      XmlString periodXml = XmlString.Factory.newInstance();
      periodXml.setStringValue(params[0]);
      XmlAnySimpleType paramPeriod = commandParams.addNewParam();
      paramPeriod.set( periodXml );
      String whereValue;
      if ( params.length == 3 ) {
        XmlInteger numDaysXml = XmlInteger.Factory.newInstance();
        numDaysXml.setBigIntegerValue( new BigInteger(params[1]) );
        XmlAnySimpleType paramNumDays = commandParams.addNewParam();
        paramNumDays.set( numDaysXml );
        whereValue = params[2];
      } else {
        String timePattern =  "yyyy-MM-dd";
        XmlDate startDateXml = XmlDate.Factory.newInstance();
        startDateXml.setDateValue
          ( StringMethods.convertDateString(params[1], timePattern) );
        XmlAnySimpleType paramStartDate = commandParams.addNewParam();
        paramStartDate.set( startDateXml );

        XmlDate endDateXml = XmlDate.Factory.newInstance();
        endDateXml.setDateValue
          ( StringMethods.convertDateString(params[2], timePattern) );
        XmlAnySimpleType paramEndDate = commandParams.addNewParam();
        paramEndDate.set( endDateXml );
        whereValue = params[3];
      }
      XmlString whereXml = XmlString.Factory.newInstance();
      whereXml.setStringValue(whereValue);
      XmlAnySimpleType paramWhere = commandParams.addNewParam();
      paramWhere.set( whereXml );

    } else if ( ! type.equals("database") && ! type.equals("guids") ) {
      logger.warn( "Skipping unknown query type " + type );
    }
    return query;
  }
  /**
   * Read in query store file and set temp dir
   *
   * @param filePath  A string containing the path to an XML file.
   *
   * @param queryDir A directory where cached queries can be stored
   *
   * @throws edu.sdsc.inca.ConfigurationException if unable to open queries
   */
  private void init(String filePath, String queryDir)
    throws ConfigurationException {

    this.tempDir = queryDir;
    this.setFile( filePath );
    if ( qsFile.exists() ) {
      try {
        logger.debug( "Reading query store: " + qsFile );
        queryStore = QueryStoreDocument.Factory.parse( qsFile, (new XmlOptions()).setLoadStripWhitespace() );
        for( Query q : queryStore.getQueryStore().getQueryArray() ) {
          DepotQuery cache = this.readQuery( q );
          if ( cache != null ) {
            logger.debug( "Adding query " + q.getName() + " to memory" );
            cachedQueries.put( q.getName(), cache );
          }
        }
      } catch ( Exception e ) {
        logger.error
          ( "Unable to parse query store file " + qsFile.getAbsolutePath(), e );
        clear();
      }
    } else {
      clear();
    }
  }

  /**
   * Read a query from the provided xml object and parse it into a DepotQuery
   * object.
   *
   * @param q  A xml object that describes a query
   *
   * @return A new DepotQuery object that can be used to fetch results
   */
  public DepotQuery readQuery( Query q ) {
    DepotQuery depotQuery = null;

    int cachePeriod = 0;
    String reloadAt = "*";
    if ( q.isSetCache() ) {
      cachePeriod = q.getCache().getReloadPeriod().intValue();
      reloadAt = q.getCache().getReloadAt();
    }
    if ( q.getType().getCommand().equals("database") ) {
      depotQuery = new DepotQuery
        ( cachePeriod, reloadAt, this.tempDir, q.getName(), "queryDatabase" );
    } else if ( q.getType().getCommand().equals("guids") ) {
      depotQuery = new DepotQuery
        ( cachePeriod, reloadAt, this.tempDir, q.getName(), "queryGuids" );
    } else if ( q.getType().getCommand().equals("hql") ) {
      XmlString hql = ((XmlString)q.getType().getParams().getParamArray(0) );
      depotQuery = new DepotQuery
        ( cachePeriod, reloadAt, this.tempDir, q.getName(), "queryHql",
          hql.getStringValue() );
    } else if ( q.getType().getCommand().equals("instance") ) {
      XmlString nickname = ((XmlString)q.getType().getParams().getParamArray(0));
      XmlString resource = ((XmlString)q.getType().getParams().getParamArray(1));
      XmlString target = ((XmlString)q.getType().getParams().getParamArray(2));
      XmlDate collected = ((XmlDate)q.getType().getParams().getParamArray(3));
      depotQuery = new DepotQuery
        ( cachePeriod, reloadAt, this.tempDir, q.getName(), "queryInstance",
          nickname.getStringValue(), resource.getStringValue(),
          target.getStringValue(), collected.getDateValue() );
    } else if ( q.getType().getCommand().equals("instanceById") ) {
      XmlLong instanceId = ((XmlLong)q.getType().getParams().getParamArray(0));
      XmlLong configId = ((XmlLong)q.getType().getParams().getParamArray(1));
      depotQuery = new DepotQuery
        ( cachePeriod, reloadAt, this.tempDir, q.getName(), "queryInstanceById",
          instanceId.getLongValue(),  configId.getLongValue() );
    } else if ( q.getType().getCommand().equals("latest")) {
      XmlString where = ((XmlString)q.getType().getParams().getParamArray(0));
      depotQuery = new DepotQuery
        ( cachePeriod, reloadAt, this.tempDir, q.getName(), "queryLatest",
          where.getStringValue() );
    } else if ( q.getType().getCommand().equals("period") ) {
      if ( q.getType().getParams().sizeOfParamArray() == 2 ) {
        XmlInteger numDays =
          ((XmlInteger)q.getType().getParams().getParamArray(0));
        XmlString where = ((XmlString)q.getType().getParams().getParamArray(1));
        depotQuery = new DepotQuery
          ( cachePeriod, reloadAt, this.tempDir, q.getName(), "queryPeriod",
            numDays.getBigIntegerValue().intValue(), where.getStringValue() );
      } else {
        XmlDate startDate = ((XmlDate)q.getType().getParams().getParamArray(0));
        XmlDate endDate = ((XmlDate)q.getType().getParams().getParamArray(1));
        XmlString where = ((XmlString)q.getType().getParams().getParamArray(2));
        depotQuery = new DepotQuery
          ( cachePeriod, reloadAt, this.tempDir, q.getName(), "queryPeriod",
            startDate.getDateValue(), endDate.getDateValue(),
            where.getStringValue() );
      }
    } else if ( q.getType().getCommand().equals("statusHistory") ) {
      XmlString period = ((XmlString)q.getType().getParams().getParamArray(0));
      if ( q.getType().getParams().sizeOfParamArray() == 3 ) {
        XmlInteger numDays =
          ((XmlInteger)q.getType().getParams().getParamArray(1));
        XmlString where = ((XmlString)q.getType().getParams().getParamArray(2));
        depotQuery = new DepotQuery
          ( cachePeriod, reloadAt, this.tempDir, q.getName(),
            "queryStatusHistory", period.getStringValue(),
            numDays.getBigIntegerValue().intValue(), where.getStringValue() );
      } else {
        XmlDate startDate = ((XmlDate)q.getType().getParams().getParamArray(1));
        XmlDate endDate = ((XmlDate)q.getType().getParams().getParamArray(2));
        XmlString where = ((XmlString)q.getType().getParams().getParamArray(3));
        depotQuery = new DepotQuery
          ( cachePeriod, reloadAt, this.tempDir, q.getName(),
            "queryStatusHistory", period.getStringValue(),
            startDate.getDateValue(), endDate.getDateValue(),
            where.getStringValue() );
      }
    } else {
      logger.warn( "Skipping unknown query type " + q.getType().getCommand() );
    }
    return depotQuery;
  }

}
