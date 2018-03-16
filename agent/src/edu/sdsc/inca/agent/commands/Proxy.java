package edu.sdsc.inca.agent.commands;


import java.io.IOException;

import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.protocol.StandardMessageHandler;
import edu.sdsc.inca.Agent;
import edu.sdsc.inca.agent.ReporterManagerController;


/**
 * Handles the PROXY command.  This command is sent by a remote reporter
 * manager to indicate that it needs the MyProxy password to fetch a new
 * proxy credential, it will then fetch the proxy credential, and
 * wipe the password from memory.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class Proxy extends StandardMessageHandler {
  public void execute(ProtocolReader reader,
                      ProtocolWriter writer,
                      String dn) throws ProtocolException {
    Statement statement = null;
    String resource = "";
    try {
      statement = reader.readStatement();
      resource = new String( statement.getData() );
      if ( resource == null ) resource = "";
      Thread.currentThread().setName( resource );
      logger.info("Reporter Manager '" + resource + "' requesting proxy info");
    } catch ( IOException e ) {
      logger.error
        ( "Received proxy information request but unable to read data" );
      return;
    }
    try {
      ReporterManagerController rmc =
        Agent.getGlobalAgent().getReporterManager( resource) ;
      if ( rmc == null ) {
        writer.write( Statement.getErrorStatement(
          "Unable to locate reporter manager controller for " + resource)
        );
      } else if ( ! rmc.hasProxy() ) {
        writer.write( Statement.getErrorStatement(
          "No proxy information registered for resource " + resource)
        );
      } else {
        rmc.getProxy().send( writer, reader );
      }
    } catch ( Exception e ) {
      logger.error(
        "Failed to send proxy information to reporter manager '" +
        resource + "'", e
      );
    }

  }
}
