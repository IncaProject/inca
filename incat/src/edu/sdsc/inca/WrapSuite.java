package edu.sdsc.inca;


import java.util.ArrayList;

import org.apache.xmlbeans.XmlOptions;

import edu.sdsc.inca.dataModel.suite.Suite;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.dataModel.util.SeriesConfig;
import edu.sdsc.inca.protocol.Protocol;


/**
 * A class that wraps a suite, represented by an Inca XmlBean, with some
 * convenience methods.
 */
public class WrapSuite {

  protected SuiteDocument doc;
  protected Suite suite;
  protected ArrayList<WrapSeries> series;

  /**
   * Creates a new suite.
   *
   * @param name the suite name
   * @param description the suite description
   */
  public WrapSuite(String name, String description) {
    this.doc = SuiteDocument.Factory.newInstance();
    this.suite = this.doc.addNewSuite();
    this.suite.addNewSeriesConfigs();
    this.setName(name);
    this.setDescription(description);
    this.suite.setGuid("");
    this.suite.setVersion(java.math.BigInteger.valueOf(1));
    this.series = new ArrayList<WrapSeries>();
  }

  /**
   * Creates a new WrapSuite to wrap an existing Suite.
   *
   * @param suite the suite to wrap
   */
  public WrapSuite(Suite suite) {
    this.doc = SuiteDocument.Factory.newInstance();
    this.doc.setSuite(suite);
    this.suite = suite;
    this.series = new ArrayList<WrapSeries>();
    SeriesConfig[] scs = this.suite.getSeriesConfigs().getSeriesConfigArray();
    for(int i = 0; i < scs.length; i++) {
      this.series.add(new WrapSeries(scs[i]));
    }
  }

  /**
   * Adds another series to the suite.
   *
   * @return the new series
   */
  public WrapSeries addNewSeries() {
    SeriesConfig sc = this.suite.getSeriesConfigs().addNewSeriesConfig();
    sc.addNewSeries();
    sc.addNewSchedule().addNewCron();
    WrapSeries result = new WrapSeries(sc);
    result.setReporter("");
    result.setResource("");
    result.setCron("* * * * *");
    result.setAction(Protocol.SERIES_CONFIG_ADD);
    this.series.add(result);
    return result;
  }

  /**
   * Copies all information from another suite into this one.
   *
   * @param original the WrapSuite to duplicate
   */
  public void copy(WrapSuite original) {
    this.setDescription(original.getDescription());
    this.setName(original.getName());
    WrapSeries[] series = original.getAllSeries();
    for(int i = 0; i < series.length; i++) {
      WrapSeries newSeries = this.addNewSeries();
      newSeries.copy(series[i]);
    }
  }

  /**
   * Returns a new WrapSuite that contains the modifications necessary to
   * produce a specified WrapSuite from this one.
   *
   * @param ws the modified WrapSuite
   * @return a new WrapSuite that shows the differences needed to produce ws
   */
  public WrapSuite differences(WrapSuite ws) {
    WrapSuite result = new WrapSuite(ws.getName(), ws.getDescription());
    result.suite.setGuid(ws.suite.getGuid());
    WrapSeries[] thisSeries = this.getAllSeries();
    WrapSeries[] wsSeries = ws.getAllSeries();
    // Figure out which series to change/delete ...
    for(int i = 0; i < thisSeries.length; i++) {
      WrapSeries series = thisSeries[i];
      int j;
      for(j = 0;
          j < wsSeries.length &&
          (wsSeries[j] == null || !series.equals(wsSeries[j]));
          j++) {
        // empty
      }
      if(j == wsSeries.length) {
        WrapSeries copy = result.addNewSeries();
        copy.copy(series);
        copy.setAction(Protocol.SERIES_CONFIG_DELETE);
      } else {
        wsSeries[j] = null; // So we don't consider it added below
      }
    }
    // ... and which to add
    for(int i = 0; i < wsSeries.length; i++) {
      WrapSeries series = wsSeries[i];
      if(series != null) {
        WrapSeries copy = result.addNewSeries();
        copy.copy(series);
        copy.setAction(Protocol.SERIES_CONFIG_ADD);
      }
    }
    return result;
  }

  /**
   * Returns the set of series in the suite as an array, empty if none.
   *
   * @return the set of series, empty if none
   */
  public WrapSeries[] getAllSeries() {
    return
      this.series.toArray(new WrapSeries[this.series.size()]);
  }

  /**
   * Returns the description of the suite.
   *
   * @return the suite description
   */
  public String getDescription() {
    return this.suite.getDescription();
  }

  /**
   * Returns the name of the suite.
   *
   * @return the suite name
   */
  public String getName() {
    return this.suite.getName();
  }

  /**
   * Returns a specified SeriesConfig from the Suite.
   *
   * @param index the index into the SeriesConfig array of the desired item
   */
  public WrapSeries getSeriesAt(int index) {
    return this.series.get(index);
  }

  /**
   * Returns the number of series configs in the suite.
   *
   * @return the number of series configs
   */
  public int getSeriesCount() {
    return this.series.size();
  }

  /**
   * Returns the Suite wrapped by this WrappedSuite.
   *
   * @return the wrapped Suite
   */
  public Suite getSuite() {
    return this.suite;
  }

  /**
   * Removes a specified series from the suite.
   *
   * @param series the series to remove
   */
  public void removeSeries(WrapSeries series) {
    SeriesConfig sc = series.getSeries();
    SeriesConfig[] scs = this.suite.getSeriesConfigs().getSeriesConfigArray();
    for(int i = 0; i < scs.length; i++) {
      if(scs[i] == sc) {
        this.suite.getSeriesConfigs().removeSeriesConfig(i);
        this.series.remove(i);
        return;
      }
    }
  }

  /**
   * An override of the default toString function.
   */
  @Override
  public String toString() {
    return this.getName();
  }

  /**
   * Sets the suite description to a specified value.
   *
   * @param description the description of the suite
   */
  public void setDescription(String description) {
    if(description != null) {
      this.suite.setDescription(description);
    } else if(this.suite.isSetDescription()) {
      this.suite.unsetDescription();
    }
  }

  /**
   * Sets the suite name to a specified value.
   *
   * @param name the name of the suite
   */
  public void setName(String name) {
    this.suite.setName(name);
  }

  /**
   * Returns XML for the Suite.
   *
   * @return the suite, as an XML string
   */
  public String toXml() {
    return this.doc.xmlText((new XmlOptions()).setSavePrettyPrint());
  }

}
