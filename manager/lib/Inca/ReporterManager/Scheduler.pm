package Inca::ReporterManager::Scheduler;

################################################################################

=head1 NAME

Inca::ReporterManager::Scheduler - Abstract class for ReporterManager schedulers

=head1 SYNOPSIS

=for example begin

  package Inca::ReporterManager::Scheduler::MyScheduler;
  use base qw(Inca::ReporterManager::Scheduler);
  @ISA = ( "Inca::ReporterManager::Scheduler" );

  # this function is called from the constructor to read additional 
  # scheduler arguments.  If your scheduler doesn't have any, you 
  # can use the default definition (which does nothing)
  sub readArgs {
  }

  # you must overload this in order to accept scheduling requests
  sub submit {
  }

=for example end

=head1 DESCRIPTION

This is an abstract base class for reporter manager schedulers.  A
reporter manager scheduler at a minimum needs to supply its own submit
function.  If a scheduler has arguments, then it can override readArgs.

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
use Inca::Logger;
use Inca::Config::Suite::SeriesConfig;
use Inca::Validate qw(:all);

# Perl standard
use Carp;
use Cwd;
use POSIX ":sys_wait_h";

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

our $ADD = "add";
our $DELETE = "delete";

my $SELF_PARAM_REQ = { isa => "Inca::ReporterManager::Scheduler" };

# ensure we wait on dead children (i.e., don't create zombie processes)
$SIG{CHLD} = \&_REAPER;

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( $args, $check_period, $credentials, $depots, $depot_timeout,
            $reporter_cache, $rim_path, $tmpdir )

Base class constructor for reporter manager schedulers.  The constructor must
be called with the following arguments.

=over 2 

B<Arguments>:

=over 17

=item args

A reference to a hash array containing the schedulers configuration
inforamtion

=item check_period

A positive integer indicating the period in seconds of which to check the
reporter for exceeding resource limits. 

=item credentials

A reference to a hash array containing the credential information.

=item depots

A reference to an array of depot uris.  The report will be sent to the first
depot in the list.  If the first depot is unreachable, the next depots in the
list will be tried.

=item depot_timeout

A positive integer indicating the period in seconds of which to time out a 
send of a report to the depot.

=item reporter_cache 

An object of type Inca::ReporterManager::ReporterCache which is used to map
reporter uris to a local path.

=item rim_path

A string containing the path to the reporter-instance-manager program.

=item tmpdir

A string containing a path to a temporary file space that Inca can use while
executing reporters

=back

=back

=begin testing

  use Inca::ReporterManager::Scheduler;
  use Test::Exception;
  dies_ok { new Inca::ReporterManager::Scheduler() } 
           'object dies with no args';

  use Inca::ReporterManager::ReporterCache;
  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  lives_ok { new Inca::ReporterManager::Scheduler( 
    undef, 2, {}, [], 120,"rA", $rc, "sbin/reporter-instance-manager", undef, "/tmp" 
  ) } 
  'object created with undef scheduler_args';

  lives_ok { new Inca::ReporterManager::Scheduler( 
    {}, 2, {}, [], 120,"resourceA", $rc, "sbin/reporter-instance-manager", undef, "/tmp" 
  ) } 
  'object created with scheduler_args';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  $self->{logfile} = undef;
  $self->{logger} = Inca::Logger->get_logger( $class );

  my $args;
  ( $args, $self->{check_period}, $self->{credentials}, $self->{depot_uris},
    $self->{depot_timeout}, $self->{id}, $self->{reporter_cache}, 
    $self->{rim_path}, $self->{suspend}, $self->{tmpdir} ) =
      validate_pos( @_, HASHREF | UNDEF, $POSITIVE_INTEGER_PARAM_REQ, HASHREF,
                        ARRAYREF, $POSITIVE_INTEGER_PARAM_REQ, SCALAR, 
                        $REPORTER_CACHE_PARAM_REQ, SCALAR, 
                        $SUSPEND_PARAM_REQ | UNDEF, SCALAR );

  $self->readArgs( $args );

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 isRecurring( )
  
Indicates if the scheduler is recurring and should be persisted.

=over 2 

B<Returns>:

Returns true if the scheduler is recurring and false otherwise.

=back

=cut
#-----------------------------------------------------------------------------#
sub isRecurring {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return 0;
}

