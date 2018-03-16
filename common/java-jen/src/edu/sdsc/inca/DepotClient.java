package edu.sdsc.inca;

import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.dataModel.reportDetails.ReportDetailsDocument;
import edu.sdsc.inca.util.ConfigProperties;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import java.util.Calendar;
import java.net.SocketTimeoutException;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

/**
 * This class allows access to the services provided by an Inca Depot.  Along
 * with defining an API that understands the Depot protocol, it defines a main
 * method that allows access to an Depot via a telnet-style interface.
 */
public class DepotClient extends Client {

  public static final String CRLF = "\r\n";
  final private static int MAX_TIMEOUT_TRIES = 20;


  // Command-line options--see main method
  public static final String DEPOT_CLIENT_OPTS =
    ConfigProperties.mergeValidOptions(
      CLIENT_OPTS.replaceFirst(" *S\\|server[^\\n]*\\n", ""),
      "  D|depot  str  Depot specification; host:port\n",
      true);

  // Protected class vars
  protected static final Logger logger = Logger.getLogger(DepotClient.class);

  /**
   * A convenience function for setting multiple configuration properties at
   * once.  In addition to the keys recognized by the superclass, recognizes:
   * "depot", the specification of the Depot.
   *
   * @param config contains client configuration values
   * @throws ConfigurationException on a faulty configuration property value
   */
  public void setConfiguration(Properties config) throws ConfigurationException{
    super.setConfiguration(config);
    String prop;
    if((prop = config.getProperty("depot")) != null) {
      this.setServer(prop, 0);
    }
  } 

  /**
   * Asks the Depot to insert a new report into the database.
   *
   * @param resource the name of the resource that generated the report
   * @param reportXml the XML for the report itself (see report schema)
   * @param sysusage system usage information
   * @param stderr optional stderr output from the reporter
   * @param context execution context for the reporter
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public void insertReport(String resource,
                           String reportXml,
                           String sysusage,
                           String stderr,
                           String context)
    throws IOException, ProtocolException {
    StringBuffer data =
      new StringBuffer(resource).append(" ").append(context).append(CRLF);
    if(stderr != null) {
      data.append(Protocol.INSERT_STDERR_COMMAND).append(" ").append(stderr).
           append(CRLF);
    }
    data.append(Protocol.INSERT_STDOUT_COMMAND).append(" ").append(reportXml).
         append(CRLF).append(Protocol.INSERT_SYSUSAGE_COMMAND).append(" ").
         append(sysusage);
    this.dialog(Protocol.INSERT_COMMAND, data.toString());
  }

  /**
   * Asks the Depot for information about the classes in the database.  Returns
   * XML that specifies the name of each class and the names of its fields.
   *
   * @return XML that represents the DB structure
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String queryDatabase() throws IOException, ProtocolException {
    return this.dialog(Protocol.QUERY_DB_COMMAND, "");
  }

  /**
   * Asks the Depot for the ReportDetailsDocument for a specified instance.
   *
   * @param instanceId the id of the instance to retrieve
   * @param configId the id of the related series config to retrieve
   * @return the ReportDetailsDocument for the instance, null if none
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   * @throws XmlException if the Depot response cannot be parsed
   */
  public String queryInstance(long instanceId, long configId)
    throws IOException, ProtocolException, XmlException {
   
    String[] xml =
      queryDialog(Protocol.QUERY_INSTANCE_COMMAND, instanceId + " " + configId);
    if ( xml != null && xml.length > 0 ) {
      return xml[0];
    } else {
      return null;
    }
  }

