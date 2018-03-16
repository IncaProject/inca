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

my $Original_File = 'sbin/reporter-manager';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 160 sbin/reporter-manager

  use File::Copy;
  use Inca::IO;
  use Inca::Logger;
  use Inca::Net::Protocol::Statement;
  use Inca::Net::MockDepot();

  my $logfile = "/tmp/log4perl.log.$$";

  Inca::Logger->screen_init( "ERROR" );

  sub testSuite {
    my $suite = shift;
    my $num_reports = shift;
    my $encrypted = shift;

    $encrypted = 0 if ( ! defined $encrypted );

    my $port = 8518;
    my $pid;
    my $ppid = "$$";
    my @reports;
    if ( $pid = fork() ) {
      my $depot = new Inca::Net::MockDepot();
      is( 
        $depot->readReportsFromSSL($num_reports, $port,"ca1","t/certs/trusted"),
        $num_reports,
        "$num_reports reports successfully read"
      );
      @reports = $depot->getReports();
      my @files = glob( "/tmp/log4perl.log-*.$ppid" );
      ok( scalar(@files)==1, "log file was generated" );
      unlink $files[0];
    } else {
      my ($key, $passOpt, $passValue);
      if ( $encrypted ) {
        $key = "t/certs/client_ca1key.pem"; 
        $passOpt = "-P";
        $passValue = "true";
        my ($piperead, $pipewrite);
        pipe $piperead, $pipewrite;
        open( STDIN, "<&" . fileno($piperead) ) ||
          die "cannot redirect stdin: $!";
        print $pipewrite "test";
        close $pipewrite;
      } else {
        $key = "t/certs/client_ca1keynoenc.pem"; 
        $passOpt = "";
        $passValue = "";
      }
      exec "sbin/reporter-manager", "-s", $suite, "-r", "./t", "-e",
           "bin/inca-null-reporter", "-d", "incas://localhost:" . $port,
           "-c", "t/certs/client_ca1cert.pem", "-k", 
           $key, $passOpt, $passValue, "-t", "t/certs/trusted", 
           "--logfile=" . $logfile, "--id=resourceA", $ENV{INCA_TEST_SUSPEND};
    }
    kill( 3, $pid );
    return @reports;
  }

  copy( "sbin/reporter-manager", "/tmp/rm-copy$$" );
  chmod( 0755, "/tmp/rm-copy$$" );
  my $stderr = `/tmp/rm-copy$$ 2>&1`;
  like( $stderr, qr/specify alternative location/, 
       'fails if no rim and rim not in default location' );
  ok( unlink("/tmp/rm-copy$$"), "tmp rm copy deleted" );

  $stderr = `sbin/reporter-manager -R "/doesnotexist" 2>&1`;
  like($stderr, qr/.*path.*to -R|rim.*not exist/, 'fails if bad rim');
  isnt( $?, 0, "error returns non-zero status" );

  $stderr = `sbin/reporter-manager -s t/suite.xml 2>&1`;
  like( $stderr, qr/No local cache/, 'fails if no reporter cache' );

  $stderr = `sbin/reporter-manager -s t/suite.xml -r /usr/inca/cache 2>&1`;
  like( $stderr, qr/not a valid directory path/, 
        'fails if reporter cache bad dir');

  $stderr = `sbin/reporter-manager -s t/suite.xml -r /tmp 2>&1`;
  like( $stderr, qr/Path to error reporter/, 'fails if no error reporter');

  $stderr = `sbin/reporter-manager -r /tmp -e bin/inca-null-reporter -d file:/tmp/depot.tmp 2>&1`;
  like( $stderr, qr/Resource id not specified/, 
       'fails if no resource id specified');

  $stderr = `sbin/reporter-manager -s t/suite.xml -r /tmp -e bin/inca-null-reporter -i resourceA 2>&1`;
  like( $stderr, qr/No depot/, 'fails if no depots specified');

  $stderr = `sbin/reporter-manager -r /tmp -e bin/inca-null-reporter -d file:/tmp/depot.tmp -i resourceA 2>&1`;
  like( $stderr, qr/No suite file/, 'fails if no file specified' );

  $stderr = `sbin/reporter-manager -s t/suite_badxml.xml -r /tmp -e bin/inca-null-reporter -d file:/tmp/depot.tmp -i resourceA 2>&1`;
  like( $stderr, qr/Error reading/, 'fails if bad file specified' );

  $stderr = `sbin/reporter-manager -r /tmp -e bin/inca-null-reporter -d file:/tmp/depot.tmp -i resourceA 2>&1`;
  like( $stderr, qr/No suite file or agent/, 
       'fails if -s and -a are not specified');

  `sbin/reporter-manager -L ERROR -T -c t/certs/client_ca1cert.pem --k t/certs/client_ca1keynoenc.pem --t t/certs/trusted`;
  ok( ! $?, "test option worked" );

  # test ssl on sequential
  testSuite( "t/suite.xml", 2 );
  is( $?, 0, "exit status 0 when sequential suite" );

  # test suspend
  $ENV{INCA_TEST_SUSPEND} = '--suspend=load1>0.000001';
  my @suspendReports = testSuite( "t/suite.xml", 2 );
  like( $suspendReports[0], qr/high load/, 'high load detected');
  delete $ENV{INCA_TEST_SUSPEND};

  # test ssl on sequential with encrypted key
  testSuite( "t/suite.xml", 2, 1 );

  # test ssl on cron 
  my $tmp =  File::Spec->tmpdir();
  unlink( "$tmp/schedule.xml" ) if -f "$tmp/schedule.xml";
  @reports = testSuite( "t/cron_suite.xml", 2 );
  my ($type1) = $reports[0] =~ /(gcc|openssl)/m;
  my ($type2) = $reports[1] =~ /(gcc|openssl)/m;
  ok( "$type1$type2" eq "gccopenssl" || "$type1$type2" eq "opensslgcc",
      "cron thru ssl - gcc and openssl reports found" );
  ok( -f "$tmp/schedule.xml", "schedule file written" );
  unlink( "$tmp/schedule.xml" ) if -f "$tmp/schedule.xml";

  # test agent
  use Inca::Net::MockAgent;
  use Socket qw(:crlf);

  my $port = 8520;
  my $pid;

  if ( my $rmpid = fork() ) {
    my $agent = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
    my ($conn, $numStmts, @responses) = $agent->accept(
      "REGISTER NEW" => ['OK', undef ]
    );
    $conn->close();
    is( $numStmts, 2, "ping and register received" );
    is( $responses[0]->toString(), "PING ME$CRLF", "PING sent" );
    is($responses[1]->toString(), "REGISTER NEW resourceA$CRLF", "REGISTER sent");
    is( waitpid($rmpid, 0), $rmpid, "manager exitted" );
    $agent->stop();
  } else {
    exec "sbin/reporter-manager -a incas://localhost:$port -L FATAL -r ./t -e bin/inca-null-reporter -d file:///tmp/depot.out -c t/certs/client_ca1cert.pem --k t/certs/client_ca1keynoenc.pem --t t/certs/trusted -i resourceA ";
  }

  # test agent in manual mode
  unlink( "$tmp/schedule.xml" ) if -f "$tmp/schedule.xml";
  if ( my $rmpid = fork() ) {
    my $agent = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
    my ($conn, $numStmts, @responses) = $agent->accept(
      "REGISTER NEW" => ['OK', undef ]
    );
    $conn->close();
    is( $numStmts, 2, "manual - ping and register received" );
    is( $responses[0]->toString(), "PING ME$CRLF", "manual - PING sent" );
    is($responses[1]->toString(), "REGISTER NEW resourceA$CRLF", "manual - REGISTER sent");
    $agent->stop();
    sleep( 10 );
    $agent = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
    ($conn, $numStmts, @responses) = $agent->accept(
      "REGISTER NEW" => ['OK', undef ]
    );
    is( $numStmts, 2, "manual restart - ping and register received" );
    is( $responses[0]->toString(), "PING ME$CRLF", "manual restart - PING sent" );
    is($responses[1]->toString(), "REGISTER NEW resourceA$CRLF", "manual restart - REGISTER sent");
    ok( kill(INT, $rmpid), "manual restart - kill signal reached manager" );
    is( waitpid($rmpid, 0), $rmpid, "manager exitted" );
    $agent->stop();
  } else {
    exec "sbin/reporter-manager -a incas://localhost:$port -L FATAL -r ./t -e bin/inca-null-reporter -d file:///tmp/depot.out -c t/certs/client_ca1cert.pem --k t/certs/client_ca1keynoenc.pem --t t/certs/trusted -i resourceA -m";
  }
  unlink( "$tmp/schedule.xml" ) if -f "$tmp/schedule.xml";


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

