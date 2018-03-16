package Inca::Reporter::Performance::Benchmark;

################################################################################

=head1 NAME

Inca::Reporter::Performance::Benchmark - Convenience module for 
managing a single benchmark entry

=head1 SYNOPSIS

  use Inca::Reporter::Performance::Benchmark;
  my $benchmark = new Inca::Reporter::Performance::Benchmark();

=head1 DESCRIPTION

Convenience module for the Inca::Reporter::Performance module.  Manages
a single benchmark entry.

=cut 
################################################################################

use strict;
use warnings;
use Carp;

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new

Class constructor which returns a new
Inca::Reporter::Performance::Benchmark object.

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {
    parameters => {},
    statistics => {},
    parameterNames => [],
    statisticNames => []
  };
  bless $self, $class;
  return $self;
}

#-----------------------------------------------------------------------------#

=head2 getParameter($name)

=cut
#-----------------------------------------------------------------------------#
sub getParameter {
  my ($self, $name) = @_;
  my $param = $self->{parameters}->{$name};
  return defined($param) ? ($param->{value}, $param->{units}) : undef;
}

#-----------------------------------------------------------------------------#

=head2 getStatistic($name)

=cut
#-----------------------------------------------------------------------------#
sub getStatistic {
  my ($self, $name) = @_;
  my $stat = $self->{statistics}->{$name};
  return defined($stat) ? ($stat->{value}, $stat->{units}) : undef;
}

#-----------------------------------------------------------------------------#

=head2 parameterNames

=cut
#-----------------------------------------------------------------------------#
sub parameterNames {
  my $self = shift;
  return @{$self->{parameterNames}};
}

#-----------------------------------------------------------------------------#

=head2 setParameter($name, $value, $units) 

Sets a parameter that was used to obtain the benchmark.  $name is a unique
identifier for this parameter and $value its value.  The optional $units
describes a scalar $value (e.g., Gb/s, secs, etc.).

=cut
#-----------------------------------------------------------------------------#
sub setParameter {
  my ($self, $name, $value, $units) = @_;
  die "name '$name' must start with an alpha character and contain only alphanumeric characters or _ or -" if $name !~ /^[a-zA-Z][\w-]+$/;
  $self->{parameters}->{$name} = {value => $value, units => $units};
  push( @{$self->{parameterNames}}, $name );
}

#-----------------------------------------------------------------------------#

=head2 setStatistic($name, $value, $units) 

Sets a statistic that was obtained for the benchmark.  $name is a unique
identifier for this parameter and $value its value.  The optional $units
describes a scalar $value (e.g., Gb/s, secs, etc.).

=cut
#-----------------------------------------------------------------------------#
sub setStatistic {
  my ($self, $name, $value, $units) = @_;
  die "name '$name' must start with an alpha character and contain only alphanumeric characters or _ or -" if $name !~ /^[a-zA-Z][\w-]+$/;
  $self->{statistics}->{$name} = {value => $value, units => $units};
  push( @{$self->{statisticNames}}, $name );
}

#-----------------------------------------------------------------------------#

=head2 statisticNames

=cut
#-----------------------------------------------------------------------------#
sub statisticNames {
  my $self = shift;
  return @{$self->{statisticNames}};
}

1;

__END__

=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

