/*
 * ConnectionManager.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.util.Properties;


/**
 * Maintains a global instance of a <code>ConnectionSource</code> object.
 *
 * @author Paul Hoover
 *
 */
public class ConnectionManager {

  // data fields


  private static ConnectionSource m_connSource;


  // public methods


  /**
   * Returns the global <code>ConnectionSource</code> object.
   *
   * @return a ConnectionSource object
   */
  public static ConnectionSource getConnectionSource()
  {
    return m_connSource;
  }

  /**
   * Sets the global <code>ConnectionSource</code> object.
   *
   * @param connSource the <code>ConnectionSource</code>
   */
  public static void setConnectionSource(ConnectionSource connSource)
  {
    m_connSource = connSource;
  }

  /**
   * Creates an instance of a <code>ConnectionSource</code> object and sets it as the global
   * instance. The new instance is created using the set of properties returned from the
   * {@link ConnectionSource#getDatabaseConfiguration() getDatabaseConfiguration} method
   * of the <code>ConnectionSource</code> class.
   *
   * @throws IOException
   */
  public static void setConnectionSource() throws IOException
  {
    Properties configProps = ConnectionSource.getDatabaseConfiguration();
    ConnectionSource connSource = new HikariCPConnectionSource(configProps);

    setConnectionSource(connSource);
  }
}
