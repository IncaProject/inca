/*
 * HikariCPConnectionSource.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;


/**
 *
 * @author Paul Hoover
 *
 */
public class HikariCPConnectionSource extends ConnectionSource {

  // data fields


  private final HikariDataSource m_dataSource;


  // constructors


  /**
   * Constructs an instance of the object using the set of properties returned from
   * the {@link ConnectionSource#getDatabaseConfiguration() getDatabaseConfiguration}
   * method of the <code>ConnectionSource</code> class.
   *
   * @throws IOException
   */
  public HikariCPConnectionSource() throws IOException
  {
    this(getDatabaseConfiguration());
  }

  /**
   * Constructs an instance of the object using the given set of properties.
   *
   * @param configProps
   */
  public HikariCPConnectionSource(Properties configProps)
  {
    HikariConfig config = new HikariConfig(configProps);

    m_dataSource = new HikariDataSource(config);
  }


  // public methods


  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getConnection() throws SQLException
  {
    return m_dataSource.getConnection();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close()
  {
    m_dataSource.close();
  }
}
