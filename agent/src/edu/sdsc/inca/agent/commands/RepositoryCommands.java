package edu.sdsc.inca.agent.commands;


import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;

import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.StandardMessageHandler;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.repository.Repository;
import edu.sdsc.inca.Agent;


public class RepositoryCommands extends StandardMessageHandler {

  Logger logger = Logger.getLogger(this.getClass().toString());

  /**
   * Execute the get/putrepositories command.
   */
  public void execute(ProtocolReader reader,
                      ProtocolWriter writer,
                      String dn) {

    Thread.currentThread().setName( "agent" );    
    try {

      Statement reply = null;
      Statement request = null;
      boolean updateCachedPackages = false;

      try {
        request = reader.readStatement();
      } catch(ProtocolException e) {
        logger.error("Protocol exeception " + e + " on command read");
        return;
      }
      String command = new String(request.getCmd());

      if(command.equals(Protocol.CATALOG_GET_COMMAND)) {
        Agent agent = Agent.getGlobalAgent();
        StringBuffer catalog = new StringBuffer();
        Properties[] reporters = new Properties[0];
        String url = new String(request.getData());
        if(url.equals("")) {
          // Force test for reporter updates
          agent.setRepositories(agent.getRepositories().getRepositories());
          updateCachedPackages = true;
          reporters = agent.getRepositories().getCatalog();
        } else {
          try {
            reporters = new Repository(new URL(url)).getCatalog();
          } catch(Exception e) {
            logger.error("Unable to read repository " + url);
            reply = Statement.getErrorStatement
              ("Unable to read repository " + url);
          }
        }
        if(reply == null) {
          for(int i = 0; i < reporters.length; i++) {
              Enumeration<?> e = reporters[i].propertyNames();
              while(e.hasMoreElements()) {
                String key = (String)e.nextElement();
                catalog.append(key).append(":");
                catalog.append(reporters[i].get(key)).append("\n");
              }
              catalog.append("\n");
          }
          reply = Statement.getOkStatement(catalog.toString());
        }
      } else {
        logger.error("Received unrecognized message");
        reply = Statement.getErrorStatement("Unknown command " + command);
      }

      writer.write(reply);

      if( updateCachedPackages ) {
        Agent.getGlobalAgent().notifyAdminsofChanges
          ( Agent.getGlobalAgent().updateCachedPackages() );
      }

    } catch(IOException e) {
      logger.error("Reply failed", e);
    }

  }

}
