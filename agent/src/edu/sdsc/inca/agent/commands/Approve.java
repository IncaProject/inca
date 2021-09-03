package edu.sdsc.inca.agent.commands;


import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import edu.sdsc.inca.Agent;
import edu.sdsc.inca.agent.ReporterManagerController;
import edu.sdsc.inca.dataModel.inca.IncaDocument;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.StandardMessageHandler;
import edu.sdsc.inca.protocol.Statement;


/**
 * Handles the approve commands.  A resource can choose to require approval
 * for all changes.  When a change comes in, the agent stores the changes as
 * proposed changes and sends email to the resource admin.  The admin runs
 * an approve script from the resource and asks for proposed changes.  When
 * the admin approves changes, the script sends the APPROVE followed by the
 * resource name and list of approved changes.  The Agent will subsequently
 * send the changes to the manager.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class Approve extends StandardMessageHandler {

  @Override
  public void execute(ProtocolReader reader,
                      ProtocolWriter writer,
                      String dn) throws Exception {

    IncaDocument doc = null;
    Statement request = null;
    try {
      request = reader.readStatement();
    } catch(Exception e) {
      throw new ProtocolException( "Unable to read statement", e );
    }

    String command = new String(request.getCmd());
    // get proposed changes
    if( command.equals(Protocol.PROPOSED_GET_COMMAND) ) {
      String resource = new String(request.getData());
      ReporterManagerController rmc =
        Agent.getGlobalAgent().getReporterManager(resource );
      writer.write( new Statement("OK " + rmc.getProposedChanges().xmlText()) );
    // approve proposed changes
    } else if( command.equals(Protocol.APPROVE_COMMAND) ) {
      String data = new String(request.getData());
      int delimiterIndex = data.indexOf( " " );
      String resource = data.substring(0, delimiterIndex );
      String approveFor = Protocol.APPROVE_ACTION + " " + resource;
      if( !MessageHandler.isPermitted(dn, approveFor)) {
        throw new ProtocolException(approveFor + " not allowed by " + dn);
      }
      String xml = data.substring( delimiterIndex+1 );
      try {
        doc = IncaDocument.Factory.parse(xml, (new XmlOptions()).setLoadStripWhitespace());
        if(!doc.validate()) {
          throw new XmlException("Invalid Inca config XML '" + xml + "'");
        }
      } catch(XmlException e) {
        throw new ProtocolException("Unable to parse Inca config XML: " + e);
      }

      ReporterManagerController rmc =
        Agent.getGlobalAgent().getReporterManager(resource );
      rmc.approveSuites( doc );
      writer.write( new Statement("OK") );
    }
  }

}
