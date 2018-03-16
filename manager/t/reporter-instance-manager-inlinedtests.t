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

my $Original_File = 'sbin/reporter-instance-manager';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 156 sbin/reporter-instance-manager

  use Cwd;
  use File::Spec;
  use Inca::Logger;
  use Inca::Net::MockDepot;
  my $file_depot = "file://" . File::Spec->catfile( getcwd(), "depot.tmp.$$" );

  # sequential
  `sbin/reporter-instance-manager --id=resourceA --path="t/echo_report" --context="echo_report" --name=echo_report --niced=0 --wait=5 --depot=$file_depot --depot-timeout=120 --error-reporter="bin/inca-null-reporter" --reporter-cache="t" --var="/tmp" --version="1" --wallTime=300 --cpuTime=60 --memory=50 -level=ERROR`;
  my $depot = new Inca::Net::MockDepot;
  ok( 
    $depot->readReportsFromFile( "./depot.tmp.$$", 0 ), 
    "report read from ./depot.tmp.$$" 
  );
  is( $depot->getNumReports(), 1, "Received 1 report" );

  # test ssl
  use Inca::IO;
  use Inca::Net::Protocol::Statement;
  my $port = 8518;
  my @instances = ( 
    "sbin/reporter-instance-manager --id=resourceA --path='t/echo_report' --name=echo_report --context='echo_report' --niced=0 --wait=5 --depot=incas://localhost:$port --depot-timeout=120 --error-reporter='bin/inca-null-reporter' --reporter-cache='t' --var='/tmp' --version=1 --cert=t/certs/client_ca1cert.pem --key=t/certs/client_ca1keynoenc.pem --trusted=t/certs/trusted -level=ERROR", 
    "sbin/reporter-instance-manager --id=resourceA --target-id=resourceB --path='t/echo_report' --name=echo_report --context='echo_report' --niced=0 --wait=5 --depot=incas://localhost:$port --depot-timeout=120 --error-reporter='bin/inca-null-reporter' --reporter-cache='t' --var='/tmp' --version=1 --cert=t/certs/client_ca1cert.pem --key=t/certs/client_ca1keynoenc.pem --trusted=t/certs/trusted -level=ERROR", 
    "echo test | sbin/reporter-instance-manager --id=resourceA --path='t/echo_report' --name=echo_report --context='echo_report' --niced=0 --wait=5 --depot=incas://localhost:$port --depot-timeout=120 --error-reporter='bin/inca-null-reporter' --reporter-cache='t' --var='/tmp' --version=1 --cert=t/certs/client_ca1cert.pem --key=t/certs/client_ca1key.pem --trusted=t/certs/trusted --passphrase -level=ERROR" 
  );
  my $pid;
  for ( my $i = 0; $i <= $#instances; $i++ ) {
    if ( $pid = fork() ) {
      my $depot = new Inca::Net::MockDepot();
      $depot->readReportsFromSSL( 1, $port, "ca1", "t/certs/trusted" );
      is( $depot->getNumReports(), 1, "Received 1 report" );
      ok( $depot->{lastTargetResource} =~ /resourceB/, "found target resource" ) if ( $i == 1 );
    } else {
      exec $instances[$i];
    }
  }


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

