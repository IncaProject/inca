package edu.sdsc.inca.agent.access;

import edu.sdsc.inca.AgentTest;
import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.util.ResourcesWrapperTest;
import edu.sdsc.inca.agent.AccessMethod;

/**
 * Test Manual access method
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class ManualTest extends AccessMethodTestCase {
  String resource = null;

 public void setUp() throws Exception {
    resource = AgentTest.TEST_RESOURCE + "-manual";
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    if ( resources.getResources(resource,true).length < 1 ) {
      throw new ConfigurationException(
        "Resource '" + resource + "' not in resource file '" +
        AgentTest.RESOURCES_FILE + "'"
      );
    }
    checkResult = false;
    procs = new AccessMethod[1];
    procs[0] = new Manual( resource, resources );
  }
}
