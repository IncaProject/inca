package edu.sdsc.inca.depot.persistent;


import org.apache.log4j.Logger;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.SessionFactory;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;


/**
 * Basic Hibernate helper class, handles SessionFactory, Session and Transaction.
 * <p>
 * Uses a static initializer to read startup options and initialize
 * <tt>Configuration</tt> and <tt>SessionFactory</tt>.
 * <p>
 * This class tries to figure out if either ThreadLocal handling of the
 * <tt>Session</tt> and <tt>Transaction</tt> should be used, for resource local
 * transactions or BMT, or if CMT with automatic <tt>Session</tt> handling is enabled.
 * <p>
 * To keep your DAOs free from any of this, just call <tt>HibernateUtil.getCurrentSession()</tt>
 * in the constructor of each DAO., The recommended way to
 * set resource local or BMT transaction boundaries is an interceptor, or a request filter.
 * <p>
 * This class also tries to figure out if JNDI binding of the <tt>SessionFactory</tt>
 * is used, otherwise it falls back to a global static variable (Singleton).
 * <p>
 * If you want to assign a global interceptor, set its fully qualified
 * class name with the system (or hibernate.properties/hibernate.cfg.xml) property
 * <tt>hibernate.util.interceptor_class</tt>. It will be loaded and instantiated
 * on static initialization of HibernateUtil; it has to have a
 * no-argument constructor. You can call <tt>getInterceptor()</tt> if
 * you need to provide settings before using the interceptor.
 * <p>
 * Note: This class supports annotations by default, hence needs JDK 5.0
 * and the Hibernate Annotations library on the classpath. Change the single
 * commented line in the source to make it compile and run on older JDKs with
 * XML mapping files only.
 *
 * @author christian@hibernate.org
 */
public class HibernateUtil {

  private static Logger log = Logger.getLogger(HibernateUtil.class);

  private static final String INTERCEPTOR_CLASS =
      "hibernate.util.interceptor_class";

  private static Configuration configuration;
  private static SessionFactory sessionFactory;
  private static ThreadLocal<Session> threadSession = new ThreadLocal<Session>();
  private static ThreadLocal<Transaction> threadTransaction = new ThreadLocal<Transaction>();

  private static boolean useThreadLocal = true;

  static {
    // Create the initial SessionFactory from the default configuration files
    try {

      configuration = new Configuration();

      // Read not only hibernate.properties, but also hibernate.cfg.xml
      configuration.configure();

      // Assign a global, user-defined interceptor with no-arg constructor
      String interceptorName = configuration.getProperty(INTERCEPTOR_CLASS);
      if (interceptorName != null) {
        Class interceptorClass =
            HibernateUtil.class.getClassLoader().loadClass(interceptorName);
        Interceptor interceptor = (Interceptor)interceptorClass.newInstance();
        configuration.setInterceptor(interceptor);
      }

      // Disable ThreadLocal Session/Transaction handling if CMT is used
      if (org.hibernate.transaction.CMTTransactionFactory.class.getName()
          .equals( configuration.getProperty(Environment.TRANSACTION_STRATEGY) ) )
        useThreadLocal = false;

      if (configuration.getProperty(Environment.SESSION_FACTORY_NAME) != null) {
        // Let Hibernate bind it to JNDI
        configuration.buildSessionFactory();
      } else {
        // or use static variable handling
        sessionFactory = configuration.buildSessionFactory();
      }

    } catch (Throwable ex) {
      // We have to catch Throwable, otherwise we will miss
      // NoClassDefFoundError and other subclasses of Error
      log.error("Building SessionFactory failed.", ex);
      throw new ExceptionInInitializerError(ex);
    }
  }

  /**
   * Returns the original Hibernate configuration.
   *
   * @return Configuration
   */
  public static Configuration getConfiguration() {
    return configuration;
  }

