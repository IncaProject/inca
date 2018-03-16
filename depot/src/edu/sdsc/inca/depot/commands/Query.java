/*
 * Query.java
 */
package edu.sdsc.inca.depot.commands;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.sdsc.inca.dataModel.util.Tags;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import edu.sdsc.inca.Depot;
import edu.sdsc.inca.dataModel.graphSeries.GraphInstance;
import edu.sdsc.inca.dataModel.graphSeries.GraphSeries;
import edu.sdsc.inca.dataModel.reportDetails.ReportDetailsDocument;
import edu.sdsc.inca.dataModel.util.AnyXmlSequence;
import edu.sdsc.inca.dataModel.util.Log;
import edu.sdsc.inca.dataModel.util.ReportDetails;
import edu.sdsc.inca.depot.persistent.ComparisonResult;
import edu.sdsc.inca.depot.persistent.ComparisonResultDAO;
import edu.sdsc.inca.depot.persistent.ConnectionSource;
import edu.sdsc.inca.depot.persistent.DAO;
import edu.sdsc.inca.depot.persistent.HibernateUtil;
import edu.sdsc.inca.depot.persistent.InstanceInfo;
import edu.sdsc.inca.depot.persistent.Notification;
import edu.sdsc.inca.depot.persistent.PersistenceException;
import edu.sdsc.inca.depot.persistent.PersistentObject;
import edu.sdsc.inca.depot.persistent.Report;
import edu.sdsc.inca.depot.persistent.ReportDAO;
import edu.sdsc.inca.depot.persistent.Schedule;
import edu.sdsc.inca.depot.persistent.Series;
import edu.sdsc.inca.depot.persistent.SeriesConfig;
import edu.sdsc.inca.depot.persistent.SeriesConfigDAO;
import edu.sdsc.inca.depot.persistent.Suite;
import edu.sdsc.inca.depot.util.HibernateMessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.queryResult.ReportSummaryDocument;
import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.util.XmlWrapper;


/**
 *
 */
public class Query extends HibernateMessageHandler {


  /**
   *
   */
  private interface InstanceProcessor {

    void process(InstanceInfo instance, Map<Long, Report> reports) throws Exception;
  }

  /**
   *
   */
  private static class PeriodProcessor implements InstanceProcessor {

    private Series series;
    private SeriesConfig config;
    private Map<Long, String> comparisons;
    private Map<Date, GraphSeries> result;


    /**
     *
     * @param s
     * @param sc
     * @param c
     * @param r
     */
    public PeriodProcessor(Series s, SeriesConfig sc, Map<Long, String> c, Map<Date, GraphSeries> r) {

      series = s;
      config = sc;
      comparisons = c;
      result = r;
    }


    /**
     *
     * @param instance
     * @param reports
     * @throws Exception
     */
    public void process(InstanceInfo instance, Map<Long, Report> reports) throws Exception {

      Long reportId = instance.getReportId();
      Date collected = instance.getCollected();
      Report r = reports.get(reportId);

      if (r == null) {
        r = ReportDAO.load(reportId);

        reports.put(reportId, r);
      }

      String cr = comparisons.get(reportId);
      GraphSeries gs = GraphSeries.Factory.newInstance();
      GraphInstance gi = gs.addNewObject();

      gi.setResource(series.getResource());
      gi.setTargetHostname(series.getTargetHostname());
      gi.setNickname(config.getNickname());
      gi.setInstanceId(instance.getId().toString());
      gi.setReportId(reportId.toString());
      gi.setConfigId(config.getId().toString());
      gi.setBody( AnyXmlSequence.Factory.parse(r.getBody()) );

      Calendar cal = Calendar.getInstance();

      cal.setTime(collected);
      gi.setCollected(cal);

      String message = r.getExit_message();
      boolean success = cr == null || cr.equals("\t") ? r.getExit_status() : cr.matches("^Success");

      gi.setExitStatus(success ? "Success" : "Failure");
      gi.setExitMessage(message == null || message.equals("\t") ? "" : message);
      gi.setComparisonResult(cr == null ? "" : cr);

      result.put(collected, gs);
    }
  }

  /**
   *
   */
  private static class StatusProcessor implements InstanceProcessor {

    private int limitError;
    private Map<Long, String> comparisons;
    private List<StatusCount> counts;


    /**
     *
     * @param limit
     * @param cmps
     * @param cnts
     */
    public StatusProcessor(int limit, Map<Long, String> cmps, List<StatusCount> cnts) {

      limitError = limit;
      comparisons = cmps;
      counts = cnts;
    }


    /**
     *
     * @param instance
     * @param reports
     * @throws Exception
     */
    public void process(InstanceInfo instance, Map<Long, Report> reports) throws Exception {

      Long reportId = instance.getReportId();
      Date collected = instance.getCollected();
      Report r = reports.get(reportId);

      if (r == null) {
        r = ReportDAO.load(reportId);

        reports.put(reportId, r);
      }

      String message = r.getExit_message();
      String comparison = comparisons.get(reportId);

      if ((message == null || message.equals(PersistentObject.DB_EMPTY_STRING)) && comparison != null && !comparison.equals("Success"))
        message = comparison;
      else if (message != null && comparison != null && comparison.equals("Success"))
        message = comparison;

      if (message != null && limitError > 0 && message.length() >= limitError)
        message = message.substring(0, limitError);

      for (StatusCount status : counts) {
        if (collected.getTime() < status.lowBound || collected.getTime() > status.highBound)
          continue;

        Long count = status.messageCounts.get(message);

        count = (count == null ? 0 : count) + 1;

        status.messageCounts.put(message, count);
      }
    }
  }

  /**
   *
   */
  private static class StatusCount {

    public long lowBound;
    public long highBound;
    public Map<String, Long> messageCounts;


    /**
     *
     * @param low
     * @param high
     */
    public StatusCount(long low, long high) {

      lowBound = low;
      highBound = high;
      messageCounts = new HashMap<String, Long>();
    }
  }

