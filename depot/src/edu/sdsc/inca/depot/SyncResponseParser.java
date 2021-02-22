/*
 * SyncResponseParser.java
 */
package edu.sdsc.inca.depot;


import java.io.InputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.sdsc.inca.Depot;
import edu.sdsc.inca.depot.persistent.Arg;
import edu.sdsc.inca.depot.persistent.ArgSignature;
import edu.sdsc.inca.depot.persistent.ConnectionManager;
import edu.sdsc.inca.depot.persistent.DatabaseTools;
import edu.sdsc.inca.depot.persistent.PersistenceException;
import edu.sdsc.inca.depot.persistent.Series;


/**
 *
 * @author Paul Hoover
 *
 */
public class SyncResponseParser extends DefaultHandler {

  /**
   *
   */
  private interface InsertStatementBuilder {

    /**
     *
     * @param dbConn
     * @param table
     * @param key
     * @param columns
     * @return
     * @throws SQLException
     */
    UpdateStatement createStatement(Connection dbConn, String table, String key, String columns) throws SQLException;
  }

  /**
   *
   */
  private static class SequenceStatementBuilder implements InsertStatementBuilder {

    /**
    *
    * @param dbConn
    * @param table
    * @param key
    * @param columns
    * @return
    * @throws SQLException
    */
    @Override
    public UpdateStatement createStatement(Connection dbConn, String table, String key, String columns) throws SQLException
    {
      return new SequenceKeyInsertStatement(dbConn, table, key, columns);
    }
  }

  /**
   *
   */
  private static class AutoGenStatementBuilder implements InsertStatementBuilder {

    /**
    *
    * @param dbConn
    * @param table
    * @param key
    * @param columns
    * @return
    * @throws SQLException
    */
    @Override
    public UpdateStatement createStatement(Connection dbConn, String table, String key, String columns) throws SQLException
    {
      return new AutoGenKeyInsertStatement(dbConn, table, columns);
    }
  }

  /**
   *
   */
  private interface ParserState {

    /**
     *
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    void characters(char[] ch, int start, int length) throws SAXException;

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     * @throws IOException
     * @throws PersistenceException
     */
    void endElement(String uri, String localName, String qName) throws SAXException, SQLException, IOException, PersistenceException;

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     * @throws SQLException
     */
    void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException, SQLException;
  }

  /**
   *
   */
  private abstract class Capture implements ParserState {

    private StringBuilder m_builder = null;


    // public methods


    /**
     *
     * @param ch
     * @param start
     * @param length
     */
    @Override
    public void characters(char[] ch, int start, int length)
    {
      if (m_builder != null)
        m_builder.append(new String(ch, start, length));
    }


    // protected methods


    /**
     *
     */
    protected void startTextCapture()
    {
      assert m_builder == null;

      m_builder = new StringBuilder();
    }

    /**
     *
     * @return
     */
    protected String getTextAsString()
    {
      assert m_builder != null;

      String text = m_builder.toString();

      m_builder = null;

      if (text.length() == 0)
        return null;

      return text;
    }

    /**
     *
     * @return
     */
    protected Boolean getTextAsBoolean()
    {
      String text = getTextAsString();

      if (text == null)
        return null;

      return Boolean.valueOf(text);
    }

    /**
     *
     * @return
     * @throws SAXException
     */
    protected Long getTextAsLong() throws SAXException
    {
      String text = getTextAsString();

      if (text == null)
        return null;

      try {
        return Long.valueOf(text);
      }
      catch (NumberFormatException numErr) {
        throw new SAXException("Can't convert value " + text + " to a Long");
      }
    }

    /**
     *
     * @return
     * @throws SAXException
     */
    protected Integer getTextAsInteger() throws SAXException
    {
      String text = getTextAsString();

      if (text == null)
        return null;

      try {
        return Integer.valueOf(text);
      }
      catch (NumberFormatException numErr) {
        throw new SAXException("Can't convert value " + text + " to an Integer");
      }
    }

    /**
     *
     * @return
     * @throws SAXException
     */
    protected Float getTextAsFloat() throws SAXException
    {
      String text = getTextAsString();

      if (text == null)
        return null;

      try {
        return Float.valueOf(text);
      }
      catch (NumberFormatException numErr) {
        throw new SAXException("Can't convert value " + text + " to a Float");
      }
    }

    /**
     *
     * @return
     * @throws SAXException
     */
    protected Timestamp getTextAsTimestamp() throws SAXException
    {
      String text = getTextAsString();

      if (text == null)
        return null;

      try {
        return Timestamp.valueOf(text);
      }
      catch (IllegalArgumentException argErr) {
        throw new SAXException("Can't convert value " + text + " to a Timestamp");
      }
    }
  }

  /**
   *
   */
  private abstract class ParseTableRoot implements ParserState {

    protected int m_totalInserts = 0;
    protected final UpdateStatement m_insertStmt;


    // constructors


    /**
     *
     * @param table
     * @param key
     * @param columns
     * @throws SQLException
     */
    protected ParseTableRoot(String table, String key, String columns) throws SQLException
    {
      m_insertStmt = m_stmtBuilder.createStatement(m_dbConn, table, key, columns);
    }


    // public methods


    /**
     *
     * @param ch
     * @param start
     * @param length
     */
    @Override
    public void characters(char[] ch, int start, int length)
    {
      // do nothing
    }
  }

  /**
   *
   */
  private class ParseSuiteRows extends ParseTableRoot {

    // constructors


    /**
     *
     * @throws SQLException
     */
    public ParseSuiteRows() throws SQLException
    {
      super("incasuite", "incaid", "incaname, incaguid, incadescription, incaversion");
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("suiteRows")) {
        m_insertStmt.close();

        m_logger.debug("Finished parsing " + m_totalInserts + " Suite records");

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("suite"))
        m_states.addFirst(new ParseSuite(this));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }

