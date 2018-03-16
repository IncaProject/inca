package edu.sdsc.inca.depot.persistent;

import java.util.Properties;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.tool.hbm2ddl.SchemaExport;

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
  public void setUp() throws Exception {

    if(sequentialTestsLeftToRun <= 0) {
      // schema export will set up the actual database
      SchemaExport export = null;
      try {
        export = new SchemaExport(HibernateUtil.getConfiguration());
      } catch (HibernateException e) {
        logger.error("Can't create schema exporter", e);
        Assert.fail();
      }
      // Attempt to remove database in case it already exists.
      export.drop(false, true);
      export.create(false, true);
    }

    Depot.setRunningDepot(new Depot());

    super.setUp();

  }

  /**
   * clean up after tests.
   * @throws Exception
   */
  public void tearDown() throws Exception {

    HibernateUtil.closeSession();

    if(--sequentialTestsLeftToRun <= 0) {
      // schema export will cleanup up the actual database
      SchemaExport export = null;
      try {
        export = new SchemaExport(HibernateUtil.getConfiguration());
      } catch (HibernateException e) {
        logger.error("Can't create schema exporter",e);
        Assert.fail();
      }
      export.drop(false,true);
    }

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
