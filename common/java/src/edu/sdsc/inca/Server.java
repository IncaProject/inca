package edu.sdsc.inca;

import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.MessageHandlerFactory;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.ConfigProperties;
import edu.sdsc.inca.util.WorkQueue;
import edu.sdsc.inca.util.Worker;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLSocket;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * @author Cathie Olschanowsky
 * @author Jim Hayes
 * @author Shava Smallen
 *         <p/>
 *         A multithreaded server that speaks the Inca protocol.  Provides the
 *         basic implementation for all Java servers within the Inca Framework.
 *         <br/> NOTE: All of the set* methods that manipulate Server
 *         configuration have no effect if called while the Server is running.
 *         <p/>
 *         If the server is shut down using SIGINT there is a hook that will
 *         shut it down properly. This takes a few seconds, but makes sure that
 *         all currently active work is finished before shutdown is completed.
 *         On a SIGKILL work may be lost and the state may be unstable.
 *         <p/>
 * @has 1..1 Has 1..1 inca.util.WorkQueue
 * @has 1..* Has 1..* inca.util.Worker
 */
public class Server extends Component {

  final public static int ACCEPT_TIMEOUT = 5000;
  final public static int CLIENT_TIMEOUT = 5000;

  static {
    MessageHandlerFactory.registerMessageHandler
      (Protocol.GET_LOG_COMMAND, "edu.sdsc.inca.protocol.GetLog");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.LOG_CONFIG_COMMAND, "edu.sdsc.inca.protocol.LogConfig");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.PERMIT_COMMAND, "edu.sdsc.inca.protocol.Permit");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.PING_COMMAND, "edu.sdsc.inca.protocol.Ping");
    MessageHandlerFactory.registerMessageHandler
      (Protocol.START_COMMAND, "edu.sdsc.inca.protocol.VerifyProtocolVersion");
    MessageHandlerFactory.registerMessageHandler(Protocol.REVOKE_COMMAND, "edu.sdsc.inca.protocol.Revoke");
    MessageHandlerFactory.registerMessageHandler(Protocol.REVOKE_ALL_COMMAND, "edu.sdsc.inca.protocol.RevokeAll");
  }

  // Command-line options
  public static final String SERVER_OPTS =
    ConfigProperties.mergeValidOptions(
      COMPONENT_OPTS,
      "  n|numthreads  int    # threads in worker pool\n" +
      "  m|maxQueue    int    max # of pending requests\n" +
      "  v|var         path   Absolute path to server temp dir\n",
      true
    );

  // Protected class vars
  protected static Logger logger = Logger.getLogger(Server.class);

  // Configuration instance variables
  protected String tempPath = null;
  protected int workerCount = 10;
  protected int maxQueueSize;

  // Other instance variables
  protected ClientDispatcher cd = null;
  protected ServerSocket ssocket;
  protected WorkQueue<Worker> workQueue; // all workers will use this Queue
  protected Worker[] workers;    // The worker pool

  /**
   * Returns the path where the server stores temporary files.
   *
   * @return the temp directory path
   */
  public String getTempPath() {
    return this.tempPath;
  }

  /**
   * Returns the number of worker threads this server uses.
   *
   * @return the number of worker threads
   */
  public int getWorkerCount() {
    return this.workerCount;
  }

  /**
   *
   * @return
   */
  public int getMaxQueueSize() {
    return this.maxQueueSize;
  }

  /**
   * Returns true iff the Server is accepting connections.
   *
   * @return true if the server is accepting connections
   */
  public boolean isRunning() {
    return this.cd != null && !this.cd.isShutdown();
  }

  /**
   * Start a thread that accepts and dispatches client connections.
   *
   * @throws Exception if the server fails to start
   */
  public void runServer() throws Exception {

    try {
      this.ssocket =
        (ServerSocket)super.createSocket(true, this.hostname, this.getPort());
    } catch(Exception e) {
      logger.fatal("Unable to init socket:", e);
      throw e;
    }
    try {
      this.ssocket.setSoTimeout(ACCEPT_TIMEOUT);
    } catch(IOException e) {
      // Hmmm... can't set the timeout? Try to run with the default
      logger.warn("Server timeout setting failed");
    }

    this.workQueue = new WorkQueue<Worker>(maxQueueSize);
    this.workers = new Worker[workerCount];
    for(int i = 0; i < workerCount; i++) {
      this.workers[i] = new Worker(workQueue);
      this.workers[i].start();
    }
    Runtime.getRuntime().addShutdownHook(new ServerShutdownHook(this));
    this.cd = new ClientDispatcher(this.ssocket, this.workQueue);
    logger.info("Starting Inca Server on port " + this.getPort());
    this.cd.start();

  }

  /**
   * A convenience function for setting multiple configuration properties at
   * once.  In addition to the keys recognized by the superclass, recognizes:
   * "numthreads", * the number of threads in the worker pool; "var", the path
   * to the temporary file dir.
   *
   * @param config contains server configuration values
   * @throws ConfigurationException on a faulty configuration property value
   */
  public void setConfiguration(Properties config) throws ConfigurationException{
    String prop;
    super.setConfiguration(config);
    if((prop = config.getProperty("numthreads")) != null) {
      this.setWorkerCount((new Integer(prop)).intValue());
    }
    if((prop = config.getProperty("var")) != null) {
      this.setTempPath(prop);
    }
    if((prop = config.getProperty("maxQueue")) != null) {
      this.setMaxQueueSize((new Integer(prop)).intValue());
    } else {
      this.setMaxQueueSize(this.getWorkerCount() * 50);
    }
  }

  /**
   * Sets the directory path where the Server stores temporary files.
   *
   * @param path the temporary directory path
   * @throws ConfigurationException if the path can't be read from the classpath
   */
  public void setTempPath(String path) throws ConfigurationException {
    File tempPath = new File(path);
    if (!tempPath.exists() && !tempPath.mkdir()) {
      throw new ConfigurationException
        ("Unable to create temporary dir '" + path + "'");
    }
    this.tempPath = tempPath.getAbsolutePath();
    // Use the class name in the permissions path to avoid collisions among
    // multiple Components that share a temp directory.
    String className = this.getClass().getName();
    // trim package name
    className = className.substring(className.lastIndexOf(".") + 1);
    MessageHandler.setPermissionsPath
      (this.tempPath + File.separator + className.toLowerCase()+".permissions");
    logger.info("Placing temporary files in " + this.tempPath);
  }

  /**
   * Sets the number of workers the Server uses to handle incoming messages.
   *
   * @param count The (positive) worker count
   */
  public void setWorkerCount(int count) {
    this.workerCount = count;
  }

  /**
   *
   * @param max
   */
  public void setMaxQueueSize(int max) {
    this.maxQueueSize = max;
  }

  /**
   * Asks the Server to shutdown.  Interrupts the server and each of its
   * workers.  The workers will finish their current task; the server shuts down
   * once all workers have finished. This could take a while.
   *
   * @throws InterruptedException if the shutdown is interrupted
   */
  public synchronized void shutdown() throws InterruptedException {
    logger.debug("Server shutting down");
    if(this.cd == null || this.cd.isShutdown()) {
      logger.debug("already idle");
      return;
    }
    this.cd.interrupt();
    while(!this.cd.isShutdown()) {
      logger.debug("waiting for shutdown");
      Thread.sleep(100);
    }
    // Shut down the workers
    for(int i = 0; i < this.workers.length; i++) {
      this.workers[i].interrupt();
    }
    for(int i = 0; i < this.workers.length; i++) {
      logger.debug("Wait for worker " + workers[i].getName());
      for(int maxWait = 3000;
          this.workers[i].isAlive() && maxWait > 0;
          maxWait -= 100) {
        try {
          Thread.sleep(100);
        } catch(InterruptedException e) {
          // empty
        }
      }
      if(this.workers[i].isAlive()) {
        logger.warn("Shutdown wait for " + workers[i].getName() + " failed");
      }
    }
    // Close the socket
    try {
      this.ssocket.close();
    } catch(IOException e) {
      logger.error("Error closing socket", e);
    }
  }

  /**
   * A class that listens on a server socket and adds new client connections to
   * a work queue.
   */
  protected class ClientDispatcher extends Thread {

    protected boolean shutdown;
    protected ServerSocket ss;
    protected WorkQueue<Worker> wq;

    /**
     * Constructs a new ClientDispatcher.
     *
     * @param ss the server socket to listen to
     * @param wq the work queue on which to place new client connections
     */
    public ClientDispatcher(ServerSocket ss,
                            WorkQueue<Worker> wq) {
      this.shutdown = false;
      this.ss = ss;
      this.wq = wq;
    }

    /**
     * Returns true iff this thread has been interrupted and clean up is done.
     */
    public synchronized boolean isShutdown() {
      return this.shutdown;
    }

    /**
     * Begin listening to the server socket and queueing client connections.
     */
    public void run() {

      while(!this.isInterrupted()) {
        Socket client = null;
        try {
          client = this.ss.accept();
        } catch(SocketTimeoutException e) {
          continue; // allows periodic interrupt check
        } catch(IOException e) {
          logger.fatal("Unable to accept connections", e);
          break;
        }

        try {
          client.setSoTimeout(CLIENT_TIMEOUT);
        } catch(IOException e) {
          // Hmmm... can't set the timeout? Try to run with the default
          logger.warn("Client timeout setting failed");
        }

        String info = client.getInetAddress().toString();
        if(client instanceof SSLSocket) {
          try {
            Certificate[] certs =
              ((SSLSocket)client).getSession().getPeerCertificates();
            if(certs.length > 0 && certs[0] instanceof X509Certificate) {
              info += " DN " +
                ((X509Certificate)certs[0]).getSubjectX500Principal().getName();
            }
          } catch(Exception e) {
            // empty
          }
        }
        logger.info("Connection from " + info);

        if (!this.wq.addWork(new ServerInput(client))) {
          logger.warn("Failed to add connection to work queue");

          try {
            OutputStream output = null;

            try {
              output = client.getOutputStream();

              MessageHandler.errorReply(output, "Server is busy");
            }
            finally {
              if (output != null)
                output.close();

              if (!client.isClosed())
                client.close();
            }
          }
          catch (IOException ioErr) {
            logger.error("Error while sending error reply", ioErr);
          }
        }
      }

      // All done
      this.setShutdown(true);
      logger.debug("Finished shutting down");

    }

    protected synchronized void setShutdown(boolean shutdown) {
      this.shutdown = shutdown;
    }

  }

  /**
   * This Class is run in a seperate thread when the server is shut down using
   * ^C or SIGINT.  If it is shutdown using SIGKILL then none of the cleanup
   * will be done - which is a problem.
   */
  protected class ServerShutdownHook extends Thread {

    protected Server server;

    /**
     * Create a new shutdown hook and attach it to the given server.
     *
     * @param server server that may need shut down
     */
    public ServerShutdownHook(Server server) {
      this.server = server;
    }

    /**
     * Notify the server of a shutdown request.
     */
    public void run() {
      logger.debug("interrupting server");
      try {
        this.server.shutdown();
      } catch(InterruptedException e) {
        logger.error("Interuption during shutdown", e);
      }
      logger.debug("server should be shutdown");
    }

  }

  /**
   * A main function to start a generic server.  It takes command-line arguments
   * for each Server configuration value: <br/>
   * <pre>
   * usage: <code>java edu.sdsc.inca.Server</code><br/>
   *  -a,--auth yes|no       Authenticate the connection?
   *  -c,--cert <path>       Path to the server certificate
   *  -h,--help              Prints a short help and usage statement
   *  -h,--hostname <string> Hostname of server if different than what Java
   *                         discovers
   *  -i,--init <path>       Path to server configuration properties file.
   *  -k,--key <path>        Path to the server key
   *  -n,--numthreads <int>  # of worker threads that the server runs
   *  -P,--password yes|no   Read encryption password from stdin
   *  -p,--port <int>        The port for the server to listen on
   *  -t,--trusted <path>    Path to a directory containing all of the
   *                         trusted certificates.
   *  -T,--taskwait <int>    # milliseconds for timing out task I/O
   *  -v,--var <path>        The directory the server should use to save
   *                         temp files.  Must be an absolute path.
   *  -V,--version           Print program version and exit.
   *  -w,--wait <int>        # millisecs between shutdown checks
   * </pre><br/>
   * Each of these values may also be specified in the initialization file
   * or in system properties defined via the -D switch.  In both of these cases,
   * the configuration name must be prefixed with "inca.server.", e.g., use
   * "inca.server.numthreads" to specify the numthreads value.  Command line
   * arguments override any properties specified in this fashion.
   *
   * @param args command line arguments, as above.
   */
  public static void main(String[] args) {
    Server s = new Server();
    try {
      configComponent
        (s, args, SERVER_OPTS, "inca.server.", "edu.sdsc.inca.Server",
         "inca-common-java-version");
      s.runServer();
      while(s.isRunning()) {
        Thread.sleep(2000);
      }
    } catch(Exception e) {
      logger.fatal("Server error: ", e);
      System.err.println("Server error: " + e);
      System.exit(1);
    }
  }

}
