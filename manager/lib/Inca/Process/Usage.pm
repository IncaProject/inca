package Inca::Process::Usage;

################################################################################

=head1 NAME

Inca::Process::Usage - Data object to store the system usage of a process

=head1 SYNOPSIS

=for example begin

  use Inca::Process::Usage;
  my $usage = new Inca::Process::Usage();
  $usage->setMemory( 100 );
  $usage->setCpuTime( 2 );
  $usage->setWallClockTime( 5 );

=for example end

=for example_testing
  is( $usage->getMemory(), 100, 'getMemory from example' );
  is( $usage->getCpuTime(), 2, 'getCpuTime from example' );
  is( $usage->getWallClockTime(), 5, 'getWallClockTime from example' );

=head1 DESCRIPTION

Describes the system usage of a process in terms of cpu usage, memory usage,
and wall clock time.  This object is used by the Inca::Process::Profiler
module to store monitoring data for a process while it is executing.

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

my $SELF_PARAM_REQ = { isa => __PACKAGE__ };

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( )

Class constructor which returns a new Inca::Process::Usage object.  

=begin testing

  use Inca::Process::Usage;
  use Test::Exception;
  lives_ok { new Inca::Process::Usage() } 'object created';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};

  bless ($self, $class);

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 getCpuTime( )

Get the CPU time used by a process in seconds.

=over 2

B<Returns>:

An integer representing the number of CPU seconds used by the process.

=back

=cut
#-----------------------------------------------------------------------------#
sub getCpuTime {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{cpu_secs}; 
}

#-----------------------------------------------------------------------------#

=head2 getMemory( )

Get the memory usage of a process in megabytes (MB).

=over 2

B<Returns>:

An integer representing the number of megabytes of memory used by the process.

=back

=cut
#-----------------------------------------------------------------------------#
sub getMemory {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{memory_mb}; 
}

#-----------------------------------------------------------------------------#

=head2 getWallClockTime( )

Get the wall clock (or elapsed time) for a process in seconds.

=over 2

B<Returns>:

An integer representing the wall clock (or elapsed time) for a process
in seconds.

=back

=cut
#-----------------------------------------------------------------------------#
sub getWallClockTime {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{wall_secs}; 
}

#-----------------------------------------------------------------------------#

=head2 greaterThan( $usage )

Compare the calling object to the passed Inca::Process::Usage object and 
return true if the calling object is greater than the passed object.

=over 2

B<Returns>:

A true value if the calling object is greater than the passed object;
otherwise returns true.

=back

