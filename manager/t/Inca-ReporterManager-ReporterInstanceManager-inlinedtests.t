#!/usr/bin/perl -w

use Test::More 'no_plan';

package Catch;

sub TIEHANDLE {
	my($class, $var) = @_;
	return bless { var => $var }, $class;
}

sub PRINT  {
	my($self) = shift;
	${'main::'.$self->{var}} .= join '', @_;
}

sub OPEN  {}    # XXX Hackery in case the user redirects
sub CLOSE {}    # XXX STDERR/STDOUT.  This is not the behavior we want.

sub READ {}
sub READLINE {}
sub GETC {}
sub BINMODE {}

my $Original_File = 'lib/Inca/ReporterManager/ReporterInstanceManager.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 163 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;
  lives_ok { new Inca::ReporterManager::ReporterInstanceManager() } 
           'object created';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 234 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;

  my $rim = new Inca::ReporterManager::ReporterInstanceManager();
  is( $rim->checkSuspendCondition(), undef, "no suspend when no condition" );
  $rim->setSuspend( "load1>15" );
  is( $rim->checkSuspendCondition(), undef, "no suspend when no load" );
  $rim->setSuspend( "load1>0.000001" );
  is( $rim->checkSuspendCondition(), 'load1>0.000001', "suspend when load" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 519 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager();
  my $rim = new Inca::ReporterManager::ReporterInstanceManager();
  ok( ! $rim->hasAgent(), 'hasAgent for false' );
  $rim->setAgent( "incas://inca.sdsc.edu:6233" );
  ok( $rim->hasAgent(), 'hasAgent for true' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 552 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager();
  my $rim = new Inca::ReporterManager::ReporterInstanceManager();
  ok( ! $rim->hasCredentials(), 'hasCredentials for false' );
  my $credentials = { cert => 't/cert.pem', key => 't/key.pem',
                      passphrase => 'secret', trusted => 't/trusted' };
  $rim->setCredentials( $credentials );
  ok( $rim->hasCredentials(), 'hasCredentials for true' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 587 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::Config::Suite::SeriesConfig;
  use Inca::ReporterManager::ReporterInstanceManager();
  my $rim = new Inca::ReporterManager::ReporterInstanceManager();
  ok( ! $rim->hasSeriesConfig(), 'hasSeriesConfig for false' );
  my $sc = new Inca::Config::Suite::SeriesConfig();
  $rim->setSeriesConfig( $sc );
  ok( $rim->hasSeriesConfig(), 'hasSeriesConfig for true' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 619 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  untie *STDOUT;
  untie *STDERR;
  use Cwd;
  use File::Spec;
  use Inca::Logger;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::Scheduler;
  use Inca::Net::MockAgent;
  use Inca::Net::MockDepot;

  Inca::Logger->screen_init( "FATAL" );

  my $DEPOT = "file://" . File::Spec->catfile( getcwd(), "depot.tmp.$$" );
  my $PORT = 8519;
  my $RETURN_CODE = 1;
  my $VERIFY = 1;

  sub runConfig {
    my $desc = shift;
    my $sc = shift;
    my @regexs = @_;

    my $rim = Inca::ReporterManager::ReporterInstanceManager->_createTestRM();
    if ( exists $ENV{INCA_TEST_SUSPEND} ) {
      $rim->setSuspend( $ENV{INCA_TEST_SUSPEND} );
    }
    $rim->setDepots( $DEPOT );
    $rim->setSeriesConfig( $sc );
    $rim->setAgent( "incas://localhost:$PORT" ) if $desc =~ /proxy/;
    my $success = $rim->runReporter();
    if ( $VERIFY ) {
      is( $success, $RETURN_CODE, "runReporter($desc) returned $RETURN_CODE" ); 
    }
    my $depot = new Inca::Net::MockDepot;
    if ( $VERIFY ) {
      ok($depot->readReportsFromFile("./depot.tmp.$$", 0), "$desc report read");
      is($depot->{lastTargetResource}, undef, "target resource is undef" );
    }
    my @reports = $depot->getReports();
    for my $regex ( @regexs ) {
      like( $reports[0], qr/$regex/ms, "$desc report matches $regex" );
    }
    return $reports[0];
  }

  #-------------------------------------------
  # simple execution to file
  #-------------------------------------------
  my $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "echo_report" );
  $sc->setNiced( 1 );
  $sc->setContext( "sh -c \"echo_report\"" );
  $sc->setLimits( { "wallClockTime" => 10,
                    "cpuTime" => 10,
                    "memory" => 20 } );
  my $report = runConfig( "basic", $sc, "^REPORT resourceA\nsh -c \".*" );
  my ($wall_secs) = $report =~ /^wall_secs=(.*)/m;
  cmp_ok( $wall_secs, "<=", 1, "wall secs is reasonable" );

  #-------------------------------------------
  # test suspend is working
  #-------------------------------------------
  $ENV{INCA_TEST_SUSPEND} = "load1>0.00000001";
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "echo_report" );
  $report = runConfig( "suspend", $sc, "high load" );
  delete $ENV{INCA_TEST_SUSPEND};

  #-------------------------------------------
  # reporter that uses INSTALL_DIR
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "repo_report" );
  $sc->setContext( $sc->getName() . ' -verbose="1" -help="no"' );
  my $perl5lib = $ENV{PERL5LIB};
  delete $ENV{INSTALLDIR};
  runConfig( "INSTALL_DIR", $sc, ".*<dir>" . getcwd() . "\/t<\/dir>.*" );
  delete $ENV{PYTHONPATH};
  runConfig( "PYTHONPATH", $sc, ".*<dir>" . getcwd() . "\/t/lib/python<\/dir>.*" );
  delete $ENV{PERL5LIB};
  runConfig( "PERL5LIB", $sc, ".*<dir>" . getcwd() . "\/t/lib/perl<\/dir>.*" );
  delete $ENV{LD_LIBRARY_PATH};
  runConfig( "LD_LIBRARY_PATH", $sc, ".*<dir>" . getcwd() . "\/t/lib<\/dir>.*" );

  $ENV{PYTHONPATH} = "somedir";
  runConfig
    ("PYTHONPATH", $sc, ".*<dir>".getcwd() . "\/t/lib/python:somedir<\/dir>.*");
  $ENV{PERL5LIB} = "somedir";
  runConfig
    ( "PERL5LIB", $sc, ".*<dir>" . getcwd() . "\/t/lib/perl:somedir<\/dir>.*" );
  $ENV{LD_LIBRARY_PATH} = "somedir";
  runConfig
    ("LD_LIBRARY_PATH", $sc, ".*<dir>" . getcwd() . "\/t/lib:somedir<\/dir>.*");
  $ENV{PERL5LIB} = $perl5lib;
  
  #-------------------------------------------
  # reporter with env var as arg
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "var_reporter" );
  $sc->setContext( $sc->getName() . ' -verbose="1" -help="no" -args="$HOME"' );
  runConfig( 
    "env var in arg", $sc, 
    "<completed>true<\/completed>.*",
    "<value>-verbose=1 -help=no -args=$ENV{HOME}<\/value>.*"
  );

  #-------------------------------------------
  # reporter with non-existent env var as arg
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "var_reporter" );
  $sc->setContext( $sc->getName() . ' -verbose="1" -help="no" -args="$BLAH"' );
  runConfig( 
    "non-existent env var in arg", $sc, 
    "<completed>true<\/completed>.*",
    "<value>-verbose=1 -help=no -args=<\/value>.*"
  );
 
  #-------------------------------------------
  # not a valid reporter
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "not_a_reporter" );
  $sc->setContext( $sc->getName() . ' -verbose="1" -help="no"' );
  $sc->addArgument( "verbose", "1" );
  $sc->addArgument( "help", "no" );
  my $errorPrefix =
    $Inca::ReporterManager::ReporterInstanceManager::ERROR_PREFIX;
  runConfig( 
    "invalid", $sc, 
    "<completed>false<\/completed>.*",
    "<errorMessage>$errorPrefix:  Error, no report produced to stdout.*<\/errorMessage>.*",
    "<name>verbose<\/name>.*"
  );

  #-------------------------------------------
  # faulty reporter
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "bad_reporter" );
  runConfig( 
    "faulty", $sc, 
    "<completed>false<\/completed>.*",
    "<errorMessage>$errorPrefix:  Exec of reporter .*<\/errorMessage>.*"
  );

  #-------------------------------------------
  # badly written reporter
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "bad_reporter2" );
  runConfig( 
    "poor", $sc, 
    "<completed>false<\/completed>.*",
    "<errorMessage>.*no report.*"
  );

  #-------------------------------------------
  # no report but there is stderr
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "no_report" );
  runConfig( 
    "null", $sc, 
    "<completed>false<\/completed>.*",
    "<errorMessage>.*no report.*"
  );

  #-------------------------------------------
  # report to stdout with extra junk before the xml 
  #-------------------------------------------
  $sc =Inca::ReporterManager::Scheduler->_createTestConfig("xtraStdout_report");
  runConfig( "extra stdout", $sc, "^<?" );

  #-------------------------------------------
  # report to stdout with no proxy
  #-------------------------------------------
  ok( -f "$ENV{HOME}/.inca.myproxy.info", "proxy information found" );
  open( FD, "<$ENV{HOME}/.inca.myproxy.info" ) ||
    fail( "Unable to open proxy info" );
  my ($mp_hostname, $mp_port, $mp_username, $mp_password);
  while( <FD> ) {
    eval $_;
  }
  close FD;
  
  SKIP: {
    skip "proxy info not found", 1 unless -f "$ENV{HOME}/.inca.myproxy.info";
    local $SIG{CHLD} = 'DEFAULT';

    my $agent = new Inca::Net::MockAgent( $PORT, "ca1", "t/certs/trusted" );
    my $proxy_reporter = Inca::ReporterManager::Scheduler->_createTestConfig(
      "grid.middleware.globus.unit.proxy"
    );
    $proxy_reporter->setContext( $proxy_reporter->getName() . " -verbose=1 -log=3" );
    if ( fork() ) {
      runConfig( "proxy", $proxy_reporter, "completed>false" );
    } else {
      sleep 2;
      $agent->accept(
        "PROXY" => [ "ERROR No proxy information registered for resource" ],
        OK => []
      );
      exit;
    }
    $agent->stop();
  };

  #-------------------------------------------
  # report to stdout with proxy
  #-------------------------------------------
  SKIP: {
    skip "myproxy not found", 2 unless -f "$ENV{HOME}/.inca.myproxy.info";
    local $SIG{CHLD} = 'DEFAULT';
    `grid-proxy-destroy >/dev/null 2>&1`;
    `grid-proxy-info >/dev/null 2>&1`;
    cmp_ok( $?, "!=", 0, "grid-proxy-info returned error" );

    my $agent = new Inca::Net::MockAgent( $PORT, "ca1", "t/certs/trusted" );
    my $proxy_reporter = Inca::ReporterManager::Scheduler->_createTestConfig(
      "grid.middleware.globus.unit.proxy"
    );
    $proxy_reporter->setContext( "env X509_USER_PROXY=@.incaProxy@ " .
      $proxy_reporter->getName() . " -verbose=1 -log=3" );
    if ( fork() ) {
      runConfig( "proxy", $proxy_reporter, "completed>true" );
    } else {
      sleep 2;
      $agent->accept(
        "PROXY" => [ "HOSTNAME $mp_hostname", "PORT $mp_port", "USERNAME $mp_username", "PASSWORD $mp_password", "LIFETIME 1"],
        "OK" => []
      );
      exit;
    }
    $agent->stop();
  };

  #-------------------------------------------
  # report to stdout with proxy (myproxy server down)
  #-------------------------------------------
  $RETURN_CODE = 0;
  $agent = new Inca::Net::MockAgent( $PORT, "ca1", "t/certs/trusted" );
  $proxy_reporter = Inca::ReporterManager::Scheduler->_createTestConfig(
    "grid.middleware.globus.unit.proxy"
  );
  $proxy_reporter->setContext( $proxy_reporter->getName() . " -verbose=1 -log=3" );
  if ( fork() ) {
    runConfig( "proxy down", $proxy_reporter, "errorMessage>Internal" );
  } else {
    sleep 2;
    $agent->accept(
      "PROXY" => [ "HOSTNAME fake_host", "PORT $mp_port", "USERNAME $mp_username", "PASSWORD $mp_password", "LIFETIME 1"],
      "OK" => []
    );
    exit;
  }
  $agent->stop();
  $RETURN_CODE = 1;

  #-------------------------------------------
  # Limits
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "stream_report" );
  $sc->setLimits( { "cpuTime" => 1, "memory" => 100 } );
  $report = runConfig( 
    "long - limits exceeded", $sc, 
    "<completed>false<\/completed>.*",
    "<errorMessage>.*exceeded usage limits.*"
  );
  my ($memory) = $report=~ /^memory_mb=(.*)/m;
  cmp_ok( $memory, ">=", 45, "runReporter(limits) - memory is reasonable" );

  #-------------------------------------------
  # more resource intensive reporter to file
  #-------------------------------------------
  $sc->setLimits( 
    { "wallClockTime" => 100, "cpuTime" => 10, "memory" => 100 } 
  );
  $report = runConfig( "long", $sc, "completed>true" );
  ($memory) = $report =~ /^memory_mb=(.*)/m;
  cmp_ok( $memory, ">=", 45, "memory is reasonable" );

  #-------------------------------------------
  # more resource intensive reporter to ssl server
  #-------------------------------------------
  my @clientCredentials = ( { cert => "t/certs/client_ca1cert.pem",
                              key => "t/certs/client_ca1keynoenc.pem", 
                              passphrase => undef,
                              trusted => "t/certs/trusted" },
                            { cert => "t/certs/client_ca1cert.pem",
                              key => "t/certs/client_ca1key.pem", 
                              passphrase => "test",
                              trusted => "t/certs/trusted" }
                          );
  $depot = new Inca::Net::MockDepot();
  my $pid;
  if ( $pid = fork() ) {
    my $numReports = $depot->readReportsFromSSL(
      2, $PORT, "ca1", "t/certs/trusted"
    );
    is( $numReports, 2, "2 reports received from mock depot" );
    is( $depot->{lastTargetResource}, "resourceA", "target resource received" );
  } else {
    $VERIFY = 0;
    for ( my $i = 0; $i <= $#clientCredentials; $i++ ) {
      $sc = Inca::ReporterManager::Scheduler->_createTestConfig("echo_report");
      $sc->setTargetResource( "resourceA" );
      $DEPOT = "incas://localhost:$PORT";
      runConfig( "quick ssl", $sc );
    }
    exit;
  }


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1095 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;

  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                            agent => "cuzco.sdsc.edu:8233" );
  my $agent = $rim->getAgent();
  is( $agent, "cuzco.sdsc.edu:8233", 'Set agent works from constructor' );
  $rim->setAgent( "inca.sdsc.edu:8235" );
  $agent = $rim->getAgent();
  is( $agent, "inca.sdsc.edu:8235", 'Set depots works from function' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1141 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::Constants qw(:defaults);

  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                                                           checkPeriod => 3 );
  is( $rim->getCheckPeriod(), 3, 'set check period from constructor' );
  $rim->setCheckPeriod( 10 );
  is( $rim->getCheckPeriod(), 10, 'set check period' );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( );
  is( $rim->getCheckPeriod(), $DEFAULT_CHECK_PERIOD,
     'default check period is $DEFAULT_CHECK_PERIOD');


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1186 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;

  my $credentials = { cert => 't/cert.pem', key => 't/key.pem',
                      passphrase => 'pwd', trusted => 't/trusted' };
  my $credentials_new = { cert => 't/new/cert.pem', key => 't/key.pem',
                      passphrase => undef, trusted => 't/trusted' };
  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                  credentials => $credentials );
  ok( eq_hash($rim->getCredentials(), $credentials), 
          'set credentials worked from constructor' );
  $rim->setCredentials( $credentials_new );
  ok( eq_hash($rim->getCredentials(), $credentials_new), 
          'set/get credentials worked' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1237 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;

  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                            depotURIs => ["cuzco.sdsc.edu:8234"] );
  my @depots = $rim->getDepots();
  is( $depots[0], "cuzco.sdsc.edu:8234", 'Set depots works from constructor' );
  $rim->setDepots( qw(inca.sdsc.edu:8235 inca.sdsc.edu:8236) );
  @depots = $rim->getDepots();
  is( $depots[0], "inca.sdsc.edu:8235", 'Set depots works from function (1)' );
  is( $depots[1], "inca.sdsc.edu:8236", 'Set depots works from function (2)' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1285 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::Constants qw(:defaults);

  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                                                           depotTO => 3 );
  is( $rim->getDepotTimeout(), 3, 'set depot timeout from constructor' );
  $rim->setDepotTimeout( 10 );
  is( $rim->getDepotTimeout(), 10, 'set depot timeout' );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( );
  is( $rim->getDepotTimeout(), $DEFAULT_DEPOT_TIMEOUT, 
      'default check period is $DEFAULT_DEPOT_TIMEOUT' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1331 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;

  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                                                           id => "resourceA" );
  is( $rim->getId(), "resourceA", 'set id from constructor' );
  $rim->setId( "resourceB" );
  is( $rim->getId(), "resourceB", 'set id' );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( );
  is( $rim->getId(), undef, 'default id is undef' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1375 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;
  use Cwd;

  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  my $rc_new = new Inca::ReporterManager::ReporterCache( "/tmp" );
  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                  reporterCache => $rc );
  is( $rim->getReporterCache->getLocation(), getcwd() . "/t",
          'set reporter admin worked from constructor' );
  $rim->setReporterCache( $rc_new );
  is( $rim->getReporterCache->getLocation(), "/tmp",
          'set reporter admin worked from set/get functions' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1424 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::Config::Suite::SeriesConfig;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;

  my $config1 = new Inca::Config::Suite::SeriesConfig( path => "path1" );
  my $config2 = new Inca::Config::Suite::SeriesConfig( path => "path2" );
  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
    config => $config1
  );
  is ( $rim->getSeriesConfig()->getPath(), 'path1', 
       'set config worked from constructor' );
  $rim->setSeriesConfig( $config2 );
  is ( $rim->getSeriesConfig()->getPath(), 'path2', 'set/get config worked' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1473 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::Constants qw(:defaults);

  my $rim = 
    new Inca::ReporterManager::ReporterInstanceManager( suspend => 'load1>5' );
  is( $rim->getSuspend(), 'load1>5', 'set suspend from constructor' );
  $rim->setSuspend( 'load5>10' );
  is( $rim->getSuspend(), 'load5>10', 'set suspend' );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( );
  is( $rim->getSuspend(), undef, 'default suspend is undef');


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1517 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;

  my $rim = new Inca::ReporterManager::ReporterInstanceManager(); 
  is( $rim->getTmpDirectory(), "/tmp", 'default tmp set' );
  $rim->setTmpDirectory( "/scratch" );
  is( $rim->getTmpDirectory(), "/scratch", 'set/getTmpDirectory work' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1576 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::ReporterCache;
  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;
  use Cwd;

  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  my $sc = new Inca::Config::Suite::SeriesConfig();
  $sc->setName( "echo_report" );
  $sc->setPath( "t/echo_report" );
  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                  config => $sc,
                  reporterCache => $rc );
  is( $rim->_createCmdLine(), undef,
      '_createCmdLine returns undef when context not specified' );
  $sc->setContext( $sc->getName() );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                  config => $sc,
                  reporterCache => $rc );
  is( $rim->_createCmdLine(), "t/echo_report",
          '_createCmdLine works for simple case' );
  $sc->setNiced( 1 );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                  config => $sc,
                  reporterCache => $rc );
  is( $rim->_createCmdLine(), "nice t/echo_report",
          '_createCmdLine works for simple case with nice' );
  
  # try set context
  $sc->setNiced( 0 );
  $sc->setContext( 
    'PATH=bin:${PATH}; ' . $sc->getName() . '; rm -f /tmp/blah' 
  );
  is( $rim->_createCmdLine(), 
      'PATH=bin:${PATH}; nice t/echo_report; rm -f /tmp/blah',
       '_createCmdLine works for context' );
  
  $sc->setContext( 
    'PATH=bin:${PATH}; ' . $sc->getName() . ' -name1="value1" -name1="value2"; rm -f /tmp/blah' 
  );
  is( $rim->_createCmdLine(), 
      'PATH=bin:${PATH}; nice t/echo_report -name1="value1" -name1="value2"; rm -f /tmp/blah',
      '_createCmdLine works for with args' );

  # special chars in reporter name
  $sc->setName( "grid.security++.unit" );
  $sc->setContext( 
    'PATH=bin:${PATH}; ' . $sc->getName() . ' -name1="value1" -name1="value2"; rm -f /tmp/blah' 
  );
  is( $rim->_createCmdLine(), 
      'PATH=bin:${PATH}; nice t/echo_report -name1="value1" -name1="value2"; rm -f /tmp/blah',
      '_createCmdLine works for special chars in reporter name' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1729 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  untie *STDOUT;
  untie *STDERR;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::Scheduler;
  use Inca::Config::Suite::SeriesConfig;
  use File::Spec;
  use Cwd;

  my $errorPrefix =
    $Inca::ReporterManager::ReporterInstanceManager::ERROR_PREFIX;
  my $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "echo_report" );
  $sc->setLimits( { "wallClockTime" => 10,
                    "cpuTime" => 10,
                    "memory" => 20 } );
  my $rim = new Inca::ReporterManager::ReporterInstanceManager->_createTestRM();
  $rim->setDepots( "file://" . File::Spec->catfile(getcwd(), "depot.tmp.$$") );
  $rim->setSeriesConfig( $sc );
  my $success = $rim->_generateErrorReport( "An Error Message", 0 );
  like($rim->{stdout}, qr/^<rep:report/m, "_generateErrorReport: report found");
  like( $rim->{stdout}, qr/<errorMessage>An Error Message/m, 
        "_generateErrorReport - error found" );

  $success = $rim->_generateErrorReport( "An Error Message", 1 );
  like
    ($rim->{stdout}, qr/^<rep:report/m, "_generateErrorReport - report found");
  like( $rim->{stdout}, qr/<errorMessage>$errorPrefix:  An Error Message/m,
        "_generateErrorReport - error found" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1860 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  untie *STDOUT;
  tie *STDERR, 'Catch', '_STDERR_' or die $!;

  use Inca::IO;
  use Inca::ReporterManager::ReporterCache;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::Scheduler;
  use Inca::Net::Protocol::Statement;
  use Inca::Net::MockAgent;
  use Inca::Config::Suite::SeriesConfig;

  use Inca::Logger;

  Inca::Logger->screen_init( "ERROR" );

  my $PORT = 6329;

  sub sendReport {
    my $depotTimeout = shift;

    sleep( 2 );
    my $sc = Inca::ReporterManager::Scheduler->_createTestConfig("echo_report");
    my $rim = Inca::ReporterManager::ReporterInstanceManager->_createTestRM();
    $rim->setSeriesConfig( $sc );
    $rim->setDepotTimeout( $depotTimeout) if defined $depotTimeout;
    $rim->{stdout} = "stdout";
    $rim->{usage} = "usage";
    return $rim->_sendReportToDepot(  "incas://localhost:$PORT"  );
  }

  my $pid;
  if ( $pid = fork() ) {
    my $success = sendReport(10);
    is( $success, 1, "report accepted" );
    $_STDERR_ = "";

    $success = sendReport(10);
    is( $success, 0, "error report from depot" );
    like( $_STDERR_, qr/This is an error/, "error logged when depot error");
    like( $_STDERR_, qr/Error writing to depot/, 
          "message logged when report not sent to the depot" );

    $_STDERR_ = "";
    $success = sendReport(10);
    is( $success, 1, "error report issued" );
    like(  $_STDERR_, qr/Report rejected by Inca depot/, 
          "message logged when error report issued");

    $_STDERR_ = "";
    $success = sendReport(10);
    is( $success, 0, "error report rejected" );
    like( $_STDERR_, qr/Internal error: problem/,
          "message logged when depot rejects error report" );

    $_STDERR_ = "";
    $success = sendReport(10);
    is( $success, 0, "error sending error report" );
    like( $_STDERR_, qr/Detected depot error/,
          "message logged when depot errors on error report" );
  } else {
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport( $PORT );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR This is an error message from the depot" );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR Invalid report XML" );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport($PORT );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR Invalid report XML" );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR Invalid report XML" );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR Invalid report XML" );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR java.net.SocketTimeoutException: Read timed " );
    exit;
  }


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 2001 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  untie *STDOUT;
  tie *STDERR, 'Catch', '_STDERR_' or die $!;

  use Inca::IO;
  use Inca::ReporterManager::ReporterCache;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::Scheduler;
  use Inca::Net::Protocol::Statement;
  use Inca::Net::MockAgent;
  use Inca::Net::MockDepot;
  use Inca::Config::Suite::SeriesConfig;

  use Inca::Logger;

  Inca::Logger->screen_init( "INFO" );

  my $PORT = 6329;

  sub sendReportToDepots {
    my $depotTimeout = shift;
    my @ports = @_;

    my @uris = map( "incas://localhost:$_", @ports );

    sleep( 2 );
    my $sc = Inca::ReporterManager::Scheduler->_createTestConfig("echo_report");
    my $rim = Inca::ReporterManager::ReporterInstanceManager->_createTestRM();
    $rim->setDepots( @uris );
    $rim->setSeriesConfig( $sc );
    $rim->setDepotTimeout( $depotTimeout) if defined $depotTimeout;
    $rim->{stdout} = "stdout";
    $rim->{usage} = "usage";
    `rm -f $rim->{archiveDir}/*`;
    return $rim->_sendReportToDepots();
  }

  my $pid;
  if ( $pid = fork() ) {
    my $success = sendReportToDepots(10, $PORT);
    is( $success, 1, "report accepted" );
    like( $_STDERR_, qr/Report successfully sent/, "success logged");
    $_STDERR_ = "";

    $success = sendReportToDepots(10, 20000, $PORT);
    is( $success, 1, "report accepted" );
    like( $_STDERR_, qr/Report successfully sent/, "success logged");
    like( $_STDERR_, qr/Unable to send report/, 
          "error logged when first depot not available");
    $_STDERR_ = "";

    $success = sendReportToDepots(10, $PORT);
    is( $success, 0, "error sending report" );
    like($_STDERR_, qr/Unable to send report/, "error logged when depot error");
    my @files = glob( "/tmp/archive/*" );
    is( $#files, 0, "report archived" );
 
    $success = sendReportToDepots(5, $PORT);
    is( $success, 0, "timeout of send worked" );
    like($_STDERR_, qr/exceeded/, "error logged when depot timeout exceeded");
    @files = glob( "/tmp/archive/*" );
    is( $#files, 0, "report archived" );

    sleep 6; # wait for mock depot to close


  } else {
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport( $PORT );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport( $PORT );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR This is an error message from the depot" );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR This is an error message from the depot", 8 );

    exit;
  }


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 2148 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  untie *STDOUT;
  tie *STDERR, 'Catch', '_STDERR_' or die $!;

  use Inca::IO;
  use Inca::ReporterManager::ReporterCache;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::Scheduler;
  use Inca::Net::Protocol::Statement;
  use Inca::Net::MockAgent;
  use Inca::Config::Suite::SeriesConfig;

  use Inca::Logger;

  Inca::Logger->screen_init( "ERROR" );

  my $PORT = 6329;

  sub runRIM {
    my $depotTimeout = shift;

    sleep( 2 );
    my $sc = Inca::ReporterManager::Scheduler->_createTestConfig("echo_report");
    my %credentials = ( cert => "t/certs/client_ca1cert.pem",
                        key => "t/certs/client_ca1keynoenc.pem", 
                        passphrase => undef,
                        trusted => "t/certs/trusted" );
    my $rim = Inca::ReporterManager::ReporterInstanceManager->_createTestRM();
    $rim->setDepots( "incas://localhost:$PORT" );
    $rim->setSeriesConfig( $sc );
    $rim->setDepotTimeout( $depotTimeout) if defined $depotTimeout;
    $rim->{stdout} = "stdout";
    $rim->{usage} = "usage";
    my $client = new Inca::Net::Client("incas://localhost:$PORT", %credentials);
    ok( defined $client, "client started" );
    my $success = $rim->_sendReportToStream( $client );
    $client->close();
    return $success;
  }

  my $pid;
  if ( $pid = fork() ) {
    my $success = runRIM(10);
    is( $success, 1, "report accepted" );
    $_STDERR_ = "";

    $success = runRIM(10);
    is( $success, 0, "invalid report xml -- error report sent" );
    like( $_STDERR_, qr/Report rejected by Inca depot/, 
          "error logged when report rejected");

    $_STDERR_ = "";
    $success = runRIM(10);
    is( $success, -1, "error received by depot" );
    like( $_STDERR_, qr/Error writing to depot/, 
          "error logged when error issued by depot");

    $_STDERR_ = "";
    $success = runRIM(10);
    is( $success, -1, "unknown response from depot" );
    like( $_STDERR_, qr/Unknown response from the depot/, 
          "message logged when unknown depot response" );

  } else {
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport( $PORT );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR Invalid report XML" );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR java.net.SocketTimeoutException: Read timed" );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "STARS shine at night" );
    exit;
  }


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/ReporterManager/ReporterInstanceManager.pm

  use Inca::ReporterManager::ReporterInstanceManager;
  my $rim = new Inca::ReporterManager::ReporterInstanceManager();
  my $sc = new Inca::Config::Suite::SeriesConfig();
  $sc->setPath( "var/reporter-packages/bin/cluster.compiler.gcc.version-3" );
  $sc->setName( "cluster.compiler.gcc.version" );
  $sc->setVersion( "3" );
  $sc->setContext( "cluster.compiler.gcc.version -verbose=1 -log=3" );
  my $credentials = { cert => "t/certs/client_ca1cert.pem",
                      key => "t/certs/client_ca1keynoenc.pem", 
                      passphrase => undef,
                      trusted => "t/certs/trusted" };
  $rim = new Inca::ReporterManager::ReporterInstanceManager();
  $rim->setSeriesConfig( $sc );
  $rim->setId( "resourceA" );
  $rim->setCredentials( $credentials );
  $rim->setCheckPeriod(1);
  $rim->setPath( "t/stream_report" );
  $rim->setDepots( "incas://localhost:$port" );
  $rim->setReporterCache( $rc );
  my $success = $rim->runReporter();

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

