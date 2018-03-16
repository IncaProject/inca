package edu.sdsc.inca.depot.persistent;


import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.xmlbeans.XmlObject;


/**
 * A Suite is a named collection of SeriesConfigs.
 */
public class Suite extends PersistentObject {

  /** id set iff this object is stored in the DB. */
  private Long id;

  /** Persistent fields. */
  private String name;
  private String guid;
  private String description;
  private int version;

  /** Relations. */
  private Set<SeriesConfig> seriesConfigs;

  /**
   * Default constructor.
   */
  public Suite() {
    this("", "", "");
  }

  /**
   * Constructs a new Suite containing the specified values.
   *
   * @param name the name of the Suite
   * @param guid the guid (agent + name) of the Suite
   * @param description the description of the Suite
   */
  public Suite(String name, String guid, String description) {
    this.setName(name);
    this.setGuid(guid);
    this.setDescription(description);
    this.setVersion(0);
    this.setSeriesConfigs(new HashSet<SeriesConfig>());
  }

  /**
   * Copies information from an Inca schema XmlBean Suite object so that this
   * object contains equivalent information.
   *
   * @param o the XmlBean Suite object to copy
   * @return this, for convenience
   */
  public PersistentObject fromBean(XmlObject o) {
    return this.fromBean((edu.sdsc.inca.dataModel.suite.Suite)o);
  }

