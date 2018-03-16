package edu.sdsc.inca.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

/**
 * A class of useful static utility methods that really don't fit elsewhere.
 *
 * @author Jim Hayes &lt;jhayes@sdsc.edu&gt;
 */
public class StringMethods {

  public final static String MIME_DELIM = "Inca-2-by-SDSC";
  private static Logger logger = Logger.getLogger( StringMethods.class );

  /**
   * Append a series of strings to the specified string buffer.
   *
   * @param buffer   A string buffer.
   * @param strings  The strings to append to it.
   */
  public static void appendToStringBuffer( StringBuffer buffer, String[] strings ) {
    for ( int i = 0; i < strings.length; i++ ) {
      buffer.append( strings[i] );
    }
  }

  /**
   * Returns an indicator of the relationship between two operands.
   *
   * @param left the left operand
   * @param right the right operand
   * @return a negative number if left &lt; right, a positive number if left
   *   &gt; right, 0 if the two are equal
   */
  static public int compareTo(String left, String right) {
    boolean stringCompare = false;
    if(left.length() > 0 && (left.charAt(0) == '"' || left.charAt(0) == '\'')) {
      left = left.substring(1, left.length() - 1);
      stringCompare = true;
    }
    if(right.length() > 0 && (right.charAt(0)=='"' || right.charAt(0)=='\'')) {
      right = right.substring(1, right.length() - 1);
      stringCompare = true;
    }
    if(stringCompare) {
      return left.compareTo(right);
    }
    // Split each operand at "." and compare piece-by-piece.  Treat each pair
    // as integers or strings, as appropriate.
    String[] leftPieces = left.split("\\.");
    String[] rightPieces = right.split("\\.");
    int result = 0;
    for(int i = 0;
        i < leftPieces.length && i < rightPieces.length && result == 0;
        i++) {
      try {
        result = Integer.parseInt(leftPieces[i]) -
                 Integer.parseInt(rightPieces[i]);
      } catch(NumberFormatException e) {
        result = leftPieces[i].compareTo(rightPieces[i]);
      }
    }
    // If all pieces equal, the longer is considered greater
    if(result == 0) {
      result = leftPieces.length - rightPieces.length;
    }
    return result;
  }

  /**
   * Recursively delete a directory
   *
   * Cut-n-paste from http://www.rgagnon.com/javadetails/java-0483.html
   *
   * @param path  The path to the directory to delete.
   *
   * @return True if delete successful and false otherwise.
   */
  public static boolean deleteDirectory(File path) {
    if( path.exists() ) {
      File[] files = path.listFiles();
      for(int i=0; i<files.length; i++) {
        if(files[i].isDirectory()) {
          deleteDirectory(files[i]);
        }
        else {
          files[i].delete();
        }
      }
    }
    return( path.delete() );
  }

  /**
   * Returns the contents of a specified file as a String, with lines from the
   * file delimited by newlines (\n).
   *
   * @param path the path to the file to read
   * @return the file contents
   * @throws IOException on an open/read error
   */
  public static String fileContents(String path) throws IOException {
    BufferedReader f = new BufferedReader(new FileReader(path));
    StringBuffer result = new StringBuffer();
    String line;
    while((line = f.readLine()) != null) {
      result.append(line).append("\n");
    }
    f.close();
    return result.toString();
  }

  /**
   * Find a file from the classpath and return the contents as a string.
   *
   * @param filename  The name of the file to search for in the classpath.
   *
   * @return The contents of the file or null if not found
   *
   * @throws java.io.IOException if trouble opening file
   */
  static public String fileContentsFromClasspath( String filename )
    throws IOException {

    // locate in classpath
    String filePath = findInClasspath(filename);
    if ( filePath != null ) {
      return fileContents( filePath );
    } else {
      return null;
    }
  }

  /**
   * Find a file in the classpath and return its path.
   *
   * @param filename  A file to search for in the classpath
   *
   * @return A string containing the path of the file or null if not found
   */
  public static String findInClasspath(String filename) {
    URL url = ClassLoader.getSystemClassLoader().getResource( filename );
    if(url == null) {
      return null;
    }
    logger.debug( "Located file " + url.getFile() );
    return url.getFile();
  }

