package edu.sdsc.inca.consumer;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import edu.sdsc.inca.util.Constants;
import edu.sdsc.inca.util.XmlWrapper;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.AgentClient;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.dataModel.inca.IncaDocument;
import edu.sdsc.inca.dataModel.suite.Suite;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.dataModel.resourceConfig.ResourceConfigDocument;
import edu.sdsc.inca.dataModel.util.Resource;
import edu.sdsc.inca.dataModel.util.Resources;
import edu.sdsc.inca.dataModel.util.Macros;
import edu.sdsc.inca.dataModel.util.SeriesConfig;
import edu.sdsc.inca.dataModel.queryResults.ObjectDocument;

import java.io.IOException;
import java.util.Properties;

/**
 * Special bean that periodically queries and caches the current Inca
 * configuration stored on the agent. This bean starts up a thread to
 * periodically query the
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class AgentBean extends Thread implements java.io.Serializable {
  private static Logger logger = Logger.getLogger(AgentBean.class);

  private int cacheReloadPeriod = 5 * Constants.MILLIS_TO_MINUTE;
  private Properties beanConfig;
  private IncaDocument incaDoc;
  private ResourcesWrapper resources = new ResourcesWrapper();

  /**
   * Create a new AgentBean object.
   */
  public AgentBean() {
    super( "AgentBean" );
  }

  /**
   * Create a new document using the __groupname__ as a replacement for the
   * hostname and includes just the passed in macros
   *
   * @param id  only return resources that are part of this group id
   *
   * @param macros  The macro values to return for these resources
   *
   * @return The processed resource configuration document.
   *
   * @throws edu.sdsc.inca.ConfigurationException if trouble parsing resources
   */
  public ResourceConfigDocument filter( String id, String[] macros )
    throws ConfigurationException {

    ResourcesWrapper resources = this.getResources();
    String[] resourceNames = new String[0];
    if ( id == null ) {
      // return all resources -- but don't retrieve the hosts (i.e.,
      // the resources that have a __groupname__ macro)
      Object[] result = resources.getResourceConfigDocument().selectPath(
        "//resource/macros/macro[name='" + Protocol.GROUPNAME_MACRO + "']/../.."
      );
      if ( result != null ) {
        resourceNames = new String[result.length];
        for ( int i = 0; i < result.length; i++ ) {
          resourceNames[i] = ((Resource)result[i]).getName();
        }
      }
    } else {
      resourceNames = resources.getResources( id, false );
    }
    ResourceConfigDocument newDoc=ResourceConfigDocument.Factory.newInstance();
    newDoc.addNewResourceConfig().addNewResources();
    if ( resourceNames == null || resourceNames.length < 1 ) {
      return newDoc;
    }

    Properties distinctResources = new Properties();
    for ( String resource : resourceNames ) {
      String shortName = resources.getValue(resource, Protocol.GROUPNAME_MACRO);
      if ( shortName == null ) shortName = resource;
      if ( distinctResources.getProperty(shortName) != null ) continue;
      distinctResources.setProperty( shortName, "" );
      logger.debug
        ( "Adding resource " + resource + " with " + shortName + " to config" );
      Resources theseResources = newDoc.getResourceConfig().getResources();
      Resource thisResource = theseResources.addNewResource();
      thisResource.addNewMacros();
      thisResource.setName( shortName );
      logger.debug( "groupname is " + thisResource.getName() );

      for (int j = 0; j < macros.length; j++) {
        String[] values = resources.getValues( resource, macros[j] );
        Macros theseMacros = thisResource.getMacros();
        theseMacros.addNewMacro();
        theseMacros.getMacroArray(j).setName(macros[j]);
        theseMacros.getMacroArray(j).setValueArray(values);
      }
    }
    return newDoc;
  }

  /**
   * Get the current catalog contents.
   *
   * @return An xml document containing the contents of all repositories.
   */
  public String getCatalog() {
    IncaDocument incaDoc = this.getIncaDoc();
    StringBuffer catalogXml = new StringBuffer( "<catalogs>" );
    if ( incaDoc.getInca().isSetRepositories() &&
      incaDoc.getInca().getRepositories().sizeOfRepositoryArray() > 0 ) {

      try {
        AgentClient agentClient = new AgentClient();
        agentClient.setConfiguration( this.beanConfig );
        logger.info( "Contacting agent " + agentClient.getUri() );
        agentClient.connect();
        for (String r:incaDoc.getInca().getRepositories().getRepositoryArray()){
          logger.info( "Getting repository catalog " + r );
          catalogXml.append( agentClient.getCatalogAsXml(r) );
        }
        agentClient.close();
      } catch (Exception e) {
        logger.warn( "Problem fetching catalogs", e );
      }
    }
    catalogXml.append( "</catalogs>" );
    return catalogXml.toString();
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
   * Return cached inca config document.
   *
   * @return Inca config document or null if not loaded yet.
   */
  public synchronized IncaDocument getIncaDoc() {
    return this.incaDoc;
  }

  /**
   * Return the cached resources object.
   *
   * @return  A resources object
   */
  public synchronized ResourcesWrapper getResources() {
    return resources;
  }

  /**
   * Return the cached suite configuration.
   *
   * @return A string containing an xml document containing the configured
   * suites.
   */
  public String getSuites() {
    IncaDocument tempDoc = IncaDocument.Factory.newInstance();
    IncaDocument incaDoc = this.getIncaDoc();
    if ( incaDoc != null && incaDoc.getInca().isSetSuites() ) {
      tempDoc.addNewInca().setSuites( incaDoc.getInca().getSuites() );
    } else {
      tempDoc.addNewInca();
    }
    return XmlWrapper.prettyPrint( tempDoc.getInca().xmlText(), "  " );
  }

  /**
   * Return the suite names as an XML document
   *
   * @return A string containing an xml document containing the suite names
   */
  public String getSuiteNames() {
    IncaDocument tempDoc = IncaDocument.Factory.newInstance();
    IncaDocument incaDoc = this.getIncaDoc();
    if ( incaDoc != null && incaDoc.getInca().isSetSuites() ) {
      IncaDocument.Inca.Suites suites = tempDoc.addNewInca().addNewSuites();
      for( Suite s : incaDoc.getInca().getSuites().getSuiteArray() ) {
        suites.addNewSuite().setName( s.getName() ); 
      }
    } else {
      tempDoc.addNewInca();
    }

    return XmlWrapper.prettyPrint( tempDoc.getInca().xmlText(), "  " );
  }

  public String getUri( ) {
    AgentClient ac = new AgentClient();
    try {
      ac.setConfiguration( this.beanConfig );
      return ac.getUri();
    } catch (ConfigurationException e) {
      return beanConfig.getProperty("agent");
    }
  }

  /**
   * The functionality of the thread.  Periodically, will fetch the
   * configuration from the depot.  Will use the configured
   * cache reload period to wait in between iterations.
   */
  public void run() {
    logger.info( "Starting inca config cache thread" );
    boolean interrupted = false;
    while ( ! interrupted ) {
      try {
        IncaDocument tempDoc = queryIncaConfig();
        if ( tempDoc == null ) {
          logger.error( "Received a null inca configuration from agent" );
          continue;
        }
        synchronized( this ) {
          incaDoc = tempDoc;
        }
        this.configUpdated( this.getIncaDoc() );
        logger.debug(
          "Inca config cache thread sleeping " + this.cacheReloadPeriod + " ms"
        );
      } catch ( Exception e ) {
        logger.error("Unable to retrieve inca configuration", e);
      } finally {
        try {
          Thread.sleep( this.cacheReloadPeriod );
        } catch ( InterruptedException e ) {
          logger.info("Interrupting inca config cache thread" );
          interrupted = true;
        }

      }
    }
    logger.info( "Exitting inca config cache thread" );
  }

  /**
   * Write an INFO message to the consumer log
   *
   * @param message A string to write to the consumer log
   */
  public void setLog( String message ) {
    logger.info( message );
  }

  /**
   * Will execute a run now request
   *
   * @param queryXml A string containing a query object containing a single
   * series config
   *
   * @return true if able to submit run now request and false if not
   *
   * @throws ConfigurationException if problem with configuration
   * @throws IOException if problem contacting agent
   * @throws ProtocolException if problem communicating with agent
   * @throws XmlException if problem parsing queryXml
   */
  public void setRunNow( String queryXml )
    throws ConfigurationException, IOException, ProtocolException,XmlException {

    ObjectDocument queryObject = ObjectDocument.Factory.parse(queryXml);
    SeriesConfig config =
      SeriesConfig.Factory.parse( queryObject.getObject().xmlText());
    // empty schedule object so it is a run now request
    if ( config.getSchedule().isSetCron()) {
      config.getSchedule().unsetCron();
    }
    if ( config.getSchedule().isSetNumOccurs() ) {
      config.getSchedule().unsetNumOccurs();
    }
    if ( config.getSchedule().isSetSuspended()) {
      config.getSchedule().unsetSuspended();
    }

    // create run now suite
    SuiteDocument runNowDoc = SuiteDocument.Factory.newInstance();
    Suite suite = runNowDoc.addNewSuite();
    suite.setName(Protocol.IMMEDIATE_SUITE_NAME);
    suite.setGuid("");
    suite.addNewSeriesConfigs().addNewSeriesConfig();
    suite.getSeriesConfigs().setSeriesConfigArray(0, config);

    AgentClient ac = new AgentClient();
    ac.setConfiguration( this.beanConfig );
    long startTime = Util.getTimeNow();
    logger.info( "Contacting agent " + ac.getUri() + " to run now on " +
                 config.getNickname() );
    ac.connect();
    ac.runNow( Protocol.RUNNOW_TYPE_CONSUMER, runNowDoc.xmlText() );
    ac.close();
    Util.printElapsedTime( startTime, "run now" );
  }

  /**
   * Set the attributes for connecting to the agent and also the maxWait
   * and reload frequency.
   *
   * @param beanConfig  A properties object containing the config options for
   * this object
   */
  public synchronized void setBeanConfig(Properties beanConfig) {
    logger.debug( "Setting configBean configuration" );
    this.beanConfig = beanConfig;

    // configure consumer
    String prop;
    if((prop = beanConfig.getProperty("reload")) != null) {
      this.setCacheReloadPeriod(
        (new Integer(prop)) * Constants.MILLIS_TO_SECOND
      );
    }
    
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
   * Return xml document in string
   *
   * @return   Inca config in xml format.
   */
  public String toString() {
    return XmlWrapper.prettyPrint( incaDoc.xmlText(), "  " );
  }

  // Private Functions

  /**
   * Notify objects that the inca configuration has been updated
   *
   * @param incaConfig   The new Inca configuration
   */
  private void configUpdated( IncaDocument incaConfig ) {
    ResourceConfigDocument tmpDoc =
      ResourceConfigDocument.Factory.newInstance();
    if ( incaConfig.getInca().isSetResourceConfig() ) {
      tmpDoc.setResourceConfig( incaConfig.getInca().getResourceConfig() );
    } else { // create a blank resources document
      tmpDoc.addNewResourceConfig();
      tmpDoc.getResourceConfig().addNewResources();
    }
    long startTime = Util.getTimeNow();
    try {
      ResourcesWrapper temp = new ResourcesWrapper(tmpDoc);
      synchronized ( this ) {
        this.resources = temp;
      }
    } catch (XmlException e) {
      logger.error( "Unable to parse resources" );
    }
    Util.printElapsedTime( startTime, "Resources parse" );
  }

  /**
  * Will fetch the inca configuration from the agent.
  *
  * @return A inca config XML document with the repositories, resources, and
  * suites returned by the agent
  *
  * @throws ConfigurationException if problem with configuration
  * @throws IOException if problem contacting agent
  * @throws ProtocolException if problem communicating with agent
  * @throws XmlException if problem parsing response with agent
  */
 private IncaDocument queryIncaConfig()
   throws ConfigurationException, IOException, ProtocolException,XmlException {

   AgentClient ac = new AgentClient();
   ac.setConfiguration( this.beanConfig );
   long startTime = Util.getTimeNow();
   logger.info( "Contacting agent " + ac.getUri() );
   ac.connect();
   String config = ac.getConfig();
   ac.close();
   Util.printElapsedTime( startTime, "query agent" );
   if( config != null ) {
     return IncaDocument.Factory.parse(config);
   } else {
     return null;
   }
 }

  /**
   * Allows object to be serialized.  Unsure if Jetty will use this or not.
   *
   * @param in  The stream to read the object from
   *
   * @throws IOException if trouble reading object or parsing xml
   * @throws ClassNotFoundException never but needed to override function
   */
  private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    try {
      this.incaDoc = IncaDocument.Factory.parse( in );
      logger.debug( "Read inca config object from stream" );
    } catch (XmlException e) {
      throw new IOException( "Unable to read in inca config from stream" );
    }
  }

  /**
   * Allows object to be serialized.  Unsure if Jetty will use this or not.
   *
   * @param out  The stream to write the object to.
   *
   * @throws IOException if trouble writing object
   */
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    logger.debug( "Writing inca config to stream" );
    this.incaDoc.save(out);
  }


}
