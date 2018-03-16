package edu.sdsc.inca.consumer;

import org.apache.log4j.Logger;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;

import java.awt.*;

/**
 * A bar item label renderer class that changes the color of the label based
 * the difference in values between bars.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class BarItemLabelColorRenderer extends BarRenderer {
  private static Logger logger = Logger.getLogger(BarItemLabelColorRenderer.class);
  private Color posChange;
  private Color negChange;
  private Color noChange;

  public BarItemLabelColorRenderer
    ( BarRenderer b, Color posChange, Color negChange, Color noChange ) {
    super();

    // colors
    this.posChange = posChange;
    this.negChange = negChange;
    this.noChange = noChange;

    // inherit attributes
    this.setBase(b.getBase());
    this.setIncludeBaseInRange(b.getIncludeBaseInRange());
    this.setItemMargin(b.getItemMargin());
    this.setDrawBarOutline(b.isDrawBarOutline());
    this.setMaximumBarWidth(b.getMaximumBarWidth());
    this.setBasePositiveItemLabelPosition(b.getBasePositiveItemLabelPosition());
    this.setBaseNegativeItemLabelPosition(b.getBaseNegativeItemLabelPosition());
    this.setGradientPaintTransformer(b.getGradientPaintTransformer());
    this.setMinimumBarLength(b.getMinimumBarLength());
  }

  /**
   * Overrides super classes function to return a per item label text color
   *
   * @param row  The row index of the item
   * @param col  The col index of the item
   *
   * @return  A color of text to use when printing the item label
   */
  public Paint getItemLabelPaint(int row, int col) {
    CategoryDataset dataset = this.getPlot().getDataset();
    if (row < dataset.getRowCount()-1 && dataset.getValue(row+1,col) != null) {
      Double diff = (Double)dataset.getValue(row,col) -
                    (Double)dataset.getValue(row+1,col);
      if ( diff > 0 ) {
        return posChange;
      } else if ( diff < 0 ) {
        return negChange;
      } 
    }
    return noChange;
  }

}