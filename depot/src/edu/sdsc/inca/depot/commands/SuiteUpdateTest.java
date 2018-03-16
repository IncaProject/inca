package edu.sdsc.inca.depot.commands;


import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Set;

import edu.sdsc.inca.depot.commands.SuiteUpdate;
import edu.sdsc.inca.depot.persistent.*;
import edu.sdsc.inca.protocol.ProtocolReader;


/**
 *
 * @author cmills
 * @author jhayes
 *
 */
public class SuiteUpdateTest extends PersistentTest {

  public void updateSuite(Suite s) throws Exception {
    SuiteUpdate su = new SuiteUpdate();
    String request = "SUITE " + s.toXml() + "\r\n";
    ProtocolReader reader = new ProtocolReader(new StringReader(request));
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    su.execute(reader, outBytes, null);
    if(outBytes.toString() == null) {
      throw new Exception("No output from SuiteUpdate");
    } else if(!outBytes.toString().startsWith("OK")) {
      throw new Exception(outBytes.toString());
    }
  }

  public void testBasicInsert() throws Exception {
    Suite[] suites = new Suite[] {
      Suite.generate("suite1", 1), Suite.generate("suite2", 4),
      Suite.generate("suite3", 2)
    };
    for(int i = 0; i < suites.length; i++) {
      try {
        updateSuite(suites[i]);
      } catch(Exception e) {
        fail("protocol exception:" + e);
      }
      assertNotNull(SuiteDAO.load(suites[i]));
    }
  }

  public void testImmediateInsert() throws Exception {
    Suite immediate = Suite.generate("immediate", 0);
    SeriesConfig sc = SeriesConfig.generate("localhost", "@@", 2);
    Schedule sched = new Schedule();
    sched.setType("immediate");
    sc.setSchedule(sched);
    immediate.addSeriesConfig(sc);
    try {
      updateSuite(immediate);
    } catch(Exception e) {
      fail("protocol exception:" + e);
    }
    assertNotNull(SuiteDAO.load(immediate));
  }

  public void testAddSeries() {
    int ADD_SERIES_COUNT = 5;
    Suite testSuite = Suite.generate("addTester", 0);
    try {
      updateSuite(testSuite);
    } catch(Exception e) {
      fail("protocol exception:" + e);
    }
    testSuite = Suite.generate("addTester", ADD_SERIES_COUNT);
    Suite dbSuite = null;
    try {
      updateSuite(testSuite);
      dbSuite = SuiteDAO.load(testSuite);
    } catch(Exception e) {
      fail("protocol exception:" + e);
    }
    assertNotNull(dbSuite);
    assertEquals(ADD_SERIES_COUNT, dbSuite.getSeriesConfigs().size());
  }

  public void testDeactivateSeries() {
    int ADD_SERIES_COUNT = 5;
    int DELETE_SERIES_COUNT = 2;
    Suite testSuite = Suite.generate("activeSuite", ADD_SERIES_COUNT);
    try {
      updateSuite(testSuite);
    } catch(Exception e) {
      fail("protocol exception:" + e);
    }
    Suite deleteSuite = Suite.generate("activeSuite", 0);
    for(int i = 0; i < DELETE_SERIES_COUNT; i++) {
      SeriesConfig sc = testSuite.getSeriesConfig(ADD_SERIES_COUNT - i - 1);
      sc.setAction("delete");
      deleteSuite.addSeriesConfig(sc);
    }
    Suite dbSuite = null;
    try {
      updateSuite(deleteSuite);
      dbSuite = SuiteDAO.load(testSuite);
    } catch(Exception e) {
      fail("protocol exception:" + e);
    }
    assertNotNull(dbSuite);
    Set<SeriesConfig> configs = dbSuite.getSeriesConfigs();
    assertEquals(ADD_SERIES_COUNT, configs.size());
    int deactivated = 0;
    for(Iterator<SeriesConfig> it = configs.iterator(); it.hasNext(); ) {
      SeriesConfig config = it.next();
      if(config.getDeactivated() != null) {
        deactivated++;
      }
    }
    assertEquals(DELETE_SERIES_COUNT, deactivated);
  }

  public void testReactivateSeries() {
    int ADD_SERIES_COUNT = 5;
    int DELETE_SERIES_COUNT = 4;
    int REACTIVATE_SERIES_COUNT = 2;
    Suite testSuite = Suite.generate("activeSuite", ADD_SERIES_COUNT);
    try {
      updateSuite(testSuite);
    } catch(Exception e) {
      fail("protocol exception:" + e);
    }
    Suite deleteSuite = Suite.generate("activeSuite", 0);
    for(int i = 0; i < DELETE_SERIES_COUNT; i++) {
      SeriesConfig sc = testSuite.getSeriesConfig(ADD_SERIES_COUNT - i - 1);
      sc.setAction("delete");
      deleteSuite.addSeriesConfig(sc);
    }
    try {
      updateSuite(deleteSuite);
    } catch(Exception e) {
      fail("protocol exception:" + e);
    }
    Suite reactivateSuite = Suite.generate("activeSuite", 0);
    for(int i = 0; i < REACTIVATE_SERIES_COUNT; i++) {
      SeriesConfig sc = testSuite.getSeriesConfig(ADD_SERIES_COUNT - i - 1);
      sc.setAction("add");
      reactivateSuite.addSeriesConfig(sc);
    }
    Suite dbSuite = null;
    try {
      updateSuite(reactivateSuite);
      dbSuite = SuiteDAO.load(testSuite);
    } catch(Exception e) {
      fail("protocol exception:" + e);
    }
    assertNotNull(dbSuite);
    Set<SeriesConfig> configs = dbSuite.getSeriesConfigs();
    assertEquals(ADD_SERIES_COUNT, configs.size());
    int deactivated = 0;
    for(Iterator<SeriesConfig> it = configs.iterator(); it.hasNext(); ) {
      SeriesConfig config = it.next();
      if(config.getDeactivated() != null) {
        deactivated++;
      }
    }
    assertEquals(DELETE_SERIES_COUNT - REACTIVATE_SERIES_COUNT, deactivated);
  }

}
