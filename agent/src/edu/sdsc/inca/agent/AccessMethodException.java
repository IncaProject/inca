package edu.sdsc.inca.agent;

/**
 * Since each access method will raise exceptions specific
 * to that access method, this class provides a way to encapsulate those
 * errors so that calling code can separate out remote failures w/o using
 * the very general Exception class.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class AccessMethodException extends Exception {

  /**
   * Creates a new AccessMethodException Object.
   */
  public AccessMethodException() {
  }

  /**
   * Creates a new AccessMethodException Object.
   *
   * @param s the message
   */
  public AccessMethodException(final String s) {
    super(s);
  }

  /**
   * Creates a new AccessMethodException Object.
   *
   * @param s         message
   * @param throwable cause
   */
  public AccessMethodException(final String s, final Throwable throwable) {
    super(s, throwable);
  }

  /**
   * Creates a new AccessMethodException Object.
   *
   * @param throwable cause
   */
  public AccessMethodException(final Throwable throwable) {
    super(throwable);
  }

}
