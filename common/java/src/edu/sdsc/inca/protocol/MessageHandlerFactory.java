package edu.sdsc.inca.protocol;

import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Cathie Olschanowsky
 *
 * This factory reads a protocol stream and instantiates MessageHandler
 * classes.  MessageHandler classes can be added using the addTask method.
 *
 * @depend - Uses - inca.Protocol.ProtocolException
 * @depend - Uses - inca.Protocol.ProtocolReader
 * @depend - Creates - inca.util.MessageHandler
 * 
 */
public class MessageHandlerFactory {

  private static Properties handlers = new Properties();
  private static ClassLoader cl = MessageHandlerFactory.class.getClassLoader();

  /**
   * Returns an instance of a MessageHandler based on the next command coming
   * over a connection.  Returns null on end of stream.
   *
   * @param reader the reader from which the next command is read
   * @return the Task Handler mapped to the next command.
   * @throws IllegalAccessException
   * @throws InstantiationException Some problem instatiating the implemenation
   * @throws IOException Problem reader next command
   * @throws ProtocolException Bad data/unknown command in the input stream
   */
  public static MessageHandler newInstance(ProtocolReader reader)
    throws IllegalAccessException, InstantiationException, IOException,
           ProtocolException {
    // peek at the first command in the conversation
    String cmd = reader.peekCommand();
    if(cmd == null) {
      return null;
    }
    // retrieve and instatiate the class that handles that command
    String className = handlers.getProperty(cmd);
    if(className == null) {
      throw new ProtocolException("Received unknown command '" + cmd + "'");
    }
    final Class handler;
    try {
      handler = cl.loadClass(className);
    } catch(ClassNotFoundException e) {
      throw new IOException("Class not found:" + e);
    }
    // return the command that is responsible for handling the conversation
    return (MessageHandler)handler.newInstance();
  }

  /**
   * Add a task to the known list.
   * Every server that uses this class will need to do this.
   *
   * @param command The Command as it will be seen in the protocol
   * @param handler The handler that should be invoked upon this command
   */
  public static void registerMessageHandler(String command, String handler) {
    handlers.setProperty(command, handler);
  }

}
