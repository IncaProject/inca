package Inca::Net::Client;

################################################################################

=head1 NAME

Inca::Net::Client - Base client library for reading/writing Inca protocol

=head1 SYNOPSIS

=for example begin

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

=for example end

=head1 DESCRIPTION

A base class for creating Inca clients.  Provides convenience methods
for connecting to an Inca server and reading/writing the Inca protocol.

=cut 
################################################################################

#=============================================================================#
# Usage
#=============================================================================#

# pragmas
use strict;
use warnings;
use vars qw($VERSION);

# Perl standard
use Carp;
use Socket qw(:crlf);

# Inca
use Inca::Constants qw(:all);
use Inca::IO;
use Inca::Net::Protocol::Statement;
use Inca::Validate qw(:all);

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ = { isa => __PACKAGE__ };

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( $uri, %Options )

Class constructor which returns a new Inca::Net::Client object.  The
constructor must be called with a $uri to the server.  The constructor
may be called with any of the following attributes.

=over 2

B<Arguments>:

=over 7

=item uri

A string containing the uri of the Inca server in the format of
<scheme>://<path> where

=over 13

=item scheme

The type of URI being represented by the string (either I<file>, I<inca>, or
I<incas>)

=item path

The location of the URI being represented (e.g., localhost:7070)

=back

=item args

A list of follow on arguments for the stream type (e.g., cert, key, and
trusted certificate directory for the SSL connection).

=back

=back

=begin testing

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


=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  # set up stream
  $self->{uri} = $_[0]; # save uri for printing log statements
  $self->{stream} = new Inca::IO( @_ );
  if ( ! defined $self->{stream} ) {
     return undef;
  }
  $self->{logger} = Inca::Logger->get_logger( $class );

  if ( ref($self->{stream}) eq "IO::File" ) {
    $self->writeStatement( 
      "START", 
      $Inca::Net::Protocol::Statement::PROTOCOL_VERSION
    );
  } else {
    $self->writeAndReadStatement( 
      "START", 
      $Inca::Net::Protocol::Statement::PROTOCOL_VERSION, 
      "OK", 
      $Inca::Net::Protocol::Statement::PROTOCOL_VERSION
    );
  }

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 close( )

Close the client connection to the server.

=cut
#-----------------------------------------------------------------------------#
sub close {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  close( $self->{stream} ) if defined $self->{stream};
}

#-----------------------------------------------------------------------------#

=head2 hasReadStatement( $cmdRegex, $dataRegex )

Read an Inca statement from the open stream and compare it to the expected
command and data regular expressions.

=over 2 

B<Arguments>:

=over 10

=item cmdRegex

The expected command regular expression that should be read from the stream.

=item dataRegex

The expected data regular expression that should be read from the stream.

=back

B<Returns>:

Returns true if the read statement matches the expected statement; false
otherwise.

=back

=cut
#-----------------------------------------------------------------------------#
sub hasReadStatement {
  my ( $self, $cmdRegex, $dataRegex ) = validate_pos( @_, 
    $SELF_PARAM_REQ, SCALAR, $SCALAR_PARAM_OPT
  );

  my ($cmd, $data) = $self->readStatement();
  if ( $cmd !~ /$cmdRegex/m ) {
    $self->{logger}->error("Received command '$cmd'; expected '$cmdRegex'");
    return 0;
  }
  if ( defined $data && defined $dataRegex && $data !~ /$dataRegex/m ) {
    $self->{logger}->error("Received data '$data'; expected '$dataRegex'");
    return 0;
  }
  return 1;
}

#-----------------------------------------------------------------------------#

=head2 readStatement( )

Read an Inca statement from the open stream.

=over 2 

B<Returns>:

If called from a scalar context, returns an object of
Inca::Net::Protocol::Statement which contains the content of the read Inca
statement.  If called from an array context, will return an array of 
2 elements: (command, data).

=back

