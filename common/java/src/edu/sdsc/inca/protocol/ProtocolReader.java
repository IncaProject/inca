package edu.sdsc.inca.protocol;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import javax.net.ssl.SSLException;

import org.apache.log4j.Logger;


/**
 * @author Cathie Olschanowsky
 * @author Jim Hayes
 *
 * Used for efficiently handling the input streams that hold inca
 * protocol bytes.
 * <p/>
 * To use it
 * <p/>
 * reader = new ProtocolReader(new InputStreamReader(inputStream));
 * reader.peekCommand
 * reader.getStatement ...
 *
 * @depend - uses - inca.protocol.Statement
 */
public class ProtocolReader extends BufferedReader {

  public static final int MAX_COMMAND_LENGTH = 512;
  public static final int CR = '\r';
  public static final int LF = '\n';
  public static final int SP = ' ';

  // Protected class vars
  protected static Logger logger = Logger.getLogger(ProtocolReader.class);

  // Instance variables
  protected boolean closed = false;

  /**
   * Create a buffering character-input stream that uses an input
   * buffer of the specified size.
   *
   * @param reader the reader that the data can be read from
   * @param i size of the input buffer
   */
  public ProtocolReader(Reader reader, int i) {
    super(reader, i);
  }

  /**
   * Create a buffering protocol-input stream that uses a default-sized
   * input buffer.
   *
   * @param reader the reader that the protocol can be read from
   */
  public ProtocolReader(Reader reader) {
    super(reader);
  }

  /**
   * Create a buffering character-input stream that uses an input
   * buffer of the specified size.
   *
   * @param input the stream that the data can be read from
   * @param i size of the input buffer
   */
  public ProtocolReader(InputStream input, int i) {
    super(new InputStreamReader(input), i);
  }

  /**
   * Create a buffering protocol-input stream that uses a default-sized
   * input buffer.
   *
   * @param input the stream that the protocol can be read from
   */
  public ProtocolReader(InputStream input) {
    super(new InputStreamReader(input));
  }

  /**
   * Closes the reader.
   *
   * @throws IOException on a close error
   */
  public void close() throws IOException {
    this.closed = true;
    super.close();
  }

  /**
   * Indicates whether or not the reader has been closed.
   *
   * @return true if the reader is closed, else false
   */
  public boolean isClosed() {
    return this.closed;
  }

  /**
   * Returns the next byte from the input stream; -1 on EOF.
   */
  private int readAByte() throws IOException, ProtocolException {
    int result;
    try {
      result = this.read();
    } catch(SSLException e) {
      // Raised if the peer closes the connection during the SSL handshake.
      throw new ProtocolException("Unable to complete SSL handshake");
    }
    return result;
  }

  /**
   * Returns the next command from the stream, or null on end.  The command
   * will be re-read by the next call to readStatement.
   *
   * @return the next command
   * @throws IOException on a read error
   * @throws ProtocolException if the contents of the read are malformed
   */
  public String peekCommand() throws IOException, ProtocolException {
    char[] buffer = new char[MAX_COMMAND_LENGTH];
    int length;
    this.mark(MAX_COMMAND_LENGTH);
    for(length = 0; length < buffer.length; length++) {
      int b = this.readAByte();
      if(b < 0) { // EOF
        if(length > 0) {
          logger.error("Unterminated statement in input stream");
          throw new ProtocolException("Unterminated statement in input stream");
        } else {
          logger.debug("Peer closed connection");
          return null;
        }
      }
      if(b == SP || b == CR) {
        break;
      }
      buffer[length] = (char)b;
    }
    this.reset();
    String result = new String(buffer, 0, length);
    logger.debug("Peeked at '" + result + "'");
    return result;
  }

	/**
	 * Reads a complete statement from the input stream, or null if the stream
	 * has been closed.
	 *
	 * @return the next statement -- complete
	 * @throws IOException on a read error
	 * @throws ProtocolException if the contents of the read are malformed
	 */
	public Statement readStatement() throws IOException, ProtocolException
	{
		ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
		Writer outStream = new BufferedWriter(new OutputStreamWriter(outBytes));
		String command = readStatement(outStream);

		if (command == null)
			return null;

		outStream.flush();

		return new Statement(command, outBytes.toString());
	}

	/**
	 * Reads a complete statement from the input stream, or null if the stream
	 * has been closed. The data portion of the statement is written to the given
	 * output stream.
	 * 
	 * @param outStream a stream to write the statement data to
	 * @return the command
	 * @throws IOException on a read error
	 * @throws ProtocolException if the contents of the read are malformed
	 */
	public String readStatement(Writer outStream) throws IOException, ProtocolException
	{
		byte[] command = new byte[MAX_COMMAND_LENGTH];
		int commandLength = 0;
		boolean lastCharWasCR = false;

		while (commandLength < MAX_COMMAND_LENGTH) {
			int nextByte = readAByte();

			if (nextByte < 0) {
				if (commandLength > 0) {
					String message = "Unterminated statement in input stream";

					logger.error(message);

					throw new ProtocolException(message);
				}
				else {
					logger.debug("Peer closed connection");

					return null;
				}
			}

			if (lastCharWasCR == true) {
				if (nextByte == LF)
					return new String(command, 0, commandLength - 1);
				else
					lastCharWasCR = false;
			}

			if (nextByte == CR)
				lastCharWasCR = true;
			else if (nextByte == SP)
				break;

			command[commandLength++] = (byte) nextByte;
		}

		while (true) {
			int nextByte = readAByte();

			if (nextByte < 0) {
				String message = "Unterminated statement in input stream";

				logger.error(message);

				throw new ProtocolException(message);
			}

			if (lastCharWasCR == true) {
				if (nextByte == LF)
					break;
				else {
					outStream.write(CR);

					lastCharWasCR = false;
				}
			}

			if (nextByte == CR) {
				lastCharWasCR = true;

				continue;
			}

			outStream.write(nextByte);
		}

		return new String(command, 0, commandLength);
	}
}
