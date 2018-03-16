package edu.sdsc.inca.repository;

import edu.sdsc.inca.util.RpmPackage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * The ReporterPackage class provides access to a single reporter package.
 * Typically these packages will be taken from an Inca reporter repository, but
 * the interface allows the manipulation of of stand-alone packages as well.
 */
public class ReporterPackage {

  /**
   * Instantiates a new ReporterPackage located via url.
   * @param url base URL for repository; file:, ftp:, http: supported
   */
  public ReporterPackage(URL url) throws IOException {
    InputStream is = url.openConnection().getInputStream();
    RpmPackage rpm = new RpmPackage(is);
    this.props = new Properties();
    RpmPackage.RpmProperty[] headerProps = rpm.getHeader();
    for(int i = 0; i < headerProps.length; i++) {
      RpmPackage.RpmProperty rp = headerProps[i];
      this.props.put(rp.getTagAsString(false), rp.getValueAsString());
    }
    this.reporterStream = new GZIPInputStream(is);
  }

  /**
   * Returns the set of properties that describes the reporter package--a
   * superset of the properties stored for the package in a repository.
   */
  public Properties getProperties() {
    return this.props;
  }

  /**
   * Returns an input stream from which the package gzip file may be read.
   */
  public GZIPInputStream getReporterStream() {
    return this.reporterStream;
  }

  /**
   * Returns the package signature, used to verify the contents of the package
   * gzip file.
   */
  public String getSignature() {
    return ""; /* TODO */
  }

  private GZIPInputStream reporterStream;
  private Properties props;

}