=cut
#-----------------------------------------------------------------------------#
sub readStatement {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  my $line;
  my $statement_text = "";
  my $stream = $self->{stream};
  while ( 1 ) {
    eval { $line = <$stream>; };
    if ( $@ ) {
      $self->{logger}->error( "Error during read: $@" );
      last;
    }
    last if ! defined $line;
    $statement_text .= $line;
    last if ( $line =~ /$CRLF$/ );
  }
  if ( $statement_text eq "" ) {
    return wantarray ? (undef, undef) : undef;
  }
  my $stmt = new Inca::Net::Protocol::Statement( stmt => $statement_text );
  return wantarray ? ($stmt->getCmd(), $stmt->getData()) : $stmt;
}

#-----------------------------------------------------------------------------#

=head2 writeAndReadStatement( $cmd, $data, $expectedCmd, $expectedData )

Writes an Inca statement to the open stream, wait for a response, and
compare the response to the expected command and data.

=over 2

B<Arguments>:

=over 13

=item cmd

A string containing the command to write to the open stream.

=item data

A string containing the data to write to the open stream.

=item expectedCmd

The expected command that should be read from the stream.

=item expectedData

The expected data that should be read from the stream.

=back

=back

=cut
#-----------------------------------------------------------------------------#
sub writeAndReadStatement {
  my ($self, $cmd, $data, $expectedCmd, $expectedData) = validate_pos( 
    @_, $SELF_PARAM_REQ, SCALAR, SCALAR, SCALAR, $SCALAR_PARAM_OPT
  );
  $self->writeStatement( $cmd, $data );
  if ( defined $expectedData ) {
    return $self->hasReadStatement( $expectedCmd, $expectedData );
  } else {
    return $self->hasReadStatement( $expectedCmd );
  }
}

#-----------------------------------------------------------------------------#

=head2 writeError( $msg ) 

Writes an Inca error message statement to the open stream. 

=over 2

B<Arguments>:

=over 5

=item msg 

A string containing the error message to pass back to the server.

=back

=back

=cut
#-----------------------------------------------------------------------------#
sub writeAndLogError {
  my ($self, $msg) = validate_pos(@_, $SELF_PARAM_REQ, SCALAR);

  $self->{logger}->error( $msg );
  $self->writeStatement(
    Inca::Net::Protocol::Statement->getErrorStatement($msg)
  );
}

#-----------------------------------------------------------------------------#

=head2 writeStatement( $stmt ) or writeStatement( $cmd, $data )

Writes an Inca statement to the open stream. 

=over 2

B<Arguments>:

=over 6

=item stmt

An object of Inca::Net::Protocol::Statement that should be written to the
open stream.

=item cmd

A string containing the command to write to the open stream.

=item data

A string containing the data to write to the open stream.

=back

=back

=cut
#-----------------------------------------------------------------------------#
sub writeStatement {
  my ($self, $stmt, $cmd, $data);
  if ( scalar(@_) == 2 ) {
    ( $self, $stmt ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );
  } else {
    ( $self, $cmd, $data ) = validate_pos(@_, $SELF_PARAM_REQ, SCALAR, SCALAR|UNDEF);
    if ( defined $data ) {
      $stmt = new Inca::Net::Protocol::Statement( cmd => $cmd, data => $data );
    } else {
      $stmt = new Inca::Net::Protocol::Statement( cmd => $cmd );
    }
  }
  if ( $stmt->toString =~ /password/i ) {
    $self->{logger}->debug( "Write " . $stmt->getCmd() . " ******" );
  } else {
    $self->{logger}->debug( "Write " . $stmt->toString() );
  }
  print {$self->{stream}} $stmt->toString();
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _connect( $stream )
#
# Create a new object by connecting to an existing stream.  Mostly for
# testing purposes
#
# Arguments:
#
# stream  An open stream to a server or client
#-----------------------------------------------------------------------------#
sub _connect {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  # set up stream
  $self->{stream} = shift;
  $self->{logger} = Inca::Logger->get_logger( $class );

  return $self;
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

No known problems.

=cut
