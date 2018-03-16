package edu.sdsc.inca.protocol;

/**
 * This class declares constants and contains documentation pertaining to the
 * Inca message protocol.  At the basic level, an Inca <i>Statement</i>
 * consists of a command word (by convention all uppercase) optionally followed
 * by a space and payload data.  The format of the payload is defined by the
 * command.  Statements end with a CRLF combination.
 *
 * Replies from Inca servers typically have one of two formats.  If the command
 * succeeds, the server responds with a statement that has "OK" as the command
 * word and any requested data as the payload.  If an error occurs, the server
 * responds with a statement that has "ERROR" as the command word and an
 * explanatory message as the payload.
 */
public class Protocol {

  // Server responses.
  /**
   * Command: ERROR SP message CRLF
   * Response: none.
   * Response on any server error, with a descriptive message.
   */
  public static final String FAILURE_COMMAND = "ERROR";

  /**
   * Command: OK [SP data] CRLF
   * Response: none.
   * Response on server success.  The presence and format of the data depends
   * on the command.
   */
  public static final String SUCCESS_COMMAND = "OK";

  /**
   * Command: QUERYRESULT SP xml CRLF
   * Reponse: none.
   */
  public static final String QUERY_RESULT = "QUERYRESULT";

  // Globally-recognized server commands.

  /**
   * Command: GETLOG CRLF
   * Response: OK SP text CRLF.
   * For testing; returns the current contents of the component log file.
   */
  public static final String GET_LOG_COMMAND = "GETLOG";

  /**
   * Command: LOGCONFIG SP property=level ,appender [,appender etc]
   * Response: OK CRLF.
   * For testing; changes the log4j settings.
   */
  public static final String LOG_CONFIG_COMMAND = "LOGCONFIG";

  /**
   * Command: PERMIT SP dn SP action CRLF
   * Response: OK CRLF.
   * Indicate that a client passing using a certificate with a specified DN
   * should have exclusive permission to perform a specified action.  The DN
   * may be surrounded by quotes to allow for embedded spaces.
   */
  public static final String PERMIT_COMMAND = "PERMIT";

  /**
   * Command: PING SP text CRLF
   * Response: OK SP text CRLF.
   * For testing; echos whatever data is sent in the command.
   */
   public static final String PING_COMMAND = "PING";

   /**
    * Command: START SP version
    * Response: OK SP version.
    * Tests whether client and server are using the same protocol version.
    */
   public static final String START_COMMAND = "START";

  // Recognized Agent protocol commands.

  /**
   * Command: APPROVE SP resourceId SP xml CRLF
   * Response: OK CRLF.
   * Message from a Reporter Manager to the Agent indicating that it has
   * approved the enclosed suite updates.  The Agent responds with OK.
   */
  public static final String APPROVE_COMMAND = "APPROVE";

  /**
   * Command: GETCATALOG [SP url] CRLF
   * Response: OK SP reporters CRLF.
   * Returns the catalog from a specific repository (if url is supplied) or a
   * merged catalog from all repositories known to the agent.  reporters is a
   * blank-line separated set of reporters, each of which is a newline-separated
   * set of attribute:value pairs that describe an individual reporter.
   */
  public static final String CATALOG_GET_COMMAND = "GETCATALOG";

  /**
   * Command: GETCONFIG CRLF
   * Response: OK SP xml CRLF.
   * See the IncaDocument schema for the xml format.
   */
  public static final String CONFIG_GET_COMMAND = "GETCONFIG";

  /**
   * Command: GETPROPOSED SP resourceId CRLF
   * Response: OK SP xml CRLF.
   *
   * Message from inca approve script (on resource) to Agent to retrieve the
   * proposed suite changes for the specified reporter manager.  Agent responds
   * with a list of suites containing series configs changes).  See the Inca
   * schema for the xml format.
   */
  public static final String PROPOSED_GET_COMMAND = "GETPROPOSED";

  /**
   * Command: PROXY_RENEW_INFO SP hostname CRLF
   * Response: HOSTNAME SP hostname CRLF
   *           [DN SP dn CRLF]
   *           PORT SP port CRLF
   *           PASSWORD SP password CRLF
   *           LIFETIME SP lifetime CRLF
   * Response: OK CRLF
   */
  public static final String PROXY_RENEW_INFO_GET_COMMAND = "PROXY_RENEW_INFO";

