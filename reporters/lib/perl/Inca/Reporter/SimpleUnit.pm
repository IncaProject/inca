package Inca::Reporter::SimpleUnit;
use Inca::Reporter;
@ISA = ('Inca::Reporter');

################################################################################

=head1 NAME

Inca::Reporter::SimpleUnit - Module for creating simple unit reporters

=head1 SYNOPSIS

  use Inca::Reporter::SimpleUnit;
  my $reporter = new Inca::Reporter::SimpleUnit(
    name => 'Reporter Name',
    version => 0.1,
    description => 'A really helpful reporter description',
    url => 'http://url.to.more.reporter.info'
    unit_name => 'What this reporter tests'
  );

=head1 DESCRIPTION

This module is a subclass of Inca::Reporter that provides convenience methods
for testing the successful operation of a software package.  If the test
completes, the report body contains

  <unitTest>
    <ID>unitX</ID>
  </unitTest>

Otherwise, the exit_status of the report is set to false.

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

Class constructor which returns a new Inca::Reporter::SimpleUnit object.  The
constructor supports the following parameter in addition to those supported by
L<Inca::Reporter>.
  
=over 13

=item unit_name
  
the name of the unit being tested; default ''.
    
=back

=cut

#-----------------------------------------------------------------------------#
sub new {
  my ($this, %attrs) = @_;
  my $class = ref($this) || $this;
  my $unit_name = defined($attrs{unit_name}) ? $attrs{unit_name} : '';
  delete $attrs{unit_name};
  my $self = $class->SUPER::new(%attrs);
  $self->{unit_name} = $unit_name;
  $self->addDependency(__PACKAGE__);
  bless ($self, $class);
  return $self;
}

#-----------------------------------------------------------------------------#

=head2 getUnitName

Return the name of the unit being tested.

=cut

#-----------------------------------------------------------------------------#
sub getUnitName {
  my $self = shift;
  return $self->{unit_name};
}

#-----------------------------------------------------------------------------#

=head2 reportBody

Constructs and returns the body of the reporter.

=cut

#-----------------------------------------------------------------------------#
sub reportBody {
  my $self = shift;
  my $idXml = $self->xmlElement('ID', 1, $self->getUnitName());
  return $self->xmlElement('unitTest', 0, $idXml);
}

#-----------------------------------------------------------------------------#

=head2 setUnitName($name)

Set the name of the unit being tested to $name.

=cut

#-----------------------------------------------------------------------------#
sub setUnitName {
  my ($self, $name) = @_;
  carp "Missing name argument" and return if !defined($name);
  $self->{unit_name} = $name;
}

#-----------------------------------------------------------------------------#

=head2 unitFailure($msg) 

Sets the result of this unit test to be failed with failure message $msg.

=cut

#-----------------------------------------------------------------------------#
sub unitFailure {
  my $self = shift;
  $self->setResult(0, shift);
}

#-----------------------------------------------------------------------------#

=head2 unitSuccess

Sets the result of this unit test to be successful.

=cut

#-----------------------------------------------------------------------------#
sub unitSuccess {
  my $self = shift;
  $self->setResult(1);
}

1;

__END__

=head1 EXAMPLE

  my $reporter = new Inca::Reporter::SimpleUnit(
    name => 'grid.resource_management.gram.unit.auth',
    version => 1.0,
    description => 'Verifies user can authenticate to gatekeeper',
    url => 'http://www.ncsa.uiuc.edu/People/jbasney',
    unit_name => 'authenticate'
  );
  $reporter->addArg('site', 'target site for authentication');
  $reporter->processArgv(@ARGV);

  my $site = $reporter->argValue('site');
  my $output = $reporter->loggedCommand("globusrun -a -r $site");
  if($output =~ /GRAM Authentication reporter successful/)) {
    $reporter->unitSuccess();
  } else {
    $reporter->unitFail($output);
  }
  $reporter->print();

  my $reporter = new Inca::Reporter::SimpleUnit(
    name => 'runCProgram',
    version => 1.0,
    description => 'Verifies a C program can be compiled and run',
    url => 'http://www.makebelieve.org',
    unit_name => 'runCProgramTest'
  );
  $reporter->processArgv(@ARGV);
  # run make
  if($success) {
    $reporter->log('info', "make completed in $seconds seconds");
  } else {
    $reporter->unitFail('compile failed');
  }
  # check filesize of input file
  if($success) {
    $reporter->log('info', "filesize is $bytes bytes");
    # check permissions
    $reporter->log('warn', 'file is group writeable');
  } else {
    $reporter->unitFail('compile failed');
  }
  # run program
  if($success) {
    $reporter->unitSuccess();
  } else {
    $reporter->unitFail($output);
  }
  $reporter->print();

=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 SEE ALSO

L<Inca::Reporter>

=cut
