package Inca::Reporter::GlobusUnit;
use Inca::Reporter::SimpleUnit;
@ISA = ('Inca::Reporter::SimpleUnit');

################################################################################

=head1 NAME

Inca::Reporter::GlobusUnit - Convenience module for creating simple unit
reporters that submit a test via globus

=head1 SYNOPSIS

  use Inca::Reporter::GlobusUnit;
  my $reporter = new Inca::Reporter::GlobusUnit(
    name => 'Reporter Name',
    version => 0.1,
    description => 'A really helpful reporter description',
    url => 'http://url.to.more.reporter.info'
    unit_name => 'What this reporter tests'
  );

=head1 DESCRIPTION

This module is a subclass of Inca::Reporter::SimpleUnit which provides
convenience methods for submitting a unit test via globus.

=cut
################################################################################

use strict;
use warnings;
use Net::Domain qw/hostfqdn/;
use File::Temp qw/tempdir/;

#=============================================================================#

=head1 CLASS METHODS

=cut

#=============================================================================#

#-----------------------------------------------------------------------------#

=head2 new

Class constructor which returns a new GlobusUnit object.
See L<Inca::Reporter::SimpleUnit> for parameters.

=cut

#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = $class->SUPER::new(@_);
  $self->addDependency(__PACKAGE__);
  $self->addDependency('Inca::Reporter::GridProxy');
  bless ($self, $class);
  return $self;
}

#-----------------------------------------------------------------------------#

=head2 submitCSource(%attrs)

Submit a small C program to execute via a local GRAM.  In addition to the
parameters recognized by submitJob, the required $attrs{code} specifies the
source to compile.

=cut

#-----------------------------------------------------------------------------#
sub submitCSource {
  my ($self, %attrs) = @_;
  return (undef, "No code passed to submitCSource") if !defined($attrs{code});

  my $clean = defined($attrs{cleanup}) ? $attrs{cleanup} : 1;
  my $dir = tempdir(CLEANUP => 1);
  my $flavor = defined($attrs{flavor}) ? "--flavor=$attrs{flavor}" : "";
  chdir($dir);

  my $cc = '$(GLOBUS_CC)';
  my $ld = '$(GLOBUS_LD)';
  if(defined($attrs{mpi}) && $attrs{mpi}) {
    $cc = 'mpicc';
    $ld = 'mpicc';
  }
  my $makeFile = $self->loggedCommand("globus-makefile-header $flavor");
  return (undef, "globus-makefile-header failed: $makeFile\n") if $? != 0;
  $makeFile .= '

all:
	' . $cc . ' $(GLOBUS_CFLAGS) $(GLOBUS_INCLUDES) -c gh.c
	' . $ld . ' -o gh gh.o $(GLOBUS_LDFLAGS) $(GLOBUS_PKG_LIBS) $(GLOBUS_LIBS)
';

  return (undef, "Unable to write Makefile\n") if !open(OUT, '>Makefile');
  print OUT $makeFile;
  return (undef, "Unable to write source\n") if !open(OUT, '>gh.c');
  print OUT $attrs{code};
  close OUT;

  my($error, $output);
  $output = $self->loggedCommand('make');
  return (undef, "make failed: $output\n") if $?;

  if(defined($attrs{env}) && $attrs{env} ne '') {
    $attrs{env} += ':';
  } else {
    $attrs{env} = ''
  }
  $attrs{env} .= "LD_LIBRARY_PATH=$ENV{GLOBUS_LOCATION}/lib";
  $attrs{executable} = "$dir/gh";
  $attrs{remote} = 0;
  ($output, $error) = $self->submitJob(%attrs);
  if($clean) {
    unlink glob "$dir/*";
    chdir "$ENV{HOME}";
    rmdir $dir;
  }
  return ($output, $error);

}

#-----------------------------------------------------------------------------#

=head2 submitJob(%attrs)

Submit a job to execute a command via Globus.  Recognized parameters:

=over 13

=item arguments

arguments to pass to executable; default ''

=item check

poll job for completion every this many seconds; default 30

=item cleanup

remove temporary files after run; default true

=item count

number of hosts to use; default 1

=item debug

log the submision command and the result with -dumprsl; default false

=item duroc

