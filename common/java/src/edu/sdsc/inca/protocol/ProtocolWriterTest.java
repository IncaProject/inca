package edu.sdsc.inca.protocol;

import java.io.IOException;
import java.io.StringWriter;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * User: cmills
 * Date: Feb 28, 2005
 * Time: 10:39:25 AM
 * Tests writing a char array and a statement to the protocol writer.
 */
public class ProtocolWriterTest extends TestCase {

  public static final String CRLF = "\r\n";
  public static final String TEST_STRING = "this is a test";

  /**
   * Test writing a simple char array.
   *
   * @throws IOException
   */
  public void testBasicCharArray() throws IOException {
    StringWriter sw = new StringWriter();
    ProtocolWriter pw = new ProtocolWriter(sw);
    pw.write("this is a test".toCharArray());
    pw.flush();
    String output = sw.toString().replaceAll(CRLF, "");;
    Assert.assertTrue(output.equals(TEST_STRING));
  }

  /**
   * Test writing a simple statement.
   *
   * @throws IOException
   * @throws ProtocolException
   */
  public void testStatement() throws IOException, ProtocolException {
    StringWriter sw = new StringWriter();
    ProtocolWriter pw = new ProtocolWriter(sw);
    Statement errorStatement = Statement.getErrorStatement(TEST_STRING);
    pw.write(errorStatement);
    pw.flush();
    String output = sw.toString().replaceAll(CRLF, "");;
    Assert.assertEquals(errorStatement, new Statement(output));
  }

}
