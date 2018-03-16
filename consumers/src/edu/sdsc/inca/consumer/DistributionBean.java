package edu.sdsc.inca.consumer;

import de.laures.cewolf.DatasetProduceException;
import de.laures.cewolf.DatasetProducer;
import de.laures.cewolf.tooltips.CategoryToolTipGenerator;
import edu.sdsc.inca.dataModel.queryResults.ObjectDocument;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.impl.values.XmlAnyTypeImpl;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.Vector;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;

/**
 * A dataset producer for graphing distribution data based on an xpath values
 * in a xml document.  This is implemented by extending the category dataset
 * using the xpath values for the xaxis and their count for the yaxis. Multiple
 * rows (or series) can be added to create a stacked bar graph.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class DistributionBean extends DefaultCategoryDataset
                          implements DatasetProducer, CategoryToolTipGenerator {

  public static final String NAMESPACE_DECLS = TimeSeriesBean.NAMESPACE_DECLS;
  private static Logger logger = Logger.getLogger(DistributionBean.class);

  private ObjectDocument doc = null;
  private Vector<String> colKeys = new Vector<String>();
  private Vector<String> rowKeys = new Vector<String>();
  private String id = null;

  /**
   * Create a new distribution dataset which will pull data from the provided
   * xml document
   *
   * @param id   A unique id that can be used for a graph id
   * @param xml  An string containing an xml document following the query
   *             results schema: http://inca.sdsc.edu/dataModel/queryResults_2.0
   *
   * @throws XmlException if problem parsing xml
   */
  public DistributionBean( String id, String xml ) throws XmlException {
    logger.info( "Creating distribution dataset bean " + id );
    this.id = id;
    this.doc = ObjectDocument.Factory.parse( xml );
  }

  /**
   * Add a new row to the dataset.  Will then run the xpath expression over
   * the xml and add values to the distribution.
   *
   * @param xpath  the values that will be calculated into the distribution
   *
   * @param rowKey  an identifier to use for the row (or series)
   *
   * @param statusAsFloat if true, assumes xpath is a path to a graph instance
   * object and that we want the status of it interpreted as a float 
   */
  public void add(String xpath, String rowKey, boolean statusAsFloat) {
    logger.debug( "Adding row key " + rowKey + " using xpath " + xpath );
    rowKeys.add( rowKey );
    XmlObject[] objects = doc.selectPath( NAMESPACE_DECLS + xpath );
    if ( objects == null ) return;
    logger.debug( "Found " + objects.length + " row key values" );
    HashMap<String,Integer> valueMap = new HashMap<String,Integer>();
    for( XmlObject object : objects ) {
      String value = statusAsFloat ?
        Float.toString(TimeSeriesBean.getResult(object)) :
        ((XmlAnyTypeImpl)object).getStringValue();
      if ( ! this.colKeys.contains(value) ) {
        logger.debug( "Adding col key " + value );
        this.colKeys.add( value );
        for( String existingRowKey : this.rowKeys ) {
          this.addValue
            ( (java.lang.Number)0, existingRowKey, this.colKeys.size()-1 );          
        }
      }
      if ( ! valueMap.containsKey(value) ) {
        valueMap.put( value, 0 );
      }
      valueMap.put( value, (valueMap.get(value)) + 1 );
    }
    for( int i = 0; i < colKeys.size(); i++ ) {
      int count = 0;
      if ( valueMap.containsKey(colKeys.get(i)) ) {
        count = valueMap.get(colKeys.get(i));
      }
      this.addValue( (java.lang.Number)count, rowKey, i );
    }
  }

  /**
   * Text to display when a bar is moused over
   *
   * @param data  Jfree CategoryDataset object to add mouseover text to.
   *
   * @param row  The row value that was moused over
   *
   * @param column  The column value that was moused over
   *
   * @return  Mouseover text string
   */
  public String generateToolTip( CategoryDataset data, int row, int column ) {
    DistributionBean categoryBean = (DistributionBean)data;
    return Util.formatStringAsTooltip( categoryBean.colKeys.get( column ) );
  }

  /**
   * Return the index of the column key value
   *
   * @param colValue  A value in the the column keys
   *
   * @return  An index in the column key vector
   */
  public int getColKeyIndex( String colValue ) {
    return this.colKeys.indexOf( colValue );
  }
  
  /**
   * Return the vector of column keys (or unique values found in the xpath)
   *
   * @return A vector of column keys.
   */
  public Vector<String> getColKeys() {
    return this.colKeys;
  }

  /**
   * Returns a unique id for this dataset
   */
  public String getProducerId() {
    return this.id;
  }

  /**
   * Return the index of the row key value
   *
   * @param rowValue  A value in the the row keys
   *
   * @return  An index in the row key vector
   */
  public int getRowKeyIndex( String rowValue ) {
    return this.rowKeys.indexOf( rowValue );
  }

  /**
   * Return the vector of row keys (or series)
   *
   * @return  A vector of row keys
   */
  public Vector<String> getRowKeys() {
     return this.rowKeys;
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
   * Return this object as the dataset
   *
   * @param params  Additional params for the dataset production.
   *
   * @return  A CategoryDataset object containing a distribution dataset
   *
   *
   * @throws de.laures.cewolf.DatasetProduceException
   */
  public Object produceDataset(Map params) throws DatasetProduceException {
    return this;
  }
}