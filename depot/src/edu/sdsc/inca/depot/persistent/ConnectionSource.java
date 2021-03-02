/*
 * ConnectionSource.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.cfg.Configuration;


/**
 * A class that provides a source of JDBC <code>Connection</code> objects.
 *
 * @author Paul Hoover
 *
 */
public abstract class ConnectionSource {

  // data fields


  private static final String DATABASE_CONFIG_FILE = "hikaricp.properties";


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
   */
  public abstract void close();

  /**
   * Returns a set of database properties from the resource located at <code>DATABASE_CONFIG_FILE</code>.
   *
   * @return a <code>Properties</code> object containing the set of database properties
   * @throws IOException
   */
  public static Properties getDatabaseConfiguration() throws IOException
  {
    Properties result = new Properties();
    InputStream configStream = ClassLoader.getSystemClassLoader().getResourceAsStream(DATABASE_CONFIG_FILE);

    if (configStream == null) {
      try {
        configStream = new FileInputStream(DATABASE_CONFIG_FILE);
      }
      catch (FileNotFoundException err) {
        Properties hibernateProps = (new Configuration()).getProperties();
        String dbUsername = hibernateProps.getProperty("hibernate.connection.username");
        String dbPassword = hibernateProps.getProperty("hibernate.connection.password");
        String dbUrl = hibernateProps.getProperty("hibernate.connection.url");

        result.put("username", dbUsername);
        result.put("password", dbPassword);
        result.put("jdbcUrl", dbUrl);

        return result;
      }
    }

    try {
      result.load(configStream);
    }
    finally {
      configStream.close();
    }

    return result;
  }
}