  /**
   * Asks the Depot for all the suite guids in its database.
   *
   * @return an array of guids
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String[] queryGuids() throws IOException, ProtocolException {
    String reply = this.dialog(Protocol.QUERY_GUIDS_COMMAND, "");
    return reply.equals("") ? new String[0] : reply.split("\\n");
  }

  /**
   * Asks the Depot to run an HQL query; returns the result.
   *
   * @param select the HQL select query
   * @return an array of strings, the result of the query
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String[] queryHql(String select) throws IOException, ProtocolException{
    return queryDialogWithNoSoTimeout(Protocol.QUERY_HQL_COMMAND, select);
  }

  /**
   * Asks the Depot for XML for the latest instance of each series selected by
   * an HQL WHERE clause expression.
   *
   * @param expr an HQL WHERE clause expression specifying the desired series
   * @return an array of XML strings representing each instance
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String[] queryLatest(String expr)
    throws IOException, ProtocolException {
    return queryDialog(Protocol.QUERY_LATEST_COMMAND, expr);
  }

  /**
   * Asks the Depot for XML for instances collected over a given period of time
   * for each series selected by an HQL WHERE clause expression.
   *
   * @param numDays the number of days of history to retrieve from today
   * @param expr an HQL WHERE clause expression specifying the desired series
   * @return an array of GraphInstance XML strings, one per instance
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String[] queryPeriod(int numDays, String expr)
    throws IOException, ProtocolException {

    // the start date
    Calendar startDate = Calendar.getInstance();
    startDate.add( Calendar.DATE, -numDays );

    // now
    Calendar endDate = Calendar.getInstance();

    return queryPeriod( startDate.getTime(), endDate.getTime(), expr );
  }

  /**
   * Asks the Depot for XML for instances collected over a given period of time
   * for each series selected by an HQL WHERE clause expression.
   *
   * @param begin series instances collected before this time are ignored
   * @param end series instances collected after this time are ignored
   * @param expr an HQL WHERE clause expression specifying the desired series
   * @return an array of GraphInstance XML strings, one per instance
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String[] queryPeriod(Date begin, Date end, String expr)
    throws IOException, ProtocolException {
    String request = begin.getTime() + " " + end.getTime() + " " + expr;
    return queryDialog(Protocol.QUERY_PERIOD_COMMAND, request);
  }

  /**
   * Asks the Depot for XML that summarizes the success/failure history over a
   * given period to today for each series selected by an HQL WHERE
   * clause expression.
   *
   * @param period the summarizing period; may be one of the literals "DAY",
   *                "WEEK", "MONTH", or "QUARTER" or a number of minutes
   * @param numDays the number of days of history to retrieve from today
   * @param expr an HQL WHERE clause expression specifying the desired series
   * @return an array of XML strings summarizing each series
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public static long MILLIS_IN_A_DAY = 1000L * 60L * 60L * 24L;
  public String[] queryStatusHistory(String period, int numDays, String expr)
    throws IOException, ProtocolException {
    Calendar cal = Calendar.getInstance();
    long endTime = cal.getTimeInMillis() - 1;
    // DST causes confusion with the following--days aren't of fixed length
    // cal.add(Calendar.DATE, -numDays);
    cal.setTimeInMillis(cal.getTimeInMillis() - (long)numDays*MILLIS_IN_A_DAY);
    long startTime = cal.getTimeInMillis();
    String request = period + " " + startTime + " " + endTime + " " + expr;
    return queryDialog(Protocol.QUERY_STATUS_COMMAND, request);
  }

  /**
   * Asks the Depot for XML that summarizes the success/failure history over a
   * given period for each series selected by an HQL WHERE clause expression.
   *
   * @param period the summarizing period; may be one of the literals "DAY",
   *                "WEEK", "MONTH", or "QUARTER" or a number of minutes
   * @param begin series instances collected before this time are ignored
   * @param end series instances collected after this time are ignored
   * @param expr an HQL WHERE clause expression specifying the desired series
   * @return an array of XML strings summarizing each series
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String[] queryStatusHistory
    (String period, Date begin, Date end, String expr)
    throws IOException, ProtocolException {
    String request =
      period + " " + begin.getTime() + " " + end.getTime() + " " + expr;
    return queryDialog(Protocol.QUERY_STATUS_COMMAND, request);
  }

  /**
   * Asks the Depot to insert a report that may predate existing reports into
   * the database.
   *
   * @param resource the name of the resource that generated the report
   * @param reportXml the XML for the report itself (see report schema)
   * @param sysusage system usage information
   * @param stderr optional stderr output from the reporter
   * @param context execution context for the reporter
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public void resendReport(String resource,
                           String reportXml,
                           String sysusage,
                           String stderr,
                           String context)
    throws IOException, ProtocolException {
    StringBuffer data =
      new StringBuffer(resource).append(" ").append(context).append(CRLF);
    if(stderr != null) {
      data.append(Protocol.INSERT_STDERR_COMMAND).append(" ").append(stderr).
           append(CRLF);
    }
    data.append(Protocol.INSERT_STDOUT_COMMAND).append(" ").append(reportXml).
         append(CRLF).append(Protocol.INSERT_SYSUSAGE_COMMAND).append(" ").
         append(sysusage);
    this.dialog(Protocol.RESEND_COMMAND, data.toString());
  }

  /**
   * Asks the Depot to add XML for a suite to its database, replacing any
   * existing XML for the same suite.
   *
   * @param suiteXml XML representing the suite to be saved
   * @return the reply message data
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  public String updateSuite(String suiteXml)
    throws IOException, ProtocolException {
    return this.dialog(Protocol.SUITE_UPDATE_COMMAND, suiteXml);
  }

  /**
   * An internal method that handles the functionality common to several
   * query methods.
   *
   * @param command the command to send the Depot
   * @param data the data to accompany the command
   * @return an array of query response strings sent by the Depot
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  protected String[] queryDialog(String command, String data)
    throws IOException, ProtocolException {
    Statement reply;
    Vector result = new Vector();
    this.write(new Statement(command, data));
    while((reply = this.read()) != null) {
      command = new String(reply.getCmd());
      data = new String(reply.getData());
      if(Protocol.END_QUERY_RESULTS_COMMAND.equals(command)) {
        break;
      } else if(!Protocol.QUERY_RESULT.equals(command)) {
        throw new ProtocolException("Received error: " + data);
      }
      result.add(data);
    }
    logger.debug(result.size() + " results returned from suite query");
    return (String[])result.toArray(new String[result.size()]);
  }

  /**
   * An internal method that handles the functionality common to several
   * query methods.  Similar to queryDialog but wil ignore socket timeouts
   * up to MAX.
   *
   * @param command the command to send the Depot
   * @param data the data to accompany the command
   * @return an array of query response strings sent by the Depot
   * @throws IOException on read/write error
   * @throws ProtocolException on an invalid message
   */
  protected String[] queryDialogWithNoSoTimeout(String command, String data)
    throws IOException, ProtocolException {
    Statement reply;
    Vector result = new Vector();
    this.write(new Statement(command, data));
    int numSoTimeouts = 1;
    while( true ) {
      try {
        reply = this.read();
      } catch (SocketTimeoutException e) {
        numSoTimeouts++;
        logger.info
          ( "Received socket timeout " + numSoTimeouts + " during read" );
        if ( numSoTimeouts > MAX_TIMEOUT_TRIES ) {
          logger.info( "Maximum number of timeouts exceeded...giving up" );
          throw new IOException
            ( "No response from depot after " + MAX_TIMEOUT_TRIES + " tries");
        } else {
          continue;
        }
      }
      if ( reply == null ) break;
      command = new String(reply.getCmd());
      data = new String(reply.getData());
      if(Protocol.END_QUERY_RESULTS_COMMAND.equals(command)) {
        break;
      } else if(!Protocol.QUERY_RESULT.equals(command)) {
        throw new ProtocolException("Received error: " + data);
      }
      result.add(data);
    }
    logger.debug(result.size() + " results returned from suite query");
    return (String[])result.toArray(new String[result.size()]);
  }

