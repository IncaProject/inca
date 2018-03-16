package edu.sdsc.inca;

import junit.framework.TestCase;
import edu.sdsc.inca.protocol.Protocol;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Jim Hayes
 */
public class WrapResourceTest extends TestCase {

  /**
   * Tests the WrapResource constructor.
   */
  public void testConstructor() throws Exception {
    WrapResource r1 = new WrapResource();
    assertEquals(0, r1.getInheritedMacroNames().length);
    assertEquals(0, r1.getLocalMacroNames().length);
    assertNull(r1.getMacroValue(Protocol.COMPUTE_PORT_MACRO));
  }

  /**
   * Tests the WrapResource equals method on empty resources.
   */
  public void testDifferencesNoSeries() throws Exception {
    WrapResource r1 = new WrapResource();
    WrapResource r2 = new WrapResource();
    assertTrue(r1.equals(r2));
  }

  public static final String HOST = "blue.ufo.edu";
  public static final String PORT = "1234";
  public static final String DN = "/CN=farf";
  public static final String SERVICE = "volley";
  public static final String HOST_PORT = HOST + ":" + PORT;
  public static final String HOST_PORT_SERVICE = HOST_PORT + "/" + SERVICE;
  public static final String HOST_SERVICE = HOST + "/" + SERVICE;
  public static final String HOST_DN = HOST + ":" + DN;
  public static final String HOST_PORT_DN = HOST_PORT + ":" + DN;
  public static final String HOST_SERVICE_DN = HOST_SERVICE + ":" + DN;
  public static final String HOST_PORT_SERVICE_DN = HOST_PORT_SERVICE + ":" +DN;

  /**
   * Tests the WrapResource server parser.
   */
  public void testServer() throws Exception {

    WrapResource r1 = new WrapResource();
    assertEquals("", r1.getServer(WrapResource.PROXY_SERVER));

    r1.setServer(WrapResource.PROXY_SERVER, HOST);
    assertEquals(HOST, r1.getServer(WrapResource.PROXY_SERVER));
    assertEquals(HOST, r1.getMacroValue(Protocol.MYPROXY_HOST_MACRO));
    assertNull(r1.getMacroValue(Protocol.MYPROXY_PORT_MACRO));
    assertNull(r1.getMacroValue(Protocol.GRAM_SERVICE_MACRO));
    assertNull(r1.getMacroValue(Protocol.MYPROXY_DN_MACRO));

    r1.setServer(WrapResource.PROXY_SERVER, HOST_PORT);
    assertEquals(HOST_PORT, r1.getServer(WrapResource.PROXY_SERVER));
    assertEquals(HOST, r1.getMacroValue(Protocol.MYPROXY_HOST_MACRO));
    assertEquals(PORT, r1.getMacroValue(Protocol.MYPROXY_PORT_MACRO));
    assertNull(r1.getMacroValue(Protocol.GRAM_SERVICE_MACRO));
    assertNull(r1.getMacroValue(Protocol.MYPROXY_DN_MACRO));

    r1.setServer(WrapResource.PROXY_SERVER, HOST_DN);
    assertEquals(HOST_DN, r1.getServer(WrapResource.PROXY_SERVER));
    assertEquals(HOST, r1.getMacroValue(Protocol.MYPROXY_HOST_MACRO));
    assertNull(r1.getMacroValue(Protocol.MYPROXY_PORT_MACRO));
    assertNull(r1.getMacroValue(Protocol.GRAM_SERVICE_MACRO));
    assertEquals(DN, r1.getMacroValue(Protocol.MYPROXY_DN_MACRO));

    r1.setServer(WrapResource.PROXY_SERVER, HOST_PORT_DN);
    assertEquals(HOST_PORT_DN, r1.getServer(WrapResource.PROXY_SERVER));
    assertEquals(HOST, r1.getMacroValue(Protocol.MYPROXY_HOST_MACRO));
    assertEquals(PORT, r1.getMacroValue(Protocol.MYPROXY_PORT_MACRO));
    assertNull(r1.getMacroValue(Protocol.GRAM_SERVICE_MACRO));
    assertEquals(DN, r1.getMacroValue(Protocol.MYPROXY_DN_MACRO));

    r1.setServer(WrapResource.COMPUTE_SERVER, HOST_PORT_SERVICE);
    assertEquals(HOST_PORT_SERVICE, r1.getServer(WrapResource.COMPUTE_SERVER));
    assertEquals(HOST, r1.getMacroValue(Protocol.COMPUTE_SERVER_MACRO));
    assertEquals(PORT, r1.getMacroValue(Protocol.COMPUTE_PORT_MACRO));
    assertEquals(SERVICE, r1.getMacroValue(Protocol.GRAM_SERVICE_MACRO));
    assertNull(r1.getMacroValue(Protocol.GRAM_DN_MACRO));

    r1.setServer(WrapResource.COMPUTE_SERVER, HOST_PORT_SERVICE_DN);
    assertEquals
      (HOST_PORT_SERVICE_DN, r1.getServer(WrapResource.COMPUTE_SERVER));
    assertEquals(HOST, r1.getMacroValue(Protocol.COMPUTE_SERVER_MACRO));
    assertEquals(PORT, r1.getMacroValue(Protocol.COMPUTE_PORT_MACRO));
    assertEquals(SERVICE, r1.getMacroValue(Protocol.GRAM_SERVICE_MACRO));
    assertEquals(DN, r1.getMacroValue(Protocol.GRAM_DN_MACRO));

    r1.setServer(WrapResource.COMPUTE_SERVER, HOST_SERVICE);
    assertEquals(HOST_SERVICE, r1.getServer(WrapResource.COMPUTE_SERVER));
    assertEquals(HOST, r1.getMacroValue(Protocol.COMPUTE_SERVER_MACRO));
    assertNull(r1.getMacroValue(Protocol.COMPUTE_PORT_MACRO));
    assertEquals(SERVICE, r1.getMacroValue(Protocol.GRAM_SERVICE_MACRO));
    assertNull(r1.getMacroValue(Protocol.GRAM_DN_MACRO));

  }

}
