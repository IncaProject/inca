package edu.sdsc.inca.protocol;

import java.util.Arrays;

/**
 * @author Cathie Olschanowsky
 * @author Jim Hayes
 *
 * A statements is the most basic part of the Inca Protocol.  Its format is
 * CMD [SP DATA]
 * <p/>
 * Description: Assists in sending/receiving and parsing the Inca protocol.
 */
public class Statement {

  private static final char[] ERROR_CHARS = "ERROR".toCharArray();
  private static final char[] OK_CHARS = "OK".toCharArray();
  private static final char[] START_CHARS = "START".toCharArray();
  private static final char SP = ' ';
  private static final String VERSION = "1";

  private char[] cmd;
  private char[] data;

  /**
   * Creates a statement with no command or data.
   */
  public Statement() {
    this.cmd = new char[0];
    this.data = new char[0];
  }

  /**
   * Creates a Statment from a byte array that contains both the command and
   * optional data.
   *
   * @param bytes the raw bytes
   */
  public Statement(char[] bytes) {
    int i;
    for(i = 0; i < bytes.length && bytes[i] != SP; i++) {
      // empty
    }
    if(i < bytes.length) {
      this.cmd = new char[i];
      System.arraycopy(bytes, 0, cmd, 0, i);
      this.data = new char[bytes.length - i - 1];
      System.arraycopy(bytes, i + 1, data, 0, bytes.length - i - 1);
    } else {
      this.cmd = (char[])bytes.clone();
      this.data = new char[0];
    }
  }

  /**
   * Create a new statement object from separate command and data arrays.
   *
   * @param cmd  the characters that make up the command
   * @param data the option characters that make up the data
   */
  public Statement(char[] cmd, char[] data) {
    this.cmd = (char[])cmd.clone();
    this.data = data == null ? new char[0] : (char[])data.clone();
  }

  /**
   * Creates a Statment from a string that contains both the command and
   * optional data.
   *
   * @param bytes the raw bytes
   */
  public Statement(String stmt) {
    this(stmt.toCharArray());
  }

  /**
   * Create a new statement object from separate command and data strings.
   *
   * @param cmd  the command string
   * @param data the optional data string
   */
  public Statement(String cmd, String data) {
    this(cmd.toCharArray(), data == null ? null : data.toCharArray());
  }

  /**
   * Retrieve the cmd.
   *
   * @return a char[] that is the cmd part of the statment
   */
  public char[] getCmd() {
    return cmd;
  }

  /**
   * set the cmd.
   *
   * @param cmd the raw characters for the command
   */
  public void setCmd(char[] cmd) {
    this.cmd = (char[])cmd.clone();
  }

  /**
   * retrieve the data.
   *
   * @return the data part of the statement
   */
  public char[] getData() {
    return data;
  }

  /**
   * set the data.
   *
   * @param data raw characters for data
   */
  public void setData(char[] data) {
    this.data = (char[])data.clone();
  }

  /**
   * Converts the statement to bytes that can be sent across the wire.
   *
   * @return the whole statement as characters
   */
  public char[] toChars() {
    char[] result;
    if(this.data.length == 0) {
      result = (char [])this.cmd.clone();
    } else {
      result = new char[this.cmd.length + 1 + this.data.length];
      System.arraycopy(this.cmd, 0, result, 0, this.cmd.length);
      result[this.cmd.length] = SP;
      System.arraycopy
        (this.data, 0, result, this.cmd.length + 1, this.data.length);
    }
    return result;
  }

  /**
   * Compares two statements for equality.
   *
   * @param other
   * @return true if the two Statements are equivalent
   */
  public boolean equals(Object o) {
    Statement other = (Statement)o;
    return Arrays.equals(this.cmd, other.cmd) &&
           Arrays.equals(this.data, other.data);
  }

  /**
   * Creates a new ERROR message with the given error message.
   *
   * @param s the error message
   * @return A statement object contining the CMD ERROR and the message
   */
  public static Statement getErrorStatement(String data) {
    return new Statement(ERROR_CHARS, data.toCharArray());
  }

  /**
   * Creates a new OK message with the given payload.
   */
  public static Statement getOkStatement(String data) {
    return new Statement(OK_CHARS, data.toCharArray());
  }

  /**
   * Creates a new Start message with the current Inca Protocol Version.
   *
   * @return The start message with current version.
   */
  public static Statement getStartStatement() {
    return new Statement(START_CHARS, VERSION.toCharArray());
  }

  /**
   * Returns the Inca protocol version.
   * @return Inca protocol version
   */
  public static String getVersion() {
    return VERSION;
  }

  public String toString() {
    return new String(this.toChars());
  }

}