  /**
   * A simple main method for exercising the DepotClient.
   * <br/>
   * <pre>
   * usage: <code>java inca.DepotClient</code><br/>
   *  -a|--auth     boolean Authenticated (secure) connection?
   *  -c|--cert     path    Path to the authentication certificate
   *  -D|--depot    str     Depot specification; host:port
   *  -h|--help     null    Print help/usage
   *  -i|--init     path    Path to properties file
   *  -k|--key      path    Path to the authentication key
   *  -P|--password str     Specify how to obtain encryption password
   *  -t|--trusted  path    Path to authentication trusted certificate dir
   *  -V|--version  null    Display program version
   * </pre>
   * <br/>
   * Each of these properties, other than --init, can also be specified in the
   * initialization file or in system properties defined via the -D switch.  In
   * each of these cases, the configuration name must be prefixed with
   * "inca.depot.", e.g., use "inca.depot.cert" to specify the cert value.
   * Command line arguments override any properties specified in this fashion.
   * main allows the user to communicate with the Depot via a telnet-style
   * interface.  Supported values for the --password argument are: "no" or
   * "false" (no encryption); "yes", "true", or "stdin:prompt" (read password
   * from stdin after optionally prompting); "pass:text" (specify the password
   * directly--should only be used for testing and, possibly, in the init file).
   * <br/>
   *
   * @param args command line arguments, as above.
   */
  public static void main(final String[] args) {
    DepotClient d = new DepotClient();
    try {
      configComponent
        (d,args,DEPOT_CLIENT_OPTS,"inca.depot.","edu.sdsc.inca.DepotClient",
         "inca-common-java-version" );
      d.connect();
    } catch(Exception e) {
      logger.fatal("Client error: ", e);
      System.err.println("Client error: " + e);
      System.exit(1);
    }
    Client.telnetDialog(d, "Depot> ");
  }

}
