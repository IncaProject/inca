package edu.sdsc.inca.depot.util;

/**
 * A class that allows an Inca installation to make changes to incoming reports
 * before they're processed by the Depot.
 *
 * @author jhayes
 */
public class ReportFilter {

  protected String context = null;
  protected String resource = null;
  protected String stderr = null;
  protected String stdout = null;
  protected String sysusage = null;
  protected String targetHostname = null;

  /**
   * Constructs a new ReportFilter.
   */
  public ReportFilter() {
    // empty
  }

  /**
   * Returns the filtered reporter context.
   *
   * @return the filtered reporter context
   */
  public String getContext() {
    return this.context;
  }

  /**
   * Returns the filtered reporter resource name.
   *
   * @return the filtered reporter resource name
   */
  public String getResource() {
    return this.resource;
  }

  /**
   * Returns the filtered reporter stderr output.
   *
   * @return the filtered reporter resource stderr output; may be null
   */
  public String getStderr() {
    return this.stderr;
  }

  /**
   * Returns the filtered reporter stdout output.
   *
   * @return the filtered reporter stdout output; must be valid report XML
   */
  public String getStdout() {
    return this.stdout;
  }

  /**
   * Returns the filtered reporter system resource usage report.
   *
   * @return the filtered reporter system resource usage report
   */
  public String getSysusage() {
    return this.sysusage;
  }

  /**
   *
   * @return
   */
  public String getTargetHostname() {
    return this.targetHostname;
  }

  /**
   * Sets the filtered reporter context.
   *
   * @param context the filtered reporter context
   */
  public void setContext(String context) {
    this.context = context;
  }

  /**
   * Sets the filtered reporter resource name.
   *
   * @param resource the filtered reporter resource name
   */
  public void setResource(String resource) {
    this.resource = resource;
  }

  /**
   * Sets the filtered reporter stderr output.
   *
   * @param stderr the filtered reporter resource stderr output; may be null
   */
  public void setStderr(String stderr) {
    this.stderr = stderr;
  }

  /**
   * Sets the filtered reporter stdout output.
   *
   * @param stdout the filtered reporter stdout output; must be valid report XML
   */
  public void setStdout(String stdout) {
    this.stdout = stdout;
  }

  /**
   * Sets the filtered reporter system resource usage report.
   *
   * @param sysusage the filtered reporter system resource usage report
   */
  public void setSysusage(String sysusage) {
    this.sysusage = sysusage;
  }

  /**
   *
   * @param hostname
   */
  public void setTargetHostname(String hostname) {
    this.targetHostname = hostname;
  }

}