  /**
   *
   */
  private static class ConfigData {

    public String nickname;
    public String seriesName;
    public String suiteName;
    public String seriesUri;
    public String instanceTable;
    public String notification;
    public Timestamp deployed;
    public Timestamp lastRun;
    public final List<Long> instanceIds = new ArrayList<Long>();
    public final Set<String> frequencies = new TreeSet<String>();
    public final Set<String> resources = new TreeSet<String>();
    public final Set<String> targets = new TreeSet<String>();
  };


  private static Logger logger = Logger.getLogger(Query.class);
  private static final int FETCH_SIZE = 100;
  private static final Statement S_FINISH =
    new Statement(Protocol.END_QUERY_RESULTS_COMMAND.toCharArray(), null);

  /**
   * Execute queries on the depot.
   *
   * @param reader   Reader to the query request.
   * @param writer   Writer to the remote process making the request.
   * @throws Exception
   */
  public void executeHibernateAction(
    ProtocolReader reader, ProtocolWriter writer, String dn) throws Exception {

    if (Depot.getRunningDepot().requestingSync() && Depot.getRunningDepot().workQueueIsLocked())
      throw new ProtocolException("synchronizing");

    Statement queryStatement = reader.readStatement();
    String cmd = new String(queryStatement.getCmd());
    String data = new String(queryStatement.getData());
    StopWatch timer = new Log4JStopWatch(logger);
    try {
      if(cmd.equals(Protocol.QUERY_DB_COMMAND)) {
        getDbInfo(writer);
      } else if(cmd.equals(Protocol.QUERY_GUIDS_COMMAND)) {
        getGuidList(writer);
      } else if(cmd.equals(Protocol.QUERY_HQL_COMMAND)) {
        getSelectOutput(writer, data, true);
        writer.write(S_FINISH);
      } else if(cmd.equals(Protocol.QUERY_INSTANCE_COMMAND)) {
        getInstance(writer, data);
        writer.write(S_FINISH);
      } else if(cmd.equals(Protocol.QUERY_INSTANCE_BY_ID_COMMAND)) {
        getInstanceById(writer, data);
        writer.write(S_FINISH);
      } else if(cmd.equals(Protocol.QUERY_LATEST_COMMAND)) {
        getLatestInstances(writer, data);
        writer.write(S_FINISH);
      } else if(cmd.equals(Protocol.QUERY_PERIOD_COMMAND)) {
        getPeriodInstances(writer, data);
        writer.write(S_FINISH);
      } else if(cmd.equals(Protocol.QUERY_SQL_COMMAND)) {
        getSelectOutput(writer, data, false);
        writer.write(S_FINISH);
      } else if(cmd.equals(Protocol.QUERY_STATUS_COMMAND)) {
        getStatusHistory(writer, data);
        writer.write(S_FINISH);
      } else if (cmd.equals(Protocol.QUERY_DEPOT_PEERS_COMMAND)) {
        getDepotPeerUris(writer);
      } else if (cmd.equals(Protocol.QUERY_REPORTERS_COMMAND)) {
        getReporterSeries(writer);
      } else if (cmd.equals(Protocol.QUERY_REPORTERS_DETAIL_COMMAND)) {
        getReporterSeriesDetail(writer);
      }

      timer.stop( cmd );
    } catch(Exception e) {
      e.printStackTrace();
      throw e;
    }

  }

  /**
   * Return XML that represents the structure of the database.
   *
   * @param writer Writer to the remote process making the request
   *
   * @throws Exception if trouble querying the database
   */
  private void getDbInfo(ProtocolWriter writer) throws Exception {
    StringBuffer response = new StringBuffer("<incadb>\n");
    Map<?, ?> metadata = HibernateUtil.getSessionFactory().getAllClassMetadata();
    for( Object obj : metadata.keySet() ) {
      String name = (String)obj;
      response.append("  <dbclass>\n    <name>");
      response.append(name.replaceAll(".*\\.", ""));
      response.append("</name>\n");
      String[] properties =
        ((ClassMetadata)metadata.get(name)).getPropertyNames();
      for(String field : properties ) {
        // For client convenience, lie about how the Report body is stored
        if(field.equals("bodypart1")) {
          field = "body";
        } else if(field.equals("bodypart2") || field.equals("bodypart3")) {
          continue;
        }
        response.append("    <field>\n      <name>");
        response.append(field);
        response.append("</name>\n    </field>\n");
      }
      response.append("  </dbclass>\n");
    }
    response.append("</incadb>");
    writer.write(Statement.getOkStatement(response.toString()));
  }

  /**
   * Return a list of the Suite guids in the database.
   *
   * @param writer Writer to the remote process making the request
   * @throws Exception if trouble querying the database or writing result
   */
  private void getGuidList(ProtocolWriter writer) throws Exception {
    Iterator<?> guids = DAO.selectMultiple("select s.guid from Suite as s", null);
    List<String> guidList = new ArrayList<String>();

    while (guids.hasNext())
      guidList.add((String)guids.next());

    String[] result = guidList.toArray(new String[guidList.size()]);

    writer.write(Statement.getOkStatement(StringMethods.join("\n", result)));
  }

