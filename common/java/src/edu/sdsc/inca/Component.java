package edu.sdsc.inca;


import edu.sdsc.inca.util.ConfigProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.*;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;


/**
 * This class encapsulates the common behavior of the Client and Server classes.
 */
public class Component {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private static final String COMPONENT_KEY_KEY = "componentkey";
  private static final String MAJOR_VERSION = "major";
  private static final String MINOR_VERSION = "minor";
  private static final String DEFAULT_APPENDER = "stdout";
  private static final String PROVIDER = "BC";

  // Command-line options
  final public static String COMPONENT_OPTS =
    "  a|auth     boolean Authenticated (secure) connection?\n" +
    "  c|cert     path    Path to the authentication certificate\n" +
    "  h|help     null    Print help/usage\n" +
    "  H|hostname str     Hostname where the server is running\n" +
    "  i|init     path    Path to properties file\n" +
    "  k|key      path    Path to the authentication key\n" +
    "  l|logfile  str     Route log messages to a file\n" +
    "  p|port     int     Server listening port\n" +
    "  P|password str     Specify how to obtain encryption password\n" +
    "  t|trusted  path    Path to authentication trusted certificate dir\n" +
    "  V|version  null    Display program version\n";

  // Protected class vars
  protected static final Logger logger = Logger.getLogger(Component.class);

  // Configuration instance variables
  protected boolean authenticate = true;
  protected String certPath = null;
  protected String hostname = null;
  protected String keyPath = null;
  protected String logFile = null;
  protected String password = null;
  protected int port = -1;
  protected String trustedPath = null;

  // Other instance variables
  protected Certificate cert = null;
  protected PrivateKey key = null;
  protected Vector<Object> trusted = new Vector<Object>();


  /**
   * Add a single trusted certificate.
   *
   * @param trusted the certificate to add
   */
  public void addTrustedCert(Certificate trusted) {
    this.trusted.add(trusted);
  }

  /**
   * Is the connection authenticated?
   *
   * @return whether or not the connection is authenticated
   */
  public boolean getAuthenticate() {
    return this.authenticate;
  }

  /**
   * Returns the component certificate.  Valid only after the component's
   * socket has been created.
   *
   * @return the component certificate
   */
  public Certificate getCertificate() {
    return this.cert;
  }

  /**
   * Returns the path to the component certificate.
   *
   * @return The path to the certificate.
   */
  public String getCertificatePath() {
    return this.certPath;
  }

  /**
   * Returns the DN from one end of a secure connection.
   *
   * @param s the connection
   * @param peer indicates whether the local or peer DN is desired
   * @return the requested DN; null if the socket is not secure
   */
  public static String getDn( Socket s, boolean peer ) {
    try {
      SSLSession session = ((SSLSocket)s).getSession();
      Certificate[] certs =
        peer ? session.getPeerCertificates() : session.getLocalCertificates();
      if(certs.length > 0 && certs[0] instanceof X509Certificate) {
        return ((X509Certificate)certs[0]).getSubjectX500Principal().getName();
      }
    } catch(Exception e) {
      // empty
    }
    return null;
  }

  /**
   * Returns the name of the host where the server is running.
   *
   * @return A string containing the hostname of the server
   */
  public String getHostname() {
    if(this.hostname == null || this.hostname.equals("localhost")) {
      // Try to resolve into an externally-visible name
      try {
        this.hostname = InetAddress.getLocalHost().getCanonicalHostName();
        if(this.hostname.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
          logger.warn("Unable to get external hostname; fallback to IP");
        }
      } catch(Exception e) {
        logger.warn("Unable to get local IP address");
         this.hostname = "localhost";
      }
    }
    return this.hostname;
  }

  /**
   * Returns the component key.  Valid only after the component's socket has
   * been created.
   *
   * @return the component key
   */
  public PrivateKey getKey() {
    return this.key;
  }

  /**
   * Returns the path to the component private key.
   *
   * @return The path to the private key.
   */
  public String getKeyPath() {
    return this.keyPath;
  }

  /**
   * Returns the path to the file where the component writes log messages.
   *
   * @return the path to the log file
   */
  public String getLogFile() {
    return this.logFile;
  }

