package edu.sdsc.inca.depot.persistent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class transfers ArgSignature objects between memory and the DB.
 */
public class ArgSignatureDAO extends DAO {

  /**
   * Returns an ArgSignature object from the DB with the same field values as
   * one specified, or null if no such object appears in the DB.
   *
   * @param as an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static ArgSignature load(ArgSignature as) throws PersistenceException {
    String query = "select a from ArgSignature as a where a.signature = :p0";
    return
      (ArgSignature)DAO.selectUnique(query, new Object[] {as.getSignature()});
  }

  /**
   * Returns an ArgSignature object from the DB with the id specified, or null
   * if no such object appears in the DB.
   *
   * @param id the id of the object to be retrieved
   * @return an object from the DB marked with the given id
   * @throws PersistenceException on err
   */
  public static ArgSignature load(Long id) throws PersistenceException {
    String query = "select a from ArgSignature as a where a.id = :p0";
    return
      (ArgSignature)DAO.selectUnique(query, new Object[] { id });
  }

  /**
   * Returns an ArgSignature object from the DB with the same field values as
   * one specified, or the saved version of the specified object if no such
   * object appears in the DB.  Synchronized to avoid race conditions that
   * could result in DB duplicates.
   *
   * @param as an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static synchronized ArgSignature loadOrSave(ArgSignature as)
    throws PersistenceException {
    ArgSignature dbSig = ArgSignatureDAO.load(as);
    if(dbSig != null) {
      return dbSig;
    }
    Set<Arg> dbArgs = new HashSet<Arg>();
    for(Iterator<Arg> it = as.getArgs().iterator(); it.hasNext(); ) {
      dbArgs.add(ArgDAO.loadOrSave((Arg)it.next()));
    }
    return (ArgSignature)DAO.save(new ArgSignature(dbArgs));
  }

  /**
   * A wrapper around DAO.update that handles the necessary casting.
   *
   * @param as the object to update
   * @return as, for convenience
   * @throws PersistenceException on error
   */
  public static ArgSignature update(ArgSignature as)
    throws PersistenceException {
    return (ArgSignature)DAO.update(as);
  }

}