  /**
   * Command: REGISTER SP [NEW|EXISTING] SP resourceId CRLF
   * Response: OK CRLF.
   * Registers a reporter manager with the agent.  The payload is the
   * fully-qualified DNS name of the host where the manager is running.
   */
  public static final String REGISTER_COMMAND = "REGISTER";

  /**
   * Command: RUNNOW SP [incat|consumer] SP xml CRLF
   * Response: OK CRLF.
   * See the SuiteDocument schema for the xml format.
   */
  public static final String RUN_NOW_COMMAND = "RUNNOW";

  /**
   * Command: SETCONFIG SP xml CRLF
   * Response: OK CRLF.
   * See the IncaDocument schema for the xml format.
   */
  public static final String CONFIG_SET_COMMAND = "SETCONFIG";

  /**
   * Suite name for run immediately requests.
   */
  public static final String IMMEDIATE_SUITE_NAME = "_runNow";

  /**
   * Constants for matching resource macro names, Agent predefined macro names,
   * and some default values.
   */
  public static final String MACRO_NAME_PATTERN =  "[A-Za-z_][\\w\\-\\.:]*(->[\\w\\-\\.:]+)?";
  public static final String PREDEFINED_MACRO_NAME_PATTERN = "__[A-Za-z]+__";
  public static final String CHECK_PERIOD_MACRO = "__checkPeriod__";
  public static final String
    COMPUTE_METHOD_MACRO = "__remoteInvocationMethod__";
  public static final String COMPUTE_PORT_MACRO = "__incaComputePort__";
  public static final String COMPUTE_SERVER_MACRO = "__incaComputeServer__";
  public static final String EMAIL_MACRO = "__incaAdminEmail__";
  public static final String EQUIVALENT_MACRO = "__equivalent__";
  public static final String FILE_PORT_MACRO = "__gridFtpPort__";
  public static final String FILE_SERVER_MACRO = "__gridFtpServer__";
  public static final String GRAM_DN_MACRO = "__gramDN__";
  public static final String GRAM_SERVICE_MACRO = "__gramService__";
  public static final String GRIDFTP_DN_MACRO = "__gridFtpDN__";
  public static final String GROUPNAME_MACRO = "__groupname__";
  public static final String GASS_PORT_MACRO = "__gassPort__";
  public static final String HOSTS_MACRO = "__incaHosts__";
  public static final String LOGIN_ID_MACRO = "__sshUserName__";
  public static final String MYPROXY_DN_MACRO = "__myproxyDn__";
  public static final String MYPROXY_HOST_MACRO = "__myproxyHost__";
  public static final String MYPROXY_PORT_MACRO = "__myproxyPort__";
  public static final String MYPROXY_LIFETIME_MACRO = "__myproxyLifetime__";
  public static final String MYPROXY_PASSWORD_MACRO = "__myproxyPassword__";
  public static final String MYPROXY_USERNAME_MACRO = "__myproxyUsername__";
  public static final String PATTERN_MACRO = "__regexp__";
  public static final String PROXY_RENEW_MACRO = "__proxyRenewal__";
  public static final String PROXY_MACRO = "__incaProxy__";
  public static final String SSH_IDENTITY_MACRO = "__sshPrivateKeyFile__";
  public static final String SSH_PASSWORD_MACRO = "__sshPrivateKeyPassword__";
  public static final String
    WORKING_DIR_MACRO = "__incaReporterManagerLocation__";
  public static final String CHECK_PERIOD_MACRO_DEFAULT = "1";
  public static final String[] COMPUTE_METHODS = {
    "globus2", "local", "manual", "ssh"
  };
  public static final String COMPUTE_METHOD_MACRO_DEFAULT = "ssh";
  public static final String GLOBUS_SERVER_PORT_MACRO_DEFAULT = "2119";
  public static final String GRAM_SERVICE_MACRO_DEFAULT = "jobmanager";
  public static final String FILE_PORT_MACRO_DEFAULT = "2811";
  public static final String WORKING_DIR_MACRO_DEFAULT = "incaReporterManager";

