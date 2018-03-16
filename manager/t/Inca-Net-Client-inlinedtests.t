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

my $Original_File = 'lib/Inca/Net/Client.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 123 lib/Inca/Net/Client.pm

  use Inca::Net::Client;
  use Inca::Net::Protocol::Statement;
  use Socket qw(:crlf);
  use Inca::IO;
  use Cwd;
  use File::Spec;
  use Test::Exception;

  dies_ok { new Inca::Net::Client() } 'object dies with no args';

  my $port = 6430;
  my $server = Inca::IO->_startServer( $port, "ca1", "t/certs/trusted" );
  my $pid;
  if ( $pid = fork() ) {
    my $server_sock = $server->accept();
    my $client = Inca::Net::Client->_connect( $server_sock );
    ok( $client->hasReadStatement( '^STAR.$', $Inca::Net::Protocol::Statement::PROTOCOL_VERSION),
        "connected to stream" );
    $client->writeStatement( 
      "OK",
      $Inca::Net::Protocol::Statement::PROTOCOL_VERSION 
    );
    
    ok( $client->hasReadStatement( "H", "L" ), "read statement" );
    ok( $client->hasReadStatement( "Multi", "L\nsdf\nsdf\n" ), 
        "read statement2" );
    ok( $client->writeAndReadStatement( "Hi", "there", "OK", "100" ),
        "writeAndRead" );
    ok( $client->hasReadStatement("ERROR", "some error"), "writeAndLogError" );
    ok( $client->hasReadStatement( "OK" ), "Read ok only" );
    ok( $client->writeAndReadStatement( "Hi", "there", "OK" ),
        "writeAndRead" );
    close $server_sock;
  } else {
    my $client = new Inca::Net::Client( "incas://localhost:$port", 
                        cert => "t/certs/client_ca1cert.pem",
                        key => "t/certs/client_ca1keynoenc.pem", 
                        passphrase => undef,
                        trusted => "t/certs/trusted" );

    my $pingStmt = new Inca::Net::Protocol::Statement( cmd=>"H", data=>"L" );
    $client->writeStatement( $pingStmt );
    $client->writeStatement( "Multi", "L\nsdf\nsdf\n" );
    $client->readStatement();
    $client->writeStatement( "OK", "100" );
    $client->writeAndLogError( "some error" );
    $client->writeStatement( "OK", undef );
    $client->readStatement();
    $client->writeStatement( "OK", undef );
    $client->close();
    exit;
  }
  close $server;

  my $file = "file://" . File::Spec->catfile( getcwd(), "net.tmp.$$");
  my $client = new Inca::Net::Client( $file );
  $client->writeStatement( "TEST", "THIS" );
  $client->close();
  open( FD, "./net.tmp.$$" );
  local $/;
  my $content = <FD>;
  close FD;
  is( $content, "START 1$CRLF" . "TEST THIS$CRLF", "client to file okay" );



    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Net/Client.pm

  use Inca::Net::Client;
  my $client = new Inca::Net::Client( $uri, %credentials );
  $client->connect();
  my $ping_stmt = new Inca::Net::Protocol::Statement( 
    cmd => "PING",
    data => "ME"
  );
  $client->writeStatement( $ping_stmt );
  # or 
  $client->writeStatement( "PING", "ME" );
  $response = $client->readStatement();
  print $response->getCmd(), $response->getData();
  # or 
  my ($cmd, $data) = $client->readStatement();
  # or
  my $success = $client->hasReadStatement( "OK", "ME" );
  # or
  my $success = $client->writeAndReadStatement( "PING", "ME", "OK", "ME" );
  $client->close();

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

