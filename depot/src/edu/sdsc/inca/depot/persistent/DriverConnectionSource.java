/*
 * DriverConnectionSource.java
 */
package edu.sdsc.inca.depot.persistent;


import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;


/**
 * Provides <code>Connection</code> objects from the JDBC <code>DriverManager</code> class.
 *
 * @author Paul Hoover
 *
 */
public class DriverConnectionSource extends ConnectionSource {

  // data fields


  private static final Logger m_log = Logger.getLogger(DriverConnectionSource.class);
  private final String m_dbUsername;
  private final String m_dbPassword;
  private final String m_dbUrl;


  // constructors


  /**
   * Constructs an instance of the object using the set of properties returned from
   * the {@link ConnectionSource#getDatabaseConfiguration() getDatabaseConfiguration}
   * method of the <code>ConnectionSource</code> class.
   * @throws ClassNotFoundException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   *
   */
  public DriverConnectionSource() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException
  {
    this(getDatabaseConfiguration());
  }

  /**
   * Constructs an instance of the object using the given set of properties.
   *
   * @param configProps
   * @throws ClassNotFoundException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public DriverConnectionSource(Properties configProps) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException
  {
    loadDriver(configProps);

    m_dbUsername = configProps.getProperty("hibernate.connection.username");
    m_dbPassword = configProps.getProperty("hibernate.connection.password");
    m_dbUrl = configProps.getProperty("hibernate.connection.url");

    m_log.debug("url: " + m_dbUrl);
    m_log.debug("user: " + m_dbUsername);
  }


  // public methods


  /**
   * Returns a <code>Connection</code> object from the data source.
   *
   * @return a <code>Connection</code> object
   * @throws SQLException
   */
  @Override
  public Connection getConnection() throws SQLException
  {
    Properties connProps = new Properties();

    connProps.setProperty("user", m_dbUsername);
    connProps.setProperty("password", m_dbPassword);

    Connection connection = DriverManager.getConnection(m_dbUrl, connProps);

    return connection;
  }

  /**
   * Releases any resources required by the data source. The <code>DriverManager</code> class
   * doesn't require manual resource management, so this method does nothing.
   *
   * @throws SQLException
   */
  @Override
  public void close() throws SQLException
  {
    // do nothing
  }
}
