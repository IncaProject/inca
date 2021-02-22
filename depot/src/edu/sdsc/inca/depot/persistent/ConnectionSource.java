/*
 * ConnectionSource.java
 */
package edu.sdsc.inca.depot.persistent;


import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.cfg.Configuration;


/**
 * A class that provides a source of JDBC <code>Connection</code> objects.
 *
 * @author Paul Hoover
 *
 */
public abstract class ConnectionSource {

  // data fields


  private static final Logger m_baseLog = Logger.getLogger(ConnectionSource.class);


  // public methods


  /**
   * Returns a <code>Connection</code> object from the data source.
   *
   * @return a <code>Connection</code> object
   * @throws SQLException
   */
  public abstract Connection getConnection() throws SQLException;

  /**
   * Releases any resources required by the data source.
   *
   * @throws SQLException
   */
  public abstract void close() throws SQLException;

  /**
   * Returns a set of database properties from the resource located at <code>DATABASE_CONFIG_URL</code>.
   *
   * @return a <code>Properties</code> object containing the set of database properties
   */
  public static Properties getDatabaseConfiguration()
  {
    Configuration config = new Configuration();

    return config.getProperties();
  }


  // protected methods


  /**
   * Loads the database vendor's JDBC driver class, using a property from the provided <code>Properties</code> object.
   *
   * @param configProps a <code>Properties</code> object that contains a value for the key "driverClass"
   * @throws ClassNotFoundException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  protected static void loadDriver(Properties configProps) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException
  {
    String driverClassName = configProps.getProperty("hibernate.connection.driver_class");

    m_baseLog.debug("Loading driver " + driverClassName);

    Class.forName(driverClassName).getDeclaredConstructor().newInstance();
  }
}
