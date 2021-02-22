/*
 * XmlBeanObject.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;


/**
 *
 * @author Paul Hoover
 *
 */
public abstract class XmlBeanObject {

  // data fields


  private static final Logger m_log = Logger.getLogger(XmlBeanObject.class);


  // public methods


  /**
   * Copies information from an Inca schema XmlBean object so that this object
   * contains equivalent information.
   *
   * @param o the XmlBean object to copy
   * @return this, for convenience
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public abstract XmlBeanObject fromBean(XmlObject o) throws IOException, SQLException, PersistenceException;

  /**
   * Returns an Inca schema XmlBean object that contains information equivalent
   * to this object.
   *
   * @return an XmlBean object that contains equivalent information
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public abstract XmlObject toBean() throws IOException, SQLException, PersistenceException;

  /**
   * Returns XML that represents the information in this object.
   *
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public String toXml() throws IOException, SQLException, PersistenceException
  {
    return toBean().xmlText();
  }


  // protected methods


  /**
   *
   * @param value
   * @param max
   * @param label
   * @return
   */
  protected String normalize(String value, int max, String label)
  {
    if (value == null || value.isEmpty())
      value = Row.DB_EMPTY_STRING;
    else if (value.length() > max) {
      m_log.warn("Truncating " + label + " '" + value + "' to fit into a DB table column of " + max + " chars");

      value = value.substring(0, max);
    }

    return value;
  }
}
