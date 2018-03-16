/*
 * MyProxy.java
 */
package edu.sdsc.inca.agent.util;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.ProtocolException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;


/**
 *
 * @author Paul Hoover
 *
 */
public class MyProxy {

  // data fields


  public static final int DEFAULT_PORT = 7512;
  public static final int DEFAULT_LIFETIME = 43200;
  private static final int KEY_SIZE = 2048;
  private static final String VERSION = "VERSION=MYPROXYv2";
  private static final String COMMAND = "COMMAND=";
  private static final String USERNAME = "USERNAME=";
  private static final String PASSPHRASE = "PASSPHRASE=";
  private static final String LIFETIME = "LIFETIME=";
  private static final String RESPONSE = "RESPONSE=";
  private static final String ERROR = "ERROR=";
  private static final Logger m_logger = Logger.getLogger(MyProxy.class);
  private final int m_port;
  private final String m_server;
  private final String m_username;
  private final String m_passphrase;


  // constructors


  public MyProxy(String server, int port, String username, String passphrase)
  {
    m_server = server;
    m_port = port;
    m_username = username;
    m_passphrase = passphrase;
  }


  // public methods


  public void writeCredential(int lifetime) throws IOException, GeneralSecurityException, OperatorCreationException
  {
    String fileName = getProxyFileName();

    writeCredential(fileName, lifetime);
  }

  public void writeCredential(String fileName, int lifetime) throws IOException, GeneralSecurityException, OperatorCreationException
  {
    String credential = getCredential(lifetime);
    File credFile = new File(fileName);

    credFile.delete();
    credFile.createNewFile();

    Files.setPosixFilePermissions(credFile.toPath(), EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

    Writer output = new FileWriter(credFile);

    try {
      output.write(credential);
    }
    finally {
      output.close();
    }
  }

  public String getCredential(int lifetime) throws IOException, GeneralSecurityException, OperatorCreationException
  {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

    generator.initialize(KEY_SIZE);

    KeyPair pair = generator.genKeyPair();

    byte[] certRequest =  createCertificateRequest(pair);
    byte[] myProxyRequest = createMyProxyRequest(0, lifetime);

    SSLSocket socket = connect();
    InputStream socketIn = null;
    OutputStream socketOut = null;

    try {
      socketIn = new BufferedInputStream(socket.getInputStream());
      socketOut = new BufferedOutputStream(socket.getOutputStream());

      socketOut.write('0');
      socketOut.flush();
      socketOut.write(myProxyRequest);
      socketOut.flush();

      String line = readLine(socketIn, false);

      if (!line.equals(VERSION))
        throw new ProtocolException("unexpected response: " + line);

      line = readLine(socketIn, false);

      if (!line.startsWith(RESPONSE) || line.length() != RESPONSE.length() + 1)
        throw new ProtocolException("unexpected response: " + line);

      char responseType = line.charAt(RESPONSE.length());

      if (responseType == '1') {
        StringBuilder message = new StringBuilder("logon failed: ");

        while ((line = readLine(socketIn, true)) != null) {
          if (line.startsWith(ERROR)) {
            message.append(line.substring(ERROR.length()));
            message.append('\n');
          }
        }

        throw new GeneralSecurityException(message.toString());
      }
      else if (responseType != '0')
        throw new ProtocolException("unexpected response type: " + responseType);

      while ((line = readLine(socketIn, true)) != null)
        m_logger.debug("extra response line: " + line);

      socketOut.write(certRequest);
      socketOut.flush();

      int numCerts = socketIn.read();

      if (numCerts < 0)
        throw new EOFException();

      CertificateFactory certFactory = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
      Collection<? extends Certificate> generated = certFactory.generateCertificates(socketIn);
      StringWriter result = new StringWriter();
      JcaPEMWriter writer = new JcaPEMWriter(result);

      try {
        for (Certificate cert : generated)
          writer.writeObject(cert);

        writer.writeObject(pair.getPrivate());
      }
      finally {
        writer.close();
      }

      return result.toString();
    }
    finally {
      if (socketIn != null)
        socketIn.close();

      if (socketOut != null)
        socketOut.close();

      socket.close();
    }
  }

  public static boolean checkCredential() throws IOException
  {
    String fileName = getProxyFileName();
    File credFile = new File(fileName);

    if (!credFile.exists())
      return false;

    PEMParser parser = new PEMParser(new FileReader(credFile));

    try {
      X509CertificateHolder certHolder = (X509CertificateHolder)parser.readObject();

      return certHolder.isValidOn(new Date());
    }
    finally {
      parser.close();
    }
  }


  // private methods


  private SSLSocket connect() throws IOException, GeneralSecurityException
  {
    List<X509Certificate> certFiles = readCertFiles();
    KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());

    store.load(null, null);

    int certNum = 0;

    for (X509Certificate cert : certFiles) {
      String alias = "certificate" + certNum++;

      store.setCertificateEntry(alias, cert);
    }

    TrustManagerFactory manager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

    manager.init(store);

    SSLContext context = SSLContext.getInstance("SSL");

    context.init(null, manager.getTrustManagers(), null);

    SSLSocketFactory factory = context.getSocketFactory();
    SSLSocket socket = (SSLSocket)factory.createSocket(m_server, m_port);

    socket.startHandshake();

    return socket;
  }

