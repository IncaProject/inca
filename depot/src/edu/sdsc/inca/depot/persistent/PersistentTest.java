package edu.sdsc.inca.depot.persistent;


import java.util.Properties;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.Depot;


/**
 * Implement the setup and teardown function for all the PersistentTests.
 */
public abstract class PersistentTest extends TestCase {

  /**
   * logger used by all persistent tests.
   */
  protected static Logger logger = Logger.getLogger(PersistentTest.class);

  // # of tests to run before rebuilding DB
  protected static int sequentialTestsLeftToRun = 0;

  /**
   * Create the empty database to start with.
   *
   * @throws HibernateException
   */
  @Override
  public void setUp() throws Exception {

    if(sequentialTestsLeftToRun <= 0) {
      DatabaseTools.removeDatabase();
      DatabaseTools.initializeDatabase();
    }

    Depot.setRunningDepot(new Depot());

    super.setUp();
  }

  /**
   * clean up after tests.
   * @throws Exception
   */
  @Override
  public void tearDown() throws Exception {

    if(--sequentialTestsLeftToRun <= 0)
      DatabaseTools.removeDatabase();

    super.tearDown();
  }

  /**
   *
   * @param key
   * @param value
   * @throws ConfigurationException
   */
  protected void setDepotProperty(String key, String value) throws ConfigurationException {

	  Properties config = new Properties();

	  config.setProperty(key, value);

	  Depot.getRunningDepot().setConfiguration(config);
  }
}
