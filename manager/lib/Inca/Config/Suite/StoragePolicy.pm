package Inca::Config::Suite::StoragePolicy;

################################################################################

=head1 NAME

Inca::Config::Suite::StoragePolicy - Storage policy for a Inca reporter

=head1 SYNOPSIS

=for example begin

  use Inca::Config::Suite::StoragePolicy;
  my $sp = new Inca::Config::Suite::StoragePolicy();
  $sp->addDepots( "cuzco.sdsc.edu:8258", "inca.sdsc.edu:8235" );

=for example end

=head1 DESCRIPTION

A convenience object for handling the storage policy for a reporter.  The 
storage policy is used by the depot to know where and how to store the
reporter data.  Currently, the storage policy is a list of depots that are
interested in the reporter data.

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
use Inca::Constants qw(:params);
use Inca::Validate qw(:all);

# Perl standard
use Carp;

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ = { isa => "Inca::Config::Suite::StoragePolicy" };

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( )

Class constructor which returns a new
Inca::Config::Suite::StoragePolicy object.  

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  

  # initialize
  $self->{depots} = [];

  bless ($self, $class);
  return $self;
}

#-----------------------------------------------------------------------------#

=head2 addDepots( @depot_uris )

Add an arbitrary number of depot uris to the storage policy.  The depot uris
indicate the depot interested in the results of a reporter execution.  E.g.,
addDepots( "cuzco.sdsc.edu:7777" )

=over 2

B<Arguments>:

=over 13

=item depot_uris

Any number of strings containing a uri to a depot [host:port, ...]

=back

=back

=begin testing

  use Inca::Config::Suite::StoragePolicy;
  use Test::Exception;
  my $sp = new Inca::Config::Suite::StoragePolicy();
  lives_ok { $sp->addDepots( "cuzco.sdsc.edu:8234" ) } 'basic addDepots';
  my @depots = qw( cuzco.sdsc.edu:8234 inca.sdsc.edu:8235 inca.sdsc.edu:8236 );
  lives_ok { $sp->addDepots(@depots) } 'addDepots w/ 3 depots';
  dies_ok { $sp->addDepots() } 'addDepots w/ no depots dies';
  dies_ok { $sp->addDepots( "cuzco.sdsc.edu" ) } 'addDepots w/o port dies';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub addDepots {
  my ( $self, @depots ) = 
     validate_pos( @_, $SELF_PARAM_REQ, 
                       $URI_PARAM_REQ, ($URI_PARAM_REQ) x (@_ - 2)
                 );
  push( @{$self->{depots}}, @depots );
}

#-----------------------------------------------------------------------------#

=head2 equals( $sp )

Return true if $sp is equivalent to ourself; false otherwise.

=over 2

B<Arguments>:

=over 5

=item sp  

An object of type Inca::Config::Suite::StoragePolicy

=back

=back

=begin testing

  use Inca::Config::Suite::StoragePolicy;
  use Test::Exception;
  my $sp = new Inca::Config::Suite::StoragePolicy();
  $sp->addDepots( "cuzco.sdsc.edu:8234" );
  my $sp2 = new Inca::Config::Suite::StoragePolicy();
  my @depots = qw( cuzco.sdsc.edu:8234 inca.sdsc.edu:8235 inca.sdsc.edu:8236 );
  $sp2->addDepots(@depots); 
  ok( ! $sp->equals($sp2), "equals returns false when different length" );
  $sp->addDepots( qw(inca.sdsc.edu:8235 inca.sdsc.edu:8236) );
  ok( $sp->equals($sp2), "equals returns true when true" );
  my $sp3 = new Inca::Config::Suite::StoragePolicy();
  $sp3->addDepots(qw(cuzco.sdsc.edu:8234 inca.sdsc.edu:8235 inca.sdsc.edu:823));
  ok( ! $sp->equals($sp3), "equals returns false when different" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub equals {
  my ( $self, $sp ) = validate_pos( @_, $SELF_PARAM_REQ, $SELF_PARAM_REQ );
  return 0 if ( scalar(@{$self->{depots}}) ne scalar(@{$sp->{depots}}) );
  for my $depot ( @{$self->{depots}} ) {
    return 0 if ( ! grep( /^$depot$/, @{$sp->{depots}}) );
  }
  return 1;
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

No known problems.

=cut
