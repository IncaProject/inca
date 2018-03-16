package Inca::IO;

################################################################################

=head1 NAME

Inca::IO - Wrapper module for handling IO streams and functions

=head1 SYNOPSIS

=for example begin

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

=for example end

=head1 DESCRIPTION

This module provides an abstract factory for dealing with different IO
streams.  It currently handles 3 types:

=over 8

=item B<Type>

B<Format>

=item file

file://path_to_file

=item inca

inca://hostname:port 

=item incas

incas://hostname:port 

=back

It also provides some IO-related static functions like tempfile.

=cut 
################################################################################

#=============================================================================#
# Usage
#=============================================================================#

# pragmas
use strict;
use warnings;
use vars qw($VERSION);

# Inca
use Inca::Logger;
use Inca::Validate qw(:all);

# Perl standard
use Carp;
use File::Spec;
use IO::File;
use IO::Socket::INET;

my $MAX_TRIES = 100; # for tempfile
my $ioSocketSslAvailable;
BEGIN {
  $ioSocketSslAvailable = 0;
  eval {
    require IO::Socket::SSL; 
    $IO::Socket::SSL::DEBUG = 1;
  };
  $ioSocketSslAvailable = 1;
}

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $HOST_EXPR = { regex => qr/[\w\.]+/ };
my $PORT_EXPR = { regex => qr/\d+/ };
my $PATH_EXPR = { type => SCALAR };
my $PASSPHRASE_EXPR = { type => SCALAR | UNDEF };

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#

=head2 new( $uri, @args )

Class constructor which returns a new Inca::IO object based on the uri.

=over 2

B<Arguments>:

=over 7

=item uri

A string in the format of <scheme>://<path> where

=over 13

=item scheme

The type of URI being represented by the string (either I<file> or I<incas>)

=item path

The location of the URI being represented (e.g., localhost:7070)

=back

=item args

A list of follow on arguments for the stream type (e.g., cert, key, passphrase, and
trusted certificate directory for the SSL connection).

=back

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my ($class, $url) = @_;

  my ($scheme, $path) = $url =~ m#^(\w+)://(.*)$#;
  my $logger = Inca::Logger->get_logger( $class );

  if ( $scheme eq 'file' ) {
    return Inca::IO->createFileWriteStream( $path );
  } elsif ( $scheme eq 'inca' ) {
    my ( $host, $port ) = split( ':', $path );
    return Inca::IO->createSocketStream( $host, $port );
  } elsif ( $scheme eq 'incas' ) {
    my ( $host, $port ) = split( ':', $path );
    my @creds = Inca::IO->_readSSLCreds( @_ );
    return Inca::IO->createSSLStream( $host, $port, @creds );
  } else {
    $logger->logcroak( "Unsupported scheme '$scheme'" );
  }

}

#-----------------------------------------------------------------------------#

=head2 createFileWriteStream( $path )

Create new Inca::IO object which wraps a IO::File object.  

=over 2

B<Arguments>:

=over 7

=item path

A string that contains the path to the file to be written to.

=back

B<Returns>:

A new IO::File object. 

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub createFileWriteStream {
  my $class = shift;

  my $logger = Inca::Logger->get_logger( $class );

  my @args = validate_pos( @_, SCALAR );
  my $path = $args[0];
  my $fh = new IO::File( ">> $path" );
  if ( ! defined $fh ) {
    $logger->error( "Unable to open file $path: $!" );
  }
  return $fh;
}

#-----------------------------------------------------------------------------#

=head2 createSocketStream( $server )

Create new Inca::IO object which wraps a IO::Socket::INET object.  

=over 2

B<Arguments>:

=over 7

=item server

The server specification in host:port format

=back

B<Returns>:

A new Inca::IO::INET object. 

=back

=begin testing

  use strict;
  use warnings;
  use Inca::IO;
  use Test::Exception;
  use File::Temp qw/ :POSIX /;

  # TODO

