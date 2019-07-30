/*
 * GssApiGroup1Sha1.java
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
public class GssApiGroup1Sha1 extends GssApiConstGroup {

	// data fields


	public static final String ALGORITHM_NAME;

	static {
		ALGORITHM_NAME = "gss-group1-sha1-" + GSS_MECH_GLOBUS_GSSAPI_OPENSSL_HASH;
	}


	// constructors


	public GssApiGroup1Sha1()
	{
		super("SHA-1");
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
		return DiffieHellmanGroups.group1;
	}
}
