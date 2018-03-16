package edu.sdsc.inca.depot.persistent;

import edu.sdsc.inca.dataModel.util.AnyXmlSequence;
import java.util.Calendar;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;


/**
 * This class represents the output from a Single Reporter.
 *
 * @author Cathie Olschanowsky
 */
public class Report extends PersistentObject {

  /** id set iff this object is stored in the DB. */
  private Long id;

  /** Persistent fields. */
  private Boolean exit_status;
  private String exit_message; // Long string
  private String bodypart1; // Long string
  private String bodypart2; // Long string
  private String bodypart3; // Long string
  private String stderr;

  /** Relations. */
  private Series series;
  private RunInfo runInfo;

  private static Logger logger = Logger.getLogger(Report.class);

  /**
   * Default constructor.
   */
  public Report() {
    this(new Boolean(true), "", "", null);
  }

  /**
   * Full constructor.
   *
   * @param exit_status  the exist_status of the run
   * @param exit_message the exit_message of the run
   * @param body         the body is the bulk of the results
   * @param series which series does this report belong to?
   */
  public Report(Boolean exit_status, String exit_message, String body,
                Series series) {
    this.setExit_status(exit_status);
    this.setExit_message(exit_message);
    this.setBody(body);
    this.setStderr("");
    this.setSeries(series);
    this.setRunInfo(new RunInfo());
  }

  /**
   * Copies information from an Inca schema XmlBean Report object so that this
   * object contains equivalent information.
   *
   * @param o the XmlBean Report object to copy
   * @return this, for convenience
   */
  public PersistentObject fromBean(XmlObject o) {
    return fromBean((edu.sdsc.inca.dataModel.util.Report)o);
  }

