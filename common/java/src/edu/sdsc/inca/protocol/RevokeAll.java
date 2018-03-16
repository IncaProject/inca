/*
 * RevokeAll.java
 */
package edu.sdsc.inca.protocol;


/**
 *
 * @author Paul Hoover
 *
 */
public class RevokeAll extends StandardMessageHandler {

	/**
	 *
	 * @param reader
	 * @param writer
	 * @param dn
	 * @throws Exception
	 */
	public void execute(ProtocolReader reader, ProtocolWriter writer, String dn) throws Exception
	{
		String action = new String(reader.readStatement().getData());

		if(!isPermitted(dn, Protocol.REVOKE_ACTION))
			throw new ProtocolException(Protocol.REVOKE_ACTION + " not allowed by " + dn);

		revokeAllPermissions(action);

		writer.write(Statement.getOkStatement(""));
	}
}
