/*
 * GssApiGroup18Sha512MicV2.java
 */
package com.sshtools.ssh.components.jce;


import java.math.BigInteger;

import org.ietf.jgss.Oid;

import com.sshtools.ssh.components.DiffieHellmanGroups;


/**
 *
 * @author Paul Hoover
 *
 */
public class GssApiGroup18Sha512MicV2 extends GssApiConstGroup {

	// data fields


	public static final String ALGORITHM_NAME = "gss-group18-sha512-" + GSS_MECH_GLOBUS_GSSAPI_OPENSSL_MICV2_HASH;


	// constructors


	public GssApiGroup18Sha512MicV2()
	{
		super("SHA-512");
	}


	// public methods


	@Override
	public String getAlgorithm()
	{
		return ALGORITHM_NAME;
	}


	// protected methods


	@Override
	protected Oid getOid()
	{
		return GSS_MECH_GLOBUS_GSSAPI_OPENSSL_MICV2;
	}

	@Override
	protected BigInteger getP()
	{
		return DiffieHellmanGroups.group18;
	}
}