  /**
   * Copies information from an Inca schema XmlBean Report object so that this
   * object contains equivalent information.
   *
   * @param r the XmlBean Report object to copy
   * @return this, for convenience
   */
  public Report fromBean(edu.sdsc.inca.dataModel.util.Report r) {
    this.setBody(r.getBody().xmlText());
    this.setExit_status(Boolean.valueOf(r.getExitStatus().getCompleted()));
    this.setExit_message(r.getExitStatus().getErrorMessage());
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
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Retrieve the exit status of this report.
   *
   * @return exit_status
   */
  public Boolean getExit_status() {
    return this.exit_status;
  }

  /**
   * Set the exit status of this report.
   *
   * @param exit_status true for pass false for fail
   */
  public void setExit_status(Boolean exit_status) {
    this.exit_status = exit_status;
  }

  /**
   * Retrieve the exit message of this report.
   *
   * @return the exit message
   */
  public String getExit_message() {
    return this.exit_message;
  }

  /**
   * Set the exit message of this report.
   *
   * @param exit_message the text of the exit message
   */
  public void setExit_message(String exit_message) {
    if(exit_message == null || exit_message.equals("")) {
      exit_message = DB_EMPTY_STRING;
    }
    this.exit_message =
      truncate(exit_message, MAX_DB_LONG_STRING_LENGTH, "error message");
  }

  /**
   * Retrieve the body of this report as an XML string.
   *
   * @return XML string
   */
  public String getBody() {
    String result = this.getBodypart1();
    String part2 = this.getBodypart2();
    String part3 = this.getBodypart3();
    if(!part2.equals(DB_EMPTY_STRING)) {
      result += part2;
    }
    if(!part3.equals(DB_EMPTY_STRING)) {
      result += part3;
    }
    return result;
  }

  /**
   * Set the body of this report.
   *
   * @param body xml body of report
   */
  public void setBody(String body) {
    if(body == null) {
      body = "";
    }
    int length = body.length();
    if(length <= MAX_DB_LONG_STRING_LENGTH) {
      this.setBodypart1(body);
      this.setBodypart2("");
      this.setBodypart3("");
    } else if(length <= MAX_DB_LONG_STRING_LENGTH * 2) {
      this.setBodypart1(body.substring(0, MAX_DB_LONG_STRING_LENGTH));
      this.setBodypart2(body.substring(MAX_DB_LONG_STRING_LENGTH));
      this.setBodypart3("");
    } else if(length <= MAX_DB_LONG_STRING_LENGTH * 3) {
      this.setBodypart1(body.substring(0, MAX_DB_LONG_STRING_LENGTH));
      this.setBodypart2
        (body.substring(MAX_DB_LONG_STRING_LENGTH,MAX_DB_LONG_STRING_LENGTH*2));
      this.setBodypart3(body.substring(MAX_DB_LONG_STRING_LENGTH * 2));
    } else {
      logger.error("Rejecting too-long report body '" + body + "'");
      this.setBodypart1("");
      this.setBodypart2("");
      this.setBodypart3("");
    }
  }

  /**
   * Retrieve the first part of the body of this report.
   */
  public String getBodypart1() {
    return this.bodypart1;
  }

  /**
   * Set the second part of the body of this report.
   */
  public void setBodypart1(String bodypart) {
    if(bodypart == null || bodypart.equals("")) {
      bodypart = DB_EMPTY_STRING;
    }
    this.bodypart1 = truncate(bodypart, MAX_DB_LONG_STRING_LENGTH, "stderr");
  }

  /**
   * Retrieve the second part of the body of this report.
   */
  public String getBodypart2() {
    return this.bodypart2;
  }

  /**
   * Set the second part of the body of this report.
   */
  public void setBodypart2(String bodypart) {
    if(bodypart == null || bodypart.equals("")) {
      bodypart = DB_EMPTY_STRING;
    }
    this.bodypart2 = truncate(bodypart, MAX_DB_LONG_STRING_LENGTH, "stderr");
  }

  /**
   * Retrieve the third part of the body of this report.
   */
  public String getBodypart3() {
    return this.bodypart3;
  }

  /**
   * Set the third part of the body of this report.
   */
  public void setBodypart3(String bodypart) {
    if(bodypart == null || bodypart.equals("")) {
      bodypart = DB_EMPTY_STRING;
    }
    this.bodypart3 = truncate(bodypart, MAX_DB_LONG_STRING_LENGTH, "stderr");
  }

  /**
   * Get the Series that this report is part of.
   *
   * @return The Series this report belongs to.
   */
  public Series getSeries() {
    return this.series;
  }

  /**
   * set the report series that this report is part of.
   *
   * @param series the Series this report belongs to
   */
  public void setSeries(Series series) {
    this.series = series;
  }

  /**
   * Get the RunInfo associated with this Report.
   *
   * @return The RunInfo this report belongs to.
   */
  public RunInfo getRunInfo() {
    return this.runInfo;
  }

  /**
   * Set the RunInfo associated with this Report.
   *
   * @param runInfo the RunInfo this report belongs to
   */
  public void setRunInfo(RunInfo runInfo) {
    this.runInfo = runInfo;
  }

  /**
   * Retrieve the contents of STDERR output by the reporter.
   *
   * @return what the reporter sent to STDERR
   */
  public String getStderr() {
    return this.stderr;
  }

  /**
   * Set the contents of stderr -- can be used for querying or by Hibernate.
   *
   * @param stderr contents of stderr
   */
  public void setStderr(String stderr) {
    if(stderr == null || stderr.equals("")) {
      stderr = DB_EMPTY_STRING;
    }
    this.stderr = truncate(stderr, MAX_DB_LONG_STRING_LENGTH, "stderr");
  }

  /**
   * Returns a Inca schema XmlBean Report object that contains information
   * equivalent to this object.
   *
   * @return an XmlBean Report object that contains equivalent information
   */
  public XmlObject toBean() {
    edu.sdsc.inca.dataModel.util.Report result =
      edu.sdsc.inca.dataModel.util.Report.Factory.newInstance();
    result.setGmt(Calendar.getInstance()); // Client should overwrite this
    result.setHostname(this.getRunInfo().getHostname());
    result.setName(this.getSeries().getReporter());
    result.setVersion(this.getSeries().getVersion());
    result.setWorkingDir(this.getRunInfo().getWorkingDir());
    result.setReporterPath(this.getRunInfo().getReporterPath());
    result.setArgs((edu.sdsc.inca.dataModel.util.Args)this.getRunInfo().getArgSignature().toBean());
    try {
      String body = this.getBody();
      if(body.equals(DB_EMPTY_STRING)) {
        result.setBody(AnyXmlSequence.Factory.newInstance());
      } else {
        result.setBody(AnyXmlSequence.Factory.parse(body));
      }
    } catch (XmlException e) {
      logger.error("Unable to parse body from DB:", e);
    }
    result.addNewExitStatus();
    result.getExitStatus().setCompleted(this.getExit_status().booleanValue());
    result.getExitStatus().setErrorMessage(this.getExit_message());
    return result;
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
    } else if(!(o instanceof Report)) {
      return false;
    }
    Report report = (Report)o;
    return
      this.getBody().equals(report.getBody()) &&
      this.getExit_message().equals(report.getExit_message()) &&
      this.getExit_status().equals(report.getExit_status()) &&
      this.getSeries().equals(report.getSeries()) &&
      this.getStderr().equals(report.getStderr()) &&
      this.getRunInfo().equals(report.getRunInfo());
  }

  public int hashCode() {
    return 29 * this.getBody().hashCode() +
                this.getExit_message().hashCode() +
                this.getExit_status().hashCode() +
                this.getSeries().hashCode() +
                this.getStderr().hashCode() +
                this.getRunInfo().hashCode();
  }

}
