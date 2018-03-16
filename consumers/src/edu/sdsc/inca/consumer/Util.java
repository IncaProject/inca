package edu.sdsc.inca.consumer;

import org.apache.log4j.Logger;

import java.util.Calendar;

import edu.sdsc.inca.util.XmlWrapper;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class Util {
  public static final int TOOLTIP_MAX_STRING_LENGTH = 120;

  private static Logger logger = Logger.getLogger(Util.class);

  /**
   * Format text for a tooltip (jfreechart doesn't like ' or long  strings.
   * We also take out \n to make it easier to see.
   *
   * @param tooltipText  A string to use for a tooltip text.
   *
   * @return  A formatted tooltip string that can be used with jfreechart.
   */
  public static String formatStringAsTooltip( String tooltipText ) {
    String newTooltipText = XmlWrapper.escape
      ( tooltipText.replaceAll("\n","").replaceAll("'", "") );
    if ( newTooltipText.length() > TOOLTIP_MAX_STRING_LENGTH ) {
      newTooltipText = newTooltipText.substring(0, TOOLTIP_MAX_STRING_LENGTH);
    }
    return newTooltipText;
  }

  /**
   * Small convenience function for printing out timing information.
   *
   * @param startTime  The time at which to compute the elapsed time from now.
   *
   * @return the elapsed time 
   */
  public static long getElapsedTime( long startTime ) {
    return getTimeNow() - startTime;
  }

  /**
   * Small convenience function to return the current time in milliseconds.
   *
   * @return the current time in milliseconds
   */
  public static long getTimeNow() {
    return Calendar.getInstance().getTimeInMillis();
  }

  /**
   * Small convenience function for printing out timing information.
   *
   * @param startTime  The time at which to compute the elapsed time from now.
   *
   * @param id  A small identifier string that can be used in the debug
   * statement.
   */
  public static void printElapsedTime( long startTime, String id ) {
    float elapsed = getElapsedTime( startTime );
    logger.info( id + " time = " + (elapsed / 1000) );
  }

}
