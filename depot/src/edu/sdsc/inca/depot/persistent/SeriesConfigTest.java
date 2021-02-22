package edu.sdsc.inca.depot.persistent;


import edu.sdsc.inca.dataModel.suite.SuiteDocument;


/**
 * Test the functionality of the SeriesConfig and SeriesConfigDAO classes.
 */
public class SeriesConfigTest extends PersistentTest {

  /**
   * Test the SeriesConfig class constructors.
   */
  public void testConstructors() {
    SeriesConfig sc = new SeriesConfig();
    assertEquals(-1, sc.getActivated());
    assertEquals(-1, sc.getDeactivated());
  }

  /**
   * Test SeriesConfigDAO's fromBean method.
   */
  public void testFromBean() throws Exception {

    Suite s = Suite.generate("aSuite", 3);
    SeriesConfig sc = s.getSeriesConfig(0);
    Limits scLimits = new Limits(5, 3, 2);
    sc.setLimits(scLimits);
    Schedule scSchedule =
      new Schedule("1", "2", "3", "4", "5", "cron", Integer.valueOf(1));
    sc.setSchedule(scSchedule);
    AcceptedOutput scAo = new AcceptedOutput("a", "b");
    sc.setAcceptedOutput(scAo);

    SuiteDocument doc = SuiteDocument.Factory.parse(s.toXml());
    SeriesConfig fromBean = new SeriesConfig().fromBean
      (doc.getSuite().getSeriesConfigs().getSeriesConfigArray()[0]);
    Schedule fbSchedule = fromBean.getSchedule();
    Limits fbLimits = fromBean.getLimits();
    AcceptedOutput fbAo = fromBean.getAcceptedOutput();

    assertEquals(sc.getActivated(), sc.getActivated());
    assertEquals(sc.getDeactivated(), sc.getDeactivated());
    assertNotNull(fbLimits);
    assertEquals(scLimits.getWallClockTime(), fbLimits.getWallClockTime());
    assertEquals(scLimits.getMemory(), fbLimits.getMemory());
    assertEquals(scLimits.getCpuTime(), fbLimits.getCpuTime());
    assertNotNull(fbAo);
    assertEquals(scAo.getComparison(), fbAo.getComparison());
    assertNotNull(fbSchedule);
    assertEquals(scSchedule.getType(), fbSchedule.getType());
    assertEquals(scSchedule.getMinute(), fbSchedule.getMinute());
    assertEquals(scSchedule.getHour(), fbSchedule.getHour());
    assertEquals(scSchedule.getMday(), fbSchedule.getMday());
    assertEquals(scSchedule.getWday(), fbSchedule.getWday());
    assertEquals(scSchedule.getMonth(), fbSchedule.getMonth());
    assertEquals(scSchedule.getNumOccurs(), fbSchedule.getNumOccurs());
    assertNull(fromBean.getSuite(0));
    assertNotNull(fromBean.getSeries());

  }

}
