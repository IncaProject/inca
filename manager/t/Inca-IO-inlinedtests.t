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

my $Original_File = 'lib/Inca/IO.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 154 lib/Inca/IO.pm

  use Inca::IO;
  use Test::Exception;
  use File::Temp qw/ :POSIX /;
  dies_ok { new Inca::IO() } 'dies when called with no args';
  my $filename = tmpnam();
  my $io = new Inca::IO( "file://$filename" );
  print $io "hi\n";
  close( $io );
  open( CHECK, "< $filename" );
  local $/;
  my $contents = <CHECK>;
  close CHECK;
  is( $contents, "hi\n", "able to write to file IO stream from constructor" );

  my $port = 6429;
  my $server = Inca::IO->_startServer( $port, "ca1", "t/certs/trusted" );
  my $pid;
  if ( $pid = fork() ) {
    my $server_sock = $server->accept();
    is( <$server_sock>, "hi\n", 
        "able to write to SSL IO stream from constructor" );
    close $server_sock;
  } else {
    $io = new Inca::IO( "incas://localhost:$port", 
                        cert => "t/certs/client_ca1cert.pem",
                        key => "t/certs/client_ca1keynoenc.pem", 
                        passphrase => undef,
                        trusted => "t/certs/trusted" );
    print $io "hi\n";
    close $io;
    exit;
  }
  close $server;


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 239 lib/Inca/IO.pm

  use strict;
  use warnings;
  use Inca::IO;
  use Test::Exception;
  use File::Temp qw/ :POSIX /;

  my $filename = tmpnam();
  my $io = Inca::IO->createFileWriteStream( $filename );
  isa_ok( $io, "IO::File", 'IO::File object created from createFileWriteStream' );
  print $io "Hello there\n";
  ok( -f $filename, 'file creation works' );
  close( $io );
  open( CHECK, "< $filename" );
  local $/;
  my $contents = <CHECK>;
  close CHECK;
  is( $contents, "Hello there\n", "able to write to file IO stream" );
  unlink $filename;
  ok( ! -f $filename, 'file deleted ok' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 303 lib/Inca/IO.pm

  use strict;
  use warnings;
  use Inca::IO;
  use Test::Exception;
  use File::Temp qw/ :POSIX /;

  # TODO


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 378 lib/Inca/IO.pm

  use warnings;
  use Inca::IO;
  use IO::Socket::SSL;
  use Test::Exception;
  use Inca::Logger;

  tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
  tie *STDERR, 'Catch', '_STDERR_' or die $!;

   my %logconfig = ( 
    'log4perl.rootLogger' => 'ERROR, Screen',
    'log4perl.appender.Screen' => 'Log::Log4perl::Appender::Screen',
    'log4perl.appender.Screen.stderr' => 1,
    'log4perl.appender.Screen.layout' => 'Log::Log4perl::Layout::SimpleLayout' 
  );
  Inca::Logger::init( \%logconfig );
 
  my $port = 6440;
  my $server = Inca::IO->_startServer( $port, "ca1", "t/certs/trusted" );
  ok( $server, "server set up correctly for testing" ) || 
      diag( "Failure was $!\n" );

  my ( $pid, $i );
  my @test_data;
  my @test_desc = ( 'connect with same CA worked - dir',
                    'connect with same CA worked - dir w/ passphrase',
                    'connect with same CA worked - file',
                    'connect with different CA worked' );
  if ( $pid = fork() ) {
    for ( $i = 0; $i <= $#test_desc; $i++ ) {
      my $server_sock = $server->accept();
      my $out;
      while ( defined ($out = <$server_sock>) ) {
        push ( @test_data, $out );
      }
      close $server_sock;
    }
  } else {
    # same ca using trusted ca dir
    my $client = Inca::IO->_startClient( $port, "ca1", 't/certs/trusted' );
    print $client "hi\n";
    close $client;

    # same ca using trusted ca dir with passphrase
    $client = Inca::IO->_startClient( $port, "ca1", 't/certs/trusted', 'test' );
    print $client "hi\n";
    close $client;

    # same ca using trusted ca file
    $client = Inca::IO->_startClient( $port, "ca1", 't/certs/all.pem' );
    print $client "hi\n";
    close $client;

    # different ca using trusted ca file
    $client = Inca::IO->_startClient( $port, "ca2", 't/certs/trusted' );
    print $client "hi\n";
    close $client;

    exit;
  }
  for ( $i = 0; $i <= $#test_desc; $i++ ) {
    is( $test_data[$i], "hi\n", $test_desc[$i] );
  }
  close $server;

  ###### server doesn't trust client ########
  $port++;
  $server = Inca::IO->_startServer( $port, "ca1", "t/certs/trusted/917e65e4.0",
                                    trust_type => "file" );
  ok( $server, "server set up correctly for testing" ) || 
      diag( "Failure was $!\n" );

  if ( $pid = fork() ) {
    my $new_client = Inca::IO->_startClient( $port, "ca2", 't/certs/trusted' );
    ok( ! defined $new_client, 'server does not trust client - undef returned');
    like( $_STDERR_, qr/Unable to create/, 'server does not trust client' );
  } else {
    my $server_sock = $server->accept();
    exit;
  }
  close $server;

  ######### client doesn't trust server ########
  $port++;
  $server = Inca::IO->_startServer( $port, "ca1", "t/certs/trusted" );
  ok( $server, "server set up correctly for testing" ) || 
      diag( "Failure was $!\n" );
  if ( $pid = fork() ) {
    $new_client = Inca::IO->_startClient( $port, "ca2", 
                                          't/certs/trusted/1e415a79.0' );
    ok( ! defined $new_client, 'client does not trust server - undef returned');
    like( $_STDERR_, qr/Unable to create/, 'client does not trust server' );
  } else {
    my $server_sock = $server->accept();
    exit;
  }
  close $server;

  ######### client nor server trust each other ########
  $port++;
  $server = Inca::IO->_startServer( $port, "ca1", "t/certs/trusted/917e65e4.0",
                                    trust_type => "file" );
  ok( $server, "server set up correctly for testing" ) || 
      diag( "Failure was $!\n" );
  if ( $pid = fork() ) {
    my $new_client = Inca::IO->_startClient( $port, "ca2", 
                                               't/certs/trusted/1e415a79.0' );
    ok( ! defined $new_client, 
        'client nor server trust each other - undef returned');
    like($_STDERR_, qr/Unable to create/, 'client nor server trust each other');
  } else {
    my $server_sock = $server->accept();
    exit;
  }
  close $server;


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 576 lib/Inca/IO.pm

  use warnings;
  use Inca::IO;
  use File::Spec;

  # cleanup
  my @files = ( glob( "t/incatest*"), glob("/tmp/incatest*") );
  for my $file ( @files ) { unlink $file; }

  # just try to open 10 tempfiles
  for ( my $i = 0; $i < 10; $i++ ) {
    my ($fh, $filename) = Inca::IO->tempfile( "incatest", "t" );
    is ( $filename, "t/incatest.$$.$i", "correct filename" );
    ok( -f $filename, "tempfile $filename created" );
    print $fh "la di da\n";
    close $fh;
    open( $fh, "<$filename" );
    my $content = <$fh>;
    ok( $content =~ /la di da/, "stuff written to file" );
    close $fh;
  }

  # make sure the next 90 are put in t
  for ( my $i = 10; $i < 100; $i++ ) {
    my ($fh, $filename) = Inca::IO->tempfile( "incatest", "t" );
    is ( $filename, "t/incatest.$$.$i", "correct $filename filename" );
  }

  # the next 100 should be in /tmp
  for ( my $i = 0; $i < 100; $i++ ) {
    my ($fh, $filename) = Inca::IO->tempfile( "incatest", "t" );
    ok( -f File::Spec->tmpdir() . "/incatest.$$.$i", "correct $filename filename" );
  }
  my ($fh, $filename) = Inca::IO->tempfile( "incatest", "t" );

  # max tries over, should be undef
  is( $fh, undef, "fh undefined when exceeded num files" );
  is( $filename, undef, "filename undefined when exceeded num files" );
  
  # cleanup
  @files = ( glob( "t/incatest*"), glob("/tmp/incatest*") );
  for my $file ( @files ) { unlink $file; }

  # goes to /tmp if no dir specified
  ($fh, $filename) = Inca::IO->tempfile( "incatest" );
  ok( -f File::Spec->tmpdir() . "/incatest.$$.0", "goes to /tmp" );
  unlink( File::Spec->tmpdir() . "/incatest.$$.0" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 675 lib/Inca/IO.pm

  use Inca::IO;
  use Test::Exception;

  dies_ok { Inca::IO->_readSSLCreds( cert => 't/certs/cert.pem', 
                                     trusted => 't/certs/trusted' ); }
          'dies when not all creds specified';
  my @ssl;
  lives_ok { @ssl = Inca::IO->_readSSLCreds( cert => 't/certs/cert.pem', 
                                      key => 't/certs/key.pem',
                                      passphrase => "test",
                                      trusted => 't/certs/trusted' ); }
          'lives when all creds specified';
  ok(eq_array(\@ssl, [qw(t/certs/cert.pem t/certs/key.pem test t/certs/trusted)]),
      'correct array returned from _readSSLCreds' );
  lives_ok { @ssl = Inca::IO->_readSSLCreds( cert => 't/certs/cert.pem', 
                                      key => 't/certs/key.pem',
                                      passphrase => undef,
                                      trusted => 't/certs/trusted' ); }
          'lives when all creds specified';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/IO.pm

  use Inca::IO; 
  my $stream = new Inca::IO( 
    "incas://localhost:1111", 
    cert => "mycert.pem",  
    key => "mykey.pem", 
    passphrase => $passphrase,
    trusted => "mytrustedcertdir" 
  );
  print $stream "important data";
  close( $stream );

  # or

  my $stream = new Inca::IO( "inca://localhost:1111" );
  print $stream "important data";
  close( $stream );

  # or

  $stream = new Inca::IO( "file:///home/file" );
  print $stream "important data";
  close( $stream );

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

