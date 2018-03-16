package edu.sdsc.inca.protocol;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.log4j.Logger;


/**
 * @author Cathie Olschanowsky
 *
 * A buffered protocol writer. The same as a buffered Writer with the addition
 * of a write method that will accept a Statement.
 *
 * @depend - uses - inca.protocol.Statement
 */

public class ProtocolWriter extends BufferedWriter {

  private static final char[] CRLF = "\r\n".toCharArray();
  private static final char SP = ' ';

  // Protected class vars
  protected static Logger logger = Logger.getLogger(ProtocolWriter.class);

  // Instance variables
  protected boolean closed = false;

  /**
   * Create a buffered character-output stream that uses a default-sized
   * output buffer.
   *
   * @param writer the writer to send the data to
   */
  public ProtocolWriter(Writer writer) {
    super(writer);
  }

  /**
   * Create a new buffered character-output stream that uses an output
   * buffer of the given size.
   *
   * @param writer to direct the output to
   * @param i      buffer size
   */
  public ProtocolWriter(Writer writer, int i) {
    super(writer, i);
  }

  /**
   * Create a buffered character-output stream that uses a default-sized
   * output buffer.
   *
   * @param output the stream to send the data to
   */
  public ProtocolWriter(OutputStream output) {
    super(new OutputStreamWriter(output));
  }

  /**
   * Create a new buffered character-output stream that uses an output
   * buffer of the given size.
   *
   * @param output the stream to direct the output to
   * @param i      buffer size
   */
  public ProtocolWriter(OutputStream output, int i) {
    super(new OutputStreamWriter(output), i);
  }

  /**
   * Closes the writer.
   *
   * @throws IOException on a close error
   */
  public void close() throws IOException {
    this.closed = true;
    super.close();
  }

  /**
   * Indicates whether or not the writer has been closed.
   *
   * @return true if the writer is closed, else false
   */
  public boolean isClosed() {
    return this.closed;
  }

  /**
   * Write a statement to the protocolWriter.
   *
   * @param statement data to write
   * @throws IOException if the write fails
   */
  public void write(Statement statement) throws IOException {
    logger.debug("Write '" + statement.toString() + "'");
    this.write(statement.toChars());
    this.write(CRLF);
    this.flush();
  }

	/**
	 * Write a statement to the protocolWriter.
	 *
	 * @param command command for the statement to write
	 * @param inStream data for the statement to write
	 * @throws IOException if the write fails
	 */
	public void write(String command, Reader inStream) throws IOException
	{
		write(command.toCharArray());
		write(SP);

		char[] readBuffer = new char[8192];
		int charsRead;

		while ((charsRead = inStream.read(readBuffer, 0, readBuffer.length)) >= 0)
			write(readBuffer, 0, charsRead);

		write(CRLF);
		flush();
	}
}
