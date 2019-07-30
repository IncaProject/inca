/*
 * GssApiKeyexAuthentication.java
 */
package com.sshtools.ssh2;


import java.io.IOException;

import com.sshtools.ssh.SshException;
import com.sshtools.ssh.components.GssApi;
import com.sshtools.util.ByteArrayWriter;


/**
 *
 * @author Paul Hoover
 *
 */
public class GssApiKeyexAuthentication implements AuthenticationClient {

	// data fields


	private static final String METHOD_NAME = "gssapi-keyex";
	private boolean m_implicit;
	private String m_username;


	// constructors


	public GssApiKeyexAuthentication()
	{
		this(true);
	}

	public GssApiKeyexAuthentication(boolean implicit)
	{
		m_implicit = implicit;
	}


	// public methods


	@Override
	public String getMethod()
	{
		return METHOD_NAME;
	}

	@Override
	public String getUsername()
	{
		return m_username;
	}

	@Override
	public void setUsername(String username)
	{
		m_username = username;
	}

	@Override
	public void authenticate(AuthenticationProtocol authentication, String servicename) throws SshException, AuthenticationResult
	{
		try {
			String username;

			if (m_implicit)
				username = null;
			else
				username = m_username;

			ByteArrayWriter writer = new ByteArrayWriter();

			try {
				writer.writeBinaryString(authentication.getKeyExchange().getExchangeHash());
				writer.write(AuthenticationProtocol.SSH_MSG_USERAUTH_REQUEST);
				writer.writeString(username);
				writer.writeString(servicename);
				writer.writeString(METHOD_NAME);

				byte[] mic = GssApi.getMic(authentication.transport.getGssContext(), writer.toByteArray());

				writer.reset();

				writer.write(AuthenticationProtocol.SSH_MSG_USERAUTH_REQUEST);
				writer.writeString(username);
				writer.writeString(servicename);
				writer.writeString(METHOD_NAME);
				writer.writeBinaryString(mic);

				authentication.sendMessage(writer.toByteArray());

				byte[] response = authentication.readMessage();

				throw new SshException("Unexpected message returned from authentication protocol: " + response[0], SshException.PROTOCOL_VIOLATION);
			}
			finally {
				writer.close();
			}
		}
		catch (IOException ioErr) {
			throw new SshException(ioErr);
		}
	}
}
