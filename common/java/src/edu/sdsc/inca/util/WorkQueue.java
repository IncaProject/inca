package edu.sdsc.inca.util;


import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;


/**
 * @author Cathie Olschanowsky
 *
 * Generic work queue class that allows any type of object to be added and removed.
 * This class is thread safe.
 */
public class WorkQueue<T> {

    private static Logger logger = Logger.getLogger(WorkQueue.class);
    private final Queue<WorkItem<T>> queue = new LinkedList<WorkItem<T>>();
    private final int maxSize;


    /**
     *
     */
    public WorkQueue() {
      this(0);
    }

    /**
     *
     * @param max
     */
    public WorkQueue(int max) {
      maxSize = max;
    }

    /**
    * Clear the queue of all work
    */
    public synchronized void clear() {
      while( ! this.isEmpty() ) {
        try {
          this.getWork();
        } catch (InterruptedException e) {
          logger.info( "Interrupted while clearing queue" );
          break;
        }
      }
    }

    /**
     * Add a task to the workQueue.
     *
     * @param o Object that is the work to be added ( should be a socket )
     */
    public synchronized boolean addWork(WorkItem<T> o) {
        logger.debug("Adding Object to Queue");
        if (o == null) {
            throw new NullPointerException
                    ("attempting to push null onto work queue");
        }

        if (maxSize > 0 && queue.size() > maxSize) {
          logger.debug("Maximum queue size exceeded");

          return false;
        }

        queue.add(o);
        notify();

        return true;
    }

    /**
     * retrieve work from the workQueue.
     * blocks if empty.
     *
     * @return An Object that represents the next unit of work.
     * @throws InterruptedException
     */
    public synchronized WorkItem<T> getWork() throws InterruptedException {
        logger.debug("Attempting to retrieve work");
        while (queue.isEmpty()) {
            wait();
        }
        logger.debug("retrieving work");
        return queue.remove();
    }

    /**
     * check to see if there is any work to be done.
     *
     * @return true if there is work available.
     */
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}
