/*
 * GssApiGroup16Sha512.java
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
public class GssApiGroup16Sha512 extends GssApiConstGroup {

	// data fields


	public static final String ALGORITHM_NAME = "gss-group16-sha512-" + GSS_MECH_GLOBUS_GSSAPI_OPENSSL_HASH;


	// constructors


	public GssApiGroup16Sha512()
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
		return GSS_MECH_GLOBUS_GSSAPI_OPENSSL;
	}

	@Override
	protected BigInteger getP()
	{
		return DiffieHellmanGroups.group16;
	}
}
