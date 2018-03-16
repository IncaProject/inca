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

my $Original_File = 'bin/inca-run-now';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 96 bin/inca-run-now

  use Inca::IO;
  use Socket qw(:crlf);
  use Inca::Net::MockDepot;
  use Inca::Net::MockAgent;

  Inca::Logger->screen_init( "FATAL" );

  my $stderr = `bin/inca-run-now 2>&1`;
  isnt( $?, 0, "error code returned" );
  like($stderr, qr/Error, script must be run/, 'catches user error');

  my $user = getlogin();
  $stderr = `bin/inca-run-now -u $user 2>&1`;
  isnt( $?, 0, "error code returned" );
  like($stderr, qr/Missing .* pattern argument/, 'fails if no pattern');

  $stderr = `bin/inca-run-now -u $user "NoMatch" 2>&1`;
  like($stderr, qr/Missing resource/, 'resource id caught');

  $stderr = `bin/inca-run-now -u $user -i XX -s t/config.xml -L FATAL "NoMatch" 2>&1`;
  like($stderr, qr/No matches/, 'read in schedule and no matches');

  $stderr = `bin/inca-run-now -u $user -i XX -l -s t/config.xml ".*" 2>&1`;
  like($stderr, qr/Suite:  sampleSuite \(10 series\)/, 'list all' );
  like($stderr, qr/wget_page_test/, 'list all - series nickname found' );

  $stderr = `bin/inca-run-now -u $user -i XX -l -s t/config.xml sampleSuite 2>&1`;
  like($stderr, qr/Suite:  sampleSuite \(10 series\)/, 'suite specified' );
  like($stderr, qr/wget_page_test/, 'suite - series nickname found' );

  $stderr = `bin/inca-run-now -u $user -i XX -l -s t/config.xml ant_version 2>&1`;
  like($stderr, qr/Suite:  sampleSuite \(1 series\)/, 'series specified' );
  like($stderr, qr/ant_version/, 'series - series nickname found' );

  $stderr = `bin/inca-run-now -u $user -i XX -l -s t/config.xml ".*a.*" 2>&1`;
  like($stderr, qr/Suite:  sampleSuite \(5 series\)/, 'nickname preference' );

  $stderr = `bin/inca-run-now -u $user -i XX -l -S -s t/config.xml ".*a.*" 2>&1`;
  like($stderr, qr/Suite:  sampleSuite \(10 series\)/, 'suite preference' );

  unlink "var/schedule.xml";
  my $DEPOT_PORT = 6346;
  if ( my $pid = fork() ) {
    my $depot = new Inca::Net::MockDepot();
    my $numReports = $depot->readReportsFromSSL(
      2, $DEPOT_PORT, "ca1", "t/certs/trusted"
    );
    is( $numReports, 2, "2 report received" );
    my @reports = $depot->getReports();
    for my $report ( @reports ) {
      ok( $report =~ /<completed>true<\/completed>/, "found completed tag" );
    } 
  } else {
     `cp t/config_suite.xml var/schedule.xml`;
     `bin/inca-run-now -T -u $user -i XX -r t -d "incas://localhost:$DEPOT_PORT" -c t/certs/client_ca1cert.pem --k t/certs/client_ca1keynoenc.pem --t t/certs/trusted ant_version -P false '.*'`;
     exit;
  }
  my $numFiles = glob( "inca.run_now*" );
  is( $numFiles, undef, "no leftover inca.run_now" );
  unlink "var/schedule.xml";

  # check reporters with proxies
  $DEPOT_PORT++;

  # check proxy reporter
  ok( defined $ENV{GLOBUS_LOCATION}, "GLOBUS_LOCATION found for testing" );
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

  my $AGENT_PORT = 7345;
  my $agent = new Inca::Net::MockAgent($AGENT_PORT, "ca1", "t/certs/trusted");
  if ( my $pid = fork() ) {
    if ( my $ppid = fork() ) {
      my $depot = new Inca::Net::MockDepot();
      my $numReports = $depot->readReportsFromSSL(
        1, $DEPOT_PORT, "ca1", "t/certs/trusted"
      );
      is( $numReports, 1, "1 report received" );
      my @reports = $depot->getReports();
      ok( $reports[0] =~ /validproxy/, "found validproxy tag" ) || diag( $reports[0] );
    } else {
      my @myproxyArgs = ( "HOSTNAME $mp_hostname" );
      push( @myproxyArgs, "DN $ENV{MYPROXY_SERVER_DN}" ) if exists $ENV{MYPROXY_SERVER_DN};
      push( @myproxyArgs, "PORT $mp_port", "USERNAME $mp_username", "PASSWORD $mp_password", 
                          "LIFETIME 1"),
      my ($conn, $numStmts, @responses ) = $agent->accept(
        "PROXY" => [ @myproxyArgs ],
        "OK" => []
      );
      $agent->stop();
      exit;
    }
  } else {
     sleep 2;
     `bin/inca-run-now -T -u $user -i XX -a "incas://localhost:$AGENT_PORT" -r t -s t/config_grid_suite.xml -d "incas://localhost:$DEPOT_PORT" -c t/certs/client_ca1cert.pem --k t/certs/client_ca1keynoenc.pem --t t/certs/trusted ant_version -P false '.*'`;
     exit;
  }
  $numFiles = glob( "inca.run_now*" );
  is( $numFiles, undef, "no leftover inca.run.now" );
  unlink "/tmp/schedule.xml";


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

