/*
 * ScheduledPurge.java
 */
package edu.sdsc.inca.depot;


import java.sql.SQLException;
import java.util.Calendar;

import org.apache.log4j.Logger;

import edu.sdsc.inca.Depot;


/**
 *
 * @author Paul Hoover
 *
 */
public class ScheduledPurge extends Thread {

  // data fields


  private static final Logger m_logger = Logger.getLogger(ScheduledPurge.class);
  private final int m_cutoff;
  private final int m_hour;
  private final int m_period;


  // constructors


  /**
   *
   * @param hour
   * @param period
   * @param cutoff
   */
  public ScheduledPurge(int hour, int period, int cutoff)
  {
    super("DB-Purge-Thread");

    m_hour = hour;
    m_period = period;
    m_cutoff = cutoff;
  }


  // public methods


  /**
   *
   */
  @Override
  public void run()
  {
    try {
      sleepUntilHour(m_hour, 0);

      while(!isInterrupted()) {
        if (!Depot.getRunningDepot().syncInProgress()) {
          Calendar cutoffDate = Calendar.getInstance();

          cutoffDate.add(Calendar.DAY_OF_YEAR, -m_cutoff);

          (new PurgeDatabase()).purge(cutoffDate.getTime());
        }
        else
          m_logger.info("skipping purge due to DB synchronization");

        sleepUntilHour(m_hour, m_period);
      }
    }
    catch (SQLException sqlErr) {
      m_logger.error(sqlErr.getMessage());

      SQLException nextErr;

      while ((nextErr = sqlErr.getNextException()) != null)
        m_logger.error(nextErr.getMessage());
    }
    catch (InterruptedException interruptErr) {
      m_logger.debug(getName() + " interrupted");
    }
    finally {
      m_logger.debug(getName() + " closing down");
    }
  }


  // private methods


  /**
   *
   * @param hour
   * @param days
   * @return
   * @throws InterruptedException
   */
  private void sleepUntilHour(int hour, int days) throws InterruptedException
  {
    Calendar now = Calendar.getInstance();
    Calendar runAt = Calendar.getInstance();

    runAt.add(Calendar.DAY_OF_YEAR, days);
    runAt.set(Calendar.HOUR_OF_DAY, hour);
    runAt.set(Calendar.MINUTE, 0);
    runAt.set(Calendar.SECOND, 0);
    runAt.set(Calendar.MILLISECOND, 0);

    if (runAt.before(now)) {
      int newDay = now.get(Calendar.DAY_OF_YEAR) + 1;

      runAt.set(Calendar.DAY_OF_YEAR, newDay);
    }

    long time = runAt.getTimeInMillis() - now.getTimeInMillis();

    sleep(time);
  }
}
