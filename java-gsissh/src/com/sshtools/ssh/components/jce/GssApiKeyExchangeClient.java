/*
 * GssApiKeyExchangeClient.java
 */
package com.sshtools.ssh.components.jce;


import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.Base64;

import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

import com.sshtools.logging.Log;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.components.GssApi;
import com.sshtools.ssh.components.SshKeyExchangeClient;
import com.sshtools.ssh2.TransportProtocol;
import com.sshtools.util.ByteArrayReader;
import com.sshtools.util.ByteArrayWriter;


/**
 *
 * @author Paul Hoover
 *
 */
public abstract class GssApiKeyExchangeClient extends SshKeyExchangeClient {

	// data fields


	protected static final Oid GSS_MECH_GLOBUS_GSSAPI_OPENSSL;
	protected static final Oid GSS_MECH_GLOBUS_GSSAPI_OPENSSL_MICV2;
	protected static final String GSS_MECH_GLOBUS_GSSAPI_OPENSSL_HASH;
	protected static final String GSS_MECH_GLOBUS_GSSAPI_OPENSSL_MICV2_HASH;
	private static final int SSH_MSG_KEXGSS_INIT = 30;
	private static final int SSH_MSG_KEXGSS_CONTINUE = 31;
	private static final int SSH_MSG_KEXGSS_COMPLETE = 32;
	private static final int SSH_MSG_KEXGSS_HOSTKEY = 33;
	private static final int SSH_MSG_KEXGSS_ERROR = 34;

