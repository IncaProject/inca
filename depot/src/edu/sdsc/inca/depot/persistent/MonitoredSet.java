/*
 * MonitoredSet.java
 */
package edu.sdsc.inca.depot.persistent;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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


    protected SetIterator(Iterator<E> setIter)
    {
      m_setIter = setIter;
    }


    // public methods


    @Override
    public boolean hasNext()
    {
      return m_setIter.hasNext();
    }

    @Override
    public E next()
    {
      m_currentElement = m_setIter.next();

      return m_currentElement;
    }

    @Override
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
  @Override
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
  @Override
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
  @Override
  public void clear()
  {
    List<E> elements = new ArrayList<E>(m_set);

    m_set.clear();

    addSetClearOp(elements);
  }

  /**
   *
   * @param o
   * @return
   */
  @Override
  public boolean contains(Object o)
  {
    return m_set.contains(o);
  }

  /**
   *
   * @param c
   * @return
   */
  @Override
  public boolean containsAll(Collection<?> c)
  {
    return m_set.containsAll(c);
  }

  /**
   *
   * @return
   */
  @Override
  public boolean isEmpty()
  {
    return m_set.isEmpty();
  }

  /**
   *
   * @return
   */
  @Override
  public Iterator<E> iterator()
  {
    return new SetIterator(m_set.iterator());
  }

  /**
   *
   * @param o
   * @return
   */
  @Override
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
  @Override
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
  @Override
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
  @Override
  public int size()
  {
    return m_set.size();
  }

  /**
   *
   * @return
   */
  @Override
  public Object[] toArray()
  {
    return m_set.toArray();
  }

  /**
   *
   * @param a
   * @return
   */
  @Override
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
  protected abstract void addSetClearOp(List<E> elements);
}