    /**
     *
     * @param name
     * @param guid
     * @param description
     * @param version
     * @return
     * @throws SQLException
     */
    public long addRecord(String name, String guid, String description, Integer version) throws SQLException
    {
      m_insertStmt.setString(1, name);
      m_insertStmt.setString(2, guid);
      m_insertStmt.setString(3, description);
      m_insertStmt.setInt(4, version);

      long suiteId = m_insertStmt.update();

      if (m_logger.isDebugEnabled())
        m_totalInserts += 1;

      return suiteId;
    }
  }

  /**
   *
   */
  private class ParseSuite extends Capture {

    private final ParseSuiteRows m_owner;
    private Long m_oldId = null;
    private String m_name = null;
    private String m_guid = null;
    private String m_description = null;
    private Integer m_version = null;


    // constructors


    /**
     *
     * @param owner
     */
    public ParseSuite(ParseSuiteRows owner)
    {
      m_owner = owner;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id"))
        m_oldId = getTextAsLong();
      else if (localName.equals("name"))
        m_name = getTextAsString();
      else if (localName.equals("guid"))
        m_guid = getTextAsString();
      else if (localName.equals("description"))
        m_description = getTextAsString();
      else if (localName.equals("version"))
        m_version = getTextAsInteger();
      else if (localName.equals("suite")) {
        long newSuiteId = m_owner.addRecord(m_name, m_guid, m_description, m_version);

        m_suiteIds.put(m_oldId, newSuiteId);

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id") || localName.equals("name") || localName.equals("guid") ||
          localName.equals("description") || localName.equals("version"))
        startTextCapture();
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseArgRows extends ParseTableRoot {

    // constructors


    /**
     *
     * @throws SQLException
     */
    public ParseArgRows() throws SQLException
    {
      super("incaarg", "incaid", "incaname, incavalue");
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("argRows")) {
        m_insertStmt.close();

        m_logger.debug("Finished parsing " + m_totalInserts + " Arg records");

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("arg"))
        m_states.addFirst(new ParseArg(this));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }

    /**
     *
     * @param name
     * @param value
     * @return
     * @throws SQLException
     */
    public long addRecord(String name, String value) throws SQLException
    {
      m_insertStmt.setString(1, name);
      m_insertStmt.setString(2, value);

      long argId = m_insertStmt.update();

      if (m_logger.isDebugEnabled())
        m_totalInserts += 1;

      return argId;
    }
  }

  /**
   *
   */
  private class ParseArg extends Capture {

    private final ParseArgRows m_owner;
    private Long m_oldId = null;
    private String m_name = null;
    private String m_value = null;


    // constructors


    /**
     *
     * @param owner
     */
    public ParseArg(ParseArgRows owner)
    {
      m_owner = owner;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id"))
        m_oldId = getTextAsLong();
      else if (localName.equals("name"))
        m_name = getTextAsString();
      else if (localName.equals("value"))
        m_value = getTextAsString();
      else if (localName.equals("arg")) {
        long newId = m_owner.addRecord(m_name, m_value);

        m_argIds.put(m_oldId, newId);

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id") || localName.equals("name") || localName.equals("value"))
        startTextCapture();
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseArgSignatureRows extends ParseTableRoot {

    private final Map<String, Long> m_signatures = new TreeMap<String, Long>();
    private final Map<Long, Set<Arg>> m_mappings = new TreeMap<Long, Set<Arg>>();


    // constructors


    /**
     *
     * @throws SQLException
     */
    public ParseArgSignatureRows() throws SQLException
    {
      super("incaargsignature", "incaid", "incasignature");
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("argSignatureRows")) {
        m_insertStmt.close();

        m_logger.debug("Mapping ArgSignatures to Args...");

        insertArgMappings();

        m_logger.debug("Finished parsing " + m_totalInserts + " ArgSignature records");

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("argSignature"))
        m_states.addFirst(new ParseArgSignature(this));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }

    /**
     *
     * @param signature
     * @param args
     * @return
     * @throws SQLException
     */
    public long addRecord(String signature, Set<Arg> args) throws SQLException
    {
      m_insertStmt.setString(1, signature);

      long argSignatureId = m_insertStmt.update();

      m_signatures.put(signature, argSignatureId);
      m_mappings.put(argSignatureId, args);

      if (m_logger.isDebugEnabled())
        m_totalInserts += 1;

      return argSignatureId;
    }

    /**
     *
     * @param signature
     * @return
     */
    public Long findDuplicate(String signature)
    {
      return m_signatures.get(signature);
    }


    // private methods


    /**
     *
     * @throws SQLException
     */
    private void insertArgMappings() throws SQLException
    {
      BatchUpdateStatement mappingsStmt = new BatchUpdateStatement(m_dbConn,
        "INSERT INTO incaargs ( incaargs_id, incainput_id ) " +
        "VALUES ( ?, ? )"
      );

      for (Map.Entry<Long, Set<Arg>> entry : m_mappings.entrySet()) {
        Long argSignatureId = entry.getKey();
        Set<Arg> args = entry.getValue();

        for (Arg element : args) {
          mappingsStmt.setLong(1, argSignatureId);
          mappingsStmt.setLong(2, element.getId());
          mappingsStmt.update();
        }
      }

      mappingsStmt.close();
    }
  }

  /**
   *
   */
  private class ParseArgMappings extends Capture {

    private final List<Long> m_ids = new ArrayList<Long>();
    private final Set<Arg> m_args;


    // constructors


    /**
     *
     * @param args
     */
    public ParseArgMappings(Set<Arg> args)
    {
      m_args = args;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     * @throws PersistenceException
     * @throws IOException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException, IOException, PersistenceException
    {
      if (localName.equals("id")) {
        Long oldArgId = getTextAsLong();
        Long newArgId = m_argIds.get(oldArgId);

        if (newArgId != null)
          m_ids.add(newArgId);
        else
          m_logger.warn("Couldn't find an Arg record with previous id " + oldArgId);
      }
      else if (localName.equals("args")) {
        for (Long argId : m_ids) {
          Arg newArg = new Arg(argId);

          m_args.add(newArg);
        }

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id"))
        startTextCapture();
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseArgSignature extends Capture {

    private final ParseArgSignatureRows m_owner;
    private final Set<Arg> m_args = new HashSet<Arg>();
    private Long m_oldId = null;


    // constructors


    /**
     *
     * @param owner
     */
    public ParseArgSignature(ParseArgSignatureRows owner)
    {
      m_owner = owner;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id"))
        m_oldId = getTextAsLong();
      else if (localName.equals("argSignature")) {
        String signature = (new ArgSignature(m_args)).getSignature();
        Long newId = m_owner.findDuplicate(signature);

        if (newId == null)
          newId = m_owner.addRecord(signature, m_args);
        else
          m_logger.warn("Found a duplicate of ArgSignature record " + newId);

        m_argSignatureIds.put(m_oldId, newId);

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id"))
        startTextCapture();
      else if (localName.equals("args"))
        m_states.addFirst(new ParseArgMappings(m_args));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseSeriesRows extends ParseTableRoot {

    // constructors


    /**
     *
     * @throws SQLException
     */
    public ParseSeriesRows() throws SQLException
    {
      super("incaseries", "incaid", "incareporter, incaversion, incauri, incacontext, incanice, incaresource, incatargethostname, incaargSignature_id");
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("seriesRows")) {
        m_insertStmt.close();

        updateTableNameColumns();
        createInstanceTables();

        m_logger.debug("Finished parsing " + m_totalInserts + " Series records");

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("series"))
        m_states.addFirst(new ParseSeries(this));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }

    /**
     *
     * @param reporter
     * @param version
     * @param uri
     * @param context
     * @param nice
     * @param resource
     * @param targetHostname
     * @param argSignatureId
     * @return
     * @throws SQLException
     */
    public long addRecord(String reporter, String version, String uri, String context, Boolean nice, String resource, String targetHostname, Long argSignatureId) throws SQLException
    {
      m_insertStmt.setString(1, reporter);
      m_insertStmt.setString(2, version);
      m_insertStmt.setString(3, uri);
      m_insertStmt.setString(4, context);
      m_insertStmt.setBoolean(5, nice);
      m_insertStmt.setString(6, resource);
      m_insertStmt.setString(7, targetHostname);
      m_insertStmt.setLong(8, argSignatureId);

      long seriesId = m_insertStmt.update();

      if (m_logger.isDebugEnabled())
        m_totalInserts += 1;

      return seriesId;
    }


    // private methods


    /**
     *
     * @throws SQLException
     */
    private void updateTableNameColumns() throws SQLException
    {
      Statement updateStmt = m_dbConn.createStatement();

      try {
        updateStmt.executeUpdate(
            "UPDATE incaseries " +
            "SET incainstancetablename = 'incainstanceinfo_' || incaid, " +
              "incalinktablename = 'incaseriesconfigsinstances_' || incaid"
        );

        m_dbConn.commit();
      }
      finally {
        updateStmt.close();
      }
    }

    /**
     *
     * @throws SQLException
     */
    private void createInstanceTables() throws SQLException
    {
      Statement selectStmt = m_dbConn.createStatement();
      ResultSet rows = null;

      try {
        rows = selectStmt.executeQuery(
            "SELECT incainstancetablename, incalinktablename " +
            "FROM incaseries"
        );

        while (rows.next()) {
          String instanceTableName = rows.getString(1);
          String linkTableName = rows.getString(2);

          Series.createInstanceTables(m_dbConn, instanceTableName, linkTableName);
        }
      }
      finally {
        if (rows != null)
          rows.close();

        selectStmt.close();
      }
    }
  }

  /**
   *
   */
  private class ParseSeries extends Capture {

    private final ParseSeriesRows m_owner;
    private Long m_oldId = null;
    private String m_reporter = null;
    private String m_version = null;
    private String m_uri = null;
    private String m_context = null;
    private Boolean m_nice = null;
    private String m_resource = null;
    private String m_targetHostname = null;
    private Long m_argSignatureId = null;


    // constructors


    /**
     *
     * @param owner
     */
    public ParseSeries(ParseSeriesRows owner)
    {
      m_owner = owner;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id"))
        m_oldId = getTextAsLong();
      else if (localName.equals("reporter"))
        m_reporter = getTextAsString();
      else if (localName.equals("version"))
        m_version = getTextAsString();
      else if (localName.equals("uri"))
        m_uri = getTextAsString();
      else if (localName.equals("context"))
        m_context = getTextAsString();
      else if (localName.equals("nice"))
        m_nice = getTextAsBoolean();
      else if (localName.equals("resource"))
        m_resource = getTextAsString();
      else if (localName.equals("targetHostname"))
        m_targetHostname = getTextAsString();
      else if (localName.equals("argSignatureId")) {
        Long oldSignatureId = getTextAsLong();
        Long newSignatureId = m_argSignatureIds.get(oldSignatureId);

        if (newSignatureId != null)
          m_argSignatureId = newSignatureId;
        else
          m_logger.warn("Couldn't find an ArgSignature record with previous id " + oldSignatureId + " for Series record with previous id " + m_oldId);
      }
      else if (localName.equals("series")) {
        if (m_argSignatureId != null) {
          long newId = m_owner.addRecord(m_reporter, m_version, m_uri, m_context, m_nice, m_resource, m_targetHostname, m_argSignatureId);

          m_seriesIds.put(m_oldId, newId);
        }

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id") || localName.equals("reporter") || localName.equals("version") ||
          localName.equals("uri") || localName.equals("context") || localName.equals("nice") ||
          localName.equals("resource") || localName.equals("targetHostname") || localName.equals("argSignatureId"))
        startTextCapture();
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseSeriesConfigRows extends ParseTableRoot {

    private final Map<Long, Set<Long>> m_mappings = new TreeMap<Long, Set<Long>>();
    private final Map<Long, List<String>> m_tags = new TreeMap<Long, List<String>>();


    // constructors


    /**
     *
     * @param SQLException
     */
    public ParseSeriesConfigRows() throws SQLException
    {
      super("incaseriesconfig", "incaid", "incaactivated, incadeactivated, incanickname, incawallClockTime, incacpuTime, incamemory, incacomparitor, incacomparison, incanotifier, incatarget, incatype, incaminute, incahour, incamonth, incamday, incawday, incanumOccurs, incasuspended, incaseries_id, incalatestInstanceId, incalatestComparisonId");
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("seriesConfigRows")) {
        m_insertStmt.close();

        m_logger.debug("Mapping SeriesConfigs to Suites...");

        insertSuiteConfigMappings();

        m_logger.debug("Adding SeriesConfig tags...");

        insertConfigTags();

        m_logger.debug("Finished parsing " + m_totalInserts + " SeriesConfig records");

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("seriesConfig"))
        m_states.addFirst(new ParseSeriesConfig(this));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }

    /**
     *
     * @param activated
     * @param deactivated
     * @param nickname
     * @param wallClockTime
     * @param cpuTime
     * @param memory
     * @param comparitor
     * @param comparison
     * @param notifier
     * @param target
     * @param type
     * @param minute
     * @param hour
     * @param month
     * @param mday
     * @param wday
     * @param numOccurs
     * @param suspended
     * @param seriesId
     * @param latestComparisonId
     * @param suites
     * @param tags
     * @return
     * @throws SQLException
     */
    public long addRecord(Timestamp activated, Timestamp deactivated, String nickname, Float wallClockTime, Float cpuTime, Float memory, String comparitor, String comparison, String notifier, String target, String type, String minute, String hour, String month, String mday, String wday, Integer numOccurs, Boolean suspended, Long seriesId, Long latestComparisonId, Set<Long> suites, List<String> tags) throws SQLException
    {
      m_insertStmt.setTimestamp(1, activated);
      m_insertStmt.setTimestamp(2, deactivated);
      m_insertStmt.setString(3, nickname);
      m_insertStmt.setFloat(4, wallClockTime);
      m_insertStmt.setFloat(5, cpuTime);
      m_insertStmt.setFloat(6, memory);
      m_insertStmt.setString(7, comparitor);
      m_insertStmt.setString(8, comparison);
      m_insertStmt.setString(9, notifier);
      m_insertStmt.setString(10, target);
      m_insertStmt.setString(11, type);
      m_insertStmt.setString(12, minute);
      m_insertStmt.setString(13, hour);
      m_insertStmt.setString(14, month);
      m_insertStmt.setString(15, mday);
      m_insertStmt.setString(16, wday);
      m_insertStmt.setInt(17, numOccurs);
      m_insertStmt.setBoolean(18, suspended);
      m_insertStmt.setLong(19, seriesId);
      m_insertStmt.setLong(20, -1L);
      m_insertStmt.setLong(21, latestComparisonId);

      long seriesConfigId = m_insertStmt.update();

      m_mappings.put(seriesConfigId, suites);
      m_tags.put(seriesConfigId, tags);

      if (m_logger.isDebugEnabled())
        m_totalInserts += 1;

      return seriesConfigId;
    }


    // private methods


    /**
     *
     * @throws SQLException
     */
    private void insertSuiteConfigMappings() throws SQLException
    {
      BatchUpdateStatement mappingsStmt = new BatchUpdateStatement(m_dbConn,
        "INSERT INTO incasuitesseriesconfigs ( incasuite_id, incaseriesconfig_id ) " +
        "VALUES ( ?, ? )"
      );

      for (Map.Entry<Long, Set<Long>> entry : m_mappings.entrySet()) {
        Long configId = entry.getKey();
        Set<Long> suiteIds = entry.getValue();

        for (Long suiteId : suiteIds) {
          mappingsStmt.setLong(1, suiteId);
          mappingsStmt.setLong(2, configId);
          mappingsStmt.update();
        }
      }

      mappingsStmt.close();
    }

    /**
     *
     * @throws SQLException
     */
    private void insertConfigTags() throws SQLException
    {
      BatchUpdateStatement tagsStmt = new BatchUpdateStatement(m_dbConn,
        "INSERT INTO incaseriesconfigtags ( tag, incaseriesconfig_id ) " +
        "VALUES ( ?, ? )"
      );

      for (Map.Entry<Long, List<String>> entry : m_tags.entrySet()) {
        Long configId = entry.getKey();
        List<String> configTags = entry.getValue();

        for (String tag : configTags) {
          tagsStmt.setString(1, tag);
          tagsStmt.setLong(2, configId);
          tagsStmt.update();
        }
      }

      tagsStmt.close();
    }
  }

  /**
   *
   */
  private class ParseSuiteConfigMappings extends Capture {

    private final Set<Long> m_suites;


    // constructors


    /**
     *
     * @param suites
     */
    public ParseSuiteConfigMappings(Set<Long> suites)
    {
      m_suites = suites;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id")) {
        Long oldSuiteId = getTextAsLong();
        Long newSuiteId = m_suiteIds.get(oldSuiteId);

        if (newSuiteId != null)
          m_suites.add(newSuiteId);
        else
          m_logger.warn("Couldn't find a Suite record with previous id " + oldSuiteId);
      }
      else if (localName.equals("suites")) {
        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id"))
        startTextCapture();
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseConfigTags extends Capture {

    private final List<String> m_tags;


    // constructors


    /**
     *
     * @param tags
     */
    public ParseConfigTags(List<String> tags)
    {
      m_tags = tags;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("tag")) {
        String tag = getTextAsString();

        m_tags.add(tag);
      }
      else if (localName.equals("tags")) {
        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("tag"))
        startTextCapture();
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseSeriesConfig extends Capture {

    private final ParseSeriesConfigRows m_owner;
    private final Set<Long> m_suites = new HashSet<Long>();
    private final List<String> m_tags = new ArrayList<String>();
    private Long m_oldId = null;
    private Timestamp m_activated = null;
    private Timestamp m_deactivated = null;
    private String m_nickname = null;
    private Float m_wallClockTime = null;
    private Float m_cpuTime = null;
    private Float m_memory = null;
    private String m_comparitor = null;
    private String m_comparison = null;
    private String m_notifier = null;
    private String m_target = null;
    private String m_type = null;
    private String m_minute = null;
    private String m_hour = null;
    private String m_month = null;
    private String m_mday = null;
    private String m_wday = null;
    private Integer m_numOccurs = null;
    private Boolean m_suspended = null;
    private Long m_seriesId = null;
    private Long m_latestInstanceId = null;
    private Long m_latestComparisonId = null;


    // constructors


    /**
     *
     * @param owner
     */
    public ParseSeriesConfig(ParseSeriesConfigRows owner)
    {
      m_owner = owner;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id"))
        m_oldId = getTextAsLong();
      else if (localName.equals("activated"))
        m_activated = getTextAsTimestamp();
      else if (localName.equals("deactivated"))
        m_deactivated = getTextAsTimestamp();
      else if (localName.equals("nickname"))
        m_nickname = getTextAsString();
      else if (localName.equals("wallClockTime"))
        m_wallClockTime = getTextAsFloat();
      else if (localName.equals("cpuTime"))
        m_cpuTime = getTextAsFloat();
      else if (localName.equals("memory"))
        m_memory = getTextAsFloat();
      else if (localName.equals("comparitor"))
        m_comparitor = getTextAsString();
      else if (localName.equals("comparison"))
        m_comparison = getTextAsString();
      else if (localName.equals("notifier"))
        m_notifier = getTextAsString();
      else if (localName.equals("target"))
        m_target = getTextAsString();
      else if (localName.equals("type"))
        m_type = getTextAsString();
      else if (localName.equals("minute"))
        m_minute = getTextAsString();
      else if (localName.equals("hour"))
        m_hour = getTextAsString();
      else if (localName.equals("month"))
        m_month = getTextAsString();
      else if (localName.equals("mday"))
        m_mday = getTextAsString();
      else if (localName.equals("wday"))
        m_wday = getTextAsString();
      else if (localName.equals("numOccurs"))
        m_numOccurs = getTextAsInteger();
      else if (localName.equals("suspended"))
        m_suspended = getTextAsBoolean();
      else if (localName.equals("seriesId")) {
        Long oldSeriesId = getTextAsLong();
        Long newSeriesId = m_seriesIds.get(oldSeriesId);

        if (newSeriesId != null)
          m_seriesId = newSeriesId;
        else
          m_logger.warn("Couldn't find a Series record with previous id " + oldSeriesId + " for SeriesConfig record with previous id " + m_oldId);
      }
      else if (localName.equals("latestInstanceId"))
        m_latestInstanceId = getTextAsLong();
      else if (localName.equals("latestComparisonId"))
        m_latestComparisonId = getTextAsLong();
      else if (localName.equals("seriesConfig")) {
        if (m_seriesId != null) {
          long newId = m_owner.addRecord(m_activated, m_deactivated, m_nickname, m_wallClockTime, m_cpuTime, m_memory, m_comparitor, m_comparison, m_notifier, m_target, m_type, m_minute, m_hour, m_month, m_mday, m_wday, m_numOccurs, m_suspended, m_seriesId, m_latestComparisonId, m_suites, m_tags);

          m_seriesConfigIds.put(m_oldId, newId);

          if (m_latestInstanceId >= 0) {
            List<Long> seriesConfigIds = m_latestInstanceIds.get(m_latestInstanceId);

            if (seriesConfigIds == null) {
              seriesConfigIds = new ArrayList<Long>();

              m_latestInstanceIds.put(m_latestInstanceId, seriesConfigIds);
            }

            seriesConfigIds.add(newId);
          }

          if (m_latestComparisonId >= 0)
            m_latestComparisonIds.put(m_latestComparisonId, newId);
        }

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id") || localName.equals("activated") || localName.equals("deactivated") ||
          localName.equals("nickname") || localName.equals("wallClockTime") || localName.equals("cpuTime") ||
          localName.equals("memory") || localName.equals("comparitor") || localName.equals("comparison") ||
          localName.equals("notifier") || localName.equals("target") || localName.equals("type") ||
          localName.equals("minute") || localName.equals("hour") || localName.equals("month") ||
          localName.equals("mday") || localName.equals("wday") || localName.equals("numOccurs") ||
          localName.equals("suspended") || localName.equals("seriesId") || localName.equals("latestInstanceId") ||
          localName.equals("latestComparisonId"))
        startTextCapture();
      else if (localName.equals("suites"))
        m_states.addFirst(new ParseSuiteConfigMappings(m_suites));
      else if (localName.equals("tags"))
        m_states.addFirst(new ParseConfigTags(m_tags));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseRunInfoRows extends ParseTableRoot {

    // constructors


    /**
     *
     * @throws SQLException
     */
    public ParseRunInfoRows() throws SQLException
    {
      super("incaruninfo", "incaid", "incahostname, incaworkingDir, incareporterPath, incaargSignature_id");
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("runInfoRows")) {
        m_insertStmt.close();

        m_logger.debug("Finished parsing " + m_totalInserts + " RunInfo records");

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("runInfo"))
        m_states.addFirst(new ParseRunInfo(this));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }

    /**
     *
     * @param hostname
     * @param workingDir
     * @param reporterPath
     * @param argSignatureId
     * @return
     * @throws SQLException
     */
    public long addRecord(String hostname, String workingDir, String reporterPath, Long argSignatureId) throws SQLException
    {
      m_insertStmt.setString(1, hostname);
      m_insertStmt.setString(2, workingDir);
      m_insertStmt.setString(3, reporterPath);
      m_insertStmt.setLong(4, argSignatureId);

      long runInfoId = m_insertStmt.update();

      if (m_logger.isDebugEnabled())
        m_totalInserts += 1;

      return runInfoId;
    }
  }

  /**
   *
   */
  private class ParseRunInfo extends Capture {

    private final ParseRunInfoRows m_owner;
    private Long m_oldId = null;
    private String m_hostname = null;
    private String m_workingDir = null;
    private String m_reporterPath = null;
    private Long m_argSignatureId = null;


    // constructors


    /**
     *
     * @param owner
     */
    public ParseRunInfo(ParseRunInfoRows owner)
    {
      m_owner = owner;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id"))
        m_oldId = getTextAsLong();
      else if (localName.equals("hostname"))
        m_hostname = getTextAsString();
      else if (localName.equals("workingDir"))
        m_workingDir = getTextAsString();
      else if (localName.equals("reporterPath"))
        m_reporterPath = getTextAsString();
      else if (localName.equals("argSignatureId")) {
        Long oldSignatureId = getTextAsLong();
        Long newSignatureId = m_argSignatureIds.get(oldSignatureId);

        if (newSignatureId != null)
          m_argSignatureId = newSignatureId;
        else
          m_logger.warn("Couldn't find an ArgSignature record with previous id " + oldSignatureId + " for RunInfo record with previous id " + m_oldId);
      }
      else if (localName.equals("runInfo")) {
        if (m_argSignatureId != null) {
          long newId = m_owner.addRecord(m_hostname, m_workingDir, m_reporterPath, m_argSignatureId);

          m_runInfoIds.put(m_oldId, newId);
        }

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id") || localName.equals("hostname") || localName.equals("workingDir") ||
          localName.equals("reporterPath") || localName.equals("argSignatureId"))
        startTextCapture();
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseReportRows extends ParseTableRoot {

    // constructors


    /**
     *
     * @throws SQLException
     */
    public ParseReportRows() throws SQLException
    {
      super("incareport", "incaid", "incaexit_status, incaexit_message, incabodypart1, incabodypart2, incabodypart3, incastderr, incaseries_id, incarunInfo_id");
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("reportRows")) {
        m_insertStmt.close();

        m_logger.debug("Finished parsing " + m_totalInserts + " Report records");

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("report"))
        m_states.addFirst(new ParseReport(this));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }

    /**
     *
     * @param exitStatus
     * @param exitMessage
     * @param bodypart1
     * @param bodypart2
     * @param bodypart3
     * @param stderr
     * @param seriesId
     * @param runInfoId
     * @return
     * @throws SQLException
     */
    public long addRecord(Boolean exitStatus, String exitMessage, String bodypart1, String bodypart2, String bodypart3, String stderr, Long seriesId, Long runInfoId) throws SQLException
    {
      m_insertStmt.setBoolean(1, exitStatus);
      m_insertStmt.setString(2, exitMessage);
      m_insertStmt.setString(3, bodypart1);
      m_insertStmt.setString(4, bodypart2);
      m_insertStmt.setString(5, bodypart3);
      m_insertStmt.setString(6, stderr);
      m_insertStmt.setLong(7, seriesId);
      m_insertStmt.setLong(8, runInfoId);

      long reportId = m_insertStmt.update();

      if (m_logger.isDebugEnabled())
        m_totalInserts += 1;

      return reportId;
    }
  }

  /**
   *
   */
  private class ParseReport extends Capture {

    private final ParseReportRows m_owner;
    private Long m_oldId = null;
    private Boolean m_exitStatus = null;
    private String m_exitMessage = null;
    private String m_bodypart1 = null;
    private String m_bodypart2 = null;
    private String m_bodypart3 = null;
    private String m_stderr = null;
    private Long m_seriesId = null;
    private Long m_runInfoId = null;


    // constructors


    /**
     *
     * @param owner
     */
    public ParseReport(ParseReportRows owner)
    {
      m_owner = owner;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id"))
        m_oldId = getTextAsLong();
      else if (localName.equals("exitStatus"))
        m_exitStatus = getTextAsBoolean();
      else if (localName.equals("exitMessage"))
        m_exitMessage = getTextAsString();
      else if (localName.equals("bodypart1"))
        m_bodypart1 = getTextAsString();
      else if (localName.equals("bodypart2"))
        m_bodypart2 = getTextAsString();
      else if (localName.equals("bodypart3"))
        m_bodypart3 = getTextAsString();
      else if (localName.equals("stderr"))
        m_stderr = getTextAsString();
      else if (localName.equals("seriesId")) {
        Long oldSeriesId = getTextAsLong();
        Long newSeriesId = m_seriesIds.get(oldSeriesId);

        if (newSeriesId != null)
          m_seriesId = newSeriesId;
        else
          m_logger.warn("Couldn't find a Series record with previous id " + oldSeriesId + " for Report record with previous id " + m_oldId);
      }
      else if (localName.equals("runInfoId")) {
        Long oldRunInfoId = getTextAsLong();
        Long newRunInfoId = m_runInfoIds.get(oldRunInfoId);

        if (newRunInfoId != null)
          m_runInfoId = newRunInfoId;
        else
          m_logger.warn("Couldn't find a RunInfo record with previous id " + oldRunInfoId + " for Report record with previous id " + m_oldId);
      }
      else if (localName.equals("report")) {
        if (m_seriesId != null && m_runInfoId != null) {
          long newId = m_owner.addRecord(m_exitStatus, m_exitMessage, m_bodypart1, m_bodypart2, m_bodypart3, m_stderr, m_seriesId, m_runInfoId);

          m_reportIds.put(m_oldId, newId);
        }

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id") || localName.equals("exitStatus") || localName.equals("exitMessage") ||
          localName.equals("bodypart1") || localName.equals("bodypart2") || localName.equals("bodypart3") ||
          localName.equals("stderr") || localName.equals("seriesId") || localName.equals("runInfoId"))
        startTextCapture();
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseComparisonResultRows extends ParseTableRoot {

    private final BatchUpdateStatement m_fixRefsStmt;


    // constructors


    /**
     *
     * @throws SQLException
     */
    public ParseComparisonResultRows() throws SQLException
    {
      super("incacomparisonresult", "incaid", "incaresult, incareportId, incaseriesConfigId");

      m_fixRefsStmt = new BatchUpdateStatement(m_dbConn,
        "UPDATE incaseriesconfig " +
        "SET incalatestComparisonId = ? " +
        "WHERE incaid = ?"
      );
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     * @throws IOException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException, IOException
    {
      if (localName.equals("comparisonResultRows")) {
        m_insertStmt.close();
        m_fixRefsStmt.close();

        m_logger.debug("Finished parsing " + m_totalInserts + " ComparisonResult records");

        m_depot.removeSyncLock();

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("comparisonResult"))
        m_states.addFirst(new ParseComparisonResult(this));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }

    /**
     *
     * @param result
     * @param reportId
     * @param seriesConfigId
     * @return
     * @throws SQLException
     */
    public long addRecord(String result, Long reportId, Long seriesConfigId) throws SQLException
    {
      m_insertStmt.setString(1, result);
      m_insertStmt.setLong(2, reportId);
      m_insertStmt.setLong(3, seriesConfigId);

      long comparisonResultId = m_insertStmt.update();

      if (m_logger.isDebugEnabled())
        m_totalInserts += 1;

      return comparisonResultId;
    }

    /**
     *
     * @param comparisonId
     * @param seriesConfigId
     * @throws SQLException
     */
    public void fixReferences(long comparisonId, long seriesConfigId) throws SQLException
    {
      m_fixRefsStmt.setLong(1, comparisonId);
      m_fixRefsStmt.setLong(2, seriesConfigId);
      m_fixRefsStmt.update();
    }
  }

  /**
   *
   */
  private class ParseComparisonResult extends Capture {

    private final ParseComparisonResultRows m_owner;
    private Long m_oldId = null;
    private String m_result = null;
    private Long m_reportId = null;
    private Long m_seriesConfigId = null;


    // constructors


    /**
     *
     * @param owner
     */
    public ParseComparisonResult(ParseComparisonResultRows owner)
    {
      m_owner = owner;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id"))
        m_oldId = getTextAsLong();
      else if (localName.equals("result"))
        m_result = getTextAsString();
      else if (localName.equals("reportId")) {
        Long oldReportId = getTextAsLong();
        Long newReportId = m_reportIds.get(oldReportId);

        if (newReportId != null)
          m_reportId = newReportId;
        else
          m_logger.warn("Couldn't find a Report record with previous id " + oldReportId + " for ComparisonResult record with previous id " + m_oldId);
      }
      else if (localName.equals("seriesConfigId")) {
        Long oldSeriesConfigId = getTextAsLong();
        Long newSeriesConfigId = m_seriesConfigIds.get(oldSeriesConfigId);

        if (newSeriesConfigId != null)
          m_seriesConfigId = newSeriesConfigId;
        else
          m_logger.warn("Couldn't find a SeriesConfig record with previous id " + oldSeriesConfigId + " for ComparisonResult record with previous id " + m_oldId);
      }
      else if (localName.equals("comparisonResult")) {
        if (m_reportId != null && m_seriesConfigId != null) {
          long newId = m_owner.addRecord(m_result, m_reportId, m_seriesConfigId);

          Long seriesConfigId = m_latestComparisonIds.get(m_oldId);

          if (seriesConfigId != null)
            m_owner.fixReferences(newId, seriesConfigId);
        }

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id") || localName.equals("result") || localName.equals("reportId") ||
          localName.equals("seriesConfigId"))
        startTextCapture();
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseInstanceInfoRows extends ParseTableRoot {

    private final ParseSeriesInstance m_owner;
    private final Map<Long, Set<Long>> m_mappings = new TreeMap<Long, Set<Long>>();
    private final BatchUpdateStatement m_fixRefsStmt;


    // constructors


    /**
     *
     * @param owner
     * @throws SQLException
     */
    public ParseInstanceInfoRows(ParseSeriesInstance owner) throws SQLException
    {
      super(owner.getInstanceTableName(), "incaid", "incacollected, incacommited, incamemoryUsageMB, incacpuUsageSec, incawallClockTimeSec, incalog, incareportId");

      m_owner = owner;
      m_fixRefsStmt = new BatchUpdateStatement(m_dbConn,
        "UPDATE incaseriesconfig " +
        "SET incalatestinstanceid = ? " +
        "WHERE incaid = ? " +
          "AND incalatestinstanceid < 0"
      );
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("instanceInfoRows")) {
        m_insertStmt.close();
        m_fixRefsStmt.close();

        m_logger.debug("Mapping InstanceInfos to SeriesConfigs...");

        insertConfigInstanceMappings();

        m_logger.debug("Finished parsing " + m_totalInserts + " InstanceInfo records");

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("instanceInfo"))
        m_states.addFirst(new ParseInstanceInfo(this));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }

    /**
     *
     * @param collected
     * @param commited
     * @param memoryUsageMB
     * @param cpuUsageSec
     * @param wallClockTimeSec
     * @param log
     * @param reportId
     * @param seriesConfigs
     * @return
     * @throws SQLException
     */
    public long addRecord(Timestamp collected, Timestamp commited, Float memoryUsageMB, Float cpuUsageSec, Float wallClockTimeSec, String log, Long reportId, Set<Long> seriesConfigs) throws SQLException
    {
      m_insertStmt.setTimestamp(1, collected);
      m_insertStmt.setTimestamp(2, commited);
      m_insertStmt.setFloat(3, memoryUsageMB);
      m_insertStmt.setFloat(4, cpuUsageSec);
      m_insertStmt.setFloat(5, wallClockTimeSec);
      m_insertStmt.setString(6, log);
      m_insertStmt.setLong(7, reportId);

      long instanceInfoId = m_insertStmt.update();

      m_mappings.put(instanceInfoId, seriesConfigs);

      if (m_logger.isDebugEnabled())
        m_totalInserts += 1;

      return instanceInfoId;
    }

    /**
     *
     * @param instanceId
     * @param seriesConfigId
     * @throws SQLException
     */
    public void fixReferences(long instanceId, long seriesConfigId) throws SQLException
    {
      m_fixRefsStmt.setLong(1, instanceId);
      m_fixRefsStmt.setLong(2, seriesConfigId);
      m_fixRefsStmt.update();
    }


    // private methods


    /**
     *
     * @throws SQLException
     */
    private void insertConfigInstanceMappings() throws SQLException
    {
      BatchUpdateStatement mappingsStmt = new BatchUpdateStatement(m_dbConn,
        "INSERT INTO " + m_owner.getLinkTableName() + " ( incaseriesconfig_id, incainstance_id ) " +
        "VALUES ( ?, ? )"
      );

      for (Map.Entry<Long, Set<Long>> entry : m_mappings.entrySet()) {
        Long instanceId = entry.getKey();

        for (Long configId : entry.getValue()) {
          mappingsStmt.setLong(1, configId);
          mappingsStmt.setLong(2, instanceId);
          mappingsStmt.update();
        }
      }

      mappingsStmt.close();
    }
  }

  /**
   *
   */
  private class ParseConfigInstanceMappings extends Capture {

    private final Set<Long> m_seriesConfigs;


    // constructors


    /**
     *
     * @param seriesConfigs
     */
    public ParseConfigInstanceMappings(Set<Long> seriesConfigs)
    {
      m_seriesConfigs = seriesConfigs;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id")) {
        Long oldSeriesConfigId = getTextAsLong();
        Long newSeriesConfigId = m_seriesConfigIds.get(oldSeriesConfigId);

        if (newSeriesConfigId != null)
          m_seriesConfigs.add(newSeriesConfigId);
        else
          m_logger.warn("Couldn't find a SeriesConfig record with previous id " + oldSeriesConfigId);
      }
      else if (localName.equals("seriesConfigs")) {
        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id"))
        startTextCapture();
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseInstanceInfo extends Capture {

    private final ParseInstanceInfoRows m_owner;
    private final Set<Long> m_seriesConfigs = new HashSet<Long>();
    private Long m_oldId = null;
    private Timestamp m_collected = null;
    private Timestamp m_commited = null;
    private Float m_memoryUsageMB = null;
    private Float m_cpuUsageSec = null;
    private Float m_wallClockTimeSec = null;
    private String m_log = null;
    private Long m_reportId = null;


    // constructors


    /**
     *
     * @param owner
     */
    public ParseInstanceInfo(ParseInstanceInfoRows owner)
    {
      m_owner = owner;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id"))
        m_oldId = getTextAsLong();
      else if (localName.equals("collected"))
        m_collected = getTextAsTimestamp();
      else if (localName.equals("commited"))
        m_commited = getTextAsTimestamp();
      else if (localName.equals("memoryUsageMB"))
        m_memoryUsageMB = getTextAsFloat();
      else if (localName.equals("cpuUsageSec"))
        m_cpuUsageSec = getTextAsFloat();
      else if (localName.equals("wallClockTimeSec"))
        m_wallClockTimeSec = getTextAsFloat();
      else if (localName.equals("log"))
        m_log = getTextAsString();
      else if (localName.equals("reportId")) {
        Long oldReportId = getTextAsLong();
        Long newReportId = m_reportIds.get(oldReportId);

        if (newReportId != null)
          m_reportId = newReportId;
        else
          m_logger.warn("Couldn't find a Report record with previous id " + oldReportId + " for InstanceInfo record with previous id " + m_oldId);
      }
      else if (localName.equals("instanceInfo")) {
        if (m_reportId != null) {
          long newId = m_owner.addRecord(m_collected, m_commited, m_memoryUsageMB, m_cpuUsageSec, m_wallClockTimeSec, m_log, m_reportId, m_seriesConfigs);
          List<Long> seriesConfigIds = m_latestInstanceIds.get(m_oldId);

          if (seriesConfigIds != null) {
            for (Long seriesConfigId : seriesConfigIds)
              m_owner.fixReferences(newId, seriesConfigId);
          }
        }

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("id") || localName.equals("collected") || localName.equals("commited") ||
          localName.equals("memoryUsageMB") || localName.equals("cpuUsageSec") || localName.equals("wallClockTimeSec") ||
          localName.equals("log") || localName.equals("reportId"))
        startTextCapture();
      else if (localName.equals("seriesConfigs"))
        m_states.addFirst(new ParseConfigInstanceMappings(m_seriesConfigs));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseSeriesInstanceRows implements ParserState {

    // public methods


    /**
     *
     * @param ch
     * @param start
     * @param length
     */
    @Override
    public void characters(char[] ch, int start, int length)
    {
      // do nothing
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
      if (localName.equals("seriesInstanceRows")) {
        m_logger.debug("Finished parsing InstanceInfo records");

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException, SQLException
    {
      if (localName.equals("seriesInstances"))
        m_states.addFirst(new ParseSeriesInstance());
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class ParseSeriesInstance extends Capture {

    private Long m_seriesId = null;
    private String m_instanceTableName = null;
    private String m_linkTableName = null;


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("id")) {
        Long oldSeriesId = getTextAsLong();

        m_seriesId = m_seriesIds.get(oldSeriesId);

        if (m_seriesId != null)
          getTableNames();
        else
          m_logger.warn("Couldn't find a Series record with previous id " + oldSeriesId);
      }
      else if (localName.equals("seriesInstances")) {
        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException, SQLException
    {
      if (localName.equals("id"))
        startTextCapture();
      else if (localName.equals("instanceInfoRows")) {
        m_logger.debug("Parsing InstanceInfo records for Series record " + m_seriesId + "...");

        m_states.addFirst(new ParseInstanceInfoRows(this));
      }
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }

    /**
     *
     * @return
     */
    public String getInstanceTableName()
    {
      return m_instanceTableName;
    }

    /**
     *
     * @return
     */
    public String getLinkTableName()
    {
      return m_linkTableName;
    }


    // private methods


    /**
     *
     * @throws SQLException
     */
    private void getTableNames() throws SQLException
    {
      PreparedStatement selectStmt = m_dbConn.prepareStatement(
          "SELECT incainstancetablename, incalinktablename " +
          "FROM incaseries " +
          "WHERE incaid = ?"
      );
      ResultSet row = null;

      try {
        selectStmt.setLong(1, m_seriesId);

        row = selectStmt.executeQuery();

        if (row.next()) {
          m_instanceTableName = row.getString(1);
          m_linkTableName = row.getString(2);
        }
      }
      finally {
        if (row != null)
          row.close();

        selectStmt.close();
      }
    }
  }

  /**
   *
   */
  private class ParseKbArticleRows extends ParseTableRoot {

    // constructors


    /**
     *
     * @throws SQLException
     */
    public ParseKbArticleRows() throws SQLException
    {
      super("incakbarticle", "incaid", "incaentered, incaerrormsg, incaseries, incareporter, incaauthorname, incaauthoremail, incaarticletitle, incaarticletext");
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("kbArticleRows")) {
        m_insertStmt.close();

        m_logger.debug("Finished parsing " + m_totalInserts + " KbArticle records");

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("kbArticle"))
        m_states.addFirst(new ParseKbArticle(this));
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }

    /**
     *
     * @param entered
     * @param errorMsg
     * @param series
     * @param reporter
     * @param authorName
     * @param authorEmail
     * @param articleTitle
     * @param articleText
     * @return
     * @throws SQLException
     */
    public long addRecord(Timestamp entered, String errorMsg, String series, String reporter, String authorName, String authorEmail, String articleTitle, String articleText) throws SQLException
    {
      m_insertStmt.setTimestamp(1, entered);
      m_insertStmt.setString(2, errorMsg);
      m_insertStmt.setString(3, series);
      m_insertStmt.setString(4, reporter);
      m_insertStmt.setString(5, authorName);
      m_insertStmt.setString(6, authorEmail);
      m_insertStmt.setString(7, articleTitle);
      m_insertStmt.setString(8, articleText);

      long articleId = m_insertStmt.update();

      if (m_logger.isDebugEnabled())
        m_totalInserts += 1;

      return articleId;
    }
  }

  /**
   *
   */
  private class ParseKbArticle extends Capture {

    private final ParseKbArticleRows m_owner;
    private Timestamp m_entered = null;
    private String m_errorMsg = null;
    private String m_series = null;
    private String m_reporter = null;
    private String m_authorName = null;
    private String m_authorEmail = null;
    private String m_articleTitle = null;
    private String m_articleText = null;


    // constructors


    /**
     *
     * @param owner
     */
    public ParseKbArticle(ParseKbArticleRows owner)
    {
      m_owner = owner;
    }


    // public methods


    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @throws SQLException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException, SQLException
    {
      if (localName.equals("entered"))
        m_entered = getTextAsTimestamp();
      else if (localName.equals("errorMsg"))
        m_errorMsg = getTextAsString();
      else if (localName.equals("series"))
        m_series = getTextAsString();
      else if (localName.equals("reporter"))
        m_reporter = getTextAsString();
      else if (localName.equals("authorName"))
        m_authorName = getTextAsString();
      else if (localName.equals("authorEmail"))
        m_authorEmail = getTextAsString();
      else if (localName.equals("articleTitle"))
        m_articleTitle = getTextAsString();
      else if (localName.equals("articleText"))
        m_articleText = getTextAsString();
      else if (localName.equals("kbArticle")) {
        m_owner.addRecord(m_entered, m_errorMsg, m_series, m_reporter, m_authorName, m_authorEmail, m_articleTitle, m_articleText);

        assert m_states.getFirst() == this;

        m_states.removeFirst();
      }
      else
        throw new SAXException("Unexpected end tag for element " + localName);
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
      if (localName.equals("entered") || localName.equals("errorMsg") || localName.equals("series") ||
          localName.equals("reporter") || localName.equals("authorName") || localName.equals("authorEmail") ||
          localName.equals("articleTitle") || localName.equals("articleText"))
        startTextCapture();
      else
        throw new SAXException("Unexpected start tag for element " + localName);
    }
  }

  /**
   *
   */
  private class Start implements ParserState {

    // public methods


    /**
     *
     * @param ch
     * @param start
     * @param length
     */
    @Override
    public void characters(char[] ch, int start, int length)
    {
      // do nothing
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     */
    @Override
    public void endElement(String uri, String localName, String qName)
    {
      // do nothing
    }

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SQLException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SQLException
    {
      if (localName.equals("suiteRows")) {
        m_logger.debug("Parsing Suite records...");

        m_states.addFirst(new ParseSuiteRows());
      }
      else if (localName.equals("argRows")) {
        m_logger.debug("Parsing Arg records...");

        m_states.addFirst(new ParseArgRows());
      }
      else if (localName.equals("argSignatureRows")) {
        m_logger.debug("Parsing ArgSignature records...");

        m_states.addFirst(new ParseArgSignatureRows());
      }
      else if (localName.equals("seriesRows")) {
        m_logger.debug("Parsing Series records...");

        m_states.addFirst(new ParseSeriesRows());
      }
      else if (localName.equals("seriesConfigRows")) {
        m_logger.debug("Parsing SeriesConfig records...");

        m_states.addFirst(new ParseSeriesConfigRows());
      }
      else if (localName.equals("runInfoRows")) {
        m_logger.debug("Parsing RunInfo records...");

        m_states.addFirst(new ParseRunInfoRows());
      }
      else if (localName.equals("reportRows")) {
        m_logger.debug("Parsing Report records...");

        m_states.addFirst(new ParseReportRows());
      }
      else if (localName.equals("comparisonResultRows")) {
        m_logger.debug("Parsing ComparisonResult records...");

        m_states.addFirst(new ParseComparisonResultRows());
      }
      else if (localName.equals("seriesInstanceRows")) {
        m_logger.debug("Parsing InstanceInfo records...");

        m_states.addFirst(new ParseSeriesInstanceRows());
      }
      else if (localName.equals("kbArticleRows")) {
        m_logger.debug("Parsing KbArticle records...");

        m_states.addFirst(new ParseKbArticleRows());
      }
    }
  }


  private static final Logger m_logger = Logger.getLogger(SyncResponseParser.class);

  private final Map<Long, Long> m_suiteIds = new TreeMap<Long, Long>();
  private final Map<Long, Long> m_argIds = new TreeMap<Long, Long>();
  private final Map<Long, Long> m_argSignatureIds = new TreeMap<Long, Long>();
  private final Map<Long, Long> m_seriesIds = new TreeMap<Long, Long>();
  private final Map<Long, Long> m_seriesConfigIds = new TreeMap<Long, Long>();
  private final Map<Long, Long> m_runInfoIds = new TreeMap<Long, Long>();
  private final Map<Long, Long> m_reportIds = new TreeMap<Long, Long>();
  private final Map<Long, Long> m_latestComparisonIds = new TreeMap<Long, Long>();
  private final Map<Long, List<Long>> m_latestInstanceIds = new TreeMap<Long, List<Long>>();
  private final LinkedList<ParserState> m_states = new LinkedList<ParserState>();
  private final Connection m_dbConn;
  private final Depot m_depot;
  private final InsertStatementBuilder m_stmtBuilder;


  // constructors


  /**
   *
   * @param dbConn
   * @param owner
   */
  public SyncResponseParser(Connection dbConn, Depot owner)
  {
    m_dbConn = dbConn;
    m_depot = owner;

    if (DatabaseTools.usesGeneratedKeys())
      m_stmtBuilder = new AutoGenStatementBuilder();
    else
      m_stmtBuilder = new SequenceStatementBuilder();
  }


  // public methods


  /**
   *
   * @param ch
   * @param start
   * @param length
   * @throws SAXException
   */
  @Override
  public void characters(char[] ch, int start, int length) throws SAXException
  {
    m_states.getFirst().characters(ch, start, length);
  }

  /**
   *
   */
  @Override
  public void endDocument()
  {
    m_suiteIds.clear();
    m_argIds.clear();
    m_argSignatureIds.clear();
    m_seriesIds.clear();
    m_seriesConfigIds.clear();
    m_runInfoIds.clear();
    m_reportIds.clear();
    m_latestComparisonIds.clear();
    m_latestInstanceIds.clear();
    m_states.clear();
  }

  /**
   *
   * @param uri
   * @param localName
   * @param qName
   * @throws SAXException
   */
  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException
  {
    try {
      m_states.getFirst().endElement(uri, localName, qName);
    }
    catch (SQLException sqlErr) {
      SQLException nextErr = sqlErr.getNextException();

      while (nextErr != null) {
        m_logger.error(nextErr.getMessage());

        nextErr = nextErr.getNextException();
      }

      throw new SAXException(sqlErr.getMessage());
    }
    catch (IOException | PersistenceException err) {
      throw new SAXException(err);
    }
  }

  /**
   *
   * @throws SAXException
   */
  @Override
  public void startDocument() throws SAXException
  {
    try {
      m_dbConn.setAutoCommit(false);

      m_states.addFirst(new Start());
    }
    catch (SQLException sqlErr) {
      throw new SAXException(sqlErr.getMessage());
    }
  }

  /**
   *
   * @param uri
   * @param localName
   * @param qName
   * @param atts
   * @throws SAXException
   */
  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
  {
    try {
      m_states.getFirst().startElement(uri, localName, qName, atts);
    }
    catch (SQLException sqlErr) {
      throw new SAXException(sqlErr.getMessage());
    }
  }

  /**
   *
   * @param owner
   * @param inStream
   * @throws SQLException
   * @throws IOException
   * @throws SAXException
   */
  public static void parseResponse(Depot owner, InputStream inStream) throws SQLException, IOException, SAXException
  {
    Connection dbConn = ConnectionManager.getConnectionSource().getConnection();

    try {
      parseResponse(dbConn, owner, inStream);
    }
    finally {
      dbConn.close();
    }
  }


  // private methods


  /**
   *
   * @param dbConn
   * @param owner
   * @param inStream
   * @throws IOException
   * @throws SAXException
   */
  private static void parseResponse(Connection dbConn, Depot owner, InputStream inStream) throws IOException, SAXException
  {
    SyncResponseParser handler = new SyncResponseParser(dbConn, owner);
    XMLReader reader = XMLReaderFactory.createXMLReader();

    reader.setContentHandler(handler);
    reader.setDTDHandler(handler);
    reader.setEntityResolver(handler);
    reader.setErrorHandler(handler);

    reader.parse(new InputSource(inStream));
  }
}
