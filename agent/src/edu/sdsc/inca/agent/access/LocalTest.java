package edu.sdsc.inca.agent.access;

import edu.sdsc.inca.AgentTest;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.util.ResourcesWrapperTest;
import edu.sdsc.inca.agent.AccessMethod;

/**
 * Test Local access method
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class LocalTest extends AccessMethodTestCase {
  private Local proc = null;

  public void setUp() throws Exception {
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    procs = new AccessMethod[1];
    procs[0] = new Local( AgentTest.TEST_RESOURCE, resources );

  }
}