  /**
   * Joins the separate strings in values into a single string with
   * fields separated by the value of separator and returns that new string.
   *
   * @param separator  The string that will separate the values.
   *
   * @param values The list of strings that should be joined
   *
   * @return The new joined string.
   */
  public static String join( String separator, String[] values ) {
    if ( values.length < 1 ) {
      return "";
    }
    StringBuffer buffer = new StringBuffer( values[0] );
    for ( int i = 1; i < values.length; i++ ) {
      buffer.append( separator );
      buffer.append( values[i] );
    }
    return buffer.toString();
  }

  /**
   * Send a notification email to the specified email address.
   *
   * @param address  The email address to send the email to.
   * @param subject  The subject of the email.
   * @param message  The message body of the email
   */
  public static void sendEmail(String address, String subject, String message) {
    StringMethods.sendEmail
      ( address, subject, message, new File[0], new String[0] );
  }

  /**
   * Send a notification email to the specified email address.
   *
   * @param address  The email address to send the email to.
   * @param subject  The subject of the email.
   * @param message  The message body of the email
   * @param attachments  List of files to attach.
   * @param types  The types of attachments of the files (e.g., text/html)
   *
   */
  public static void sendEmail(String address, String subject, String message,
                               File[] attachments, String[] types ) {

    String mimeMessage = null;
    try {
      logger.debug("Sending mail '" + subject + "' to " + address);
      mimeMessage = mimeMessage
        ( address, subject, message, attachments, types );
      runCommand(new String[]{"/usr/sbin/sendmail", "-t", "-oi"}, mimeMessage);
    } catch(Exception e) {
      logger.warn( "Problem using /usr/sbin/sendmail: " + e );
      logger.info( "Attempting to fallback to mail" );
      try {
        String quote = subject.indexOf("'") >= 0 ? "\"" : "'";
        subject = quote + subject + quote;
        runCommand
          ( new String[] {"/bin/sh", "-c", "mail -s " + subject + " " + address},
            mimeMessage );
      } catch ( Exception e2 ) {
        logger.error("Unable to send mail: ", e);
      }
    }
  }

  /**
   * Convenience function for running a system command. Waits for command
   * to complete and if the exit code is non-zero, it logs any output from
   * stderr.
   *
   * @param command   The command to pass to Runtime.exec
   * @param stdin     The string to write to stdin.
   *
   * @throws IOException
   * @throws InterruptedException
   */
  private static void runCommand( String[] command, String stdin )
    throws IOException, InterruptedException {

    String commandString = StringMethods.join( " ", command );
    logger.debug( "Running: " + commandString );

    Process p = Runtime.getRuntime().exec(command);
    if ( stdin != null ) {
      OutputStreamWriter osw =
        new OutputStreamWriter(new BufferedOutputStream(p.getOutputStream()));
      osw.write(stdin);
      osw.close();
    }

    p.waitFor();
    if(p.exitValue() != 0) {
      BufferedReader reader =
        new BufferedReader(new InputStreamReader(p.getErrorStream()));
      String error = "", line;
      while((line = reader.readLine()) != null) {
        error += line;
      }
      logger.error("Error running '" + command + "': " + error);
    }
  }

  public static final Pattern LEADING_TAG =
    Pattern.compile("\\s*<([^> ]*)[^>]*(/>|>(.*?)</\\1>)");

