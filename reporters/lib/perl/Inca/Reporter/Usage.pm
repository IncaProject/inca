package Inca::Reporter::Usage;
use Inca::Reporter;
@ISA = ('Inca::Reporter');

################################################################################

=head1 NAME

Inca::Reporter::Usage - Module for creating simple usage reports

=head1 SYNOPSIS

  use Usage;
  my $reporter = new Usage(
    name => 'My Reporter',
    version => 0.1,
    description => 'What my reporter does',
    url => 'http://some.where/'
  );

=head1 DESCRIPTION

This module is a subclass of Inca::Reporter which provides a simple schema for
reporting usage data.  The reporter will return the following body if the
reporter is successful:

    <usage>
      <entry>
        <type>type1</type>
        <name>foo</name>
        <statistics>
          <statistic>
            <name>count</name>
            <value>9</value>
          </statistic>
        </statistics>
      </entry>
      <entry>

      .
      .
      .

    </usage>

=cut

################################################################################

use strict;
use warnings;
use Carp;

#=============================================================================#

=head1 CLASS METHODS

#-----------------------------------------------------------------------------#

=head2 new

Class constructor which returns a new Usage object.  The constructor accepts
the parameters supported by L<Inca::Reporter>.

=cut

#-----------------------------------------------------------------------------#
sub new {
  my ($this, %attrs) = @_;
  my $class = ref($this) || $this;
  my $self = $class->SUPER::new(%attrs);
  $self->{entries} = [];
  $self->addDependency(__PACKAGE__);
  bless ($self, $class);
  return $self;
}

#-----------------------------------------------------------------------------#

=head2 addEntry($entry)

This method is used to add an entry to the usage report.  It takes a hash
reference containing the type and name of the entry as well as a hash of
statistics.  For example:

  addEntry(
  {
    'type' => 'foo',
    'name' => 'bar',
    'stats' =>
    {
      'count' => 1,
      'blort' => 'baz'
    }
  });

Will create an entry of type 'foo', with a name of 'bar' and two statistics,
count with a value of 1 and 'blort' with a value of 'baz'.

=cut

#-----------------------------------------------------------------------------#
sub addEntry {
  my ($self, $entry) = @_;
  push(@{ $self->{entries} }, $entry);
}

#-----------------------------------------------------------------------------#

=head2 fail($msg)

This method is used to indicate that the reporter failed.  It takes a single
arguement which is the error message which will be returned.

=cut

#-----------------------------------------------------------------------------#
sub fail {
  my ($self, $msg) = @_;
  $self->setResult(0, $msg);
}

#-----------------------------------------------------------------------------#

=head2 getEntries()

Returns an array of all entries that have been added to the reporter.

=cut

#-----------------------------------------------------------------------------#
sub getEntries {
  my ($self) = @_;
  return @{$self->{entries}}
}

#-----------------------------------------------------------------------------#

=head2 reportBody

Constructs the body and returns it.

=cut

#-----------------------------------------------------------------------------#
sub reportBody {
  my ($self) = @_;
  my @xml;
  foreach my $entry ($self->getEntries()) {
    push(@xml, $self->xmlElement('entry', 0, $self->_reportEntry($entry)));
  }
  return $self->xmlElement('usage', 0, @xml);
}

#-----------------------------------------------------------------------------#

=head2 success()

This method is called to indicate that the reporter has succeeded.

=cut

#-----------------------------------------------------------------------------#
sub success {
  my ($self) = @_;
  $self->setResult(1);
}

#-----------------------------------------------------------------------------#

sub _reportEntry {
  my ($self, $entry) = @_;
  my @xml;
  push(@xml, $self->xmlElement('type', 1, $entry->{type}),
             $self->xmlElement('name', 1, $entry->{name}),
             $self->xmlElement('statistics', 0, $self->_reportStatistics($entry->{stats})));
  return @xml;
}

#-----------------------------------------------------------------------------#

sub _reportStatistics {
  my ($self, $stats) = @_;
  my @xml;
  foreach my $stat (keys %$stats) {
    my @stat = ($self->xmlElement('name', 1, $stat),
                $self->xmlElement('value', 1, $stats->{$stat}));
    push(@xml, $self->xmlElement('statistic', 0, @stat));
  }
  return @xml;
}

=head1 EXAMPLE

  Coming soon.

=head1 AUTHOR

Jon Dugan <jdugan@ncsa.uiuc.edu>

=head1 SEE ALSO

L<Inca::Reporter>

=cut

1;
