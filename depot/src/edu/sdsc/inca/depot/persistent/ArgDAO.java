package edu.sdsc.inca.depot.persistent;


/**
 * This class transfers Arg objects between memory and the DB.
 */
public class ArgDAO extends DAO {

  /**
   * Returns an Arg object from the DB with the same field values as one
   * specified, or null if no such object appears in the DB.
   *
   * @param a an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   *
   */
  public static Arg load(Arg a) throws PersistenceException {
    String query = "select a from Arg as a where a.name=:p0 and a.value=:p1";
    return
      (Arg)DAO.selectUnique(query, new Object[] {a.getName(), a.getValue()});
  }

  /**
   * Returns an Arg object from the DB with the id specified, or null if no
   * such object appears in the DB.
   *
   * @param id the id of the object to be retrieved
   * @return an object from the DB marked with the given id
   * @throws PersistenceException on err
   *
   */
  public static Arg load(Long id) throws PersistenceException {
    String query = "select a from Arg as a where a.id=:p0";
    return
      (Arg)DAO.selectUnique(query, new Object[] { id });
  }

  /**
   * Returns an Arg object from the DB with the same field values as one
   * specified, or the saved version of the specified object if no such
   * object appears in the DB.  Synchronized to avoid race conditions that
   * could result in DB duplicates.
   *
   * @param a an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static synchronized Arg loadOrSave(Arg a) throws PersistenceException {
    Arg dbArg = ArgDAO.load(a);
    return dbArg == null ? (Arg)DAO.save(a) : dbArg;
  }

  /**
   * A wrapper around DAO.update that handles the necessary casting.
   *
   * @param a the object to update
   * @return a, for convenience
   * @throws PersistenceException on error
   */
  public static Arg update(Arg a) throws PersistenceException {
    return (Arg)DAO.update(a);
  }

}
