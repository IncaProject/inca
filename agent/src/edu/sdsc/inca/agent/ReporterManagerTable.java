package edu.sdsc.inca.agent;

import org.apache.log4j.Logger;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Calendar;

import edu.sdsc.inca.util.Constants;
import edu.sdsc.inca.Agent;

/**
 * Convenience class for storing and tracking reporter managers.  This wraps a
 * Hashtable and provides functions specifically for working with reporter
 * manager objects (e.g., you don't have to cast an object after getting it).
 * It also is a thread that checks the viability of reporter managers.  In the
 * case that a reporter manager starter thread gets stuck, this thread will
 * force a restart.  This class is thread-safe.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class ReporterManagerTable  extends Thread {
  // static variables
  public static long MAX_START_TIME = Constants.MILLIS_TO_HOUR;
  private static Logger logger = Logger.getLogger( ReporterManagerTable.class );

  // member variables
  private int checkPeriod = Agent.PING_PERIOD;
  private Hashtable rms = new Hashtable();

  /**
   * Returns true if the specified resource exists in the table. Note, that
   * the reporter manager may still be in the start up process.  
   *
   * @param resource  A resource name from the resource configuration file.
   * @return True if the resource exists and false otherwise.
   */
  public boolean containsResource( String resource ) {
    return rms.containsKey( resource );
  }

  /**
   * Returns the reporter manager if it exists in the table regardless if
   * it's running or not.
   *
   * @param resource
   * @return A reporter manager object.
   */
  public ReporterManagerController get( String resource ) {
    return (ReporterManagerController)rms.get( resource );
  }

  /**
   * Get the frequency of which this thread should check the health of the
   * reporter managers.
   *
   * @return  The period in milliseconds.
   */
  public int getCheckPeriod() {
    return checkPeriod;
  }

  /**
   * Return the a list of resources who have reporter managers stored in the
   * table.
   *
   * @return   A list of resource names.
   */
  public String[] getResourceNames() {
    Iterator iterator = rms.keySet().iterator();
    String[] names = new String[rms.size()];
    for ( int i = 0; i < rms.size(); i++ ) {
      names[i] = (String)iterator.next();
    }
    return names;
  }

  /**
   * Add a reporter manager to the table.
   *
   * @param resource  A string for retrieving the reporter manager from the
   * table.
   * @param rm A reporter manager to store in the table.
   */
  public void put( String resource, ReporterManagerController rm ) {
    rms.put( resource, rm );
  }

  /**
   * Removes the specified reporter manager from the table.
   *
   * @param resource  The name of the resource whose reporter manager
   * should be removed from the table.
   */
  public void remove( String resource ) {
    rms.remove( resource );
  }

  /**
   * Check all reporter manager periodically to see if they are still running.
   * If the rm.isRunning flag is false, we know there is no remote reporter
   * manager running.  In that case, either
   *
   * 1) the reporter manager's starter thread is running and we just need to
   * give it time to do start up a remote reporter manager,
   *
   * 2) the reporter manager's starter thread is running but is blocked on
   * a call and needs to be timed out, or
   *
   * 3) the reporter manager starter thread is not running in which case we
   * can start up a new one.
   *
   * We are ignoring (2) until we have more experience with the nature of
   * reporter managers not getting restarted automatically.
   */
  public void run() {
    try {
      logger.info(
        "Will check reporter managers every " +
        this.checkPeriod / Constants.MILLIS_TO_SECOND + " seconds"
      );
      while ( ! isInterrupted() ) {
        Thread.sleep( this.checkPeriod );
        logger.info( "Checking reporter managers" );
        String[] resources = this.getResourceNames();
        for( int i = 0; i < resources.length; i++ ) {
          ReporterManagerController rm = this.get( resources[i] );
          if( ! rm.isRunning() ) {
            logger.warn
              ( "Reporter manager " + rm.getResource() + " appears to be down");
            if ( ! rm.getReporterManagerStarter().isAlive() ) {
              logger.info( "No active starter thread" );
              rm.restart();
            } else {
              if ( rm.getReporterManagerStarter().getStartAttemptTime() > 0 ) {
                logger.info( rm.getResource() + " starter thread in progress" );
                long elapsedTime = Calendar.getInstance().getTimeInMillis() -
                           rm.getReporterManagerStarter().getStartAttemptTime();
                if ( elapsedTime > MAX_START_TIME ) {
                  logger.warn
                    ("Timing out " + rm.getResource() + " starter thread");                  
                  rm.restart();
               } else {
                  logger.debug( rm.getResource() + " starter thread ok" );
                }
              } else {
                logger.debug( rm.getResource() + " starter thread waiting" );
              }
            }
          } else {
            logger.info(  "Reporter manager " + rm.getResource() + " is up" );
          }
        }
        logger.info( "Done checking reporter managers" );
      }
    } catch (InterruptedException e) {
      logger.debug( "ReporterManagerTable interrupted while sleeping");
    } finally {
      logger.info( "ReporterManagerTable stopping checks");
    }
  }

  /**
   * Set the frequency of which the thread should check the health of the
   * reporter managers.
   *
   * @param checkPeriod  Period in milliseconds.
   */
  public void setCheckPeriod(int checkPeriod) {
    this.checkPeriod = checkPeriod;
  }
}
