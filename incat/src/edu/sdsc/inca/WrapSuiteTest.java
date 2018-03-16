package edu.sdsc.inca;

import junit.framework.TestCase;
import edu.sdsc.inca.protocol.Protocol;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Jim Hayes
 */
public class WrapSuiteTest extends TestCase {

  static private final String NAME1 = "suite 1";
  static private final String NAME2 = "suite 2";
  static private final String DESCRIPTION = "a suite";
  static private final String REPORTER1 = "any.reporter.1";
  static private final String REPORTER2 = "any.reporter.2";
  static private final String REPORTER3 = "any.reporter.3";
  static private final String REPORTER4 = "any.reporter.4";
  static private final String REPORTER5 = "any.reporter.5";
  static private final String REPORTER6 = "any.reporter.6";

  /**
   * Tests the WrapSeries constructor.
   */
  public void testConstructor() throws Exception {
    WrapSuite suite = new WrapSuite(NAME1, DESCRIPTION);
    assertEquals(DESCRIPTION, suite.getDescription());
    assertEquals(NAME1, suite.getName());
    assertEquals(0, suite.getSeriesCount());
    assertNotNull(suite.getSuite());
    assertEquals(NAME1, suite.toString());
  }

  /**
   * Tests the WrapSuite differences method on empty suites.
   */
  public void testDifferencesNoSeries() throws Exception {
    WrapSuite s1 = new WrapSuite(NAME1, DESCRIPTION);
    WrapSuite s2 = new WrapSuite(NAME2, DESCRIPTION);
    WrapSuite diff = s1.differences(s2);
    assertEquals(s2.getName(), diff.getName());
    assertEquals(s2.getDescription(), diff.getDescription());
    assertEquals(0, diff.getSeriesCount());
  }

  /**
   * Tests the WrapSuite differences method when adding series.
   */
  public void testDifferencesAddSeries() throws Exception {
    WrapSuite s1 = new WrapSuite(NAME1, DESCRIPTION);
    WrapSuite s2 = new WrapSuite(NAME2, DESCRIPTION);
    WrapSeries series = s2.addNewSeries();
    series.setReporter(REPORTER1);
    series = s2.addNewSeries();
    series.setReporter(REPORTER2);
    WrapSuite diff = s1.differences(s2);
    assertEquals(2, diff.getSeriesCount());
    assertEquals(REPORTER1, diff.getSeriesAt(0).getReporter());
    assertEquals(Protocol.SERIES_CONFIG_ADD, diff.getSeriesAt(0).getAction());
    assertEquals(REPORTER2, diff.getSeriesAt(1).getReporter());
    assertEquals(Protocol.SERIES_CONFIG_ADD, diff.getSeriesAt(1).getAction());
  }

  /**
   * Tests the WrapSuite differences method when removing series.
   */
  public void testDifferencesDeleteSeries() throws Exception {
    WrapSuite s1 = new WrapSuite(NAME1, DESCRIPTION);
    WrapSuite s2 = new WrapSuite(NAME2, DESCRIPTION);
    WrapSeries series = s1.addNewSeries();
    series.setReporter(REPORTER1);
    series = s1.addNewSeries();
    series.setReporter(REPORTER2);
    WrapSuite diff = s1.differences(s2);
    assertEquals(2, diff.getSeriesCount());
    assertEquals(REPORTER1, diff.getSeriesAt(0).getReporter());
    assertEquals
      (Protocol.SERIES_CONFIG_DELETE, diff.getSeriesAt(0).getAction());
    assertEquals(REPORTER2, diff.getSeriesAt(1).getReporter());
    assertEquals
      (Protocol.SERIES_CONFIG_DELETE, diff.getSeriesAt(1).getAction());
  }

  /**
   * Tests the WrapSuite differences method on equivalent series.
   */
  public void testDifferencesEqual() throws Exception {
    WrapSuite s1 = new WrapSuite(NAME1, DESCRIPTION);
    WrapSuite s2 = new WrapSuite(NAME2, DESCRIPTION);
    WrapSeries series = s1.addNewSeries();
    series.setReporter(REPORTER1);
    series = s2.addNewSeries();
    series.setReporter(REPORTER1);
    series = s1.addNewSeries();
    series.setReporter(REPORTER2);
    series = s2.addNewSeries();
    series.setReporter(REPORTER2);
    WrapSuite diff = s1.differences(s2);
    assertEquals(0, diff.getSeriesCount());
  }

  /**
   * Tests the WrapSuite differences method on rearranged series.
   */
  public void testDifferencesRearranged() throws Exception {
    WrapSuite s1 = new WrapSuite(NAME1, DESCRIPTION);
    WrapSuite s2 = new WrapSuite(NAME2, DESCRIPTION);
    WrapSeries series = s1.addNewSeries();
    series.setReporter(REPORTER1);
    series = s2.addNewSeries();
    series.setReporter(REPORTER2);
    series = s1.addNewSeries();
    series.setReporter(REPORTER2);
    series = s2.addNewSeries();
    series.setReporter(REPORTER1);
    WrapSuite diff = s1.differences(s2);
    assertEquals(0, diff.getSeriesCount());
  }

  /**
   * Tests the WrapSuite differences method w/both additions and deletions.
   */
  public void testDifferencesMultiple() throws Exception {
    WrapSuite s1 = new WrapSuite(NAME1, DESCRIPTION);
    WrapSuite s2 = new WrapSuite(NAME2, DESCRIPTION);
    WrapSeries series = s1.addNewSeries();
    series.setReporter(REPORTER1);
    series = s1.addNewSeries();
    series.setReporter(REPORTER2);
    series = s1.addNewSeries();
    series.setReporter(REPORTER3);
    series = s1.addNewSeries();
    series.setReporter(REPORTER4);
    series = s2.addNewSeries();
    series.setReporter(REPORTER5);
    series = s2.addNewSeries();
    series.setReporter(REPORTER6);
    series = s2.addNewSeries();
    series.setReporter(REPORTER1);
    WrapSuite diff = s1.differences(s2);
    assertEquals(5, diff.getSeriesCount());
    // Series that should be in diff and their associated actions
    String[] changedReporters = new String[] {
      REPORTER2, REPORTER3, REPORTER4, REPORTER5, REPORTER6
    };
    String[] changedActions = new String[] {
      Protocol.SERIES_CONFIG_DELETE, Protocol.SERIES_CONFIG_DELETE,
      Protocol.SERIES_CONFIG_DELETE, Protocol.SERIES_CONFIG_ADD,
      Protocol.SERIES_CONFIG_ADD
    };
    // Make sure every series in diff is associated with the correct action ...
    for(int i = 0; i < diff.getSeriesCount(); i++) {
      String reporter = diff.getSeriesAt(i).getReporter();
      String action = diff.getSeriesAt(i).getAction();
      for(int j = 0; j < changedReporters.length; j++) {
        if(reporter.equals(changedReporters[j])) {
          assertEquals(changedActions[j], action);
          changedReporters[j] = "";
        }
      }
    }
    // ... and that all the expected series are in diff
    for(int i = 0; i < changedReporters.length; i++) {
      assertEquals("", changedReporters[i]);
    }
  }

}
