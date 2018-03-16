package edu.sdsc.inca.agent;

import org.apache.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.util.Vector;
import java.util.Calendar;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.io.*;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.math.BigInteger;

import edu.sdsc.inca.agent.access.Manual;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.StringMethods;
import edu.sdsc.inca.util.Constants;
import edu.sdsc.inca.util.ResourcesWrapper;
import edu.sdsc.inca.ConfigurationException;
import edu.sdsc.inca.Component;
import edu.sdsc.inca.Agent;

/**
 * Handles the creation of a remote reporter manager process on a specified
 * resource.
 *
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class ReporterManagerStarter extends Thread {

  // Constants -- directories and temporary files
  final static public String RMBUILDSCRIPT = "buildRM.sh";
  final static public String RMCERT = "rmcert.pem";
  final static public String RMDIST = "Inca-ReporterManager.tar.gz";
  final static public String RMKEY = "rmkey.pem";
  final static public String RMREQ = "rmreq.pem";
  final static public String RMTRUSTED = "trusted";
  final static public String LAST_CHANGE = "suiteChange.xml";
  final static public String CURRENT_SUITE = "updatedSuite.xml";

  // Constants -- cert/key creation
  final static private String SIGNATURE_ALGORITHM = "SHA512withRSA";
  final static private String PROVIDER = "BC"; // BouncyCastle

  final static protected int DEFAULT_AGE = 5; // years
  private static Logger logger = Logger.getLogger(ReporterManagerStarter.class);

  // Member variables -- to start remote reporter manager
  private Agent             agent = null;
  private String            bashLoginOption = null;
  private String[]          equivHosts = new String[0];
  private String            host = null;
  private int               hostId = 0;
  private boolean           isRunning = false;
  private AccessMethod      processHandle = null;
  private String            resource = null;
  private String            rmRootPath = null;
  private long              startAttemptTime = 0;
  private String            suspend = null;
  private String            tempDir = null;
  private int               waitCheckPeriod =
    Integer.parseInt(Protocol.CHECK_PERIOD_MACRO_DEFAULT);

  // Member variables -- cert/key creation
  private BigInteger      caSerialNumber = BigInteger.ONE; //uniq certificate #

  private Certificate     rmCert = null; // manager
  private PrivateKey      rmKey = null;   // manager

  /**
   * Create a new reporter manager starter object for a resource.
   *
   * @param resource   Name of the resource (from the resource configuration
   *                   file) to start the reporter manager on.
   * @param agent    Path to a location where temporary/state files can be
   */
  public ReporterManagerStarter( String resource, Agent agent ) {
    // name the thread
    super( resource );

    this.resource = resource;
    this.agent = agent;
    this.setTempDir( agent.getTempPath() );
    this.refreshHosts();
  }

  /**
   * Create a new reporter manager starter object, whose configuration is
   * identical to rmStarter
   *
   * @param rmStarter  A reporter manager starter object that will be used to
   * configure this reporter manager starter object.
   */
  public ReporterManagerStarter( ReporterManagerStarter rmStarter ) {
    // name the thread
    super( rmStarter.resource );

    this.agent = rmStarter.agent;
    this.bashLoginOption = rmStarter.bashLoginOption;
    this.caSerialNumber = rmStarter.caSerialNumber;
    this.equivHosts = rmStarter.equivHosts;
    this.host = rmStarter.host;
    this.hostId = rmStarter.hostId;
    isRunning = rmStarter.isRunning;
    this.processHandle = rmStarter.processHandle;
    this.resource = rmStarter.resource;
    this.rmCert = rmStarter.rmCert;
    this.rmKey = rmStarter.rmKey;
    this.rmRootPath = rmStarter.rmRootPath;
    this.tempDir = rmStarter.tempDir;
    this.waitCheckPeriod = rmStarter.waitCheckPeriod;
    this.refreshHosts();
  }

  /**
   * Start the reporter manager process on the remote resource using the
   * method specified in the resource configuration file.
   *
   * @throws AccessMethodException  if problem creating remote reporter manager
   *                                process
   */
  public void create( ) throws AccessMethodException {

    Vector<String> arguments = new Vector<String>();
    arguments.add( this.bashLoginOption );
    arguments.add( "-c" );
    String rmCmd = getRmCmd();

    String passphraseToUse = null;
    if ( this.agent.getPassword() != null &&
         ! this.agent.getPassword().equals("")){
      rmCmd += " -P true";
      passphraseToUse = this.agent.getPassword() + "\n"; // to terminate password in stdin for sbin/inca
    } else {
      rmCmd += " -P false";
    }
    arguments.add( rmCmd );

    String[] argArrayType = new String[arguments.size()];
    String[] argStringArray = arguments.toArray(argArrayType);
    logger.info( "Creating reporter manager process on '" + resource + "'" );
    processHandle.start("/bin/bash", argStringArray, passphraseToUse, rmRootPath);
    if ( processHandle.isActive() ) {
      return;
    }
    throw new AccessMethodException( "Reporter manager is not active");
  }

  /**
   * Test for the bash login shell option on the reporter manager resource.
   * Since we're not guaranteed that all access methods will source the users's
   * initialization file and we invoke processes that rely on the user's
   * environment (e.g., to find the openssl the user has specified in their
   * initialization file), we invoke these processes with bash using the login
   * shell option.  Since the bash login option is not portable (i.e., -l or
   * --login), we have to test each option by running a small echo command and
   * checking the result.
   *
   * @throws InterruptedException      if interrupted while determining remote
   *                                   bash login option
   * @throws ReporterManagerException  if unable to determine remote bash login
   *                                   option.
   */
  public void findBashLoginShellOption()
    throws InterruptedException, ReporterManagerException {

    if ( this.isManual() ) {
      return;  // in manual, the user will be using a login shell
    }
    logger.info( "Testing for -l or --login" );
    for ( String loginOpt : new String[]{"-l", "--login" } ) {
      AccessMethodOutput result = null;
      try {
        result = processHandle.run(
          "/bin/bash", new String[] { loginOpt, "-c", "echo inca" }
        );
        logger.info( "Stderr='" + result.getStderr() + "'");
        logger.info( "Stdout='" + result.getStdout() + "'");
        if ( Pattern.matches("(?m)(?s).*inca.*", result.getStdout()) ) {
          logger.info( "Bash option " + loginOpt + " succeeded");
          this.bashLoginOption = loginOpt;
          return;
        } else {
          logger.info( loginOpt + " failed");
        }
      } catch ( AccessMethodException e ) {
        logger.info( "Bash login option " + loginOpt + " failed" );
        logger.debug( "Bash login option " + loginOpt + " failed", e );
      }

    }
    throw new ReporterManagerException( "All bash login options failed" );
  }

  /**
   * Returns the currently selected host (chosen by the run method when the
   * reporter manager is started).
   *
   * @return  The host that the reporter manager is currently executing on.
   */
  public String getCurrentHost() {
    return host;
  }

  /**
   * Returns the equivalent hosts for this resource.
   *
   * @return  The list of equivalent hosts for this resource
   */
  public String[] getEquivalentHosts() {
    return this.equivHosts;
  }

  /**
   * Return the path to the remote reporter manager installation directory.
   *
   * @return  The path to the root reporter manager directory
   */
  public String getRmRootPath() {
    return rmRootPath;
  }

  /**
   * Return the process handle being used by this reporter manager starter.
   * For testing purposed mostly.
   *
   * @return the process handle being used by this reporter manager starter
   */
  public AccessMethod getProcessHandle() {
    return processHandle;
  }

  /**
   * Returns the amount of elapsed time since attempting a start of a
   * remote reporter manager.
   *
   * @return  Elapsed time in milliseconds;
   */
  public synchronized long getStartAttemptTime() {
    return startAttemptTime;
  }

  /**
   * Returns the suspend condition for the reporter manager
   *
   * @return The suspend condition if set and null if not set
   */
  public String getSuspend() {
    return suspend;
  }

  /**
   * Gets the directory path where the temporary files can be stored.
   *
   * @return  A path to a local directory.
   */
  public String getTemporaryDirectory() {
    return tempDir;
  }

  /**
   * Return the current check period for reporters
   *
   * @return  The current check period for reporters in seconds
   */
  public int getWaitCheckPeriod() {
    return waitCheckPeriod;
  }

  /**
   * Return whether  the reporter manager is being controlled manually (and not
   * by the agent)
   *
   * @return  True if the reporter manager is being controlled manually and
   * false if it is not.
   */
  public boolean isManual() {
    return processHandle.getClass().equals(Manual.class);
  }

  /**
   * Return true if the reporter manager is currently registered and running.
   *
   * @return  True if the reporter manager is currently registered and running
   * and false otherwise.
   */
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * Checks to see if the reporter manager is available and installed on the
   * machine.
   *
   * @return  True if the reporter manager distribution is found on the machine.
   *
   * @throws InterruptedException if interrupted while determining if remote
   *                              reporter manager is staged.
   */
  public boolean isStaged() throws InterruptedException {
    boolean staged = true;

    String[] argArray = new String[]{ "reporter-manager", "-T", "-P", "false" };
    logger.info( "Checking reporter manager install on '" + resource + "'" );
    AccessMethodOutput testOutput = new AccessMethodOutput();
    AccessMethodOutput tailOut = null;
    try {
      testOutput = processHandle.run
        ( rmRootPath + "/sbin/inca", argArray, "", rmRootPath );
      argArray = new String[]{
        "-c",
        "tail -4 var/reporter-manager.out"
      };
      tailOut = processHandle.run( "/bin/sh", argArray, "", rmRootPath );
    } catch ( AccessMethodException e ) {
      logger.error( "Test of reporter manager failed on " + resource, e );
      staged = false;
    }
    if ( staged && ! Pattern.matches( "(?m)(?s).*Testing XML parsing.*",
                                      tailOut.getStdout().trim() ) ) {
      logger.error( "Test of reporter-manager failed on " + resource );
      logger.error( "Stdout = '" + testOutput.getStdout() );
      logger.error( "Stderr = '" + testOutput.getStderr() );
      staged = false;
    }
    if ( staged && ! Pattern.matches( "(?m)(?s).*XML::Simple working.*",
                                      tailOut.getStdout().trim() ) ) {
      logger.error( "Perl XML libraries not installed");
      staged = false;
    }
    if ( staged && this.agent.hasCredentials() )  {
      if ( ! Pattern.matches("(?m)(?s)^.*IO::Socket::SSL working.*$",
                             tailOut.getStdout().trim() ) ) {
        logger.error( "Perl SSL libraries not installed");
        staged = false;
      }
    }
    return staged;
  }

  /**
   * Assume the current host is not reachable and try the next host in the
   * list (if there is one)
   *
   * @throws ConfigurationException  if configuration related problem with the
   *                                 next host selected
   */
  public void nextHost() throws ConfigurationException {
    hostId++;
    if ( hostId >= this.equivHosts.length ) {
      hostId = 0;
    }
    this.setHost( hostId );
  }

  /**
   * Starts a thread to create the reporter manager on the remote resource.  If
   * there is more than one host, it will choose the first one that it can
   * successfully create a reporter manager on.  If unable to create a
   * reporter manager on any resource, will wait 10 minutes and then try again.
   */
  public void run() {

    logger.debug( "Start remote reporter manager thread for " + resource );
    try {
      while( true ) {
        boolean atLeastOneCreateAttempt = false;
        logger.debug( "Looking for host to start " + resource + " reporter manager from " + StringMethods.join( " | ", this.equivHosts) );
        for ( int i = 0; i < this.equivHosts.length; i++ ) {
          this.setStartAttemptTimeNow();
          try {
            if ( i != 0 ) {
              this.nextHost();
            } else {
              this.setHost( 0 );
            }
          } catch ( ConfigurationException e ) {
            logger.error("Host configuration for " + this.host + " failed",e);
            continue;
          }
          String desc = "start reporter manager on " + this.resource +
                        " using host " + this.host;
          logger.info("Attempting to " + desc );
          try {
            if ( ! this.isManual() ) {
              if ( this.bashLoginOption == null ) findBashLoginShellOption();
              if ( ! this.isStaged() ) this.stage( null );
              this.create();
            }
            this.setStartAttemptComplete(); // before wait if no exceptions
            atLeastOneCreateAttempt = true;
            if ( this.waitForReporterManager() ) return;
          } catch ( InterruptedException e ) {
            throw e;
          } catch ( ReporterManagerException e ) {
            logger.error( "Unable to " + desc + ": " + e, e );
          } catch ( AccessMethodException e ) {
            logger.error( "Unable to " + desc + ": " + e, e );
          } catch( Throwable t ) {
            logger.error
              ( "Catching unknown exception; unable to " + desc + ":" + t, t );
          } finally {
            this.setStartAttemptComplete();
          }
        }
        if ( ! atLeastOneCreateAttempt ) {
          synchronized( this ) {
            logger.info(
              "Tried all equivalent hosts for resource " + resource +
              "...waiting " + this.agent.getStartAttemptWaitPeriod() +
              " milliseconds before next start attempt"
            );
            this.wait( this.agent.getStartAttemptWaitPeriod() );
          }
        }
      }
    } catch ( InterruptedException e ) {
      logger.error(
        "Caught interrupt while attempting to start reporter manager...exitting",
        e
      );
    }
  }

  /**
   * Set the resources document referenced by the reporter manager.
   */
  public void refreshHosts( ) {

    this.hostId = 0;
    try {
      String[] hosts = this.agent.getResources().getResources(resource, false);
      if ( hosts.length == 0 ) {
        hosts = new String[] {resource};
      }
      this.equivHosts = hosts;
      this.setHost( this.hostId );
    } catch ( ConfigurationException e ) {
      logger.error(
        "Problem reading resource configuration for " + this.resource, e
      );
    }
  }

  /**
   * For testing purposes only.  Set correct flag for remote bash login option.
   *
   * @param bashLoginOption  flag for remote bash login option.
   */
  public void setBashLoginOption(String bashLoginOption) {
    this.bashLoginOption = bashLoginOption;
  }

  /**
   * Set the clock on when a start attempt has been made so we can time it
   * out if it gets stuck.
   */
  public synchronized void setStartAttemptTimeNow() {
    startAttemptTime = Calendar.getInstance().getTimeInMillis();
  }

  /**
   * Set that the start attempt has been completed.
   */
  public synchronized void setStartAttemptComplete() {
    startAttemptTime = 0;
  }


  /**
   * Set the status of the reporter manager to running and notify any
   * waiting threads.
   *
   * @param isRunning  True indicates the reporter manager is running on the
   * remote machine and available; false otherwise.
   */
  public void setRunning( boolean isRunning ) {
    synchronized(this) {
      logger.debug
        ("Set reporter manager " + resource + " status to running " +isRunning);
      this.isRunning = isRunning;
      notifyAll(); // notify anybody who may be waiting on this
    }
  }

  /**
   * Set the location of the temporary directory.
   *
   * @param tempDir  A path to a directory where temporary files can be stored.
   */
  public void setTempDir( String tempDir ) {
    this.tempDir = tempDir + File.separator + Agent.RMDIR + File.separator +
                   resource;
    File dir = new File( this.tempDir );
    if ( ! dir.exists() ) {
      logger.info( "Creating temporary directory '" + this.tempDir + "'" );
      logger.info( "Directories created: " + dir.mkdirs() );
    }
  }

  /**
   * Stage the Reporter Manager distribution over to the remote resource.
   * Will first put over the reporter manager tarball and build script,
   * invoke the build script, and wait for completion.
   *
   * @param upgrade  If not null, runs the buildRM.sh script with the upgrade
   *                 option turned on. Otherwise a regular full install is done.
   *
   * @throws InterruptedException if interrupted while executing build script
   * @throws ReporterManagerException if build fails
   */
  public void stage( String upgrade )
    throws InterruptedException, ReporterManagerException {
    if ( upgrade != null && ! isStaged() ) {
      throw new ReporterManagerException(
        "Cannot upgrade reporter manager on resource '" + resource + "': " +
        "no existing installation found"
      );
    }

    // The reporter manager distribution and build script are stored in the
    // classpath (more specifically in inca-agent.jar).  So, we need to
    // extract them in order to have a valid path to give to the access method
    // copy functions.  We also need to grab the trusted certificates.
    String tmpRmDistPath = null;
    String tmpRmBuildScriptPath = null;
    String[] trustedFiles = new String[0];
    try {
      tmpRmDistPath = writeClasspathResourceToFile( RMDIST, this.tempDir );
      tmpRmBuildScriptPath = writeClasspathResourceToFile(
        RMBUILDSCRIPT, this.tempDir
      );
      if ( this.agent.hasCredentials() ) {
        trustedFiles = writeTrustedCertsToTempDir();
      }
    } catch ( IOException e ) {
      throw new ReporterManagerException(
        "Problem writing reporter manager dist files to temporary directory", e
      );
    }
    try {
      Vector<String> files = new Vector<String>();
      files.addAll( Arrays.asList(trustedFiles) );
      files.add( tmpRmDistPath ); files.add( tmpRmBuildScriptPath );
      processHandle.put( files.toArray(new String[files.size()]), rmRootPath );
      build( upgrade );
      if ( this.agent.hasCredentials() ) { generateRmCredentials(); }
    } catch ( AccessMethodException e ) {
      String errorMsg = "Unable to stage reporter manager to " + host;
      logger.error( errorMsg, e );
      throw new ReporterManagerException( errorMsg, e );
    }
  }

  /**
   * To be used after the function create in order to wait for the remote
   * reporter manager process to start up and connect to the agent.  Will
   * wait up to startAttemptWaitPeriod milliseconds before returning false.
   *
   * @return True if the remote reporter manager has registered and false
   * if startAttemptWaitPeriod milliseconds have passed and the remote
   * reporter manager has still not registered.
   *
   * @throws InterruptedException  if interrupted while waiting for remote
   *                               reporter manager to check in.
   */
  public boolean waitForReporterManager() throws InterruptedException {
    long startTime = Calendar.getInstance().getTimeInMillis();
    long elapsedTime = 0;
    while( true ) {
      if ( this.isRunning() ) {
        logger.info(
          "Remote reporter manager registered for " + resource +
          "; start up thread complete"
        );
        return true;
      }
      long waitTimeLeft = this.agent.getStartAttemptWaitPeriod() - elapsedTime;
      logger.info(
        "Waiting up to " + (waitTimeLeft/Constants.MILLIS_TO_SECOND) +
        " secs for reporter manager " + resource + " to check in"
      );
      synchronized( this ) {
        this.wait( waitTimeLeft );
      }
      elapsedTime = Calendar.getInstance().getTimeInMillis() - startTime;
      if ( elapsedTime > this.agent.getStartAttemptWaitPeriod() ) {
        logger.debug(
          "Reporter manager " + resource + " has not checked in after " +
          + (this.agent.getStartAttemptWaitPeriod()/Constants.MILLIS_TO_SECOND)
          + " secs; trying again"
        );
        return false;
      } else {
        logger.debug(
          "Reporter manager " + resource +
          " received notify but reporter manager not yet running"
        );
      }
    }

  }

  // Protected Functions

  /**
   * Return the command needed to start up the reporter manager on resource
   *
   * @return  A string containing a bash command (w/o password setting)
   */
  protected String getRmCmd() {
    String rmCmd = "cd " + rmRootPath + "; sbin/inca restart";
    rmCmd += " reporter-manager " ;
    rmCmd += " -l " + rmRootPath + "/var/reporter-manager.log";
    for ( String depot : this.agent.getDepotUris() ) {
      rmCmd += " -d " + depot + " ";
    }
    if ( this.agent.hasCredentials() )  {
      rmCmd += " -c etc/" + RMCERT;
      rmCmd += " -k etc/" + RMKEY;
      rmCmd += " -t etc/" + RMTRUSTED;
    }
    if ( this.suspend != null ) {
      // we add single quotes since special shell sign (>) expected
      rmCmd += " -S '" + this.suspend + "'";
    }
    rmCmd += " -e bin/inca-null-reporter";
    rmCmd += " -r var/reporter-packages";
    rmCmd += " -R sbin/reporter-instance-manager";
    rmCmd += " -v var";
    rmCmd += " -w " + waitCheckPeriod;
    rmCmd += " -i " + resource;
    rmCmd += " -a " + this.agent.getUri();
    rmCmd += " -L " + Logger.getRootLogger().getLevel().toString();
    return rmCmd;
  }

  // Private Functions

  /**
   * Run the build script on the remote resource using the method specified
   * in the resource configuration file.  The call to the build script is as
   * follows:
   *
   * <pre>
   * /bin/bash &lt;buildScript&gt; &lt;rmRootPath&gt; &lt;rmDistTarball&gt;
   * </pre>
   *
   * If the string "SUCCEEDED" is not returned by the build script, we consider
   * it failed and an exception is thrown.
   *
   * @param upgrade  If not null, runs the buildRM.sh script with the upgrade
   *                 option turned on. Otherwise a regular full install is done.
   *
   * @throws InterruptedException if interrupted while running the build script
   * @throws AccessMethodException  if build script fails
   */
  private void build( String upgrade )
    throws InterruptedException, AccessMethodException {

    String[] arguments = upgrade == null ?
      new String[]{ this.bashLoginOption, rmRootPath + "/" + RMBUILDSCRIPT,
                    rmRootPath, RMDIST, } :
      new String[]{ this.bashLoginOption, rmRootPath + "/" + RMBUILDSCRIPT,
                    "-u", upgrade, rmRootPath, RMDIST, };

    logger.info( "Invoking build script on '" + resource + "'" );
    AccessMethodOutput result = processHandle.run( "/bin/bash", arguments );
    if ( ! this.isManual() && ! isStaged() ) {
      throw new AccessMethodException(
        "Stage of reporter manager to " + resource +
        " failed: build script didn't return success: \n" +
        "stderr = ' " + result.getStderr() + "'\n" +
        "stdout = ' " + result.getStdout() + "'"
      );
    }
    logger.info( "Build script completed on '" + resource + "'" );
  }

  /**
   * Create a certificate for the reporter manager from the given certificate
   * request
   *
   * @return  The directory where the public and private key are stored.
   *
   * @throws  GeneralSecurityException if unable to create certificate
   * @throws  IOException if unable to store certificate
   * @throws OperatorCreationException
   *
   */
  protected String createRmCertificate( )
    throws GeneralSecurityException, IOException, OperatorCreationException {

    PKCS10CertificationRequest request = loadRmRequest();

    // Configure certificate generator
    Calendar notBefore = Calendar.getInstance();
    Calendar expireAfter = Calendar.getInstance();

    expireAfter.add(Calendar.YEAR, DEFAULT_AGE);

    X509Certificate agentCert = (X509Certificate)agent.getCertificate();
    X500Name agentDn = X500Name.getInstance(agentCert.getSubjectX500Principal().getEncoded());
    SubjectPublicKeyInfo subjPubKeyInfo = request.getSubjectPublicKeyInfo();
    X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(agentDn, caSerialNumber.add(BigInteger.ONE), notBefore.getTime(), expireAfter.getTime(), request.getSubject(), subjPubKeyInfo);

    // some extensions I saw in code samples
    SubjectKeyIdentifier subjKeyId = (new JcaX509ExtensionUtils()).createSubjectKeyIdentifier(subjPubKeyInfo);

    certBuilder.addExtension(Extension.subjectKeyIdentifier, false, subjKeyId);
    certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
    certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.dataEncipherment | KeyUsage.digitalSignature ));

    JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM);

    signerBuilder.setProvider(PROVIDER);

    ContentSigner signer = signerBuilder.build(agent.getKey().getPrivate());
    X509CertificateHolder certHolder = certBuilder.build(signer);
    JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();

    certConverter.setProvider(PROVIDER);

    rmCert = certConverter.getCertificate(certHolder);

    return writeRmCertificate();
  }

  /**
   * Generate reporter manager credentials.  First runs the remote command
   * to generate a key and cert request.  Then we copy over the cert request
   * and generate a certificate.  Finally we transfer the certificate back
   * to the reporter manager.
   *
   * @throws AccessMethodException  if trouble running remote create certificate
   *                                command or transferring files
   * @throws InterruptedException if interrupted while generating credentials
   * @throws ReporterManagerException if trouble creating certificate
   */
  private void generateRmCredentials() throws
    AccessMethodException, InterruptedException, ReporterManagerException {

    logger.info(
      "Generating credentials for reporter manager '" + resource + "'"
    );
    String passToUse = null;
    String passOption = null;
    if ( this.agent.getPassword() != null &&
         ! this.agent.getPassword().equals("")){
      passOption = "true";
      passToUse = this.agent.getPassword() + "\n"; // to terminate stdin
    } else {
      passOption = "false";
    }
    processHandle.run
      ( rmRootPath + "/sbin/inca",
        new String[]{ "createRmCertRequest", "-P", passOption },
        passToUse );

    // copy over cert request
    logger.info( "Copying over certificate request for " + resource );
    File rmReqFile = new File( tempDir + File.separator + RMREQ );
    if ( rmReqFile.exists() ) rmReqFile.delete();
    processHandle.get( rmRootPath + "/etc/" + RMREQ, tempDir );

    // generate certificate
    String certPath = null;
    try {
      certPath = createRmCertificate();
    } catch ( Exception e ) {
      throw new ReporterManagerException(
        "Unable to generate credentials for remote reporter manager " +
        resource + ": " + e, e
      );
    }

    // copy certificate over to reporter manager
    processHandle.put( certPath, rmRootPath + "/etc" );
  }

  /**
   * Load the certificate request from the reporter manager from file.
   *
   * @return A valid certificate request object.
   *
   * @throws IOException if unable to load remote reporter manager certificate
   *                     request file
   */
  private PKCS10CertificationRequest loadRmRequest() throws IOException {
    String reqPath = tempDir + File.separator + RMREQ;
    FileInputStream input = new FileInputStream( reqPath );
    PEMParser pemParser = null;

    try {
      pemParser = new PEMParser(new InputStreamReader(input));

      return (PKCS10CertificationRequest)pemParser.readObject();
    }
    finally {
      if (pemParser != null)
        pemParser.close();

      input.close();
    }
  }

  /**
   * Parse the working dir string and resolve different ways of expressing
   * the home dir
   *
   * @param dir    A string containing the raw value of working dir
   *
   * @param method  An access method that will be used to resolve home
   *
   * @return  A string containing a valid rm working dir
   */
  protected static String parseWorkingDir(String dir, AccessMethod method ) {
    String newWorkingDir = dir;
    if ( newWorkingDir == null || newWorkingDir.equals("")
      || newWorkingDir.equals("~") ) {
      newWorkingDir = Protocol.WORKING_DIR_MACRO_DEFAULT;
    }
    if ( newWorkingDir.startsWith("~/") ) {
      newWorkingDir = newWorkingDir.replaceFirst( "^~/", "" );
    }
    if ( ! newWorkingDir.startsWith(File.separator) ) {
      newWorkingDir = method.prependHome( newWorkingDir );
    }
    return newWorkingDir;
  }

  /**
   * Select the specified host for the resource and set the reporter manager
   * attributes accordingly.
   *
   * @param hostId  The host id to select to execute the reporter manager on.
   *
   * @throws ConfigurationException  If unable to get host configuration info
   */
  protected void setHost( int hostId ) throws ConfigurationException {

    this.host = this.equivHosts[hostId];
    logger.info(
      "Selecting host '" + this.host + "' for resource " + this.resource
    );

    // properties read in from resource configuration file
    ResourcesWrapper resources = this.agent.getResources();
    this.processHandle =
      AccessMethod.create( host, resources, this.getTemporaryDirectory() );

    this.rmRootPath = parseWorkingDir
      (resources.getValue(host,Protocol.WORKING_DIR_MACRO), this.processHandle);

    String checkPeriodString =
      resources.getValue(host, Protocol.CHECK_PERIOD_MACRO);
    if ( checkPeriodString != null ) {
      waitCheckPeriod = Integer.parseInt( checkPeriodString );
    }

    this.suspend = resources.getValue( this.host, Protocol.SUSPEND_MACRO);
  }

  /**
   * Write the loaded trusted certificates to file so that they can be
   * transferred over to the reporter manager.
   *
   * @return An array of file names for the trusted certificates
   *
   * @throws IOException  If unable to write trusted certs to disk
   */
  private String[] writeTrustedCertsToTempDir() throws IOException {

    File trustedTmpDir  = new File( tempDir + File.separator + RMTRUSTED );
    trustedTmpDir.mkdir();
    Certificate[] trusted = this.agent.getTrustedCertificates();
    String[] filenames = new String[trusted.length]; // plus key and cert
    for ( int i = 0; i < trusted.length; i++ ) {
      String trustedPath = trustedTmpDir.getPath() + File.separator + "trusted"
                           + i + ".pem";
      filenames[i] = trustedPath;
      logger.info( "Writing rm trusted cert to " + trustedPath );
      PEMWriter writer = new PEMWriter(
        new OutputStreamWriter(new FileOutputStream( trustedPath ) )
      );
      writer.writeObject( trusted[i] );
      writer.close();
    }
    return filenames;
  }

  /**
   * Extract a file from the classpath and write it to a temporary file.  Useful
   * for when a file is packed into a jar file.
   *
   * @param filename   The name of the file to locate in the classpath.
   * @param dir        The directory to where the file should be written to.
   *
   * @return The path to the file.
   *
   * @throws IOException  If unable to write a classpath resource to a file.
   */
  private String writeClasspathResourceToFile( String filename, String dir )
    throws IOException {

    String path = dir + File.separator + filename;
    InputStream resourceStream = Component.openResourceStream( filename );
    if ( resourceStream == null ) {
      throw new IOException( filename + " not found in classpath" );
    }
    logger.info( "Writing " + filename + " to " + dir );
    FileOutputStream tmpResourceStream = new FileOutputStream( path );
    byte[] buffer = new byte[2048];
    for (;;)  {
      int nBytes = resourceStream.read(buffer);
      if (nBytes <= 0) break;
      tmpResourceStream.write(buffer, 0, nBytes);
    }
    tmpResourceStream.flush();
    tmpResourceStream.close();
    resourceStream.close();
    return path;
  }

  /**
   * Write the reporter manager certificate to disk.
   *
   * @return A string containing the path to the certificate.
   *
   * @throws IOException If unable to write reporter manager certificate to disk
   */
  private String writeRmCertificate() throws IOException {
    String certPath = tempDir + File.separator + RMCERT;
    logger.info( "Writing rm certificate to " + certPath );
    PEMWriter writer = new PEMWriter(
      new OutputStreamWriter(new FileOutputStream(certPath))
    );
    writer.writeObject( rmCert );
    writer.close();
    return certPath;
  }

}
