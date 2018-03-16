package edu.sdsc.inca.util;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.log4j.Logger;


/**
 * @author Cathie Olschanowsky
 *
 * Abstract class to help implement worker threads in higher level classes.
 *
 *
 */
public class Worker extends Thread {

  private static final Logger logger = Logger.getLogger(Worker.class);
  private final WorkQueue<Worker> q;

  /**
   * Create a new Worker which is attached to the named queue.
   *
   * @param q the q to attach the worker to
   */
  public Worker(WorkQueue<Worker> q) {
    this.q = q;
  }

  /**
   * Start the worker.
   * In order to start the worker in its own thread use start().  Calling
   * run directly will most likely make your program hang.
   */
  public void run() {
    try {
      // If there is an interrupt during the work stage a flag is set
      // and this will make the thread terminate properly in that case.
      // properly means that all the work it has claimed it has finished
      while (!this.isInterrupted()) {
        // Retrieve some work; block if the queue is empty
        // the call to getWork blocks -- so there is no need to worry
        // about that stuff from implementing classes
        WorkItem<Worker> o = q.getWork();

        try {
          o.doWork(this);
        }
        catch (Exception err) {
          ByteArrayOutputStream logMessage = new ByteArrayOutputStream();
          PrintStream stream = new PrintStream(logMessage);

          stream.print(this.getName());
          stream.print(" caught an exception while doing work: ");
          err.printStackTrace(stream);

          logger.error(logMessage.toString());
        }
      }
    } catch (InterruptedException e) {
      logger.debug(this.getName()+" interrupted while waiting on queue");
    } finally {
      logger.debug(this.getName() + ": closing down");
    }
  }

  /**
   * Returns the name of the thread -- there really isn't any other state kept
   * in the class.
   *
   * @return a string representation of the object
   */
  public String toString() {
    return this.getName();
  }

}
