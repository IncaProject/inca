package edu.sdsc.inca.depot.persistent;


/**
 * This class transfers SeriesConfig objects between memory and the DB.
 */
public class SeriesConfigDAO extends DAO {

  /**
   * Returns a SeriesConfig object from the DB with the id specified, null if
   * no such object appears in the DB.
   *
   * @param id the id of the object to be retrieved
   * @return an object from the DB marked with the given id
   * @throws PersistenceException on err
   */
  public static SeriesConfig load(Long id) throws PersistenceException {
    String query = "select sc from SeriesConfig as sc where sc.id = " + id;
    return (SeriesConfig)DAO.selectUnique(query, null);
  }

  /**
   * Returns a SeriesConfig object from the DB with the same field values as
   * one specified, or the saved version of the specified object if no such
   * object appears in the DB.  Synchronized to avoid race conditions that
   * could could result in DB duplicates.
   *
   * @param sc an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static synchronized SeriesConfig loadOrSave(SeriesConfig sc)
    throws PersistenceException {
    // SeriesConfig presently has no load-by-query method; go straight to save
    sc.setSeries(SeriesDAO.loadOrSave(sc.getSeries()));

    return (SeriesConfig)DAO.save(sc);
  }

  /**
   * A wrapper around DAO.update that handles the necessary casting.
   *
   * @param sc the SeriesConfig to update
   * @return sc for convenience
   * @throws PersistenceException on error
   *
   */
  public static SeriesConfig update(SeriesConfig sc)
    throws PersistenceException {
    return (SeriesConfig)DAO.update(sc);
  }

}
