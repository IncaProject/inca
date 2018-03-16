/*
 * RegisterPeer.java
 */
package edu.sdsc.inca.depot.commands;


import java.io.OutputStream;
import java.util.Properties;

import edu.sdsc.inca.Depot;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;


/**
 *
 * @author Paul Hoover
 *
 */
public class RegisterPeer extends MessageHandler {

  /**
   *
   * @param reader the reader connected to the client
   * @param output the output stream connected to the client
   * @param dn the DN of the client, null if no authentication
   * @throws Exception
   */
  public void execute(ProtocolReader reader, OutputStream output, String dn) throws Exception
  {
    String peerUri = new String(reader.readStatement().getData());

    if (peerUri == null || peerUri.length() == 0)
      throw new ProtocolException("Peer URI missing from " + Protocol.REGISTER_PEER_COMMAND + " command");

    if (!isDuplicatePeer(peerUri))
      Depot.getRunningDepot().addPeerConfig(peerUri);

    String perms = getPermissionsAsXml();

    ProtocolWriter writer = new ProtocolWriter(output);

    try {
      writer.write(Statement.getOkStatement(perms));
    }
    finally {
      writer.close();
    }

    grantPeerPermission(dn);

    reader.close();
  }


  // private methods


  /**
   *
   * @param uri
   * @return
   */
  private String extractAddress(String uri)
  {
    int offset = uri.indexOf("://");

    if (offset < 0)
      return uri;

    return uri.substring(offset + 3);
  }

  /**
   *
   * @param uri
   * @return
   */
  private boolean isDuplicatePeer(String uri)
  {
    String address = extractAddress(uri);

    for (Properties config : Depot.getRunningDepot().getPeerConfigs()) {
      String peerUri = config.getProperty("peer");
      String peerAddress = extractAddress(peerUri);

      if (address.equals(peerAddress))
        return true;
    }

    return false;
  }
}