  /**
   * Return a report details document to the client for the given series config
   * nickname, series resource, and instance collection time
   *
   * @param writer writer to the remote process making the request.
   * @param request the instance query request
   * @throws Exception if trouble querying the database or writing result
   */
  private void getInstance(ProtocolWriter writer, String request) throws Exception {

    String[] pieces = request.split("\n");

    if(pieces.length != 4)
      throw new ProtocolException("Expected 'nickname resource target collected', got '" + request + "'");

    String nickname = pieces[0];
    String resource = pieces[1];
    String target = pieces[2];
    long milliSeconds;

    try {
      milliSeconds = Long.parseLong(pieces[3]);
    }
    catch (NumberFormatException formatErr) {
      throw new ProtocolException("Non-numeric value '" + pieces[3] + "' given for collected");
    }

    Date collected = new Date(milliSeconds);
    InstanceInfo ii = new InstanceInfo(nickname, resource, target, collected);
    Report r = ReportDAO.load(ii.getReportId());
    Set<SeriesConfig> scs = ii.getSeriesConfigs();
    SeriesConfig sc = null;

    for( SeriesConfig config : scs ) {
      if ( config.getSchedule().getType().equals("cron") ) {
        sc = config;
      }
    }

    if ( sc == null ) sc = scs.iterator().next();

    String query = "select cr from ComparisonResult cr " +
                   "where cr.reportId = :p0 " +
                     "and cr.seriesConfigId = :p1";

    Object[] params = { r.getId(), sc.getId() };
    ComparisonResult cr = (ComparisonResult)DAO.selectUnique(query, params);
    ReportDetailsDocument rdd = toBean(sc, r, ii, cr);

    writer.write(new Statement(Protocol.QUERY_RESULT, rdd.toString()));
  }

  /**
   * Return a report details document to the client for the given report
   * instance and configuration.
   *
   * @param writer  Writer to the remote process making the request.
   * @param request The instance query request which contains the
   * instance id, followed by a space, and then the report series configuration
   * id.
   *
   * @throws Exception if trouble querying the database or writing result
   */
  private void getInstanceById(ProtocolWriter writer, String request)
    throws Exception {
    String[] pieces = request.split(" ");
    Long instanceId, configId;
    if(pieces.length != 2) {
      throw new ProtocolException
        ("Expected 'instanceId configId', got '" + request + "'");
    }
    try {
      instanceId = Long.valueOf(pieces[0]);
      configId = Long.valueOf(pieces[1]);
    } catch(NumberFormatException e) {
      throw new ProtocolException("Non-numeric id in '" + request + "'");
    }
    SeriesConfig sc = SeriesConfigDAO.load(configId);

    if(sc == null)
      throw new PersistenceException("Request for unknown config " + configId);

    InstanceInfo ii = new InstanceInfo(sc.getSeries(), instanceId);
    Report r = ReportDAO.load(ii.getReportId());
    String query = "select cr from ComparisonResult cr " +
                   "  where cr.reportId = " + r.getId() +
                   "  and cr.seriesConfigId = " + configId;
    ComparisonResult cr = (ComparisonResult)DAO.selectUnique(query, null);
    ReportDetailsDocument rdd = toBean(sc, r, ii, cr);
    String response = rdd.toString();
    writer.write(new Statement(Protocol.QUERY_RESULT, response));
  }

  /**
   * Return the output from an HQL/SQL query.
   *
   * @param writer  writer to the remote process making the request.
   * @param select the HQL/SQL select statement
   * @param isHql indicates whether the select is HQL or SQL
   * @throws Exception if trouble running query
   */
  private void getSelectOutput
    (ProtocolWriter writer, String select, boolean isHql) throws Exception {
    // Allow only query HQL/SQL, to avoid a client modifying tables
    if(!select.matches("(?i)^\\s*(SELECT|FROM)\\s.*$")) {
      throw new PersistenceException("Not a SELECT: " + select);
    }
    // Parse the column names from the select statement so that we can use them
    // as XML tags in the reply.  A token is a quoted literal, a name/number, a
    // paren, comma, or operator.  Take care not to treat comma in a function
    // invocation (e.g., concat(a,b) as terminating a column name.
    ArrayList<String> names = new ArrayList<String>();
    Matcher m = Pattern.compile
      ("('[^']*'|\"[^\"]\"|[\\w\\.\\-]+|[\\(\\),]|[^\\w\\(\\),]+)\\s*").
      matcher(select.replaceFirst("(?i)^SELECT\\s+(DISTINCT\\s+)?", ""));
    int parenDepth = 0;
    String name = "";
    while(true) {
      m.find();
      String token = m.group(1);
      if(parenDepth == 0 && token.matches("(?i)as")) {
        name = ""; // Use the subsequent AS name as the entire name
      } else if(parenDepth == 0 && (token.matches("(?i),|FROM"))) {
        name = name.replaceAll("[^\\w\\.\\-]", "_");
        names.add(name);
        name = "";
        if(!token.equals(",")) {
          break;
        }
      } else {
        name += token;
        if(token.equals("(")) {
          parenDepth++;
        } else if(token.equals(")")) {
          parenDepth--;
        }
      }
    }
    // For client convenience, automatic translate references to report body
    // into bodypart1||bodypart2||bodypart3
    select = select.replaceFirst("\\b[aA][sS]\\s+body\\b", "as xBODYx");
    while(true) {
      m = (Pattern.compile("\\b((\\w+)\\.)?body\\b")).matcher(select);
      if(!m.find()) {
        break;
      }
      String prefix = m.group(1) == null ? "" : m.group(1);
      select = select.substring(0, m.start()) +
               prefix + "bodypart1 || " +
               prefix + "bodypart2 || " +
               prefix + "bodypart3" +
               select.substring(m.end());
    }
    select = select.replaceFirst("xBODYx", "body");
    // Make the query
    Session session = HibernateUtil.getCurrentSession();
    List<?> l;
    try {
      l = isHql ? session.createQuery(select).list() :
                  session.createSQLQuery(select).list();
    } catch(Exception e) {
      e.printStackTrace();
      throw new PersistenceException(e.toString());
    }
    // Send one response per row.  If the row is a PersistentObject (e.g.,
    // "select sc from SeriesConfig sc", then we can use its toXml() method to
    // translate it; otherwise, we'll have an object array which we'll
    // translate to XML using the parsed names above.
    for( Object o : l ) {
      String xml;
      if(o instanceof PersistentObject) {
        xml = ((PersistentObject)o).toXml();
      } else {
        StringBuffer sb = new StringBuffer();
        Object[] values;
        try {
          values = (Object [])o;
        } catch(Exception e) {
          values = new Object[] {o};
        }
        sb.append("<object>\n");
        for(int j = 0; j < values.length; j++) {
          name = names.get(j) == null ? j + "" : names.get(j);
          Object value = values[j];
          String s = value == null ? "null" :
            value instanceof PersistentObject?((PersistentObject)value).toXml():
            XmlWrapper.escape( value.toString() );
          sb.append("<");
          sb.append(name);
          sb.append(">");
          sb.append(s);
          sb.append("</");
          sb.append(name);
          sb.append(">\n");
        }
        sb.append("</object>");
        xml = sb.toString();
      }
      writer.write(new Statement(Protocol.QUERY_RESULT, xml));
    }
  }

