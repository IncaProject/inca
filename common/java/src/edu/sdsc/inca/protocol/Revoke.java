/*
 * Revoke.java
 */
package edu.sdsc.inca.protocol;


/**
 *
 * @author Paul Hoover
 *
 */
public class Revoke extends StandardMessageHandler {

	/**
	 *
	 * @param reader
	 * @param writer
	 * @param dn
	 * @throws Exception
	 */
	public void execute(ProtocolReader reader, ProtocolWriter writer, String dn) throws Exception
	{
		String data = new String(reader.readStatement().getData());
		int nameBegin;
		int nameEnd;

		if (data.startsWith("\"")) {
			nameBegin = 1;
			nameEnd = data.indexOf("\"", 1);
		}
		else if (data.startsWith("'")) {
			nameBegin = 1;
			nameEnd = data.indexOf("'", 1);
		}
		else {
			nameBegin = 0;
			nameEnd = data.indexOf(' ', 1);
		}

		if (nameEnd < 0)
			throw new ProtocolException("Bad message format");

		String name = data.substring(nameBegin, nameEnd);
		String action = data.substring(nameEnd + 1).trim();

		if(!isPermitted(dn, Protocol.REVOKE_ACTION))
			throw new ProtocolException(Protocol.REVOKE_ACTION + " not allowed by " + dn);

		revokePermission(name, action);

		writer.write(Statement.getOkStatement(""));
	}
}