  /**
   * Translates XML content into an HTML table for easy viewing.
   *
   * @param xml XML content--either a series of (balanced) tags or text
   * @param indent an indent string used to pretty-print the HTML; the top-level
   *               call will typically pass an empty string
   * @return an XML table that displays the content
   */
  public static String xmlContentToHtml(String xml, String indent) {

    Matcher m = LEADING_TAG.matcher(xml);

    // Return XML text unchanged
    if(!m.find()) {
      return xml;
    }

    Properties columns = new Properties();
    Vector columnOrder = new Vector();
    StringBuffer htmlBits = new StringBuffer();

    do {
      String tag = m.group(1);
      String content = m.group(3);
      content = content == null ? "" : xmlContentToHtml(content, indent + "  ");
      String oldContent = columns.getProperty(tag);
      if(oldContent == null) {
        // New tag
        columnOrder.add(tag);
        columns.setProperty(tag, content);
      } else if(oldContent.length() == 0) {
        // Previously empty column
        columns.setProperty(tag, content);
      } else if(content.length() == 0) {
        // empty
      } else if(!oldContent.endsWith("</tr></table>")) {
        // Second value; put in table, one value per row
        columns.setProperty(tag,
          "<table border='1'><tr><td>" + oldContent + "</td></tr><tr><td>" +
          content + "</td></tr></table>"
        );
      } else {
        // Third or subsequent value; append to existing table
        columns.setProperty(tag,
          oldContent.substring(0, oldContent.length() - "</table>".length()) +
          "<tr><td>" + content + "</td></tr></table>");
      }
    } while(m.find(m.end()));

    // Wrap the processed tags into an HTML table w/tags as the column headers
    htmlBits.append(indent + "<table border='1'>\n" + indent + "  <tr>");
    for(int i = 0; i < columnOrder.size(); i++) {
      htmlBits.append("<th>" + (String)columnOrder.get(i) + "</th>");
    }
    htmlBits.append("</tr>\n");
    htmlBits.append(indent + "  <tr>");
    for(int i = 0; i < columnOrder.size(); i++) {
      String cell = columns.getProperty((String)columnOrder.get(i));
      htmlBits.append("<td>" + cell + "</td>");
    }
    htmlBits.append("</tr>\n");
    htmlBits.append(indent + "</table>");

    return htmlBits.toString();

  }

  /**
   * Construct a multi-part MIME email message that can contain attachments.
   *
   * @param address   The email address the message will be sent to.
   * @param subject   The subject of the email message.
   * @param message   The body of the email message.
   * @param attachments  List of files to attach.
   * @param types  The types of attachments of the files (e.g., text/html)
   *
   * @return  The MIME text that can be sent to sendmail
   *
   * @throws IOException
   */
  protected static String mimeMessage( String address, String subject,
                                       String message, File[] attachments,
                                       String[] types )
    throws IOException {

    StringBuffer mime = new StringBuffer();
    appendToStringBuffer( mime, new String[] { "To: ", address, "\n" } );
    mime.append( "Mime-Version: 1.0\n" );
    appendToStringBuffer(mime,
      new String[]{"Content-Type: multipart/mixed; boundary=",MIME_DELIM,"\n"});
    appendToStringBuffer( mime, new String[]{"Subject: ", subject, " \n\n\n"} );
    appendToStringBuffer( mime, new String[] { "--", MIME_DELIM, "\n" } );
    mime.append( "Content-Transfer-Encoding: 7bit\n" );
    mime.append( "Content-Type: text/plain;\n" );
    mime.append( "\tcharset=US-ASCII;\n" );
    mime.append( "\tdelsp=yes;\n" );
    mime.append( "\tformat=flowed\n" );
    mime.append( "\n" );
    mime.append( message );
    mime.append( "\n" );
    for ( int i = 0; i < attachments.length; i++ ) {
      appendToStringBuffer( mime, new String[] { "--", MIME_DELIM, "\n" } );
      mime.append( "Content-Transfer-Encoding: 7bit\n" );
      appendToStringBuffer
        ( mime, new String[] { "Content-Type: ", types[i], ";\n" } );
      mime.append( "\tx-unix-mode=0644;\n" );
      appendToStringBuffer
        ( mime, new String[]{"\tname=", attachments[i].getName(), "\n"} );
      mime.append( "Content-Disposition: attachment;\n" );
      appendToStringBuffer( mime, new String[] {
        "\tfilename=", attachments[i].getName(), "\n\n" } );
      mime.append( fileContents(attachments[i].getAbsolutePath() ) );
    }
    appendToStringBuffer( mime, new String[] { "--", MIME_DELIM, "--\n\n" } );

    return mime.toString();
  }

  /**
   * Convert a String to a Date using the format passed
   *
   * @param sdate   The date string (e.g. 10/21/07)
   * @param format  The format of the date string (e.g. MM/dd/yy)
   *
   * @return date  A java.util.Date
   */
  public static Date convertDateString(String sdate, String format){
    Date date=null;
    try {
      date = new SimpleDateFormat(format).parse(sdate);
    }
    catch (Exception e) {
    }
    return date;
  }

}
