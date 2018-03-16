package Inca::Reporter::Performance;
use Inca::Reporter;
use Inca::Reporter::Performance::Benchmark;
@ISA = ('Inca::Reporter');

################################################################################

=head1 NAME

Inca::Reporter::Performance - Convenience module for performance-related
reporters

=head1 SYNOPSIS

  use Inca::Reporter::Performance;
  my $performance = new Inca::Reporter::Performance(
    name => 'My performance reporter',
    version => 1,
    description => 'Measures host performance',
    url => 'http://inca.sdsc.edu',
    measurement_name => 'host performance'
  );
  ...
  my $benchmark = new Inca::Reporter::Performance::Benchmark();
  $benchmark->addParameter('num_cpus', 16 );
  $benchmark->addStatistic('bandwidth', 10, 'Mb/s');
  $performance->addBenchmark('sample', $benchmark);
  $reporter->print();

=head1 DESCRIPTION

Module for writing performance related reporters.  A performance reporter
has one or more benchmarks.  Each benchmark has one or more statistics
(i.e., results) and can further be described with one or more parameters.  For
example,

  <performance> 
    <ID>some_id</ID>
    <benchmark>
      <ID>sample</ID>
      <parameters>
        <ID>parameters</ID>
        <parameter>
          <ID>num_cpus</ID>
          <value>16</value>
        </parameter>
      </parameters>
      <statistics>
        <ID>statistics</ID>
        <statistic>
          <ID>bandwidth</ID>
          <value>10</value>
          <units>Mb/s</units>
        </statistic>
      </statistics>
    </benchmark>
  </performance>

By default, the exit status of the reporter will be set to true (i.e.,
success).  See L<Inca::Reporter::Performance::Benchmark> for more information.

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

=head2 new

Class constructor which returns a new Inca::Reporter::Performance object.  The
constructor supports the following parameter in addition to those supported by
L<Inca::Reporter>.
  
=over 13

=item measurement_name
  
the name of the performance metric measured by the reporter; default ''.
    
=item short

shorten the body by printing XML in compact form (i.e., thru attributes);
default 0

=back

=cut

#-----------------------------------------------------------------------------#
sub new {
  my ($this, %attrs) = @_;
  my $class = ref($this) || $this;
  my $name = defined($attrs{measurement_name}) ? $attrs{measurement_name} : '';
  delete $attrs{measurement_name};
  my $short = defined($attrs{short}) ? $attrs{short} : 0;
  delete $attrs{short};
  my $self = $class->SUPER::new(%attrs);
  $self->{measurement_name} = $name;
  $self->{short} = $short;
  $self->{benchmark} = {};
  $self->addDependency(__PACKAGE__);
  $self->addDependency('Inca::Reporter::Performance::Benchmark');
  bless ($self, $class);
  return $self;
}

#-----------------------------------------------------------------------------#

=head2 addBenchmark($name, $benchmark)

Add a benchmark to the reporter.  $benchmark is an object of type
Inca::Reporter::Performance::Benchmark. $name identifies the benchmark.

=cut
#-----------------------------------------------------------------------------#
sub addBenchmark {
  my ($self, $name, $benchmark) = @_;
  $self->{benchmark}->{$name} = $benchmark;
}

#-----------------------------------------------------------------------------#

=head2 addNewBenchmark($name)

A convenience that combines the allocation of a benchmark and its addition to
the reporter.

=cut
#-----------------------------------------------------------------------------#
sub addNewBenchmark {
  my ($self, $name) = @_;
  my $benchmark = new Inca::Reporter::Performance::Benchmark();
  $self->addBenchmark($name, $benchmark);
  return $benchmark;
}

#-----------------------------------------------------------------------------#

=head2 getMeasurementName

Returns the name of the performance metric measured by the reporter.

=cut
#-----------------------------------------------------------------------------#
sub getMeasurementName {
  my $self = shift;
  return $self->{measurement_name};
}

#-----------------------------------------------------------------------------#

=head2 reportBody

Constructs and returns the body of the reporter.

=cut

#-----------------------------------------------------------------------------#
sub reportBody {
  my $self = shift;

  if ( $self->{short} ) {
    return $self->reportBodyShort();
  } else {
    return $self->reportBodyLong();
  }
}

#-----------------------------------------------------------------------------#

=head2 reportBodyLong

Constructs and returns the body of the reporter using no attributes.

=cut

