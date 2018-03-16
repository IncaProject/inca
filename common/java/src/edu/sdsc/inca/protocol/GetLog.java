package edu.sdsc.inca.protocol;


import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;

import edu.sdsc.inca.util.StringMethods;


/**
 * @author Jim Hayes
 *
 * This command retrieves the server log file.  Useful for server debugging.
 */
public class GetLog extends StandardMessageHandler {

  public void execute(ProtocolReader reader,
                      ProtocolWriter writer,
                      String dn) throws Exception {
    reader.readStatement();
    Enumeration<?> e = Logger.getRootLogger().getAllAppenders();
    String logText = "";
    while(e.hasMoreElements()) {
      Appender a = (Appender)e.nextElement();
      if(a instanceof FileAppender) {
        logText = StringMethods.fileContents(((FileAppender)a).getFile());
        break;
      }
    }
    writer.write(Statement.getOkStatement(logText));
  }

}
