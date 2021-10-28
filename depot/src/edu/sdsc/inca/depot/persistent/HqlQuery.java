/*
 * HqlQuery.java
 */
package edu.sdsc.inca.depot.persistent;


import java.util.Collections;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.query.HQLQueryPlan;
import org.hibernate.hql.ParameterTranslations;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.type.EntityType;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;


/**
 *
 * @author Paul Hoover
 *
 */
public class HqlQuery {

  // data fields


  private static final int FETCH_SIZE = 100;
  private static final Logger m_log = Logger.getLogger(HqlQuery.class);
  private static SessionFactoryImplementor m_sessionFactory;
  private final HQLQueryPlan m_queryPlan;


  static {
    try {
      Configuration configuration = new Configuration();

      configuration.configure();

      m_sessionFactory = (SessionFactoryImplementor)configuration.buildSessionFactory();
    }
    catch (Throwable err) {
      throw new ExceptionInInitializerError(err);
    }
  }


  // constructors


  /**
   *
   * @param query
   */
  public HqlQuery(String query)
  {
    m_queryPlan = new HQLQueryPlan(query, true, Collections.EMPTY_MAP, m_sessionFactory);
  }


  // public methods


  /**
   *
   * @return
   * @throws SQLException
   */
  public List<Object> select() throws SQLException
  {
    return select((Map<String, Object>)null);
  }

  /**
   *
   * @param params
   * @return
   * @throws SQLException
   */
  public List<Object> select(Object[] params) throws SQLException
  {
    Map<String, Object> paramMap = createParamMap(params);

    return select(paramMap);
  }

  /**
   *
   * @param params
   * @return
   * @throws SQLException
   */
  public List<Object> select(Map<String, Object> params) throws SQLException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
      dbConn.setAutoCommit(false);

      Iterator<Object> resultList = select(dbConn, params);
      List<Object> result = new ArrayList<Object>();

      while (resultList.hasNext()) {
        Object row = resultList.next();

        result.add(row);
      }

      return result;
    }
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  public Iterator<Object> select(Connection dbConn) throws SQLException
  {
    return select(dbConn, (Map<String, Object>)null);
  }

  /**
   *
   * @param dbConn
   * @param params
   * @return
   * @throws SQLException
   */
  public Iterator<Object> select(Connection dbConn, Object[] params) throws SQLException
  {
    Map<String, Object> paramMap = createParamMap(params);

    return select(dbConn, paramMap);
  }

  /**
   *
   * @param dbConn
   * @param params
   * @return
   * @throws SQLException
   */
  public Iterator<Object> select(Connection dbConn, Map<String, Object> params) throws SQLException
  {
    SessionImplementor session = (SessionImplementor)m_sessionFactory.openTemporarySession();
    QueryTranslator[] translators = m_queryPlan.getTranslators();
    Iterator<Object> result;

    if (translators.length == 1)
      result = executeSelect(dbConn, params, translators[0], session);
    else {
      List<Iterator<Object>> resultList = new ArrayList<Iterator<Object>>();

      for (QueryTranslator translator : translators) {
        Iterator<Object> selectResult = executeSelect(dbConn, params, translator, session);

        resultList.add(selectResult);
      }

      result = new CombinedIterator(resultList);
    }

    return result;
  }

  /**
   *
   * @return
   * @throws SQLException
   */
  public Object selectUnique() throws SQLException
  {
    return selectUnique((Map<String, Object>)null);
  }

  /**
   *
   * @param params
   * @return
   * @throws SQLException
   */
  public Object selectUnique(Object[] params) throws SQLException
  {
    Map<String, Object> paramMap = createParamMap(params);

    return selectUnique(paramMap);
  }

  /**
   *
   * @param params
   * @return
   * @throws SQLException
   */
  public Object selectUnique(Map<String, Object> params) throws SQLException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
      Iterator<Object> resultList = select(dbConn, params);

      if (!resultList.hasNext())
        return null;

      Object result = resultList.next();

      if (resultList.hasNext())
        m_log.warn("Unique query returned more than one object");

      return result;
    }
  }


  // private methods


  /*
   *
   */
  private Map<String, Object> createParamMap(Object[] params)
  {
    if (params == null)
      return null;

    Map<String, Object> result = new HashMap<String, Object>();

    for (int index = 0 ; index < params.length ; index += 1) {
      String key = "p" + index;

      result.put(key, params[index]);
    }

    return result;
  }

  /*
   *
   */
  private Iterator<Object> executeSelect(Connection dbConn, Map<String, Object> params, QueryTranslator translator, SessionImplementor session) throws SQLException
  {
    String query = translator.getSQLString();
    ParameterTranslations translations = translator.getParameterTranslations();
    PreparedStatement selectStmt = dbConn.prepareStatement(query);

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      for (Object name : translations.getNamedParameterNames()) {
        if (params == null || !params.containsKey(name))
          throw new SQLException("Missing named parameter " + name);

        Object value = params.get(name);
        Type paramType = translations.getNamedParameterExpectedType((String)name);
        int[] locations = translations.getNamedParameterSqlLocations((String)name);

        if (paramType instanceof EntityType) {
          if (value != null) {
            GeneratedKeyRow entity = (GeneratedKeyRow)value;

            value = entity.getId();
          }

          paramType = LongType.INSTANCE;
        }

        for (int location : locations)
          paramType.nullSafeSet(selectStmt, value, location + 1, session);
      }

      ResultSet rows = selectStmt.executeQuery();

      return new HqlIterator(selectStmt, rows, translator, session);
    }
    catch (SQLException sqlErr) {
      selectStmt.close();

      throw sqlErr;
    }
  }
}
