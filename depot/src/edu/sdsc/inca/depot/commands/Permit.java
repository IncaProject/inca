/*
 * Permit.java
 */
package edu.sdsc.inca.depot.commands;


import java.io.OutputStream;
import java.util.Properties;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.Depot;
import edu.sdsc.inca.depot.DepotPeerClient;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.util.WorkItem;
import edu.sdsc.inca.util.Worker;


/**
 *
 * @author Paul Hoover
 *
 */
public class Permit extends MessageHandler {

  /**
   *
   */
  private static class NotifyPermit implements WorkItem<Worker> {

    private final Properties peerConfig;
    private final String permitDns;
    private final String permitAction;


    // constructors


    /**
     *
     * @param config
     * @param dns
     * @param action
     */
    public NotifyPermit(Properties config, String dns, String action)
    {
      peerConfig = config;
      permitDns = dns;
      permitAction = action;
    }


    // public methods


    /**
     *
     * @param context
     * @throws ConfigurationException
     */
    public void doWork(Worker context) throws ConfigurationException
    {
      DepotPeerClient peer = new DepotPeerClient(peerConfig);

      try {
        peer.connect();
        peer.sendNotifyPermit(permitDns, permitAction);
      }
      catch (Exception err) {
        logger.warn("Unable to send " + Protocol.NOTIFY_PERMIT_COMMAND + " command to " + peer.getUri() + ": " + err.getMessage());
      }
      finally {
        if (peer.isConnected())
          peer.close();
      }
    }
  }


  // public methods


  /**
   *
   * @param reader the reader connected to the client
   * @param output the output stream connected to the client
   * @param dn the DN of the client, null if no authentication
   * @throws Exception
   */
  public void execute(ProtocolReader reader, OutputStream output, String dn) throws Exception
  {
    Statement stmt = reader.readStatement();
    String data = new String(stmt.getData());
    int namesBegin;
    int namesEnd;

    if (data.startsWith("\"")) {
      namesBegin = 1;
      namesEnd = data.indexOf("\"", 1);
    }
    else if (data.startsWith("'")) {
      namesBegin = 1;
      namesEnd = data.indexOf("'", 1);
    }
    else {
      namesBegin = 0;
      namesEnd = data.indexOf(' ', 1);
    }

    if (namesEnd < 0)
      throw new ProtocolException("Bad message format");

    String allNames = data.substring(namesBegin, namesEnd);
    String[] names = allNames.split("\\n");
    String action = data.substring(namesEnd + 1).trim();

    for (int i = 0 ; i < names.length ; i += 1) {
      if (!grantPermission(names[i], action))
        throw new ProtocolException("Exclusive permission for " + action + " already registered");
    }

    ProtocolWriter writer = new ProtocolWriter(output);

    try {
      writer.write(Statement.getOkStatement(""));
    }
    finally {
      writer.close();
    }

    if ((new String(stmt.getCmd())).equals(Protocol.NOTIFY_PERMIT_COMMAND))
      return;

    for (Properties config : Depot.getRunningDepot().getPeerConfigs())
      Depot.getRunningDepot().addWork(new NotifyPermit(config, allNames, action));
  }
}
