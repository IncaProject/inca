/*
 * MemoryValue.java
 */
package edu.sdsc.inca.depot.persistent;


/**
 *
 * @author Paul Hoover
 *
 * @param <T>
 */
abstract class MemoryValue<T> implements Value<T> {

  // data fields


  protected T m_memValue;


  // constructors


  /**
   *
   */
  protected MemoryValue()
  {
    this(null);
  }

  /**
   *
   * @param value
   */
  protected MemoryValue(T value)
  {
    setValue(value);
  }


  // public methods


  /**
   *
   * @param value
   */
  @Override
  public void setValue(T value)
  {
    m_memValue = value;
  }

  /**
   *
   * @return
   */
  @Override
  public T getValue()
  {
    return m_memValue;
  }

  /**
   *
   * @return
   */
  @Override
  public boolean isNull()
  {
    return m_memValue == null;
  }

  /**
   *
   * @param other
   * @return
   */
  @Override
  public boolean isEqual(T other)
  {
    if (m_memValue != null)
      return m_memValue.equals(other);
    else
      return other == null;
  }

  /**
   *
   */
  @Override
  public void reset()
  {
    m_memValue = null;
  }

  /**
   *
   * @return
   */
  @Override
  public String toString()
  {
    return m_memValue.toString();
  }
}
