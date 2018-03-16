package Inca::Net::Protocol::Statement;

################################################################################

=head1 NAME

Inca::Net::Protocol::Statement - Convenience object for creating Inca protocol statements

=head1 SYNOPSIS

=for example begin

  use Inca::Net::Protocol::Statement;
  my $statement = new Inca::Net::Protocol::Statement();
  $statement->setCmd( "PING" );
  $statement->setData( "me" );
  print $statement->toString();

=for example end

=for example_testing
  use Socket qw(:crlf);
  is( $_STDOUT_, "PING me$CRLF", "ping example" );

=head1 DESCRIPTION

This class assists in sending/receiving and parsing the Inca protocol. The
Inca protocol is a sequence of statements where a statement is the most basic
part of the Inca Protocol. It has the following patterm:  CMD SP DATA CRLF.  

=over 6

=item CMD  

A text string indicating an action or description of the data that follows.

=item SP   

A space.

=item DATA 

The crux of the message containing any stream of characters except CRLF.

=item CRLF

A carriage return followed by a line feed indicating the end of the statement.

=back 

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
use Inca::Validate qw(:all);
use Inca::Constants qw(:all);

# Perl standard
use Carp;
use Socket qw(:crlf);

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';
our $PROTOCOL_VERSION = 1;
my $CMDS = { start => "START", error => "ERROR", ok => "OK" };
my $CODES = { ok => 100 };

# arguments
my $SELF_EXPR = { isa => "Inca::Net::Protocol::Statement" };
my $CMD_EXPR = { regex => qr/\w+/, optional => 1 };
my $DATA_EXPR = { type => SCALAR, optional => 1 };
my $STMT_EXPR = { type => SCALAR, optional => 1 };
my $STMT_REGEX = qr/^(\S+)(?: (.*))?$CRLF$/s;

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( %Options )

Class constructor which returns a new Inca::Net::Protocol::Statement object.
The constructor may be called with any of the following attributes.

=over 2 

B<Options>:

=over 7

=item cmd

Set the CMD string of the statement.

=item data

Set the DATA of the statement.

=item stmt

Parse an existing Inca statement.

=back

=back

=begin testing

  use Inca::Net::Protocol::Statement;
  use Test::Exception;
  lives_ok { new Inca::Net::Protocol::Statement() } 'object created';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  # set up defaults
  $self->{cmd} = undef;
  $self->{data} = undef;
  $self->{stmt} = undef;

  # read options
  my %options = validate( @_, { cmd => $CMD_EXPR, 
                                data => $DATA_EXPR, 
                                stmt => $STMT_EXPR } );

  $self->setCmd( $options{cmd} ) if exists $options{cmd};
  $self->setData( $options{data} ) if exists $options{data}; 
  $self->setStatement( $options{stmt} ) if exists $options{stmt}; 

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 getCmd( )

Returns the CMD string of the statement.

=over 2

B<Returns>:

A text string indicating an action or description of the data that follows.

=back

=cut
#-----------------------------------------------------------------------------#
sub getCmd {
  my ( $self ) = validate_pos( @_, $SELF_EXPR );

  return $self->{cmd};
}

#-----------------------------------------------------------------------------#

=head2 getData( )

Returns the DATA part of the statement.

=over 2

B<Returns>:

The crux of the message containing any stream of characters except CRLF.

=back

=cut
#-----------------------------------------------------------------------------#
sub getData {
  my ( $self ) = validate_pos( @_, $SELF_EXPR );

  return $self->{data};
}

#-----------------------------------------------------------------------------#

=head2 getErrorStatement( $error )

Creates a new ERROR message with the given error message.  An error message
will serve to close the connection between client and server.

=over 2

B<Arguments>:

=over 7

=item error

A string containing an error message to report to the other side of the
connection.

=back

B<Returns>:

Returns a new statement representing: ERROR SP Error Message CRLF

=back