  /**
   * Returns the global SessionFactory.
   *
   * @return SessionFactory
   */
  public static SessionFactory getSessionFactory() {
    SessionFactory sf = null;
    String sfName = configuration.getProperty(Environment.SESSION_FACTORY_NAME);
    if ( sfName != null) {
      log.debug("Looking up SessionFactory in JNDI.");
      try {
        sf = (SessionFactory) new InitialContext().lookup(sfName);
      } catch (NamingException ex) {
        throw new RuntimeException(ex);
      }
    } else {
      sf = sessionFactory;
    }
    if (sf == null)
      throw new IllegalStateException("SessionFactory not available.");
    return sf;
  }

  /**
   * Closes the current SessionFactory and releases all resources.
   * <p>
   * The only other method that can be called on HibernateUtil
   * after this one is rebuildSessionFactory(Configuration).
   */
  public static void shutdown() {
    log.debug("Shutting down Hibernate.");
    // Close caches and connection pools
    getSessionFactory().close();

    // Clear static variables
    configuration = null;
    sessionFactory = null;

    // Clear ThreadLocal variables
    threadSession.set(null);
    threadTransaction.set(null);
  }


  /**
   * Rebuild the SessionFactory with the static Configuration.
   * <p>
   * This method also closes the old SessionFactory before, if still open.
   * Note that this method should only be used with static SessionFactory
   * management, not with JNDI or any other external registry.
   */
  public static void rebuildSessionFactory() {
    log.debug("Using current Configuration for rebuild.");
    rebuildSessionFactory(configuration);
  }

  /**
   * Rebuild the SessionFactory with the given Hibernate Configuration.
   * <p>
   * HibernateUtil does not configure() the given Configuration object,
   * it directly calls buildSessionFactory(). This method also closes
   * the old SessionFactory before, if still open.
   *
   * @param cfg
   */
  public static void rebuildSessionFactory(Configuration cfg) {
    log.debug("Rebuilding the SessionFactory from given Configuration.");
    synchronized(sessionFactory) {
      if (sessionFactory != null && !sessionFactory.isClosed())
        sessionFactory.close();
      if (cfg.getProperty(Environment.SESSION_FACTORY_NAME) != null)
        cfg.buildSessionFactory();
      else
        sessionFactory = cfg.buildSessionFactory();
      configuration = cfg;
    }
  }

  /**
   * Retrieves the current Session local to the thread.
   * <p/>
   * If no Session is open, opens a new Session for the running thread.
   * If CMT is used, returns the Session bound to the current JTA
   * container transaction. Most other operations on this class will
   * then be no-ops or not supported, the container handles Session
   * and Transaction boundaries, ThreadLocals are not used.
   *
   * @return Session
   */
  public static Session getCurrentSession() {
    if (useThreadLocal) {
      Session s = threadSession.get();
      if (s == null) {
        log.debug("Opening new Session for this thread.");
        s = getSessionFactory().openSession();
        threadSession.set(s);
      }
      return s;
    } else {
      return getSessionFactory().getCurrentSession();
    }
  }

  /**
   * Closes the Session local to the thread.
   * <p>
   * Is a no-op (with warning) if called in a CMT environment. Should be
   * used in non-managed environments with resource local transactions, or
   * with EJBs and bean-managed transactions.
   */
  public static void closeSession() {
    if (useThreadLocal) {
      Session s = threadSession.get();
      threadSession.set(null);
      Transaction tx = threadTransaction.get();
      if (tx != null && (!tx.wasCommitted() || !tx.wasRolledBack()) )
        throw new IllegalStateException("Closing Session but Transaction still open!");
      if (s != null && s.isOpen()) {
        log.debug("Closing Session of this thread.");
        s.close();
      }
    } else {
      log.warn("Using CMT/JTA, intercepted superfluous close call.");
    }
  }

  /**
   * Start a new database transaction.
   * <p>
   * Is a no-op (with warning) if called in a CMT environment. Should be
   * used in non-managed environments with resource local transactions, or
   * with EJBs and bean-managed transactions. In both cases, it will either
   * start a new transaction or join the existing ThreadLocal or JTA
   * transaction.
   */
  public static void beginTransaction() {
    if (useThreadLocal) {
      Transaction tx = threadTransaction.get();
      if (tx == null) {
        log.debug("Starting new database transaction in this thread.");
        tx = getCurrentSession().beginTransaction();
        threadTransaction.set(tx);
      }
    } else {
      log.warn("Using CMT/JTA, intercepted superfluous tx begin call.");
    }
  }

