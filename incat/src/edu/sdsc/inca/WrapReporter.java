package edu.sdsc.inca;

import java.util.Properties;

/**
 * An class that wraps a reporter, represented as a Properties object, with
 * some convenience methods.
 */
public class WrapReporter {

  /**
   * Constructs a new WrapReporter.
   *
   * @param reporter a set of properties that defines the reporter
   */
  public WrapReporter(Properties reporter) {
    this.reporter = reporter;
  }

  /**
   * Returns the value of a single reporter property.
   *
   * @param property the nume of the specified property
   * @return the value of the specified property
   */
  public String getProperty(String property) {
    return this.reporter.getProperty(property);
  }

  /**
   * Sets the value of a single reporter property.
   *
   * @param property the nume of the specified property
   * @param value the value of the specified property
   */
  public void setProperty(String property, String value) {
    this.reporter.setProperty(property, value);
  }

  /**
   * An override of the default toString function.
   */
  public String toString() {
    return this.reporter.getProperty("name");
  }

  public Properties reporter;

}
