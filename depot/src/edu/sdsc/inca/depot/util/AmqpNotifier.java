/*
 * AmqpNotifier.java
 */
package edu.sdsc.inca.depot.util;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.Calendar;

import edu.sdsc.inca.depot.persistent.*;
import edu.sdsc.inca.dataModel.util.Report;
import org.apache.log4j.Logger;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultSaslConfig;

import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.dataModel.report.ReportDocument;

/**
 *
 * @author Paul Hoover
 * @author Shava Smallen
 *
 */
public class AmqpNotifier implements ReportNotifier {
  // data fields

  private static final int JSON_INDENT = 4;
  private static final String PROVIDER = "BC";
  //private static final String REPORT_FIELD = "@inca.report@";
  private static final Logger m_logger = Logger.getLogger(AmqpNotifier.class);
  private final String m_exchange;
  private final List<ConnectionFactory> m_factories = new ArrayList<>();
  private final String m_routing_key;
  private final String m_json_template;
  private final String m_report_fieldname;
  private String cert;
  private boolean debug;
  private String key;
  private String keyPassPhrase;
  private String[] trustedCerts;
  private static final long credRefreshPeriod = 3600000; // 1 hour in millis
  private long lastRefresh = 0;

  // constructors

  /**
   * Constructor for AmqpNotifier.  The routingKey and jsonTemplate may contain variables in the form of @varname@.
   * There are preset var names: inca.nickname, inca.target_hostname, inca.resource, inca.gmt, inca.result, and
   * inca.errorMessage.  Also var names can be pulled from the nickname using nicknamePattern, where any () can be
   * assigned a variable from nicknameVariables.  There is also a special variable called inca.report which is inserted
   * into the field named by reportFieldName.
   *
   * @param uris            URIs for available AMQP servers (format: amqp://host:port/virtualhost
   * @param exchange        Name of AMQP exchange to publish to
   * @param routingKey      Routing key to use; may contain @vars@
   * @param jsonTemplate    Template for message sent to the amqp server.  May contain @vars@.
   * @param reportFieldName Field name specified in jsonTemplate where the inca report will be placed.
   * @throws ConfigurationException
   */
  public AmqpNotifier(String[] uris, String exchange, String routingKey, String jsonTemplate, String reportFieldName)
          throws ConfigurationException {
    for (String uri : uris) {
      ConnectionFactory newFactory = new ConnectionFactory();

      try {
        newFactory.setUri(uri);
        m_logger.info("Read AMQP URI: " + uri);
      } catch (Exception e) {
        throw new ConfigurationException("Problem setting AMQP server URI", e);
      }
      newFactory.setConnectionTimeout(30000);

      m_factories.add(newFactory);
    }

    m_exchange = exchange;
    m_routing_key = routingKey;
    m_logger.info("Will publish to exchange '" + m_exchange + "' with routing key format '" + routingKey + "'");
    m_json_template = jsonTemplate;
    m_report_fieldname = "@" + reportFieldName + "@";
    m_logger.info("Read JSON template with inca report in field '" + m_report_fieldname + "': " + m_json_template);
    this.debug = false;
  }

  /**
   * Constructor for AmqpNotifier.  The routingKey and jsonTemplate may contain variables in the form of @varname@.
   * There are preset var names: inca.nickname, inca.target_hostname, inca.resource, inca.gmt, inca.result, and
   * inca.errorMessage.  Also var names can be pulled from the nickname using nicknamePattern, where any () can be
   * assigned a variable from nicknameVariables.  There is also a special variable called inca.report which is inserted
   * into the field named by reportFieldName.
   *
   * @param uris            URIs for available AMQP servers (format: amqp://host:port/virtualhost
   * @param exchange        Name of AMQP exchange to publish to
   * @param routingKey      Routing key to use; may contain @vars@
   * @param jsonTemplate    Template for message sent to the amqp server.  May contain @vars@.
   * @param reportFieldName Field name specified in jsonTemplate where the inca report will be placed.
   * @throws ConfigurationException
   */
  public AmqpNotifier(String[] uris, String exchange, String routingKey, String jsonTemplate, String reportFieldName,
                      boolean debug)
          throws ConfigurationException {
    this(uris, exchange, routingKey, jsonTemplate, reportFieldName);
    this.debug = debug;
  }

