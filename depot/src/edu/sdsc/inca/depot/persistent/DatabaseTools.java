/*
 * DatabaseTools.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;


/**
 *
 * @author Paul Hoover
 *
 */
public class DatabaseTools {

  /**
   *
   */
  private enum IdentifierCase {
    LOWER_CASE,
    UPPER_CASE,
    MIXED_CASE
  }


  private static final Logger m_log = Logger.getLogger(DatabaseTools.class);
  public static final String HSQL_DB_NAME = "HSQL Database Engine";
  public static final String MYSQL_DB_NAME = "MySQL";
  public static final String MARIA_DB_NAME = "MariaDB";
  public static final String ORACLE_DB_NAME = "Oracle";
  public static final String POSTGRESQL_DB_NAME = "PostgreSQL";
  private static String PRODUCT_NAME;
  private static String BINARY_TYPE_NAME;
  private static String BOOLEAN_TYPE_NAME;
  private static String DATE_TYPE_NAME;
  private static String FLOAT_TYPE_NAME;
  private static String INTEGER_TYPE_NAME;
  private static String KEY_TYPE_NAME;
  private static String LONG_TYPE_NAME;
  private static String STRING_TYPE_NAME;
  private static String TEXT_TYPE_NAME;
  private static String BOOLEAN_TRUE;
  private static String BOOLEAN_FALSE;
  private static IdentifierCase IDENTIFIER_CASE;
  private static boolean USES_GENERATED_KEYS;


  static {
    try {
      try (Connection dbConn = (new DriverConnectionSource()).getConnection()) {
        DatabaseMetaData dbData = dbConn.getMetaData();

        PRODUCT_NAME = dbData.getDatabaseProductName();

        if (PRODUCT_NAME.equals(HSQL_DB_NAME)) {
          BINARY_TYPE_NAME = "blob";
          BOOLEAN_TYPE_NAME = "boolean";
          DATE_TYPE_NAME = "timestamp";
          FLOAT_TYPE_NAME = "float";
          INTEGER_TYPE_NAME = "integer";
          KEY_TYPE_NAME = "bigint generated by default as identity (start with 1)";
          LONG_TYPE_NAME = "bigint";
          STRING_TYPE_NAME = "varchar";
          TEXT_TYPE_NAME = "longvarchar";
          BOOLEAN_TRUE = "true";
          BOOLEAN_FALSE = "false";
          USES_GENERATED_KEYS = true;
        }
        else if (PRODUCT_NAME.equals(MARIA_DB_NAME)) {
          BINARY_TYPE_NAME = "longblob";
          BOOLEAN_TYPE_NAME = "tinyint(1)";
          DATE_TYPE_NAME = "datetime";
          FLOAT_TYPE_NAME = "float";
          INTEGER_TYPE_NAME = "integer";
          KEY_TYPE_NAME = "bigint auto_increment";
          LONG_TYPE_NAME = "bigint";
          STRING_TYPE_NAME = "varchar";
          TEXT_TYPE_NAME = "longtext";
          BOOLEAN_TRUE = "1";
          BOOLEAN_FALSE = "0";
          USES_GENERATED_KEYS = true;
        }
        else if (PRODUCT_NAME.equals(MYSQL_DB_NAME)) {
          BINARY_TYPE_NAME = "longblob";
          BOOLEAN_TYPE_NAME = "tinyint(1)";
          DATE_TYPE_NAME = "datetime";
          FLOAT_TYPE_NAME = "float";
          INTEGER_TYPE_NAME = "integer";
          KEY_TYPE_NAME = "bigint auto_increment";
          LONG_TYPE_NAME = "bigint";
          STRING_TYPE_NAME = "varchar";
          TEXT_TYPE_NAME = "longtext";
          BOOLEAN_TRUE = "1";
          BOOLEAN_FALSE = "0";
          USES_GENERATED_KEYS = true;
        }
        else if (PRODUCT_NAME.equals(ORACLE_DB_NAME)) {
          BINARY_TYPE_NAME = "blob";
          BOOLEAN_TYPE_NAME = "number(1,0)";
          DATE_TYPE_NAME = "timestamp";
          FLOAT_TYPE_NAME = "float";
          INTEGER_TYPE_NAME = "number(10,0)";
          KEY_TYPE_NAME = "number(19,0)";
          LONG_TYPE_NAME = "number(19,0)";
          STRING_TYPE_NAME = "varchar2";
          TEXT_TYPE_NAME = "clob";
          BOOLEAN_TRUE = "1";
          BOOLEAN_FALSE = "0";
          USES_GENERATED_KEYS = false;
        }
        else if (PRODUCT_NAME.equals(POSTGRESQL_DB_NAME)) {
          BINARY_TYPE_NAME = "bytea";
          BOOLEAN_TYPE_NAME = "bool";
          DATE_TYPE_NAME = "timestamp";
          FLOAT_TYPE_NAME = "float4";
          INTEGER_TYPE_NAME = "int4";
          KEY_TYPE_NAME = "int8";
          LONG_TYPE_NAME = "int8";
          STRING_TYPE_NAME = "varchar";
          TEXT_TYPE_NAME = "text";
          BOOLEAN_TRUE = "true";
          BOOLEAN_FALSE = "false";
          USES_GENERATED_KEYS = false;
        }
        else
          m_log.warn("Unsupported database product " + PRODUCT_NAME);

        if (dbData.storesLowerCaseIdentifiers())
          IDENTIFIER_CASE = IdentifierCase.LOWER_CASE;
        else if (dbData.storesUpperCaseIdentifiers())
          IDENTIFIER_CASE = IdentifierCase.UPPER_CASE;
        else
          IDENTIFIER_CASE = IdentifierCase.MIXED_CASE;
      }
    }
    catch (Exception err) {
      ByteArrayOutputStream logMessage = new ByteArrayOutputStream();

      err.printStackTrace(new PrintStream(logMessage));

      m_log.error(logMessage.toString());
    }
  }


