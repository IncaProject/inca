/*
 * GssApiGroupClient.java
 */
package com.sshtools.ssh.components.jce;


import java.math.BigInteger;

import com.sshtools.ssh.SshException;
import com.sshtools.ssh.components.ComponentManager;
import com.sshtools.ssh.components.Digest;


/**
 *
 * @author Paul Hoover
 *
 */
public abstract class GssApiConstGroup extends GssApiKeyExchangeClient {

	// data fields


	private static final BigInteger G = BigInteger.valueOf(2);


	// constructors


	protected GssApiConstGroup(String hashAlgorithm)
	{
		super(hashAlgorithm);
	}


	// protected methods


	@Override
	protected BigInteger getG()
	{
		return G;
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

		hash.putBigInteger(e);
		hash.putBigInteger(f);
		hash.putBigInteger(secret);

		return hash.doFinal();
	}
}