	static {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			Base64.Encoder encoder = Base64.getEncoder();

			GSS_MECH_GLOBUS_GSSAPI_OPENSSL = new Oid("1.3.6.1.4.1.3536.1.1");
			GSS_MECH_GLOBUS_GSSAPI_OPENSSL_HASH = encoder.encodeToString(digest.digest(GSS_MECH_GLOBUS_GSSAPI_OPENSSL.getDER()));

			digest.reset();

			GSS_MECH_GLOBUS_GSSAPI_OPENSSL_MICV2 = new Oid("1.3.6.1.4.1.3536.1.1.1");
			GSS_MECH_GLOBUS_GSSAPI_OPENSSL_MICV2_HASH = encoder.encodeToString(digest.digest(GSS_MECH_GLOBUS_GSSAPI_OPENSSL_MICV2.getDER()));
		}
		catch (GSSException | NoSuchAlgorithmException err) {
			throw new RuntimeException(err);
		}
	}


	// constructors


	protected GssApiKeyExchangeClient(String hashAlgorithm)
	{
		super(hashAlgorithm);
	}


	// public methods


	@Override
	public boolean isKeyExchangeMessage(int messageId)
	{
		if (messageId == SSH_MSG_KEXGSS_INIT || messageId == SSH_MSG_KEXGSS_CONTINUE || messageId == SSH_MSG_KEXGSS_COMPLETE || messageId == SSH_MSG_KEXGSS_HOSTKEY || messageId == SSH_MSG_KEXGSS_ERROR)
			return true;

		return false;
	}

	@Override
	public void performClientExchange(String clientId, String serverId, byte[] clientKexInit, byte[] serverKexInit) throws SshException
	{
		GssApi.GssContext context = new GssApi.GssContext();

		try {
			KeyPair dhPair = generateKeyPair();
			BigInteger e = ((DHPublicKey)dhPair.getPublic()).getY();
			BigInteger f = null;
			String hostname = transport.getProvider().getHost();
			GssApi.InitReply reply = null;
			byte[] inputToken = null;
			byte[] mic = null;
			boolean firstMessage = true;
			int messageType = 0;

			transport.setIgnoreHostKeyifEmpty(true);

			do {
				Log.debug(this, "Calling GssApi.initSecContext");

				reply = GssApi.initSecContext(context, getOid(), hostname, inputToken);

				if (reply.status == GssApi.GSS_S_COMPLETE) {
					if ((reply.flags & GssApi.GSS_C_MUTUAL_FLAG) == 0)
						throw new SshException("Mutual authentication failed", SshException.INTERNAL_ERROR);

					if ((reply.flags & GssApi.GSS_C_INTEG_FLAG) == 0)
						throw new SshException("Integrity check failed", SshException.INTERNAL_ERROR);
				}
				else if (reply.status != GssApi.GSS_S_CONTINUE_NEEDED) {
					if (reply.token.length > 0) {
						ByteArrayWriter writer = new ByteArrayWriter();

						try {
							writer.write(SSH_MSG_KEXGSS_CONTINUE);
							writer.writeBinaryString(reply.token);

							transport.sendMessage(writer.toByteArray(), true);
						}
						finally {
							writer.close();
						}
					}

					throw new SshException("GssApi.initSecContext failed with status " + reply.status, SshException.INTERNAL_ERROR);
				}

				inputToken = null;

				if (reply.token.length > 0) {
					ByteArrayWriter writer = new ByteArrayWriter();

					try {
						if (firstMessage) {
							Log.debug(this, "Sending SSH_MSG_KEXGSS_INIT");

							writer.write(SSH_MSG_KEXGSS_INIT);
							writer.writeBinaryString(reply.token);
							writer.writeBigInteger(e);

							firstMessage = false;
						}
						else {
							Log.debug(this, "Sending SSH_MSG_KEXGSS_CONTINUE");

							writer.write(SSH_MSG_KEXGSS_CONTINUE);
							writer.writeBinaryString(reply.token);
						}

						transport.sendMessage(writer.toByteArray(), true);
					}
					finally {
						writer.close();
					}

					while (true) {
						byte[] message = transport.nextMessage();

						messageType = message[0];

						ByteArrayReader reader = new ByteArrayReader(message, 1, message.length - 1);

						try {
							if (messageType == SSH_MSG_KEXGSS_HOSTKEY) {
								Log.debug(this, "Received SSH_MSG_KEXGSS_HOSTKEY");

								if (hostKey != null)
									throw new SshException("Already received server host key", SshException.INTERNAL_ERROR);

								hostKey = reader.readBinaryString();
							}
							else if (messageType == SSH_MSG_KEXGSS_CONTINUE) {
								Log.debug(this, "Received SSH_MSG_KEXGSS_CONTINUE");

								if (reply.status == GssApi.GSS_S_COMPLETE)
									throw new SshException("SSH_MSG_KEXGSS_CONTINUE received when complete", SshException.INTERNAL_ERROR);

								inputToken = reader.readBinaryString();

								break;
							}
							else if (messageType == SSH_MSG_KEXGSS_COMPLETE) {
								Log.debug(this, "Received SSH_MSG_KEXGSS_COMPLETE");

								f = reader.readBigInteger();
								mic = reader.readBinaryString();

								if (reader.read() > 0) {
									inputToken = reader.readBinaryString();

									if (reply.status == GssApi.GSS_S_COMPLETE) {
										String errorMessage = "Protocol error: received token when complete";

										transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, errorMessage);

										throw new SshException(errorMessage, SshException.INTERNAL_ERROR);
									}
								}
								else if (reply.status != GssApi.GSS_S_COMPLETE) {
									String errorMessage = "Protocol error: didn't receive final token";

									transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, errorMessage);

									throw new SshException(errorMessage, SshException.INTERNAL_ERROR);
								}

								break;
							}
							else if (messageType == SSH_MSG_KEXGSS_ERROR) {
								Log.debug(this, "Received SSH_MSG_KEXGSS_ERROR");

								long majorStatus = reader.readInt();
								long minorStatus = reader.readInt();
								String errorMessage = reader.readString();
								String language = reader.readString();

								Log.info(this, "GSSAPI error: " + errorMessage + " (major: " + majorStatus + ", minor: " + minorStatus + ", language: " + language + ")");

								throw new SshException("GSSAPI error: " + errorMessage, SshException.INTERNAL_ERROR);
							}
							else {
								String errorMessage = "Protocol error: unexpected packet type " + messageType;

								transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, errorMessage);

								throw new SshException(errorMessage, SshException.INTERNAL_ERROR);
							}
						}
						finally {
							reader.close();
						}
					}
				}
				else if (reply.status != GssApi.GSS_S_COMPLETE)
					throw new SshException("Not complete, and no token output", SshException.INTERNAL_ERROR);
			} while (reply.status == GssApi.GSS_S_CONTINUE_NEEDED);

			if (messageType != SSH_MSG_KEXGSS_COMPLETE)
				throw new SshException("Didn't receive an SSH_MSG_KEXGSS_COMPLETE when expected", SshException.INTERNAL_ERROR);

			if (!validateKey(f)) {
				String errorMessage = "Bad server public key";

				transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, errorMessage);

				throw new SshException(errorMessage, SshException.INTERNAL_ERROR);
			}

			BigInteger x = ((DHPrivateKey)dhPair.getPrivate()).getX();

			secret = f.modPow(x, getP());
			exchangeHash = calculateExchangeHash(clientId, serverId, clientKexInit, serverKexInit, e, f);

			if (!GssApi.verifyMic(context, exchangeHash, mic)) {
				String errorMessage = "Server MIC verification failed";

				transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, errorMessage);

				throw new SshException(errorMessage, SshException.INTERNAL_ERROR);
			}

			if (transport.getGssContext() == null) {
				transport.setGssContext(context);

				context = null;
			}
		}
		catch (IOException ioErr) {
			throw new SshException(ioErr);
		}
		finally {
			if (context != null)
				GssApi.deleteSecContext(context);
		}
	}


	// protected methods


	protected abstract Oid getOid();

	protected abstract BigInteger getP();

	protected abstract BigInteger getG();

	protected abstract byte[] calculateExchangeHash(String clientId, String serverId, byte[] clientKexInit, byte[] serverKexInit, BigInteger e, BigInteger f) throws SshException;


	// private methods


	private KeyPair generateKeyPair() throws SshException
	{
		try {
			Provider dhProvider = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH);
			KeyPairGenerator generator;

			if (dhProvider != null)
				generator = KeyPairGenerator.getInstance(JCEAlgorithms.JCE_DH, dhProvider);
			else
				generator = KeyPairGenerator.getInstance(JCEAlgorithms.JCE_DH);

			DHParameterSpec paramSpec = new DHParameterSpec(getP(), getG());

			generator.initialize(paramSpec);

			KeyPair result;

			while (true) {
				result = generator.generateKeyPair();

				BigInteger key = ((DHPublicKey)result.getPublic()).getY();

				if (validateKey(key))
					break;
			}

			return result;
		}
		catch (NoSuchAlgorithmException algErr) {
			throw new SshException("JCE does not support Diffie Hellman key exchange", SshException.JCE_ERROR);
		}
		catch (InvalidAlgorithmParameterException paramErr) {
			throw new SshException("Failed to generate DH value", SshException.JCE_ERROR);
		}
	}

	private boolean validateKey(BigInteger key)
	{
		if (key.compareTo(BigInteger.ONE) < 0 || key.compareTo(getP().subtract(BigInteger.ONE)) > 0)
			return false;

		int numBits = key.bitLength();
		int numSet = 0;

		for (int n = 0 ; n < numBits ; n += 1) {
			if (key.testBit(n))
				numSet += 1;

			if (numSet > 3)
				return true;
		}

		return false;
	}
}
