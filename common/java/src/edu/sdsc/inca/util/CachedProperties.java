package edu.sdsc.inca.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.net.URL;
import org.apache.log4j.Logger;

/**
 * Creates cached Properties fetched according to refreshMins.
 */
public class CachedProperties {
  private static Logger logger = Logger.getLogger(CachedProperties.class);
  private Properties prop = new Properties();
  private long lastRefresh = 0;
  private String fileName;
  private String defaultRefresh;
  private String propPrefix;


  /**
   * Sets the name of the properties file in the classpath and the default
   * number of minutes to fetch it if not specified as a system property.
   */
  public CachedProperties(String propPrefix, String fileName, String defaultRefresh){
    this.fileName = fileName;
    this.defaultRefresh = defaultRefresh;
    this.propPrefix = propPrefix;
  }

  /**
   * Gets property list from file in classpath if cache has expired
   * according to refreshMins.
   *
   * @return  cached Properties
   */
  public synchronized Properties getProperties()  {
    String propFile = System.getProperty(propPrefix + fileName+"File");
    if(propFile == null) {
      propFile  = fileName+".properties";
    }
    String refresh = System.getProperty(propPrefix+fileName+"Refresh");
    if(refresh == null) {
      refresh  = defaultRefresh;
    }
    Integer refreshMins = Integer.parseInt(refresh);
    long minSinceLastRefresh = (System.currentTimeMillis()-lastRefresh)/60000;
    if (minSinceLastRefresh >= refreshMins){
      URL url = ClassLoader.getSystemClassLoader().getResource(propFile);
      if(url == null) {
        logger.error( propFile + " not found in classpath" );
      } else {
        logger.debug( "Located file " + url.getFile()
          + " refresh every " + refreshMins + " min.");
        prop.clear();
        try {
          InputStream is = url.openStream();
          prop.load(is);
          is.close();
        } catch (IOException e){
          logger.error( "Can't load "+fileName+" properties file" );
        }
      }
      lastRefresh = System.currentTimeMillis();
    }
    return prop;
  }
}

