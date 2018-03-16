package edu.sdsc.inca.consumer;

import de.laures.cewolf.DatasetProduceException;
import de.laures.cewolf.DatasetProducer;
import de.laures.cewolf.links.CategoryItemLinkGenerator;
import de.laures.cewolf.tooltips.CategoryToolTipGenerator;
import org.apache.log4j.Logger;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.Map;
import java.util.Date;
import java.util.HashMap;

/**
 * Extension of DefaultCategoryDataset class that can be used in Cewolf and
 * supports mouseovers and links.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class CategoryBean extends DefaultCategoryDataset
                          implements DatasetProducer, CategoryToolTipGenerator,
                                     CategoryItemLinkGenerator {

  public static final String NAMESPACE_DECLS = TimeSeriesBean.NAMESPACE_DECLS;
  private static Logger logger = Logger.getLogger(DistributionBean.class);

  private String id = null;
  HashMap<String,String> links = new HashMap<String,String>();
  HashMap<String,String> tooltips = new HashMap<String,String>();

  /**
   * Create a new category dataset
   */
  public CategoryBean( )  {
    logger.info( "Creating category dataset bean" );
  }

  /**
   * Create a new category dataset with a specific id
   *
   * @param id a string containing an identifier for this dataset
   */
  public CategoryBean( String id )  {
    this.id = id;
    logger.info( "Creating category dataset bean " + id );
  }

  /**
   * Add a link to a dataset value.  A user can click on that link when the
   * value is clicked on in the graph.
   *
   * @param link  A url
   * @param row   The row index of the value
   * @param column The column index of the value
   */
  public void addLink( String link, int row, int column ) {
    String key = row + "," + column;
    links.put(key, link);
  }

  /**
   * Add a tooltip for dataset value.  The tooltip text will display when
   * the value is moused over in the graph.
   *
   * @param tooltip  A string to display when a value is moused over
   * @param row   The row index of the value
   * @param column The column index of the value
   */
  public void addTooltip( String tooltip, int row, int column ) {
    String key = row + "," + column;
    tooltips.put(key, tooltip);
  }

  /**
   * Called by CeWolf when generating graph to get the link for a specific
   * value in the dataset.
   *
   * @param dataset This object.
   * @param row   The row index of the value
   * @param columnKey The name of the column
   * @return A string containing a url
   */
  public String generateLink( Object dataset, int row, Object columnKey) {
    CategoryDataset cat = (CategoryDataset)dataset;
    int column = cat.getColumnIndex((Comparable)columnKey);
    String key = row + "," + column;
    if ( links.containsKey(key) ) {
      return links.get(key) ;
    } else {
      return "";
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
    String key = row + "," + column;
    if ( tooltips.containsKey(key) ) {
      return tooltips.get(key);
    } else {
      return "";
    }
  }


  /**
   * Returns a unique id for this dataset
   */
  public String getProducerId() {
    return this.id;
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

  /**
   * Set the id for this dataset (for CeWolf)
   *
   * @param id   A strin containing an id
   */
  public void setProducerId( String id ) {
    this.id = id;
  }
}