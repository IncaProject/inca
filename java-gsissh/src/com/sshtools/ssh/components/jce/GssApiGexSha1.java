/*
 * GssApiGexSha1.java
 */
package com.sshtools.ssh.components.jce;


import org.ietf.jgss.Oid;


/**
 *
 * @author Paul Hoover
 *
 */
public class GssApiGexSha1 extends GssApiGroupExchange {

	// data fields


	public static final String ALGORITHM_NAME = "gss-gex-sha1-" + GSS_MECH_GLOBUS_GSSAPI_OPENSSL_HASH;


	// constructors


	public GssApiGexSha1()
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
}