  /**
   * Copies information from an Inca schema XmlBean Suite object so that this
   * object contains equivalent information.
   *
   * @param s the XmlBean Suite object to copy
   * @return this, for convenience
   */
  public Suite fromBean(edu.sdsc.inca.dataModel.suite.Suite s) {
    this.setName(s.getName());
    this.setGuid(s.getGuid());
    this.setDescription(s.getDescription());
    if(s.getVersion() != null) {
      this.setVersion(s.getVersion().intValue());
    }
    edu.sdsc.inca.dataModel.util.SeriesConfig[] scs =
      s.getSeriesConfigs().getSeriesConfigArray();
    for(int i = 0; i < scs.length; i++) {
      this.addSeriesConfig(new SeriesConfig().fromBean(scs[i]));
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
   * Get the name of the Suite.
   *
   * @return the Suite name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Set the name of the Suite.
   *
   * @param name the Suite name
   */
  public void setName(String name) {
    if(name == null || name.equals("")) {
      name = DB_EMPTY_STRING;
    }
    this.name = truncate(name, MAX_DB_STRING_LENGTH, "suite name");
  }

  /**
   * Get the guid (agent id + name) of the Suite.
   *
   * @return the Suite guid
   */
  public String getGuid() {
    return this.guid;
  }

  /**
   * Set the guid (agent id + name) of the Suite.
   *
   * @param guid the Suite guid
   */
  public void setGuid(String guid) {
    if(guid == null || guid.equals("")) {
      guid = DB_EMPTY_STRING;
    }
    this.guid = truncate(guid, MAX_DB_STRING_LENGTH, "guid");
  }

  /**
   * Get the (usually user-supplied) Suite description string.
   *
   * @return the Suite description
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Set the (usually user-supplied) Suite description string.
   *
   * @param description the Suite description
   */
  public void setDescription(String description) {
    if(description == null || description.equals("")) {
      description = DB_EMPTY_STRING;
    }
    this.description =
      truncate(description, MAX_DB_STRING_LENGTH, "suite description");
  }

  /**
   * Get the version number of the Suite--the number of times updated.
   *
   * @return the Suite version number
   */
  public int getVersion() {
    return this.version;
  }

  /**
   * Set the version number of the Suite--the number of times updated.
   *
   * @param version the Suite version number
   */
  public void setVersion(int version) {
    this.version = version;
  }

  /**
   * Add 1 to the version number of the Suite--the number of times updated.
   */
  public void incrementVersion() {
    this.version++;
  }

  /**
   * Retrieves a specified SeriesConfig from the associated set.
   *
   * @param i the index into the set of the desired SeriesConfig
   * @return the specified SeriesConfig; null if none
   */
  public SeriesConfig getSeriesConfig(int i) {
    for(Iterator<SeriesConfig> it = seriesConfigs.iterator(); it.hasNext(); i--) {
      SeriesConfig sc = (SeriesConfig)it.next();
      if(i == 0) {
        return sc;
      }
    }
    return null;
  }

  /**
   * Gets the collection of SeriesConfig objects associated with this Suite.
   *
   * @return the set of associated SeriesConfigs
   */
  public Set<SeriesConfig> getSeriesConfigs() {
    return seriesConfigs;
  }

  /**
   * Sets the collection of SeriesConfig objects associated with this Suite.
   * Hibernate only.
   *
   * @param configs the set of associated SeriesConfigs
   */
  protected void setSeriesConfigs(Set<SeriesConfig> configs) {
    this.seriesConfigs = configs;
  }

  /**
   * Add a SeriesConfig to the set of those associated with the Suite.
   *
   * @param sc a SeriesConfig to associate
   */
  public void addSeriesConfig(SeriesConfig sc) {
    this.seriesConfigs.add(sc);
    sc.addSuite(this);
  }

  /**
   * Returns a Inca schema XmlBean Suite object that contains information
   * equivalent to this object.
   *
   * @return an XmlBean Suite object that contains equivalent information
   */
  public XmlObject toBean() {
    edu.sdsc.inca.dataModel.suite.Suite result =
      edu.sdsc.inca.dataModel.suite.Suite.Factory.newInstance();
    result.setName(this.getName());
    result.setGuid(this.getGuid());
    result.setDescription(this.getDescription());
    result.setVersion(BigInteger.valueOf(this.getVersion()));
    edu.sdsc.inca.dataModel.suite.Suite.SeriesConfigs resultSeriesConfigs =
      result.addNewSeriesConfigs();
    Iterator<SeriesConfig> it = this.getSeriesConfigs().iterator();
    for(int i = 0; it.hasNext(); i++) {
      resultSeriesConfigs.addNewSeriesConfig();
      resultSeriesConfigs.setSeriesConfigArray(i, (edu.sdsc.inca.dataModel.util.SeriesConfig)((SeriesConfig)it.next()).toBean());
    }
    return result;
  }

  /**
   * Returns XML text for this Suite.
   *
   * @return XML text
   */
  public String toXml() {
    edu.sdsc.inca.dataModel.suite.SuiteDocument doc =
      edu.sdsc.inca.dataModel.suite.SuiteDocument.Factory.newInstance();
    doc.setSuite((edu.sdsc.inca.dataModel.suite.Suite)this.toBean());
    return doc.xmlText();
  }

  /**
   * Compares another object to this Suite for logical equality.
   *
   * @param o the object to compare
   * @return true iff the comparison object represents the same Suite
   */
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    } else if(!(o instanceof Suite)) {
      return false;
    }
    return this.guid.equals(((Suite)o).getGuid());
  }

  /**
   * Calculate a hash code using the same fields that where used in equals.
   *
   * @return a hash code for the Suite
   */
  public int hashCode() {
    return 29 * this.getGuid().hashCode();
  }

  /**
   * Returns a phony suite with a given number of unrelated (sharing neither
   * resource nor context) SeriesConfigs.  Useful for testing.
   *
   * @param guid the suite guid
   * @param seriesConfigs the number of series configs to populate the suite
   * @return a new Suite
   */
  public static Suite generate(String guid, int seriesConfigs) {
    Suite result = new Suite("A suite name", guid, "auto-suite");
    for(int i = 0; i < seriesConfigs; i++) {
      SeriesConfig sc = SeriesConfig.generate("host" + i, "reporter" + i, 3);
      sc.setNickname(guid + " series " + i);
      result.addSeriesConfig(sc);
    }
    return result;
  }

}
