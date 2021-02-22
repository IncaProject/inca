package edu.sdsc.inca.depot.persistent;


import java.util.Date;
import java.util.Iterator;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.Interceptor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;


public class HibernateExplorer {

  private static final int FETCH_SIZE = 100;
  private static final String INTERCEPTOR_CLASS = "hibernate.util.interceptor_class";
  private static Configuration configuration;
  private static SessionFactory sessionFactory;
  private static ThreadLocal<Session> threadSession = new ThreadLocal<Session>();
  private static boolean useThreadLocal = true;


  static {
    try {
      configuration = new Configuration();

      configuration.configure();

      String interceptorName = configuration.getProperty(INTERCEPTOR_CLASS);

      if (interceptorName != null) {
        Class<?> interceptorClass = HibernateExplorer.class.getClassLoader().loadClass(interceptorName);
        Interceptor interceptor = (Interceptor)interceptorClass.getDeclaredConstructor().newInstance();

        configuration.setInterceptor(interceptor);
      }

      if (org.hibernate.transaction.CMTTransactionFactory.class.getName().equals( configuration.getProperty(Environment.TRANSACTION_STRATEGY)))
        useThreadLocal = false;

      if (configuration.getProperty(Environment.SESSION_FACTORY_NAME) != null)
        configuration.buildSessionFactory();
      else
        sessionFactory = configuration.buildSessionFactory();
    }
    catch (Throwable ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }


  public static void main(String[] args)
  {
    try {
      ConnectionManager.setConnectionSource();

      /*
      Object[] params = new Object[3];

      params[0] = new Series(1959);
      params[1] = true;
      params[2] = new RunInfo(8084429);

      Iterator<?> result = selectMultiple("select r, r.series, r.runInfo from Report as r where r.series = :p0 and r.exit_status = :p1 and r.runInfo = :p2", params);
      */
      Object[] params = new Object[1];

      //params[0] = 1231;
      params[0] = "sdsc-comet";

      Iterator<?> result = selectMultiple("select r from Series as s join s.reports as r where s.targetHostname = :p0", params);

      while (result.hasNext()) {
        Object row = result.next();

        if (row.getClass().isArray()) {

        }
        else {

        }
      }
    }
    catch (Exception err) {
      err.printStackTrace(System.err);

      System.exit(-1);
    }
  }


  private static Iterator<?> selectMultiple(String query, Object[] params) throws PersistenceException
  {
    Session session = getCurrentSession();
    Query q = session.createQuery(query);

    q.setFetchSize(FETCH_SIZE);

    if (params != null) {
      for (int i = 0; i < params.length; i++) {
        Object param = params[i];
        String name = "p" + i;
        if (param instanceof Boolean) {
          q.setBoolean(name, ((Boolean)param).booleanValue());
        } else if (param instanceof Date) {
          q.setTimestamp(name, (Date)param);
        } else if (param instanceof Integer) {
          q.setInteger(name, ((Integer)param).intValue());
        } else if (param instanceof Long) {
          q.setLong(name, ((Long)param).longValue());
        } else if (param instanceof String) {
          q.setString(name, (String)param);
        } else {
          q.setEntity(name, param);
        }
      }
    }

    return q.iterate();
  }

  private static SessionFactory getSessionFactory()
  {
    SessionFactory sf = null;
    String sfName = configuration.getProperty(Environment.SESSION_FACTORY_NAME);

    if (sfName != null) {
      try {
        sf = (SessionFactory) new InitialContext().lookup(sfName);
      }
      catch (NamingException ex) {
        throw new RuntimeException(ex);
      }
    }
    else {
      sf = sessionFactory;
    }

    if (sf == null)
      throw new IllegalStateException("SessionFactory not available.");

    return sf;
  }

  private static Session getCurrentSession()
  {
    if (useThreadLocal) {
      Session s = threadSession.get();

      if (s == null) {
        s = getSessionFactory().openSession();

        threadSession.set(s);
      }

      return s;
    }
    else {
      return getSessionFactory().getCurrentSession();
    }
  }
}
