package edu.sdsc.inca.depot.persistent;


import edu.sdsc.inca.dataModel.suite.SuiteDocument;


/**
 * Test the functionality of the Suite and SuiteDAO classes.
 */
public class SuiteTest extends PersistentTest {

  /**
   * Test the Suite class constructors.
   */
  public void testConstructors() throws Exception {
    Suite aSuite = new Suite();
    assertEquals(0, aSuite.getVersion());
    assertNotNull(aSuite.getName());
    assertNotNull(aSuite.getGuid());
    assertNotNull(aSuite.getDescription());
    assertNotNull(aSuite.getSeriesConfigs());
    aSuite = new Suite("someName", "RA:someName", "a test suite");
    assertEquals(0, aSuite.getVersion());
    assertEquals("someName", aSuite.getName());
    assertEquals("RA:someName", aSuite.getGuid());
    assertEquals("a test suite", aSuite.getDescription());
    assertNotNull(aSuite.getSeriesConfigs());
  }

  /**
   * Test SuiteDAO's fromBean method.
   */
  public void testFromBean() throws Exception {
    Suite s = Suite.generate("aSuite", 3);
    SuiteDocument doc = SuiteDocument.Factory.parse(s.toXml());
    Suite aSuite = new Suite().fromBean(doc.getSuite());
    assertEquals(s.getVersion(), aSuite.getVersion());
    assertEquals(s.getName(), aSuite.getName());
    assertEquals(s.getGuid(), aSuite.getGuid());
    assertEquals(s.getDescription(), aSuite.getDescription());
    assertEquals(s.getSeriesConfigs().size(), aSuite.getSeriesConfigs().size());
  }

}
