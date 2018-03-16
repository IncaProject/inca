package edu.sdsc.inca.depot.persistent;

/**
 * This class transfers ComparisonResult objects between memory and the DB.
 */
public class ComparisonResultDAO extends DAO {

  /**
   * Returns a ComparisonResult from the DB with the same field values as one
   * specified, null if no such ComparisonResult appears in the DB.
   *
   * @param cr an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static ComparisonResult load(ComparisonResult cr)
    throws PersistenceException {
    String query = "select cr from ComparisonResult as cr" +
                   " where cr.result = :p0" +
                   " and cr.reportId = :p1" +
                   " and cr.seriesConfigId = :p2";
    Object[] params = {cr.getResult(),cr.getReportId(),cr.getSeriesConfigId()};
    return (ComparisonResult)DAO.selectUnique(query, params);
  }

  /**
   * Returns a ComparisonResult object from the DB with the id specified, null
   * if no such object appears in the DB.
   *
   * @param id the id of the object to be retrieved
   * @return an object from the DB marked with the given id
   * @throws PersistenceException on err
   */
  public static ComparisonResult load(Long id) throws PersistenceException {
    String query = "select cr from ComparisonResult as cr where cr.id = " + id;
    ComparisonResult result = (ComparisonResult)DAO.selectUnique(query, null);
    // TODO: this hack deals with the special "null comparison" that we used to
    // enter in the II table.  Remove the hack once we're sure it's not needed.
    return result!=null && result.getReportId().longValue()<0 ? null : result;
  }

  /**
   * Returns a ComparisonResult object from the DB with the same field values
   * as one specified, or the saved version of the specified object if no such
   * object appears in the DB.  Synchronized to avoid race conditions that
   * could result in DB duplicates.
   *
   * @param cr an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static synchronized ComparisonResult loadOrSave(ComparisonResult cr)
    throws PersistenceException {
    ComparisonResult dbCr = ComparisonResultDAO.load(cr);
    return dbCr == null ? (ComparisonResult)DAO.save(cr) : dbCr;
  }

  /**
   * A wrapper around DAO.update that handles the necessary casting.
   *
   * @param cr the object to update
   * @return cr for convenience
   * @throws PersistenceException on error
   */
  public static ComparisonResult update(ComparisonResult cr)
      throws PersistenceException {
    return (ComparisonResult)DAO.update(cr);
  }

}
