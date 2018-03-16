package edu.sdsc.inca.depot.persistent;

import org.apache.xmlbeans.XmlObject;

/** Represents host-specific execution information derived from a Report. */
public class RunInfo extends PersistentObject {

  /** id set iff this object is stored in the DB. */
  private Long id;

  /** Persistent fields. */
  private String hostname;
  private String workingDir;
  private String reporterPath;

  /** Relations. */
  private ArgSignature argSignature;

  /**
   * Default constructor.
   */
  public RunInfo() {
    this("", "", "");
  }

  /**
   * Full constructor.
   */
  public RunInfo(String hostname, String workingDir, String reporterPath) {
    this.setHostname(hostname);
    this.setWorkingDir(workingDir);
    this.setReporterPath(reporterPath);
    this.setArgSignature(new ArgSignature());
  }

  /**
   * Copies information from an Inca schema XmlBean Report object so that this
   * object contains equivalent information.
   *
   * @param o the XmlBean Report object to copy
   * @return this, for convenience
   */
  public PersistentObject fromBean(XmlObject o) {
    return this.fromBean((edu.sdsc.inca.dataModel.util.Report)o);
  }

  /**
   * Copies information from an Inca schema XmlBean Report object so that this
   * object contains equivalent information.
   *
   * @param r the XmlBean Report object to copy
   * @return this, for convenience
   */
  public RunInfo fromBean(edu.sdsc.inca.dataModel.util.Report r) {
    this.setHostname(r.getHostname());
    this.setWorkingDir(r.getWorkingDir());
    this.setReporterPath(r.getReporterPath());
    this.setArgSignature(new ArgSignature().fromBean(r.getArgs()));
    return this;
  }

  /**
   * Retrieve the id -- null if not yet connected to database.
   *
   * @return The Long representation of the DB ID
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Set the id.  Hibernate use only.
   *
   * @param id The DB ID.
   */
  protected void setId(Long id) {
    this.id = id;
  }

  /**
   * Get the RunInfo hostname.
   *
   * @return the RunInfo hostname
   */
  public String getHostname() {
    return this.hostname;
  }

  /**
   * Set the RunInfo hostname.
   *
   * @param hostname the RunInfo hostname
   */
  public void setHostname(String hostname) {
    if(hostname == null || hostname.equals("")) {
      hostname = DB_EMPTY_STRING;
    }
    this.hostname = truncate(hostname, MAX_DB_STRING_LENGTH, "hostname");
  }

  /**
   * Get the RunInfo working directory.
   *
   * @return the RunInfo working directory
   */
  public String getWorkingDir() {
    return this.workingDir;
  }

  /**
   * Set the RunInfo working directory.
   *
   * @param workingDir the RunInfo working directory
   */
  public void setWorkingDir(String workingDir) {
    if(workingDir == null || workingDir.equals("")) {
      workingDir = DB_EMPTY_STRING;
    }
    this.workingDir = truncate(workingDir, MAX_DB_STRING_LENGTH, "working dir");
  }

  /**
   * Get the RunInfo reporter relative path.
   *
   * @return the RunInfo reporter relative path
   */
  public String getReporterPath() {
    return this.reporterPath;
  }

  /**
   * Set the RunInfo reporter relative.
   *
   * @param reporterPath the RunInfo reporter relative path
   */
  public void setReporterPath(String reporterPath) {
    if(reporterPath == null || reporterPath.equals("")) {
      reporterPath = DB_EMPTY_STRING;
    }
    this.reporterPath =
      truncate(reporterPath, MAX_DB_STRING_LENGTH, "reporter path");
  }

  /**
   * Get the signature for the set of Args associated with this RunInfo.
   *
   * @return the Series arg signature
   */
  public ArgSignature getArgSignature() {
    return this.argSignature;
  }

  /**
   * Set the signature for the set of Args associated with this RunInfo.
   * Hibernate only.
   *
   * @param sig the Series arg signature
   */
  public void setArgSignature(ArgSignature sig) {
    this.argSignature = sig;
  }

  /**
   * Returns a Inca schema XmlBean object that contains information equivalent
   * to this object.
   *
   * @return an XmlBean object that contains equivalent information
   */
  public XmlObject toBean() {
    return null; // No XmlBean equivalent to RunInfo
  }

  /**
   * Compares another object to this Report for logical equality.
   *
   * @param o the object to compare
   * @return true iff the comparison object represents the same Report
   */
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    } else if(!(o instanceof RunInfo)) {
      return false;
    }
    return this.toString().equals(o.toString());
  }

  /**
   * Override of the default toString method.
   */
  public String toString() {
    return this.hostname + "," + this.workingDir + "," +
           this.reporterPath + "," + this.argSignature;
  }

  /**
   * Returns XML that represents the information in this object.
   */
  public String toXml() {
    // RunInfo has no corresponding XML bean.  This implementation is
    // for debugging purposes.
    return "<runInfo>\n" +
           "  <hostname>" + this.getHostname() + "</hostname>\n" +
           "  <workingDir>" + this.getWorkingDir() + "</workingDir>\n" +
           "  <reporterPath>" + this.getReporterPath() + "</reporterPath>\n" +
           "</runInfo>\n";
  }

}
