package edu.sdsc.inca.protocol;

import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test basic functionality of Permit
 */
public class PermitTest extends TestCase {

  public final static String CRLF = "\r\n";
  public final static String MESSAGE = "hello, sailor!";
  public final static String DN = "NotARealDN";
  public final static String ACTION = Protocol.PING_ACTION + " " + MESSAGE;
  public final static String PERMIT =
    Protocol.PERMIT_COMMAND + " " + DN + " " + ACTION;
  public final static String PING = Protocol.PING_COMMAND + " " + MESSAGE;
  private static File f;
  static {
    try {
      f = File.createTempFile
        ("ptest", "perm", new File(System.getProperty("user.dir")));
    } catch(IOException e) {
      // empty
    }
    f.deleteOnExit();
    MessageHandler.setPermissionsPath(f.getAbsolutePath());
  }

  public void testNoPermissions() throws Exception {
    MessageHandler.resetPermissions();
    ProtocolReader in = new ProtocolReader(new StringReader(PING + CRLF));
    StringWriter sw = new StringWriter();
    ProtocolWriter out = new ProtocolWriter(sw);
    try {
      new Ping().execute(in, out, null);
    } catch(Exception e) {
      fail("protocol exception: "+ e);
    }
    String reply = sw.toString();
    assertNotNull(reply);
    assertTrue(reply.startsWith("OK"));
    in = new ProtocolReader(new StringReader(PING + CRLF));
    sw = new StringWriter();
    out = new ProtocolWriter(sw);
    try {
      new Ping().execute(in, out, DN);
    } catch(Exception e) {
      fail("protocol exception: "+ e);
    }
    reply = sw.toString();
    assertNotNull(reply);
    assertTrue(reply.startsWith("OK"));
  }

  public void testPermit() throws Exception {
    MessageHandler.resetPermissions();
    ProtocolReader in = new ProtocolReader(new StringReader(PERMIT + CRLF));
    StringWriter sw = new StringWriter();
    ProtocolWriter out = new ProtocolWriter(sw);
    try {
      new Permit().execute(in, out, null);
    } catch(Exception e) {
      fail("protocol exception: "+ e);
    }
    String reply = sw.toString();
    assertNotNull(reply);
    assertTrue(reply.startsWith("OK"));
    in = new ProtocolReader(new StringReader(PING + CRLF));
    sw = new StringWriter();
    out = new ProtocolWriter(sw);
    try {
      new Ping().execute(in, out, DN);
    } catch(Exception e) {
      fail("protocol exception: "+ e);
    }
    reply = sw.toString();
    assertNotNull(reply);
    assertEquals("OK " + MESSAGE + CRLF, reply);
  }

  public void testForbidden() throws Exception {
    MessageHandler.resetPermissions();
    String otherPermit = PERMIT.replaceFirst(DN, "OtherDN");
    ProtocolReader in = new ProtocolReader(new StringReader(otherPermit+CRLF));
    StringWriter sw = new StringWriter();
    ProtocolWriter out = new ProtocolWriter(sw);
    try {
      new Permit().execute(in, out, null);
    } catch(Exception e) {
      fail("protocol exception: "+ e);
    }
    String reply = sw.toString();
    assertNotNull(reply);
    assertTrue(reply.startsWith("OK"));
    in = new ProtocolReader(new StringReader(PING + CRLF));
    sw = new StringWriter();
    out = new ProtocolWriter(sw);
    try {
      new Ping().execute(in, out, DN);
      fail("Not rejected");
    } catch(Exception e) {
      // empty
    }
  }

  public void testPersistence() throws Exception {

    String path = f.getAbsolutePath();
    MessageHandler.resetPermissions();
    Properties p = new Properties();
    try {
      p.load(new FileInputStream(path));
      fail(p.size() + " properties in file after reset");
    } catch(IOException e) {
      // empty
    }

    String permit = PERMIT.replaceFirst(DN, "CN=a").replaceFirst(MESSAGE, "X");
    ProtocolReader in = new ProtocolReader(new StringReader(permit + CRLF));
    StringWriter sw = new StringWriter();
    ProtocolWriter out = new ProtocolWriter(sw);
    try {
      new Permit().execute(in, out, null);
    } catch(Exception e) {
      fail("protocol exception: "+ e);
    }
    String reply = sw.toString();
    assertNotNull(reply);
    assertTrue(reply.startsWith("OK"));
    try {
      p.load(new FileInputStream(path));
    } catch(IOException e) {
      fail("Exception when reading non-empty properties:" + e);
    }
    assertEquals(1, p.size());

  }

  public void testMultipleSettings() throws Exception {

    MessageHandler.resetPermissions();

    ProtocolReader in = new ProtocolReader(new StringReader(PERMIT + CRLF));
    StringWriter sw = new StringWriter();
    ProtocolWriter out = new ProtocolWriter(sw);
    try {
      new Permit().execute(in, out, null);
    } catch(Exception e) {
      fail("protocol exception: "+ e);
    }
    String reply = sw.toString();
    assertNotNull(reply);
    assertTrue(reply.startsWith("OK"));

    String otherDN = "CN=a";
    String otherAction = "X";
    String permit = PERMIT.replaceFirst(ACTION, otherAction);
    in = new ProtocolReader(new StringReader(permit + CRLF));
    sw = new StringWriter();
    out = new ProtocolWriter(sw);
    try {
      new Permit().execute(in, out, null);
    } catch(Exception e) {
      fail("protocol exception: "+ e);
    }
    reply = sw.toString();
    assertNotNull(reply);
    assertTrue(reply.startsWith("OK"));

    assertTrue(MessageHandler.isPermitted(DN, ACTION));
    assertFalse(MessageHandler.isPermitted(otherDN, otherAction));

  }

}