  // public methods

  /**
   * Execute AMQP notification.
   *
   * @param command  Name of inca command that triggered this notification
   * @param report   Content of incoming report
   * @param series   Matching series for report
   * @param instance DB instance for report
   * @throws Exception
   */
  @Override
  public void notify(String command, Report report, Series series, InstanceInfo instance) throws Exception {
    this.refreshSslContext();
    for (SeriesConfig dbSc : series.getSeriesConfigs()) {
      if (dbSc.getDeactivated() != null || dbSc.getSchedule().getType().equals("immediate"))
        continue;

      AmqpMessage msg = this.createMessage(dbSc, report, series, instance);

      for (ConnectionFactory factory : m_factories) {
        if (sendJSONToAmqpServer(instance, msg.routingKey, msg.json.getBytes(), factory)) break;
      }
    }
  }

  protected AmqpMessage createMessage(SeriesConfig dbSc, Report report, Series series, InstanceInfo instance)
          throws IOException, SQLException, PersistenceException, JSONException {

    m_logger.debug("Located series config " + dbSc.getId() + ": " + dbSc.getNickname() + ", " + series.getResource());
    String testResult = "success";
    if (!this.debug) {
      testResult = getReportResult(report, instance, dbSc);
      if (testResult == null) {
        return null;
      }
    }

    HashMap<String, String> jsonValues = new HashMap<String, String>();
    for (String tag : dbSc.getTags()) {
      if (tag.contains("=")) {
        String[] tagParts = tag.split("=");
        String tagName = tagParts[0];
        String tagValue = "";
        if (tagParts.length >= 2) {
          tagValue = tagParts[1];
        }
        jsonValues.put("inca.tag." + tagName, tagValue);
      }
    }
    jsonValues.put("inca.nickname", dbSc.getNickname());
    jsonValues.put("inca.target_hostname", series.getTargetHostname());
    jsonValues.put("inca.resource", series.getResource());
    jsonValues.put("inca.gmt", report.getGmt().toString());
    m_logger.debug("Calendar type " + report.getGmt().getClass().toString());
    if (dbSc.getSchedule() != null) {
      Calendar gmtExpires = dbSc.getSchedule().calculateExpires(report.getGmt());
      if (gmtExpires != null) {
        jsonValues.put("inca.gmtexpires", gmtExpires.toString());
        long diff = (gmtExpires.getTimeInMillis() - report.getGmt().getTimeInMillis()) / 1000;
        jsonValues.put("inca.validity", Long.toString(diff));
      }
    }
    jsonValues.put("inca.result", testResult);
    jsonValues.put("inca.errorMessage", report.getExitStatus().getErrorMessage());
    JSONObject notification_json = new JSONObject(getJsonTemplateWithReport(report));
    resolveJSONVars(jsonValues, notification_json, new String[]{m_report_fieldname});
    m_logger.debug("Message = " + notification_json.toString());

    String routingKey = resolveStringVars(jsonValues, m_routing_key);
    if (routingKey.matches("^.*@[^@]+@.*$")) {
      m_logger.warn("Routing key " + routingKey + " had unresolved variables; simplifying to nickname.targetResource");
      String resource = series.getTargetHostname() == null ||
              series.getTargetHostname().equals(Row.DB_EMPTY_STRING) ? series.getResource() : series.getTargetHostname();
      routingKey = dbSc.getNickname() + "." + resource;
    }


    return new AmqpMessage(routingKey, notification_json.toString(JSON_INDENT));
  }

  /**
   * Configure authentication to AMQP server.
   *
   * @param cert          Path to certificate file to authenticate against AMQP server
   * @param key           Path to certificate key to authenticate against AMQP server
   * @param keyPassPhrase Passphrase for certificate key
   * @param trustedCerts  List of trusted certificates
   * @throws ConfigurationException If problem setting credentials to authenticate to Amqp server.
   */
  public void setSslContext(String cert, String key, String keyPassPhrase, String[] trustedCerts) throws ConfigurationException {
    this.cert = cert;
    this.key = key;
    this.keyPassPhrase = keyPassPhrase;
    this.trustedCerts = trustedCerts;

    if (this.keyPassPhrase == null)
      this.keyPassPhrase = "";

    refreshSslContext();
  }

