package edu.sdsc.inca.depot.persistent;

/**
 * This class transfers RunInfo objects between memory and the DB.
 */
public class RunInfoDAO extends DAO {

  /**
   * Returns a RunInfo from the DB with the same field values as one specified,
   * null if no such RunInfo appears in the DB.
   *
   * @param ri an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static RunInfo load(RunInfo ri) throws PersistenceException {
    String query = "select ri from RunInfo as ri inner join ri.argSignature as argSig" +
                   " where ri.hostname = :p0" +
                   " and ri.workingDir = :p1" +
                   " and ri.reporterPath = :p2" +
                   " and argSig.signature = :p3";
    Object[] params = {
      ri.getHostname(), ri.getWorkingDir(), ri.getReporterPath(),
      ri.getArgSignature().getSignature()
    };
    return (RunInfo)DAO.selectUnique(query, params);
  }

  /**
   * Returns a RunInfo from the DB with the id specified, or null if no such
   * RunInfo appears in the DB.
   *
   * @param id the id of the object to be retrieved
   * @return an object from the DB marked with the given id
   * @throws PersistenceException on err
   */
  public static RunInfo load(Long id) throws PersistenceException {
    String query = "select ri from RunInfo as ri where ri.id = :p0";
    Object[] params = { id };
    return (RunInfo)DAO.selectUnique(query, params);
  }

  /**
   * Returns a RunInfo object from the DB with the same field values as one
   * specified, or the saved version of the specified object if no such object
   * appears in the DB.  Synchronized to avoid race conditions that could
   * result in DB duplicates.
   *
   * @param ri an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static synchronized RunInfo loadOrSave(RunInfo ri)
    throws PersistenceException {
    RunInfo dbInfo = RunInfoDAO.load(ri);
    if(dbInfo != null) {
      return dbInfo;
    }
    ri.setArgSignature(ArgSignatureDAO.loadOrSave(ri.getArgSignature()));
    return (RunInfo)DAO.save(ri);
  }

  /**
   * A wrapper around DAO.update that handles the necessary casting.
   *
   * @param ri the object to update
   * @return ri for convenience
   * @throws PersistenceException on error
   */
  public static RunInfo update(RunInfo ri) throws PersistenceException {
    ri.setArgSignature(ArgSignatureDAO.loadOrSave(ri.getArgSignature()));
    return (RunInfo)DAO.update(ri);
  }

}