#-----------------------------------------------------------------------------#

=head2 isRunning( )
  
Check if the scheduler is running.  A subclass should override this function
only if their scheduler maintains objects that need to be explicitly started
and stopped.  This base class function does nothing.

=over 2 

B<Returns>:

Returns true if the scheduler is running and false otherwise.

=back

=cut
#-----------------------------------------------------------------------------#
sub isRunning {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return 1;
}

#-----------------------------------------------------------------------------#

=head2 readArgs( $args )

A subclass should override this function if they want to reads in scheduler
specific configuration information.  This base class function does nothing.

=over 2

B<Arguments>:

=over 7

=item args

A reference to a hash containing configuration information for the scheduler

=back

=back

=begin testing

  use Test::Exception;

  package Inca::ReporterManager::Scheduler::MyScheduler;
  use base qw(Inca::ReporterManager::Scheduler);
  @ISA = ( "Inca::ReporterManager::Scheduler" );

  sub readArgs {
    my $self = shift;
    $self->{var} = undef;
  }

  package main;
  use Inca::ReporterManager::ReporterCache;
  my $rc = new Inca::ReporterManager::ReporterCache( 't' );
  my $scheduler = new Inca::ReporterManager::Scheduler::MyScheduler(
    undef, 50, {}, [], 120,"A", $rc, "sbin/reporter-instance-manager", undef,
    "/tmp" 
  );
  ok( exists $scheduler->{var}, "readArgs called from constructor" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub readArgs {
  # do nothing
}

#-----------------------------------------------------------------------------#

=head2 setLogger( )
  
Set a log file for the reporter-instance-managers.

=cut
#-----------------------------------------------------------------------------#
sub setLogger {
  my ( $self, $logfile ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{logfile} = $logfile;
}

#-----------------------------------------------------------------------------#

=head2 start( )

A subclass should override this function only if their scheduler maintains
objects that need to be explicitly started and stopped.  This base class
function does nothing.

=over 2

B<Returns>:

Returns true if there were no errors starting the scheduler; returns false
otherwise.

=back

=begin testing

  use Test::Exception;

  package Inca::ReporterManager::Scheduler::MyScheduler;
  use base qw(Inca::ReporterManager::Scheduler);
  @ISA = ( "Inca::ReporterManager::Scheduler" );

  package main;
  use Inca::ReporterManager::ReporterCache;
  my $rc = new Inca::ReporterManager::ReporterCache( 't' );
  my $scheduler = new Inca::ReporterManager::Scheduler::MyScheduler(
    undef, 50, [], {}, 120,"A", $rc, "sbin/reporter-instance-manager", undef,
    "/tmp" 
  );
  ok( $scheduler->start(), "default start returns 1" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub start {
  return 1;
}

#-----------------------------------------------------------------------------#

=head2 stop( )

A subclass should override this function only if their scheduler maintains
objects that need to be explicitly started and stopped.  This base class
function does nothing.

=over 2

B<Returns>:

Returns true if there were no errors stopping the scheduler; returns false
otherwise.

=back

=begin testing

  use Test::Exception;

  package Inca::ReporterManager::Scheduler::MyScheduler;
  use base qw(Inca::ReporterManager::Scheduler);
  @ISA = ( "Inca::ReporterManager::Scheduler" );

  package main;
  use Inca::ReporterManager::ReporterCache;
  my $rc = new Inca::ReporterManager::ReporterCache( 't' );
  my $scheduler = new Inca::ReporterManager::Scheduler::MyScheduler(
    undef, 50, [], {}, 120, "A", $rc, "sbin/reporter-instance-manager", undef,
    "/tmp" 
  );
  ok( $scheduler->stop(), "default stop returns 1" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub stop {
  return 1;
}

#-----------------------------------------------------------------------------#

=head2 submit( $action, @reporters )

Accepts an action (e.g., add, delete) and any number of reporters and executes
them according to the subclass scheduling algorithm.  B<Subclasses must
override this function>.  This base class function will issue a croak if
called.

=over 2

B<Arguments>:

=over 13

=item action

A string containing an action (e.g., add, delete) pertaining to the group
of reporters.

=item configs

An array of Inca::Config::Suite::SeriesConfig objects.

=back

B<Returns>:

Returns 1 if the reporters where accepted by the scheduler; otherwise returns
0.

=back

=begin testing

  use Test::Exception;

  package Inca::ReporterManager::Scheduler::MyScheduler;
  use base qw(Inca::ReporterManager::Scheduler);
  @ISA = ( "Inca::ReporterManager::Scheduler" );

  package main;
  use Inca::ReporterManager::ReporterCache;
  my $rc = new Inca::ReporterManager::ReporterCache( 't' );
  my $scheduler = new Inca::ReporterManager::Scheduler::MyScheduler(
    undef, 50, [], {}, 120,"A", $rc, "sbin/reporter-instance-manager", undef,
    "/tmp" 
  );
  dies_ok { $scheduler->submit() } 'dies when base class submit called';


=end testing

=cut
#-----------------------------------------------------------------------------#
sub submit {
  my $self = shift;

  my $package_name = __PACKAGE__;
  $self->{logger}->logdie( "You cannot call the submit function of $package_name.  You must override this function in your subclass" );
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _createTestConfig( $name )
#
# Create a sample series config object using the reporter $name.
#
# Arguments:
#
#  name  A string containing the name of a reporter in the t directory
#
# Note:  For testing purposes only.
#-----------------------------------------------------------------------------#
sub _createTestConfig {
  my $class = shift;
  my $name = shift;

  my $sc = new Inca::Config::Suite::SeriesConfig();
  $sc->setAction( 'add' );
  $sc->setName( $name );
  $sc->setNickname( "testA" );
  $sc->setVersion( "1" );
  $sc->setPath( getcwd() . "/t/" . $sc->getName() );
  $sc->setContext( $sc->getName() );
  $sc->setGuid( "incas://a.sdsc.edu/suiteB" );
  $sc->setUri( "incas://b.sdsc.edu/reporters/" . $name );
  return $sc;
}

#-----------------------------------------------------------------------------#
# _getRIMCmdArgs( $reporter )
#
# Called by _execCronEntry when a reporter is scheduled to execute to get
# the command-line needed to execute the reporter-instance-manager.
#
# Returns:
#
# Returns an array of arguments that should be passed to the 
# reporter-instance-manager

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Inca::ReporterManager::ReporterCache;
  use Inca::ReporterManager::Scheduler::Cron;
  untie *STDOUT;
  untie *STDERR;
  use strict;
  use warnings;
  use Cwd;
  use File::Spec;
  my $file_depot = "file://" . File::Spec->catfile( getcwd(), "depot.tmp.$$" );

  my $agent_uri = "incas://localhost:3234";
  my $credentials = { key => "key", cert => "cert", trusted => "trusted" };
  my $sc = new Inca::Config::Suite::SeriesConfig();
  $sc->setName( "echo_report" );
  $sc->setTargetResource( "resourceB" );
  $sc->setVersion( "1" );
  $sc->setContext( "echo_report" );
  $sc->setPath( getcwd() . "/t/echo_report" );
  $sc->setNiced( 1 );
  $sc->setProxyContact( $agent_uri );
  $sc->setLimits( { cpuTime => 60, wallClockTime => 300, memory => 50 } );
  my $rc = new Inca::ReporterManager::ReporterCache( 't',
    errorReporterPath => "bin/inca-null-reporter" 
  );
  my $sched = new Inca::ReporterManager::Scheduler::Cron( 
                    undef,
                    1,
                    $credentials, 
                    [ $file_depot ],
                    120,
                    "resourceA",
                    new Inca::ReporterManager::ReporterCache( 't',
                      errorReporterPath => "bin/inca-null-reporter"
                    ),
                    "sbin/reporter-instance-manager",
                    undef,
                    "/tmp"
                    );
  my @args = $sched->_getRIMCmdArgs( $sc );
  my @cmp_args = ( "--cert=cert", "--key=key", "--trusted=trusted", "--level=DEBUG", "--depot=$file_depot", "--depot-timeout=120", "--path=" . getcwd() . "/t/echo_report", "--name=echo_report", "--version=1", "--context=echo_report", "--id=resourceA", "--target-id=resourceB", "--error-reporter=bin/inca-null-reporter", "--reporter-cache=" . getcwd() . "/t", "--niced=1", "--agent=".$agent_uri, "--var=/tmp", "--wait=1", "--cpuTime=60", "--memory=50",  "--wallTime=300" );
  ok( eq_array(\@cmp_args, \@args), "RIM cmd-line is correct with credentials");
  
  delete $sched->{credentials};
  @args = $sched->_getRIMCmdArgs( $sc );
  my @cmp_args2 = @cmp_args[3..$#cmp_args];
  ok( eq_array(\@cmp_args2, \@args), "RIM cmd-line is correct without credentials" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _getRIMCmdArgs {
  my ( $self, $config ) = 
    validate_pos( @_, $SELF_PARAM_REQ, $SERIESCONFIG_PARAM_REQ );

  if ( ! $config->hasContext() ) {
    $self->{logger}->error( "No reporter context specified" );
    return undef;
  }
  if ( ! $config->hasPath() ) {
    $self->{logger}->error( "No reporter path specified" );
    return undef;
  }

  my @rim; 
  if ( exists $self->{credentials} && defined $self->{credentials} ) {
    push( @rim, "--cert=" . $self->{credentials}->{cert} ) if 
      ( exists $self->{credentials}->{cert} && 
        defined $self->{credentials}->{cert} );
    push( @rim, "--key=" . $self->{credentials}->{key} ) if
      ( exists $self->{credentials}->{key} && 
        defined $self->{credentials}->{key} );
    push( @rim, "--trusted=" . $self->{credentials}->{trusted} ) if
      ( exists $self->{credentials}->{trusted} && 
        defined $self->{credentials}->{trusted} );
  }
  push( @rim, "--logfile=" . $self->{logfile} ) if defined $self->{logfile};
  my $level = undef; # enclose below in a eval in case Log4perl does not exist
  eval {
    require Log::Log4perl::Level;
    $level = Log::Log4perl::Level::to_level( $self->{logger}->level() );
  };
  push( @rim, "--level=" . $level ) if defined $level;
  for my $depot ( @{$self->{depot_uris}} ) {
    push( @rim, "--depot=$depot" );
  }
  push( @rim, "--depot-timeout=" .$self->{depot_timeout}) 
    if exists $self->{depot_timeout};
  push( @rim, "--path=" . $config->getPath() );
  push( @rim, "--name=" . $config->getName() );
  push( @rim, "--version=" . $config->getVersion() );
  push( @rim, "--context=" . $config->getContext() );
  push( @rim, "--id=" . $self->{id} ) if exists $self->{id};
  push( @rim, "--target-id=" . $config->getTargetResource() ) 
    if $config->hasTargetResource();
  if ( exists $self->{reporter_cache} ) {
    push( @rim, "--error-reporter=" . 
          $self->{reporter_cache}->getErrorReporterPath() );
    push( @rim, "--reporter-cache=".$self->{reporter_cache}->getLocation() );
  }
  push( @rim, "--niced=" . $config->getNiced() ) if $config->hasNiced();
  push( @rim, "--agent=" . $config->getProxyContact() ) 
    if $config->hasProxyContact();
  push(@rim, "--suspend=" . $self->{suspend}) if defined $self->{suspend};
  push( @rim, "--var=" . $self->{tmpdir} ) if exists $self->{tmpdir};
  push( @rim, "--wait=" .$self->{check_period}) if exists $self->{check_period};
  if ( $config->hasLimits() ) {
    my $limits = $config->getLimits();
    push( @rim, "--cpuTime=" . $limits->getCpuTime() ) if $limits->hasCpuTime();
    push( @rim, "--memory=" . $limits->getMemory() ) if $limits->hasMemory();
    push( @rim, "--wallTime=" . $limits->getWallClockTime() ) if $limits->hasWallClockTime();
  }
  return @rim;
}

#-----------------------------------------------------------------------------#
# _REAPER( )
#
# Fail safe function that will be used to wait any dead child processes.
# This prevents zombie processes from being created and filling up a 
# machine.  This is straight from the Perl Cookbook.
#
#-----------------------------------------------------------------------------#
sub _REAPER {
  my $stiff;
  while (($stiff = waitpid(-1, &WNOHANG)) > 0) {
      # do something with $stiff if you want
  }
  $SIG{CHLD} = \&_REAPER;                  # install *after* calling waitpid
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

No known problems.

=cut
