/*
 * CompositeKey.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * An object that represents a <code>WHERE</code> clause search criterion based on a composite
 * <code>KEY</code> constraint.
 *
 * @author Paul Hoover
 *
 */
class CompositeKey implements Criterion {

  // data fields


  private final List<Column<?>> m_columns = new ArrayList<Column<?>>();


  // constructors


  /**
   * Constructs a representation of a search criterion.
   *
   * @param columns an array of <code>Column</code> objects that describes the <code>KEY</code> constraint
   */
  CompositeKey(Column<?>... columns)
  {
    assert columns != null && columns.length > 0;

    for (Column<?> col : columns)
      m_columns.add(col);
  }


  // public methods


  /**
   * Creates a phrase describing the search clause criterion using the names and values of the
   * contained <code>Column</code> objects.
   *
   * @return a phrase describing the criterion
   */
  @Override
  public String getPhrase()
  {
    StringBuilder builder = new StringBuilder();
    Iterator<Column<?>> cols = m_columns.iterator();

    builder.append(cols.next().getName());
    builder.append(" = ?");

    while (cols.hasNext()) {
      builder.append(" AND ");
      builder.append(cols.next().getName());
      builder.append(" = ?");
    }

    return builder.toString();
  }

  /**
   * Sets the value of a parameter in a <code>PreparedStatement</code> object using the value of
   * the contained <code>Column</code> objects.
   *
   * @param statement the <code>PreparedStatement</code> object for which a parameter will be set
   * @param index the offset that indicates the parameter to set
   * @return the next offset to use when setting parameters
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  @Override
  public int setParameter(PreparedStatement statement, int index) throws IOException, SQLException, PersistenceException
  {
    for (Column<?> col : m_columns) {
      col.setParameter(statement, index);

      index += 1;
    }

    return index;
  }

  /**
   * Returns a string representation of the object. The representation is built using the
   * <code>toString</code> methods of the contained <code>Column</code> objects.
   *
   * @return a string representation of the object
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    Iterator<Column<?>> cols = m_columns.iterator();

    builder.append(cols.next().toString());

    while (cols.hasNext()) {
      builder.append(" AND ");
      builder.append(cols.next().toString());
    }

    return builder.toString();
  }
}
