/*
 * MonitoredSet.java
 */
package edu.sdsc.inca.depot.persistent;


import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


/**
 *
 * @author Paul Hoover
 *
 * @param <E>
 */
abstract class MonitoredSet<E> implements Set<E> {

  /**
   *
   */
  private class SetIterator implements Iterator<E> {

    private final Iterator<E> m_setIter;
    private E m_currentElement;


    // constructors


    /**
     *
     * @param setIter
     */
    protected SetIterator(Iterator<E> setIter)
    {
      m_setIter = setIter;
    }


    // public methods


    /**
     *
     * @return
     */
    public boolean hasNext()
    {
      return m_setIter.hasNext();
    }

    /**
     *
     * @return
     */
    public E next()
    {
      m_currentElement = m_setIter.next();

      return m_currentElement;
    }

    /**
     *
     */
    public void remove()
    {
      addSetRemoveOp(m_currentElement);

      m_setIter.remove();
    }
  }


  private final Set<E> m_set;


  // constructors


  /**
   *
   * @param set
   */
  MonitoredSet(Set<E> set)
  {
    m_set = set;
  }


  // public methods


  /**
   *
   * @param o
   * @return
   */
  public boolean add(E o)
  {
    if (m_set.add(o)) {
      addSetAddOp(o);

      return true;
    }

    return false;
  }

  /**
   *
   * @param c
   * @return
   */
  public boolean addAll(Collection<? extends E> c)
  {
    boolean changed = false;

    for (E element : c) {
      if (add(element))
        changed = true;
    }

    return changed;
  }

  /**
   *
   */
  public void clear()
  {
    m_set.clear();

    addSetClearOp();
  }

  /**
   *
   * @param o
   * @return
   */
  public boolean contains(Object o)
  {
    return m_set.contains(o);
  }

  /**
   *
   * @param c
   * @return
   */
  public boolean containsAll(Collection<?> c)
  {
    return m_set.containsAll(c);
  }

  /**
   *
   * @return
   */
  public boolean isEmpty()
  {
    return m_set.isEmpty();
  }

  /**
   *
   * @return
   */
  public Iterator<E> iterator()
  {
    return new SetIterator(m_set.iterator());
  }

  /**
   *
   * @param o
   * @return
   */
  @SuppressWarnings("unchecked")
  public boolean remove(Object o)
  {
    if (m_set.remove(o)) {
      addSetRemoveOp((E) o);

      return true;
    }

    return false;
  }

  /**
   *
   * @param c
   * @return
   */
  public boolean removeAll(Collection<?> c)
  {
    boolean changed = false;

    for (Object element : c) {
      if (remove(element))
        changed = true;
    }

    return changed;
  }

  /**
   *
   * @param c
   * @return
   */
  public boolean retainAll(Collection<?> c)
  {
    boolean changed = false;

    for (E element : m_set) {
      if (!c.contains(element)) {
        remove(element);

        changed = true;
      }
    }

    return changed;
  }

  /**
   *
   * @return
   */
  public int size()
  {
    return m_set.size();
  }

  /**
   *
   * @return
   */
  public Object[] toArray()
  {
    return m_set.toArray();
  }

  /**
   *
   * @param a
   * @return
   */
  public <T> T[] toArray(T[] a)
  {
    return m_set.toArray(a);
  }

  /**
   *
   * @param other
   * @return
   */
  @Override
  public boolean equals(Object other)
  {
    return m_set.equals(other);
  }

  /**
   *
   * @return
   */
  @Override
  public int hashCode()
  {
    return m_set.hashCode();
  }


  // protected methods


  /**
   *
   * @param element
   */
  protected abstract void addSetAddOp(E element);

  /**
   *
   * @param element
   */
  protected abstract void addSetRemoveOp(E element);

  /**
   *
   */
  protected abstract void addSetClearOp();
}
