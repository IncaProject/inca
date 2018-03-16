package edu.sdsc.inca.protocol;

import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;
import java.io.StringReader;
import java.io.StringWriter;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test basic functionality of VerifyProtocolVersion
 */
public class VerifyProtocolVersionTest extends TestCase {

  public final static String CRLF = "\r\n";

  /**
   * test finding matching versions.
   *
   * @throws Exception
   */
  public void testMatch() throws Exception {
    Statement call = Statement.getStartStatement();
    ProtocolReader pr = new ProtocolReader(new StringReader(call + CRLF));
    StringWriter sw = new StringWriter();
    ProtocolWriter pw = new ProtocolWriter(sw);
    VerifyProtocolVersion vpv = new VerifyProtocolVersion();
    vpv.execute(pr, pw, null);
    pw.flush();
    Statement response = new Statement(sw.toString().replaceAll(CRLF, ""));
    Assert.assertEquals
      (response, Statement.getOkStatement(Statement.getVersion()));
  }

  /**
   * test finding mismatched versions.
   *
   * @throws Exception
   */
  public void testMismatch() throws Exception {
    Statement call = Statement.getStartStatement();
    call.setData("X".toCharArray());
    ProtocolReader pr = new ProtocolReader(new StringReader(call + CRLF));
    StringWriter sw = new StringWriter();
    ProtocolWriter pw = new ProtocolWriter(sw);
    VerifyProtocolVersion vpv = new VerifyProtocolVersion();
    vpv.execute(pr, pw, null);
    pw.flush();
    Statement response = new Statement(sw.toString().replaceAll(CRLF, ""));
    Assert.assertEquals
      (new String(response.getCmd()), Protocol.FAILURE_COMMAND);
  }

}
