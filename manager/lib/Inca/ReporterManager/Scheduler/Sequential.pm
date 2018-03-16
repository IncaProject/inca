package Inca::ReporterManager::Scheduler::Sequential;
use base qw(Inca::ReporterManager::Scheduler);
@ISA = ( "Inca::ReporterManager::Scheduler" );

################################################################################

=head1 NAME

Inca::ReporterManager::Scheduler::Sequential - Executes reporter in sequence

=head1 SYNOPSIS

=for example begin

  use Inca::ReporterManager::Scheduler::Sequential;
  use Inca::ReporterManager::ReporterCache;
  use Cwd;
  use File::Spec;

  my $file_depot = "file://" . File::Spec->catfile(getcwd(), "depot.$$");
  my $sched = new Inca::ReporterManager::Scheduler::Sequential( 
                    1,
                    [ $file_depot ],
                    "resourceA",
                    new Inca::ReporterManager::ReporterCache( 't'),
                    undef, 
                    undef );
  my $sc = new Inca::Config::Suite::SeriesConfig();
  $sc->setName( "echo_report" );
  $sc->setVersion( "1" );
  $sc->setPath( getcwd() . "/t/echo_report" );
  $sc->setContext( "echo_report" );
  $sched->submit( 'add', undef, $sc );

=for example end

A scheduler for the reporter manager which executes reporters sequentially.
The scheduler is forked for each start request. It does not take any
additional configuration parameters.

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
use Inca::ReporterManager::ReporterInstanceManager;
use Inca::Config::Suite::SeriesConfig;
use Inca::Validate qw(:all);

# Perl standard
use Carp;
use Data::Dumper;
use POSIX;

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ = { isa => __PACKAGE__ };
my $ACTION_PARAM_REQ = { regex => qr/add/ };

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

Class constructor which returns a new Inca::ReporterManager::Scheduler::Cron
object.  The constructor must be called with the following arguments.

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

  use Inca::ReporterManager::Scheduler::Sequential;
  use Test::Exception;
  untie *STDOUT;
  untie *STDERR;

  dies_ok { new Inca::ReporterManager::Scheduler::Sequential() } 
          'dies with empty args';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = $class->SUPER::new( @_ );
  
  bless ($self, $class);
  
  $self->{reporters} = [];
  $self->{pid} = 0;
  
  return $self;
}


#-----------------------------------------------------------------------------#

=head2 start( )

Start the sequential scheduler.

=over 2

B<Returns>:

Returns true if there were no errors starting the scheduler; returns false
otherwise.

=back

=begin testing

  use Test::Exception;

=end testing

=cut
#-----------------------------------------------------------------------------#
sub start {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  local $SIG{CHLD} = 'DEFAULT';
  if ( ($self->{pid} = fork()) ) {
    $self->{reporters} = []; # reset to 0
    return 1;
  } else {
    while( scalar(@{$self->{reporters}}) > 0 ) {
      my $config = shift( @{$self->{reporters}} );
      my $rim = new Inca::ReporterManager::ReporterInstanceManager(
                      config => $config,
                      checkPeriod => $self->{check_period},
                      depotURIs => $self->{depot_uris},
                      depotTO => $self->{depot_timeout},
                      id => $self->{id},
                      reporterCache => $self->{reporter_cache},
                      tmpDir => $self->{tmpdir}
                      );
      next if ( ! defined $rim );
      if ( exists $self->{credentials} and defined $self->{credentials} ) {
        $rim->setCredentials( $self->{credentials} );
      }
      if ( $config->hasProxyContact() ) {
        $rim->setAgent( $config->getProxyContact() ); 
      }
      if ( defined $self->{suspend} ) {
        $rim->setSuspend( $self->{suspend} );
      }
      if ( ! $rim->runReporter() ) {
        $self->{logger}->error( "Problem running " . $config->getName() );
      }
    }
    $self->{logger}->info("Sequential scheduler finished executing reporters");
    exit;
  }
  return 1;
}

