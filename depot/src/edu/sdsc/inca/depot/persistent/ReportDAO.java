package edu.sdsc.inca.depot.persistent;

import org.apache.log4j.Logger;

/**
 * This class transfers Report objects between memory and the DB.
 */
public class ReportDAO extends DAO {

  /**
   * Returns a Report object from the DB with the same field values as one
   * specified, null if no such object appears in the DB.
   *
   * @param r an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static Report load(Report r) throws PersistenceException {
    Series s = r.getSeries();
    if(s.getId() == null) {
      s = SeriesDAO.load(s);
    }
    String query = "select r from Report as r" +
                           " where r.series = :p0" +
                           " and r.exit_status = :p1" +
                           " and r.bodypart1 = :p2" +
                           " and r.bodypart2 = :p3" +
                           " and r.bodypart3 = :p4" +
                           " and r.exit_message = :p5" +
                           " and r.stderr = :p6" +
                           " and r.runInfo = :p7";
    Object[] params = new Object[] {
      s, r.getExit_status(), r.getBodypart1(), r.getBodypart2(),
      r.getBodypart3(), r.getExit_message(), r.getStderr(), r.getRunInfo()
    };
    return (Report)DAO.selectUnique(query, params);
  }

  /**
   * Returns a Report object from the DB with the id specified, null if
   * no such object appears in the DB.
   *
   * @param id the id of the object to be retrieved
   * @return an object from the DB marked with the given id
   * @throws PersistenceException on err
   */
  public static Report load(Long id) throws PersistenceException {
    String query = "select r from Report as r where r.id = " + id;
    return (Report)DAO.selectUnique(query, null);
  }

  /**
   * Returns a Report object from the DB with the same field values as one
   * specified, or the saved version of the specified object if no such object
   * appears in the DB.  Synchronized to avoid race conditions that could
   * result in DB duplicates.
   *
   * @param r an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static synchronized Report loadOrSave(Report r)
    throws PersistenceException {
    Report dbReport = ReportDAO.load(r);
    return dbReport == null ? (Report)DAO.save(r) : dbReport;
  }

  /**
   * A wrapper around DAO.update that handles the necessary casting.
   *
   * @param r the object to update
   * @return r, for convenience
   * @throws PersistenceException on error
   *
   */
  public static Report update(Report r) throws PersistenceException {
    return (Report)DAO.update(r);
  }

}
