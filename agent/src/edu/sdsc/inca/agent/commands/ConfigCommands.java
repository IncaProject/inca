package edu.sdsc.inca.agent.commands;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.log4j.Logger;

import edu.sdsc.inca.Agent;
import edu.sdsc.inca.dataModel.inca.IncaDocument;
import edu.sdsc.inca.dataModel.resourceConfig.ResourceConfigDocument;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.repository.Repository;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.util.SuiteWrapper;
import edu.sdsc.inca.util.XmlWrapper;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.StandardMessageHandler;
import edu.sdsc.inca.protocol.Statement;


public class ConfigCommands extends StandardMessageHandler {

  Logger logger = Logger.getLogger(this.getClass().toString());

  /**
   * Execute the get/putrepositories command.
   */
  public void execute(ProtocolReader reader,
                      ProtocolWriter writer,
                      String dn) throws ProtocolException {

    Thread.currentThread().setName( "agent" );

    Statement reply = null;
    Statement request = null;
    Agent agent = Agent.getGlobalAgent();
    try {
      request = reader.readStatement();
    } catch(Exception e) {
      throw new ProtocolException( "Unable to read statement", e );
    }
    String command = new String(request.getCmd());

    if(command.equals(Protocol.CONFIG_GET_COMMAND)) {

      logger.info( "Received request for configuration" );
      String[] suiteNames = agent.getSuites().getNames();
      IncaDocument doc = IncaDocument.Factory.newInstance();
      IncaDocument.Inca inca = doc.addNewInca();
      IncaDocument.Inca.Repositories repoList = inca.addNewRepositories();
      IncaDocument.Inca.Suites suiteList = inca.addNewSuites();
      for( Repository repository : agent.getRepositories().getRepositories() ) {
        repoList.addRepository(repository.getURL().toString());
      }
      inca.setResourceConfig(agent.getResources().getResourceConfigDocument().getResourceConfig());
      for(int i = 0; i < suiteNames.length; i++) {
        suiteList.addNewSuite();
        suiteList.setSuiteArray(i, agent.getSuites().getSuite(suiteNames[i]).getSuiteDocument().getSuite());
      }
      String passphrase = null;
      if(agent.getResources() != null) {
        passphrase = agent.getResources().getPassphrase();
      }
      String xml = doc.xmlText();
      try {
        xml = XmlWrapper.cryptSensitive(xml, passphrase, false);
        reply = Statement.getOkStatement( xml );
        writer.write(reply);
      } catch ( Exception e ) {
        logger.error( "Reply failed " + e );
      }

    } else if(command.equals(Protocol.CONFIG_SET_COMMAND)) {

      HashMap<String,Integer> proposedChangeCount =
        new HashMap<String,Integer>();

      logger.info( "Received set config request" );

      // Parse the new configuration
      IncaDocument doc = null;
      String passphrase = null;
      if(agent.getResources() != null) {
        passphrase = agent.getResources().getPassphrase();
      }
      String xml = new String(request.getData());
      try {
        xml = XmlWrapper.cryptSensitive(xml, passphrase, true);
        doc = IncaDocument.Factory.parse(xml);
      } catch (Exception e) {
        throw new ProtocolException("Unable to parse configuration " + e, e);
      }

      if(!MessageHandler.isPermitted(dn, Protocol.CONFIG_ACTION)) {
        throw new ProtocolException
          (Protocol.CONFIG_ACTION + " not allowed by " + dn);
      }

      IncaDocument.Inca inca = doc.getInca();
      logger.info("Applying new configuration to agent");

      // Update the repository list
      if( inca.getRepositories() != null ) {
        logger.info( "Updating repository list" );
        String[] repoArray = inca.getRepositories().getRepositoryArray();
        Repository[] repos = new Repository[repoArray.length];
        int i = 0;
        try {
          for(; i < repoArray.length; i++) {
            repos[i] = null;
            logger.debug( "Adding repository " + repoArray[i] );
            repos[i] = new Repository(new URL(repoArray[i]));
          }
          agent.setRepositories(repos);
        } catch(MalformedURLException e) {
          throw new ProtocolException( "Bad URL " + repoArray[i], e );
        } catch(IOException e) {
           logger.error("Unable to access repository url " + repoArray[i],e);
        }
        logger.info( "Finished updating repository list" );
      }

      // Send OK now, since the remaining steps could take a while
      try {
        writer.write(new Statement(Protocol.SUCCESS_COMMAND.toCharArray()));
      } catch ( IOException e ) {
        logger.error("Reply to set configuration request failed: " + e);
      }

      if( inca.getRepositories() != null ) {
        HashMap<String,Integer> thisCount = agent.updateCachedPackages();
        Agent.sumHashes( proposedChangeCount, thisCount );
      }

      if( inca.getResourceConfig() != null ) {
        // Update the resource list--may cause changes in reporter scheduling
        // if existing series are running on changed resources
        logger.info("Updating resource configuration");
        ResourceConfigDocument rcd =
          ResourceConfigDocument.Factory.newInstance();
        rcd.setResourceConfig(inca.getResourceConfig());
        try {
          HashMap<String,Integer> thisCount =
            agent.updateResources(new ResourcesWrapper(rcd));
          Agent.sumHashes( proposedChangeCount, thisCount );
        } catch ( Exception e ) {
          logger.error("Error updating resources on agent: ", e );
        }
        logger.info("Resource configuration update complete");
      }

      if( inca.getSuites() != null ) {
        logger.info( "Updating suites" );
        for(int i = 0; i < inca.getSuites().sizeOfSuiteArray(); i++) {
          SuiteDocument sd = SuiteDocument.Factory.newInstance();
          sd.setSuite(inca.getSuites().getSuiteArray(i));
          try {
            HashMap<String,Integer> thisCount =
              agent.updateSuite(new SuiteWrapper(sd));
            Agent.sumHashes( proposedChangeCount, thisCount );
          } catch(Exception e) {
            logger.error("Error in suite: ", e);
          }
        }
        logger.info( "Suite updates complete" );
      }
      agent.notifyAdminsofChanges( proposedChangeCount ); 
      logger.info( "Set config request complete" );

    } else if(command.equals(Protocol.RUN_NOW_COMMAND)) {

      logger.info( "Received run now request from " + dn );

      // Parse the new configuration
      SuiteWrapper suite = null;
      String data = new String(request.getData());
      int spaceIndex = data.indexOf( " " );
      String type = data.substring( 0, spaceIndex );
      if(!MessageHandler.isPermitted(dn, Protocol.RUNNOW_ACTION+" "+type)) {
        throw new ProtocolException
          (Protocol.RUNNOW_ACTION + " not allowed by " + dn);
      }

      String xml = data.substring( spaceIndex + 1 );
      try {
        SuiteDocument doc = SuiteDocument.Factory.parse(xml);
        suite = new SuiteWrapper(doc);
      } catch (Exception e) {
        throw new ProtocolException("Unable to parse suite " + e, e);
      }


      String suiteName = suite.getSuiteDocument().getSuite().getName();
      if ( ! suiteName.equals(Protocol.IMMEDIATE_SUITE_NAME) ) {
        throw new ProtocolException( "Invalid suite name " + suiteName + "; " +
                                     Protocol.IMMEDIATE_SUITE_NAME + " only" );  
      }

      try {
        writer.write(new Statement(Protocol.SUCCESS_COMMAND.toCharArray()));
      } catch ( IOException e ) {
        logger.error("Reply to run now request failed: " + e);
      }

      agent.updateSuite( suite );
    } else {
      throw new ProtocolException( "Unknown command " + command );
    }

  }

}
