package edu.sdsc.inca.depot.persistent;


import java.util.HashSet;
import java.util.Set;


/**
 * This class transfers Suite objects between memory and the DB.
 */
public class SuiteDAO extends DAO {

  /**
   * Returns a Suite from the DB with the same field values as one specified,
   * null if no such Suite appears in the DB.
   *
   * @param s a Suite that contains field values used in the retrieval
   * @return an Suite from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static Suite load(Suite s) throws PersistenceException {
    String query = "select s from Suite as s where s.guid = :p0";
    return (Suite)DAO.selectUnique(query, new Object[] {s.getGuid()});
  }

  /**
   * Returns a Suite object from the DB with the id specified, or null if no
   * such object appears in the DB.
   *
   * @param id the id of the object to be retrieved
   * @return an object from the DB marked with the given id
   * @throws PersistenceException on err
   */
  public static Suite load(Long id) throws PersistenceException {
    String query = "select s from Suite as s where s.id = :p0";
    return (Suite)DAO.selectUnique(query, new Object[] { id });
  }

  /**
   * Returns a Suite object from the DB with the same field values as one
   * specified, or the saved version of the specified object if no such object
   * appears in the DB.  Synchronized to avoid race conditions that could
   * result in DB duplicates.
   *
   * @param s an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static synchronized Suite loadOrSave(Suite s) throws PersistenceException {
    Suite dbSuite = SuiteDAO.load(s);

    if (dbSuite != null)
      return dbSuite;

    Set<SeriesConfig> savedConfigs = new HashSet<SeriesConfig>();

    for (SeriesConfig sc : s.getSeriesConfigs())
      savedConfigs.add(SeriesConfigDAO.loadOrSave(sc));

    s.setSeriesConfigs(savedConfigs);

    return (Suite)DAO.save(s);
  }

  /**
   * A wrapper around DAO.update that handles the necessary casting.
   *
   * @param s the Suite to update
   * @return s for convenience
   * @throws PersistenceException on error
   */
  public static Suite update(Suite s) throws PersistenceException {
    return (Suite)DAO.update(s);
  }

}
