package edu.sdsc.inca.consumer;

import de.laures.cewolf.DatasetProduceException;
import de.laures.cewolf.DatasetProducer;
import de.laures.cewolf.links.XYItemLinkGenerator;
import de.laures.cewolf.tooltips.XYToolTipGenerator;
import edu.sdsc.inca.dataModel.queryResults.ObjectDocument;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCalendar;
import org.apache.xmlbeans.impl.values.XmlObjectBase;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import java.util.Date;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * A dataset producer for graphing time series data based on an xpath values
 * in a xml document.  This is implemented by extending the time series dataset
 * to pull values from xpaths.  There is a main xpath expression used to pick
 * out filter the document.  Relative xpath expressions are used for the
 * xaxis value, yaxis value, tooltip, and url.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */

public class TimeSeriesBean extends TimeSeriesCollection
                            implements DatasetProducer, XYToolTipGenerator,
                                       XYItemLinkGenerator  {

  final static public String NAMESPACE_DECLS =
    "declare namespace q='http://inca.sdsc.edu/dataModel/queryResults_2.0';";


  private static Logger logger = Logger.getLogger(TimeSeriesBean.class);
  private float failedValue = -1;
  private String id = null;
  private boolean hasFailedValue = false;
  private Vector<Vector<String>> links = new Vector<Vector<String>>();
  private Vector<Vector<String>> tooltips = new Vector<Vector<String>>();
  static String ignorePattern = null;


  /**
   * Create a new time series collection with the specified id
   *
   * @param id a unique identifier for identifying this time series collection
   */
  public TimeSeriesBean( String id ) {
    this.id = id;
  }

  /**
   * Add a new series to the time series collection.
   *
   * @param xml  An xml document containing values to be added to the time
   *             series
   * @param xpath  A path to a specific object within the xml document which
   *               contains time series values.
   * @param relTimestampXpath The relative path of the timestamp
   * @param relValueXpath  The relative path of the actual value
   * @param relLinkXpath   The relative path of the value to use for the
   *                       url link
   * @param relTooltipXpath The relative path of the value to use for the
   *                        mouseover tooltip
   * @param label  The name to use in the legend for the time series.
   *
   * @throws DatasetProduceException if unable to parse XML
   */
  public void addSeries( String xml, String xpath, String relTimestampXpath,
                         String relValueXpath, String relLinkXpath,
                         String relTooltipXpath, String label )
    throws DatasetProduceException {

       // process data for each test and resource
    ObjectDocument doc;
    logger.debug( "Generating data for time series " + this.id );
    try {
      doc = ObjectDocument.Factory.parse( xml );
    } catch (XmlException e) {
      throw new DatasetProduceException( "Unable to parse xml: " + e );
    }
    XmlObject[] tsObjects = doc.selectPath( NAMESPACE_DECLS + xpath );

    logger.debug("Processing " + tsObjects.length + " objects for " + this.id);
    TimeSeries series = new TimeSeries( label, Minute.class );
    long startTime = Util.getTimeNow();
    Vector<String> seriesLinks = new Vector<String>();
    Vector<String> seriesTooltips = new Vector<String>();
    for ( XmlObject tsObject : tsObjects ) {
      // did user specify a value xpath; if not assume exit status
      float value;
      if ( relValueXpath != null ) {
        // is there a value? if not use the value the user selected for failed
        // reports.  Otherwise try to parse the value.
        XmlObject[] valueObjects = tsObject.selectPath(relValueXpath);
        if ( valueObjects == null || valueObjects.length < 1 ) {
          if ( hasFailedValue ) {
            value = this.failedValue;
          } else {
            continue;
          }
        } else {
          XmlObjectBase valueObject = (XmlObjectBase)valueObjects[0];
          try {
            value = new Float(valueObject.getStringValue());
          } catch (NumberFormatException e) {
            logger.warn( "Unable to convert value to float: " + valueObject.getStringValue() );
            continue;
          }
        }
      } else {
        value = getResult(tsObject);
      }

      // Get timestamp and value
      XmlObjectBase timestampObject =
        (XmlObjectBase)tsObject.selectPath(relTimestampXpath)[0];

      // Get tooltip if a tooltip xpath exists
      XmlObjectBase tooltipObject = null;
      XmlObject[] tObjects = tsObject.selectPath(relTooltipXpath);
      if ( tObjects != null && tObjects.length == 1 ) {
        tooltipObject = (XmlObjectBase)tObjects[0];
      }

      // Get link if a link xpath exists
      XmlObjectBase lValue = null;
      XmlObject[] lObjects = tsObject.selectPath(relLinkXpath);
      if ( lObjects != null || lObjects.length == 1 ) {
        lValue = (XmlObjectBase)lObjects[0];
      }     

      try {
        Date date = new XmlCalendar( timestampObject.getStringValue() ).getTime();
        series.add( new Minute(date), value );
        String tooltip = "";
        if ( tooltipObject != null ) {
          tooltip = Util.formatStringAsTooltip(tooltipObject.getStringValue()) +
                    " @ ";
        }
        tooltip = tooltip + "(" + date.toString() + ") ";
        seriesTooltips.add( tooltip );
        if ( lValue != null ) seriesLinks.add( lValue.getStringValue() );
        logger.debug( "Added value " + value  + " at " + date + " with tooltip " + tooltip + (lValue == null ? "" : " and link " + lValue) );
      } catch (Exception e) {
        logger.warn( "Unable to add instance " + e );
      }
    }
    links.add( seriesLinks );
    tooltips.add( seriesTooltips );
    this.addSeries(series);
    Util.printElapsedTime( startTime, "time series " + this.id );
  }

   /**
   * Return the link to use when the cursor is hovered over a data point
   *
   * @param data  This object.
   *
   * @param series  The series index containing the data point
   *
   * @param item  The index of the data point in the series
  *
   * @return  A string containing a url
   */
  public String generateLink( Object data, int series, int item ) {
    return ((TimeSeriesBean)data).generateLink(series, item);
  }

  /**
   * Return the link to use when the given date point index is moused over
   *
   * @param index  The index of the data point that was moused over
   *
   * @param series  The series index containing the data point
   *
   * @return  A url for the given datapoint
   */
  public String generateLink( int series, int index ) {
    return this.links.get(series).get(index);
  }

  /**
   * Return the text to display when the cursor is hovered a data point
    *
    * @param data  This object.
    *
    * @param series  The series index containing the data point
    *
    * @param item  The index of the data point in the series
    *
    * @return  A string containing a url
    */
  public String generateToolTip( XYDataset data, int series, int item ) {
    return ((TimeSeriesBean)data).generateToolTip(series, item);
  }

  /**
   * Return the link to use when the given date point index is moused over
   *
   * @param series  The series index containing the data point
   *
   * @param index  The index of the data point that was moused over
   *
   * @return  A url for the given datapoint
   */

  public String generateToolTip( int series, int index ) {
    return tooltips.get(series).get(index);
  }

  /**
   * Returns a unique ID for this DatasetProducer
   */
  public String getProducerId() {
    return this.id;
  }

  /**
    * Determine pass/fail/unknown status for a graph instance based on error
    * message and comparison result.
    *
    * @param gi An XML parse graph instance node
    *
    * @return  1 if completed is true or the comparison result is Success.
    * -1 if comparison is Failure or completed is false.  Neutral (0) if
    * err message indicates downtime or Inca error
    */
   public static float getResult(XmlObject gi) {
     float value;
     XmlObject[] exitValueObj = gi.selectPath( "exit_status" );
     String exit_status = ((XmlObjectBase)exitValueObj[0]).getStringValue();
     exitValueObj = gi.selectPath( "exit_message" );
     String exit_message = ((XmlObjectBase)exitValueObj[0]).getStringValue();
     exitValueObj = gi.selectPath( "comparisonResult" );
     String cr = ((XmlObjectBase)exitValueObj[0]).getStringValue();
     float result;
     if (ignorePattern != null &&
         Pattern.compile(ignorePattern, Pattern.MULTILINE).matcher(exit_message).find()) {
       result = 0;
     }else if (cr != null && Pattern.matches("^Success.*$", cr)){
       result = 1;
     }else if (cr != null && Pattern.matches("^Failure.*$", cr)){
       result = -1;
     }else{
       result = exit_status.equals("Success") ? 1 : -1;
     }
     value = result;
     return value;
   }

  /**
   * This method influences Cewolf's caching behaviour.
   *
   * Example of invalid data after a day (86,400 seconds):
   *   log.debug(getClass().getName() + "hasExpired()");
   *   return (System.currentTimeMillis() - since.getTime()) > 86400000;
   */
  public boolean hasExpired(Map params, Date since) {
    return true;
  }

  /**
   * Return a time series collection using data supplied from the
   * provided xpaths
   *
   * @param params  Additional params for the dataset production.
  *
   * @return A  TimeSeriesCollection object with data and timeestamps.
   *
   * @throws DatasetProduceException if trouble generating data
   */
  public Object produceDataset(Map params) throws DatasetProduceException {
    return this;
  }

  /**
   * Set the value to use when a report has failed
   *
   * @param failedValue  A value to use for a report that has failed.  By
   * default, the failed reports are excluded
   */
  public void setFailedValue( float failedValue ) {
    this.hasFailedValue = true;
    this.failedValue = failedValue;
  }

  /**
   * Set a regular expression pattern to mark neutrally if an error matches it
   *
   * @param ignoreErrors  The pattern to mark neutrally in error msgs
   */
  public static void setIgnorePattern(String ignoreErrors) {
    ignorePattern = ignoreErrors;
  }
}
