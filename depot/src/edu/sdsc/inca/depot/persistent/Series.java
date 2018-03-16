package edu.sdsc.inca.depot.persistent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.xmlbeans.XmlObject;

/**
 * @author Cathie Olschanowsky
 * @author Jim Hayes
 *
 * This class represents a series of reports.  Each report in a series shares
 * the same resource and context
 */
public class Series extends PersistentObject {

  /** id set iff this object is stored in the DB. */
  private Long id;

  /** Persistent fields. */
  private String reporter;
  private String version;
  private String uri;     // Long string
  private String context; // Long string
  private boolean nice;
  private String resource;
  private String targetHostname;
  private String instanceTableName;
  private String linkTableName;
  // See TODO file note about limits

  /** Relations. */
  private ArgSignature argSignature;
  private Collection<Report> reports;
  private Set<SeriesConfig> seriesConfigs;

  /**
   * Default constructor.
   */
  public Series() {
    this("", "", "");
  }

  /**
   * Constructs a new Series containing the specified values.
   *
   * @param resource the Series resource
   * @param context the Series context string
   * @param reporter the Series reporter
   */
  public Series(String resource, String context, String reporter) {
    this.setReporter(reporter);
    this.setVersion("");
    this.setUri(reporter);
    this.setContext(context);
    this.setNice(false);
    this.setResource(resource);
    this.setTargetHostname(null);
    this.setArgSignature(new ArgSignature());
    this.setReports(new ArrayList<Report>());
    this.setSeriesConfigs(new HashSet<SeriesConfig>());
  }

  /**
   * Copies information from an Inca schema XmlBean Series object so that this
   * object contains equivalent information.
   *
   * @param o the XmlBean Series object to copy
   * @return this, for convenience
   */
  public PersistentObject fromBean(XmlObject o) {
    if(o instanceof edu.sdsc.inca.dataModel.util.Report) {
      return this.fromBean((edu.sdsc.inca.dataModel.util.Report)o);
    } else {
      return this.fromBean((edu.sdsc.inca.dataModel.util.Series)o);
    }
  }

  /**
   * Copies information from an Inca schema XmlBean Report object so that this
   * object contains equivalent information.
   *
   * @param r the XmlBean Report object to copy
   * @return this, for convenience
   */
  public Series fromBean(edu.sdsc.inca.dataModel.util.Report r) {
    this.setReporter(r.getName());
    this.setVersion(r.getVersion());
    this.setResource(r.getHostname());
    this.getArgSignature().fromBean(r.getArgs());
    return this;
  }

