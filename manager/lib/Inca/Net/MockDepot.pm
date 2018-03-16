package Inca::Net::MockDepot;

################################################################################

=head1 NAME

Inca::Net::MockDepot - Emulates an Inca depot (for testing purposes only)

=head1 SYNOPSIS

=for example begin

  use Inca::Net::MockDepot;
  my $depot = new Inca::Net::MockDepot();
  my $numReports = $depot->readReportsFromSSL(
    4, 8434, "ca1", "t/certs/trusted"
  );
  $numReports = $depot->readReportsFromFile("./depot.tmp.$$", 0)

=for example end

=head1 DESCRIPTION

Emulates the actions of a depot so we can test out the functionality of
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

# Inca
use Inca::Constants qw(:all);
use Inca::IO;
use Inca::Logger;
use Inca::Net::Client;
use Inca::Net::Protocol::Statement;
use Inca::Validate qw(:all); 

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ = { isa => "Inca::Net::MockDepot"};

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( $port, $ca, $trusted, trust_type => [file, dir] )

Class constructor which returns a new Inca::Net::MockDepot object.  

=begin testing

  use Inca::Net::MockDepot;
  use Test::Exception;
  lives_ok { new Inca::Net::MockDepot() } 'object created';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  $self->{logger} = Inca::Logger->get_logger( $class );

  $self->{reports} = [];
  $self->{lastTargetResource} = undef;

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 getNumReports( )

Return the number of reports successfully received by the mock depot.

=over 2

B<Returns>:

An integer containing the number of reports successfully received by the mock
depot.

=back

=cut

#-----------------------------------------------------------------------------#
sub getNumReports {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return scalar( @{$self->{reports}} );
}

#-----------------------------------------------------------------------------#

=head2 getReports( )

Return an array of strings containing reports read by the mock depot.

=over 2

B<Returns>:

An array of strings containing the reports successfully received by the mock
depot.

=back

=cut

#-----------------------------------------------------------------------------#
sub getReports {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return @{$self->{reports}}; 
}

#-----------------------------------------------------------------------------#

=head2 readReportsFromFile( $filename )

Read a report from file and clean the file from disk.

=over 2

B<Returns>:

True upon successful read and false otherwise.

=back

=cut

#-----------------------------------------------------------------------------#
sub readReportsFromFile {
  my ( $self, $filename, $delay ) = validate_pos( 
    @_, $SELF_PARAM_REQ, SCALAR, $POSITIVE_INTEGER_PARAM_REQ 
  );

  if ( $delay > 0 ) {
    my $secs_slept = 0;
    while( $secs_slept < $delay ) {
      $secs_slept += sleep($delay - $secs_slept + 1);
    }
  }

  open( FD, "<$filename" ) || return 0;  
  my ($content, $line);
  my $numReportsRead = 0;
  while ( ($line=<FD>) ) {  
    if ( $line =~ /START/ ) {
      $content = "";
    }
    $content .= $line;
    if ( $line =~ /memory_mb/ ) {
      my $result = $self->_verifyReportContent( $content );
      $numReportsRead++ if ( ! defined $result ); 
    }
  }
  close( FD );
  unlink $filename;
  return $numReportsRead;
}

#-----------------------------------------------------------------------------#

=head2 readReportsFromSSL( $numReports, $port, $ca, $trusted, trust_type => [file, dir] )

Starts SSL server on $port using the CA certificate $ca and trusted
cert dir and accepts $numReports from clients. 

=over 2

B<Arguments>:

=over 13

=item numReports

The number of connections the mock depot should accept

=item port

An integer representing the port number the mock depot should listen on.

=item ca       

A string containing the name of the CA in t/certs to use
  
=item trusted  

A string containing the path to the trusted CA dir or file

=back

B<Options>:

=over 13

=item trust_type

Indicates whether trusted is a file or dir [default: dir]

=back 

=back

=begin testing

  use Inca::Net::MockDepot;
  use Test::Exception;

  my $depot = new Inca::Net::MockDepot();
  $depot->readReportsFromSSL( 0, 8080, "ca1", "etc/trusted");

=end testing

=cut
#-----------------------------------------------------------------------------#
sub readReportsFromSSL {
  my $self = shift;

  my $numReports = shift;
  my $server = Inca::IO->_startServer( @_ );
  $self->{logger}->logdie("startServer return undefined") if ! defined $server;

  my $numReportsRead = 0;
  for ( my $i = 0; $i < $numReports; $i++ ) {
    $self->{logger}->debug( "Mock depot waiting to receive report $i" );
    my $server_sock = $server->accept();
    $self->{logger}->logdie("accept return undefined") if ! defined $server_sock;
    #next if ! defined $server_sock;
    my $conn = Inca::Net::Client->_connect( $server_sock );
    $self->{logger}->debug( "Mock depot received connection" );
    my $startFound = $conn->hasReadStatement( 
      "START",
      $Inca::Net::Protocol::Statement::PROTOCOL_VERSION 
    );
    if ( $startFound ) {
      $conn->writeStatement( 
        "OK", 
        $Inca::Net::Protocol::Statement::PROTOCOL_VERSION
      );
    } else {
      $conn->writeStatement( "ERROR", "No start statement found" );
    }
    my $stmt = $conn->readStatement(); 
    $self->{logger}->debug( "Received statement" );
    if ( $stmt->getCmd() eq "REPORT" ) {
      if ( $stmt->getData() !~ /\S+\n.+\n\S*$/ ) {
        $conn->writeStatement( "ERROR", "id or context not found" );
      } else {
        my @reportId = split( /\n/,  $stmt->getData() );
        $self->{logger}->debug( "received " . scalar(@reportId) . 
                                " parts of data" );
        $self->{lastTargetResource} = $reportId[2];
      }
      my ($content, $line);
      while( $line = <$server_sock> ) {
        $content .= $line;
        last if $content =~ /memory_mb/;
      }
      $self->{logger}->debug( "Received report $content" );
      my $result = $self->_verifyReportContent( $content );
      if ( defined $result ) {
        $conn->writeStatement( "ERROR", $result );
      } else {
        $numReportsRead++;
        $conn->close();
      }
    } else {
      $self->{logger}->error( 
        "Received uknown statement " . $stmt->getStatement() 
      );
    }
  }

  $server->close();

  return $numReportsRead;
}


#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _verifyReportContent( $content )
#
# Given a string containing the read report from the mock depot, verify
# that all components of the message are there.  If an error is found,
# it returns a string containing the error message.  Otherwise, undef is 
# returned upon success.
#
# Arguments:
#
# content  A string containing the report received from the mock depot
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
  $self->{logger}->debug("Received " . scalar(@{$self->{reports}}) ." reports");
  return undef; 
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

No known problems.

=cut
