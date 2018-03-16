package edu.sdsc.inca;

import edu.sdsc.inca.util.ConfigProperties;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import junit.framework.TestCase;

/**
 * None of the tests in this class actually start the server... All the tests that do that are in
 * serverClientTest.
 * <p/>
 * These are to test the individual functions that the server depends on.
 */
public class ServerTest extends TestCase {

  /**
   * Test that the constructor finishes correctly and the shutdown hook was set.
   */
  public void testConstructor() {
    Server server = new Server();
    assertNotNull(server);
  }

  /**
   * Test that a valid key file can be read.
   */
  public void testSetCertificate() {
    Server server = new Server();
    assertNull(server.getCertificatePath());
    try {
      ConfigProperties config = new ConfigProperties();
      config.putAllTrimmed(System.getProperties(), "inca.component.");
      config.loadFromStream
        (Component.openResourceStream("inca.properties"), "inca.component.");
      server.setConfiguration(config);
      server.setCertificatePath("servercert.pem");
      assertNotNull(server.getCertificatePath());
    } catch(Exception e) {
      fail("i/o problem: " + e);
    }
  }

  /**
   * Test that a valid key file can be read.
   */
  public void testSetKey() {
    Server server = new Server();
    assertNull(server.getCertificatePath());
    try {
      ConfigProperties config = new ConfigProperties();
      config.putAllTrimmed(System.getProperties(), "inca.component.");
      config.loadFromStream
        (Component.openResourceStream("inca.properties"), "inca.component.");
      server.setConfiguration(config);
      server.setKeyPath("serverkey.pem");
      assertNotNull(server.getKeyPath());
    } catch(Exception e) {
      fail("i/o problem: " + e);
    }
  }

  public void testOpenResourceStream() throws IOException {
    File f = File.createTempFile("foo", null);
    FileWriter output = new FileWriter(f);
    output.close();
    assertNotNull(Component.openResourceStream(f.getAbsolutePath()));
  }

}