=end testing

=cut
#-----------------------------------------------------------------------------#
sub createSocketStream {
  my $class = shift;
  my ( $host, $port ) = validate_pos( @_, $HOST_EXPR, $PORT_EXPR );

  my $sock = new IO::Socket::INET( 
    PeerAddr => $host,
    PeerPort => $port,
    Proto    => 'tcp'
  );

  if ( ! defined $sock ) {
    my $logger = Inca::Logger->get_logger( $class );
    $logger->error( "Unable to contact $host:$port  $!" );
  }
  return $sock;
}

#-----------------------------------------------------------------------------#

=head2 createSSLStream( $host, $port, $cert, $key, $passphrase, $trusted_certs )

Create new Inca::IO object which wraps a IO::Socket::SSL.

=over 2

B<Arguments>:

=over 7

=item host

A string that contains the name of the host to contact.

=item port

An integer that contains the port number on the host to contact.

=item cert

A string containing the path to the certificate file.

=item key

A string containing the path to the key file.

=item passphrase

A string containing passphrase to the provided key or undefined if none

=item trusted_certs

A string containing the path to the trusted certificate file or directory.

=back

B<Returns>:

An object of type IO::Socket::SSL. 

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub createSSLStream {
  my $class = shift;

  my $logger = Inca::Logger->get_logger( $class );
  if(!$ioSocketSslAvailable) {
    $logger->error("SSL not available");
    return undef;
  }

  my ( $host, $port, $cert, $key, $passphrase, $trusted_certs ) = 
     validate_pos( @_, $HOST_EXPR, $PORT_EXPR, $PATH_EXPR, $PATH_EXPR, 
                       $PASSPHRASE_EXPR, $PATH_EXPR );

  my ( $ca_dir, $ca_file );
  if ( -f $trusted_certs ) {
    $ca_file = $trusted_certs;
    $ca_dir = "";
  } elsif ( -d $trusted_certs ) {
    $ca_dir = $trusted_certs;
    $ca_file = "";
  } else {
    $logger->error("trusted_certs '$trusted_certs' is not a file or directory");
    return undef;
  }

  my $sock = new IO::Socket::SSL( 
    PeerAddr => $host,
    PeerPort => $port,
    Proto    => 'tcp',
    SSL_use_cert => 1,
    SSL_verify_mode => 0x07,
    SSL_key_file => $key,
    SSL_cert_file => $cert,
    SSL_ca_path => $ca_dir,
    SSL_ca_file => $ca_file,
    SSL_passwd_cb => sub { return $passphrase }
  );

  if ( ! defined $sock ) {
    $logger->error( "Unable to create Inca::IO socket: $!: " .
                    IO::Socket::SSL::errstr() );
  } 

  return $sock;
}

#-----------------------------------------------------------------------------#

=head2 tempfile( $prefix, $dir )

Open a temporary file for writing.  If it is not possible to create a
temporary file in $dir, /tmp will be tried.

=over 2

B<Arguments>:

=over 7

=item prefix

A string that contains the prefix of the file name to use.

=item dir

An optional string containing the name of the directory to place the temporary
file.  If undefined, /tmp will be used

=back

B<Returns>:

