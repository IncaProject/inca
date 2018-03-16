package edu.sdsc.inca.util;

import edu.sdsc.inca.ConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import junit.framework.TestCase;

/**
 * @author Jim Hayes
 */
public class ConfigPropertiesTest extends TestCase {

  static Properties p;
  static final String PROP_FILE_PATH = "configTestProperties";
  static final String PREFIX = "my.";
  static {
    p = new Properties();
    p.setProperty(PREFIX + "name", "Johnathan Livingston Seagull");
    p.setProperty(PREFIX + "address", "123 Fourth Street");
    p.setProperty(PREFIX + "city", "Encinitas");
    p.setProperty(PREFIX + "state", "California");
    p.setProperty(PREFIX + "zip", "92024");
  }
  static final String VALID1 =
    "  b|bb    null two bs or not\n" +
    "  d       int  the letter d\n" +
    "  f|final int  the final option\n";
  static final String VALID2 =
    "  a|ambercrombie path fits before\n" +
    "  c|ccc          str  we three cs\n" +
    "  d              str  it's dumb to duplicate\n" +
    "  e|every        str  for ever\n";
  static final String VALID_MERGED_SORTED =
    "  a|ambercrombie path fits before\n" +
    "  b|bb           null two bs or not\n" +
    "  c|ccc          str  we three cs\n" +
    "  d              int  the letter d\n" +
    "  e|every        str  for ever\n" +
    "  f|final        int  the final option\n";
  static final String VALID_MERGED_UNSORTED =
    "  b|bb           null two bs or not\n" +
    "  d              int  the letter d\n" +
    "  f|final        int  the final option\n" +
    "  a|ambercrombie path fits before\n" +
    "  c|ccc          str  we three cs\n" +
    "  e|every        str  for ever\n";


  public void testLoadFromFile() {
    try {
      createPropertyFile( p);
      ConfigProperties cp = new ConfigProperties();
      cp.loadFromFile(PROP_FILE_PATH, "");
      new File(PROP_FILE_PATH).delete();
      Enumeration e = cp.propertyNames();
      while(e.hasMoreElements()) {
        String name = (String)e.nextElement();
        assertEquals(p.getProperty(name), cp.getProperty(name));
      }
    } catch(IOException e) {
      fail("IOException " + e);
    }
  }

  public void testPutAllTrimmed() {
    ConfigProperties cp = new ConfigProperties();
    cp.putAllTrimmed(p, PREFIX);
    Enumeration e = cp.propertyNames();
    while(e.hasMoreElements()) {
      String name = (String)e.nextElement();
      assertEquals(p.getProperty(PREFIX + name), cp.getProperty(name));
    }
  }

  public void testMergeEmptyOptions() {
    assertEquals(VALID1, ConfigProperties.mergeValidOptions(VALID1, "", true));
  }

  public void testMergeValidOptions() {
    assertEquals(VALID_MERGED_SORTED, ConfigProperties.mergeValidOptions(VALID1, VALID2, true));
    assertEquals(VALID_MERGED_UNSORTED, ConfigProperties.mergeValidOptions(VALID1, VALID2, false));
  }

  public void testNoArgs() {
    String[] args = new String[0];
    ConfigProperties cp = new ConfigProperties();
    try {
      cp.setPropertiesFromArgs(VALID_MERGED_SORTED, args);
    } catch(ConfigurationException e) {
      fail(e.toString());
    }
  }

  public void testStandardArgs() {
    String[] args = new String[] {
      "-a", "/tmp", "-b", "-c", "anything", "-d", "5", "-e", "stuff", "-f", "6"
    };
    ConfigProperties cp = new ConfigProperties();
    try {
      cp.setPropertiesFromArgs(VALID_MERGED_SORTED, args);
    } catch(ConfigurationException e) {
      fail(e.toString());
    }
    String msg = checkArgs(VALID_MERGED_SORTED, cp, args);
    if(msg != null) {
      fail(msg);
    }
  }

  public void testLongArgs() {
    String[] args = new String[] {
      "--ambercrombie", "/tmp", "--bb", "--ccc", "anything", "--every", "stuff",
      "--final", "6"
    };
    ConfigProperties cp = new ConfigProperties();
    try {
      cp.setPropertiesFromArgs(VALID_MERGED_SORTED, args);
    } catch(ConfigurationException e) {
      fail(e.toString());
    }
    String msg = checkArgs(VALID_MERGED_SORTED, cp, args);
    if(msg != null) {
      fail(msg);
    }
  }

  public void testUnknownArg() {
    String[] args = new String[] {
      "-a", "/tmp", "-b", "-x", "anything", "-d", "5", "-e", "stuff",
      "-f", "6"
    };
    ConfigProperties cp = new ConfigProperties();
    try {
      cp.setPropertiesFromArgs(VALID_MERGED_SORTED, args);
    } catch(ConfigurationException e) {
      return;
    }
    fail("Bad argument not rejected");
  }

  public void testMissingValue() {
    String[] args = new String[] {
      "-a", "/tmp", "-b", "-c", "anything", "-d", "5", "-e", "stuff", "-f"
    };
    ConfigProperties cp = new ConfigProperties();
    try {
      cp.setPropertiesFromArgs(VALID_MERGED_SORTED, args);
    } catch(ConfigurationException e) {
      return;
    }
    fail("Missing arg value not rejected");
  }

  private String checkArgs(String valid, ConfigProperties cp, String[] args) {
    for(int i = 0; i < args.length; i++) {
      String arg = args[i];
      if(arg.startsWith("--")) {
        arg = arg.substring(2);
      } else if(valid.indexOf(arg.charAt(1) + "|") < 0) {
        arg = arg.substring(1);
      } else {
        arg = valid.substring(valid.indexOf(arg.charAt(1) + "|") + 2);
        arg = arg.substring(0, arg.indexOf(" "));
      }
      String value = arg.startsWith("b") ? "" : args[++i];
      String prop = cp.getProperty(arg);
      if(prop == null) {
        return "No property for arg '" + arg + "'";
      } else if(!value.equals(prop)) {
        return "Property '" + arg + "' expected '" + value +
               "' found '" + prop + "'";
      }
    }
    return null;
  }

  private void createPropertyFile( Properties p ) throws IOException {
    FileWriter f = new FileWriter(PROP_FILE_PATH);
    Enumeration e = p.propertyNames();
    while(e.hasMoreElements()) {
      String name = (String)e.nextElement();
      f.write(name + "=" + p.getProperty(name) + "\n");
    }
    f.close();
  }

}