=begin testing

  use Inca::Net::Protocol::Statement;
  use Socket qw(:crlf);

  my $statement = new Inca::Net::Protocol::Statement();
  is( $statement->getErrorStatement( "to err is human" )->toString(), 
      "ERROR to err is human$CRLF", "getErrorStatement()" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getErrorStatement {
  my ( $self, $error ) = validate_pos( @_, $SELF_EXPR, SCALAR );

  my $statement = new Inca::Net::Protocol::Statement();
  $statement->setCmd( $CMDS->{error} );
  $statement->setData( $error );
  return $statement;
}

#-----------------------------------------------------------------------------#

=head2 getOkStatement( data => $data )

Creates a new OK message with the supplied $data.  If $data is not specified,
100 is used.

=over 2

B<Options>:

=over 7

=item data

An string representing information to send with the OK statement [default: 100]

=back

B<Returns>:

Returns a new statement representing: OK SP DATA CRLF

=back

=begin testing

  use Inca::Net::Protocol::Statement;
  use Socket qw(:crlf);

  is( Inca::Net::Protocol::Statement->getOkStatement()->toString(), 
      "OK$CRLF", "getOkStatement()" );
  is( Inca::Net::Protocol::Statement->getOkStatement( data=>200 )->toString(), 
      "OK 200$CRLF", "getOkStatement(data=>200)" );
  is( Inca::Net::Protocol::Statement->getOkStatement( data=>"str")->toString(), 
      "OK str$CRLF", "getOkStatement(data=>str)" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getOkStatement {
  my @self_arg = (shift);
  my ( $self ) = validate_pos( @self_arg, $SELF_EXPR );
  my %options = validate( @_, { data => $SCALAR_PARAM_OPT } );

  my $statement = new Inca::Net::Protocol::Statement();
  $statement->setCmd( $CMDS->{ok} );
  $statement->setData( $options{data} ) if defined $options{data};
  return $statement;
}

#-----------------------------------------------------------------------------#

=head2 getStartStatement( )

Returns a new Start message with the current Inca protocol version.

=over 2

B<Returns>:

Returns a new statement object representing: START SP Version CRLF

=back

=begin testing

  use Inca::Net::Protocol::Statement;
  use Socket qw(:crlf);

  my $statement = new Inca::Net::Protocol::Statement();
  is( $statement->getStartStatement()->toString(), "START 1$CRLF", 
      "getStartStatement()" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getStartStatement {
  my ( $self ) = validate_pos( @_, $SELF_EXPR );

  my $statement = new Inca::Net::Protocol::Statement();
  $statement->setCmd( $CMDS->{start} );
  $statement->setData( $PROTOCOL_VERSION );
  return $statement;
}

#-----------------------------------------------------------------------------#

=head2 getStatement( )

Returns a string containing the Inca statement.  Equivalent to toString()

=over 2

B<Returns>:

A complete string representing a Statement in the Inca protocol.

=back

=cut
#-----------------------------------------------------------------------------#
sub getStatement {
  my ( $self ) = validate_pos( @_, $SELF_EXPR );

  return $self->toString();
}

#-----------------------------------------------------------------------------#

=head2 setCmd( $cmd )

Set the CMD string of the statement.

=over 2

B<Arguments>:

=over 13

=item cmd

A text string indicating an action or description of the data that follows.

=back

=back

=begin testing

  use Inca::Net::Protocol::Statement;
  use Test::Exception;

  my $statement = new Inca::Net::Protocol::Statement( cmd => "PING" );
  is( $statement->getCmd(), "PING", "set CMD works from constructor" );
  $statement->setCmd( "START" );
  is( $statement->getCmd(), "START", "set/getCmd works" );
  
=end testing

=cut
#-----------------------------------------------------------------------------#
sub setCmd {
  my ( $self, $cmd ) = validate_pos( @_, $SELF_EXPR, $CMD_EXPR );

  $self->{cmd} = $cmd;
}

#-----------------------------------------------------------------------------#

=head2 setData( $cmd )

Set the DATA string of the statement.

=over 2

B<Arguments>:

=over 13

=item data

The crux of the message containing any stream of characters except CRLF.

=back

=back

=begin testing

  use Inca::Net::Protocol::Statement;
  use Test::Exception;

  my $statement = new Inca::Net::Protocol::Statement( data => "blech" );
  is( $statement->getData(), "blech", "set DATA works from constructor" );
  $statement->setData( "yuck" );
  is( $statement->getData(), "yuck", "set/getData works" );
  
=end testing

=cut
#-----------------------------------------------------------------------------#
sub setData {
  my ( $self, $data ) = validate_pos( @_, $SELF_EXPR, $DATA_EXPR );

  $self->{data} = $data;
}

#-----------------------------------------------------------------------------#

=head2 setStatement( $stmt )

Parse an existing Inca statement into the existing object.

=over 2

B<Arguments>:

=over 13

=item stmt

A string containing the Inca statement in the format:  CMD SP DATA CRLF.

=back

=back

=begin testing

  use Inca::Net::Protocol::Statement;
  use Test::Exception;

  my $statement = new Inca::Net::Protocol::Statement( 
     cmd => "tree",
     data => "is in a forrest" 
  );
  my $statement2 = new Inca::Net::Protocol::Statement( 
     cmd => "cactus",
     data => "is in a desert" 
  );
  my $statement3 = new Inca::Net::Protocol::Statement( 
     cmd => "multiline",
     data => "it was the winter of discontent\n it was the something\nand something else" 
  );
  my $statement4 = new Inca::Net::Protocol::Statement( 
     cmd => "cactus"
  );
  my $echo = new Inca::Net::Protocol::Statement(stmt => $statement->toString());
  is( $echo->getData(), $statement->getData(), 
      "setStatement works from constructor(1)" );
  is( $echo->getData(), $statement->getData(), 
      "setStatement works from constructor(2)" );
  $echo->setStatement( $statement2->getStatement() );
  is( $echo->getCmd(), $statement2->getCmd(), 
      "set/getStatement works (1)" );
  is( $echo->getData(), $statement2->getData(), 
      "setStatement works from constructor(2)" );
  $echo->setStatement( $statement3->getStatement() );
  is( $echo->getCmd(), $statement3->getCmd(), 
      "set/getStatement works for multiline data (cmd)" );
  is( $echo->getData(), $statement3->getData(), 
      "set/getStatement works for multiline data (data)" );
  $echo->setStatement( $statement4->getStatement() );
  is( $echo->getCmd(), $statement4->getCmd(), 
      "set/getStatement works for cmd only" );
  is( $echo->getData(), $statement4->getData(), 
      "set/getStatement works for cmd only (data undef)" );
  
=end testing

=cut
#-----------------------------------------------------------------------------#
sub setStatement {
  my ( $self, $stmt ) = validate_pos( @_, $SELF_EXPR, $STMT_EXPR );

  ( $self->{cmd}, $self->{data} ) = $stmt =~ $STMT_REGEX;
}

#-----------------------------------------------------------------------------#

=head2 toString( )

Returns completed statement as a string.

=over 2

B<Returns>:

A complete string representing a Statement in the Inca protocol.

=back

=begin testing

  use Inca::Net::Protocol::Statement;
  use Socket qw(:crlf);

  my $statement = new Inca::Net::Protocol::Statement();
  $statement->setCmd( "PING" );
  $statement->setData( "testdata" );
  is( $statement->toString(), "PING testdata$CRLF",
      "toString works with complete statement" );

  my $bad_statement = new Inca::Net::Protocol::Statement();
  $bad_statement->setCmd( "PING" );
  is( $bad_statement->toString(), "PING$CRLF", 
      "toString works for cmd only" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub toString {
  my ( $self ) = validate_pos( @_, $SELF_EXPR );

  my $statement = "";
  $statement .= $self->getCmd();
  $statement .= " " if defined $self->getData();
  $statement .= $self->getData() if defined $self->getData();
  $statement .= $CRLF;

  return $statement;
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=cut
