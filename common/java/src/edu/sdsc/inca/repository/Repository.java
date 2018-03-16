package edu.sdsc.inca.repository;

import edu.sdsc.inca.util.StringMethods;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

/**
 * The Repository class provides access to the contents of an Inca reporter
 * repository--the catalog of Reporter packages and the packages themselves.
 */
public class Repository {

  public static final String CATALOG_NAME = "Packages.gz";

  // Instance variables
  protected URL baseUrl;
  protected Properties[] catalog;

  /**
   * Instantiates a repository accessed via a base URL.
   * @param baseURL the URL for the repository
   * @throws IOException if the repository does not exist or has no catalog
   */
  public Repository(URL baseURL) throws IOException {
    this.baseUrl = baseURL;
    refresh();
  }

  /**
   * Instantiates a repository with a catalog generated elsewhere.
   *
   * @param baseURL the URL for the repository
   * @param catalog an array of Properties, each element of which describes one
   *                reporter/package in the repository
   */
  public Repository(URL baseURL, Properties[] catalog) {
    this.baseUrl = baseURL;
    this.catalog = catalog;
  }

  /**
   * Returns true if the given repository url matches this repository url.
   * This function will return true for file urls that match either
   * file: or file:// formats.
   *
   * @param repositoryUrl  A url that we are comparing to this repository.
   * 
   * @return True if the given url matches the repository url and false
   * otherwise.
   */
  public boolean equals( String repositoryUrl ) {
    String thisUrl = baseUrl.toString();
    if ( thisUrl.equals(repositoryUrl) ) {
      return true;
    }
    if ( thisUrl.matches("^file:/\\w.*") ) {
      thisUrl = thisUrl.replaceFirst( "file:", "file://" );
    }
    return thisUrl.equals(repositoryUrl);
  }

  /**
    * Return whether a package exists in the repository with a given property
    * name and value.
    *
    * @param propertyName  Name of the reporter package property.
    * @param value         Value we expect the property to equal
    * @return true if a package exists in the repository with the given property
    * else false.
    */
   public boolean exists( String propertyName, String value ) {
     return getPropertiesByLookup(propertyName, value).length > 0;
   }

  /**
   * Returns information about the contents of the repository.
   * @return an array of Properties, each element of which describes one
   *         reporter/package
   */
  public Properties[] getCatalog() {
    return this.catalog;
  }

  /**
   * Return the latest version of the specified package that exists in this
   * repository or null if it doesn't exist.
   *
   * @param packageName  The name of the package we are looking for.
   *
   * @return The latest version of the specified package or null if it does
   * not exist.
   */
  public String getLatestVersionOfPackage( String packageName ) {
    String latestVersion = "0";
    Properties[] props = this.getPropertiesByLookup(
      Repositories.NAME_ATTR, packageName
    );
    if ( props.length < 1 ) return null;
    for ( int j = 0; j < props.length; j++) {
      String repoVersion = props[j].getProperty( Repositories.VERSION_ATTR );
      if ( repoVersion == null ) repoVersion = "0";
      if ( StringMethods.compareTo(repoVersion, latestVersion) > 0 ) {
        latestVersion = repoVersion;
      }
    }
    return latestVersion;
  }

  /**
   * Retrieve the repository properties for the specified package and version.
   *
   * @param name   The name of the package to retrieve properties on.
   * @param version The version of the package to retrieve properties on.
   *
   * @return  The properties of the specified package and version retrieved
   * from the repository catalog.
   */
  public Properties getProperties( String name, String version ) {
    Properties[] props = this.getPropertiesByLookup(
      Repositories.NAME_ATTR, name
    );
    if ( version == null ) {
      version = this.getLatestVersionOfPackage( name );
    }
    for ( int i = 0; i < props.length; i++) {
      String repoVersion = props[i].getProperty( Repositories.VERSION_ATTR );
      if ( StringMethods.compareTo(repoVersion, version) == 0 ) {
        return props[i];
      }
    }
    return null;
  }

  /**
   * Returns properties for all packages that match the given property
   * name and value.
   *
   * @param property  Name of the reporter package property.
   *
   * @param value     Value we expect the property to equal.
   *
   * @return List of properties from packages that match the given property.
   */
  public Properties[] getPropertiesByLookup( String property, String value ) {
    Vector props = new Vector();
    for ( int i = 0; i < catalog.length; i++ ) {
      String foundValue = catalog[i].getProperty( property );
      if ( foundValue != null && value.equals(foundValue) ) {
        props.add( catalog[i] );
      }
    }
    return (Properties[])props.toArray(new Properties[props.size()]);
  }