add (resourceManagerContact=xx) to rsl; default false

=item executable

the program to run; required

=item env

environment variable to set; default ''

=item host

host where run takes place; default localhost

=item mpi

execute as an MPI program; default false

=item queue

name of batch queue to submit job to; default none

=item remote

executable is already on the jobmanager resource; default 1

=item service

the Globus service to invoke; default to Globus default

=item timeout

kill the job and report an error after this many seconds; default 3600 (1 hr)

=back

=cut

#-----------------------------------------------------------------------------#
sub submitJob {
  my ($self, %attrs) = @_;
  return (undef, "No executable supplied to submitJob\n")
    if !defined($attrs{executable});

  my $clean = defined($attrs{cleanup}) ? $attrs{cleanup} : 1;
  my $contact = defined($attrs{host}) ? $attrs{host} : hostfqdn();
  $contact .= "/$attrs{service}" if defined $attrs{service};
  my $count = defined($attrs{count}) ? $attrs{count} : 1;
  my $debug = $attrs{debug} ? '-dumprsl' : '';
  my $err = "$ENV{HOME}/.inca.tmp.$$.err";
  my $extraRsl = "(host_count=$count)";
  $extraRsl .= "(resourceManagerContact=$contact)" if $attrs{duroc};
  $extraRsl .= '(jobtype=mpi)' if $attrs{mpi};
  my $out = "$ENV{HOME}/.inca.tmp.$$.out";
  my $pollTime = defined($attrs{check}) ? $attrs{check} : 30;
  my $timeout = defined($attrs{timeout}) ? $attrs{timeout} : 3600;

  my $cmd =
    "globus-job-submit $debug -stderr -s $err -stdout -s $out $contact " .
    "-count $count -maxtime " . int($timeout / 60) . " -x '$extraRsl'";
  if(defined($attrs{env})) {
    my $env = $attrs{env};
    $env =~ s/^|:/ -env /g;
    $cmd .= $env;
  }
  $cmd .= ' -q ' . $attrs{queue} if defined $attrs{queue};
  $cmd .= ($attrs{remote} ? ' -l ' : ' -s ') . $attrs{executable};
  $cmd .= " $attrs{arguments}" if defined $attrs{arguments};

  my $jobId = $self->loggedCommand("$cmd");
  chomp $jobId;
  return (undef, "call to '$cmd' failed: $jobId\n") if $?;
  return (undef, "invalid job id returned: '$jobId'\n") if $jobId !~ /https/;

  eval {
    local $SIG{ALRM} = sub {die "timed out\n";};
    alarm($timeout);
    my $status;
    do {
      sleep $pollTime;
      $status = $self->loggedCommand("globus-job-status $jobId");
      return (undef, "call to globus-job-status failed: $status\n") if $?;
    } while $status =~ /PENDING|ACTIVE|UNSUBMITTED/;
    alarm(0);
  };
  if($@) {
    $self->loggedCommand("globus-job-cancel -f $jobId");
    return (undef, "job did not complete within $timeout seconds\n")
  }

  my $output;
  if(open(OUTPUT, "<$out")) {
    my @lines = <OUTPUT>;
    close OUTPUT;
    $output = join("\n", @lines);
    unlink $out if $clean;
  }

  my $error;
  if(open(ERROR, "<$err")) {
    my @lines = <ERROR>;
    close ERROR;
    $error = join("\n", @lines);
    unlink $err if $clean;
  }

  return ($output, $error);

}

1;

__END__

=head1 EXAMPLE

  my $reporter = new Inca::Reporter::GlobusUnit(
    name => 'Globus hello',
    version => 1,
    description => 'Verifies local Globus exec of hello world',
    url => 'http://www.globus.org',
    unit_name => 'Globus hello'
  );

  my ($output, $err) = $reporter->submitCSource(
    code =>
  "#include <stdio.h>
   int main() {
      printf(\"Hello world\\n\");
      return 0;
   }
  "
  );

  if(!defined($output) || $output !~ /Hello/) {
    $reporter->unitFailure
      ("test failed" . (defined($err) ? ": $err" : ''));
  } else {
    $reporter->unitSuccess();
  }
  $reporter->print();

=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 SEE ALSO

L<Inca::Reporter::SimpleUnit>

=cut
