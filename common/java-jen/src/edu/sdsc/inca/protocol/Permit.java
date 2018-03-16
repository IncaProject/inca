package edu.sdsc.inca.protocol;


import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;


/**
 * @author Jim Hayes
 *
 * Registers permission for a specified DN to perform a specified action.
 */
public class Permit extends StandardMessageHandler {

  public void execute(ProtocolReader reader,
                      ProtocolWriter writer,
                      String dn) throws Exception {
    String data = new String(reader.readStatement().getData());
    boolean quoted = data.startsWith("\"") || data.startsWith("'");
    int pos = data.indexOf(quoted ? data.charAt(0) : ' ', 1);
    if(pos < 0) {
      throw new ProtocolException("Bad message format");
    }
    dn = data.substring(quoted ? 1 : 0, pos);
    String action = data.substring(pos + 1).trim();
    if(MessageHandler.permittees(action) != null && !isPermitted(dn, action)) {
      throw new ProtocolException
        ("Exclusive permission for " + action + " already registered");
    }
    writer.write(Statement.getOkStatement(""));
    MessageHandler.permit(dn, action);
  }

}
