package edu.sdsc.inca.consumer;

import org.apache.log4j.Logger;

import javax.servlet.ServletContext;

import edu.sdsc.inca.Consumer;
import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.AgentClient;
import edu.sdsc.inca.protocol.Protocol;

import java.util.Properties;

/**
 * Thread that waits for the configuration params to be set in the context
 * and then initializes the inca webapp.  This thread allows us to do the
 * webapp initialization in one place irregardless of whether the webapp
 * was started in Jetty or another container.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class WebappInit extends Thread {
  private static Logger logger = Logger.getLogger(WebappInit.class);

  private ServletContext context = null;

  WebappInit(ServletContext context) {
    this.context = context;
  }

  public void run() {
    boolean initParamsFound = false;
    while( ! initParamsFound ) {
      if ( context.getAttribute( Consumer.CONFIG_ID ) != null ) {
        initParamsFound = true;
        logger.debug( "Found init params" );
        Properties initParams =
          (Properties)context.getAttribute( Consumer.CONFIG_ID );
        AgentBean agentBean = new AgentBean();
        DepotQuery.setDepotConfig(initParams);
        agentBean.setBeanConfig(initParams);
        logger.debug( "Starting agent bean" );
        agentBean.start();
        // register run now perms
        try {
          AgentClient ac = new AgentClient();
          ac.setConfiguration( initParams );
          ac.connect();
          ac.commandPermit
            (ac.getDn(false), Protocol.RUNNOW_ACTION+" "+Protocol.RUNNOW_TYPE_CONSUMER);
          ac.close();
        } catch (Exception e) {
          logger.error( "Unable to register run now permissions", e );
        }

        DepotBean depotBean = new DepotBean();
        try {
          depotBean.setBeanConfig( initParams );
          logger.debug( "Starting depot bean" );
          depotBean.start();
        } catch (ConfigurationException e) {
          logger.error( "Problem starting depot bean" );
        }
        TimeSeriesBean.setIgnorePattern(depotBean.getIgnoreErrorPattern());
        context.setAttribute( Consumer.AGENT_BEAN_ID, agentBean );
        context.setAttribute( Consumer.DEPOT_BEAN_ID, depotBean );
      } else {
        logger.debug( "Init params not yet set" );
      }
      try {
        Thread.sleep( 2000 );
      } catch (InterruptedException e) {
        initParamsFound = true;
        logger.warn( "Interrupted while waiting for init params to come in" );
      }
    }
  }
}
