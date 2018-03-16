package edu.sdsc.inca.util;

import edu.sdsc.inca.ConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

/**
 * This is an extension of the standard Properties class that can generate
 * elements by command-line parsing and by stripping prefixes from the elements
 * of Properties objects--both useful in generating configuration values.
 */
public class ConfigProperties extends Properties {

  /**
   * Reads properties from a file, retaining only those properties that
   * have a name starting with the given prefix.  Trims the prefix from the
   * property names.
   *
   * @param path Path to file to open and load.
   * @param prefix Property name prefix to test and trim.
   * @throws IOException
   */
  public void loadFromFile(final String path,
                           final String prefix) throws IOException {
    FileInputStream is = new FileInputStream(path);
    loadFromStream(is, prefix);
    is.close();
  }

  /**
   * Reads properties from a resource, retaining only those properties that
   * have a name starting with the given prefix.  Trims the prefix from the
   * property names.
   *
   * @param res Resource to open and load.
   * @param prefix Property name prefix to test and trim.
   * @throws IOException
   */
  public void loadFromResource(final String res,
                               final String prefix) throws IOException {
    InputStream is = ClassLoader.getSystemResourceAsStream(res);
    if(is == null) {
      throw new IOException("Resource " + res + " not found in classpath");
    }
    loadFromStream(is, prefix);
    is.close();
  }

  /**
   * Reads properties from an input stream, retaining only those properties
   * that have a name starting with the given prefix.  Trims the prefix from
   * the property names.
   *
   * @param stream Resource to open and load.
   * @param prefix Property name prefix to test and trim.
   * @throws IOException
   */
  public void loadFromStream(final InputStream stream,
                             final String prefix) throws IOException {
    Properties p = new Properties();
    p.load(stream);
    this.putAllTrimmed(p, prefix);
  }

  /**
   * Like putAll, but only copies those elements that have a name starting with
   * the given prefix.  Trims the prefix from the property names.
   *
   * @param p Properties to copy.
   * @param prefix Property name prefix to test and trim.
   */
  public void putAllTrimmed(final Properties p,
                            final String prefix) {
    Enumeration e = p.propertyNames();
    while(e.hasMoreElements()) {
      String prop = (String)e.nextElement();
      if(prop.startsWith(prefix)) {
        this.setProperty(prop.substring(prefix.length()), p.getProperty(prop));
      }
    }
  }

  /**
   * Parses command-line arguments, storing each option/value pair as a
   * property.  Options that take no value are given the value "".
   * 
   * @param valid A newline-delimited list of descriptions about valid options.
   *              The tokens in each option description list the option name
   *              (or names, separated by "|") and option type ("null" for
   *              options that take no value).  Any text after the option type
   *              is ignored, making it easy to reuse the valid option
   *              description for user help.
   * @param args Command-line arguments from main().
   * @throws ConfigurationException
   */
  public void setPropertiesFromArgs(final String valid,
                                    final String[] args)
    throws ConfigurationException {
    String[] lines = valid.split("\n");
    Properties canonicalNames = new Properties();
    Properties types = new Properties();
    for(int i = 0; i < lines.length; i++) {
      String[] pieces = lines[i].trim().split(" +", 3);
      String[] names = pieces[0].split("[|]");
      for(int j = 0; j < names.length; j++) {
        types.setProperty(names[j], pieces[1]);
        canonicalNames.setProperty(names[j], names[names.length - 1]);
      }
    }
    for(int i = 0; i < args.length; i++) {
      String arg = args[i];
      String name = arg.substring(arg.startsWith("--") ? 2 : 1);
      String value = null;
      if(name.indexOf('=') >= 0) {
        value = name.substring(name.indexOf('=') + 1);
        name = name.substring(name.indexOf('='));
      }
      String type = types.getProperty(name);
      if(type == null) {
        throw new ConfigurationException("Unknown argument " + arg);
      }
      if(type.equals("null")) {
        if(value != null) {
          throw new ConfigurationException("Bad value for " + arg);
        }
        value = "";
      } else if(value == null) {
        if(++i == args.length) {
          throw new ConfigurationException("Missing value for " + arg);
        }
        value = args[i];
      }
      this.setProperty(canonicalNames.getProperty(name), value);
    }
  }

  /**
   * Merges two strings of the form accepted by setPropertiesFromArgs.  Removes
   * any duplicates and sorts and formats the result.
   *
   * @param valid1 the first set of options
   * @param valid2 the second set of options
   * @return the combined options
   */
  public static String mergeValidOptions(final String valid1,
                                         final String valid2,
                                         boolean sort) {

    int j;
    String indent;
    String longestName = "";
    String longestType = "";
    ArrayList names = new ArrayList();
    Properties types = new Properties();
    Properties texts = new Properties();
    String[] lines = (valid1 + valid2).split("\n");

    // Split each line into name(s), type, and optional descriptive text.
    for(int i = 0; i < lines.length; i++) {
      String[] pieces = lines[i].trim().split(" +", 3);
      String name = pieces[0];
      if(pieces.length < 1 || types.getProperty(name) != null) {
        continue;
      }
      if(sort) {
        for(j = 0;
            j < names.size() && name.compareTo((String)names.get(j)) > 0;
            j++) {
          // empty
        }
      } else {
        j = names.size();
      }
      names.add(j, name);
      types.setProperty(name, pieces[1]);
      if(pieces.length > 2) {
        texts.setProperty(name, pieces[2]);
      }
    }

    // Find the indent length and the longest name and type for formatting.
    for(j = 0; j < valid1.length() && valid1.charAt(j) == ' '; j++) {
      // empty
    }
    indent = valid1.substring(0, j);
    for(int i = 0; i < names.size(); i++) {
      String name = (String)names.get(i);
      String type = types.getProperty(name);
      if(name.length() > longestName.length()) {
        longestName = name;
      }
      if(type.length() > longestType.length()) {
        longestType = type;
      }
    }
    longestName = longestName.replaceAll(".", " ");
    longestType = longestType.replaceAll(".", " ");

    // Format the merged set of options.
    String result = "";
    for(int i = 0; i < names.size(); i++) {
      String name = (String)names.get(i);
      String type = types.getProperty(name);
      String text = texts.getProperty(name);
      result += indent + name;
      if(type != null) {
        result += longestName.substring(name.length()) + " " + type;
      }
      if(text != null) {
        result += longestType.substring(type.length()) + " " + text;
      }
      result += "\n";
    }

    return result;

  }

}
