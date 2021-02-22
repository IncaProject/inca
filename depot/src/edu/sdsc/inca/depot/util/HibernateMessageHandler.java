package edu.sdsc.inca.depot.util;


import java.io.OutputStream;

import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;


/**
 * Created by IntelliJ IDEA.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public abstract class HibernateMessageHandler extends MessageHandler {

  /**
   * Overrides regular MessageHandler execute function in order to handle
   * hibernate sessions effectively. Will open a hibenate session and then
   * ensure that it's closed after a task is executed.  Caller will need to
   * create a function called executeHibernateAction.
   *
   * @throws Exception
   */
  @Override
  public void execute(ProtocolReader reader,
                      OutputStream output,
                      String dn) throws Exception {

    executeHibernateAction(reader, new ProtocolWriter(output), dn);
  }

  /**
   * Execute a hibernate-related task efficiently (i.e., session is safely
   * opened and closed.
   *
   * @throws ProtocolException
   */
  public abstract void executeHibernateAction(
      ProtocolReader reader, ProtocolWriter writer, String dn) throws Exception;

}
