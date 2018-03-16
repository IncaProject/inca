package edu.sdsc.inca.depot.persistent;


/**
 * @author Cathie Olschanowsky
 *
 * Some problem was detected while trying to interact with the database.
 */
public class PersistenceException extends Exception {

  private static final long serialVersionUID = -4412939829247866394L;


  /**
   * Create the exception with the given message.
   *
   * @param s message
   */
  public PersistenceException(String s) {
    super(s);
  }

  /**
   * Create the exception with the given message and cause.
   *
   * @param s         message
   * @param throwable cause
   */
  public PersistenceException(String s, Throwable throwable) {
    super(s, throwable);
  }

  /**
   *
   * @param throwable
   */
  public PersistenceException(Throwable throwable) {
    super(throwable);
  }
}
