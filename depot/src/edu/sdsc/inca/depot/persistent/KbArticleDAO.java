/*
 * KbArticleDAO.java
 */
package edu.sdsc.inca.depot.persistent;


import org.hibernate.HibernateException;
import org.hibernate.Session;


/**
 * This class transfers Article objects between memory and the DB.
 *
 * @author Paul Hoover
 *
 */
public class KbArticleDAO extends DAO {

	/**
	 * Returns an Article from the DB with the same field values as one specified,
	 * null if no such Article appears in the DB.
	 *
	 * @param a an Article that contains field values used in the retrieval
	 * @return an Article from the DB that contains the same values
	 * @throws PersistenceException on err
	 */
	public static KbArticle load(KbArticle a) throws PersistenceException
	{
		Long id = a.getId();

		if (id == null)
			return null;

		return load(id);
	}

	/**
	 * Returns an Article object from the DB with the id specified, or null if no
	 * such object appears in the DB.
	 *
	 * @param id the id of the object to be retrieved
	 * @return an object from the DB marked with the given id
	 * @throws PersistenceException on err
	 */
	public static KbArticle load(Long id) throws PersistenceException
	{
		String query = "select a from KbArticle as a where a.id = :p0";

		return (KbArticle)DAO.selectUnique(query, new Object[] { id });
	}

	/**
	 * Returns an Article object from the DB with the same field values as one
	 * specified, or the saved version of the specified object if no such object
	 * appears in the DB.  Synchronized to avoid race conditions that could
	 * result in DB duplicates.
	 *
	 * @param a an Article that contains field values used in the retrieval
	 * @return an object from the DB that contains the same values
	 * @throws PersistenceException on err
	 */
	public static synchronized KbArticle loadOrSave(KbArticle a) throws PersistenceException
	{
		KbArticle dbArticle = KbArticleDAO.load(a);

		return dbArticle == null ? (KbArticle)DAO.save(a) : dbArticle;
	}

	/**
	 * A wrapper around DAO.update that handles the necessary casting.
	 *
	 * @param a the Article to update
	 * @return a for convenience
	 * @throws PersistenceException on error
	 */
	public static KbArticle update(KbArticle a) throws PersistenceException
	{
		return (KbArticle)DAO.update(a);
	}

	/**
	 * Deletes an Article from the database
	 *
	 * @param id the id of the object to be deleted
	 * @throws PersistenceException
	 */
	public static void delete(Long id) throws PersistenceException
	{
		KbArticle toDelete = load(id);

		if (toDelete == null)
			throw new PersistenceException("No KbArticle found with id " + id);

		Session session = HibernateUtil.getCurrentSession();

		HibernateUtil.beginTransaction();

		try {
			session.delete(toDelete);

			HibernateUtil.commitTransaction();
		}
		catch (HibernateException err) {
			HibernateUtil.rollbackTransaction();
			HibernateUtil.closeSession();

			throw new PersistenceException("DB delete exception: " + err);
		}
	}
}
