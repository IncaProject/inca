package edu.sdsc.inca.agent;

/**
 * Small class which encapsulates stderr and stdout (since we can only return
 * one value from a function).
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&rt;
 */
public class AccessMethodOutput {
  private String stdout = "";
  private String stderr = "";

  /**
   * Return the value of stderr.
   *
   * @return A string containing the content of stderr.
   */
  public String getStderr() {
    return stderr;
  }

  /**
    * Return the value of stdout.
    *
    * @return A string containing the content of stdout.
    */
   public String getStdout() {
     return stdout;
   }
  
  /**
   * Set the value of stderr.
   *
   * @param stderr  A string containing the content of stderr.
   */
  public void setStderr( String stderr ) {
    this.stderr = stderr;
  }

  /**
   * Set the value of stdout.
   *
   * @param stdout  A string containing the content of stdout.
   */
  public void setStdout( String stdout ) {
    this.stdout = stdout;
  }

}
