package edu.sdsc.inca.depot.persistent;

import java.util.Date;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * This is the base class for the depot DAOs--classes that transfer objects
 * between memory and the depot database.  Since child DAO classes will each
 * deal with a different DB class, this class doesn't define abstract methods
 * for children to override.  Instead, the set of methods that child classes
 * should define is described in comments below.  Which methods from this set
 * a child class actually implements depends on which are needed; the
 * description is intended to provide a convention to follow.
 *
 * @author cmills
 * @author jhayes
 */
public abstract class DAO {

  /* Description of child class methods: */

  /*
   * Returns an object from the DB with the same field values as one specified,
   * null if no such object appears in the DB.
   *
   * @param o an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   *
  public static DBObject load(DBObject o) throws PersistenceException;
   */

  /*
   * Returns an object from the DB with the id specified, null if no such
   * object appears in the DB.
   *
   * @param id the id of the object to be retrieved
   * @return an object from the DB marked with the given id
   * @throws PersistenceException on err
  public static DBObject load(Long id) throws PersistenceException;
   */

  /*
   * Returns an object from the DB with the same field values as one specified,
   * or the saved version of the specified object if no such object appears in
   * the DB.  Synchronized to avoid race conditions that could result in DB
   * duplicates.
   *
   * @param o an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   *
  public static synchronized DBObject loadOrSave(DBObject o)
    throws PersistenceException;
   */

  /*
   * A wrapper around DAO.update that handles the necessary casting.
   *
   * @param o the object to store
   * @return o for convenience
   * @throws PersistenceException on error
   *
  public static DBObject update(DBObject o) throws PersistenceException;
   */

  protected static Logger logger = Logger.getLogger(DAO.class);
  private static final int FETCH_SIZE = 100;

  /**
   * Return the unique object selected by a Hibernate query.
   *
   * @param query the Hibernate query; see Hibernate doc for format
   * @param params a set of objects to plug into the query in spots marked
   *               as :p0, :p1, etc.  May be null if the query has no params.
   * @return the object selected by the query; null if none
   * @throws PersistenceException on error
   */
  public static Object selectUnique(String query, Object[] params) throws PersistenceException {

    Iterator i = DAO.selectMultiple(query, params);

    if (!i.hasNext())
      return null;

    Object result = i.next();

    if (i.hasNext())
      logger.warn("Unique query returned more than one object");

    return result;
  }

  /**
   * Return the set of objects selected by a Hibernate query.
   *
   * @param query the Hibernate query; see Hibernate doc for format
   * @param params a set of objects to plug into the query in spots marked
   *               as :p0, :p1, etc.  May be null if the query has no params.
   * @return the (possibly empty) set of objects selected by the query
   * @throws PersistenceException on error
   */
  public static Iterator selectMultiple(String query, Object[] params) throws PersistenceException {

    Session session = HibernateUtil.getCurrentSession();
    Query q = session.createQuery(query);

    q.setFetchSize(FETCH_SIZE);

    if (params != null) {
      for (int i = 0; i < params.length; i++) {
        Object param = params[i];
        String name = "p" + i;
        if (param instanceof Boolean) {
          q.setBoolean(name, ((Boolean)param).booleanValue());
        } else if (param instanceof Date) {
          q.setTimestamp(name, (Date)param);
        } else if (param instanceof Integer) {
          q.setInteger(name, ((Integer)param).intValue());
        } else if (param instanceof Long) {
          q.setLong(name, ((Long)param).longValue());
        } else if (param instanceof String) {
          q.setString(name, (String)param);
        } else {
          q.setEntity(name, param);
        }
      }
    }
    try {
      long before = System.currentTimeMillis();
      Iterator result = q.iterate();
      long after = System.currentTimeMillis();
      StringBuilder paramImages = new StringBuilder();
      if(params != null) {
        for(int i = 0; i < params.length; i++) {
          paramImages.append((i == 0 ? "" : ", ") + "'" + params[i] + "'");
        }
      }
      long overhead = System.currentTimeMillis();
      paramImages.append("]");
      logger.info("Query '" + query + "'[" + paramImages + "] returned" +
                  (result.hasNext() ? "" : " 0 objects") + " in " + (after - before) +
                  " msecs " + "(+" + (overhead - after) + " msecs debugging)");
      return result;
    } catch(HibernateException e) {
      logger.error("Query '" + query + "' threw exception: " + e);
      throw new PersistenceException("DB access exception: " + e);
    }
  }

  /**
   * Stores an object that has not previously been saved in the database.
   *
   * @param o the object to store
   * @return o, for convenience
   * @throws PersistenceException on error
   */
  public static Object save(Object o) throws PersistenceException {
    long before = System.currentTimeMillis();
    Session session = HibernateUtil.getCurrentSession();
    HibernateUtil.beginTransaction();
    try {
      session.save(o);
      HibernateUtil.commitTransaction();
    } catch(HibernateException e) {
      HibernateUtil.rollbackTransaction();
      HibernateUtil.closeSession();
      throw new PersistenceException("DB save exception: " + e);
    }
    long after = System.currentTimeMillis();
    logger.debug("Saved '" + o.toString() + "' in " + (after - before) +
                 " msecs");
    return o;
  }

  /**
   * Updates an object that has previously been saved in the database.
   *
   * @param o the object to store
   * @return o, for convenience
   * @throws PersistenceException on error
   */
  public static Object update(Object o) throws PersistenceException {
    long before = System.currentTimeMillis();
    Session session = HibernateUtil.getCurrentSession();
    HibernateUtil.beginTransaction();
    try {
      session.update(o);
      HibernateUtil.commitTransaction();
    } catch(HibernateException e) {
      HibernateUtil.rollbackTransaction();
      HibernateUtil.closeSession();
      throw new PersistenceException("DB update exception: " + e);
    }
    long after = System.currentTimeMillis();
    logger.debug("Updated '" + o.toString() + "' in " + (after - before) +
                 " msecs");
    return o;
  }

}
