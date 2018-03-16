package edu.sdsc.inca.consumer;

import org.apache.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import java.util.Enumeration;
import java.util.Properties;

import edu.sdsc.inca.Consumer;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class WebappListener implements ServletContextListener {
  private static Logger logger = Logger.getLogger(WebappListener.class);


  public void contextInitialized( ServletContextEvent sce) {

    ServletContext context = sce.getServletContext();
    logger.debug( "Setting up application beans" );

    // Generate property list from webapp init params
    Properties initParams = new Properties();
    for(Enumeration e = context.getInitParameterNames(); e.hasMoreElements();) {
      String initName = (String)e.nextElement();
      logger.debug( "Found init param: " + initName );
      initParams.put( initName, context.getInitParameter(initName) );
    }

    if ( initParams.size() > 0 && initParams.containsKey("agent") &&
          initParams.containsKey("depot") ) {
      context.setAttribute( Consumer.CONFIG_ID, initParams );
    }
    WebappInit initializer = new WebappInit( context );
    initializer.start();
  }

  public void contextDestroyed( ServletContextEvent sce) {
    ServletContext context = sce.getServletContext();
    logger.debug( "Shutting down application beans" );

    String[] beans = { Consumer.AGENT_BEAN_ID, Consumer.DEPOT_BEAN_ID };
    for( String s : beans ) {
      Thread threadedBean = (Thread)context.getAttribute( s );
      if ( threadedBean == null ) {
        logger.warn( "Unable to locate bean '" + s + "'" );        
        continue;
      }
      logger.debug( "Interrupting " + threadedBean.getName() );
      threadedBean.interrupt();
      try {
        logger.debug( "Waiting for " + threadedBean.getName() + " to shutdown");
        threadedBean.join();
        logger.debug( "Shutdown complete for " + threadedBean.getName() );        

      } catch (InterruptedException e) {
        logger.warn( "Interrupted during shutdown of config bean: " + s );
      }
    }

  }

}