  // Constants that represent actions for the PERMIT command.
  public static final String APPROVE_ACTION = "approve";
  public static final String CONFIG_ACTION = "config";
  public static final String INSERT_ACTION = "insert";
  public static final String PING_ACTION = "ping";
  public static final String REPORT_ACTION = "report";
  public static final String RUNNOW_ACTION = "runNow";
  public static final String RUNNOW_TYPE_INCAT = "incat";
  public static final String RUNNOW_TYPE_CONSUMER = "consumer";
  public static final String SUITE_ACTION = "suite";
  public static final String UPDATE_ACTION = "update";

  // Recognized Reporter Manager protocol commands.

  /**
   * Command: PACKAGE SP uri CRLF
   *          FILENAME SP name CRLF
   *          VERSION SP version CRLF
   *          INSTALLPATH SP path CRLF
   *          [PERMISSION SP oct CRLF]
   *          CONTENT SP packageContent CRLF
   * Response: OK CRLF
   * Send a reporter module or tar.gz package to the reporter manager.
   */
  public static final String PACKAGE_COMMAND = "PACKAGE";

  // Recognized Depot protocol commands

  /**
   * Command: [REPORT|RESEND] SP resource SP context CRLF
   *          [STDERR SP text CRLF]
   *          STDOUT SP xml CRLF
   *          SYSUSAGE SP usage CRLF
   * Response: OK SP uri.
   * A multi-part command that instructs the depot to insert a new report into
   * the database.  The resource and context designates the series, and the two
   * texts are saved in the database without interpretation.  See the Report
   * schema for the xml format.  usage is a newline-separated set of
   * attribute=value pairs; the recognized attributes, in order, are cpu_secs,
   * wall_secs, and memory_mb.
   * Normally the Depot assumes that reports arrive in order, so that a newly-
   * arrived report was collected later than any prior ones.  The RESEND
   * message indicates that this assumption may not be correct for the enclosed
   * report; it may have been collected before previously-submitted reports.
   */
  public static final String INSERT_COMMAND = "REPORT";
  public static final String RESEND_COMMAND = "RESEND";
  public static final String NOTIFY_INSERT_COMMAND = "NOTIFYREPORT";

  /**
   * Command: QUERYDB
   * Response: OK xml.
   * Requests information about the structure of the Depot database.
   */
  public static final String QUERY_DB_COMMAND = "QUERYDB";

  /**
   * Command: QUERYGUIDS CRLF
   * Response: OK guids.
   * Requests a database lookup for the set of suite guids in the DB.  Returns
   * a newline-delimited list of guids.
   */
  public static final String QUERY_GUIDS_COMMAND = "QUERYGUIDS";

  /**
   * Command: QUERYHQL SP hql CRLF
   * Response: [QUERYRESULT SP xml CRLF etc]
   *           QUERYEND CRLF.
   * Requests a database lookup using the enclosed HQL SELECT statement.  The
   * format of any returned XML will depend on the tables queried.  This
   * command exists primarily as a Depot debugging aid, since its use depends
   * on knowledge of the internal Depot DB schema.
   */
  public static final String QUERY_HQL_COMMAND = "QUERYHQL";

  /**
   * Command: QUERYINSTANCE SP instance SP config CRLF
   * Response: QUERYRESULT SP xml CRLF
   *           QUERYEND CRLF.
   * Requests a database lookup of a specified (numeric) instance id for a
   * specified (numeric) series config id.  See the ReportDetails schema for
   * the xml format.
   */
  public static final String QUERY_INSTANCE_COMMAND = "QUERYINSTANCE";

  /**
   * Command: QUERYLATEST SP expr CRLF
   * Response: QUERYRESULT SP xml CRLF
   *           QUERYEND CRLF.
   * Requests a database lookup of the latest instances for a set of series
   * specified by an HQL WHERE clause expression.  See the ReportSummary schema
   * for the xml format.
   */
  public static final String QUERY_LATEST_COMMAND = "QUERYLATEST";

  /**
   * Command: QUERYPERIOD SP begin SP end SP expr CRLF
   * Response: QUERYRESULT SP xml CRLF
   *           QUERYEND CRLF.
   * Requests a database lookup of all instances that fall within a specified
   * time period for a set of series specified by an HQL WHERE clause
   * expression.  See the GraphSeries schema for the xml format.
   */
  public static final String QUERY_PERIOD_COMMAND = "QUERYPERIOD";

