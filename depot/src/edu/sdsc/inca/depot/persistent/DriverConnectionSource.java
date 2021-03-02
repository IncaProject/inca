/*
 * DriverConnectionSource.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
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
   *
   * @throws IOException
   */
  public DriverConnectionSource() throws IOException
  {
    this(getDatabaseConfiguration());
  }

  /**
   * Constructs an instance of the object using the given set of properties.
   *
   * @param configProps
   */
  public DriverConnectionSource(Properties configProps)
  {
    String dbUrl = configProps.getProperty("jdbcUrl");

    if (dbUrl == null) {
      String className = configProps.getProperty("dataSourceClassName");
      String host = configProps.getProperty("dataSource.serverName");
      String port = configProps.getProperty("dataSource.portNumber");
      String database = configProps.getProperty("dataSource.databaseName");

      m_dbUrl = constructJdbcUrl(className, host, port, database);
      m_dbUsername = configProps.getProperty("dataSource.user");
      m_dbPassword = configProps.getProperty("dataSource.password");
    }
    else {
      m_dbUrl = dbUrl;
      m_dbUsername = configProps.getProperty("username");
      m_dbPassword = configProps.getProperty("password");
    }

    m_log.debug("url: " + m_dbUrl + ", user: " + m_dbUsername);
  }


  // public methods


  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getConnection() throws SQLException
  {
    Properties connProps = new Properties();

    connProps.setProperty("user", m_dbUsername);
    connProps.setProperty("password", m_dbPassword);

    return DriverManager.getConnection(m_dbUrl, connProps);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close()
  {
    // do nothing
  }


  // private methods


  /**
   *
   * @param name
   * @param host
   * @param port
   * @param database
   * @return
   */
  private String constructJdbcUrl(String name, String host, String port, String database)
  {
    if (name.contains("hsqldb"))
      return "jdbc:hsqldb:file:" + database;

    if (host == null || host.isBlank())
      host = "localhost";

    if (port != null && !port.isBlank())
      port = ":" + port;
    else
      port = "";

    if (name.contains("mariadb"))
      return "jdbc:mariadb://" + host + port + "/" + database;
    else if (name.contains("mysql"))
      return "jdbc:mysql://" + host + port + "/" + database;
    else if (name.contains("oracle"))
      return "jdbc:oracle:thin:@" + host + port + ":" + database;
    else if (name.contains("postgresql"))
      return "jdbc:postgresql://" + host + port + "/" + database;
    else
      return null;
  }
}
