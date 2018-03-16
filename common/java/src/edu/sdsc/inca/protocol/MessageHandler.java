/*
 * MessageHandler.java
 */
package edu.sdsc.inca.protocol;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.Statement;


/**
 * The MessageHandler class should help guide the creation of all of the
 * message handlers for each Inca server.
 */
public abstract class MessageHandler {

	/**
	 *
	 */
	public enum PermitteeGroup {
		STANDARD,
		PEER
	}


	/**
	 *
	 */
	public static abstract class Permittee {

		public final String name;
		public final PermitteeGroup group;


		// constructors


		/**
		 *
		 * @param n
		 * @param g
		 */
		protected Permittee(String n, PermitteeGroup g)
		{
			name = n;
			group = g;
		}


		// protected methods


		/**
		 *
		 * @param action
		 * @return
		 */
		protected abstract boolean grantPermission(String action);

		/**
		 *
		 * @param action
		 * @return
		 */
		protected abstract boolean revokePermission(String action);

		/**
		 *
		 * @param action
		 * @return
		 */
		protected abstract boolean hasPermission(String action);

		/**
		 *
		 * @param perms
		 */
		protected abstract void addPermissions(Map<String, List<String>> perms);
	}

	/**
	 *
	 */
	private static class StandardPermittee extends Permittee {

		private final Set<String> actions = new TreeSet<String>();


		// constructors


		/**
		 *
		 * @param n
		 */
		protected StandardPermittee(String n)
		{
			super(n, PermitteeGroup.STANDARD);
		}


		// protected methods


		/**
		 *
		 * @param action
		 * @return
		 */
		protected boolean grantPermission(String action)
		{
			return actions.add(action);
		}

		/**
		 *
		 * @param action
		 * @return
		 */
		protected boolean revokePermission(String action)
		{
			return actions.remove(action);
		}

		/**
		 *
		 * @param action
		 * @return
		 */
		protected boolean hasPermission(String action)
		{
			return actions.contains(action);
		}

		/**
		 *
		 * @param perms
		 */
		protected void addPermissions(Map<String, List<String>> perms)
		{
			if (actions.isEmpty())
				return;

			for (String value : actions) {
				List<String> names = perms.get(value);

				if (names == null) {
					names = new ArrayList<String>();

					perms.put(value, names);
				}

				names.add(name);
			}
		}
	}

	/**
	 *
	 */
	private static class PeerPermittee extends Permittee {

		// constructors


		/**
		 *
		 * @param n
		 */
		protected PeerPermittee(String n)
		{
			super(n, PermitteeGroup.PEER);
		}


		// protected methods


		/**
		 *
		 * @param action
		 * @return
		 */
		protected boolean grantPermission(String action)
		{
			return true;
		}

		/**
		 *
		 * @param action
		 * @return
		 */
		protected boolean revokePermission(String action)
		{
			return false;
		}

		/**
		 *
		 * @param action
		 * @return
		 */
		protected boolean hasPermission(String action)
		{
			return true;
		}

		/**
		 *
		 * @param perms
		 */
		protected void addPermissions(Map<String, List<String>> perms)
		{
			List<String> names = perms.get(PEER_ACTION);

			if (names == null) {
				names = new ArrayList<String>();

				perms.put(PEER_ACTION, names);
			}

			names.add(name);
		}
	}


	private static final String PEER_ACTION = "PEER";
	private static final String SEPARATOR = "\n";

	/**
	 * logger that can be used by all MessageHandlers.
	 */
	protected static final Logger logger = Logger.getLogger(MessageHandler.class);

	private static final Lock permitteesLock = new ReentrantLock();
	private static final Map<String, Permittee> permittees = new TreeMap<String, Permittee>();
	private static String permissionsPath = null;


	// public methods


