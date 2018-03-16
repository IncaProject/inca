package Inca::Net::MockAgent;

################################################################################

=head1 NAME

Inca::Net::MockAgent - Emulates an Inca agent (for testing purposes only)

=head1 SYNOPSIS

=for example begin

  use Inca::Net::MockAgent;
  my $agent = new Inca::Net::MockAgent( 3432, "ca1", "t/certs/trusted" );
  my ($conn, $numStmts, @responses) = $agent->accept(
    "REGISTER" => [ 'OK', 'END'],
  );
  $conn->close();
  $agent->stop();

=for example end

=head1 DESCRIPTION

Emulates the actions of a agent so we can test out the functionality of
the reporter manager.

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
use IO::Socket qw(:crlf);

# Inca
use Inca::Constants qw(:all);
use Inca::IO;
use Inca::Net::Client;
use Inca::Net::Protocol::Statement;
use Inca::Validate qw(:all); 

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ = { isa => "Inca::Net::MockAgent"};
my %STD_COMMANDS = ( "PING (.*)" => ['OK $1'] );

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( $port, $ca, $trusted )

Starts SSL server on $port using the CA certificate $ca and trusted cert dir.  
Returns a new Inca::Net::MockAgent object.  

B<Arguments>:

=over 13

=item port

An integer representing the port number the mock agent should listen on.

=item ca       

A string containing the name of the CA in t/certs to use
  
=item trusted  

A string containing the path to the trusted CA dir or file

=back

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  $self->{logger} = Inca::Logger->get_logger( $class );

  my @serverParams = validate_pos( @_, $PORT_PARAM_REQ, SCALAR, SCALAR );

  $self->{server} = Inca::IO->_startServer( @serverParams ) || 
    die "Cannot start server on port $serverParams[0]";

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 accept( %commands )

Accepts a connection to the mock agent.  %commands is a hash array where the
keys are patterns to expect from the manager and the value is an array of
commands to send in response

=over 2

B<Arguments>:

=over 13

=item commands

A hash array where the keys are patterns of commands to accept from the 
manager and the values are an array of responses to send back.

=back

=back

=cut
#-----------------------------------------------------------------------------#
sub accept {
  my $self = shift;
  my %commands = @_; 

  my @patterns = keys %commands;
  my @responses = values %commands;
  push( @patterns, keys %STD_COMMANDS );
  push( @responses, values %STD_COMMANDS );

  my $server_sock = $self->{server}->accept();
  $self->{logger}->logdie("accept return undefined") if ! defined $server_sock;
  my $conn = Inca::Net::Client->_connect( $server_sock );
  $self->{logger}->debug( "connection accepted" );
  my $startFound = $conn->hasReadStatement( 
    "START", $Inca::Net::Protocol::Statement::PROTOCOL_VERSION 
  );
  if ( $startFound ) {
    $conn->writeStatement( 
      "OK", $Inca::Net::Protocol::Statement::PROTOCOL_VERSION
    );
  } else {
    $conn->writeStatement( "ERROR", "No start statement found" );
  }
  my $numStmtsRead = 0;
  my @clientResponses;
  while ( 1 ) {
    my $stmt = $conn->readStatement(); 
    last if ( ! defined $stmt ); 
    push( @clientResponses, $stmt );
    $numStmtsRead++;
    $self->{logger}->debug( "Statement read " . $stmt->toString() );
    my $i = 0;
    $i++ while defined($patterns[$i]) && $stmt->toString() !~ $patterns[$i];
    my $message;
    if( defined($patterns[$i]) ) {
      my $exit = 0;
      for my $response ( @{$responses[$i]} ) {
        if ( ! defined $response  ) {
          $self->{logger}->debug( "Closing connection" );
          $exit = 1;
          last;
        } elsif ( $response =~ /Inca::/ ) {
          $message = $response;
        } else {
          $response =~ s/"/\\"/g;
          $message = eval("\"" . $response . "\"");
          $self->{logger}->error( "eval croaked $@" ) if $@;
        }
        my $rstmt = new Inca::Net::Protocol::Statement(stmt => $message.$CRLF);
        $conn->writeStatement( $rstmt );
      }
      last if $exit;
    } else {
      $message = "ERROR Unknown message '" . $stmt->toString() . "'";
      my $rstmt = new Inca::Net::Protocol::Statement(stmt => $message.$CRLF);
      $self->{logger}->debug( "Writing statement " . $rstmt->toString() );
      $conn->writeStatement( $rstmt );
    }
  }

  $self->{logger}->debug( "Exitting" );
  return ( $server_sock, $numStmtsRead, @clientResponses );
}

#-----------------------------------------------------------------------------#

=head2 stop( )

Stop mock agent.

=cut
#-----------------------------------------------------------------------------#
sub stop {
  my $self = shift;
  
  if ( defined $self->{server} ) {
    $self->{logger}->debug( "Mock agent closed" );
    $self->{server}->close();
    $self->{server} = undef;
  }
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _verifyReportContent( $content )
#
# Given a string containing the read report from the mock agent, verify
# that all components of the message are there.  If an error is found,
# it returns a string containing the error message.  Otherwise, undef is 
# returned upon success.
#
# Arguments:
#
# content  A string containing the report received from the mock agent
#
# Returns: undef upon success and a string containing an error message if
# false.
#
#-----------------------------------------------------------------------------#
sub _verifyReportContent {
  my ( $self, $content ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  return "STDOUT not found" if ( $content !~ /^STDOUT.*/m );
  return "report start tag not found" if ( $content !~ /^<rep:report .*/m );
  return "report end tag not found" if ( $content !~ /^<\/rep:report>.*/m  );
  return "sysusage not found" if ( $content !~ /^SYSUSAGE cpu_secs=.*/m );
  return "wall_secs not found" if ( $content !~ /^wall_secs=.*/m );
  return "memory_mb not found" if ( $content !~ /^memory_mb=.*/m );
  push( @{$self->{reports}}, $content );
  print "Received " . scalar(@{$self->{reports}}) . " reports\n";
  return undef; 
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

Describe any known problems.

=cut
