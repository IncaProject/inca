package edu.sdsc.inca;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.logging.LoggerFactory;
import com.sshtools.logging.LoggerLevel;
import com.sshtools.logging.SimpleLogger;
import com.sshtools.net.SocketTransport;
import com.sshtools.ssh.PseudoTerminalModes;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshConnector;
import com.sshtools.ssh.SshSession;
//import com.sshtools.ssh.components.jce.GssApiGexSha1;
//import com.sshtools.ssh.components.jce.GssApiGexSha1MicV2;
//import com.sshtools.ssh.components.jce.GssApiGroup18Sha512;
import com.sshtools.ssh2.GssApiKeyexAuthentication;


public class Example {

	public static void main(String[] args)
	{
		try {
			LoggerFactory.setInstance(new SimpleLogger(LoggerLevel.DEBUG));

			if (args.length != 1)
				throw new Exception("usage: Example [username@]hostname[:port]");

			String hostname = args[0];
			int index = hostname.indexOf(':');
			int port;

			if (index < 0)
				port = 22;
			else {
				port = Integer.parseInt(hostname.substring(index + 1));
				hostname = hostname.substring(0, index);
			}

			boolean implicit;
			String username;

			index = hostname.indexOf("@");

			if (index < 0) {
				username = System.getProperty("user.name");
				implicit = true;
			}
			else {
				username = hostname.substring(0, index);
				hostname = hostname.substring(index + 1);
				implicit = false;
			}

			System.out.println("Connecting to " + hostname + ", port " + port + " as " + username);

			SshConnector conn = SshConnector.createInstance();

			conn.getContext().useGssApiKeyExchange(true);

			SshClient client = conn.connect(new SocketTransport(hostname, port), username, true);

			client.authenticate(new GssApiKeyexAuthentication(implicit));

			if (client.isAuthenticated()) {
				SshSession session = client.openSessionChannel();
				PseudoTerminalModes pty = new PseudoTerminalModes(client);

				pty.setTerminalMode(PseudoTerminalModes.ECHO, false);

				session.requestPseudoTerminal("vt100", 80, 24, 0, 0, pty);
				session.startShell();

				Thread listener = new Thread() {
					@Override
					public void run()
					{
						try {
							InputStream inStream = session.getInputStream();
							int read;

							while ((read = inStream.read()) > -1) {
								System.out.write(read);
								System.out.flush();
							}
						}
						catch (IOException err) {
							err.printStackTrace();
						}
					}
				};

				listener.start();

				OutputStream outStream = session.getOutputStream();
				byte[] buf = new byte[4096];
				int read;

				while ((read = System.in.read(buf)) > -1) {
					if (session.isClosed() || !client.isConnected())
						break;

					outStream.write(buf, 0, read);
				}

				session.close();
			}

			client.disconnect();
		}
		catch (Exception err) {
			err.printStackTrace(System.err);

			System.exit(1);
		}
	}
}