=begin testing

  my $a = new Inca::Process::Usage();
  $a->setCpuTime(5);
  $a->setWallClockTime(10);
  $a->setMemory(10);
  my $b = new Inca::Process::Usage();
  $b->setCpuTime(5);
  $b->setWallClockTime(12);
  $b->setMemory(10);
  ok( $b->greaterThan($a), "basic - greaterThan with true value" );
  ok( ! $a->greaterThan($b), "basic - greaterThan with false value" );

  my $c = new Inca::Process::Usage();
  $c->setCpuTime(.51);
  $c->setWallClockTime(1);
  $c->setMemory(46.16796875);
  my $d = new Inca::Process::Usage();
  $d->setCpuTime(4.84);
  $d->setWallClockTime(13);
  $d->setMemory(64);
  ok( ! $c->greaterThan($d), "real - greaterThan with false value" );
  ok( $d->greaterThan($c), "real - greaterThan with true value" );

  my $e = new Inca::Process::Usage();
  $e->setMemory(10);
  my $f = new Inca::Process::Usage();
  $f->setMemory(20);
  ok( ! $e->greaterThan($f), "partial - greaterThan with false value" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub greaterThan {
  my ( $self, $usage ) =
     validate_pos( @_, $SELF_PARAM_REQ, $SELF_PARAM_REQ );

  # first compare cpu_secs
  if ( $self->hasCpuTime() && $usage->hasCpuTime() &&
       ($self->getCpuTime() > $usage->getCpuTime()) ) {
    return 1;
  }
  # then compare memory
  if ( $self->hasMemory() && $usage->hasMemory() &&
       ($self->getMemory() > $usage->getMemory()) ) {
    return 1;
  }
  # finally, wait time
  if ( $self->hasWallClockTime() && $usage->hasWallClockTime() &&
       ($self->getWallClockTime() > $usage->getWallClockTime()) ) {
    return 1;
  }
  return 0;
}

#-----------------------------------------------------------------------------#

=head2 hasCpuTime( )

Check to see if a value for CPU time has been set.  

=over 2

B<Returns>:

Returns true if a value for CPU time has been set and is positive; otherwise
returns false.

=back

=begin testing

  use Inca::Process::Usage;

  my $usage = new Inca::Process::Usage();
  ok( ! $usage->hasCpuTime(), 'hasCpuTime - false value' );
  $usage->setCpuTime( -1 );
  ok( ! $usage->hasCpuTime(), 'hasCpuTime - false for neg value' );
  $usage->setCpuTime( 10 );
  ok( $usage->hasCpuTime(), 'hasCpuTime - true value' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasCpuTime {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return ( exists $self->{cpu_secs} && defined $self->{cpu_secs} &&
           $self->{cpu_secs} >= 0 );
}

#-----------------------------------------------------------------------------#

=head2 hasMemory( )

Check to see if a value for memory has been set.  

=over 2

B<Returns>:

Returns true if a value for memory has been set and is positive; otherwise
returns false.

=back

=begin testing

  use Inca::Process::Usage;

  my $usage = new Inca::Process::Usage();
  ok( ! $usage->hasMemory(), 'hasMemory - false value' );
  $usage->setMemory( -1 );
  ok( ! $usage->hasMemory(), 'hasMemory - false for neg value' );
  $usage->setMemory( 10 );
  ok( $usage->hasMemory(), 'hasMemory - true value' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasMemory {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return ( exists $self->{memory_mb} && defined $self->{memory_mb} &&
           $self->{memory_mb} >= 0 );
}

#-----------------------------------------------------------------------------#

=head2 hasWallClockTime( )

Check to see if a value for wall clock time has been set.  

=over 2

B<Returns>:

Returns true if a value for wall clock time has been set and is positive;
otherwise returns false.

=back

=begin testing

  use Inca::Process::Usage;

  my $usage = new Inca::Process::Usage();
  ok( ! $usage->hasWallClockTime(), 'hasWallClockTime - false value' );
  $usage->setWallClockTime( -1 );
  ok( ! $usage->hasWallClockTime(), 'hasWallClockTime - false for neg value' );
  $usage->setWallClockTime( 10 );
  ok( $usage->hasWallClockTime(), 'hasWallClockTime - true value' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasWallClockTime {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return ( exists $self->{wall_secs} && defined $self->{wall_secs} &&
           $self->{wall_secs} >= 0 );
}

#-----------------------------------------------------------------------------#

=head2 limitsExceededString( $usage )

Compare the calling object to the passed Inca::Process::Usage object and 
return a string representation of whether the calling object is greater than the
passed object (e.g., 'wall clock time of X is greater than limit Y').

=over 2

B<Arguments>:

=over 13

=item usage

An object of type Inca::Process::Usage.

=back

B<Returns>:

A string representation of whether the calling object is greater than
the limits represented in the passed object.

=back

=begin testing

  my $a = new Inca::Process::Usage();
  $a->setCpuTime(5);
  $a->setWallClockTime(10);
  $a->setMemory(10);
  my $b = new Inca::Process::Usage();
  $b->setCpuTime(5);
  $b->setWallClockTime(12);
  $b->setMemory(10);
  is( $b->limitsExceededString($a), 
      "wall clock time limit exceeded 10 secs",
      "basic - greaterThanStr with wallclocktime" );

  my $c = new Inca::Process::Usage();
  $c->setCpuTime(.51);
  $c->setWallClockTime(1);
  $c->setMemory(46.16796875);
  my $d = new Inca::Process::Usage();
  $d->setCpuTime(4.84);
  $d->setWallClockTime(13);
  $d->setMemory(64);
  is( $d->limitsExceededString($c), 
      "cpu time limit exceeded 0.51 secs",
      "real - greaterThan with true value" );

  my $e = new Inca::Process::Usage();
  $e->setMemory(10);
  my $f = new Inca::Process::Usage();
  $f->setMemory(20);
  is( $f->limitsExceededString($e), 
      "memory limit exceeded 10 MB",
      "partial - greaterThan with false value" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub limitsExceededString {
  my ( $self, $usage ) =
     validate_pos( @_, $SELF_PARAM_REQ, $SELF_PARAM_REQ );

  # first compare cpu_secs
  if ( $self->hasCpuTime() && $usage->hasCpuTime() &&
       $self->getCpuTime() > $usage->getCpuTime() ) {
    return "cpu time limit exceeded " . $usage->getCpuTime() . " secs";
  }
  # then compare memory
  if ( $self->hasMemory() && $usage->hasMemory() &&
       ($self->getMemory() > $usage->getMemory()) ) {
    return "memory limit exceeded " . $usage->getMemory() . " MB";
  }
  # finally, wait time
  if ( $self->hasWallClockTime() && $usage->hasWallClockTime() &&
       ($self->getWallClockTime() > $usage->getWallClockTime()) ) {
    return "wall clock time limit exceeded " . 
           $usage->getWallClockTime() . " secs";
  }
  return undef;
}

#-----------------------------------------------------------------------------#

=head2 setCpuTime( $seconds )

Set the CPU time used by a process in seconds.

=over 2

B<Arguments>:

=over 13

=item seconds

An integer representing the number of CPU seconds used by the process.

=back

=back

=begin testing

  use Inca::Process::Usage;
  use Test::Exception;
  my $usage = new Inca::Process::Usage();
  $usage->setCpuTime( 5 );
  is( $usage->getCpuTime(), 5, 'set/getCpuTime functions' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setCpuTime {
  my ( $self, $seconds ) = 
     validate_pos( @_, $SELF_PARAM_REQ, $POSITIVE_INTEGER_PARAM_REQ );

  $self->{cpu_secs} = $seconds;
}

#-----------------------------------------------------------------------------#

=head2 setMemory( $megabytes )

Set the memory usage of a process in megabytes (MB).

=over 2

B<Arguments>:

=over 13

=item megabytes

An integer representing the number of megabytes of memory used by the process.

=back

=back

=begin testing

  use Inca::Process::Usage;
  use Test::Exception;
  my $usage = new Inca::Process::Usage();
  $usage->setMemory( 100 );
  is( $usage->getMemory(), 100, 'set/getMemory functions' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setMemory {
  my ( $self, $megabytes ) = 
     validate_pos( @_, $SELF_PARAM_REQ, $POSITIVE_INTEGER_PARAM_REQ );

  $self->{memory_mb} = $megabytes;
}

#-----------------------------------------------------------------------------#

=head2 setWallClockTime( $seconds )

Set the wall clock (or elapsed time) for a process in seconds.

=over 2

B<Arguments>:

=over 13

=item seconds

An integer representing the wall clock (or elapsed time) for a process
in seconds.

=back

=back

=begin testing

  use Inca::Process::Usage;
  use Test::Exception;
  my $usage = new Inca::Process::Usage();
  $usage->setWallClockTime( 10 );
  is( $usage->getWallClockTime(), 10, 'set/getWallClockTime functions' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setWallClockTime {
  my ( $self, $seconds ) = 
     validate_pos( @_, $SELF_PARAM_REQ, $POSITIVE_INTEGER_PARAM_REQ );

  $self->{wall_secs} = $seconds;
}

#-----------------------------------------------------------------------------#

=head2 zeroValues( )

Set all values to 0.

=begin testing

  use Inca::Process::Usage;

  my $usage = new Inca::Process::Usage();
  $usage->zeroValues();
  is( $usage->getWallClockTime(), 0, 'zeroValues - wall clock' );
  is( $usage->getCpuTime(), 0, 'zeroValues - CPU' );
  is( $usage->getMemory(), 0, 'zeroValues - memory' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub zeroValues {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  $self->setMemory( 0 );
  $self->setCpuTime( 0 );
  $self->setWallClockTime( 0 );
}

#-----------------------------------------------------------------------------#

=head2 toString( )

Return the contents of the object as name=value pairs on separate lines.

=over 2

B<Returns>:

A string containing the contents of the object in name=value pairs.

=back

=begin testing

  use Inca::Process::Usage;

  my $printed = <<TEST;
cpu_secs=5
memory_mb=10
TEST
  my $usage = new Inca::Process::Usage();
  $usage->setCpuTime(5);
  $usage->setMemory(10);
  is( $usage->toString(), $printed, "toString - partial displays ok" );

  $printed = <<TEST;
cpu_secs=5
wall_secs=10
memory_mb=10
TEST
  $usage->setWallClockTime(10);
  is( $usage->toString(), $printed, "toString - full object" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub toString {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  my $string="";
  if ( $self->hasCpuTime() ) {
    $string .= "cpu_secs=" . $self->getCpuTime() . "\n";
  }
  if ( $self->hasWallClockTime() ) {
    $string .= "wall_secs=" . $self->getWallClockTime() . "\n";
  }
  if ( $self->hasMemory() ) {
    $string .= "memory_mb=" . $self->getMemory() . "\n";
  }
  return $string;
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

No known problems.

=cut
