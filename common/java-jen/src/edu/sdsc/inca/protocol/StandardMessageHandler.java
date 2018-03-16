/*
 * StandardMessageHandler.java
 */
package edu.sdsc.inca.protocol;


import java.io.OutputStream;


/**
 * 
 * @author Paul Hoover
 *
 */
public abstract class StandardMessageHandler extends MessageHandler {

	/**
	 * Service an incoming request from a specified client DN, using specified
	 * i/o streams.  Close the reader and writer to indicate that the client
	 * connection should be closed.
	 *
	 * @param reader the reader connected to the client
	 * @param output the output stream connected to the client
	 * @param dn the DN of the client, null if no authentication
	 */
	public void execute(ProtocolReader reader, OutputStream output, String dn) throws Exception
	{
		execute(reader, new ProtocolWriter(output), dn);
	}

	/**
	 * Service an incoming request from a specified client DN, using specified
	 * i/o streams.  Close the reader and writer to indicate that the client
	 * connection should be closed.
	 *
	 * @param reader the reader connected to the client
	 * @param writer the writer connected to the client
	 * @param dn the DN of the client, null if no authentication
	 */
	public abstract void execute(ProtocolReader reader, ProtocolWriter writer, String dn) throws Exception;
}
