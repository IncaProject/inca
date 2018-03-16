/*
 * ConnectionSource.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.Connection;
import java.sql.DriverManager;
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
public class ConnectionSource {

  protected static final Logger m_logger = Logger.getLogger(ConnectionSource.class);
  private static String m_dbUsername;
  private static String m_dbPassword;
  private static String m_dbUrl;


  static {
    try {
      Configuration config = new Configuration();

      m_dbUsername = config.getProperty("hibernate.connection.username");
      m_dbPassword = config.getProperty("hibernate.connection.password");
      m_dbUrl = config.getProperty("hibernate.connection.url");
    }
    catch (Exception err) {
      err.printStackTrace(System.err);
    }
  }


  // public methods


  /**
   * Returns a <code>Connection</code> object from the data source.
   *
   * @return a <code>Connection</code> object
   * @throws SQLException
   */
  public static Connection getConnection() throws SQLException
  {
    Properties connProps = new Properties();

    connProps.setProperty("user", m_dbUsername);
    connProps.setProperty("password", m_dbPassword);

    return DriverManager.getConnection(m_dbUrl, connProps);
  }
}
