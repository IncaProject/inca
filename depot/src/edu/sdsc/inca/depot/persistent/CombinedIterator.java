/*
 * CombinedIterator.java
 */
package edu.sdsc.inca.depot.persistent;


import java.util.Iterator;
import java.util.List;


/**
 *
 * @author Paul Hoover
 *
 */
class CombinedIterator implements Iterator<Object> {

  // data fields


  private final List<Iterator<Object>> m_iterators;
  private int m_currentPosition;
  private Iterator<Object> m_currentIterator;


  // constructors


  /**
   *
   * @param iterators
   */
  public CombinedIterator(List<Iterator<Object>> iterators)
  {
    m_iterators = iterators;
    m_currentPosition = 0;
    m_currentIterator = null;

    advanceIterator();
  }


  // public methods


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasNext()
  {
    return m_currentIterator != null && m_currentIterator.hasNext();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object next()
  {
    Object result;

    if (hasNext()) {
      result = m_currentIterator.next();

      advanceIterator();
    }
    else
      result = null;

    return result;
  }


  // private methods


  /**
   *
   */
  private void advanceIterator()
  {
    if (hasNext())
      return;

    while (m_currentPosition < m_iterators.size()) {
      m_currentIterator = m_iterators.get(m_currentPosition);

      m_currentPosition += 1;

      if (m_currentIterator.hasNext())
        break;
    }
  }
}