  /**
   * Return the latest report summaries for all series selected by an HQL
   * WHERE clause expression.
   *
   * @param writer  Writer to the remote process making the request.
   * @param expr An HQL WHERE clause expression specifying the series of
   *             interest
   * @throws Exception if trouble running query
   */
  private void getLatestInstances(ProtocolWriter writer, String expr) throws Exception {

    Iterator<?> scList = getSelectedConfigs(expr);
    Statement reply = new Statement(Protocol.QUERY_RESULT, null);

    while (scList.hasNext()) {
      SeriesConfig sc = (SeriesConfig)scList.next();
      ComparisonResult cr = null;
      InstanceInfo ii = null;
      Report r = null;
      Long id = sc.getLatestInstanceId();

      if(id != null && id >= 0) {
        ii = new InstanceInfo(sc.getSeries(), id);
        id = ii.getReportId();

        if (id != null && id >= 0)
          r = ReportDAO.load(id);
      }

      id = sc.getLatestComparisonId();

      if (id != null && id >= 0)
        cr = ComparisonResultDAO.load(id);

      if (ii == null)
        logger.debug("No latest instance for SC " + sc);

      if (r == null)
        logger.debug("No latest report for SC " + sc);

      if (cr == null)
        logger.debug("No latest comparison for SC " + sc);

      ReportSummaryDocument rsd = toBean(sc, cr, ii, r);
      String s = rsd.toString();

      reply.setData(s.toCharArray());
      writer.write(reply);
    }
  }

  /**
   * Return the a set of GraphInstances for all series selected by an HQL
   * WHERE clause expression.
   *
   * @param writer  Writer to the remote process making the request.
   * @param request a space-separated string specifying the beginning and
   *        ending timestamps and an HQL WHERE clause expression that selects
   *        the desired series.
   * @throws Exception if trouble querying database
   */
  private void getPeriodInstances(ProtocolWriter writer, String request)
    throws Exception {

    // Parse the request
    String[] pieces = request.split(" ", 3);
    if(pieces.length != 3) {
      throw new ProtocolException
        ("Expected 'begin end expr', got '" + request + "'");
    }
    Date begin, end;
    if(pieces[0].matches("^\\d+$")) {
      begin = new Date(Long.parseLong(pieces[0]));
    } else {
      throw new ProtocolException("Bad value '" + pieces[0] + "' for begin");
    }
    if(pieces[1].matches("^\\d+$")) {
      end = new Date(Long.parseLong(pieces[1]));
    } else {
      throw new ProtocolException("Bad value '" + pieces[1] + "' for end");
    }

    Iterator<?> seriesList = getSelectedSeries(pieces[2], false);
    Map<Date, GraphSeries> result = new TreeMap<Date, GraphSeries>();

    while (seriesList.hasNext()) {
      Series s = (Series)seriesList.next();
      SeriesConfig[] scSorted = new SeriesConfig[s.getSeriesConfigs().size()];
      scSorted = s.getSeriesConfigs().toArray(scSorted);
      Arrays.sort( scSorted );

      for (SeriesConfig sc : scSorted ) {
        if ( ! sc.getSchedule().getType().equals("cron") ) continue;

        Map<Long, String> comparisons = getRelatedComparisons(sc.getId());

        processRelatedInstances(sc, begin, end, new PeriodProcessor(s, sc, comparisons, result));
      }
    }

    if (result.isEmpty())
      return;

    Statement reply = new Statement(Protocol.QUERY_RESULT, null);

    for (GraphSeries gs : result.values()) {
      reply.setData(gs.xmlText().toCharArray());
      writer.write(reply);
    }
  }

