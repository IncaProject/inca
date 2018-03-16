package edu.sdsc.inca.protocol;


import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;


/**
 * @author Jim Hayes
 *
 * This command modifies the log4j settings.  Useful for server debugging.
 */
public class LogConfig extends StandardMessageHandler {

  public void execute(ProtocolReader reader,
                      ProtocolWriter writer,
                      String dn) throws Exception {
    String data = new String(reader.readStatement().getData());
    String[] pieces = data.split("=", 2);
    if(pieces.length != 2) {
      throw new ProtocolException("Malformed LOGCONFIG data '" + data + "'");
    }
    Properties p = new Properties();
    p.setProperty(pieces[0], pieces[1]);
    PropertyConfigurator.configure(p);
    writer.write(Statement.getOkStatement(""));
  }

}