  /**
   * Command: QUERYSQL SP sql CRLF
   * Response: [QUERYRESULT SP xml CRLF etc]
   *           QUERYEND CRLF.
   * Requests a database lookup using the enclosed SQL SELECT statement.  The
   * format of any returned XML will depend on the tables queried.  This
   * command exists primarily as a Depot debugging aid, since its use depends
   * on knowledge of the internal Depot DB schema.
   */
  public static final String QUERY_SQL_COMMAND = "QUERYSQL";

  /**
   * Command: QUERYSTATUS SP period SP begin SP end SP expr CRLF
   * Response: QUERYRESULT SP xml CRLF
   *           QUERYEND CRLF.
   * Requests a database lookup of the success/failure history for a set of
   * series specified by an HQL WHERE clause expression.  The period is either a
   * number of minutes to use for grouping in the response or one of the words
   * "DAY", "WEEK", "MONTH", or "QUARTER".  The begin and end values limit the
   * returned information to instances collected in between the specified
   * timestamp values.  See the StatusHistory schema for the xml format.
   */
  public static final String QUERY_STATUS_COMMAND = "QUERYSTATUS";

  /**
   * Command: QUERYUSAGE SP begin SP end SP expr CRLF
   * Response: QUERYRESULT sp xml CRLF
   *           QUERYEND CRLF.
   * Requests a database lookup of the resource usage history for a set of
   * series specified by an HQL WHERE clause expression.  The begin and end
   * values limit the returned information to instances collected in between
   * the specified timestamp values.  See the GraphEvent schema for the xml
   * format.
   */
   public static final String QUERY_USAGE_COMMAND = "QUERYUSAGE";

  /**
   * Command: SUITE SP xml CRLF
   * Response: OK SP version CRLF.
   * Applies the specified series config add/delete actions.  See the Suite
   * schema for the xml format.
   */
  public static final String SUITE_UPDATE_COMMAND = "SUITE";
  public static final String NOTIFY_SUITE_UPDATE_COMMAND = "NOTIFYSUITE";

  /**
   * Command: SYNC SP uri CRLF
   * Response: OK SP xml CRLF
   *
   */
  public static final String SYNC_COMMAND = "SYNC";

  /**
   * Command: SYNCSTART CRLF
   * Response: OK CRLF
   *
   */
  public static final String SYNC_START_COMMAND = "SYNCSTART";

  /**
   * Command: SYNCEND SP uri CRLF
   * Response: OK CRLF
   *
   */
  public static final String SYNC_END_COMMAND = "SYNCEND";

  /**
   * Actions for SeriesConfigs in SUITE_UPDATE_COMMAND.
   */
  public static final String SERIES_CONFIG_ADD = "add";
  public static final String SERIES_CONFIG_DELETE = "delete";

  // Depot INSERT command "subcommands"
  public static final String INSERT_STDERR_COMMAND = "STDERR";
  public static final String INSERT_STDOUT_COMMAND = "STDOUT";
  public static final String INSERT_SYSUSAGE_COMMAND = "SYSUSAGE";

  // Manager PACKAGE command "subcommands"
  public static final String PACKAGE_FILENAME_COMMAND = "FILENAME";
  public static final String PACKAGE_VERSION_COMMAND = "VERSION";
  public static final String PACKAGE_INSTALLPATH_COMMAND = "INSTALLPATH";
  public static final String PACKAGE_PERMISSIONS_COMMAND = "PERMISSION";
  public static final String PACKAGE_DEPENDENCIES_COMMAND = "DEPENDENCIES";
  public static final String PACKAGE_CONTENT_COMMAND = "CONTENT";

  // Manager GET_PROXY_RENEW_INFO command "subcommands"
  public static final String PROXY_DN_COMMAND = "DN";
  public static final String PROXY_HOSTNAME_COMMAND = "HOSTNAME";
  public static final String PROXY_LIFETIME_COMMAND = "LIFETIME";
  public static final String PROXY_PASSWORD_COMMAND = "PASSWORD";
  public static final String PROXY_PORT_COMMAND = "PORT";
  public static final String PROXY_USERNAME_COMMAND = "USERNAME";

  // End-of-output "command" sent by Depot
  public static final String END_QUERY_RESULTS_COMMAND = "QUERYEND";

}
