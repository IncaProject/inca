package edu.sdsc.inca.util;

/**
 * Exception for applying changes to suites.
 * 
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class SuiteModificationException extends Exception {

  /**
   * Creates a new ReporterManagerException Object.
   */
  public SuiteModificationException() {
  }

  /**
   * Creates a new ReporterManagerException Object.
   *
   * @param s the message
   */
  public SuiteModificationException(final String s) {
    super(s);
  }

  /**
   * Creates a new ReporterManagerException Object.
   *
   * @param s         message
   * @param throwable cause
   */
  public SuiteModificationException(final String s, final Throwable throwable) {
    super(s, throwable);
  }

  /**
   * Creates a new ReporterManagerException Object.
   *
   * @param throwable cause
   */
  public SuiteModificationException(final Throwable throwable) {
    super(throwable);
  }

}