  /**
   * Commit the database transaction.
   * <p>
   * Is a no-op (with warning) if called in a CMT environment. Should be
   * used in non-managed environments with resource local transactions, or
   * with EJBs and bean-managed transactions. It will commit the
   * ThreadLocal or BMT/JTA transaction.
   */
  public static void commitTransaction() {
    if (useThreadLocal) {
      Transaction tx = threadTransaction.get();
      try {
        if ( tx != null && !tx.wasCommitted()
            && !tx.wasRolledBack() ) {
          log.debug("Committing database transaction of this thread.");
          tx.commit();
        }
        threadTransaction.set(null);
      } catch (RuntimeException ex) {
        log.error( "commitTransaction failed", ex);
        rollbackTransaction();
        throw ex;
      }
    } else {
      log.warn("Using CMT/JTA, intercepted superfluous tx commit call.");
    }
  }

  /**
   * Rollback the database transaction.
   * <p>
   * Is a no-op (with warning) if called in a CMT environment. Should be
   * used in non-managed environments with resource local transactions, or
   * with EJBs and bean-managed transactions. It will rollback the
   * resource local or BMT/JTA transaction.
   */
  public static void rollbackTransaction() {
    if (useThreadLocal) {
      Transaction tx = threadTransaction.get();
      try {
        threadTransaction.set(null);
        if ( tx != null && !tx.wasCommitted() && !tx.wasRolledBack() ) {
          log.debug("Tyring to rollback database transaction of this thread.");
          tx.rollback();
          log.debug("Database transaction rolled back.");
        }
      } catch (RuntimeException ex) {
        throw new RuntimeException("Might swallow original cause, check ERROR log!", ex);
      } finally {
        closeSession();
      }
    } else {
      log.warn("Using CMT/JTA, intercepted superfluous tx rollback call.");
    }
  }

  /**
   * Reconnects a Hibernate Session to the current Thread.
   * <p>
   * Unsupported in a CMT environment.
   *
   * @param session The Hibernate Session to be reconnected.
   */
  public static void reconnect(Session session) {
    if (useThreadLocal) {
      log.debug("Reconnecting Session to this thread.");
      session.reconnect();
      threadSession.set(session);
    } else {
      log.error("Using CMT/JTA, intercepted not supported reconnect call.");
    }
  }

  /**
   * Disconnect and return Session from current Thread.
   *
   * @return Session the disconnected Session
   */
  public static Session disconnectSession() {
    if (useThreadLocal) {
      Transaction tx = threadTransaction.get();
      if (tx != null && (!tx.wasCommitted() || !tx.wasRolledBack()) )
        throw new IllegalStateException("Disconnecting Session but Transaction still open!");
      Session session = getCurrentSession();
      threadSession.set(null);
      if (session.isConnected() && session.isOpen()) {
        log.debug("Disconnecting Session from this thread.");
        session.disconnect();
      }
      return session;
    } else {
      log.error("Using CMT/JTA, intercepted not supported disconnect call.");
      return null;
    }
  }

  /**
   * Register a Hibernate interceptor with the current SessionFactory.
   * <p>
   * Every Session opened is opened with this interceptor after
   * registration. Has no effect if the current Session of the
   * thread is already open, effective on next close()/getCurrentSession().
   * <p>
   * Attention: This method effectively restarts Hibernate. If you
   * need an interceptor active on static startup of HibernateUtil, set
   * the <tt>hibernateutil.interceptor</tt> system property to its
   * fully qualified class name.
   */
  public static void registerInterceptorAndRebuild(Interceptor interceptor) {
    log.debug("Setting new global Hibernate interceptor and restarting.");
    configuration.setInterceptor(interceptor);
    rebuildSessionFactory();
  }

  public static Interceptor getInterceptor() {
    return configuration.getInterceptor();
  }

}