  /**
   * Refresh the SSL credentials in case any changes
   *
   * @throws ConfigurationException
   */
  private synchronized void refreshSslContext() throws ConfigurationException {
    long currentTime = System.currentTimeMillis();
    if (this.debug || currentTime < this.lastRefresh + credRefreshPeriod) {
      return;
    }
    this.lastRefresh = currentTime;
    m_logger.info("Refreshing SSL credentials");
    try {
      KeyStore userStore = readCredentials(this.cert, this.key, this.keyPassPhrase);
      KeyManagerFactory keyFactory = KeyManagerFactory.getInstance("SunX509");

      keyFactory.init(userStore, this.keyPassPhrase.toCharArray());

      KeyStore trustedStore = readTrustedCerts(this.trustedCerts);
      TrustManagerFactory trustFactory = TrustManagerFactory.getInstance("SunX509");
      trustFactory.init(trustedStore);

      SSLContext context = SSLContext.getInstance("SSLv3");

      context.init(keyFactory.getKeyManagers(), trustFactory.getTrustManagers(), null);

      for (ConnectionFactory factory : m_factories) {
        factory.useSslProtocol(context);
        factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
      }
    } catch (Exception e) {
      throw new ConfigurationException("Unable to configure authentication", e);
    }
  }

  // private methods

  /**
   * Get alias for certificate based on issuer and subject.
   *
   * @param cert X509 Certificate
   * @return String containing combination of subject name and issue name.
   */
  private String getAlias(X509Certificate cert) {
    String subjectCN = getCommonName(cert.getSubjectX500Principal());
    String issuerCN = getCommonName(cert.getIssuerX500Principal());

    return subjectCN + " issued by " + issuerCN;
  }

  /**
   * Get name from principal.
   *
   * @param principal Principal of X509 certificate.
   * @return Name returned from principal
   */
  private String getCommonName(X500Principal principal) {
    X500Name name = new X500Name(principal.getName());
    RDN[] rdns = name.getRDNs(BCStyle.CN);

    return IETFUtils.valueToString(rdns[0].getFirst().getValue());
  }

  /**
   * Return the JSON template with 'inca.report' already resolved.
   *
   * @param report Report object we want to insert into JSON template
   * @return JSON template string with inca.report already resolved.
   * @throws JSONException
   */
  private String getJsonTemplateWithReport(Report report) throws JSONException {
    ReportDocument reportDoc = ReportDocument.Factory.newInstance();
    reportDoc.setReport(report);
    // replace double quotes with single quotes because JSON objects use double quotes
    JSONObject jsonObj = XML.toJSONObject(reportDoc.xmlText().replace("\"", "'"));

    String json_template = m_json_template;
    return json_template.replace(m_report_fieldname, jsonObj.getJSONObject("rep:report").toString());
  }

  /**
   * Get the interpreted success or failure of incoming report.
   *
   * @param report   Incoming report object
   * @param instance Incoming instance object
   * @param dbSc     Matching series config object
   * @return String if successfully inteprets success or failure; otherwise null
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  private String getReportResult(Report report, InstanceInfo instance, SeriesConfig dbSc) throws IOException, SQLException, PersistenceException {
    edu.sdsc.inca.depot.persistent.Report dbReport;

    try {
      dbReport = new edu.sdsc.inca.depot.persistent.Report(instance.getReportId());
    }
    catch (IOException | SQLException | PersistenceException err) {
      m_logger.warn("InstanceInfo " + instance.getId() + " has invalid Report id " + instance.getReportId());

      return null;
    }

    String testResult = report.getExitStatus().getCompleted() ? "success" : "error";

    AcceptedOutput ao = dbSc.getAcceptedOutput();
    if (ao != null) {
      String comparitor = ao.getComparitor();
      if (comparitor == null || comparitor.length() < 1 || comparitor.equals(Row.DB_EMPTY_STRING)) {
        m_logger.warn("Found empty comparitor for series " + dbSc.getNickname());
        return testResult;
      }

      String comparison = (new ExprComparitor()).compare(ao, dbReport);
      testResult = comparison.startsWith(ExprComparitor.FAILURE_RESULT) ? "error" : "success";
    }
    return testResult;
  }

  /**
   * Open specified file.
   *
   * @param fileName Path of file to open
   * @return InputStream if successfully opened otherwise null
   * @throws FileNotFoundException
   */
  private InputStream openStream(String fileName) throws FileNotFoundException {
    InputStream result = ClassLoader.getSystemClassLoader().getResourceAsStream(fileName);

    if (result == null)
      result = new FileInputStream(fileName);

    return result;
  }

