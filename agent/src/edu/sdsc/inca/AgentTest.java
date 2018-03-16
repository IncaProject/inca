package edu.sdsc.inca;

import junit.framework.TestCase;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import edu.sdsc.inca.agent.RepositoryCacheTest;
import edu.sdsc.inca.agent.RepositoryCache;
import edu.sdsc.inca.agent.ReporterManagerController;
import edu.sdsc.inca.dataModel.util.Resource;
import edu.sdsc.inca.protocol.MessageHandlerFactory;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.ProtocolWriter;
import edu.sdsc.inca.protocol.StandardMessageHandler;
import edu.sdsc.inca.protocol.Statement;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.repository.Repositories;
import edu.sdsc.inca.repository.RepositoriesTest;
import edu.sdsc.inca.repository.Repository;
import edu.sdsc.inca.util.ConfigProperties;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.util.ResourcesWrapperTest;
import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.util.SuiteStagesWrapper;
import edu.sdsc.inca.util.SuiteWrapper;

import org.apache.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

/**
 * Tester class for Agent.  Other tests are in the AgentClientTest class.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class AgentTest extends TestCase {

  public static final String RESOURCES_FILE = "test/sampleresources.xml";
  public static final String TEST_RESOURCE = "localhost";
  public static final String REPOSITORY = "test/repository";
  public static final int LOW_LEVEL = 0;
  public static final int HIGH_LEVEL = 1;

  public static final String AGENT_CERT =
    "Certificate:\n" +
    "    Data:\n" +
    "        Version: 3 (0x2)\n" +
    "        Serial Number: 1 (0x1)\n" +
    "        Signature Algorithm: md5WithRSAEncryption\n" +
    "        Issuer: C=US, ST=California, O=UCSD, OU=SDSC, CN=IncaDefaultCA_client64-120.sdsc.edu\n" +
    "        Validity\n" +
    "            Not Before: Oct  2 18:25:01 2008 GMT\n" +
    "            Not After : Sep 30 18:25:01 2018 GMT\n" +
    "        Subject: C=US, ST=California, O=UCSD, OU=SDSC, CN=agent\n" +
    "        Subject Public Key Info:\n" +
    "            Public Key Algorithm: rsaEncryption\n" +
    "            RSA Public Key: (512 bit)\n" +
    "                Modulus (512 bit):\n" +
    "                    00:f2:b1:03:e8:21:f8:de:37:55:9f:46:1b:9d:41:\n" +
    "                    23:1e:75:2e:89:94:eb:e6:3f:dd:7c:d8:58:48:96:\n" +
    "                    5f:13:90:98:63:bd:ae:0e:7a:cc:1b:ee:25:7c:e6:\n" +
    "                    7f:23:6c:c6:60:40:66:f0:62:56:35:3e:3b:f5:a9:\n" +
    "                    8a:87:bc:6a:7b\n" +
    "                Exponent: 65537 (0x10001)\n" +
    "        X509v3 extensions:\n" +
    "            X509v3 Basic Constraints: critical\n" +
    "                CA:TRUE\n" +
    "    Signature Algorithm: md5WithRSAEncryption\n" +
    "        45:5a:0f:05:ad:48:2a:60:27:c0:3b:d1:2c:6c:eb:f2:2d:8e:\n" +
    "        e3:69:99:16:70:6c:af:67:c2:f3:52:f8:66:74:54:2e:50:b8:\n" +
    "        56:d2:89:61:d9:c8:b7:4c:22:4d:df:13:c6:54:aa:19:67:10:\n" +
    "        c3:e0:5c:3d:46:93:b8:78:71:23\n" +
    "-----BEGIN CERTIFICATE-----\n" +
    "MIIBwjCCAWygAwIBAgIBATANBgkqhkiG9w0BAQQFADBuMQswCQYDVQQGEwJVUzET\n" +
    "MBEGA1UECBMKQ2FsaWZvcm5pYTENMAsGA1UEChMEVUNTRDENMAsGA1UECxMEU0RT\n" +
    "QzEsMCoGA1UEAxQjSW5jYURlZmF1bHRDQV9jbGllbnQ2NC0xMjAuc2RzYy5lZHUw\n" +
    "HhcNMDgxMDAyMTgyNTAxWhcNMTgwOTMwMTgyNTAxWjBQMQswCQYDVQQGEwJVUzET\n" +
    "MBEGA1UECBMKQ2FsaWZvcm5pYTENMAsGA1UEChMEVUNTRDENMAsGA1UECxMEU0RT\n" +
    "QzEOMAwGA1UEAxMFYWdlbnQwXDANBgkqhkiG9w0BAQEFAANLADBIAkEA8rED6CH4\n" +
    "3jdVn0YbnUEjHnUuiZTr5j/dfNhYSJZfE5CYY72uDnrMG+4lfOZ/I2zGYEBm8GJW\n" +
    "NT479amKh7xqewIDAQABoxMwETAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEB\n" +
    "BAUAA0EARVoPBa1IKmAnwDvRLGzr8i2O42mZFnBsr2fC81L4ZnRULlC4VtKJYdnI\n" +
    "t0wiTd8TxlSqGWcQw+BcPUaTuHhxIw==\n" +
    "-----END CERTIFICATE-----\n";
  public static final String AGENT_KEY =
    "-----BEGIN RSA PRIVATE KEY-----\n" +
    "Proc-Type: 4,ENCRYPTED\n" +
    "DEK-Info: DES-EDE3-CBC,AE852D26BA137F89\n" +
    "\n" +
    "IIEGQruw0SMB8IWepDwTclhI2rZttn54TZNLaNfeotktmJ/BX2Wki48rbMYrCbRw\n" +
    "lifksZtS3S3jhdB3exJ6LUs3cVdiceHzz07T3BKY6/UsFweIh1Z9gwGYjkPcb3N+\n" +
    "pMOrpfylN9PIsU8+xXG3q5DF8IUge8Y/uL42qc0yuBeKXMOT7KBcEnUFfshalDEp\n" +
    "gKxh+44flUwnnG86nX3bJc/wmUTPr5DHNjrBO4dU9H+147YwsawuyGKhpVRKqeOy\n" +
    "HB/l2gPy9AaYu17ailZVDKj1D7nyW841uuTpyX6ctqWSEDZ9HrVYJ8ipslYSNKvp\n" +
    "9DlPhyC5yEXsxamxhMkb1W7SnmTNyl12XNkoCwOrzlN8n+QwsuxjBw8gA3jp1GpU\n" +
    "572DEH6+AEKuCTh3LnuuUENI+uhMN0oUZngrR6wG1sE=\n" +
    "-----END RSA PRIVATE KEY-----\n";
  public static final String CA_CERT =
    "-----BEGIN CERTIFICATE-----\n" +
    "MIIB6DCCAZKgAwIBAgIJALqC9c+YJ6BeMA0GCSqGSIb3DQEBBAUAMG4xCzAJBgNV\n" +
    "BAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMQ0wCwYDVQQKEwRVQ1NEMQ0wCwYD\n" +
    "VQQLEwRTRFNDMSwwKgYDVQQDFCNJbmNhRGVmYXVsdENBX2NsaWVudDY0LTEyMC5z\n" +
    "ZHNjLmVkdTAeFw0wODEwMDIxODI1MDFaFw0xODA5MzAxODI1MDFaMG4xCzAJBgNV\n" +
    "BAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMQ0wCwYDVQQKEwRVQ1NEMQ0wCwYD\n" +
    "VQQLEwRTRFNDMSwwKgYDVQQDFCNJbmNhRGVmYXVsdENBX2NsaWVudDY0LTEyMC5z\n" +
    "ZHNjLmVkdTBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQCh9L+oWy8h9h+TFalsmoFc\n" +
    "tszdbHJhROwk34ENqRcyQeGk/WgUzi8wRF/sdJJHt66NGd1MCs1CEJ/Sp3VdzPqN\n" +
    "AgMBAAGjEzARMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQEEBQADQQCcBQvk\n" +
    "7Eyrnfc3xck7LY1RtnuBPz+zeXdAkpSXAsvshVKO3VV3bDUYM6ZqWO/rj5VXFrtO\n" +
    "Hrz8huS/0DjxO5D1\n" +
    "-----END CERTIFICATE-----\n";
  public static final String DEPOT_CERT =
    "Certificate:\n" +
    "    Data:\n" +
    "        Version: 3 (0x2)\n" +
    "        Serial Number: 3 (0x3)\n" +
    "        Signature Algorithm: md5WithRSAEncryption\n" +
    "        Issuer: C=US, ST=California, O=UCSD, OU=SDSC, CN=IncaDefaultCA_client64-120.sdsc.edu\n" +
    "        Validity\n" +
    "            Not Before: Oct  2 18:25:02 2008 GMT\n" +
    "            Not After : Sep 30 18:25:02 2018 GMT\n" +
    "        Subject: C=US, ST=California, O=UCSD, OU=SDSC, CN=depot\n" +
    "        Subject Public Key Info:\n" +
    "            Public Key Algorithm: rsaEncryption\n" +
    "            RSA Public Key: (512 bit)\n" +
    "                Modulus (512 bit):\n" +
    "                    00:b3:4c:78:f9:67:07:87:34:ed:ce:6f:82:8b:83:\n" +
    "                    66:8a:d1:06:9d:fe:45:d4:68:77:18:4f:d1:03:84:\n" +
    "                    dd:f0:49:2c:1b:8c:ad:9f:2f:2c:b4:43:ab:85:c9:\n" +
    "                    ed:91:16:1b:bf:36:83:e6:5c:b7:1e:7d:22:c7:a8:\n" +
    "                    c0:d5:00:0b:07\n" +
    "                Exponent: 65537 (0x10001)\n" +
    "        X509v3 extensions:\n" +
    "            X509v3 Basic Constraints: critical\n" +
    "                CA:FALSE\n" +
    "            X509v3 Key Usage: critical\n" +
    "                Digital Signature, Key Encipherment\n" +
    "    Signature Algorithm: md5WithRSAEncryption\n" +
    "        7f:9a:69:02:a8:95:5c:43:18:3c:dc:bc:79:34:7e:43:af:85:\n" +
    "        df:85:1a:6e:99:ef:77:31:19:8e:21:f0:d2:fb:4b:c2:87:1c:\n" +
    "        5d:86:75:1f:2c:52:d6:90:1c:70:df:0c:28:09:2a:1e:53:10:\n" +
    "        e1:b3:d3:1d:94:b6:79:4b:a5:6a\n" +
    "-----BEGIN CERTIFICATE-----\n" +
    "MIIBzzCCAXmgAwIBAgIBAzANBgkqhkiG9w0BAQQFADBuMQswCQYDVQQGEwJVUzET\n" +
    "MBEGA1UECBMKQ2FsaWZvcm5pYTENMAsGA1UEChMEVUNTRDENMAsGA1UECxMEU0RT\n" +
    "QzEsMCoGA1UEAxQjSW5jYURlZmF1bHRDQV9jbGllbnQ2NC0xMjAuc2RzYy5lZHUw\n" +
    "HhcNMDgxMDAyMTgyNTAyWhcNMTgwOTMwMTgyNTAyWjBQMQswCQYDVQQGEwJVUzET\n" +
    "MBEGA1UECBMKQ2FsaWZvcm5pYTENMAsGA1UEChMEVUNTRDENMAsGA1UECxMEU0RT\n" +
    "QzEOMAwGA1UEAxMFZGVwb3QwXDANBgkqhkiG9w0BAQEFAANLADBIAkEAs0x4+WcH\n" +
    "hzTtzm+Ci4NmitEGnf5F1Gh3GE/RA4Td8EksG4ytny8stEOrhcntkRYbvzaD5ly3\n" +
    "Hn0ix6jA1QALBwIDAQABoyAwHjAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIF\n" +
    "oDANBgkqhkiG9w0BAQQFAANBAH+aaQKolVxDGDzcvHk0fkOvhd+FGm6Z73cxGY4h\n" +
    "8NL7S8KHHF2GdR8sUtaQHHDfDCgJKh5TEOGz0x2UtnlLpWo=\n" +
    "-----END CERTIFICATE-----\n";
  public static final String DEPOT_KEY =
    "-----BEGIN RSA PRIVATE KEY-----\n" +
    "Proc-Type: 4,ENCRYPTED\n" +
    "DEK-Info: DES-EDE3-CBC,CE8FD2268E0DE3B8\n" +
    "\n" +
    "3sE6LEovqKmHvI4hL1Q9xYT8ArqnXgnn/q+4STq7BPy7DpiYgpiJjjUSNG7vjvtv\n" +
    "Yy8f6Gp/Ho6cU1QagxbSjFdnEBQuNPkgaLNyck2jJZDP1DgF2iZMTiIQhoSRl1/1\n" +
    "J4AmEMia5DkjtHH8jJgAvzDPUXjPWkYyIrqnSaGbV88YiC3Fn3QJDCYN4VExnCFW\n" +
    "2ukF7zGdJHiGYItnR4Ank9YHXXRVD1Gu0gAjqRZl+nzdCesY2DefAMS+CLBzQLPa\n" +
    "Mc+l0VadtWAQQuw4qk8xCEZqGvlFufsVJRihXglO4rttTu4LGf9CDjWLLQv/lDSW\n" +
    "bUswD9UOhC1QDVKkejp8bbGswhXkfGvq2m9G0gW4rUBPkwugNevzJ8J25Ijxz9G+\n" +
    "MJujlnO3aLtTqj8LSmGB7OBOdTcnN90EWcRUUUNV1fA=\n" +
    "-----END RSA PRIVATE KEY-----\n";
  public static final String INCAT_CERT =
    "Certificate:\n" +
    "    Data:\n" +
    "        Version: 3 (0x2)\n" +
    "        Serial Number: 4 (0x4)\n" +
    "        Signature Algorithm: md5WithRSAEncryption\n" +
    "        Issuer: C=US, ST=California, O=UCSD, OU=SDSC, CN=IncaDefaultCA_client64-120.sdsc.edu\n" +
    "        Validity\n" +
    "            Not Before: Oct  2 18:25:02 2008 GMT\n" +
    "            Not After : Sep 30 18:25:02 2018 GMT\n" +
    "        Subject: C=US, ST=California, O=UCSD, OU=SDSC, CN=incat\n" +
    "        Subject Public Key Info:\n" +
    "            Public Key Algorithm: rsaEncryption\n" +
    "            RSA Public Key: (512 bit)\n" +
    "                Modulus (512 bit):\n" +
    "                    00:ba:f9:84:28:86:74:90:18:08:e9:91:3a:16:12:\n" +
    "                    75:b3:41:60:de:a8:cd:58:e3:f4:20:19:95:f7:c0:\n" +
    "                    c7:de:db:26:f9:8c:e4:77:c7:46:3b:11:f0:66:40:\n" +
    "                    05:b7:e4:2d:85:0b:9a:3a:c5:ff:4c:e2:ad:f9:b0:\n" +
    "                    3e:98:c7:b5:59\n" +
    "                Exponent: 65537 (0x10001)\n" +
    "        X509v3 extensions:\n" +
    "            X509v3 Basic Constraints: critical\n" +
    "                CA:FALSE\n" +
    "            X509v3 Key Usage: critical\n" +
    "                Digital Signature, Key Encipherment\n" +
    "    Signature Algorithm: md5WithRSAEncryption\n" +
    "        65:3b:a7:a6:3d:04:7d:f4:4a:f1:fd:d7:8a:7d:15:a2:e9:f1:\n" +
    "        9b:cc:ba:8c:99:3d:c7:f6:83:1e:76:17:fc:19:e9:c0:c7:88:\n" +
    "        ee:4c:77:f0:93:87:6b:d1:c4:70:62:b4:cc:7d:05:e4:f0:44:\n" +
    "        25:dc:56:dc:98:3b:43:f1:a8:5d\n" +
    "-----BEGIN CERTIFICATE-----\n" +
    "MIIBzzCCAXmgAwIBAgIBBDANBgkqhkiG9w0BAQQFADBuMQswCQYDVQQGEwJVUzET\n" +
    "MBEGA1UECBMKQ2FsaWZvcm5pYTENMAsGA1UEChMEVUNTRDENMAsGA1UECxMEU0RT\n" +
    "QzEsMCoGA1UEAxQjSW5jYURlZmF1bHRDQV9jbGllbnQ2NC0xMjAuc2RzYy5lZHUw\n" +
    "HhcNMDgxMDAyMTgyNTAyWhcNMTgwOTMwMTgyNTAyWjBQMQswCQYDVQQGEwJVUzET\n" +
    "MBEGA1UECBMKQ2FsaWZvcm5pYTENMAsGA1UEChMEVUNTRDENMAsGA1UECxMEU0RT\n" +
    "QzEOMAwGA1UEAxMFaW5jYXQwXDANBgkqhkiG9w0BAQEFAANLADBIAkEAuvmEKIZ0\n" +
    "kBgI6ZE6FhJ1s0Fg3qjNWOP0IBmV98DH3tsm+Yzkd8dGOxHwZkAFt+QthQuaOsX/\n" +
    "TOKt+bA+mMe1WQIDAQABoyAwHjAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIF\n" +
    "oDANBgkqhkiG9w0BAQQFAANBAGU7p6Y9BH30SvH914p9FaLp8ZvMuoyZPcf2gx52\n" +
    "F/wZ6cDHiO5Md/CTh2vRxHBitMx9BeTwRCXcVtyYO0PxqF0=\n" +
    "-----END CERTIFICATE-----\n";
  public static final String INCAT_KEY =
    "-----BEGIN RSA PRIVATE KEY-----\n" +
    "Proc-Type: 4,ENCRYPTED\n" +
    "DEK-Info: DES-EDE3-CBC,94EDAFCD09759D88\n" +
    "\n" +
    "2Duo3HtyOgHtQ2FmaLhpFbEHuT2P7ho3IbKKOXZHXg+GGA7wXUJqDQKa/BEIygvz\n" +
    "QfOBsFh3cJrz8zS/A6Nx7mz6FeQYZJ8QDiuAwNbpF6WRFNxCIWZiT4Oaw6bwJlLC\n" +
    "T1NXAdjBWyB7hVgBvTwXIehZqqs8hWoG/fLmNanmnOeTnB8kJCeC8zG9yX5wGezG\n" +
    "QgouHqM7Hvcan9TZhHew2nn9qp4qX4wAa6P/OJND9USs/7LYz5JIsE3Gfsn5BoYF\n" +
    "twaMDIlyZheNMd0H4OQSjOR5j2ZnIRKmAJnKR1IuCwLtJlS1Yy9yyq1sGQQsomb9\n" +
    "nyiVPF21EEDgswLqxdTa+NjjRQNp4muvPDFC02r9U7Ba4Am1PqYOVkhQcoDXJ1S/\n" +
    "lluotUl367s9JYSnv0bx7tVbv5LTMNK27iK/PyYbMBo=\n" +
    "-----END RSA PRIVATE KEY-----\n";
  public static final String PASSWORD = "abcdefg";

  static private Logger logger = Logger.getLogger(AgentTest.class);

  /**
   * A pseudo-depot used to test Agent.
   */
  static public class MockDepot extends Server {

    static {
      MessageHandlerFactory.registerMessageHandler
        (Protocol.INSERT_COMMAND, "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
        (Protocol.QUERY_DB_COMMAND, "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
        (Protocol.QUERY_GUIDS_COMMAND,
         "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
        (Protocol.QUERY_HQL_COMMAND,
         "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
      (Protocol.QUERY_INSTANCE_COMMAND,
       "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
        (Protocol.QUERY_INSTANCE_BY_ID_COMMAND,
         "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
        (Protocol.QUERY_LATEST_COMMAND,
         "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
        (Protocol.QUERY_PERIOD_COMMAND,
         "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
        (Protocol.QUERY_SQL_COMMAND,
         "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
        (Protocol.QUERY_STATUS_COMMAND,
         "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
        (Protocol.RESEND_COMMAND, "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
        (Protocol.QUERY_DEPOT_PEERS_COMMAND, "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
      MessageHandlerFactory.registerMessageHandler
        (Protocol.SUITE_UPDATE_COMMAND,
         "edu.sdsc.inca.AgentTest$MockDepot$StubMH");
    }

    private Vector<String> configs = new Vector<String>();
    private Vector<String> peers = new Vector<String>();
    private Vector<String> reports = new Vector<String>();
    private Vector<String> suites = new Vector<String>();
    private Logger logger = Logger.getLogger( this.getClass().getName() ) ;
    protected static MockDepot md;

    /**
     * Default Constructor.
     */
    public MockDepot( ) {
      super();
      MockDepot.md = this;
    }

    public void addConfig( String xml ) {
      this.configs.add( xml );
    }

    public void addPeer( String uri ) {
      this.peers.add( uri );
    }

    public void addReport( String xml ) {
      this.reports.add( xml );
    }

    public void addSuite( String xml ) {
      this.suites.add( xml );
    }

    public boolean checkReportsForPatterns( String[] patterns ) {
      for( String patternString : patterns ) {
        Pattern pattern = Pattern.compile(patternString);
        int j;
        for ( j = 0; j < this.reports.size(); j++ ) {
          if ( pattern.matcher(this.reports.get(j)).find() ) break;
        }
        if ( j >= this.reports.size() ) {
          logger.info( "Pattern " + patternString + " not found in any report" );
          return false;
        }
      }
      return true;
    }

    public void clearReports() {
      this.reports.clear();
    }

    public static MockDepot getDepot() {
      return MockDepot.md;
    }

    public String[] getReports() {
      return this.reports.toArray(new String[this.reports.size()]);
    }

    public String[] getSuites() {
      return this.suites.toArray(new String[this.suites.size()]);
    }

    public void readCredentials() throws ConfigurationException, IOException {
      try {
        this.cert = readCertData(DEPOT_CERT);
        this.key = readKeyData(DEPOT_KEY, PASSWORD);
      }
      catch (Exception err) {
        throw new ConfigurationException(err);
      }
      this.trusted = new Vector();
      this.trusted.add(readPEMData(CA_CERT));
    }

    public String[] waitForReports( int numReports ) {
      MockDepot depot = MockDepot.getDepot();
      for( int i = 0; i < 100 && depot.getReports().length < numReports; i++ ) {
        logger.debug
          ( "Sleeping 10 seconds; received " + depot.getReports().length +
            " reports; expecting " + numReports );
        try {
          Thread.sleep( 10000 );
        } catch (InterruptedException e) { /* do nothing */ }
      }
      return depot.getReports();
    }

    /**
     * Generic message handler for mock depot server
     */
    public static class StubMH extends StandardMessageHandler {
      public void execute(ProtocolReader reader,
                          ProtocolWriter writer,
                          String dn) throws Exception {
        Statement s = reader.readStatement();
        String cmd = new String(s.getCmd());
        String data = new String(s.getData());
        logger.info( "Received command " + cmd );
        if(cmd.equals(Protocol.INSERT_COMMAND)) {
          s = reader.readStatement(); // read STDERR/STDOUT
          logger.debug( "Mock depot received report" );
          getDepot().addReport( s.toString() );
          if(new String(s.getCmd()).equals(Protocol.INSERT_STDERR_COMMAND)) {
            reader.readStatement();  // read STDOUT
          }
          reader.readStatement();  // read SYSUSAGE
          writer.write(new Statement(Protocol.SUCCESS_COMMAND, data));
        } else if(cmd.equals(Protocol.SUITE_UPDATE_COMMAND)) {
          getDepot().addSuite(data);
          writer.write(new Statement(Protocol.SUCCESS_COMMAND, "1"));
        } else if(cmd.equals(Protocol.QUERY_DEPOT_PEERS_COMMAND)) {
          String peerString = StringMethods.join(
            "\n",
            getDepot().peers.toArray(new String[getDepot().peers.size()])
          );
          writer.write(new Statement(Protocol.SUCCESS_COMMAND, peerString ));
        } else {
          throw new ProtocolException("Unsupported command: " + cmd);
        }
      }
    }
  }

  /**
   * Cleanup the agent if it was started
   *
   * @throws Exception
   */
  public void setUp() throws Exception {
    File tempDir = new File( "var" );
    StringMethods.deleteDirectory( tempDir );
    StringMethods.deleteDirectory(new File("/tmp/inca2install2"));
  }

  /**
   * Start up the agent server
   *
   * @param auth        Configure the agent with authentication
   *
   * @param repository  Configure the agent with a temporary repository
   *
   * @return the Agent object
   *
   * @throws Exception  if trouble creating new agent server
   */
  public static Agent startAgent( boolean auth, boolean repository )
    throws Exception {

    Agent agent = new Agent() {
      public DepotClient[] getDepotClients() {
        DepotClient[] clients = new DepotClient[depots.length];
        for (int i = 0 ; i < depots.length ; i += 1) {
          clients[i] = new DepotClient() {
            public void readCredentials() throws ConfigurationException, IOException {
              try {
                this.cert = readCertData(AGENT_CERT);
                this.key = readKeyData(AGENT_KEY, PASSWORD);
              }
              catch (Exception err) {
                throw new ConfigurationException(err);
              }
              this.password = PASSWORD;
              this.trusted = new Vector();
              this.trusted.add(readPEMData(AGENT_CERT));
              this.trusted.add(readPEMData(CA_CERT));
            }
          };
          depotClientConfig.setProperty("depot", depots[i]);
          try {
            clients[i].setConfiguration(depotClientConfig);
          } catch (ConfigurationException configErr) {
            logger.error("Unable to configure depot clients: " + configErr.getMessage());
            return null;
          }
        }
        return clients;
      }

      public void readCredentials() throws ConfigurationException, IOException {
        try {
          this.cert = readCertData(AGENT_CERT);
          this.key = readKeyData(AGENT_KEY, PASSWORD);
        }
        catch (Exception err) {
          throw new ConfigurationException(err);
        }
        this.trusted = new Vector();
        this.trusted.add(readPEMData(AGENT_CERT));
        this.trusted.add(readPEMData(CA_CERT));
      }
    };
    ConfigProperties config = new ConfigProperties();
    config.putAllTrimmed(System.getProperties(), "inca.agent.");
    config.loadFromResource("inca.properties", "inca.agent.");
    config.setProperty( "auth", Boolean.toString(auth) );
    config.setProperty( "password", PASSWORD );
    config.remove( "logfile" );
    logger.debug( "Depot at " + MockDepot.getDepot().getUri() );
    if( MockDepot.getDepot() != null ) {
      config.setProperty( "depot", MockDepot.getDepot().getUri() );
    }
    agent.setConfiguration( config );

    if ( repository ) {
      Repositories repositories = RepositoriesTest.createSampleRepository(null);
      RepositoryCache cache = RepositoryCacheTest.createSampleRepositoryCache(
        repositories
      );
      agent.setRepositories( repositories.getRepositories() );
      agent.setRepositoryCache( cache );
    }

    agent.runServer( );
    assertTrue( "agent is alive", agent.isRunning() );

    Agent.setGlobalAgent( agent );
    agent.registerDepotPermissions( );
    return agent;

  }

  /**
   * Launches a MockDepot to which the Agent may connect.
   *
   * @param auth start depot with or without authentication
   *
   * @return the depot object
   *
   * @throws Exception if unable to start depot
   */
  public static MockDepot startDepot( boolean auth ) throws Exception {
    logger.info( "Starting mock depot" );
    MockDepot depot = new MockDepot( );
    ConfigProperties config = new ConfigProperties();
    config.putAllTrimmed(System.getProperties(), "inca.depot.");
    config.loadFromResource("inca.properties", "inca.depot.");
    config.setProperty( "auth", Boolean.toString(auth) );
    config.setProperty("password", PASSWORD);
    config.remove( "logfile" );
    depot.setConfiguration(config);
    depot.runServer();
    logger.debug( "Mock depot at " + depot.getUri() );
    return depot;
  }

  /**
   * Stops any running Agent.
   */
  public static void stopAgent( ) {
    if(Agent.getGlobalAgent() == null) {
      return;
    }
    try {
      logger.debug( "Trying to shutdown agent" );
      Agent.getGlobalAgent().shutdown();
      logger.debug( "Shutdown complete" );
    } catch(InterruptedException e) {
      // empty
    }
    Agent.setGlobalAgent( null );
  }

  /**
   * Stops any running MockDepot.
   */
  public static void stopDepot( ) {
    if(MockDepot.getDepot() == null) {
      return;
    }
    try {
      logger.debug( "Trying to shutdown agent" );
      MockDepot.getDepot().shutdown();
      logger.debug( "Shutdown complete" );
    } catch(InterruptedException e) {
      // empty
    }
  }

  /**
   * Return true if there is a system property inca.test.proxy indicating a
   * MyProxy server is available to test proxy related functionality
   *
   * @return True if proxy related tests should be run; false otherwise
   */
  public static boolean hasMyProxyServer() {
    Properties sysProps = System.getProperties();
    if ( sysProps.getProperty( "inca.test.myproxy") == null ) {
      logger.warn( "Skipping myproxy related test");
      return false;
    }
    return true;
  }

  /**
   * Return true if there is a system property inca.test.ssh indicating a
   * SSH server is available to test SSH related functionality
   *
   * @return True if SSH related tests should be run; false otherwise
   */
  public static boolean hasSshServer() {
    Properties sysProps = System.getProperties();
    if ( sysProps.getProperty( "inca.test.ssh") == null ) {
      logger.warn( "Skipping ssh related test");
      return false;
    }
    return true;
  }

  /**
   * Start up a remote reporter manager and wait for it to register
   *
   * @throws InterruptedException if receives interrupt
   */
  public void startReporterManager() throws Exception {
    logger.debug( "get reporter managerController" );

    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    Agent.getGlobalAgent().setResources( resources );
    ReporterManagerController managerController =
      Agent.getGlobalAgent().getReporterManager( TEST_RESOURCE );
    logger.debug( "got reporter managerController" );
    assertNotNull(managerController);
    assertFalse( "isRunning is false", managerController.isRunning() );
    assertTrue(
      "wait returned true",
      managerController.getReporterManagerStarter().waitForReporterManager()
    );
    assertTrue( "isRunning is true", managerController.isRunning() );
    logger.info( "Received reporter managerController" );
  }

  /**
   * Submit a suite to the Agent.
   *
   * @param suitePath    The suite to submit to the agent.
   *
   * @param numReports   The number of reports to have the mock depot receive
   *
   * @return A list of received reports from the mock depot
   *
   * @throws Exception  if trouble sending suite to agent
   */
  public String[] submitSuite( String suitePath, int numReports )
    throws Exception {

    Agent agent = Agent.getGlobalAgent();
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    agent.setResources( resources );

    SuiteWrapper suite = new SuiteWrapper( suitePath );
    String suiteName = suite.getSuiteDocument().getSuite().getName();
    agent.getRepositoryCache().resolveReporters( suite );
    logger.info( "Submitting suite " + suiteName );

    // create a blank suite
    SuiteStagesWrapper changes;
    if  ( ! agent.getSuites().hasSuite( suiteName ) ) {
      File suiteFile = File.createTempFile( "inca", ".xml" );
      if ( suiteFile.exists() ) suiteFile.delete();
      SuiteStagesWrapper suiteStages = new SuiteStagesWrapper(
        suiteFile.getAbsolutePath(), agent.getResources()
      );
      changes = suiteStages.modify(suite);
      agent.getSuites().putSuite( suiteStages );
    } else {
      SuiteStagesWrapper suiteStages = agent.getSuites().getSuite( suiteName );
      changes = suiteStages.modify(suite);
    }

    // read in samplesuitelocal.xml and apply it to suite
    HashMap resourceSuite = changes.getResourceSuites();
    assertEquals( "one suite extracted", 1, resourceSuite.size() );

    logger.debug( "Distributing suite to manager" );
    agent.distributeSuites( resourceSuite );

    logger.debug( "Reading reports" );
    String[] reports = MockDepot.getDepot().waitForReports(numReports);
    assertEquals( numReports + " reports received by depot", numReports,
                reports.length );

    return reports;

  }

  /**
   * Cleanup the agent if it was started
   *
   * @throws Exception
   */
  public void tearDown() throws Exception {
    stopAgent( );
    stopDepot( );
    File tempDir = new File( "var" );
    StringMethods.deleteDirectory( tempDir );
    StringMethods.deleteDirectory(new File("/tmp/inca2install2"));
  }

  /**
   * Tests the controller's distributeSubcriptions function.  Should start
   * up a reporter manager and a depot to receive the reports.
   *
   * @throws Exception if problem running test
   */
  public void testDeleteSuites() throws Exception {
    startDepot( false );
    startAgent( false, true );
    submitSuite( "test/samplesuitelocal.xml", 2 );

    MockDepot.getDepot().clearReports();

    Thread.sleep(2000);
    logger.info( "Preparing to delete one reporter " );
    String[] reports = submitSuite( "test/delete_samplesuitelocal.xml", 1 );
    assertTrue(
      "openssl report received",
      Pattern.compile( "openssl").matcher( reports[0] ).find()
    );
  }

  /**
   * Tests that the agent fetches the new depot uris correctly
   *
   * @throws Exception if problem running test
   */
  public void testDepotUriFetch() throws Exception {
    MockDepot depot = startDepot( false );
    Agent agent = startAgent( false, true );
    String[] depots = agent.getDepotUris();
    assertTrue( "1 depot returned", depots != null && depots.length == 1 );
    assertEquals( "depots retrieved", depots[0], depot.getUri() );
    depot.addPeer( "incas://fakehost1.sdsc.edu:6324");
    depot.addPeer( "incas://fakehost2.sdsc.edu:6324");
    depots = agent.getDepotUris();
    assertTrue("1 depot still returned", depots != null && depots.length == 1);
    agent.lastDepotUriFetch = 0; // force an update
    depots = agent.getDepotUris();
    assertTrue( "3 depots returned", depots != null && depots.length == 3 );
    assertEquals( "first depot ok", depots[0], depot.getUri() );
    assertEquals
      ( "second depot ok", depots[1], "incas://fakehost1.sdsc.edu:6324" );
  }

  /**
   * Tests the controller's distributeSubcriptions function.  Should start
   * up a reporter manager and a depot to receive the reports.
   *
   * @throws Exception if problem running test
   */
  public void testDistributeSuites() throws Exception {
    startDepot( false );
    startAgent( false, true );
    submitSuite( "test/samplesuitelocal.xml", 2 );
  }

  /**
   * Same as testDistributeSuites but with authentication
   *
   * @throws Exception if problem running test
   */
  public void testDistributeSuitesAuth() throws Exception {
    startDepot( true );
    startAgent( true, true );
    submitSuite( "test/samplesuitelocal.xml", 2 );
  }

  /**
   * Test the register task
   *
   * @throws Exception if problem running test
   */
  public void testRegisterAuth() throws Exception {
    startDepot( true );
    startAgent( true, false );
    startReporterManager();
  }

  /**
   * Test the register task
   *
   * @throws Exception if problem running test
   */
  public void testRegister() throws Exception {
    startDepot( false );
    startAgent( false, false );
    startReporterManager();
  }

  /**
   * Create a Agent object and configure it with a repositories file.  Create
   * a new Agent object and verify the repositories is there.  Set a new
   * repositories list and check the timestamp on the repositories file.
   *
   * @throws Exception if problem running test
   */
  public void testRepositoriesPersistence() throws Exception {
    startDepot( false );
    Agent agent = startAgent( false, false );
    assertEquals(
      "no resource yet",
      0,
      agent.getRepositories().getRepositories().length
    );
    Repositories repositories = RepositoriesTest.createSampleRepository(null);
    agent.setRepositories( repositories.getRepositories() );
    agent.updateCachedPackages();

    logger.debug( "Restarting agent" );
    stopAgent( );
    agent = startAgent( false, false );
    assertEquals(
      "found 1 repositories",
      1,
      agent.getRepositories().getRepositories().length
    );

    File file = new File( "var" + File.separator +  "repositories.xml" );
    long lastsave = file.lastModified();
    Thread.sleep(2000);
    Repository[] repos = new Repository[0];
    logger.debug( "Setting new repositories" );
    agent.setRepositories( repos );
    agent.updateCachedPackages();
    logger.debug( "Checking timestamps" );
    assertTrue( "repositories.xml modified", file.lastModified() > lastsave );
  }

  /**
   * 1) Save the sample resources to the tempdir and start a new agent and
   *    check the sample resources is available
   * 2) Save a new blank resources file in the agent and check the timestamp
   * 3) Configure the agent with a password and set a new resources file to
   *    be saved with encryption and then read it back in and verify the
   *    resource is there.
   *
   * @throws Exception if problem running test
   */
  public void testResourceConfigPersistence() throws Exception {
    File file = new File( "var" + File.separator + "resources.xml" );
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    resources.setFilePath( "var/resources.xml" );
    resources.setPassphrase( PASSWORD );
    resources.save( );
    int resourceCount = resources.getResourceConfigDocument().
      getResourceConfig().getResources().sizeOfResourceArray();
    startDepot( false );
    Agent agent = startAgent( false, false );
    assertEquals(
      "found " + resourceCount + " resources",
      resourceCount,
      agent.getResources().getResourceConfigDocument().getResourceConfig().getResources().sizeOfResourceArray()
    );

    long lastsave = file.lastModified();
    Thread.sleep(2000);
    resources = new ResourcesWrapper();
    agent.setResources( resources );
    assertTrue( "resource.xml modified", file.lastModified() > lastsave );
    file.delete();
    logger.info( "----------------------" );

    logger.info( "Encryption begins" );
    // test encryption - first create an encrypted resource config
    stopAgent();
    agent = startAgent( true, false );
    agent.setResources( new ResourcesWrapper() );
    assertEquals(
      "no resource yet",
      0,
      agent.getResources().getResourceConfigDocument().getResourceConfig().getResources().sizeOfResourceArray()
    );
    resources = new ResourcesWrapper();
    Resource resource =
      resources.getResourceConfigDocument().getResourceConfig().getResources().addNewResource();
    resource.setName( "resourceA" );
    logger.info( "----------------------" );

    agent.setResources( resources );
    assertTrue( "encrypted resource config written", file.exists() );
    // now try to re-read it
    stopAgent( );
    agent = startAgent( true, false );
    assertEquals(
      "resource read in",
      1,
      agent.getResources().getResourceConfigDocument().getResourceConfig().getResources().sizeOfResourceArray()
    );
    file.delete();
  }

  /**
   * Create an Agent object and save a suite.  Start a new agent object and
   * verify the suite is there.  Save a delete series change to the suite
   * and verify the timestamp and number of series.
   *
   * @throws Exception if problem running test
   */
  public void testSuitePersistence() throws Exception {
    Repositories repositories = RepositoriesTest.createSampleRepository(null);

    String filename = "var" + File.separator +
                      "suites";
    File file = new File( filename );
    StringMethods.deleteDirectory( file );
    ResourcesWrapper resources = ResourcesWrapperTest.createSampleResources();
    logger.info( "\n>> Starting with initialized agent" );
    startDepot( false );
    Agent agent = startAgent( false, false );
    agent.updateCachedPackages();
    agent.setResources( resources );
    RepositoryCache cache = RepositoryCacheTest.createSampleRepositoryCache(
      repositories
    );
    assertEquals(
      "no suites yet",
      0,
      agent.getSuites().getNames().length
    );
    logger.info( "\n>> Reading in TestSuiteLocal" );
    SuiteWrapper suite = new SuiteWrapper("test/samplesuitelocal.xml" );
    cache.resolveReporters( suite );
    logger.info( "\n>> Putting TestSuiteLocal in Agent" );
    SuiteStagesWrapper currentSuiteStagesWrapper = agent.getSuites().getSuite("TestSuiteLocal", resources);
    currentSuiteStagesWrapper.modify( suite );
    agent.getSuites().putSuite(currentSuiteStagesWrapper);
    logger.info( "\n>> Agent should now have TestSuiteLocal" );
    logger.info( "\n>> Starting fresh agent - should read in TestSuiteLocal " );

    stopAgent( );
    agent = startAgent( false, false );
    assertEquals(
      "found 1 suite",
      1,
      agent.getSuites().getNames().length
    );
    logger.info( "\n>> Reading in delete suite request" );
    SuiteWrapper suiteDeletes = new SuiteWrapper( "test/delete_samplesuitelocal.xml" );
    cache.resolveReporters( suiteDeletes );
    File suiteFile = new File(
      file.getAbsolutePath() + File.separator + "TestSuiteLocal.xml"
    );
    long lastsave = suiteFile.lastModified();
    Thread.sleep(2000);
    logger.info( "\n>> Putting in delete suite request" );
    currentSuiteStagesWrapper = agent.getSuites().getSuite("TestSuiteLocal");
    currentSuiteStagesWrapper.modify( suiteDeletes );
    agent.getSuites().putSuite(currentSuiteStagesWrapper);
    logger.info( "\n>> delete suite finished" );
    assertTrue( "TestSuiteLocal.xml modified",
                suiteFile.lastModified() > lastsave );

    assertEquals( "Have 2 configs in expanded", 2,
                  new SuiteWrapper(
                    agent.getSuites().getSuite( "TestSuiteLocal").getPerResourceSuiteDocument()).
                  getSeriesConfigCount() );

    StringMethods.deleteDirectory( file );
  }

  public static void main(String[] args)
  {
	  try {
		  AgentTest tester = new AgentTest();

		  tester.setUp();

		  try {
			  tester.testDeleteSuites();
			  tester.testDistributeSuites();
			  tester.testDistributeSuitesAuth();
			  tester.testRegister();
			  tester.testRegisterAuth();
			  tester.testRepositoriesPersistence();
			  tester.testResourceConfigPersistence();
			  tester.testSuitePersistence();
		  }
		  finally {
			  tester.tearDown();
		  }
	  }
	  catch (Exception err) {
		  err.printStackTrace(System.err);

		  System.exit(-1);
	  }
  }

  /**
   *
   * @param data
   * @return
   * @throws IOException
   */
  private static Object readPEMData(String data) throws IOException
  {
    PEMParser parser = new PEMParser(new StringReader(data));

    try {
      return parser.readObject();
    }
    finally {
      parser.close();
    }
  }

  /**
   *
   * @param data
   * @return
   * @throws IOException
   * @throws CertificateException
   */
  private static X509Certificate readCertData(String data) throws IOException, CertificateException
  {
    X509CertificateHolder certHolder = (X509CertificateHolder)readPEMData(data);
    JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();

    certConverter.setProvider("BC");

    return certConverter.getCertificate(certHolder);
  }

  /**
   *
   * @param data
   * @param password
   * @return
   * @throws IOException
   * @throws KeyStoreException
   */
  private static KeyPair readKeyData(String data, final String password) throws IOException, KeyStoreException
  {
    Object pemObject = readPEMData(data);
    PEMKeyPair pemKey;

    if (pemObject instanceof PEMKeyPair)
      pemKey = (PEMKeyPair)pemObject;
    else if (pemObject instanceof PEMEncryptedKeyPair) {
      PEMEncryptedKeyPair encryptedKey = (PEMEncryptedKeyPair)pemObject;
      JcePEMDecryptorProviderBuilder decryptBuilder = new JcePEMDecryptorProviderBuilder();

      decryptBuilder.setProvider("BC");

      PEMDecryptorProvider decryptor = decryptBuilder.build(password.toCharArray());

      pemKey = encryptedKey.decryptKeyPair(decryptor);
    }
    else
      throw new KeyStoreException(data + " does not contain a key pair");

    JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();

    keyConverter.setProvider("BC");

    return keyConverter.getKeyPair(pemKey);
  }
}
