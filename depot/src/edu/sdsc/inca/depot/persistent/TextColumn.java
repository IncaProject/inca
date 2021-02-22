/*
 * TextColumn.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;


/**
 * Represents a <code>LONGVARCHAR</code> column from a database table.
 *
 * @author Paul Hoover
 *
 */
class TextColumn extends StreamColumn<String> {

  // nested classes


  /**
   *
   */
  private static class TextValue extends MemoryValue<String> implements StreamValue<String> {

    // public methods


    /**
     *
     * @return
     */
    @Override
    public InputStream getValueAsStream()
    {
      if (isNull())
        return null;

      return new ByteArrayInputStream(m_memValue.getBytes());
    }

    /**
     *
     * @param value
     * @throws IOException
     */
    @Override
    public void setValue(InputStream value) throws IOException
    {
      String newValue = null;

      if (value != null) {
        int charsRead;
        char[] readBuffer = new char[8192];
        Reader inStream = new InputStreamReader(value);
        Writer outStream = new CharArrayWriter();

        while ((charsRead = inStream.read(readBuffer, 0, readBuffer.length)) > 0)
          outStream.write(readBuffer, 0, charsRead);

        newValue = outStream.toString();
      }

      setValue(newValue);
    }

    /**
     * Sets the value of the object using a row from a <code>ResultSet</code> object. The row used
     * is the one indicated by the current position of the <code>ResultSet</code> object's cursor.
     *
     * @param value the <code>ResultSet</code> object that contains the row
     * @param index the offset in the row that indicates the column whose value will be assigned to this object
     * @throws IOException
     * @throws SQLException
     */
    @Override
    public void setValue(ResultSet value, int index) throws IOException, SQLException
    {
      String colValue = value.getString(index);

      setValue(colValue);
    }

    /**
     *
     * @param statement
     * @param index
     * @throws SQLException
     */
    @Override
    public void setParamValue(PreparedStatement statement, int index) throws SQLException
    {
      statement.setCharacterStream(index, new StringReader(m_memValue), m_memValue.length());
    }

    /**
     *
     * @return
     */
    @Override
    public long getLength()
    {
      return m_memValue.length();
    }
  }


  // constructors


  /**
   * Constructs a column representation and assigns a <code>null</code> value to it.
   *
   * @param name the name of the column
   * @param nullable whether the column lacks or has a <code>NOT NULL</code> constraint
   * @param owner a reference to the <code>Row</code> object that owns this object
   */
  TextColumn(String name, boolean nullable, Row owner)
  {
    super(name, nullable, owner);
  }

  /**
   * Constructs a column representation and assigns the given value to it.
   *
   * @param name the name of the column
   * @param nullable whether the column lacks or has a <code>NOT NULL</code> constraint
   * @param owner a reference to the <code>Row</code> object that owns this object
   * @param value an initial value to assign to the object
   */
  TextColumn(String name, boolean nullable, Row owner, String value)
  {
    super(name, nullable, owner, value);
  }


  // protected methods


  /**
   * Returns the SQL data type of the column
   *
   * @return the SQL data type of the column
   */
  @Override
  protected int getType()
  {
    return Types.LONGVARCHAR;
  }

  /**
   *
   * @return
   */
  @Override
  protected Value<String> createValue()
  {
    return new TextValue();
  }
}