#-----------------------------------------------------------------------------#

=head2 submit( $action, $scheduler_args, @reporters )

Accepts an action (currently only 'add') and any number of reporters.  These 
reporters will then be executed in a sequential order.

=over 2

B<Arguments>:

=over 13

=item action

A string containing an action (e.g., add, delete) pertaining to the group
of reporters.

=item scheduler_args

A reference to a hash array of arguments for the scheduler

=item configs

An array of Inca::Config::Suite::SeriesConfig objects.

=back

B<Returns>:

Returns true if at least one reporter is executed; otherwise returns false.

=back

=begin testing

  untie *STDOUT;
  untie *STDERR;
  use Cwd;
  use File::Spec;
  use Inca::Logger;
  use Inca::Net::MockDepot;
  use Inca::ReporterManager::ReporterCache;
  use Inca::ReporterManager::Scheduler::Sequential;
  use Inca::ReporterManager::Scheduler;

  Inca::Logger->screen_init( "FATAL" );

  # test quick reporter
  my $sched = new Inca::ReporterManager::Scheduler::Sequential( 
    undef,
    1,
    undef,
    [ "file://" . File::Spec->catfile( getcwd(), "depot.tmp.$$") ], 
    120,
    "resourceA",
    new Inca::ReporterManager::ReporterCache( 't',
      errorReporterPath => "bin/inca-null-reporter" 
    ),
    "sbin/reporter-instance-manager",
    undef,
    "/tmp"
  );
  my $sc = Inca::ReporterManager::Scheduler->_createTestConfig( 
    "echo_report" 
  );
  my $status = $sched->submit( 'add', undef, $sc );
  $sched->start();
  my $depot = new Inca::Net::MockDepot;
  my $numReports = $depot->readReportsFromFile( "./depot.tmp.$$", 10 );
  is( $numReports, 1, "echo report written to file" );
  my @reports = $depot->getReports();
  like( $reports[0], qr/^<rep:report .*/m, 
        "submit to file - looks like reporter was printed");
  like( $reports[0], qr/<completed>true*/m, 
        "submit to file - looks like report was success");

  # test benchmark reporter
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( 
    "stream_report" 
  );
  $sc->setLimits( { 'cpuTime' => 20 } );
  $status = $sched->submit( 'add', undef, $sc );
  $sched->start();
  is( $status, 1, 'returned positive status' );
  $depot = new Inca::Net::MockDepot;
  $numReports = $depot->readReportsFromFile( "./depot.tmp.$$", 15 );
  is( $numReports, 1, "stream report written to file" );
  @reports = $depot->getReports();
  my ($memory) = $reports[0] =~ /^memory_mb=(.*)/m;
  cmp_ok( $memory, ">=", 45, "submit to file - memory is reasonable" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub submit {
  my ( $self, $action, $args, @configs ) = 
    validate_pos( @_, $SELF_PARAM_REQ, 
                      $ACTION_PARAM_REQ, 
                      HASHREF | UNDEF,
                      ($SERIESCONFIG_PARAM_REQ) x (@_ - 3) );
  if ( ref($args) eq "Inca::Config::Suite::SeriesConfig" ) {
    $self->{logger}->error( "Expected HASHREF or UNDEF in parameter #2 (not Inca::Config::Suite::SeriesConfig) for submit" );
    return 0;
  }

  if ( $action eq $Inca::ReporterManager::Scheduler::ADD ) {
    push( @{$self->{reporters}}, @configs );
    return 1;
  } elsif ( $action eq $Inca::ReporterManager::Scheduler::DELETE ) {
    $self->{logger}->error( ref($self) . " does not service delete requests" );
    return 0;
  } else {
    $self->{logger}->error(  ref($self) . " received uknown action '$action'" );
    return 0;
  }

}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

No known problems.

=head1 SEE ALSO

L<Inca::ReporterManager::Scheduler>

=cut