  /**
   * Gets the component encryption password.
   *
   * @return the encryption password
   */
  public String getPassword() {
    return this.password;
  }

  /**
   * Returns the port where the Server is listening.
   *
   * @return The port the server is or will be listening on
   */
  public int getPort() {
    return this.port;
  }

  /**
   * Returns the set of certificates trusted by the component.  Valid only
   * after the component's socket has been created.
   *
   * @return the component trusted certificates
   */
  public Certificate[] getTrustedCertificates() {
    return this.trusted.toArray(new Certificate[this.trusted.size()]);
  }

  /**
   * Get the path to the component trusted certificate directory.
   *
   * @return the trusted certificate directory
   */
  public String getTrustedPath() {
    return this.trustedPath;
  }

  /**
   * Return the uri for the server.
   *
   * @return A string containing the uri of the server
   */
  public String getUri() {
    return (this.getAuthenticate() ? "incas://" : "inca://") +
           this.getHostname() + ":" + this.getPort();
  }

  /**
   * Determines whether or not the connection is authenticated.
   *
   * @param authenticate authenticate the connection?
   */
  public void setAuthenticate(boolean authenticate) {
    this.authenticate = authenticate;
  }

  /**
   * Sets the path to the file that contains the component certificate.
   *
   * @param path path to the certificate file
   */
  public void setCertificatePath(String path) {
    this.certPath = path;
  }

  /**
   * A convenience function for setting multiple Component configuration
   * properties at once.  Recognized elements of config are: "auth", the
   * authorization indicator; "cert", the path to the certificate file;
   * "hostname", the name of the host the server is running on; "key", the
   * path to the component key; "logfile", the path to the component log;
   * "password", the encryption password; "port", the server port; "trusted",
   * the trusted cert directory.
   *
   * @param config contains configuration values
   * @throws ConfigurationException on a faulty configuration property value
   */
  public void setConfiguration(Properties config) throws ConfigurationException{
    String prop;
    logger.debug("Configuration properties:");
    for(Enumeration<Object> e = config.keys(); e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      String value = key.equals("password") ? "*****" : config.getProperty(key);
      logger.debug("'" + key + "' => '" + value + "'");
    }
    if((prop = config.getProperty("auth")) != null) {
      this.setAuthenticate
        (prop.equalsIgnoreCase("true") || prop.equalsIgnoreCase("yes"));
    }
    if((prop = config.getProperty("cert")) != null) {
      this.setCertificatePath(prop);
    }
    if((prop = config.getProperty("hostname")) != null) {
      this.setHostname(prop);
    }
    if((prop = config.getProperty("key")) != null) {
      this.setKeyPath(prop);
    }
    if((prop = config.getProperty("logfile")) != null) {
      this.setLogFile(prop);
    }
    if((prop = config.getProperty("password")) != null) {
      this.setPassword(prop);
    }
    if((prop = config.getProperty("port")) != null) {
      this.setPort(Integer.valueOf(prop));
    }
    if((prop = config.getProperty("trusted")) != null) {
      this.setTrustedPath(prop);
    }
  }

  /**
   *
   * @return the current configuration of the object
   */
  public Properties getConfiguration()
  {
    Properties config = new Properties();

    config.setProperty("auth", String.valueOf(getAuthenticate()));
    config.setProperty("cert", getCertificatePath());
    config.setProperty("key", getKeyPath());
    config.setProperty("logfile", getLogFile());
    config.setProperty("password", getPassword());
    config.setProperty("port", String.valueOf(getPort()));
    config.setProperty("trusted", getTrustedPath());

    if (hostname != null)
      config.setProperty("hostname", hostname);

    return config;
  }

  /**
   * Sets the name of the host where the server is running.
   *
   * @param hostname A string containing the hostname of the server
   */
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  /**
   * Sets the path to the file that contains the component private key.
   *
   * @param path path to the private key file
   */
  public void setKeyPath(String path) {
    this.keyPath = path;
  }

