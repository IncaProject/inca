package edu.sdsc.inca;


import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.MessageHandlerFactory;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.util.Worker;
import edu.sdsc.inca.util.WorkItem;

import org.apache.log4j.Logger;


/**
 *
 * @depend - Uses - edu.sdsc.inca.protocol.ProtocolReader
 * @depend - Uses - edu.sdsc.inca.protocol.ProtocolWriter
 * @depend - Uses - edu.sdsc.inca.util.protocol.MessageHandler
 * @depend - Uses - edu.sdsc.inca.util.protocol.MessageHandlerFactory
 * @depend - Uses - edu.sdsc.inca.util.Worker;
 * @depend - Uses - edu.sdsc.inca.util.WorkItem;
 */
public class ServerInput implements WorkItem<Worker> {

  protected static final Logger logger = Logger.getLogger(ServerInput.class);

  private final Socket socket;


  /**
   * Create the worker object associated with the named queue.
   *
   * @param q
   */
  public ServerInput(Socket s) {
	  this.socket = s;
  }

  /**
   *
   */
  public void doWork(Worker context) {

    ProtocolReader reader;
    OutputStream output;
    String dn = Component.getDn(socket, true);

    // Try to open input and output streams
    logger.debug("Servicing request from " + socket.getInetAddress() +
                 "; memory is " + Runtime.getRuntime().totalMemory());
    try {
      reader = new ProtocolReader(socket.getInputStream());
      output = socket.getOutputStream();
    } catch(IOException e) {
      logger.error("Unable to access socket stream", e);
      try {
        socket.close();
      } catch(IOException e1) {
        logger.warn("Unable to close socket", e1);
      }
      return;
    }

    // Now that we can read and write to the socket create a task and execute
    try {
      while(!reader.isClosed() && !context.isInterrupted()) {
        MessageHandler handler = null;
        try {
          handler = MessageHandlerFactory.newInstance(reader);
        } catch(SocketTimeoutException e) {
          continue; // allows periodic interrupt check
        }
        if(handler == null) { // Peer closed the connection
          logger.debug("Ending conversation");
          break;
        }
        logger.debug("Running "+ handler.getClass().getName());
        handler.execute(reader, output, dn);
      }
    } catch(Exception e) {
      StackTraceElement[] trace = e.getStackTrace();
      StringBuffer logMessage = new StringBuffer();
      for(int i = 0; i < trace.length; i++) {
        logMessage.append(trace[i].toString()).append("\n");
      }
      logger.error("Caught exception " + e + "\n" + logMessage);
      MessageHandler.errorReply(output, e.toString());
    }
    if(!reader.isClosed()) {
      try {
        reader.close();
      } catch(IOException e) {
        logger.warn("Unable to close reader", e);
      }
    }
    try {
      output.close();
    } catch(IOException e) {
      logger.warn("Unable to close writer", e);
    }
    if(!socket.isClosed()) {
      try {
        socket.close();
       } catch(IOException e) {
         logger.warn("Unable to close socket", e);
       }
    }

  }

}
