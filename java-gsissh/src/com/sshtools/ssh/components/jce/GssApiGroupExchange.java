/*
 * GssApiGroupExchange.java
 */
package com.sshtools.ssh.components.jce;


import java.io.IOException;
import java.math.BigInteger;

import com.sshtools.logging.Log;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.components.ComponentManager;
import com.sshtools.ssh.components.Digest;
import com.sshtools.ssh2.TransportProtocol;
import com.sshtools.util.ByteArrayReader;
import com.sshtools.util.ByteArrayWriter;


/**
 *
 * @author Paul Hoover
 *
 */
public abstract class GssApiGroupExchange extends GssApiKeyExchangeClient {

	// data fields


	private static final int SSH_MSG_KEXGSS_GROUPREQ = 40;
	private static final int SSH_MSG_KEXGSS_GROUP = 41;
	private static final int MIN_GROUP_SIZE = 2048;
	private static final int PREFERRED_GROUP_SIZE = 3072;
	private static final int MAX_GROUP_SIZE = 8192;
	private BigInteger m_p;
	private BigInteger m_g;


	// constructors


	protected GssApiGroupExchange(String hashAlgorithm)
	{
		super(hashAlgorithm);
	}


	// public methods


	@Override
	public boolean isKeyExchangeMessage(int messageId)
	{
		if (messageId == SSH_MSG_KEXGSS_GROUPREQ || messageId == SSH_MSG_KEXGSS_GROUP)
			return true;

		return super.isKeyExchangeMessage(messageId);
	}

	@Override
	public void performClientExchange(String clientId, String serverId, byte[] clientKexInit, byte[] serverKexInit) throws SshException
	{
		try {
			Log.debug(this, "Sending SSH_MSG_KEXGSS_GROUPREQ");

			ByteArrayWriter writer = new ByteArrayWriter();

			try {
				writer.write(SSH_MSG_KEXGSS_GROUPREQ);
				writer.writeInt(MIN_GROUP_SIZE);
				writer.writeInt(PREFERRED_GROUP_SIZE);
				writer.writeInt(MAX_GROUP_SIZE);

				transport.sendMessage(writer.toByteArray(), true);
			}
			finally {
				writer.close();
			}

			byte[] message = transport.nextMessage();

			if (message[0] != SSH_MSG_KEXGSS_GROUP) {
				String errorMessage = "Key exchange failed: expected SSH_MSG_KEXGSS_GROUP, got " + message[0];

				transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, errorMessage);

				throw new SshException(errorMessage, SshException.INTERNAL_ERROR);
			}

			Log.debug(this, "Received SSH_MSG_KEXGSS_GROUP");

			ByteArrayReader reader = new ByteArrayReader(message, 1, message.length - 1);

			try {
				m_p = reader.readBigInteger();
				m_g = reader.readBigInteger();
			}
			finally {
				reader.close();
			}

			if (m_p.bitLength() < MIN_GROUP_SIZE || m_p.bitLength() > MAX_GROUP_SIZE) {
				String errorMessage = "Key exchange failed: GEX group out of range: " + m_p.bitLength();

				transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, errorMessage);

				throw new SshException(errorMessage, SshException.INTERNAL_ERROR);
			}
		}
		catch (IOException ioErr) {
			throw new SshException(ioErr);
		}

		super.performClientExchange(clientId, serverId, clientKexInit, serverKexInit);
	}


	// protected methods


	@Override
	protected BigInteger getP()
	{
		return m_p;
	}

	@Override
	protected BigInteger getG()
	{
		return m_g;
	}

	@Override
	protected byte[] calculateExchangeHash(String clientId, String serverId, byte[] clientKexInit, byte[] serverKexInit, BigInteger e, BigInteger f) throws SshException
	{
		Digest hash = (Digest)ComponentManager.getInstance().supportedDigests().getInstance(getHashAlgorithm());

		hash.putString(clientId);
		hash.putString(serverId);
		hash.putInt(clientKexInit.length);
		hash.putBytes(clientKexInit);
		hash.putInt(serverKexInit.length);
		hash.putBytes(serverKexInit);

		if (hostKey == null)
			hash.putInt(0);
		else {
			hash.putInt(hostKey.length);
			hash.putBytes(hostKey);
		}

		hash.putInt(MIN_GROUP_SIZE);
		hash.putInt(PREFERRED_GROUP_SIZE);
		hash.putInt(MAX_GROUP_SIZE);
		hash.putBigInteger(getP());
		hash.putBigInteger(getG());
		hash.putBigInteger(e);
		hash.putBigInteger(f);
		hash.putBigInteger(secret);

		return hash.doFinal();
	}
}