  /**
   * Return the success/failure counts over a given period for all series in a
   * given newline-delimited list of suites and/or series.
   *
   * @param writer  Writer to the remote process making the request.
   * @param request a space-separated string specifying the period length,
   *   beginning and ending timestamps, and an HQL WHERE clause expression
   *   that selects the desired series.
   * @throws Exception if trouble retrieving results
   *
   */
  private void getStatusHistory(ProtocolWriter writer, String request)
    throws Exception {

    // Parse the request
    String[] pieces = request.split(" ", 4);
    if(pieces.length != 4) {
      throw new ProtocolException
        ("Expected 'period begin end expr', got '" + request + "'");
    }
    long periodInMillis;
    Date begin, end;
    String [] temp = pieces[0].split(":");
    Integer limitError = 0;
    if (temp.length == 2) {
      pieces[0] = temp[0];
      if (temp[1].matches("^\\d+$")) {
        limitError = Integer.parseInt(temp[1]);
      }
    }
    if(pieces[0].equals("DAY")) {
      periodInMillis = 24L * 60L * 60L * 1000L;
    } else if(pieces[0].equals("WEEK")) {
      periodInMillis = 7L * 24L * 60L * 60L * 1000L;
    } else if(pieces[0].equals("MONTH")) {
      periodInMillis = 30L * 24L * 60L * 60L * 1000L;
    } else if(pieces[0].equals("QUARTER")) {
      periodInMillis = 90L * 24L * 60L * 60L * 1000L;
    } else if(pieces[0].matches("^\\d+$")) {
      periodInMillis = Long.parseLong(pieces[0]) * 60L * 1000L;
    } else {
      throw new ProtocolException("Bad value '" + pieces[0] + "' for period");
    }
    if(pieces[1].matches("^\\d+$")) {
      begin = new Date(Long.parseLong(pieces[1]));
    } else {
      throw new ProtocolException("Bad value '" + pieces[1] + "' for begin");
    }
    if(pieces[2].matches("^\\d+$")) {
      end = new Date(Long.parseLong(pieces[2]));
    } else {
      throw new ProtocolException("Bad value '" + pieces[2] + "' for end");
    }
    Iterator<?> seriesList = getSelectedSeries(pieces[3], true);
    Statement reply = new Statement(Protocol.QUERY_RESULT, null);;

    while (seriesList.hasNext()) {
      Series series = (Series)seriesList.next();

      for (SeriesConfig sc : series.getSeriesConfigs()) {
        Map<Long, String> comparisons = getRelatedComparisons(sc.getId());

        StringBuilder xml = new StringBuilder();
        xml.append("<series>\n");

        if ( sc.getSuites().size() == 1 &&
          sc.getSuite(0).getName().equals(Protocol.IMMEDIATE_SUITE_NAME)) {
          break;
        }
        for (Suite s : sc.getSuites()) {
          if ( ! s.getName().equals(Protocol.IMMEDIATE_SUITE_NAME) ) {
            xml.append("  <guid>");
            xml.append(s.getGuid());
            xml.append("</guid>\n");
          }
        }

        xml.append("  <nickname>");
        xml.append(sc.getNickname());
        xml.append("</nickname>\n");
        xml.append("  <resource>");
        xml.append(series.getResource());
        xml.append("</resource>\n");
        xml.append("  <targetHostname>");
        xml.append(series.getTargetHostname());
        xml.append("</targetHostname>\n");

        List<StatusCount> counts = new ArrayList<StatusCount>();

        // For each period, from least recent to most recent ...
        for (long lowBound = begin.getTime() ; lowBound <= end.getTime() ; lowBound += periodInMillis) {
          long highBound = lowBound + periodInMillis - 1;

          counts.add(new StatusCount(lowBound, highBound));
        }

        processRelatedInstances(sc, begin, end, new StatusProcessor(limitError, comparisons, counts));

        for (StatusCount status : counts) {
          // Add XML for the successes and failures in this period
          xml.append("  <period>\n");
          xml.append("    <begin>");
          xml.append(status.lowBound);
          xml.append("</begin>\n");
          xml.append("    <end>");
          xml.append(status.highBound);
          xml.append("</end>\n");

          long successes = 0;
          Long count = status.messageCounts.get("Success");

          if (count != null) {
            status.messageCounts.remove("Success");
            successes += count;
          }

          count = status.messageCounts.get(PersistentObject.DB_EMPTY_STRING);

          if (count != null) {
            status.messageCounts.remove(PersistentObject.DB_EMPTY_STRING);
            successes += count;
          }

          xml.append("    <success>");
          xml.append(successes);
          xml.append("</success>\n");

          for (String messageKey : status.messageCounts.keySet()) {
            xml.append("    <failure><message>");
            xml.append(XmlWrapper.escape(messageKey));
            xml.append("</message><count>");
            xml.append(status.messageCounts.get(messageKey));
            xml.append("</count></failure>\n");
          }

          xml.append("  </period>\n" );
        }

        xml.append("</series>");

        reply.setData(xml.toString().toCharArray());
        writer.write(reply);
      }
    }
  }

  /**
   * Returns the URIs of the Depot's peers
   *
   * @param writer Writer to the remote process making the request
   * @throws IOException
   */
  private void getDepotPeerUris(ProtocolWriter writer) throws IOException {

    List<Properties> configs = Depot.getRunningDepot().getPeerConfigs();
    StringBuilder result = new StringBuilder();

    if (!configs.isEmpty()) {
      Iterator<Properties> uris = configs.iterator();

      result.append(uris.next().getProperty("peer"));

      while (uris.hasNext()) {
        result.append('\n');
        result.append(uris.next().getProperty("peer"));
      }
    }

    writer.write(Statement.getOkStatement(result.toString()));
  }

