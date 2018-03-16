package edu.sdsc.inca.depot.persistent;

import edu.sdsc.inca.dataModel.report.ReportDocument;
import java.util.Iterator;
import java.util.Set;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;


/**
 * Tests basic Report functionality.
 */

public class ReportTest extends PersistentTest {

  /**
   * Create the Report object with all of the raw known info.
   * @param exec the reporter exec string
   * @param stderr the reporter stderr output
   * @return a report object incorporating the parameters
   * @throws org.apache.xmlbeans.XmlException
   */
  public Report createReport(String exec, String stderr) throws XmlException {

    Series s = Series.generate("localhost", exec, 4);

    // parse and validate xml
    ReportDocument doc = ReportDocument.Factory.parse(s.generateReport());

    // the object representation of the xml
    edu.sdsc.inca.dataModel.util.Report report = doc.getReport();

    //build the report
    Report result = new Report().fromBean(report);
    result.setStderr(stderr);
    result.setSeries(s);
    result.setRunInfo(new RunInfo().fromBean(report));
    return result;

  }

  public Report saveReportAndSeries(Report r) throws PersistenceException {
    Series dbSeries = SeriesDAO.load(r.getSeries());
    if(dbSeries != null) {
      dbSeries.addReport(r);
      SeriesDAO.update(dbSeries);
    } else {
      r.setSeries(SeriesDAO.loadOrSave(r.getSeries()));
    }
    r.setRunInfo(RunInfoDAO.loadOrSave(r.getRunInfo()));
    return ReportDAO.loadOrSave(r);
  }

  /**
   * Basic test of the constructor.
   */
  public void testConstructor() {

    // create a series for use in the comparison
    Series s = new Series();

    // start out with just populating a few reports by hand
    Report r = new Report();
    assertNotNull(r);
    r.setBody("<body>xml</body>");
    r.setExit_message(null);
    r.setExit_status(new Boolean(true));
    r.setStderr(null);
    r.setSeries(s);

    Report r2 = new Report(Boolean.valueOf(true), null, "<body>xml</body>", s);
    assertTrue(r.equals(r2));

  }

  /**
   * Test partitioning of long bodies.
   */
  public void testPartitioning() {
    String fifty = "abcdefghijklmnopqrstuvwxyABCDEFGHIJKLMNOPQRSTUVWXY";
    String hundred = fifty + fifty;
    String thousand = hundred + hundred + hundred + hundred + hundred +
                      hundred + hundred + hundred + hundred + hundred;
    String fourK = thousand + thousand + thousand + thousand;
    String eightK = fourK + fourK;
    String twelveK = fourK + fourK + fourK;
    String body = "<a>" + fourK.substring(0, PersistentObject.MAX_DB_LONG_STRING_LENGTH - 7) + "</a>";
    Report r = new Report();
    r.setBody(body);
    assertEquals(body, r.getBody());
    body = "<a>" + eightK.substring(0, PersistentObject.MAX_DB_LONG_STRING_LENGTH * 2 - 7) + "</a>";
    r.setBody(body);
    assertEquals(body, r.getBody());
    body = "<a>" + twelveK.substring(0, PersistentObject.MAX_DB_LONG_STRING_LENGTH * 3 - 7) + "</a>";
    r.setBody(body);
    assertEquals(body, r.getBody());
    assertTrue(r.getBodypart1().length() <=
               PersistentObject.MAX_DB_LONG_STRING_LENGTH);
    assertTrue(r.getBodypart2().length() <=
               PersistentObject.MAX_DB_LONG_STRING_LENGTH);
    assertTrue(r.getBodypart3().length() <=
               PersistentObject.MAX_DB_LONG_STRING_LENGTH);
    body = "<a>" + twelveK + "</a>";
    r.setBody(body);
    assertTrue(r.getBody().equals("") ||
               r.getBody().equals(PersistentObject.DB_EMPTY_STRING));
  }

  public void testXMLStuff() {

    Report r = null;
    try {
      r = createReport("some context", null);
    } catch (XmlException e) {
      fail(e.toString());
    }

    assertNotNull(r);
    assertNotNull(r.getSeries());

    Report r2 = null;
    try {
      r2 = createReport("another context", null);
    } catch (XmlException e) {
      fail(e.toString());
    }

    assertNotNull(r2);
    assertNotNull(r2.getSeries());
    Report r3 = null;
    try {
      r3 = createReport("a third context", null);
    } catch (XmlException e) {
      fail(e.toString());
    }

    assertNotNull(r3);
    assertNotNull(r3.getSeries());
    assertTrue(r3.getSeries().getResource().equals(
        r2.getSeries().getResource()));
    assertTrue
      (r3.getSeries().getReporter().equals(r2.getSeries().getReporter()));
    assertTrue(r3.getSeries().getArgSignature().equals(
        r2.getSeries().getArgSignature()));
    assertFalse(r3.getSeries().equals(r2.getSeries()));

  }

  /**
   * basic test of persisting a report.
   */
  public void testPersistReport() throws Exception {

    // get a new Report
    Report r = null;
    Report r2 = null;
    Report r3 = null;
    Report r4 = null;
    Report r5 = null;

    try {
      r = createReport("context first", null);
      r2 = createReport("context first", null);
      r3 = createReport("context second", null);
      r4 = createReport("context second", null);
      r5 = createReport("context third", null);
    } catch (XmlException e) {
      logger.error("XML problem", e);
      fail(e.toString());
    }
    assertNotNull(r);
    assertNotNull(r.getSeries());
    assertNull(r.getSeries().getId());

    // save that report to the database
    try {
      logger.debug("Attempting 1");
      r = saveReportAndSeries(r);
    } catch (PersistenceException e) {
      logger.error(e.toString(), e);
      fail(e.toString());
    }
    assertNotNull(r.getId());
    assertNotNull(r.getSeries().getId());
    // save an identical one to the database
    try {
      logger.debug("Attempting 2");
      r2 = saveReportAndSeries(r2);
    } catch (PersistenceException e) {
      logger.error(e.toString(), e);
      fail(e.toString());
    }
    assertNotNull(r2.getId());
    assertEquals
      (r.getSeries().getId().longValue(), r2.getSeries().getId().longValue());

    // save 2 that are different
    try {
      logger.debug("Attempting 4");
      r4 = saveReportAndSeries(r4);
      logger.debug("Attempting 5");
      r5 = saveReportAndSeries(r5);
    } catch (PersistenceException e) {
      logger.error(e.toString(), e);
      fail(e.toString());
    }
    assertNotNull(r4.getId());
    assertFalse(r4.getId().longValue() == r2.getId().longValue());
    assertNotNull(r5.getId());
    assertFalse(r5.getId().longValue() == r2.getId().longValue());

    //save a third
    try {
      logger.debug("Attempting 3");
      r3 = saveReportAndSeries(r3);
    } catch (PersistenceException e) {
      logger.error(e.toString(), e);
      fail(e.toString());
    }
    assertTrue
      (r3.getSeries().getId().longValue()==r4.getSeries().getId().longValue());

  }

}
