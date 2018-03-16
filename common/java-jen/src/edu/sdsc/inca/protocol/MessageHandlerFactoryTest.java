package edu.sdsc.inca.protocol;

import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.Statement;
import java.io.IOException;
import java.io.StringReader;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Cathie Olschanowski
 * @author Jim Hayes
 */

public class MessageHandlerFactoryTest extends TestCase {

  static final private String CRLF = "\r\n";

  public void setUp() throws Exception {
    super.setUp();
    MessageHandlerFactory.registerMessageHandler
      ("PING", "edu.sdsc.inca.protocol.Ping");
    MessageHandlerFactory.registerMessageHandler
      ("START","edu.sdsc.inca.protocol.VerifyProtocolVersion");
  }

  /**
   * Test instantiating the START command.
   *
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws IOException
   * @throws ProtocolException
   */
  public void testStart()
    throws ClassNotFoundException, IllegalAccessException,
           InstantiationException, IOException, ProtocolException {
    ProtocolReader in = new ProtocolReader(
      new StringReader(Statement.getStartStatement() + CRLF)
    );
    MessageHandler task = MessageHandlerFactory.newInstance(in);
    Assert.assertNotNull(task);
  }

  /**
   * Test the empty stream case.
   * In this case null should be returned indicating that the end of the stream
   * has been reached
   */
  public void testEmptyStream() {
    ProtocolReader in = new ProtocolReader(new StringReader(""));
    MessageHandler task = null;
    try {
      task = MessageHandlerFactory.newInstance(in);
    } catch(Exception e) {
      fail(e.toString());
    }
    Assert.assertNull(task);
  }

  /**
   * If the stream only contains CRLF then there should be a protocol
   * exception.
   */
  public void testCRLFOnly() {
    ProtocolReader in = new ProtocolReader(new StringReader(CRLF));
    MessageHandler task = null;
    try {
      task = MessageHandlerFactory.newInstance(in);
      fail("Missing exception on empty command");
    } catch(ProtocolException e) {
      // empty
    } catch(Exception e) {
      fail(e.toString());
    }
  }

  /**
   * Incomplete statement is a protocol exception.
   */
  public void testIncompleteStatement() {
    ProtocolReader in =
      new ProtocolReader(new StringReader(Protocol.PING_COMMAND));
    MessageHandler task = null;
    try {
      task = MessageHandlerFactory.newInstance(in);
      fail("Missing exception on incomplete command");
    } catch(ProtocolException e) {
      // empty
    } catch(Exception e) {
      fail(e.toString());
    }
  }

  /**
   * Unknown command is a protocol exception.
   */
  public void testUnknownCommand() {
    ProtocolReader in = new ProtocolReader(new StringReader("HUH DATA" + CRLF));
    MessageHandler task = null;
    try {
      task = MessageHandlerFactory.newInstance(in);
      fail("Missing exception on unknown command");
    } catch (ProtocolException e) {
      // empty
    } catch(Exception e) {
      fail(e.toString());
    }
  }

}