#-----------------------------------------------------------------------------#
sub reportBodyLong {
  my $self = shift;
  my $idXml = $self->xmlElement('ID', 1, $self->getMeasurementName());
  my @benchmarkXmls;
  while(my ($name, $benchmark) = each %{$self->{benchmark}}) {
    my $bidXml = $self->xmlElement('ID', 1, $name);
    my ($paramsXml, @paramXmls, $statsXml, @statXmls);
    foreach my $param($benchmark->parameterNames()) {
      my ($value, $units) = $benchmark->getParameter($param);
      my $pidXml = $self->xmlElement('ID', 1, $param);
      my $valueXml = $self->xmlElement('value', 1, $value);
      my $unitsXml;
      $unitsXml = $self->xmlElement('units', 1, $units) if defined($units);
      push(@paramXmls,
           $self->xmlElement('parameter', 0, $pidXml, $valueXml, $unitsXml)
          );
    }
    $paramsXml = $self->xmlElement('parameters', 0, @paramXmls)
      if $#paramXmls >= 0;
    foreach my $stat($benchmark->statisticNames()) {
      my ($value, $units) = $benchmark->getStatistic($stat);
      my $sidXml = $self->xmlElement('ID', 1, $stat);
      my $valueXml = $self->xmlElement('value', 1, $value);
      my $unitsXml;
      $unitsXml = $self->xmlElement('units', 1, $units) if defined($units);
      push(@statXmls,
           $self->xmlElement('statistic', 0, $sidXml, $valueXml, $unitsXml)
          );
    }
    $statsXml = $self->xmlElement('statistics', 0, @statXmls)
      if $#statXmls >= 0;
    push(@benchmarkXmls,
         $self->xmlElement('benchmark', 0, $bidXml, $paramsXml, $statsXml)
        );
  }
  return $self->xmlElement('performance', 0, $idXml, @benchmarkXmls);

}

#-----------------------------------------------------------------------------#

=head2 reportBodyShort

Constructs and returns the body of the reporter using attributes to shorten
xml.

=cut

#-----------------------------------------------------------------------------#
sub reportBodyShort {
  my $self = shift;
  my $idXml = $self->xmlElement('ID', 1, $self->getMeasurementName());
  my @benchmarkXmls;
  for my $name (sort keys(%{$self->{benchmark}}) ) {
    my $benchmark = $self->{benchmark}->{$name};
    my ($paramsXml, %paramAttrs, %statAttrs, $statsXml);
    foreach my $param($benchmark->parameterNames()) {
      my ($value, $units) = $benchmark->getParameter($param);
      my $attrName = $param;
      $attrName = $attrName . "_" . $units if defined $units;
      $paramAttrs{$attrName} = $value;
    }
    $paramsXml = $self->xmlElement('parameters', 0, \%paramAttrs)
      if scalar(keys %paramAttrs) > 0;
    foreach my $stat($benchmark->statisticNames()) {
      my ($value, $units) = $benchmark->getStatistic($stat);
      my $attrName = $stat;
      $attrName = $attrName . "_" . $units if defined $units;
      $statAttrs{$attrName} = $value;
    }
    $statsXml = $self->xmlElement('statistics', 0, \%statAttrs);
    push(@benchmarkXmls,
         $self->xmlElement('benchmark', 0, {"ID" => $name}, $paramsXml, $statsXml)
        );
  }
  return $self->xmlElement
    ('performance', 0, {"ID" => $self->getMeasurementName()}, @benchmarkXmls);

}

#-----------------------------------------------------------------------------#

=head2 setMeasurementName($name)

Sets the name of the performance metric measured by the reporter.

=cut
#-----------------------------------------------------------------------------#
sub setMeasurementName {
  my ($self, $name) = @_;
  carp "Missing name argument" and return if !defined($name);
  $self->{measurement_name} = $name;
}

1;

__END__

=head1 EXAMPLE

  my $reporter = new Inca::Reporter::Performance(
    name => 'perfeval.local.file.copy',
    description =>
      'Measures the time it takes to copy a file from one directory to another',
    version => 1.3,
    url => 'http://totally.madeup.org',
    measurement_name => 'file_transfer_time'
  );
  $reporter->addArg('file');
  $reporter->processArgv(@ARGV);
  my $file = $reporter->getValue('file');

  my $start = time();
  `cp $file /tmp`;
  my $elapsed = time() - $start;
  my $benchmark =
    new Inca::Reporter::Performance::Benchmark();
  $benchmark->addStatistic('elapsed_time', $elapsed, 'secs');
  $reporter->addBenchmark('file_transfer_time', $benchmark);
  $reporter->print();

=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 SEE ALSO

L<Inca::Reporter::Performance::Benchmark>

=cut