  /**
   * Returns the contents of a file located via a specified path in the
   * repository.
   * @param path the relative path to the file
   * @return the file contents
   */
  public byte[] getReporter(String path) throws IOException {
    URL reporterUrl = new URL(this.baseUrl + "/" + path);
    InputStream is = reporterUrl.openConnection().getInputStream();
    byte[] result = new byte[0];
    byte[] block = new byte[1024];
    int readCount;
    while((readCount = is.read(block)) > 0) {
      byte[] expanded = new byte[result.length + readCount];
      System.arraycopy(result, 0, expanded, 0, result.length);
      System.arraycopy(block, 0, expanded, result.length, readCount);
      result = expanded;
    }
    is.close();
    return result;
  }

  /**
   * Returns a binary package located via a specified path in the repository.
   * @param path the relative path to the package file
   * @return the package contents
   */
  public ReporterPackage getReporterPackage(String path) throws IOException {
    return new ReporterPackage(new URL(this.baseUrl + "/" + path));
  }

  /**
   * Returns the base URL through which this Repository is accessed.
   * @return the repository base URL
   */
  public URL getURL() {
    return this.baseUrl;
  }

  /**
   * Refresh the catalog contents by downloading the Packages.gz file and
   * replacing the current contents.
   *
   * @throws IOException if the repository does not exist or has no catalog
   */
  public void refresh( ) throws IOException {
    URL catalogUrl = new URL(this.baseUrl + "/" + CATALOG_NAME);
    URLConnection conn = catalogUrl.openConnection();
    BufferedReader input = new BufferedReader(
      new InputStreamReader(new GZIPInputStream(conn.getInputStream()))
    );
    Vector catalogVector = new Vector();
    String s = input.readLine();
    while(true) {
      Properties props = new Properties();
      while(s != null && s.length() == 0) {
        s = input.readLine();
      }
      if(s == null) {
        break;
      }
      while(s != null && s.length() > 0) {
        int colon = s.indexOf(':');
        if(colon < 0) {
          throw new IOException("Bad catalog format");
        }
        String key = s.substring(0, colon);
        String value = s.substring(colon + 1).trim();
        while((s = input.readLine()) != null && s.length() > 0 &&
              s.substring(0, 1).matches("^\\s$")) {
          value += "\n" + s;
        }
        props.setProperty(key, value);
      }
      catalogVector.add(props);
    }
    input.close();
    this.catalog = (Properties [])
      catalogVector.toArray(new Properties[catalogVector.size()]);
  }

  /**
   * Force the repository catalog to be a specified set of reporters.
   *
   * @param catalog an array of Properties, each element of which describes one
   *                reporter/package in the repository
   */
  public void setCatalog(Properties[] catalog) {
    this.catalog = catalog;
  }

  /** Override of the default toString method. */
  public String toString() {
    return this.baseUrl.toString();
  }

  /**
   * Splits a property value from a catalog reporter entry into a set of values.
   *
   * @param s A property value from a catalog reporter entry
   * @return A set of zero or more individual values split out from the
   *         passed value
   */
  public static String[] getPropertyValues(String s) {
    // Inca v2.0 through v2.2 used semicolon as the element separator, which
    // doesn't allow for elements that contain a semicolon.  Newer versions
    // place multiple elements on separate lines.
    if(s.trim().length() == 0) {
      return new String[0];
    } else if(s.indexOf("\n") < 0) {
      return s.split(";", -1);
    } else {
      String[] result = s.replaceFirst(".*\n", "").split("\n", -1);
      for(int i = 0; i < result.length; i++) {
        result[i] = result[i].trim();
      }
      return result;
    }
  }

  /**
   * The main for this class is a test program that lists the contents of one
   * or more repositories listed on the command line.
   */
  public static void main(String[] args) {
    for(int i = 0; i < args.length; i++) {
      try {
        Repository irr = new Repository(new URL(args[i]));
        Properties[] p = irr.getCatalog();
        for(int j = 0; j < p.length; j++) {
          System.out.println(p[j].getProperty("name"));
        }
      }
      catch(Exception e) {
        System.err.println(args[i] + ": " + e);
      }
    }
  }

}