  /**
   * Copies information from an Inca schema XmlBean Series object so that this
   * object contains equivalent information.
   *
   * @param s the XmlBean Series object to copy
   * @return this, for convenience
   */
  public Series fromBean(edu.sdsc.inca.dataModel.util.Series s) {
    this.setReporter(s.getName());
    if(s.getVersion() != null) {
      this.setVersion(s.getVersion());
    }
    this.setUri(s.getUri());
    this.setContext(s.getContext());
    this.setNice(s.getNice());
    if(s.getArgs() != null) {
      this.getArgSignature().fromBean(s.getArgs());
    }
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
   * Get the Series reporter name.
   *
   * @return the Series reporter name
   */
  public String getReporter() {
    return this.reporter;
  }

  /**
   * Set the Series reporter name.
   *
   * @param reporter the Series reporter name
   */
  public void setReporter(String reporter) {
    if(reporter == null || reporter.equals("")) {
      reporter = DB_EMPTY_STRING;
    }
    this.reporter = truncate(reporter, MAX_DB_STRING_LENGTH, "reporter");
  }

  /**
   * Get the Series reporter version.
   *
   * @return the Series reporter version
   */
  public String getVersion() {
    return this.version;
  }

  /**
   * Set the Series reporter version.
   *
   * @param version the Series reporter version
   */
  public void setVersion(String version) {
    if(version == null || version.equals("")) {
      version = DB_EMPTY_STRING;
    }
    this.version = truncate(version, MAX_DB_STRING_LENGTH, "version");
  }

  /**
   * Get the Series reporter URI.
   *
   * @return the Series reporter URI
   */
  public String getUri() {
    return this.uri;
  }

  /**
   * Set the Series reporter URI.
   *
   * @param uri the Series reporter URI
   */
  public void setUri(String uri) {
    if(uri == null || uri.equals("")) {
      uri = DB_EMPTY_STRING;
    }
    this.uri = truncate(uri, MAX_DB_LONG_STRING_LENGTH, "uri");
  }

  /**
   * Get the Series context string.
   *
   * @return the Series context string
   */
  public String getContext() {
    return this.context;
  }

  /**
   * Set the Series context string.
   *
   * @param context the Series context string
   */
  public void setContext(String context) {
    if(context == null || context.equals("")) {
      context = DB_EMPTY_STRING;
    }
    this.context = truncate(context, MAX_DB_LONG_STRING_LENGTH, "context");
  }

  /**
   * Get the indicator as to whether the Series reporter should be run niced.
   *
   * @return the Series nice indicator
   */
  public boolean getNice() {
    return this.nice;
  }

  /**
   * Set the indicator as to whether the Series reporter should be run niced.
   *
   * @param nice the Series nice indicator
   */
  public void setNice(boolean nice) {
    this.nice = nice;
  }

  /**
   * Get the name of the resource where this Series runs.
   *
   * @return the Series resource
   */
  public String getResource() {
    return this.resource;
  }

  /**
   * Set the name of the resource where this Series runs.
   *
   * @param resource the Series resource
   */
  public void setResource(String resource) {
    if(resource == null || resource.equals("")) {
      resource = DB_EMPTY_STRING;
    }
    this.resource = truncate(resource, MAX_DB_STRING_LENGTH, "resource");
  }

  /**
   *
   * @return the Series target hostname
   */
  public String getTargetHostname() {
    return this.targetHostname;
  }

  /**
   *
   * @param target the Series target hostname
   */
  public void setTargetHostname(String target) {

    if(target == null || target.equals("")) {
    	target = DB_EMPTY_STRING;
    }

    this.targetHostname = truncate(target, MAX_DB_STRING_LENGTH, "targetHostname");
  }

  /**
   *
   * @return
   */
  public String getInstanceTableName() {
    return this.instanceTableName;
  }

  /**
   *
   * @param name
   */
  void setInstanceTableName(String name) {
    this.instanceTableName = name;
  }

  /**
   *
   * @return
   */
  public String getLinkTableName() {
    return this.linkTableName;
  }

  /**
   *
   * @param name
   */
  void setLinkTableName(String name) {
    this.linkTableName = name;
  }

  // NOTE: the use of an ArgSignature with this class is an optimization that
  // may do more harm than good.  The public API hides the ArgSignature.  The
  // setArgs method is declared for future use, since hibernate presently uses
  // setArgSignature instead.

  /**
   * Get the set of Args associated with this Series.
   *
   * @return the Series set of Args
   */
  public Set<Arg> getArgs() {
    return this.argSignature.getArgs();
  }

  /**
   * Set the set of Args associated with this Series.  Hibernate only.
   *
   * @param args the Series set of Args
   */
  protected void setArgs(Set<Arg> args) {
    this.argSignature.setArgs(args);
  }

  /**
   * Get the signature for the set of Args associated with this Series.
   *
   * @return the Series arg signature
   */
  public ArgSignature getArgSignature() {
    return this.argSignature;
  }

  /**
   * Set the signature for the set of Args associated with this Series.
   *
   * @param sig the Series arg signature
   */
  public void setArgSignature(ArgSignature sig) {
    this.argSignature = sig;
  }

  /**
   * Get the set of Reports associated with this Series.  NOTE: this method can
   * be very expensive, and it uses lazy initialization, so make sure that this
   * Series is associated with a session before calling it.
   *
   * @return the Series associated Reports
   */
  public Collection<Report> getReports() {
    return this.reports;
  }

  /**
   * Set the set of Reports associated with this Series.  Hibernate only.
   *
   * @param reports the Series associated Reports
   */
  protected void setReports(Collection<Report> reports) {
    this.reports = reports;
  }

  /**
   * Add a Report to the set of those associated with this Series.
   *
   * @param report a Report to associate
   */
  public void addReport(Report report) {
    // Make sure it's not already in there
    for(Iterator<Report> i = this.reports.iterator(); i.hasNext(); ) {
      if(i.next() == report) {
        return;
      }
    }
    this.reports.add(report);
    report.setSeries(this);
  }

  /**
   * Get the set of SeriesConfigs associated with this Series.
   *
   * @return the Series set of SeriesConfigs
   */
  public Set<SeriesConfig> getSeriesConfigs() {
    return this.seriesConfigs;
  }

  /**
   * Set the set of SeriesConfigs associated with this Series.  Hibernate only.
   *
   * @param configs the Series set of SeriesConfigs
   */
  protected void setSeriesConfigs(Set<SeriesConfig> configs) {
    this.seriesConfigs = configs;
  }

  /**
   * Add a SeriesConfig to the set of those associated with this Series.  Used
   * by SeriesConfig.setSeries to take (shared) ownership of this Series.
   *
   * @param sc a SeriesConfig to associate
   */
  public void addSeriesConfig(SeriesConfig sc) {
    // Make sure it's not already in there
    for(Iterator<SeriesConfig> i = this.seriesConfigs.iterator(); i.hasNext(); ) {
      if(i.next() == sc) {
        return;
      }
    }
    this.seriesConfigs.add(sc);
  }

  /**
   * Returns a Inca schema XmlBean Series object that contains information
   * equivalent to this object.
   *
   * @return an XmlBean Series object that contains equivalent information
   */
  public XmlObject toBean() {
    edu.sdsc.inca.dataModel.util.Series result =
      edu.sdsc.inca.dataModel.util.Series.Factory.newInstance();
    result.setName(this.getReporter());
    if(this.getVersion() != null) {
      result.setVersion(this.getVersion());
    }
    if(this.getUri() != null) {
      result.setUri(this.getUri());
    }
    result.setArgs
      ((edu.sdsc.inca.dataModel.util.Args)this.getArgSignature().toBean());
    result.setContext(this.getContext());
    result.setNice(this.getNice());
    return result;
  }


  /**
   * Returns a string representation of this Series.
   *
   * @return a string representation
   */
  public String toString() {
    return this.getResource()+","+this.getContext()+","+this.getVersion();
  }

  /**
   * Compares another object to this Series for logical equality.
   *
   * @param o the object to compare
   * @return true iff the comparison object represents the same Series
   */
  public boolean equals(Object o) {
    if(o == this) {
      return true;
    } else if(!(o instanceof Series)) {
      return false;
    }
    return this.toString().equals(o.toString());
  }

  /**
   * Calculate a hash code using the same fields that where used in equals.
   *
   * @return a hash code for the Series
   */
  public int hashCode() {
    return
      29 * this.getResource().hashCode() +
           this.getContext().hashCode() +
           this.getVersion().hashCode();
  }

  /**
   * Generates a phony report for this Series.  Useful for testing.
   */
  public String generateReport() {
    return this.generateReport(new Date());
  }

  /**
   * Generates a phony report for this Series.  Useful for testing.
   */
  public String generateReport(Date d) {
    String argsXml = "  <args>\n";
    // Current time in ISO 8601 format
    String gmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(d);
    gmt = gmt.substring(0,gmt.length()-2) + ":" + gmt.substring(gmt.length()-2);
    for(Iterator<Arg> it = this.getArgs().iterator(); it.hasNext(); ) {
      Arg a = (Arg)it.next();
      argsXml += "    <arg>\n" +
                 "      <name>" + a.getName() + "</name>\n" +
                 "      <value>" + a.getValue() + "</value>\n" +
                 "    </arg>\n";
    }
    argsXml += "  </args>\n";
    return
      "<rep:report xmlns:rep=\"http://inca.sdsc.edu/dataModel/report_2.1\">\n" +
      "  <gmt>" + gmt + "</gmt>\n" +
      "  <hostname>" + this.getResource() + "</hostname>\n" +
      "  <name>" + this.getReporter() + "</name>\n" +
      "  <version>" + this.getVersion() + "</version>\n" +
      "  <workingDir>/tmp</workingDir>\n" +
      "  <reporterPath>" + this.getReporter() + "</reporterPath>\n" +
      argsXml +
      "  <body><some>xml</some></body>\n" +
      "  <exitStatus>\n" +
      "    <completed>true</completed>\n" +
      "  </exitStatus>\n" +
      "</rep:report>\n";
  }

  /**
   * Returns a phony Series with a given resource and context.  Useful for
   * testing.
   *
   * @param resource the series resource
   * @param context the series context
   * @param args the number of phony arguments to generate
   * @return a new Series
   */
  public static Series generate(String resource, String context, int args) {
    Series result = new Series(resource, context, "any.reporter");
    Set<Arg> argSet = new HashSet<Arg>();
    for(int i = 0; i < args; i++) {
      argSet.add(new Arg("arg" + i, "" + (i * 3)));
    }
    result.getArgSignature().setArgs(argSet);
    return result;
  }

}