  /**
   *
   * @param writer
   * @throws Exception
   */
  private void getReporterSeries(ProtocolWriter writer) throws Exception {

    Pattern targetPattern = Pattern.compile("(.+)_to_.+");
    Map<String, Timestamp> activatedDates = new TreeMap<String, Timestamp>();
    Iterator<?> queryResult = DAO.selectMultiple(
        "SELECT seriesConfig.nickname AS nickname, seriesConfig.series.reporter AS seriesName, " +
          "MIN(seriesConfig.activated) AS minActivated " +
        "FROM SeriesConfig seriesConfig INNER JOIN seriesConfig.suites suite " +
        "WHERE suite.name != '_runNow' " +
        "GROUP BY seriesConfig.nickname, seriesConfig.series.reporter",
        null
    );

    while (queryResult.hasNext()) {
      Object[] row = (Object[])queryResult.next();
      String nickname = (String)row[0];
      String seriesName = (String)row[1];
      Timestamp minActivated = (Timestamp)row[2];
      Matcher targetMatcher = targetPattern.matcher(nickname);

      if (targetMatcher.matches())
        nickname = targetMatcher.group(1);

      String configKey = nickname + "/" + seriesName;
      Date activated = activatedDates.get(configKey);

      if (activated == null || activated.after(minActivated))
        activatedDates.put(configKey, minActivated);
    }

    Map<String, ConfigData> seriesConfigs = new TreeMap<String, ConfigData>();

    queryResult = DAO.selectMultiple(
        "SELECT seriesConfig, suite.name AS suiteName " +
        "FROM SeriesConfig seriesConfig INNER JOIN seriesConfig.suites suite " +
        "WHERE seriesConfig.deactivated IS NULL AND suite.name != '_runNow'",
        null
    );

    while (queryResult.hasNext()) {
      Object[] row = (Object[])queryResult.next();
      SeriesConfig config = (SeriesConfig)row[0];
      Series configSeries = config.getSeries();
      String nickname = config.getNickname();
      String seriesName = config.getSeries().getReporter();
      String targetHostname = configSeries.getTargetHostname().trim();
      String frequency = buildFrequency(config.getSchedule());
      if ( frequency == null ) {
        Schedule s = config.getSchedule();
        StringBuilder f = new StringBuilder();
        f.append(s.getMinute());
        f.append(" ");
        f.append(s.getHour());
        f.append(" ");
        f.append(s.getMday());
        f.append(" ");
        f.append(s.getWday());
        f.append(" ");
        f.append(s.getMonth());
        frequency = f.toString();
      }

      Matcher targetMatcher = targetPattern.matcher(nickname);

      if (targetMatcher.matches())
        nickname = targetMatcher.group(1);

      String configKey = nickname + "/" + seriesName;
      ConfigData data = seriesConfigs.get(configKey);

      if (data == null) {
        Timestamp deployed = activatedDates.get(configKey);

        data = new ConfigData();

        data.nickname = nickname;
        data.seriesName = seriesName;
        data.suiteName = (String)row[1];
        data.seriesUri = configSeries.getUri();
        data.instanceTable = configSeries.getInstanceTableName();
        data.deployed = deployed;
        data.lastRun = new Timestamp(1);

        if (config.getAcceptedOutput() != null)
          data.notification = "no";
        else
          data.notification = "yes";

        seriesConfigs.put(configKey, data);
      }

      data.frequencies.add(frequency);
      data.instanceIds.add(config.getLatestInstanceId());
      data.resources.add(configSeries.getResource());

      if (targetHostname.length() > 0)
        data.targets.add(targetHostname);
    }

    DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    writer.write(Protocol.SUCCESS_COMMAND);
    writer.write(" <configs>");

    try {
      for (ConfigData data : seriesConfigs.values()) {
        Iterator<Long> ids = data.instanceIds.iterator();
        StringBuilder idList = new StringBuilder();

        idList.append(ids.next());

        while (ids.hasNext()) {
          idList.append(", ");
          idList.append(ids.next());
        }

        List<Timestamp> dates = getCollectedDates(data.instanceTable, idList.toString());

        for (Timestamp collected : dates) {
          if (collected.after(data.lastRun))
            data.lastRun = collected;
        }

        StringBuilder resources = new StringBuilder();

        if (!data.resources.isEmpty()) {
          Iterator<String> resourceNames = data.resources.iterator();

          resources.append(resourceNames.next());

          while (resourceNames.hasNext()) {
            resources.append(", ");
            resources.append(resourceNames.next());
          }
        }

        StringBuilder targets = new StringBuilder();

        if (!data.targets.isEmpty()) {
          Iterator<String> targetNames = data.targets.iterator();

          targets.append(targetNames.next());

          while (targetNames.hasNext()) {
            targets.append(", ");
            targets.append(targetNames.next());
          }
        }

        StringBuilder frequencies = new StringBuilder();

        if (!data.frequencies.isEmpty()) {
          Iterator<String> elements = data.frequencies.iterator();

          frequencies.append(elements.next());

          while (elements.hasNext()) {
            frequencies.append(", ");
            frequencies.append(elements.next());
          }
        }

        writer.write("<config><nickname>");
        writer.write(data.nickname);
        writer.write("</nickname><resources>");
        writer.write(resources.toString());
        writer.write("</resources><targets>");
        writer.write(targets.toString());
        writer.write("</targets><reportName>");
        writer.write(data.seriesName);
        writer.write("</reportName><suiteName>");
        writer.write(data.suiteName);
        writer.write("</suiteName><seriesUri>");
        writer.write(data.seriesUri);
        writer.write("</seriesUri><frequencies>");
        writer.write(frequencies.toString());
        writer.write("</frequencies><notification>");
        writer.write(data.notification);
        writer.write("</notification><deployed>");
        writer.write(dateFormatter.format(data.deployed));
        writer.write("</deployed><lastRun>");
        writer.write(dateFormatter.format(data.lastRun));
        writer.write("</lastRun></config>");
      }
    }
    finally {
      writer.write("</configs>\r\n");
      writer.flush();
    }
  }

  /**
   *
   * @param writer
   * @throws Exception
   */
  private void getReporterSeriesDetail(ProtocolWriter writer) throws Exception {

    Iterator<?> queryResult = DAO.selectMultiple(
        "SELECT seriesConfig, suite.name AS suiteName " +
        "FROM SeriesConfig seriesConfig INNER JOIN seriesConfig.suites suite " +
        "INNER JOIN seriesConfig.series series " +
        "WHERE seriesConfig.deactivated IS NULL AND suite.name != '_runNow'",
        null
    );

    writer.write(Protocol.SUCCESS_COMMAND);
    writer.write(" <configs>");

    try {
      while (queryResult.hasNext()) {
        Object[] row = (Object[])queryResult.next();
        SeriesConfig config = (SeriesConfig)row[0];
        Series configSeries = config.getSeries();
        String suiteName = (String)row[1];
        String nickname = config.getNickname();
        String resourceHostname = configSeries.getResource();
        String targetHostname = configSeries.getTargetHostname().trim();
        String seriesName = configSeries.getReporter();
        String seriesUri = configSeries.getUri();
        String frequency = buildFrequency(config.getSchedule());
        String notifier;
        String emailTarget;

        if (config.getAcceptedOutput() != null && config.getAcceptedOutput().getNotification() != null) {
          Notification notice = config.getAcceptedOutput().getNotification();

          notifier = notice.getNotifier().replaceAll("(\\w+\\.)", "");
          emailTarget = notice.getTarget();
        }
        else {
          notifier = "none";
          emailTarget = "";
        }

        writer.write("<config><suiteName>");
        writer.write(suiteName);
        writer.write("</suiteName><seriesName>");
        writer.write(seriesName);
        writer.write("</seriesName><nickname>");
        writer.write(nickname);
        writer.write("</nickname><resource>");
        writer.write(resourceHostname);
        writer.write("</resource><target>");
        writer.write(targetHostname);
        writer.write("</target><seriesUri>");
        writer.write(seriesUri);
        writer.write("</seriesUri><frequency>");
        writer.write(frequency);
        writer.write("</frequency><notifier>");
        writer.write(notifier);
        writer.write("</notifier><emailTarget>");
        writer.write(emailTarget);
        writer.write("</emailTarget></config>");
      }
    }
    finally {
      writer.write("</configs>\r\n");
      writer.flush();
    }
  }

