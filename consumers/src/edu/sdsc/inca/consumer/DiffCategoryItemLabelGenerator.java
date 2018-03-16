package edu.sdsc.inca.consumer;

import org.apache.log4j.Logger;
import org.jfree.data.category.CategoryDataset;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;

/**
 * Display the difference in category row values as an item label.  Assumes
 * values are percentages.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class DiffCategoryItemLabelGenerator
  extends StandardCategoryItemLabelGenerator {

  private static Logger logger = Logger.getLogger
    (DiffCategoryItemLabelGenerator.class);

  /**
   * Overrides super class method to display the difference in row values
   * for a column as item label text.
   *
   * @param dataset A dataset of values
   * @param row The row index for the value
   * @param col The column index for the value
   *
   * @return A string containing the difference in values in the row.
   */
  public String generateLabel(CategoryDataset dataset, int row, int col) {
    if ( row < dataset.getRowCount()-1 ) { // not last row
      String text = " " + dataset.getValue(row,col) + "%";
      if ( dataset.getValue(row+1,col) != null ) {
        Double diff = (Double)dataset.getValue(row,col) -
          (Double)dataset.getValue(row+1,col);
        if ( diff > 0 ) {
          text += " (+" + diff.intValue() + "%)";
        } else if ( diff < 0 ) {
          text += " (" + diff.intValue() + "%)";
        }
      }
      return text;
    } else {
      return "";
    }
  }
}
