/*
 * GssApi.java
 */
package com.sshtools.ssh.components;


import org.ietf.jgss.Oid;

import com.sshtools.ssh.SshException;


/**
 *
 * @author Paul Hoover
 *
 */
public class GssApi {

	static {
		System.loadLibrary("java-gsissh");
	}


	// nested classes


	public static class GssContext {
		public long oidAddr;
		public long nameAddr;
		public long contextAddr;
	}

	public static class InitReply {
		public int status;
		public int flags;
		public byte[] token;
	}


	// data fields


	public static final int GSS_C_MUTUAL_FLAG = 2;
	public static final int GSS_C_INTEG_FLAG = 32;
	public static final int GSS_S_COMPLETE = 0;
	public static final int GSS_S_CONTINUE_NEEDED = 1;
	public static final int GSS_S_FAILURE = 851968;


	// public methods


	public static native InitReply initSecContext(GssContext context, Oid mech, String host, byte[] input);

	public static native boolean verifyMic(GssContext context, byte[] hash, byte[] mic) throws SshException;

	public static native byte[] getMic(GssContext context, byte[] message) throws SshException;

	public static native void deleteSecContext(GssContext context);
}
