package edu.sdsc.inca.depot.persistent;


/**
 * @author Cathie Olschanowsky
 *
 * Indicates that a name=value pair for an input was malformed.
 */
public class MalformedInputException extends Throwable {

  private static final long serialVersionUID = 6852884312208980374L;


  /**
   * Throw a new exception with this error string.
   *
   * @param s error
   */
  public MalformedInputException(String s) {
    super(s);
  }

  /**
   * Throw a new exception that restulted from a catch of another.
   *
   * @param s         message
   * @param throwable cause
   */
  public MalformedInputException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
