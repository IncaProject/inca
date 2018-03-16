package edu.sdsc.inca.agent.commands;


import java.io.IOException;

import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.StandardMessageHandler;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.Agent;


/**
 * Handles the REGISTER command.  This command is sent by a remote reporter
 * manager to indicate that it's ready to accept commands from the agent.
 * This task thread will persist and keep the socket open to the reporter
 * manager.  See the ReporterManagerController's register function for more information.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class Register extends StandardMessageHandler {
  public void execute(ProtocolReader reader,
                      ProtocolWriter writer,
                      String dn) throws ProtocolException {
    Statement statement = null;
    try {
      statement = reader.readStatement();

      // data should be of the form:  [NEW|EXISTING] RESOURCE_ID
      String data = new String( statement.getData() );
      String[] msgParts = data.split( " " );
      if ( ! msgParts[0].equals("NEW") && ! msgParts[0].equals("EXISTING") ){
        throw new ProtocolException
          ( "Expecting NEW or EXISTING register, received " + data );
      }
      boolean type = msgParts[0].equals( "NEW" );
      String resource = msgParts[1];
      Thread.currentThread().setName( "agent" );      
      logger.info("Reporter manager '" + resource + "' attempting to register");
      Agent.getGlobalAgent().registerReporterManager
        (resource, reader, writer, dn, type);
    } catch (IOException e) {
      logger.error("Reporter Manager registering failed", e);
    }
  }
}
