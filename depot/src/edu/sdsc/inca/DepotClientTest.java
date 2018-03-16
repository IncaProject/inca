package edu.sdsc.inca;

import junit.framework.TestCase;
import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.depot.persistent.DatabaseTools;
import edu.sdsc.inca.depot.persistent.InstanceInfo;
import edu.sdsc.inca.depot.persistent.Suite;
import edu.sdsc.inca.depot.persistent.Series;
import edu.sdsc.inca.dataModel.report.ReportDocument;
import edu.sdsc.inca.dataModel.reportDetails.ReportDetailsDocument;
import edu.sdsc.inca.dataModel.suite.SuiteDocument;
import edu.sdsc.inca.dataModel.util.Report;
import edu.sdsc.inca.queryResult.ReportSummaryDocument;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.ConfigProperties;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.KeyPair;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class DepotClientTest extends TestCase {

  private Logger logger = Logger.getLogger( this.getClass().getName() );
  private static final int CONFIG_COUNT = 4;
  private static final String SUITE_GUID = "RA:GUID";

  public void testQueryInstance() throws Exception {

    Depot d = null;
    try {
      d = startDepot();
    } catch ( Exception e ) {
      logger.error( "Depot couldn't start ", e );
      fail(e.toString());
    }

    Suite suite = Suite.generate(SUITE_GUID, CONFIG_COUNT);
    String suiteXml = suite.toXml();
    DepotClient dc = null;
    String stderr = "ls: blech: No such file or directory";
    try {
      dc = connectDepotClient("localhost", d.getPort());

      logger.debug( "SENDING SUITE" );
      dc.updateSuite( suiteXml );

      logger.debug( "SENDING FIRST REPORT" );
      Series series = suite.getSeriesConfig(0).getSeries();
      String report = series.generateReport();

      dc.insertReport(
        series.getResource(),
        report,
        "cpu_secs=12\nwall_secs=13\nmemory_mb=14\n",
        stderr,
        series.getContext(),
        series.getTargetHostname());

      logger.info( "SLEEPING 3 SECONDS TO ALLOW INSERT TO COMPLETE");
      Thread.sleep(3000);
      String[] basicResult = dc.queryLatest("suite.guid='" + SUITE_GUID + "'");
      logger.debug( "CHECKING BASICRESULTS" );
      assertEquals
        ("num of basic results correct", CONFIG_COUNT, basicResult.length);
      for(int i = 0; i < basicResult.length; i++) {
        ReportSummaryDocument sum = ReportSummaryDocument.Factory.parse(
          basicResult[i]
        );
        if(sum.getReportSummary().getHostname().equals(series.getResource())) {
          assertTrue( series.getContext() + " has body",
            sum.getReportSummary().isSetBody() );
          String result = dc.queryInstanceById(
            sum.getReportSummary().getInstanceId(),
            sum.getReportSummary().getSeriesConfigId()
          );
          ReportDetailsDocument r = ReportDetailsDocument.Factory.parse(result);
          assertNotNull( series.getContext() + " instance has report",
            r.getReportDetails().getReport() );
          assertEquals( series.getContext() + " has stderr",
                        stderr,
                        r.getReportDetails().getStderr() );

          InstanceInfo ii = new InstanceInfo(series, sum.getReportSummary().getInstanceId());

          result = dc.queryInstance(sum.getReportSummary().getNickname(), series.getResource(), series.getTargetHostname(), ii.getCollected());
          r = ReportDetailsDocument.Factory.parse(result);

          assertNotNull( series.getContext() + " instance has report",
            r.getReportDetails().getReport() );
          assertEquals( series.getContext() + " has stderr",
                        stderr,
                        r.getReportDetails().getStderr() );
        }
      }
      dc.close();
    } finally {
      stopDepot(d);
    }

  }

  public void testQuerySuite() throws XmlException {

    Depot d = null;
    try {
      d = startDepot();
    } catch ( Exception e ) {
      logger.error( "Depot couldn't start ", e );
    }

    Suite suite = Suite.generate(SUITE_GUID, CONFIG_COUNT);
    String suiteXml = suite.toXml();
    SuiteDocument suiteDoc = SuiteDocument.Factory.parse(suiteXml);
    String[] REPORTS = new String[CONFIG_COUNT];
    for(int i = 0; i < CONFIG_COUNT; i++) {
      REPORTS[i] = suite.getSeriesConfig(i).getSeries().generateReport();
    }
    ReportDocument[] reports = new ReportDocument[REPORTS.length];
    for ( int i = 0; i < reports.length; i++ ) {
      reports[i] = ReportDocument.Factory.parse(REPORTS[i]);
    }

    DepotClient dc;
    String failMsg = null;
    Calendar firstReportTime0 = null;
    Calendar firstReportTime1 = null;
    try {
      dc = connectDepotClient("localhost", d.getPort());

      logger.debug( "SENDING SUITE" );
      dc.updateSuite( suiteXml );

      logger.debug( "SENDING REPORT 0" );
      Series series = suite.getSeriesConfig(0).getSeries();
      dc.insertReport(
        series.getResource(),
        REPORTS[0],
        "cpu_secs=12\nwall_secs=13\nmemory_mb=14\n",
        null,
        series.getContext(),
        series.getTargetHostname());

      logger.info( "SLEEPING 3 SECONDS TO ALLOW INSERT TO COMPLETE");
      Thread.sleep(3000);
      String[] basicResult =
        dc.queryLatest("suite.guid = '" + SUITE_GUID + "'");
      logger.debug( "CHECKING BASICRESULTS" );
      assertEquals
        ("num of basic results correct", CONFIG_COUNT, basicResult.length );
      for(int i = 0; i < basicResult.length; i++) {
        ReportSummaryDocument sum = ReportSummaryDocument.Factory.parse(
          basicResult[i]
        );
        if(sum.getReportSummary().getHostname().equals(series.getResource())) {
          assertTrue( series.getContext() + " has body",
            sum.getReportSummary().isSetBody() );
          String result = dc.queryInstanceById(
            sum.getReportSummary().getInstanceId(),
            sum.getReportSummary().getSeriesConfigId()
          );
          ReportDetailsDocument r = ReportDetailsDocument.Factory.parse(result);
          firstReportTime0 = r.getReportDetails().getReport().getGmt();
        }
      }

      logger.debug( "SENDING REPORT 1" );
      series = suite.getSeriesConfig(1).getSeries();
      dc.insertReport(
        series.getResource(),
        REPORTS[1],
        "cpu_secs=12\nwall_secs=13\nmemory_mb=14\n",
        null,
        series.getContext(),
        series.getTargetHostname());

      logger.info( "SLEEPING 3 SECONDS TO ALLOW INSERT TO COMPLETE");
      Thread.sleep(3000);
      logger.debug( "CHECKING BASICRESULTS" );
      basicResult =
        dc.queryLatest("suite.guid='" +  suiteDoc.getSuite().getGuid() + "'");
      assertEquals
        ("num of basic results correct", CONFIG_COUNT, basicResult.length);
      for(int i = 0; i < basicResult.length; i++) {
        ReportSummaryDocument sum = ReportSummaryDocument.Factory.parse(
          basicResult[i]
        );
        if(sum.getReportSummary().getHostname().equals(series.getResource())) {
          assertTrue( series.getContext() + " has report",
            sum.getReportSummary().isSetBody() );
          String result = dc.queryInstanceById(
            sum.getReportSummary().getInstanceId(),
            sum.getReportSummary().getSeriesConfigId()
          );
          ReportDetailsDocument r = ReportDetailsDocument.Factory.parse(result);
          assertNotNull("Instance not found", result);
          firstReportTime1 = r.getReportDetails().getReport().getGmt();
        }
      }

      logger.debug( "INSERTING " + REPORTS.length + " NEW REPORTS IN DB");
      for(int i = 0; i < REPORTS.length; i++) {
        series = suite.getSeriesConfig(i).getSeries();
        logger.debug( "INSERTING REPORT " + i );
        dc.insertReport(
          series.getResource(),
          REPORTS[i],
          "cpu_secs=12\nwall_secs=13\nmemory_mb=14\n",
          null,
          series.getContext(),
          series.getTargetHostname());
        logger.info( "SLEEPING 3 SECONDS TO ALLOW INSERT TO COMPLETE");
        Thread.sleep(3000);
      }

      logger.debug( "CHECKING BASICRESULTS" );

      basicResult =
        dc.queryLatest("suite.guid='" + suiteDoc.getSuite().getGuid() + "'");
      assertEquals
        ("number of basic results correct", CONFIG_COUNT, basicResult.length );
      for(int i = 0; i < basicResult.length; i++) {
        ReportSummaryDocument sum = ReportSummaryDocument.Factory.parse(
          basicResult[i]
        );
        logger.debug( basicResult[i] );
        assertTrue( sum.getReportSummary().getHostname() + " has report",
                    sum.getReportSummary().isSetBody() );
        String result = dc.queryInstanceById(
          sum.getReportSummary().getInstanceId(),
          sum.getReportSummary().getSeriesConfigId()
        );
        ReportDetailsDocument r = ReportDetailsDocument.Factory.parse(result);
        Report report = r.getReportDetails().getReport();
        if(report.getName().equals(suite.getSeriesConfig(0).getSeries().getResource())) {
          Calendar reportTime = report.getGmt();
          assertTrue( "Received last report for 0",
            reportTime.after(firstReportTime0) );
        }
        if ( report.getName().equals(suite.getSeriesConfig(1).getSeries().getResource())) {
          Calendar reportTime = report.getGmt();
          assertTrue( "Received last report for 1",
            reportTime.after(firstReportTime1) );
        }
      }
      dc.close();
    } catch(Exception e) {
      failMsg = "unexpected exception " + e;
      logger.error( failMsg, e );
    } finally {
      stopDepot(d);
    }
    if(failMsg != null) {
      fail(failMsg);
    }
  }

  /**
   * Test whether the Depot appropriately restricts certain actions to clients
   * that have been given permission to make them.
   *
   * @throws XmlException
   */
  public void testPermit() throws XmlException {

    Depot d = null;
    try {
      d = startDepot();
    } catch ( Exception e ) {
      logger.error( "Depot couldn't start ", e );
    }

    MessageHandler.resetPermissions();
    Suite suite = Suite.generate(SUITE_GUID, CONFIG_COUNT);
    String suiteXml = suite.toXml();
    SuiteDocument suiteDoc = SuiteDocument.Factory.parse(suiteXml);
    String[] REPORTS = new String[CONFIG_COUNT];
    for(int i = 0; i < CONFIG_COUNT; i++) {
      REPORTS[i] = suite.getSeriesConfig(i).getSeries().generateReport();
    }
    ReportDocument[] reports = new ReportDocument[REPORTS.length];
    for ( int i = 0; i < reports.length; i++ ) {
      reports[i] = ReportDocument.Factory.parse(REPORTS[i]);
    }

    DepotClient dc = null;
    String failMsg = null;

    try {

      dc = connectDepotClient("localhost", d.getPort());
      String dn = dc.getDn(false);

      // Try updating suite w/no permissions ...
      logger.debug( "SENDING SUITE" );
      dc.updateSuite( suiteXml );
      // ... and w/out permission.
      dc.commandPermit(dn + "OTHER", Protocol.SUITE_ACTION);
      try {
        logger.debug( "SENDING FORBIDDEN SUITE" );
        dc.updateSuite( suiteXml );
        fail("Forbidden suite allowed");
      } catch(Exception e) {
        dc = connectDepotClient("localhost", d.getPort());
      }

      // Try reporting w/no permissions ...
      logger.debug( "SENDING REPORT 0" );
      Series series = suite.getSeriesConfig(0).getSeries();
      dc.insertReport(
        series.getResource(),
        REPORTS[0],
        "cpu_secs=12\nwall_secs=13\nmemory_mb=14\n",
        null,
        series.getContext(),
        series.getTargetHostname());
      // ... and w/out permission.
      dc.commandPermit
        (dn + "OTHER", Protocol.INSERT_ACTION + " " + series.getResource());
      try {
        logger.debug( "SENDING FORBIDDEN REPORT" );
        dc.insertReport(
          series.getResource(),
          REPORTS[0],
          "cpu_secs=12\nwall_secs=13\nmemory_mb=14\n",
          null,
          series.getContext(),
          series.getTargetHostname());
        fail("Forbidden report allowed");
      } catch(Exception e) {
        dc = connectDepotClient("localhost", d.getPort());
      }
      dc.close();

    } catch(Exception e) {
      failMsg = "unexpected exception " + e;
      logger.error( failMsg, e );
    } finally {
      stopDepot(d);
    }

    if(failMsg != null) {
      fail(failMsg);
    }

  }

  private static final String AGENT_CERT =
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
  private static final String AGENT_KEY =
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
  private static final String CA_CERT =
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
  private static final String DEPOT_CERT =
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
  private static final String DEPOT_KEY =
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
  private static final String PASSWORD = "abcdefg";

  private Depot startDepot() throws Exception {
    Depot result = new Depot() {
      public void readCredentials() throws ConfigurationException, IOException {
        this.cert = readCert(DEPOT_CERT);
        this.key = readKey(DEPOT_KEY, PASSWORD);
        this.trusted.add(readCert(AGENT_CERT));
        this.trusted.add(readCert(CA_CERT));
      }
    };
    ConfigProperties config = new ConfigProperties();
    config.putAllTrimmed(System.getProperties(), "inca.depot.");
    config.loadFromResource("inca.properties", "inca.depot.");
    config.setProperty("auth", "yes");
    config.setProperty("password", PASSWORD);
    result.setConfiguration(config);

    DatabaseTools.removeDatabase();
    DatabaseTools.initializeDatabase();

    result.runServer();

    Depot.setRunningDepot(result);
    MessageHandler.resetPermissions();

    return result;
  }

  private DepotClient connectDepotClient(String server, int port)
    throws ConfigurationException, IOException {
    DepotClient result = new DepotClient() {
      public void readCredentials() throws ConfigurationException, IOException {
        this.cert = readCert(AGENT_CERT);
        this.key = readKey(AGENT_KEY, PASSWORD);
        this.trusted.add(readCert(DEPOT_CERT));
        this.trusted.add(readCert(CA_CERT));
      }
    };
    ConfigProperties config = new ConfigProperties();
    result.setServer(server, port);
    config.putAllTrimmed(System.getProperties(), "inca.depot.");
    config.loadFromResource("inca.properties", "inca.depot.");
    config.setProperty("auth", "yes");
    config.setProperty("password", PASSWORD);
    result.setConfiguration(config);
    result.connect();
    return result;
  }

  private void stopDepot(Depot d) {
    if(d == null) {
      return;
    }
    try {
      logger.debug( "Trying to shutdown depot" );
      d.shutdown();
      logger.debug( "Shutdown complete" );
    } catch(InterruptedException e) {
      // empty
    }
  }

  private Certificate readCert(String cert) throws IOException, ConfigurationException
  {
    PEMParser parser = new PEMParser(new StringReader(cert));
    X509CertificateHolder certHolder = (X509CertificateHolder)parser.readObject();

    parser.close();

    JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();

    certConverter.setProvider("BC");

    try {
      return certConverter.getCertificate(certHolder);
    }
    catch (CertificateException certErr) {
      throw new ConfigurationException(certErr);
    }
  }

  private KeyPair readKey(String key, final String password) throws IOException
  {
    PEMParser parser = new PEMParser(new StringReader(key));
    Object pemObject = parser.readObject();

    parser.close();

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
      return null;

    JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();

    keyConverter.setProvider("BC");

    return keyConverter.getKeyPair(pemKey);
  }
}