  /**
   * Read PEM formatted file.
   *
   * @param fileName Path of PEM file to read.
   * @return Object representing PEM file.
   * @throws IOException
   */
  private Object readPEMFile(String fileName) throws IOException {
    InputStream inStream = openStream(fileName);
    PEMParser parser = null;

    try {
      parser = new PEMParser(new InputStreamReader(inStream));

      return parser.readObject();
    } finally {
      if (parser != null)
        parser.close();
      else
        inStream.close();
    }
  }

  /**
   * Read certificate file.
   *
   * @param fileName Path to certificate file.
   * @return X509 certificate object.
   * @throws IOException
   * @throws CertificateException
   */
  private X509Certificate readCertFile(String fileName) throws IOException, CertificateException {
    X509CertificateHolder certHolder = (X509CertificateHolder) readPEMFile(fileName);
    JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();

    certConverter.setProvider(PROVIDER);

    return certConverter.getCertificate(certHolder);
  }

  /**
   * Read credentials for authenticating to AMQP server.
   *
   * @param certFile      Path to certificate file to authenticate against AMQP server
   * @param keyFile       Path to certificate key to authenticate against AMQP server
   * @param keyPassPhrase Passphrase for certificate key
   * @return Keystore to authenticate to AMQP server
   * @throws IOException
   * @throws KeyStoreException
   * @throws NoSuchProviderException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   */
  private KeyStore readCredentials(String certFile, String keyFile, String keyPassPhrase) throws IOException, KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException {
    X509Certificate cert = readCertFile(certFile);
    KeyPair keyPair = readKeyFile(keyFile, keyPassPhrase);
    String alias = getAlias(cert);
    KeyStore.PrivateKeyEntry entry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), new Certificate[]{cert});
    KeyStore.PasswordProtection prot = new KeyStore.PasswordProtection(keyPassPhrase.toCharArray());
    KeyStore store = KeyStore.getInstance("PKCS12", PROVIDER);

    store.load(null, null);
    store.setEntry(alias, entry, prot);

    return store;
  }

  /**
   * Read key file.
   *
   * @param fileName Path to keyfile.
   * @param password Password on specified key file.
   * @return Key pair object.
   * @throws IOException
   * @throws KeyStoreException
   */
  private KeyPair readKeyFile(String fileName, final String password) throws IOException, KeyStoreException {
    Object pemObject = readPEMFile(fileName);
    PEMKeyPair pemKey;

    if (pemObject instanceof PEMKeyPair)
      pemKey = (PEMKeyPair) pemObject;
    else if (pemObject instanceof PEMEncryptedKeyPair) {
      PEMEncryptedKeyPair encryptedKey = (PEMEncryptedKeyPair) pemObject;
      JcePEMDecryptorProviderBuilder decryptBuilder = new JcePEMDecryptorProviderBuilder();

      decryptBuilder.setProvider(PROVIDER);

      PEMDecryptorProvider decryptor = decryptBuilder.build(password.toCharArray());

      pemKey = encryptedKey.decryptKeyPair(decryptor);
    } else
      throw new KeyStoreException(fileName + " does not contain a key pair");

    JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();

    keyConverter.setProvider(PROVIDER);

    return keyConverter.getKeyPair(pemKey);
  }

  /**
   * Create trust store from specified certificate files.
   *
   * @param certFiles List of certificate files to add to trust store
   * @return Keystore of trusted certificates.
   * @throws IOException
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   */
  private KeyStore readTrustedCerts(String[] certFiles) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
    KeyStore store = KeyStore.getInstance("JKS");

    store.load(null, null);

    for (String file : certFiles) {
      X509Certificate cert = readCertFile(file);
      KeyStore.TrustedCertificateEntry entry = new KeyStore.TrustedCertificateEntry(cert);
      String alias = getAlias(cert);
      m_logger.debug("Adding trusted certificate: " + alias);

      store.setEntry(alias, entry, null);
    }

    return store;
  }

  /**
   * Resolve variables in specified jsonObject using variables stored in jsonValues.  Any keys specified in ignoreKeys
   * will be skipped.
   *
   * @param jsonValues Dictionary of variable names and values.
   * @param jsonObject A JSONObject that may contain variables to be resolved.
   * @param ignoreKeys Do not try to resolve any of these specified keys in jsonObject
   * @throws JSONException
   */
  private void resolveJSONVars(HashMap<String, String> jsonValues, JSONObject jsonObject, String[] ignoreKeys) throws JSONException {
    String[] keys = JSONObject.getNames(jsonObject);
    for (String key : keys) {
      boolean ignoreThisKey = false;
      for (String ignoreKey : ignoreKeys) {
        if (ignoreKey.equals(key))
          ignoreThisKey = true;
      }
      if (ignoreThisKey) {
        continue;
      }
      Object obj = jsonObject.get(key);
      if (obj instanceof JSONObject) {
        JSONObject subObject = (JSONObject) obj;
        resolveJSONVars(jsonValues, subObject, ignoreKeys);
        if (subObject.length() < 1) {
          m_logger.warn("Removing " + key + " object with no keys");
          jsonObject.remove(key);
        }
      } else if (obj instanceof String) {
        String json_value = jsonObject.getString(key);
        String resolved_json_value = resolveStringVars(jsonValues, json_value);
        if (resolved_json_value.matches("^.*@[^@]+@.*$")) {
          m_logger.warn("Removing key " + key + " with unresolved variables: " + resolved_json_value);
          jsonObject.remove(key);
        } else {
          jsonObject.put(key, resolved_json_value);
        }
      } else {
        m_logger.debug("Ignoring json template entry: " + key + " of type " + obj.getClass());
      }

    }
  }

  /**
   * Resolve variables in specified string using values specified in jsonValues.
   *
   * @param jsonValues Dictionary of variable names and values
   * @param varString  String that may have variables that need to be resolved.
   * @return Resolved string.
   */
  private String resolveStringVars(HashMap<String, String> jsonValues, String varString) {
    for (Map.Entry<String, String> jsonVar : jsonValues.entrySet()) {
      if (jsonVar.getValue() != null)
        varString = varString.replace("@" + jsonVar.getKey() + "@", jsonVar.getValue());
    }
    return varString;
  }

  /**
   * Send report to Amqp Server.
   *
   * @param instance   Instance matched to report
   * @param routingKey Routing key to send message
   * @param message    Content of message
   * @param factory    Amqp server.
   * @return True if send was successful; otherwise False
   * @throws IOException
   * @throws TimeoutException
   */
  private boolean sendJSONToAmqpServer(InstanceInfo instance, String routingKey, byte[] message, ConnectionFactory factory) throws IOException, TimeoutException {
    Connection connection = null;
    Channel channel = null;

    try {
      connection = factory.newConnection();
      channel = connection.createChannel();

      channel.basicPublish(m_exchange, routingKey, null, message);
      m_logger.debug("Published a report for InstanceInfo " + instance.getId() + " using routing key " + routingKey);
      return true;
    } catch (IOException ioErr) {
      int port = factory.getPort();
      String virtualHost = factory.getVirtualHost();
      String errMessage = ioErr.getMessage();
      StringBuilder logMessage = new StringBuilder();

      logMessage.append("Unable to publish a report at ");
      logMessage.append(factory.getHost());

      if (port != ConnectionFactory.DEFAULT_AMQP_PORT && port != ConnectionFactory.DEFAULT_AMQP_OVER_SSL_PORT) {
        logMessage.append(':');
        logMessage.append(port);
      }

      if (!virtualHost.equals(ConnectionFactory.DEFAULT_VHOST)) {
        logMessage.append('/');
        logMessage.append(virtualHost);
      }

      if (errMessage != null && errMessage.length() > 0) {
        logMessage.append(": ");
        logMessage.append(errMessage);
      }

      m_logger.warn(logMessage.toString());
    } finally {
      if (channel != null && channel.isOpen())
        channel.close();

      if (connection != null && connection.isOpen())
        connection.close();
    }
    return false;
  }

  protected class AmqpMessage {
    public String json;
    public String routingKey;

    public AmqpMessage(String routingKey, String json) {
      this.routingKey = routingKey;
      this.json = json;
    }
  }
}
