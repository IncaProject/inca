package edu.sdsc.inca.agent.access;

import edu.sdsc.inca.AgentTest;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.util.ResourcesWrapperTest;
import edu.sdsc.inca.agent.AccessMethod;

/**
 * Test ssh access method
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class SshTest extends AccessMethodTestCase {

  public void setUp() throws Exception {
    procs = new AccessMethod[1];
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    procs[0] = new PublicKeySsh( AgentTest.TEST_RESOURCE, resources );
  }

  public boolean hasRequirements() {
    return AgentTest.hasSshServer();
  }
}