  /**
   * Sets the path to the file where the component writes log messages.
   *
   * @param path the path to the log file
   * @throws ConfigurationException if the path is not writable
   */
  public void setLogFile(String path) throws ConfigurationException {
    this.logFile = path;
    logger.info("Logging messages to " + this.logFile);
    Logger rootLogger = Logger.getRootLogger();
    Appender defaultAppender = rootLogger.getAppender( DEFAULT_APPENDER );
    if ( defaultAppender == null ) {
      defaultAppender = (Appender)rootLogger.getAllAppenders().nextElement();
    } else {
      rootLogger.removeAppender( DEFAULT_APPENDER );
    }
    File parent = new File(new File(path).getParent());
    if(!parent.exists()) {
      parent.mkdirs();
    }
    try {
      Appender fileAppender = new DailyRollingFileAppender
        (defaultAppender.getLayout(), this.logFile,"'.'yyyy-MM-dd");
      fileAppender.setName( DEFAULT_APPENDER );
      rootLogger.addAppender( fileAppender );
    } catch(IOException e) {
      throw new ConfigurationException(e.toString());
    }
  }

  /**
   * Sets the component encryption password.
   *
   * @param password the encryption password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Set the port to bind to.
   *
   * @param port port number
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Set the trusted certificates to the contents of the directory given.
   * The directory must be found in the classpath.
   *
   * @param path the trusted certificate directory path
   */
  public void setTrustedPath(String path) {
    this.trustedPath = path;
  }