  /**
   *
   * @param cron
   * @return
   */
  private String buildFrequency(Schedule cron)
  {
    String result = null;

    if (cron != null) {
      String field = cron.getMday();
      String unit;

      if (!field.equals("*"))
        unit = "day";
      else {
        field = cron.getHour();

        if (!field.equals("*"))
          unit = "hour";
        else {
          field = cron.getMinute();

          if (!field.equals("*"))
            unit = "minute";
          else
            unit = null;
        }
      }

      if (unit != null) {
        int offset = field.lastIndexOf('/');

        result = "every ";

        if (offset >= 0) {
          String frequency = field.substring(offset + 1);

          if (!frequency.equals("1"))
            result += frequency + " " + unit + "s";
          else
            result += unit;
        }
        else {
          if (unit.equals("day"))
            result += "month";
          else if (unit.equals("hour"))
            result += "day";
          else
            result += "hour";
        }
      }
    }

    return result;
  }

  /**
   * Creates a ReportDetailsDocument by copying fields from a DB SeriesConfig,
   * a DB report, and a DB InstanceInfo.
   *
   * @param sc the SeriesConfig for the document
   * @param r the Report for the document
   * @param ii the latest InstanceInfo for the document
   * @param cr the latest ComparisonResult for the document
   *
   * @return report details document object
   */
  private static ReportDetailsDocument toBean
    (SeriesConfig sc, Report r, InstanceInfo ii, ComparisonResult cr) {
    Set<Suite> suites = sc.getSuites();
    long[] suiteIds = new long[suites.size()];
    int index = 0;

    for (Suite s : sc.getSuites())
      suiteIds[index++] = s.getId();

    ReportDetailsDocument result =
      ReportDetailsDocument.Factory.newInstance();
    ReportDetails rd = result.addNewReportDetails();
    rd.setSuiteIdArray(suiteIds);
    rd.setSeriesConfigId(sc.getId());
    rd.setSeriesId(r.getSeries().getId());
    rd.setReportId(r.getId());
    rd.setInstanceId(ii.getId());
    rd.setSeriesConfig((edu.sdsc.inca.dataModel.util.SeriesConfig)sc.toBean());
    rd.setReport((edu.sdsc.inca.dataModel.util.Report)r.toBean());
    // Note: apparently getGmt() returns a copy, so the following doesn't work
    //rd.getReport().getGmt().setTime(ii.getCollected());
    GregorianCalendar gmt = new GregorianCalendar();
    gmt.setTime(ii.getCollected());
    rd.getReport().setGmt(gmt);
    String log = ii.getLog();
    if(log != null && !log.equals("") &&
      !log.equals(PersistentObject.DB_EMPTY_STRING)) {
      try {
        rd.getReport().setLog(Log.Factory.parse(log));
      } catch(Exception e) {
        logger.error("Unable to parse log stored in DB:", e);
      }
    }
    if(cr != null) {
      rd.setComparisonResult(cr.getResult());
    }
    rd.setSysusage((edu.sdsc.inca.dataModel.util.Limits)ii.toBean());
    rd.setStderr(r.getStderr());
    return result;
  }

  /**
   * Creates a ReportSummaryDocument by copying fields from a DB SeriesConfig.
   *
   * @param sc the SeriesConfig for the document
   * @param cr the latest comparison result for sc
   * @param ii the latest instance info for sc
   * @param r the latest report for sc
   *
   * @return report summary document
   */
  private static ReportSummaryDocument toBean
    (SeriesConfig sc, ComparisonResult cr, InstanceInfo ii, Report r)
     {
    ReportSummaryDocument result = ReportSummaryDocument.Factory.newInstance();
    ReportSummaryDocument.ReportSummary summary = result.addNewReportSummary();
    Series s = sc.getSeries();
    summary.setHostname(s.getResource());
    summary.setUri(s.getUri());
    summary.setTargetHostname(s.getTargetHostname());
    summary.setNickname(sc.getNickname());
    summary.setSeriesConfigId(sc.getId());
    if(ii != null) {
      summary.setInstanceId(ii.getId());
      GregorianCalendar gmt = new GregorianCalendar();
      gmt.setTime(ii.getCollected());
      summary.setGmt(gmt);
      Schedule sched = sc.getSchedule();
      if(sched != null) {
        Calendar gmtExpires = sched.calculateExpires(gmt);
        if(gmtExpires != null) {
          summary.setGmtExpires(gmtExpires);
        }
      }
    }
    if(r != null) {
      String bodyText = r.getBody();
      try {
        summary.setBody(AnyXmlSequence.Factory.parse(bodyText));
      } catch(XmlException e) {
        // empty
      }
      summary.setErrorMessage(r.getExit_message());
    }
    if(cr != null) {
      summary.setComparisonResult(cr.getResult());
    }
    if ( sc.getTags() != null ) {
      Tags tags = Tags.Factory.newInstance();
      tags.setTagArray(sc.getTags().toArray(new String[sc.getTags().size()]));
      summary.setTags(tags);
    }
    return result;
  }

