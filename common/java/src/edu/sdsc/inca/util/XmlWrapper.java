package edu.sdsc.inca.util;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlObject;
import org.apache.log4j.Logger;
import edu.sdsc.inca.dataModel.util.SeriesConfig;
import edu.sdsc.inca.dataModel.util.Args;
import edu.sdsc.inca.dataModel.util.Cron;
import edu.sdsc.inca.ConfigurationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;

/**
 * We provide a set of Wrapper classes which provide convenience methods for
 * dealing with XML Bean classes generated from our Inca XML schemas.
 * This class serves as the base class for these Wrapper classes
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
abstract public class XmlWrapper {

  // A pattern that matches the value of macros named .*Pass(phrase|word)__.
  // The 200-space limit between </name> and <value> (instead of \s*) is due to
  // Pattern's requirement that a look-ahead pattern have a maximum length.
  protected static Pattern PASSWORD_MACRO_VALUE_PATTERN = Pattern.compile
    ("(?i)(?<=Pass(phrase|word)(__)?</name>\\s{0,200}<value>).*?(?=</value>)");

  // Globals
  private static Logger logger = Logger.getLogger(XmlWrapper.class);

  /**
   * Return true if the given report series configs are equivalent.  Two
   * report series configs are equivalent if all their fields match with the
   * exception of the &lt;schedule&gt; element if and only if the
   * &lt;scheduleSpec&gt; element is present.  The &lt;scheduleSpec&gt; element
   * holds the schedule specified by the user (which may contain '?') and the
   * &lt;schedule&gt; element holds the schedule selected by the agent.
   *
   * Note, that I could not find any equivalency function in xmlbeans so I
   * convert both configs to string, strip off the outer
   * &lt;xml-fragment&gt; tags and then compare the strings.
   *
   * @param config1  A series config describing how to run a reporter on
   * a set of resources.
   * @param config2  A series config describing how to run a reporter on
   * a set of resources.
   *
   * @return True if the reporter set configs are equivalent and false
   * otherwise.
   */
  public static boolean configEqual
    ( SeriesConfig config1, SeriesConfig config2 ) {

    SeriesConfig config1copy = (SeriesConfig)config1.copy();
    SeriesConfig config2copy = (SeriesConfig)config2.copy();
    // an empty limits is equivalent to no limits tag
    if ( ! config1copy.getSeries().isSetLimits() ) {
      config1copy.getSeries().addNewLimits();
    }
    if ( ! config2copy.getSeries().isSetLimits() ) {
      config2copy.getSeries().addNewLimits();
    }

    // look for equivalency in cron args
    if ( config1copy.getSchedule().isSetCrontime() &&
         config2copy.getSchedule().isSetCron() ) {
      config1copy.getSchedule().setCron
        ( createCron( config1copy.getSchedule().getCrontime() ) );
      config1copy.getSchedule().unsetCrontime();
    }
    if ( config2copy.getSchedule().isSetCrontime() &&
         config1copy.getSchedule().isSetCron() ) {
      config2copy.getSchedule().setCron
        ( createCron ( config2copy.getSchedule().getCrontime() ) );
      config2copy.getSchedule().unsetCrontime();
    }

    // args can be in any order
    sortArgs( config1copy );
    sortArgs( config2copy );

    // unset nice is the same as false
    if ( ! config1copy.getSeries().isSetNice() ) {
      config1copy.getSeries().setNice( false );
    }
    if ( ! config2copy.getSeries().isSetNice() ) {
      config2copy.getSeries().setNice( false );
    }

    // convert to text and compare
    String config1Text = XmlWrapper.prettyPrint(config1copy, "  ");
    String config2Text = XmlWrapper.prettyPrint(config2copy, "  " );
    config1Text = config1Text.replaceAll( "<xml-fragment[^>]*>", "" );
    config1Text = config1Text.replaceAll( "</xml-fragment>", "" );
    config1Text = config1Text.replaceAll( "\\?=\\d+-?\\d*", "\\?" );
    config2Text = config2Text.replaceAll( "<xml-fragment[^>]*>", "" );
    config2Text = config2Text.replaceAll( "</xml-fragment>", "" );
    config2Text = config2Text.replaceAll( "\\?=\\d+-?\\d*", "\\?" );
    return config1Text.equals(config2Text);
  }

  /**
   * Create a cron object from a whitespace separated cron string
   *
   * @param crontime A string containing a crontab entry
   *
   * @return  A Cron object
   */
  public static Cron createCron( String crontime ) {
    Cron cron = Cron.Factory.newInstance();
    String[] cronArgs = crontime.split( "\\s+" );
    if ( cronArgs.length == 5 ) {
      cron.setMin( cronArgs[0] );
      cron.setHour( cronArgs[1] );
      cron.setMday( cronArgs[2] );
      cron.setMonth( cronArgs[3] );
      cron.setWday( cronArgs[4] );
    }
    return cron;
  }

  /**
   * Decrypt/encrypt the values of name/value pairs where the name is a
   * password/passphrase in the provided string.
   *
   * @param sensitiveText A string possibly containing
   * name/value password/passphrase pairs that needed to be encrypted/decrypted
   * @param passphrase  A string that can be used to decrypt/encrypt
   * passwords/passphrases
   * @param decrypt  A value of true will attempt to decrypt the sensitive text
   * while a value of false will attempt to encrypt the sensitive text.
   *
   * @return the encrypted/decrypted version of sensitiveText.
   *
   * @throws CrypterException if problem encrypting/decrypting text
   */
  public static String cryptSensitive( String sensitiveText, String passphrase,
                                       boolean decrypt) throws CrypterException{
    if(passphrase == null || passphrase.equals("")) {
      return sensitiveText;
    }
    Crypter c = new Crypter(passphrase, "AES");
    return decrypt ?
      c.decryptMatchingSubstrings(sensitiveText, PASSWORD_MACRO_VALUE_PATTERN) :
      c.encryptMatchingSubstrings(sensitiveText, PASSWORD_MACRO_VALUE_PATTERN);
  }

  /**
   * Escape illegal XML characters in a string.  The following characters
   * are illegal in XML:
   *
   * &lt; less than
   * &gt; greater than
   * &amp;  ampersand
   * &apos; apostrophe
   * &quot; quotation mark
   *
   * @param unescaped A string possibly containing illegal XML characters
   *
   * @return A properly escaped XML string.
   */
  public static String escape( String unescaped ) {
    return unescaped
       .replaceAll( "&", "&amp;" ).replaceAll( "<", "&lt;" )
       .replaceAll( ">", "&gt;" ).replaceAll( "'", "&apos;" )
       .replaceAll( "\"", "&quot;" ).replaceAll( "\\$", "&#0036;" );
  }

  /**
   * Read in a XML file from disk and decrypt any passwords/passphrases
   * contained in it.
   *
   * @param filePath  A string containing a path to the xml file.
   * @param passphrase A string containing the passphrase to use to decrypt
   * the passwords.
   *
   * @return The contents of the XML file as a string.
   *
   * @throws CrypterException if trouble decrypting password
   * @throws IOException if trouble reading file
   */
  public static String read( String filePath, String passphrase )
    throws IOException, CrypterException {

    logger.debug( "Reading in encrypted xml file '" + filePath + "'" );
    File file  = new File( filePath );
    if ( file.exists() ) {
      // Slurp the entire file.
      BufferedReader br = new BufferedReader(new FileReader(file));
      StringBuffer contents = new StringBuffer();
      String line;
      while((line = br.readLine()) != null) {
        contents.append(line).append("\n");
      }
      // decrypt passwords
      return cryptSensitive(contents.toString(), passphrase, true);
    } else {
      throw new FileNotFoundException( "File '" + filePath + "' not found" );
    }
  }

  public static void save( String xmlText, String filePath, String passphrase )
    throws CrypterException, IOException {

    File saveFile = new File( filePath );
    if ( ! saveFile.getParentFile().exists() ) {
      if ( ! saveFile.getParentFile().mkdirs() ) {
        throw new IOException
          ("Unable to create parent dirs " + saveFile.getParentFile().getAbsolutePath());
      }
    }
    
    String encryptedXml = xmlText;
    if ( passphrase != null ) {
      encryptedXml = cryptSensitive(xmlText, passphrase, false);
    }
    BufferedWriter bw = new BufferedWriter( new FileWriter(saveFile));
    bw.write(encryptedXml);
    bw.close();
    try {
      Runtime.getRuntime().exec("chmod 600 " + filePath).waitFor();
    } catch ( InterruptedException e ) {
      throw new IOException(
        "Unable to change permissions on " + filePath + ": " + e
      );
    }
  }

  /**
   * Unescape escaped XML characters in a string.  The following characters
   * need to be unescaped:
   *
   * &lt; less than
   * &gt; greater than
   * &amp;  ampersand
   * &apos; apostrophe
   * &quot; quotation mark
   *
   * @param escaped A string possibly containing escaped XML characters
   *
   * @return A unescaped string.
   */
  public static String unescape( String escaped ) {
    return escaped
      .replaceAll( "&lt;", "<" ).replaceAll( "&gt;", ">" )
      .replaceAll( "&apos;", "'" ).replaceAll( "&quot;", "\"" )
      .replaceAll( "&#0036;", "\\$" ).replaceAll( "&amp;", "&" );
  }

  /**
   * Validate the XML document and throw and XmlException if parsing fails.
   *
   * @param xml the xml object to validate
   *
   * @throws XmlException if parsing fails
   */
  protected static void validate( XmlObject xml )
    throws XmlException {

    final Collection errorList = new ArrayList();
    final XmlOptions opts = new XmlOptions();
    opts.setErrorListener(errorList);
    if (!xml.validate(opts)) {
      String errors = "";
      for (Iterator it = errorList.iterator(); it.hasNext();) {
        errors += it.next() + "\n";
      }
      throw new XmlException(
        "Validation of suite file failed: " + errors
      );
    }
  }

  /**
   * Sort the arguments in the provided series config alphabetically.
   *
   * @param config  A series config with arguments.
   */
  private static void sortArgs( SeriesConfig config ) {
    Args.Arg[] args = config.getSeries().getArgs().getArgArray();
    if ( args.length < 2 ) {
      return;
    }
    Arrays.sort(
      args,
      new Comparator() {
        public int compare( Object o1, Object o2) {
          Args.Arg arg1 = (Args.Arg)o1;
          Args.Arg arg2 = (Args.Arg)o2;
          return arg1.getName().compareTo( arg2.getName() );
        }
      }
    );
    config.getSeries().getArgs().setArgArray( args );
  }

  protected static String XML_NAME_PATTERN = "[a-zA-Z:_][\\w\\.\\-:_]*";
  protected static String PROCESSING_INSTRUCTION_PATTERN = "\\?.*?\\?";
  protected static String XML_COMMENT_PATTERN = "!--.*?--";
  protected static String XML_CDATA_PATTERN = "!\\[CDATA\\[.*?\\]\\]";
  protected static Pattern XML_NODE_PATTERN = Pattern.compile
    ("(?s)<(" + XML_NAME_PATTERN + "[^>]*" + "|" +
     "/" + XML_NAME_PATTERN + "|" + PROCESSING_INSTRUCTION_PATTERN + "|" +
     XML_CDATA_PATTERN + "|" + XML_COMMENT_PATTERN + ")>");

  /**
   * Returns a copy of a specified XML document with added spacing and newlines
   * to make it more readable.  Does not handle DOCTYPE, ENTITY, or NOTATION
   * markup or mixed content.
   *
   * @param xml the XML document
   * @param indent the text to use to indicate nesting levels; typically some
   *               number of spaces
   * @return the specified text with inserted newlines and indentation
   */
  public static String prettyPrint(String xml, String indent) {
    boolean elementHasChildren = false;
    StringBuffer newline = new StringBuffer("\n");
    Matcher m = XML_NODE_PATTERN.matcher(xml);
    StringBuffer result = new StringBuffer();
    for(int index = 0; m.find(index); index = m.end()) {
      String matched = m.group();
      char next = matched.charAt(1);
      String content = xml.substring(index, m.start());
      result.append
        (next != '/' || elementHasChildren ? content.trim() : content);
      if(next == '?' || next == '!') {
        // empty
      } else if(next == '/') {
        newline.setLength(newline.length() - indent.length());
        if(elementHasChildren) {
          result.append(newline);
        }
        elementHasChildren = true;
      } else {
        if(index > 0) {
          result.append(newline);
        }
        elementHasChildren = matched.endsWith("/>");
        if(!elementHasChildren) {
          newline.append(indent);
        }
      }
      result.append(matched);
    }
    result.append("\n");
    return result.toString();
  }

    /**
   * Returns a copy of a specified XML document with added spacing and newlines
   * to make it more readable.  Does not handle DOCTYPE, ENTITY, or NOTATION
   * markup or mixed content.
   *
   * @param obj the XML object
   * @param indent the text to use to indicate nesting levels; typically some
   *               number of spaces
   * @return the specified text with inserted newlines and indentation
   */
  public static String prettyPrint(XmlObject obj, String indent) {
    return XmlWrapper.prettyPrint(obj.xmlText(new XmlOptions()), indent);
  }

  /**
   * Provides a command line client to encrypt/decrypt Inca XML files
   *
   * @param args  command line arguments
   */
  public static void main(final String[] args) {
    String OPTS = ConfigProperties.mergeValidOptions(
      "",
      "  i|in str path to file to be encrypted/decrypted\n" +
      "  o|out str path to output file for result\n" +
      "  t|type str [encode|decode]\n",
      true
    );
    ConfigProperties config = new ConfigProperties();
    try {
      config.setPropertiesFromArgs( OPTS, args );
    } catch ( ConfigurationException e ) {
      System.err.println( "Error in configuration: " + e );
      System.exit(1);
    }
    String infile = config.getProperty( "in" );
    String outfile = config.getProperty( "out" );
    if ( infile == null ) {
      System.err.println( "Error, no input file specified" );
      System.exit(1);
    }
    if ( outfile == null ) {
      System.err.println( "Error, no output file specified" );
      System.exit(1);
    }
    String type = config.getProperty( "type" );
    if ( type.equalsIgnoreCase("encode") ) {
      System.out.print( "Please enter a password to encrypt the file: " );
    } else if ( type.equalsIgnoreCase("decode") ) {
      System.out.print( "Please enter a password to decrypt the file: " );
    } else {
      System.err.println( "Error, incorrect type specified '" + type + "'" );
      System.exit(1);
    }

    String pwd = null;
    try {
      pwd = new BufferedReader(new InputStreamReader(System.in)).readLine();
    } catch ( IOException e ) {
      System.err.println( "Error, unable to read password: " + e );
      System.exit(1);
    }
    if ( pwd == null ) {
      System.err.println( "Error, null password" );
      System.exit(1);
    }

    ResourcesWrapper inRC = null, outRC = null;
    try {
      File out = new File( outfile );
      if ( out.exists() ) {
        out.delete();
      }
      if ( type.equalsIgnoreCase("encode") ) {
        inRC = new ResourcesWrapper( infile );
        outRC = new ResourcesWrapper( outfile, pwd );
      } else {
        inRC = new ResourcesWrapper( infile, pwd );
        outRC = new ResourcesWrapper( outfile );
      }
      outRC.setResourceConfigDocument( inRC.getResourceConfigDocument() );
      outRC.save();
    } catch ( Exception e ) {
      System.err.println( "Error, "  + type + " of file '" + infile + "' " );
      e.printStackTrace();
      System.exit(1);
    }
  }


}
