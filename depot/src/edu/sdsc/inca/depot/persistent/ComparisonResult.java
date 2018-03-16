package edu.sdsc.inca.depot.persistent;

import org.apache.xmlbeans.XmlObject;

/**
 * Represents results of a comparison between accepted values and report output.
 */
public class ComparisonResult extends PersistentObject {

  /** id set iff this object is stored in the DB. */
  private Long id;

  /** Persistent fields. */
  private String result;
  private Long reportId;
  private Long seriesConfigId;

  /**
   * Default constructor.
   */
  public ComparisonResult() {
    this.setResult("");
    this.setReportId(new Long(-1));
    this.setSeriesConfigId(new Long(-1));
  }

  /**
   * Copies information from an Inca schema XmlBean object so that this object
   * contains equivalent information.
   *
   * @param o the XmlBean object to copy
   * @return this, for convenience
   */
  public PersistentObject fromBean(XmlObject o) {
    return this; // No XmlBean equivalent to ComparisonResult
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
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Return the result of the comparison in the DB.
   *
   * @return the result of this comparison
   */
  public String getResult() {
    return this.result;
  }

  /**
   * Set the result of the comparison.
   *
   * @param result the result of the comparison
   */
  public void setResult(String result) {
    if(result == null || result.equals("")) {
      result = DB_EMPTY_STRING;
    }
    this.result = truncate(result, MAX_DB_STRING_LENGTH, "comparison result");
  }

  /**
   * Get the id of the Report that was used in this comparison.
   *
   * @return the id of the report that was used in this comparison
   */
  public Long getReportId() {
    return this.reportId;
  }

  /**
   * Set the id of the report that was used in this comparison.
   *
   * @param id the id of the report that was used in this comparison
   */
  public void setReportId(Long id) {
    this.reportId = id;
  }

  /**
   * Get the id of the SeriesConfig that was used in this comparison.
   *
   * @return the id of the SeriesConfig that was used in this comparison
   */
  public Long getSeriesConfigId() {
    return this.seriesConfigId;
  }

  /**
   * Set the id of the SeriesConfig that was used in this comparison.
   *
   * @param id the id of the SeriesConfig that was used in this comparison
   */
  public void setSeriesConfigId(Long id) {
    this.seriesConfigId = id;
  }

  /**
   * Returns a Inca schema XmlBean object that contains information equivalent
   * to this object.
   *
   * @return an XmlBean object that contains equivalent information
   */
  public XmlObject toBean() {
    return null; // No XmlBean equivalent to ComparisonResult
  }

  /**
   * Returns XML that represents the information in this object.
   */
  public String toXml() {
    // ComparisonResult has no corresponding XML bean.  This implementation is
    // for debugging purposes.
    return
      "<comparison>\n" +
      "  <result>" + this.getResult() + "</result>\n" +
      "  <reportId>" + this.getReportId() + "</reportId>\n" +
      "  <seriesConfigId>" + this.getSeriesConfigId() + "</seriesConfigId>\n" +
      "</comparison>\n";
  }

}
