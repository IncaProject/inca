/*
 * PeerClient.java
 */
package edu.sdsc.inca.depot;


import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.log4j.Logger;

import edu.sdsc.inca.Client;
import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.util.ConfigProperties;


/**
 *
 * @author Paul Hoover
 *
 */
public class DepotPeerClient extends Client {

	public static final String DEPOT_PEER_CLIENT_OPTS =
		ConfigProperties.mergeValidOptions(
			CLIENT_OPTS.replaceFirst(" *S\\|server[^\\n]*\\n", ""),
			"  D|depot  str  Depot specification; host:port\n",
			true);

	private static final Logger m_logger = Logger.getLogger(DepotPeerClient.class);


	// constructors


	/**
	 *
	 */
	public DepotPeerClient()
	{
		// do nothing
	}

	/**
	 *
	 * @param config
	 * @throws ConfigurationException
	 */
	public DepotPeerClient(Properties config) throws ConfigurationException
	{
		setConfiguration(config);
	}


	// public methods


	/**
	 *
	 * @param config
	 * @throws ConfigurationException
	 */
	@Override
	public void setConfiguration(Properties config) throws ConfigurationException
	{
		super.setConfiguration(config);

		String prop = config.getProperty("peer");

		if (prop != null)
			setServer(prop, 0);
	}

	/**
	 *
	 * @param dump
	 * @return
	 * @throws IOException
	 * @throws ProtocolException
	 */
	public String requestSync(boolean dump) throws IOException, ProtocolException
	{
		String command = dump ? Protocol.SYNC_DUMP_COMMAND : Protocol.SYNC_COMMAND;

		writer.write(new Statement(command));

		String reply = reader.peekCommand();

		if (reply == null)
			throw new IOException("Server closed connection");

		if (reply.equals(Protocol.FAILURE_COMMAND)) {
			String error = new String(reader.readStatement().getData());

			throw new ProtocolException(command + " command failed: " + error);
		}

		String fileName = createFileName();

		saveSyncData(fileName);

		reply = reader.peekCommand();

		if (reply != null && reply.equals(Protocol.FAILURE_COMMAND)) {
			String error = new String(reader.readStatement().getData());

			throw new ProtocolException(command + " command failed: " + error);
		}

		return fileName;
	}

	/**
	 *
	 * @param resourceContextHostname
	 * @param stdErr
	 * @param stdOut
	 * @param usage
	 * @return
	 * @throws IOException
	 * @throws ProtocolException
	 */
	public String sendNotifyInsert(String resourceContextHostname, String stdErr, String stdOut, String usage) throws IOException, ProtocolException
	{
		writer.write(new Statement(Protocol.NOTIFY_INSERT_COMMAND, resourceContextHostname));

		if (stdErr != null)
			writer.write(new Statement(Protocol.INSERT_STDERR_COMMAND, stdErr));

		writer.write(new Statement(Protocol.INSERT_STDOUT_COMMAND, stdOut));
		writer.write(new Statement(Protocol.INSERT_SYSUSAGE_COMMAND, usage));

		Statement reply = reader.readStatement();

		if (reply == null)
			throw new IOException("Server closed connection");

		String command = new String(reply.getCmd());
		String data = new String(reply.getData());

		if (command.equals(Protocol.FAILURE_COMMAND))
			throw new ProtocolException(Protocol.NOTIFY_INSERT_COMMAND + " command failed: " + data);

		return data;
	}

	/**
	 *
	 * @param xml
	 * @return
	 * @throws IOException
	 * @throws ProtocolException
	 */
	public String sendNotifySuiteUpdate(String xml) throws IOException, ProtocolException
	{
		return dialog(Protocol.NOTIFY_SUITE_UPDATE_COMMAND, xml);
	}

	/**
	 *
	 * @param xml
	 * @throws IOException
	 * @throws ProtocolException
	 */
	public void sendNotifyKbArticleInsert(String xml) throws IOException, ProtocolException
	{
		dialog(Protocol.NOTIFY_KBARTICLE_INSERT_COMMAND, xml);
	}

