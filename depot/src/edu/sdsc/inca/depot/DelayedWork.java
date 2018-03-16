/*
 * DelayedWork.java
 */
package edu.sdsc.inca.depot;


import java.io.IOException;

import edu.sdsc.inca.util.WorkItem;
import edu.sdsc.inca.util.Worker;


/**
 *
 * @author Paul Hoover
 *
 */
public interface DelayedWork extends WorkItem<Worker> {

  /**
   *
   * @param state
   * @throws Exception
   */
  void loadState(String state) throws Exception;

  /**
   *
   * @return
   * @throws IOException
   */
  String getState() throws IOException;
}
