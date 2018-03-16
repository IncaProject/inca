package edu.sdsc.inca.protocol;


import org.apache.log4j.Logger;


/**
 * @author Cathie Olschanowsky
 * @author Jim Hayes
 *
 * Does a comparison of the Inca Protocol Versions being used.
 * If they are not an exact match - throws an exception.
 *
 * @depend - Uses - inca.protocol.Statement
 */
public class VerifyProtocolVersion extends StandardMessageHandler {

  Logger logger = Logger.getLogger(this.getClass().toString());

  public void execute(ProtocolReader reader,
                      ProtocolWriter writer,
                      String dn) throws Exception {
    String myVersion = Statement.getVersion();
    String theirVersion = new String(reader.readStatement().getData());
    logger.debug("compare my version " + myVersion + " to " + theirVersion);
    Statement reply = myVersion.equals(theirVersion) ?
      Statement.getOkStatement(myVersion) :
      Statement.getErrorStatement
        ("Incompatable version: got " + theirVersion + " need " + myVersion);
    writer.write(reply);
  }

}