	/**
	 *
	 * @param id
	 * @throws IOException
	 * @throws ProtocolException
	 */
	public void sendNotifyKbArticleDelete(String id) throws IOException, ProtocolException
	{
		dialog(Protocol.NOTIFY_KBARTICLE_DELETE_COMMAND, id);
	}

	/**
	 *
	 * @param dn
	 * @param action
	 * @throws IOException
	 * @throws ProtocolException
	 */
	public void sendNotifyPermit(String dn, String action) throws IOException, ProtocolException
	{
		char quote = dn.indexOf('"') < 0 ? '"' : '\'';

		dialog(Protocol.NOTIFY_PERMIT_COMMAND, quote + dn + quote + ' ' + action);
	}

	/**
	 *
	 * @param uri
	 * @return
	 * @throws IOException
	 * @throws ProtocolException
	 */
	public String sendRegisterPeer(String uri) throws IOException, ProtocolException
	{
		return dialog(Protocol.REGISTER_PEER_COMMAND, uri);
	}

	/**
	 * A simple main method for exercising the DepotPeerClient.
	 * <br/>
	 * <pre>
	 * usage: <code>java inca.DepotClient</code><br/>
	 *  -a|--auth     boolean Authenticated (secure) connection?
	 *  -c|--cert     path    Path to the authentication certificate
	 *  -D|--depot    str     Depot specification; host:port
	 *  -h|--help     null    Print help/usage
	 *  -i|--init     path    Path to properties file
	 *  -k|--key      path    Path to the authentication key
	 *  -P|--password str     Specify how to obtain encryption password
	 *  -t|--trusted  path    Path to authentication trusted certificate dir
	 *  -V|--version  null    Display program version
	 * </pre>
	 * <br/>
	 * Each of these properties, other than --init, can also be specified in the
	 * initialization file or in system properties defined via the -D switch.  In
	 * each of these cases, the configuration name must be prefixed with
	 * "inca.depot.", e.g., use "inca.depot.cert" to specify the cert value.
	 * Command line arguments override any properties specified in this fashion.
	 * main allows the user to communicate with the Depot via a telnet-style
	 * interface.  Supported values for the --password argument are: "no" or
	 * "false" (no encryption); "yes", "true", or "stdin:prompt" (read password
	 * from stdin after optionally prompting); "pass:text" (specify the password
	 * directly--should only be used for testing and, possibly, in the init file).
	 * <br/>
	 *
	 * @param args command line arguments, as above.
	 */
	public static void main(String[] args)
	{
		try {
			DepotPeerClient client = new DepotPeerClient();

			configComponent(client, args, DEPOT_PEER_CLIENT_OPTS, "inca.depot.", "edu.sdsc.inca.depot.DepotPeerClient", "inca-common-java-version");

			client.connect();

			telnetDialog(client, "Depot> ");
		}
		catch (Exception err) {
			m_logger.fatal("Client error: ", err);

			err.printStackTrace(System.err);

			System.exit(1);
		}
	}


	// private methods


	/**
	 *
	 * @return
	 */
	private String createFileName()
	{
		Date currentTime = Calendar.getInstance().getTime();
		String dateSuffix = (new SimpleDateFormat("yyyyMMddHHmmssSSS")).format(currentTime);
		String separator = System.getProperty("file.separator");
		String dirName = getTempPath();

		return dirName + separator + "sync-response-" + dateSuffix + ".xml.gz";
	}

	/**
	 *
	 * @param fileName
	 * @return
	 * @throws IOException
	 * @throws ProtocolException
	 */
	private String saveSyncData(String fileName) throws IOException, ProtocolException
	{
		m_logger.debug("Downloading peer DB data to " + fileName);

		try (Writer outStream = new BufferedWriter(new OutputStreamWriter(new Base64OutputStream(new FileOutputStream(fileName), false)))) {
			return reader.readStatement(outStream);
		}
	}
}
