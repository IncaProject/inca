/*
 * KbArticleDelete.java
 */
package edu.sdsc.inca.depot.commands;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.Depot;
import edu.sdsc.inca.depot.DelayedWork;
import edu.sdsc.inca.depot.DepotPeerClient;
import edu.sdsc.inca.depot.persistent.KbArticle;
import edu.sdsc.inca.depot.util.HibernateMessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.util.WorkItem;
import edu.sdsc.inca.util.Worker;


/**
 *
 * @author Paul Hoover
 *
 */
public class KbArticleDelete extends HibernateMessageHandler implements DelayedWork {

  /**
   *
   */
  private static class NotifyKbArticleDelete implements WorkItem<Worker> {

    private final Properties m_peerConfig;
    private final long m_id;


    /**
     *
     * @param config
     * @param id
     */
    public NotifyKbArticleDelete(Properties config, long id)
    {
      m_peerConfig = config;
      m_id = id;
    }


    /**
     *
     * @param context
     * @throws ConfigurationException
     */
    @Override
    public void doWork(Worker context) throws ConfigurationException
    {
      DepotPeerClient peer = new DepotPeerClient(m_peerConfig);

      try {
        peer.connect();
        peer.sendNotifyKbArticleDelete(String.valueOf(m_id));
      }
      catch (Exception err) {
        m_logger.warn("Unable to send " + Protocol.NOTIFY_KBARTICLE_DELETE_COMMAND + " command to " + peer.getUri() + ": " + err.getMessage());
      }
      finally {
        if (peer.isConnected())
          peer.close();
      }
    }
  }


  private static final Logger m_logger = Logger.getLogger(KbArticleDelete.class);
  private long m_articleId;


  /**
   *
   * @param reader
   * @param writer
   * @param dn
   * @throws Exception
   */
  @Override
  public void executeHibernateAction(ProtocolReader reader, ProtocolWriter writer, String dn) throws Exception
  {
    Statement stmt = reader.readStatement();

    m_articleId = Long.parseLong(new String(stmt.getData()));

    writer.write(Statement.getOkStatement(""));

    if ((new String(stmt.getCmd())).equals(Protocol.KBARTICLE_DELETE_COMMAND)) {
      for (Properties config : Depot.getRunningDepot().getPeerConfigs())
        Depot.getRunningDepot().addWork(new NotifyKbArticleDelete(config, m_articleId));
    }

    if (Depot.getRunningDepot().syncInProgress()) {
      if (Depot.getRunningDepot().addDelayedWork(this))
        return;
    }

    (new KbArticle(m_articleId)).delete();
  }

  /**
   *
   * @param state
   */
  @Override
  public void loadState(String state)
  {
    m_articleId = Long.parseLong(state);
  }

  /**
   *
   * @return
   */
  @Override
  public String getState()
  {
    return String.valueOf(m_articleId);
  }

  /**
   *
   * @param context
   */
  @Override
  public void doWork(Worker context)
  {
    try {
      (new KbArticle(m_articleId)).delete();
    }
    catch (Exception err) {
      ByteArrayOutputStream logMessage = new ByteArrayOutputStream();

      err.printStackTrace(new PrintStream(logMessage));

      m_logger.error(logMessage.toString());
    }
  }
}
