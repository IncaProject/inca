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

my $Original_File = 'lib/Inca/AgentClient.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 155 lib/Inca/AgentClient.pm

  use File::Copy;
  use Inca::AgentClient;
  use Inca::Net::MockAgent;
  use Inca::Net::MockDepot;
  use Inca::ReporterManager::ReporterCache;
  use Inca::ReporterManager;
  use MIME::Base64;
  untie *STDOUT; #cron won't start otherwise -- not sure why
  untie *STDERR; #cron won't start otherwise -- not sure why

  Inca::Logger->screen_init( "FATAL" );

  my $SCHEDULE_FILENAME = "/tmp/scheduleAC.xml";

  sub createAgentClient {
    my $port = shift;
    my $depot_port = shift;
    my $credentials = shift;

    unlink "/tmp/rm.log";

    if ( ! defined $credentials ) {
      $credentials = { cert => "t/certs/client_ca1cert.pem",
                       key => "t/certs/client_ca1keynoenc.pem",
                       passphrase => undef,
                       trusted => "t/certs/trusted" };
    }
    my $rc = new Inca::ReporterManager::ReporterCache( "t",
      errorReporterPath => "bin/inca-null-reporter"
    );
    my $rm = new Inca::ReporterManager();
    $rm->setId( "resourceA" );
    $rm->setCheckPeriod( 1 );
    $rm->setReporterCache( $rc );
    $rm->setDepots( "incas://localhost:$depot_port" );
    $rm->setTmpDirectory( "/tmp" );
    $rm->setCredentials( $credentials );
    $rm->setRimPath( "sbin/reporter-instance-manager" );
    $rm->setLogger( "/tmp/rm.log" );
    my $client = new Inca::AgentClient( 
      $rm, 
      "incas://localhost:$port",
      %{$rm->getCredentials()}
    );
    unlink( $SCHEDULE_FILENAME ) if -f $SCHEDULE_FILENAME;
    $rm->setScheduleFilename( $SCHEDULE_FILENAME );

    return $client;
  }

  #---------------------------------------------
  # register
  #---------------------------------------------
  my $port = 8529;
  my $pid;
  my $agent = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
  if ( $pid = fork() ) {
    my $client = createAgentClient( $port, 8888 );
    is( $client->register("id0"), 1, "registration with agent was successful" );
  } else {
    waitpid( $pid, 0 );
    my ($conn, $numStmts, @responses) = $agent->accept(
      "REGISTER NEW" => [ 'OK', undef ],
    );
    $conn->close();
    exit;
  }

  #---------------------------------------------
  # try encrypted creds
  #---------------------------------------------
  my $credentials = { cert => "t/certs/client_ca1cert.pem",
                      key => "t/certs/client_ca1key.pem",
                      passphrase => "test",
                      trusted => "t/certs/trusted" };
  if ( $pid = fork() ) {
    my $client = createAgentClient( $port, 8888, $credentials );
    is( $client->register("id0"), 1, 
        "registration with encrypted keys to agent was successful" );
  } else {
    waitpid( $pid, 0 );
    my ($conn, $numStmts, @responses) = $agent->accept(
      "REGISTER NEW" => [ 'OK',  undef ],
    );
    $conn->close();
    exit;
  }

  #---------------------------------------------
  # try to send it an error
  #---------------------------------------------
  if ( $pid = fork() ) {
    my ($conn, $numStmts, @responses ) = $agent->accept(
      "REGISTER NEW" => ['OK', 'boogedyboo'],
      "ERROR" => [undef]
    );
    is( $numStmts, 3, "3 statements received from rm" );
    is( $responses[2]->getCmd(), "ERROR", "Received error" );
    like($responses[2]->getData(), qr/unknown command/,  "Received error msg");
    $conn->close();
    waitpid( $pid, 0 );
  } else {
    my $client = createAgentClient( $port, 8888 );
    $client->register("id0");
    exit;
  }

  #---------------------------------------------
  # try to ping it
  #---------------------------------------------
  if ( $pid = fork() ) {
    my ($conn, $numStmts, @responses ) = $agent->accept(
      "REGISTER NEW" => [ "OK", "PING ME" ],
      "OK ME" => [ undef ]
    );
    $conn->close();
    is( $numStmts, 3, "3 statements received from rm" );
    is( $responses[2]->getCmd(), "OK", "Received ping ack" );
    is( $responses[2]->getData(), "ME",  "Received ping data");
    waitpid( $pid, 0 );
  } else {
    my $client = createAgentClient( $port, 8888 );
    $client->register("id0");
    exit;
  }

  #---------------------------------------------
  # send a suite
  #---------------------------------------------
  my $depot_port = $port + 1;

  # store a proxy in the myproxy server
  ok( -f "$ENV{HOME}/.inca.myproxy.info", "proxy information found" );
  open( FD, "<$ENV{HOME}/.inca.myproxy.info" ) ||
    fail( "Unable to open proxy info" );
  my ($mp_hostname, $mp_port, $mp_username, $mp_password);
  while( <FD> ) {
    eval $_;
  }
  close FD;
  `grid-proxy-info 2>/dev/null`;
  cmp_ok( $?, "!=", 0, "grid-proxy-info returned error" );

  # launch the reporter manager's agent client
  my $rmpid;
  if ( ! ($rmpid=fork()) ) {
    my $client = createAgentClient( $port, $depot_port );
    $client->register("id0");
    exit; 
  } 

  # setup the mock agent to receive one register and 2 proxy requests
  my ($mapid1, $mapid2);
  if ( ! ($mapid1=fork()) ) {
    if ( ! ($mapid2=fork()) ) {
      sleep 2;
      for ( my $i = 0; $i < 2; $i++ ) {
        my ($conn, $numStmts, @responses ) = $agent->accept(
          "PROXY" => [ "HOSTNAME $mp_hostname", "PORT $mp_port", "USERNAME $mp_username", "PASSWORD $mp_password", "LIFETIME 1"],
          "OK" => []
        );
      }
      exit;
    }
    my ($mixed_suite, $grid_suite);
    { 
      local $/;
      open( FD, "t/mixed_suite.xml" );
      $mixed_suite = <FD>;
      close FD;
      open( FD, "t/grid_suite.xml" );
      $grid_suite = <FD>;
      close FD;
    }
    my ($conn, $numStmts, @responses ) = $agent->accept(
      "REGISTER NEW" => [ "OK", "SUITE $mixed_suite" ],
      "OK incas://localhost:6323/mixedSuite" => [ "SUITE $grid_suite" ],
      "OK incas://localhost:6323/gridSuite" => []
    );
    exit;
  }

  # verify the reports that come in - should be one openssl report from run
  # now and 1-2 gcc reports and 1-2 validproxy reports from cron
  my $i = 0;
  my $gccFound = 0, $opensslFound = 0, $gridFound = 0;
  my $depot = new Inca::Net::MockDepot();
  my $numReports = $depot->readReportsFromSSL(
    4, $depot_port, "ca1", "t/certs/trusted"
  );
  is( $numReports, 4, "4 reports read from mock depot" );
  my @reports = $depot->getReports();
  for my $report ( @reports ) {
    $i++;
    like($report, qr/completed>true/, "depot received completed report $i");
    $gccFound++ if ( $report =~ /gcc/ );
    $opensslFound++ if ( $report =~ /openssl/ );
    $gridFound++ if ( $report =~ /validproxy/ );
  }
  is( $opensslFound, 1, "1 openssl report received" );
  cmp_ok( $gccFound, ">=", 1, "at least 1 gcc report received" );
  cmp_ok( $gccFound, "<=", 2, "no more than 2 gcc reports received" );
  cmp_ok( $gridFound, ">=", 1, "at least 1 grid reports received" );
  cmp_ok( $gridFound, "<=", 2, "no more than 1 grid reports received" );
  for my $spid ( ($mapid1, $rmpid) ) {
    sleep 2;
    is( kill(3, $spid), 1, "$spid killed" );
    is( waitpid($spid, 0), $spid, "$spid waited on" );
  }
  ok( -f $SCHEDULE_FILENAME, "schedule written" );
  my $sched;
  {
    local $/;
    open ( FD, "<$SCHEDULE_FILENAME" );
    $sched = <FD>;
    close FD;
  }
  ok( grep( /<proxy>incas/, $sched ), "proxy found in schedule" );

  #---------------------------------------------
  # try to send it a package
  #---------------------------------------------
  if ( $pid = fork() ) {
    # regular reporter
    my ($reporter, $package);
    {
      local $/;
      open( FD, "t/cluster.java.sun.version" );
      $reporter = <FD>;
      close FD;
      open( FD, "t/appleseeds-2.2.1.tar.gz" );
      $package = <FD>;
      $package = encode_base64($package);
      close FD;
    }
    my ($conn, $numStmts, @responses ) = $agent->accept(
      "REGISTER NEW" => [ "OK", "PACKAGE cluster.java.sun.version", "FILENAME cluster.java.sun.version-1.5", "VERSION 1.5", "INSTALLPATH tmp", "PERMISSION 755", "CONTENT $reporter" ],
     "OK cluster.java.sun.version" => [ "PACKAGE appleseeds", "FILENAME appleseeds-2.2.1.tar.gz", "VERSION 2.2.1", "INSTALLPATH tmp", "PERMISSION 755", "CONTENT $package" ],
     "OK appleseeds" => [ undef ]
    );
    $conn->close();
    waitpid( $pid, 0 );
    ok( -f "t/tmp/cluster.java.sun.version-1.5", "reporter stored correctly" );
    ok( -f "t/include/appleseeds/ipseed.h", "appleseeds installed" );
    copy( "t/tmp/appleseeds-2.2.1.tar.gz", "t/appleseeds-2.2.1.tar.gz" );
    `rm -fr t/tmp t/include t/lib t/examples t/build`;
  } else {
    my $client = createAgentClient( $port, $depot_port );
    $client->register("id0");
    exit;
  }
  $agent->stop();


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 636 lib/Inca/AgentClient.pm

  use Inca::AgentClient;
  use Inca::Net::MockAgent;
  use Test::Exception;

  my $port = 8519;
  my $agent = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
  my $pid;
  if ( $pid = fork() ) {
    my $client = new Inca::AgentClient( 
      undef, 
      "incas://localhost:$port", 
      cert => "t/certs/client_ca1cert.pem",
      key => "t/certs/client_ca1keynoenc.pem",
      passphrase => undef,
      trusted => "t/certs/trusted" 
    );
    ok( $client->_ping(), "client pings" );
    $client->close();
  } else { 
    sleep 2;
    my ($conn, $numStmts, @responses ) = $agent->accept(
      "REGISTER NEW" => [ "OK", "PING ME" ],
      "OK ME" => [ undef ]
    );
    $conn->close();
    exit;
  }                     
  $agent->stop();


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 692 lib/Inca/AgentClient.pm

  use Inca::AgentClient;
  use Inca::Net::MockAgent;
  use Inca::ReporterManager;
  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;

  my $port = 8539;
  my $agent = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
  my $pid;

  unlink "afile";
  for my $type ( qw(NEW EXISTING) ) {
    if ( $pid = fork() ) {
      my $rm = new Inca::ReporterManager( id => "x" );
      my $rc = new Inca::ReporterManager::ReporterCache( "t",
        errorReporterPath => "bin/inca-null-reporter"
      );
      $rm->setReporterCache( $rc );
      $rm->setScheduleFilename( "afile" );
      my $client = new Inca::AgentClient( 
        $rm, 
        "incas://localhost:$port", 
        cert => "t/certs/client_ca1cert.pem",
        key => "t/certs/client_ca1keynoenc.pem",
        passphrase => undef,
        trusted => "t/certs/trusted" 
      );
      ok( $client->_register( "id0" ), "client registered $type" );
      $rm->stop();
      $client->close();
    } else { 
      sleep 2;
      my ($conn, $numStmts, @responses) = $agent->accept(
        "REGISTER $type" => [ 'OK', undef ],
      );
      $conn->close();
      exit;
    }                     
    open( FD, ">afile" );
    print FD "<schedule/>\n";
    close FD;
  }
  $agent->stop();
  unlink "afile";


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 14 lib/Inca/AgentClient.pm

  use Inca::AgentClient;
  use Inca::ReporterManager;

  my $rm = new Inca::ReporterManager();
  # configure $rm
  my $client = new Inca::AgentClient( $rm, "inca://localhost:8090", %creds );
  my $status = $client->register();
  # will not return until Agent closes connection or fatal error

;

  }
};
is($@, '', "example from line 14");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

