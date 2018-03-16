package edu.sdsc.inca.agent;

/**
 * Exception for reporting problems with a remote reporter manager.
 * 
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class ReporterManagerException extends Exception {

  /**
   * Creates a new ReporterManagerException Object.
   */
  public ReporterManagerException() {
  }

  /**
   * Creates a new ReporterManagerException Object.
   *
   * @param s the message
   */
  public ReporterManagerException(final String s) {
    super(s);
  }

  /**
   * Creates a new ReporterManagerException Object.
   *
   * @param s         message
   * @param throwable cause
   */
  public ReporterManagerException(final String s, final Throwable throwable) {
    super(s, throwable);
  }

  /**
   * Creates a new ReporterManagerException Object.
   *
   * @param throwable cause
   */
  public ReporterManagerException(final Throwable throwable) {
    super(throwable);
  }

}