	/**
	 * Sets the path where DN permissions should be stored.
	 *
	 * @param path pathname of the permissions file
	 */
	public static void setPermissionsPath(String path)
	{
		permitteesLock.lock();

		try {
			permissionsPath = path;

			loadPermissions();
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 * Indicate that a specified DN may perform a specified action.
	 *
	 * @param name the entity DN
	 * @param action the action to test
	 * @return
	 */
	public static boolean grantPermission(String name, String action)
	{
		if (name == null)
			return true;

		permitteesLock.lock();

		try {
			if (permittees.isEmpty())
				loadPermissions();

			Permittee permit = permittees.get(name);

			if (permit != null) {
				if (permit.hasPermission(action))
					return true;

				if (grantedToMemberOfGroup(action, PermitteeGroup.STANDARD))
					return false;
			}
			else {
				if (grantedToMemberOfGroup(action, PermitteeGroup.STANDARD))
					return false;

				permit = new StandardPermittee(name);

				permittees.put(name, permit);
			}

			logger.debug("Granting permission to '" + action + "' to '" + name + "'");

			permit.grantPermission(action);

			storePermissions();

			return true;
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 * Indicate that a specified DN may no longer perform a specified action.
	 *
	 * @param name the entity DN
	 * @param action the action now forbidden to the DN
	 * @return
	 */
	public static boolean revokePermission(String name, String action)
	{
		if (name == null)
			return false;

		permitteesLock.lock();

		try {
			Permittee permit = permittees.get(name);

			if (permit == null || !permit.revokePermission(action))
				return false;

			logger.debug("Revoked permission to '" + action + "' from '" + name + "'");

			storePermissions();

			return true;
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 * Indicate that a specified DN may no longer perform a specified action.
	 *
	 * @param name the entity DN
	 * @param action the action now forbidden to the DN
	 * @return
	 */
	public static boolean revokeAllPermissions(String action)
	{
		permitteesLock.lock();

		try {
			boolean revoked = false;

			for (Map.Entry<String, Permittee> entry : permittees.entrySet()) {
				if (entry.getValue().revokePermission(action)) {
					logger.debug("Revoked permission to '" + action + "' from '" + entry.getKey() + "'");

					revoked = true;
				}
			}

			if (revoked)
				storePermissions();

			return revoked;
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 *
	 * @param name the entity DN
	 * @return
	 */
	public static boolean grantPeerPermission(String name)
	{
		if (name == null)
			return false;

		permitteesLock.lock();

		try {
			if (permittees.isEmpty())
				loadPermissions();

			if (permittees.containsKey(name))
				return false;

			logger.debug("Granting peer permission to '" + name + "'");

			permittees.put(name, new PeerPermittee(name));

			storePermissions();

			return true;
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 *
	 * @param name
	 * @return
	 */
	public static boolean revokePeerPermission(String name)
	{
		if (name == null)
			return false;

		permitteesLock.lock();

		try {
			Permittee permit = permittees.get(name);

			if (permit == null || permit.group != PermitteeGroup.PEER)
				return false;

			logger.debug("Revoked peer permission from '" + name + "'");

			permittees.remove(name);

			storePermissions();

			return true;
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 * Returns true iff a specified DN may perform a specified action.  All DNs
	 * are allowed to perform all actions by default; specific permission is
	 * required only for actions where some entity has been granted specific
	 * permission.
	 *
	 * @param name the entity DN
	 * @param action the action to test
	 * @return true iff the DN may perform the action
	 */
	public static boolean isPermitted(String name, String action)
	{
		if (name == null) {
			logger.debug("Client anonymous allowed to '" + action + "'; non-ssl");

			return true;
		}

		permitteesLock.lock();

		try {
			if (permittees.isEmpty()) {
				logger.debug("Client '" + name + "' allowed to '" + action + "'; no permissions");

				return true;
			}

			Permittee permit = permittees.get(name);

			if (permit != null && permit.hasPermission(action)) {
				logger.debug("Client '" + name + "' allowed to '" + action + "'; specific permit");

				return true;
			}

			if (grantedToMemberOfGroup(action, PermitteeGroup.STANDARD)) {
				logger.debug("Client '" + name + "' forbidden to '" + action + "'");

				return false;
			}

			logger.debug("Client '" + name + "' allowed to '" + action + "'; global permit");

			return true;
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 * Returns the set of DNs permitted to perform a specified action, or null if
	 * no permissions have been registered for that action.
	 *
	 * @param action the action to test
	 * @return an array of DNs permitted to perform the action, or null if no DNs have been registered for the action
	 */
	public static List<Permittee> getPermittees(String action)
	{
		permitteesLock.lock();

		try {
			List<Permittee> result = new ArrayList<Permittee>();

			for (Map.Entry<String, Permittee> entry : permittees.entrySet()) {
				Permittee permit = entry.getValue();

				if (permit.hasPermission(action))
					result.add(permit);
			}

			if (result.isEmpty())
				return null;

			return result;
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 *
	 * @param group
	 * @return
	 */
	public static List<Permittee> getPermittees(PermitteeGroup group)
	{
		permitteesLock.lock();

		try {
			List<Permittee> result = new ArrayList<Permittee>();

			for (Map.Entry<String, Permittee> entry : permittees.entrySet()) {
				Permittee permit = entry.getValue();

				if (permit.group == group)
					result.add(permit);
			}

			if (result.isEmpty())
				return null;

			return result;
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 *
	 * @return
	 */
	public static List<Permittee> getAllPermittees()
	{
		permitteesLock.lock();

		try {
			if (permittees.isEmpty())
				return null;

			return new ArrayList<Permittee>(permittees.values());
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 *
	 * @return
	 */
	public static String getPermissionsAsXml()
	{
		permitteesLock.lock();

		try {
			StringBuilder result = new StringBuilder();
			Map<String, List<String>> perms = new TreeMap<String, List<String>>();

			for (Permittee permit : permittees.values())
				permit.addPermissions(perms);

			result.append("<permissions>");

			for (Map.Entry<String, List<String>> entry : perms.entrySet()) {
				result.append("<permission><action>");
				result.append(entry.getKey());
				result.append("</action><dns>");

				for (String name : entry.getValue()) {
					result.append("<dn>");
					result.append(name);
					result.append("</dn>");
				}

				result.append("</dns></permission>");
			}

			result.append("</permissions>");

			return result.toString();
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 *
	 * @param xml
	 * @throws ParserConfigurationException
	 * @throws XPathExpressionException
	 * @throws IOException
	 * @throws SAXException
	 */
	public static void setPermissionsFromXml(String xml) throws ParserConfigurationException, XPathExpressionException, SAXException, IOException
	{
		setPermissionsFromXml(xml, null);
	}

	/**
	 *
	 * @param xml
	 * @param localDn
	 * @throws ParserConfigurationException
	 * @throws XPathExpressionException
	 * @throws IOException
	 * @throws SAXException
	 */
	public static void setPermissionsFromXml(String xml, String localDn) throws ParserConfigurationException, XPathExpressionException, SAXException, IOException
	{
		if (xml == null)
			return;

		String dnPath = "dns/dn";

		if (localDn != null)
			dnPath += "[. != '" + localDn + "']";

		permitteesLock.lock();

		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression permissionExpr = xpath.compile("/permissions/permission");
			XPathExpression actionExpr = xpath.compile("action");
			XPathExpression dnExpr = xpath.compile(dnPath);
			Document permsDoc = docBuilder.parse(new InputSource(new StringReader(xml)));
			NodeList permNodes = (NodeList) permissionExpr.evaluate(permsDoc, XPathConstants.NODESET);

			for (int i = 0 ; i < permNodes.getLength() ; i += 1) {
				Node perm = permNodes.item(i);
				String action = (String) actionExpr.evaluate(perm, XPathConstants.STRING);
				NodeList dnNodes = (NodeList) dnExpr.evaluate(perm, XPathConstants.NODESET);

				if (action.equals(PEER_ACTION)) {
					for (int j = 0 ; j < dnNodes.getLength() ; j += 1) {
						String dn = dnNodes.item(j).getTextContent();

						permittees.put(dn, new PeerPermittee(dn));
					}
				}
				else {
					for (int j = 0 ; j < dnNodes.getLength() ; j += 1) {
						String dn = dnNodes.item(j).getTextContent();
						Permittee permit = permittees.get(dn);

						if (permit == null) {
							permit = new StandardPermittee(dn);

							permittees.put(dn, permit);
						}

						permit.grantPermission(action);
					}
				}
			}

			storePermissions();
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 * Removes all permissions--useful for testing.
	 */
	public static void resetPermissions()
	{
		permitteesLock.lock();

		try {
			permittees.clear();

			storePermissions();
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 * Service an incoming request from a specified client DN, using specified
	 * i/o streams.  Close the reader and writer to indicate that the client
	 * connection should be closed.
	 *
	 * @param reader the reader connected to the client
	 * @param output the output stream connected to the client
	 * @param dn the DN of the client, null if no authentication
	 */
	public abstract void execute(ProtocolReader reader, OutputStream output, String dn) throws Exception;

	/**
	 * A convenience method that logs and writes an error message.
	 *
	 * @param output the output stream to use to send the error message
	 * @param msg the error message
	 */
	public static void errorReply(OutputStream output, String msg)
	{
		logger.error(msg);

		try {
			(new ProtocolWriter(output)).write(Statement.getErrorStatement(msg));
		}
		catch(IOException e) {
			logger.error("Unable to send error reply", e);
		}
	}


	// private methods


	/**
	 *
	 * @param action
	 * @param group
	 * @return
	 */
	private static boolean grantedToMemberOfGroup(String action, PermitteeGroup group)
	{
		permitteesLock.lock();

		try {
			for (Map.Entry<String, Permittee> entry : permittees.entrySet()) {
				Permittee permit = entry.getValue();

				if (permit.group == group && permit.hasPermission(action))
					return true;
			}

			return false;
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 * Store the current permission set to a file.
	 */
	private static void storePermissions()
	{
		permitteesLock.lock();

		try {
			if (permissionsPath == null)
				return;

			if (permittees.isEmpty()) {
				(new File(permissionsPath)).delete();

				return;
			}

			Map<String, List<String>> perms = new TreeMap<String, List<String>>();

			for (Permittee permit : permittees.values())
				permit.addPermissions(perms);

			Properties storedPerms = new Properties();

			for (Map.Entry<String, List<String>> entry : perms.entrySet()) {
				StringBuilder propValue = new StringBuilder();
				Iterator<String> names = entry.getValue().iterator();

				assert names.hasNext();

				propValue.append(names.next());

				while (names.hasNext()) {
					propValue.append(SEPARATOR);
					propValue.append(names.next());
				}

				storedPerms.setProperty(entry.getKey(), propValue.toString());
			}

			OutputStream outStream = new FileOutputStream(permissionsPath);

			try {
				storedPerms.store(outStream, null);
			}
			finally {
				outStream.close();
			}
		}
		catch (IOException ioErr) {
			logger.warn("Unable to store permissions: " + ioErr.getMessage());
		}
		finally {
			permitteesLock.unlock();
		}
	}

	/**
	 * Loads permissions from file
	 */
	private static void loadPermissions()
	{
		permitteesLock.lock();

		try {
			if (permissionsPath == null)
				return;

			Properties storedPerms = new Properties();
			InputStream inStream = new FileInputStream(permissionsPath);

			try {
				storedPerms.load(inStream);
			}
			finally {
				inStream.close();
			}

			for (Map.Entry<Object, Object> entry : storedPerms.entrySet()) {
				String action = (String) entry.getKey();
				String[] names = ((String) entry.getValue()).split(SEPARATOR);

				if (action.equals(PEER_ACTION)) {
					for (int i = 0 ; i < names.length ; i += 1)
						permittees.put(names[i], new PeerPermittee(names[i]));
				}
				else {
					for (int i = 0 ; i < names.length ; i += 1) {
						Permittee permit = permittees.get(names[i]);

						if (permit == null) {
							permit = new StandardPermittee(names[i]);

							permittees.put(names[i], permit);
						}

						permit.grantPermission(action);
					}
				}
			}
		}
		catch (IOException ioErr) {
			logger.warn("Unable to load permissions: " + ioErr.getMessage());
		}
		finally {
			permitteesLock.unlock();
		}
	}
}
