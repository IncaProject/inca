/*
 * Permit.java
 */
package edu.sdsc.inca.protocol;


import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;


/**
 * @author Jim Hayes
 *
 * Registers permission for a specified DN to perform a specified action.
 */
public class Permit extends StandardMessageHandler {

	/**
	 * Services an incoming request from a specified client DN, using the
	 * specified i/o streams
	 *
	 * @param reader the reader connected to the client
	 * @param writer the writer connected to the client
	 * @param dn the DN of the client, null if no authentication
	 * @throws Exception
	 */
	public void execute(ProtocolReader reader, ProtocolWriter writer, String dn) throws Exception
	{
		String data = new String(reader.readStatement().getData());
		int namesBegin;
		int namesEnd;

		if (data.startsWith("\"")) {
			namesBegin = 1;
			namesEnd = data.indexOf("\"", 1);
		}
		else if (data.startsWith("'")) {
			namesBegin = 1;
			namesEnd = data.indexOf("'", 1);
		}
		else {
			namesBegin = 0;
			namesEnd = data.indexOf(' ', 1);
		}

		if (namesEnd < 0)
			throw new ProtocolException("Bad message format");

		String[] names = data.substring(namesBegin, namesEnd).split("\\n");
		String action = data.substring(namesEnd + 1).trim();

		for (int i = 0 ; i < names.length ; i += 1) {
			if (!grantPermission(names[i], action))
				throw new ProtocolException("Exclusive permission for " + action + " already registered");
		}

		writer.write(Statement.getOkStatement(""));
	}
}