  private List<X509Certificate> readCertFiles() throws IOException, CertificateException
  {
    Set<Path> certPaths = getCertPaths();
    List<X509Certificate> result = new ArrayList<X509Certificate>();
    JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();

    certConverter.setProvider(BouncyCastleProvider.PROVIDER_NAME);

    for (Path cert : certPaths) {
      PEMParser parser = new PEMParser(Files.newBufferedReader(cert));
      Object parsed;

      try {
        parsed = parser.readObject();
      }
      finally {
        parser.close();
      }

      if (parsed == null || !(parsed instanceof X509CertificateHolder))
        continue;

      X509CertificateHolder certHolder = (X509CertificateHolder)parsed;

      result.add(certConverter.getCertificate(certHolder));
    }

    return result;
  }

  private Set<Path> getCertPaths() throws IOException
  {
    Set<Path> result = new TreeSet<Path>();
    String dirName = System.getProperty("user.home") + "/.globus/certificates";
    File certDir = new File(dirName);

    if (!certDir.isDirectory()) {
      dirName = "/etc/grid-security/certificates";
      certDir = new File(dirName);

      if (!certDir.isDirectory())
        return result;
    }

    DirectoryStream<Path> dirStream = Files.newDirectoryStream(certDir.toPath());

    try {
      for (Path entry : dirStream) {
        if (Files.isSymbolicLink(entry)) {
          Path target = Files.readSymbolicLink(entry);

          if (!target.isAbsolute())
            entry = Paths.get(dirName + "/" + target.toString());
          else
            entry = target;
        }

        result.add(entry);
      }
    }
    finally {
      dirStream.close();
    }

    return result;
  }

  private byte[] createMyProxyRequest(int type, int lifetime) throws IOException
  {
    ByteArrayOutputStream message = new ByteArrayOutputStream();

    message.write(VERSION.getBytes());
    message.write('\n');
    message.write(COMMAND.getBytes());
    message.write(String.valueOf(type).getBytes());
    message.write('\n');
    message.write(USERNAME.getBytes());
    message.write(m_username.getBytes());
    message.write('\n');
    message.write(PASSPHRASE.getBytes());
    message.write(m_passphrase.getBytes());
    message.write('\n');
    message.write(LIFETIME.getBytes());
    message.write(String.valueOf(lifetime).getBytes());
    message.write('\n');

    return message.toByteArray();
  }

  private byte[] createCertificateRequest(KeyPair pair) throws IOException, NoSuchAlgorithmException, OperatorCreationException
  {
    X500NameBuilder nameBuilder = new X500NameBuilder();

    nameBuilder.addRDN(BCStyle.CN, "NULL SUBJECT NAME ENTRY");

    JcaContentSignerBuilder signBuilder = new JcaContentSignerBuilder("SHA1withRSA");

    signBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);

    PKCS10CertificationRequestBuilder reqBuilder = new JcaPKCS10CertificationRequestBuilder(nameBuilder.build(), pair.getPublic());
    PKCS10CertificationRequest request = reqBuilder.build(signBuilder.build(pair.getPrivate()));

    return request.getEncoded();
  }

  private String readLine(InputStream inStream, boolean acceptEof) throws IOException
  {
    StringBuilder builder = new StringBuilder();

    while (true) {
      int next = inStream.read();

      if (next < 0) {
        if (!acceptEof)
          throw new EOFException();

        break;
      }

      if (next == '\n' || next == 0)
        break;

      builder.append((char)next);
    }

    if (builder.length() < 1)
      return null;

    return builder.toString();
  }

  private static String getProxyFileName() throws IOException
  {
    ProcessBuilder procBuilder = new ProcessBuilder("id", "-u");
    Process idJob = procBuilder.start();
    BufferedReader stdOut = new BufferedReader(new InputStreamReader(idJob.getInputStream()));
    String userId = stdOut.readLine();

    return "/tmp/x509up_u" + userId;
  }
}
