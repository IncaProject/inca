package edu.sdsc.inca.protocol;


/**
 * @author Cathie Olschanowsky
 *
 * This command is useful to make sure that the server is alive and responding.
 * Good for testing and not much else.
 *
 * @depend - Uses - inca.protocol.Statement
 */
public class Ping extends StandardMessageHandler {

  public void execute(ProtocolReader reader,
                      ProtocolWriter writer,
                      String dn) throws Exception {
    Statement statement = reader.readStatement();
    String message = new String(statement.getData());
    if(!MessageHandler.isPermitted(dn, Protocol.PING_ACTION + " " + message)) {
      throw new ProtocolException
        (Protocol.PING_ACTION + " not allowed by " + dn);
    }
    writer.write(Statement.getOkStatement(message));
  }

}