  // public methods


  /**
   *
   * @return
   */
  public static String getDbProductName()
  {
    return PRODUCT_NAME;
  }

  /**
   *
   * @return
   */
  public static String getBinaryTypeName()
  {
    return BINARY_TYPE_NAME;
  }

  /**
   *
   * @return
   */
  public static String getBooleanTypeName()
  {
    return BOOLEAN_TYPE_NAME;
  }

  /**
   *
   * @return
   */
  public static String getDateTypeName()
  {
    return DATE_TYPE_NAME;
  }

  /**
   *
   * @return
   */
  public static String getFloatTypeName()
  {
    return FLOAT_TYPE_NAME;
  }

  /**
   *
   * @return
   */
  public static String getIntegerTypeName()
  {
    return INTEGER_TYPE_NAME;
  }

  /**
   *
   * @return
   */
  public static String getKeyTypeName()
  {
    return KEY_TYPE_NAME;
  }

  /**
   *
   * @return
   */
  public static String getLongTypeName()
  {
    return LONG_TYPE_NAME;
  }

  /**
   *
   * @return
   */
  public static String getStringTypeName()
  {
    return STRING_TYPE_NAME;
  }

  /**
   *
   * @return
   */
  public static String getTextTypeName()
  {
    return TEXT_TYPE_NAME;
  }

  /**
   *
   * @return
   */
  public static String getBooleanTrue()
  {
    return BOOLEAN_TRUE;
  }

  /**
   *
   * @return
   */
  public static String getBooleanFalse()
  {
    return BOOLEAN_FALSE;
  }

  /**
   *
   * @return
   */
  public static boolean usesGeneratedKeys()
  {
    return USES_GENERATED_KEYS;
  }

  /**
   *
   * @param sequenceName
   * @return
   */
  public static String getNextValuePhrase(String sequenceName)
  {
    if (PRODUCT_NAME.equals(HSQL_DB_NAME))
      return "CALL NEXT VALUE FOR " + sequenceName;
    else if (PRODUCT_NAME.equals(ORACLE_DB_NAME))
      return "SELECT " + sequenceName + ".NEXTVAL FROM DUAL";
    else if (PRODUCT_NAME.equals(POSTGRESQL_DB_NAME))
      return "SELECT NEXTVAL('" + sequenceName + "')";
    else
      return null;
  }

  /**
   *
   * @param dbConn
   * @param table
   * @return
   * @throws SQLException
   */
  public static boolean tableExists(Connection dbConn, String table) throws SQLException
  {
    DatabaseMetaData dbData = dbConn.getMetaData();
    String tableName = convertCase(table);

    try (ResultSet row = dbData.getTables(null, null, tableName, null)) {
      return row.next();
    }
  }

  /**
   *
   * @param dbConn
   * @param table
   * @param column
   * @return
   * @throws SQLException
   */
  public static boolean columnExists(Connection dbConn, String table, String column) throws SQLException
  {
    DatabaseMetaData dbData = dbConn.getMetaData();
    String tableName = convertCase(table);
    String columnName = convertCase(column);

    try (ResultSet row = dbData.getColumns(null, null, tableName, columnName)) {
      return row.next();
    }
  }

