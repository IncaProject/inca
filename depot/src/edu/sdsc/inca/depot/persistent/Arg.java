package edu.sdsc.inca.depot.persistent;

import org.apache.xmlbeans.XmlObject;

/**
 * This class represents a command line input to a reporter.
 *
 * This class is a persistent object.  It has unique constraints on the
 * name value combination.  This means that on save there will be an exception
 * thrown if the combo already exists.
 */
public class Arg extends PersistentObject {

  /** id set iff this object is stored in the DB. */
  private Long id;

  /** Persistent fields. */
  private String name;
  private String value;

  /**
   * Default constructor.
   */
  public Arg() {
    this("", "");
  }

  /**
   * Creates an Arg object with the associated name and value.
   *
   * @param name  The name of the command line argument
   * @param value The value of the command line argument
   */
  public Arg(String name, String value) {
    this.setName(name);
    this.setValue(value);
  }

  /**
   * Copies information from an Inca schema XmlBean Arg object so that this
   * object contains equivalent information.
   *
   * @param o the XmlBean Arg object to copy
   * @return this, for convenience
   */
  public PersistentObject fromBean(XmlObject o) {
    return this.fromBean((edu.sdsc.inca.dataModel.util.Args.Arg)o);
  }

  /**
   * Copies information from an Inca schema XmlBean Arg object so that this
   * object contains equivalent information.
   *
   * @param a the XmlBean Arg object to copy
   * @return this, for convenience
   */
  public Arg fromBean(edu.sdsc.inca.dataModel.util.Args.Arg a) {
    this.setName(a.getName());
    this.setValue(a.getValue());
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
   * retrieve the input name.
   *
   * @return The name of the command line argument.
   */
  public String getName() {
    return this.name;
  }

  /**
   * set the input name.
   *
   * @param name The name of the command line argument.
   */
  public void setName(String name) {
    if(name == null || name.equals("")) {
      name = DB_EMPTY_STRING;
    }
    this.name = truncate(name, MAX_DB_STRING_LENGTH, "arg name");
  }

  /**
   * retrieve the input value.
   *
   * @return The value of the command line argument.
   */
  public String getValue() {
    return this.value;
  }

  /**
   * retrieve the input value.
   *
   * @param value The value of the command line argument.
   */
  public void setValue(String value) {
    if(value == null || value.equals("")) {
      value = DB_EMPTY_STRING;
    }
    this.value = truncate(value, MAX_DB_STRING_LENGTH, "arg value");
  }

  /**
   * Returns a Inca schema XmlBean Arg object that contains information
   * equivalent to this object.
   *
   * @return an XmlBean Arg object that contains equivalent information
   */
  public XmlObject toBean() {
    edu.sdsc.inca.dataModel.util.Args.Arg result =
      edu.sdsc.inca.dataModel.util.Args.Arg.Factory.newInstance();
    result.setName(this.getName());
    result.setValue(this.getValue());
    return result;
  }

  /**
   * For debugging purposes.
   *
   * @return A string representation of this class.
   */
  public String toString() {
    return this.getName() + "=" + this.getValue();
  }

  /**
   * Compares another object to this Arg for logical equality.
   *
   * @param o the object to compare
   * @return true iff the comparison object represents the same Arg
   */
  public boolean equals(Object o) {
    if(o == this) {
      return true;
    } else if(!(o instanceof Arg)) {
      return false;
    }
    return this.toString().equals(o.toString());
  }

  /**
   * Calculate a hash code using the same fields that where used in equals.
   * @return a hash code for this object
   */
  public int hashCode() {
    return 29 + (this.getName().hashCode() * this.getValue().hashCode());
  }

}
