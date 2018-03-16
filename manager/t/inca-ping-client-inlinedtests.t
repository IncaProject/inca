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

my $Original_File = 'bin/inca-ping-client';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 46 bin/inca-ping-client

  use Inca::IO;
  use Socket qw(:crlf);

  my $port = 7833;
  my $server = Inca::IO->_startServer( $port, "ca1", "t/certs/trusted" );

  my @tries = ( "bin/inca-ping-client --uri=incas://localhost:$port --trusted=t/certs/trusted --cert=t/certs/client_ca1cert.pem --key=t/certs/client_ca1keynoenc.pem",
                "echo test | bin/inca-ping-client --uri=incas://localhost:$port --trusted=t/certs/trusted --cert=t/certs/client_ca1cert.pem --key=t/certs/client_ca1key.pem --passphrase" );
  my $pid;
  for ( my $i = 0; $i <= $#tries; $i++ ) {
    print STDERR $tries[$i], "\n";
    if ( $pid = fork() ) {
      my $server_sock = $server->accept();
      is( <$server_sock>, "PING hi there$CRLF", "ping to server worked" );
      print $server_sock "OK hi there$CRLF";
      close $server_sock;
      waitpid( $pid, 0 );
      is( $?, 0, "$i client successfully received response" );
    } else {
      exec $tries[$i];
    }
  }

  close $server;


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