  /**
   * Creates and returns a socket with any appropriate authentication.
   *
   * @param serverSocket determines whether the return value is a Socket or
   *                     a ServerSocket
   * @param host the host to bind (Server) or connect to (Clients)
   * @param port the port to open (Server) or connect to (Clients)
   *
   * @return a new socket
   *
   * @throws ConfigurationException if a config attribute has a bad value
   * @throws IOException on socket creation failure
   */
  protected Object createSocket(boolean serverSocket,
                                String host,
                                int port)
    throws ConfigurationException, IOException {

    ServerSocket ss;

    if(port < 0) {
      logger.error("No port set");
      throw new ConfigurationException("No port set");
    }

    if(!this.authenticate) {
      logger.debug("Not using authentication");
      if(serverSocket) {
        InetSocketAddress addr;

        if (host != null)
          addr = new InetSocketAddress(host, port);
        else
          addr = new InetSocketAddress(port);

        ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(addr);
        return ss;
      } else {
        return new Socket(host, port);
      }
    }


    try {
      readCredentials();

      KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
      ks.load(null, null);
      ks.setKeyEntry
        (COMPONENT_KEY_KEY, this.key, this.password.toCharArray(),
         new Certificate[]{this.cert});
      // Add the trusted certificates to the trust store
      for(int i = 0; i < this.trusted.size(); i++) {
        try {
          ks.setCertificateEntry("trusted"+i, (Certificate)this.trusted.get(i));
        } catch(Exception e) {
          logger.error("Unable to add Cert to keystore", e);
        }
      }
      // Make sure our own certificate is trusted
      try {
        ks.setCertificateEntry("trusted" + this.trusted.size(), this.cert);
      } catch(Exception e) {
        logger.error("Unable to add Cert to keystore", e);
      }
      // Make an ssl context from our password and keystore
      SSLContext context = SSLContext.getInstance("SSL");
      String kmfAlgo = KeyManagerFactory.getDefaultAlgorithm();
      KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmfAlgo);
      String tmfAlgo = TrustManagerFactory.getDefaultAlgorithm();
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgo);
      kmf.init(ks, this.password.toCharArray());
      tmf.init(ks);
      context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
      // Create and return the socket
      if(serverSocket) {
        ss = context.getServerSocketFactory().createServerSocket();
        ((SSLServerSocket)ss).setNeedClientAuth(true);
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(port));
        return ss;
      } else {
        return context.getSocketFactory().createSocket(host, port);
      }
    } catch(Exception e) {
      logger.fatal("Unable to initialize keystore " + e);
      throw new IOException("Unable to initialize keystore " + e);
    }

  }

  /**
   * Read in the credentials using the paths to the credentials and passphrase.
   *
   * @throws ConfigurationException if problem finding credential properties
   * @throws IOException if problem reading credentials
   * @throws PKCSException
   * @throws OperatorCreationException
   */
  public void readCredentials() throws ConfigurationException, IOException, OperatorCreationException, PKCSException {
    if(this.password == null) {
      this.password = "";
    } else if(this.certPath == null) {
      logger.error("No certificate path set");
      throw new ConfigurationException("No certificate path set");
    } else if(this.keyPath == null) {
      logger.error("No key path set");
      throw new ConfigurationException("No key path set");
    }

    // Load component certificate, ...
    InputStream input = openResourceStream(this.certPath);
    if(input == null) {
      throw new IOException(this.certPath + " not found");
    }
    this.cert = readCertFile(input);

    // ... key, ...
    input = openResourceStream(this.keyPath);
    if(input == null) {
      throw new IOException(this.keyPath + " not found");
    }
    this.key = readKeyFile(input, password);

    // ... and trusted certificates.
    if(this.trustedPath != null) {
      URL url =
        ClassLoader.getSystemClassLoader().getResource(this.trustedPath);
      File trustedDir =
        url == null ? new File(this.trustedPath) : new File(url.getFile());
      if(!trustedDir.exists() || !trustedDir.isDirectory()) {
        throw new IOException(this.trustedPath + " not found");
      }
      File[] files = trustedDir.listFiles();
      for(int i = 0; i < files.length; i++) {
        File file = files[i];
        if(!file.toString().endsWith(".pem")) {
          continue;
        }
        logger.debug("Adding trusted certificate " + file);
        try {
          this.trusted.add(readCertFile(new FileInputStream(file)));
        } catch (ClassCastException e) {
          logger.warn("Exception reading trusted file " + file.toString() + ": Keys should not be stored in trusted dir");
        } catch(IOException e) {
          logger.warn ("Exception reading trusted file " + file.toString() + ": " + e.toString());
        }
      }
    }
  }


  /**
   * Read the version for the component from a file in the classpath.
   *
   * @param versionFilename  The filename of the version file in the classpath
   *
   * @return A string containing the version read from file or "unknown" if
   * not found.
   */
  public static String readVersion( String versionFilename ) {
    String version = "unknown";
    InputStream input = openResourceStream( versionFilename );
    if( input != null) {
      Properties versionFile = new Properties();
      try {
        versionFile.load(input);
      } catch ( IOException e ) {
        logger.error( "Unable to read version file " + versionFilename );
        return version;
      }
      String majorVersion = versionFile.getProperty( MAJOR_VERSION );
      if ( majorVersion != null ) {
        version = majorVersion;
      }
      String minorVersion = versionFile.getProperty( MINOR_VERSION );
      if ( minorVersion != null ) {
        version += "." + minorVersion;
      }
    }
    return version;
  }

  /**
   * Returns an input stream for a resource found either in the classpath or on
   * the file system.  Returns null if the resource is not found.
   *
   * @param resourcePath the path (relative or absolute) to the resource
   * @return an InputStream opened to the resource
   */
  public static InputStream openResourceStream(String resourcePath) {
    InputStream result =
      ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
    if(result == null) {
      try {
        result = new FileInputStream(new File(resourcePath));
      } catch(FileNotFoundException e) {
        // empty
      }
    }
    return result;
  }

  /**
   * A convenience function for configuring a Component (or descendent class)
   * from the system properties, the arguments passed to main, and the
   * properties file.  Exits the program if parsing fails, or if the --help or
   * --version arguments are included in args.
   *
   * @param c the component to configure
   * @param args command-line arguments
   * @param opts valid command-line options
   * @param prefix property prefix from system props and property file
   * @param exec main class name
   * @param versionFilename classpath filename containing the version
   * @throws ConfigurationException on faulty config properties
   * @throws IOException on an unreadable file property
   */
  public static void configComponent(Component c,
                                     String[] args,
                                     String opts,
                                     String prefix,
                                     String exec,
                                     String versionFilename )
    throws ConfigurationException, IOException {

    ConfigProperties argProps = new ConfigProperties();
    ConfigProperties config = new ConfigProperties();
    config.putAllTrimmed(System.getProperties(), prefix);
    argProps.setPropertiesFromArgs(opts, args);

    if(argProps.getProperty("help") != null) {
      System.out.println("java " + exec + "\n" + opts);
      System.exit(1);
    } else if(argProps.getProperty("version") != null) {
      String version = readVersion( versionFilename );
      System.out.println(exec + " version " + version);
      System.exit(1);
    }

    String propPath = argProps.getProperty("init", "inca.properties");
    InputStream propStream = Component.openResourceStream(propPath);
    if(propStream != null) {
      config.loadFromStream(propStream, prefix);
    }
    config.putAll(argProps);
    String password = readPassword(config.getProperty("password"));
    if(password == null || password.equals("")) {
      logger.warn("No password specified; assuming unencrypted key");
      config.remove("password");
    } else {
      config.setProperty("password", password);
    }
    c.setConfiguration(config);

    // make all properties available to all Inca components, via System properties
    for(Enumeration<Object> e = config.keys() ; e.hasMoreElements() ; ) {
      String key = (String) e.nextElement();

      System.setProperty(prefix + key, config.getProperty(key));
    }
  }

  /**
   *
   * @param property
   * @return
   * @throws ConfigurationException
   */
  protected static String readPassword(String property) throws ConfigurationException
  {
    if (property == null || property.matches("false|no"))
      return null;
    else if (property.matches("(?s)stdin(:.*)?|true|yes")) {
      if (property.startsWith("stdin:"))
        System.out.print(property.substring(6));

      try {
        return (new BufferedReader(new InputStreamReader(System.in))).readLine();
      }
      catch (IOException ioErr) {
        throw new ConfigurationException(ioErr);
      }
    }
    else if (property.startsWith("pass:")) {
      logger.warn("Specified password may be insecure");

      return property.substring(5);
    }
    else
      throw new ConfigurationException("Bad password specifier");
  }

  /**
   *
   * @param inStream
   * @return
   * @throws IOException
   */
  private Object readPEMFile(InputStream inStream) throws IOException
  {
    PEMParser parser = null;

    try {
      parser = new PEMParser(new InputStreamReader(inStream));

      return parser.readObject();
    }
    finally {
      if (parser != null)
        parser.close();
      else
        inStream.close();
    }
  }

  /**
   *
   * @param inStream
   * @return
   * @throws IOException
   * @throws ConfigurationException
   */
  private X509Certificate readCertFile(InputStream inStream) throws IOException, ConfigurationException
  {
    X509CertificateHolder certHolder = (X509CertificateHolder)readPEMFile(inStream);
    JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();

    certConverter.setProvider(PROVIDER);

    try {
      return certConverter.getCertificate(certHolder);
    }
    catch (CertificateException certErr) {
      throw new ConfigurationException(certErr);
    }
  }

  /**
   *
   * @param inStream
   * @param password
   * @return
   * @throws IOException
   * @throws ConfigurationException
   * @throws OperatorCreationException
   * @throws PKCSException
   */
  private PrivateKey readKeyFile(InputStream inStream, final String password) throws IOException, ConfigurationException, OperatorCreationException, PKCSException
  {
    Object pemObject = readPEMFile(inStream);
    PrivateKeyInfo keyInfo;

    if (pemObject instanceof PEMKeyPair)
      keyInfo = ((PEMKeyPair)pemObject).getPrivateKeyInfo();
    else if (pemObject instanceof PEMEncryptedKeyPair) {
      PEMEncryptedKeyPair encryptedKey = (PEMEncryptedKeyPair)pemObject;
      JcePEMDecryptorProviderBuilder decryptBuilder = new JcePEMDecryptorProviderBuilder();

      decryptBuilder.setProvider(PROVIDER);

      PEMDecryptorProvider decryptor = decryptBuilder.build(password.toCharArray());
      PEMKeyPair pemKey = encryptedKey.decryptKeyPair(decryptor);

      keyInfo = pemKey.getPrivateKeyInfo();
    }
    else if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo) {
      PKCS8EncryptedPrivateKeyInfo encryptedInfo = (PKCS8EncryptedPrivateKeyInfo)pemObject;
      JceOpenSSLPKCS8DecryptorProviderBuilder decryptBuilder = new JceOpenSSLPKCS8DecryptorProviderBuilder();

      decryptBuilder.setProvider(PROVIDER);

      InputDecryptorProvider decryptor = decryptBuilder.build(password.toCharArray());

      keyInfo = encryptedInfo.decryptPrivateKeyInfo(decryptor);
    }
    else
      throw new ConfigurationException("File does not contain a private key");

    JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();

    keyConverter.setProvider(PROVIDER);

    return keyConverter.getPrivateKey(keyInfo);
  }
}
