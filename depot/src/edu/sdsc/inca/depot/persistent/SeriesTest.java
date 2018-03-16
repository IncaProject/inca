package edu.sdsc.inca.depot.persistent;

import java.util.Set;
import java.util.HashSet;

/**
 * Basic tests to check for Series functionality.
 */

public class SeriesTest extends PersistentTest {


  public void testConstructors(){

    Series series = new Series();
    assertNotNull(series);

    // give the series some arguments
    series.setArgSignature(new ArgSignature());
    Set<Arg> args = new HashSet<Arg>();
    args.add(new Arg("name","value"));
    series.getArgSignature().setArgs(args);

    // populate the basic properties
    series.setResource("hostname.org");
    series.setContext("execute this");
    series.setReporter("reporter");

    // create another one with a different constructor
    Series series2 = new Series("hostname.org", "execute this", "reporter");
    series2.setArgs(args);
    assertEquals(series, series2);

    // and on last one
    ArgSignature argsig = new ArgSignature();
    argsig.setArgs(args);
    Series series3 = new Series("hostname.org", "execute this","reporter");
    series3.setArgSignature(argsig);
    assertTrue(series2.equals(series3));


  }
  /**
   * Tests loading a single instance.
   *
   */
  public void testPersistence(){

    Series series = new Series();
    assertNotNull(series);

    // give the series some arguments
    series.setArgSignature(new ArgSignature());

    Set<Arg> args = new HashSet<Arg>();
    args.add(new Arg("name","value"));
    series.getArgSignature().setArgs(args);

    // populate the basic properties
    series.setResource("hostname.org");
    series.setContext("execute this");
    series.setReporter("reporter");

    try {
      SeriesDAO.loadOrSave(series);
      assertNotNull("Series loaded from db", SeriesDAO.load( series ));
    } catch (PersistenceException e) {
      e.printStackTrace();
      fail(e.toString());
    }

    Series series2 = new Series();
    series2.setResource("hostname.org");
    series2.setReporter("reporter");
   try {
      SeriesDAO.loadOrSave(series2);
      Series series3 = SeriesDAO.load( series2 );
      logger.debug( "Context is '" + series3.getContext() + "'" );
    } catch (PersistenceException e) {
      e.printStackTrace();
      fail(e.toString());
    }

  }

}