  /**
   * Returns a list of ComparisonResult objects generated by a given set of
   * SeriesConfig objects.
   *
   * @param configId the id of a SeriesConfig
   *
   * @return Map where the keys are report ids and the values are the
   *         result
   *
   * @throws PersistenceException on DB error
   */
  private Map<Long, String> getRelatedComparisons(long configId) throws PersistenceException {

    StopWatch timer = new Log4JStopWatch(logger);
    String query = "SELECT cr.reportId, cr.result FROM ComparisonResult cr " +
      "WHERE cr.seriesConfigId = " + configId;
    // execute query and parse results
    Iterator<?> crList = DAO.selectMultiple(query, null);
    Map<Long, String> crResultsByReportId = new HashMap<Long, String>();
    while (crList.hasNext()) {
      Object[] info = (Object[])crList.next();
      crResultsByReportId.put( (Long)info[0], (String)info[1] );
    }
    timer.lap("getRelatedComparisons", "numCRs=" + crResultsByReportId.size());
    return crResultsByReportId;
  }

  /**
   * Handle the related instances for the provided reports within a specified
   * time interval
   *
   * @param sc a SeriesConfig for which we need to fetch instances
   * @param begin the begin time expressed as a Date object
   * @param end the end time expressed as a Date object
   * @param processor
   *
   * @return the results expressed as a list of InstanceInfo objects
   *
   * @throws Exception if trouble executing query
   */
  private void processRelatedInstances(SeriesConfig sc, Date begin, Date end, InstanceProcessor processor) throws Exception {

    try {
      Connection dbConn = ConnectionSource.getConnection();
      PreparedStatement selectStmt = null;
      ResultSet rows = null;

      try {
        Series s = sc.getSeries();
        String instanceTableName = s.getInstanceTableName();
        String linkTableName = s.getLinkTableName();

        selectStmt = dbConn.prepareStatement(
            "SELECT instanceid " +
            "FROM " + linkTableName +
              " INNER JOIN (" +
                "SELECT incaid AS instanceId " +
                "FROM " + instanceTableName +
                " WHERE incacollected >= ? " +
                  "AND incacollected <= ?" +
              ") AS instances ON " + linkTableName + ".incainstance_id = instances.instanceid " +
            "WHERE " + linkTableName + ".incaseriesconfig_id = ?"
        );

        selectStmt.setFetchSize(FETCH_SIZE);
        selectStmt.setTimestamp(1, new Timestamp(begin.getTime()));
        selectStmt.setTimestamp(2, new Timestamp(end.getTime()));
        selectStmt.setLong(3, sc.getId());

        rows = selectStmt.executeQuery();

        Map<Long, Report> reports = new TreeMap<Long, Report>();

        while (rows.next()) {
          InstanceInfo instance = new InstanceInfo(s, rows.getLong(1));

          processor.process(instance, reports);
        }
      }
      finally {
        if (rows != null)
          rows.close();

        if (selectStmt != null)
          selectStmt.close();

        dbConn.close();
      }
    }
    catch (SQLException sqlErr) {
      throw new PersistenceException(sqlErr.getMessage());
    }
  }

  /**
   * Returns a list of SeriesConfig objects selected by a WHERE expression.
   *
   * @param expr the HQL WHERE clause to select objects of interest
   *
   * @return list of active series configs that match the expression
   *
   * @throws PersistenceException on DB error
   */
  private Iterator<?> getSelectedConfigs(String expr) throws PersistenceException {

    String query =
      "SELECT config " +
        "FROM SeriesConfig config " +
          "INNER JOIN config.suites suite " +
          "INNER JOIN config.series series " +
        "WHERE config.deactivated IS NULL AND suite.name != '" +
        Protocol.IMMEDIATE_SUITE_NAME + "' AND (" + expr + ")";

    return DAO.selectMultiple(query, null);
  }

  /**
   * Returns a list of Series objects selected by a WHERE expression.
   *
   * @param expr the HQL WHERE clause to select objects of interest
   * @param active only select series that have active series configs
   * @return a list of Series objects that match the expression
   * @throws PersistenceException
   */
  private Iterator<?> getSelectedSeries(String expr, boolean active) throws PersistenceException {

    StringBuilder query = new StringBuilder();

    query.append(
      "SELECT DISTINCT series " +
        "FROM SeriesConfig config " +
          "INNER JOIN config.suites suite " +
          "INNER JOIN config.series series " +
        "WHERE "
    );

    if (active)
      query.append("config.deactivated IS NULL AND ");

    query.append("suite.name != '");
    query.append(Protocol.IMMEDIATE_SUITE_NAME);
    query.append("' AND ( ");
    query.append(expr);
    query.append(" )");

    return DAO.selectMultiple(query.toString(), null);
  }

  /**
   *
   * @param tableName
   * @param idList
   * @return
   * @throws PersistenceException
   */
  private List<Timestamp> getCollectedDates(String tableName, String idList) throws PersistenceException {

    try {
      Connection dbConn = ConnectionSource.getConnection();
      PreparedStatement selectStmt = null;
      ResultSet rows = null;

      try {
        selectStmt = dbConn.prepareStatement(
          "SELECT incacollected " +
          "FROM " + tableName +
          " WHERE incaid IN ( " + idList + " )"
        );

        selectStmt.setFetchSize(FETCH_SIZE);

        rows = selectStmt.executeQuery();

        List<Timestamp> result = new ArrayList<Timestamp>();

        while (rows.next())
          result.add(rows.getTimestamp(1));

        return result;
      }
      finally {
        if (rows != null)
          rows.close();

        if (selectStmt != null)
          selectStmt.close();

        dbConn.close();
      }
    }
    catch (SQLException sqlErr) {
      throw new PersistenceException(sqlErr.getMessage());
    }
  }
}