A filehandle and a string containing the name of the open file.

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub tempfile {
  my $class = shift;
  my $prefix = shift;
  my $dir = shift;

  my ($fh, $filename);
  my @dirs = ( File::Spec->tmpdir() );
  unshift( @dirs, $dir ) if defined $dir;
  for my $dir ( @dirs ) {
    for ( my $i = 0; $i < $MAX_TRIES; $i++ ) {
      $filename = File::Spec->catfile( $dir, $prefix . ".$$" . ".$i" );
      next if -f $filename;
      if ( open( $fh, ">$filename" ) ) {
        return ($fh, $filename);
      }
    }
  }
  return ( undef, undef );
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _readSSLCreds( %creds )
#
# Extract a certificate, key, passphrase, and trusted certificate directory from the
# hash 'creds'.  The keys it will check for are:
#
# cert     A string containing the path to the certificate file
#
# key      A string containing the path to the private key file
#  
# passphrase A string containing the passphrase for the private key file
#  
# trusted  A string containing the path to the trusted CA dir or file
#
# If one of these fields is missing, we will croak so calling method
# may want to encapsulate this in an 'eval'.
#
# Returns: 
#
# An array of strings containing the paths to the certificate, key, passphrase, and
# trusted certificate dir/file.
 
=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _readSSLCreds {
  my $class = shift;
  my %creds = @_;
  my $logger = Inca::Logger->get_logger( $class );

  my @ssl_creds;
  for my $type ( qw(cert key passphrase trusted) ) {
    if ( exists $creds{$type} ) {
      push( @ssl_creds, $creds{$type} );
    } else {
      $logger->logcroak( 
        "Requested SSL IO stream but missing '$type' for authentication" 
      );
    }
  }
  return @ssl_creds;
}

#-----------------------------------------------------------------------------#
# _startClient( $port, $ca, $trusted )
#
# Try to connect to SSL server on port using the CA certificate $ca and
# trusted cert dir
#
# Arguments:
#
# port     An integer containing the port to start the server on
#
# ca       A string containing the name of the CA in t/certs to use
#  
# trusted  A string containing the path to the trusted CA dir or file
#
# Returns: 
#
# A new IO::Socket::SSL object
#-----------------------------------------------------------------------------#
sub _startClient {
  my $class = shift;
  my $port = shift;
  my $ca = shift;
  my $trusted = shift;
  my $passphrase = shift;

  if ( defined $passphrase ) {
    Inca::IO->createSSLStream( 
      'localhost', $port, 't/certs/client_' . $ca . 'cert.pem',
      't/certs/client_' . $ca . 'key.pem', $passphrase, $trusted 
    );
  } else {
    Inca::IO->createSSLStream( 
      'localhost', $port, 't/certs/client_' . $ca . 'cert.pem',
      't/certs/client_' . $ca . 'keynoenc.pem', undef, $trusted 
    );
  }
}

#-----------------------------------------------------------------------------#
# _startServer( $port, $ca, $trusted, trust_type => [file, dir])
#
# Start SSL server on port using the CA certificate $ca and trusted
# cert dir.  Used only for testing.
#
# Arguments:
#
# port     An integer containing the port to start the server on
#
# ca       A string containing the name of the CA in t/certs to use
#  
# trusted  A string containing the path to the trusted CA dir or file
#
# Options:
#
# trust_type  Indicates whether trusted is a file or dir [default: dir]
#
# Returns: 
#
# A new IO::Socket::SSL object
#-----------------------------------------------------------------------------#
sub _startServer {
  my $class = shift;
  my $port = shift;
  my $ca = shift;
  my $trusted = shift;
  my %options = @_;
 
  my ( $ca_file, $ca_dir );
  if ( exists $options{trust_type} and $options{trust_type} eq "file" ) {
    $ca_dir = "";
    $ca_file = $trusted;
  } else {
    $ca_file = '';
    $ca_dir = $trusted; 
  }

  my $server = new IO::Socket::SSL(
    LocalPort => $port,
    Proto     => 'tcp',
    Listen    => 5,
    Reuse     => 1,
    SSL_use_cert => 1,
    SSL_verify_mode => 0x07,
    SSL_key_file => 't/certs/server_' . $ca . 'keynoenc.pem',
    SSL_cert_file => 't/certs/server_' . $ca . 'cert.pem',
    SSL_ca_file => $ca_file,
    SSL_ca_path => $ca_dir
  );
  return $server;
}

#=============================================================================#
# Return true module load status to true
#=============================================================================#
1;

#=============================================================================#
# The end
#=============================================================================#

__END__

=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=cut