  /**
   *
   * @param dbConn
   * @param table
   * @param column
   * @param type
   * @return
   * @throws SQLException
   */
  public static boolean columnIsType(Connection dbConn, String table, String column, String type) throws SQLException
  {
    DatabaseMetaData dbData = dbConn.getMetaData();
    String tableName = convertCase(table);
    String columnName = convertCase(column);

    try (ResultSet row = dbData.getColumns(null, null, tableName, columnName)) {
      if (!row.next())
        return false;

      String typeName = row.getString(6);

      return typeName.equalsIgnoreCase(type);
    }
  }

  /**
   * Initialize the Inca database.
   *
   * @throws HibernateException on database error
   * @throws SQLException
   */
  public static void initializeDatabase() throws HibernateException, SQLException
  {
    Configuration cfg = new Configuration();

    cfg.configure();

    SchemaExport export = new SchemaExport(cfg);

    export.create(false, true);

    if (!DatabaseTools.usesGeneratedKeys()) {
      try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
        dbConn.setAutoCommit(false);

        try (Statement createStmt = dbConn.createStatement()) {
          createStmt.execute("CREATE SEQUENCE INCAARG_incaid_seq START WITH 1");
          createStmt.execute("CREATE SEQUENCE INCAARGSIGNATURE_incaid_seq START WITH 1");
          createStmt.execute("CREATE SEQUENCE INCACOMPARISONRESULT_incaid_seq START WITH 1");
          createStmt.execute("CREATE SEQUENCE INCAKBARTICLE_incaid_seq START WITH 1");
          createStmt.execute("CREATE SEQUENCE INCAREPORT_incaid_seq START WITH 1");
          createStmt.execute("CREATE SEQUENCE INCARUNINFO_incaid_seq START WITH 1");
          createStmt.execute("CREATE SEQUENCE INCASERIES_incaid_seq START WITH 1");
          createStmt.execute("CREATE SEQUENCE INCASERIESCONFIG_incaid_seq START WITH 1");
          createStmt.execute("CREATE SEQUENCE INCASUITE_incaid_seq START WITH 1");

          dbConn.commit();
        }
      }
    }
  }

  /**
   * Delete the Inca database.
   *
   * @throws HibernateException on database error
   * @throws SQLException
   */
  public static void removeDatabase() throws HibernateException, SQLException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
      dbConn.setAutoCommit(false);

      List<String> tableNames = getTableNames(dbConn, "incaseriesconfigsinstances_");

      dropTables(dbConn, tableNames);

      tableNames = getTableNames(dbConn, "incainstanceinfo_");

      dropTables(dbConn, tableNames);

      if (!DatabaseTools.usesGeneratedKeys()) {
        tableNames.add("INCAARG");
        tableNames.add("INCAARGSIGNATURE");
        tableNames.add("INCACOMPARISONRESULT");
        tableNames.add("INCAKBARTICLE");
        tableNames.add("INCAREPORT");
        tableNames.add("INCARUNINFO");
        tableNames.add("INCASERIES");
        tableNames.add("INCASERIESCONFIG");
        tableNames.add("INCASUITE");

        dropSequences(dbConn, tableNames);
      }
    }

    Configuration cfg = new Configuration();

    cfg.configure();

    SchemaExport export = new SchemaExport(cfg);

    export.drop(false, true);
  }


  // private methods


  /**
   *
   * @param value
   * @return
   */
  private static String convertCase(String value)
  {
    if (IDENTIFIER_CASE.equals(IdentifierCase.LOWER_CASE))
      return value.toLowerCase();
    if (IDENTIFIER_CASE.equals(IdentifierCase.UPPER_CASE))
      return value.toUpperCase();
    else
      return value;
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  private static List<String> getTableNames(Connection dbConn, String prefix) throws SQLException
  {
    DatabaseMetaData dbData = dbConn.getMetaData();
    String tablePrefix = convertCase(prefix);

    try (ResultSet row = dbData.getTables(null, null, tablePrefix + "%", null)) {
      List<String> result = new ArrayList<String>();

      while (row.next()) {
        if (row.getString(4).equalsIgnoreCase("TABLE"))
          result.add(row.getString(3));
      }

      return result;
    }
  }

  /**
   *
   * @param dbConn
   * @param names
   * @throws SQLException
   */
  private static void dropTables(Connection dbConn, List<String> names) throws SQLException
  {
    try (Statement dropStmt = dbConn.createStatement()) {
      for (String name : names) {
        dropStmt.executeUpdate("DROP TABLE " + name + " CASCADE");

        dbConn.commit();
      }
    }
  }

  /**
   *
   * @param dbConn
   * @param names
   * @throws SQLException
   */
  private static void dropSequences(Connection dbConn, List<String> names) throws SQLException
  {
    try (Statement dropStmt = dbConn.createStatement()) {
      for (String name : names) {
        dropStmt.executeUpdate("DROP SEQUENCE " + name + "_incaid_seq CASCADE");

        dbConn.commit();
      }
    }
  }
}
