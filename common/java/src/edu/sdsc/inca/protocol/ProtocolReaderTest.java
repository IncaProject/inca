package edu.sdsc.inca.protocol;

import java.io.IOException;
import java.io.StringReader;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Tests basic functionality of the Protocol Reader.
 */
public class ProtocolReaderTest extends TestCase {

  String COMMAND = "command";
  String CRLF = "\r\n";
  String CR = "\r";
  String DATA = "data";
  String SP = " ";

  /**
   * This tests the ability to peek at the next command.
   */
  public void testPeek() throws IOException, ProtocolException {
    String text = COMMAND + SP + DATA;
    String textWithCrlf = text + CRLF;
    ProtocolReader pr = new ProtocolReader(new StringReader(textWithCrlf));
    Assert.assertEquals(pr.peekCommand(), COMMAND);
  }

  /**
   * This tests the ability to read a simple command.
   */
  public void testRead() throws IOException, ProtocolException {
    String text = COMMAND + SP + DATA;
    String textWithCrlf = text + CRLF;
    ProtocolReader pr = new ProtocolReader(new StringReader(textWithCrlf));
    Statement correct = new Statement(text.toCharArray());
    Statement read = pr.readStatement();
    Assert.assertEquals(correct, read);
  }

  /**
   * This tests reading a Statement that contains a carriage return.
   */
  public void testReadWithCr() throws IOException, ProtocolException {
    String text = COMMAND + SP + DATA + CR + DATA;
    String textWithCrlf = text + CRLF;
    ProtocolReader pr = new ProtocolReader(new StringReader(textWithCrlf));
    Statement correct = new Statement(text.toCharArray());
    Statement read = pr.readStatement();
    Assert.assertEquals(correct, read);
  }

}
