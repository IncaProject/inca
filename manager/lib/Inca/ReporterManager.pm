package Inca::ReporterManager;

################################################################################

=head1 NAME

Inca::ReporterManager - Executes an Inca reporter suite

=head1 SYNOPSIS

=for example begin

  use Inca::ReporterManager;
  use Inca::Config::Suite;
  use Inca::ReporterManager::ReporterCache;
  my $suite = new Inca::Config::Suite();
  $suite->read( "suite.xml" );
  my $scheduler = new Inca::ReporterManager();
  my $credentials = { cert => "t/certs/client_ca1cert.pem",
                      key => "t/certs/client_ca1keynoenc.pem",
                      passphrase => undef,
                      trusted => "t/certs/trusted" };
  $scheduler->setCredentials( $credentials );
  $scheduler->setCheckPeriod( 1 );
  $scheduelr->setDepots( "incas://localhost:$port" );
  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  $scheduler->setReporterCache( $rc );
  $scheduler->dispatch( $suite, 1 );

=for example end

=head1 DESCRIPTION

This module implements the core functionality of the reporter manager.  It
receives requests from either a reporter agent or a local Inca administrator
via its APIs indicating add/delete of a series config or store a package
(e.g., a reporter, reporter library, or external dependency).  

Each add/delete series config request indicates a reporter name and version, a
set of input arguments, limits, frequency of execution, execution priority, a
scheduler, and a storage policy (for now, a set of depots interested in the
results).  For example, if the reporter execution is on-demand, the scheduler
hands the request to the Sequential scheduler.  In many cases, the Cron
scheduler will be requested, that is the reporter execution is scheduled and
the frequency of execution is managed by an internal cron table.  When the
scheduled time for a reporter comes, a RIM is launched to run the reporter.

Each store package request is handled a bit differently based on the filename.
See L<Inca::ReporterManager::ReporterCache> for more information.

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
use Inca::Config;
use Inca::Constants qw(:all);
use Inca::Logger;
use Inca::Validate qw(:all);
use Inca::IO;
use Inca::Config::Suite;

# Perl standard
use Carp;
use Data::Dumper;
use File::Spec;
use Socket qw(:crlf);

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $CHECK_PERIOD_PARAM_OPT = 
  { %{$POSITIVE_INTEGER_PARAM_OPT}, default => $DEFAULT_CHECK_PERIOD };
my $DEPOT_TIMEOUT_PARAM_OPT = 
  { %{$POSITIVE_INTEGER_PARAM_OPT}, default => $DEFAULT_DEPOT_TIMEOUT};
my $PING_TIMEOUT = 10;
my $SELF_PARAM_REQ = { isa => "Inca::ReporterManager" };

my %SCHEDULERS = ( sequential => "Inca::ReporterManager::Scheduler::Sequential",
                   cron       => "Inca::ReporterManager::Scheduler::Cron" );

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( %Options )

Class constructor which returns a new Inca::ReporterManager object.
The constructor may be called with any of the following attributes.

=over 2 

B<Options>:

=over 13

=item checkPeriod

A positive integer indicating the period in seconds of which to check the
reporter for exceeding resource limits [default: 2]

=item credentials

A reference to a hash array containing the credential information.

=item depotURIs

A list of depot uris.  The report will be sent to the first depot in the list.
If the first depot is unreachable, the next depots in the list will be tried.

=item reporterCache 

An object of type Inca::ReporterManager::ReporterCache which is used to map
reporters to a local path.

=item rimPath

A string containing the path to the reporter-instance-manager executable.

=item tmpDir

A string containing a path to a temporary file space that Inca can use while
executing reporters

=back

=back

=begin testing

  use Test::Exception;
  use Inca::ReporterManager;
  lives_ok { new Inca::ReporterManager() } 'object created';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  # set up defaults
  $self->{logfile} = undef;
  $self->{id} = undef;
  $self->{repositories} = [];
  $self->{schedulers} = {};
  $self->{schedule} = new Inca::Config();
  $self->{scheduleFilename} = undef;
  $self->{logger} = Inca::Logger->get_logger( $class );

  # read options
  my %options = validate( @_, { checkPeriod => $CHECK_PERIOD_PARAM_OPT,
                                credentials => $HASHREF_PARAM_OPT,
                                depotURIs => $ARRAYREF_PARAM_OPT,
                                depotTO => $DEPOT_TIMEOUT_PARAM_OPT,
                                id => $SCALAR_PARAM_OPT,
                                reporterCache => $REPORTER_CACHE_PARAM_OPT,
                                rimPath => $SCALAR_PARAM_OPT,
                                scheduleFilename => $SCALAR_PARAM_OPT,
                                suspend => $SUSPEND_PARAM_OPT,
                                tmpDir => $SCALAR_PARAM_OPT } );

  $self->setCheckPeriod($options{checkPeriod}) if exists $options{checkPeriod};
  $self->setCredentials($options{credentials}) if exists $options{credentials};
  $self->setDepots( @{$options{depotURIs}} ) if exists $options{depotURIs};
  $self->setDepotTimeout($options{depotTO}) if exists $options{depotTO};
  $self->setId( $options{id} ) if exists $options{id};
  $self->setReporterCache( $options{reporterCache} ) 
    if exists $options{reporterCache};
  $self->setRimPath( $options{rimPath} ) if exists $options{rimPath};
  $self->setSuspend( $options{suspend} ) if exists $options{suspend};
  if ( exists $options{tmpDir} ) {
    $self->setTmpDirectory( $options{tmpDir} ); 
  } else {
    $self->setTmpDirectory( File::Spec->tmpdir() );
  }
  # needs to come after tmp space set
  $self->setScheduleFilename( $options{scheduleFilename} ) 
    if exists $options{scheduleFilename};
                                
  return $self;
}

#-----------------------------------------------------------------------------#

=head2 dispatch( $suite, $isNew )

Read add/delete reporter requests in suite and route to
appropriate scheduler.

=over 2

B<Arguments>:

=over 13

=item suite

An object of type Inca::Config::Suite containing add/delete series config requests

=item isNew

An object of type boolean which tells the reporter manager whether to record
the new configs or not (if we are dipatching configs on a restart)

=back

=back

=begin testing

  untie *STDOUT;
  untie *STDERR;

  use strict;
  use warnings;
  use File::Temp qw(tempfile);
  use Inca::Logger;
  use Inca::Net::MockDepot;
  use Inca::ReporterManager;
  use Inca::ReporterManager::ReporterCache;
  use Inca::Config::Suite;

  Inca::Logger->screen_init( "ERROR" );

  my $PORT = 7834;
  my $CREDS = { cert => "t/certs/client_ca1cert.pem",
                key => "t/certs/client_ca1keynoenc.pem",
                passphrase => undef,
                trusted => "t/certs/trusted" };

  my ($fh, $SCHEDULE_FILENAME) = tempfile();

  sub testRM {
    $PORT++;
    my $numReportsExpected = shift;
    my @suite_files = @_;

    my $rc = new Inca::ReporterManager::ReporterCache( "t",
      errorReporterPath => "bin/inca-null-reporter"
    );

    my $rm = new Inca::ReporterManager();
    $rm->setSuspend($ENV{INCA_TEST_SUSPEND}) if exists $ENV{INCA_TEST_SUSPEND};
    $rm->setId( "resourceA" );
    $rm->setCheckPeriod( 1 );
    $rm->setReporterCache( $rc );
    $rm->setDepots( "incas://localhost:$PORT" );
    $rm->setTmpDirectory( "/tmp" );
    $rm->setCredentials( $CREDS );
    $rm->setRimPath( "sbin/reporter-instance-manager" );
    $rm->setScheduleFilename( $SCHEDULE_FILENAME );
    pipe( READ, WRITE );

    my $pid;
    if ( ($pid = fork()) == 0 ) {
      my $out = "";
      do {
        sleep 5;
        $out = `netstat -an | grep $PORT`;
      } while ( $out !~ /$PORT/ );
      for my $suite_file ( @suite_files ) {
        my $suite = new Inca::Config::Suite();
        $suite->read( $suite_file );
        $suite->resolveReporters( $rc, "incas://localhost:8787" );
        $rm->dispatch( $suite, 1 );
        $rm->start();
      }
      if ( scalar(@suite_files) == 0 ) {
        $rm->start();
      }
      my $msg = <READ>;
      close READ;
      $rm->{logger}->info("stopping rm");
      $rm->stop();
      exit;
    }
    my $depot = new Inca::Net::MockDepot;
    my $numReports = $depot->readReportsFromSSL(
      $numReportsExpected, $PORT, "ca1", "t/certs/trusted"
    );
    print WRITE "done\n";
    close WRITE;
    my $exitpid = waitpid($pid, 0);
    ok( $exitpid == $pid || $exitpid == -1, "manager exitted" );
    my @reports = $depot->getReports();
    return ( $numReports, @reports );
  }

  my ( $numReports, @reports ) = testRM( 2, "t/suite.xml" );
  ok( -f $SCHEDULE_FILENAME, "schedule written" );
  unlink $SCHEDULE_FILENAME;
  is( $numReports, 2, "2 reports from suite file" );

  # check sequential mode with suspend
  $ENV{INCA_TEST_SUSPEND} = "load1>0.000001";
  ( $numReports, @reports ) = testRM( 2, "t/suite.xml" );
  ok( -f $SCHEDULE_FILENAME, "schedule written" );
  unlink $SCHEDULE_FILENAME;
  is( $numReports, 2, "2 reports from suite file - suspend mode" );
  like( $reports[0], qr/high load/m, "high load found in report" );
  delete $ENV{INCA_TEST_SUSPEND};

  # check var_suite
  ( $numReports, @reports ) = testRM( 1, "t/var_suite.xml" );
  ok( -f $SCHEDULE_FILENAME, "schedule written" );
  unlink $SCHEDULE_FILENAME;
  is( $numReports, 1, "1 report from var_suite file" );
  like( $reports[0], qr/-verbose=0 -help=no -var1=$ENV{HOME} -var2=/m, 
        "looks like context passed as arg");
 
  # test file with cron
  ( $numReports, @reports ) = testRM( 4, "t/cron_suite.xml" );
  ok( -f $SCHEDULE_FILENAME, "schedule written" );
  unlink $SCHEDULE_FILENAME;
  my $num_gcc_reports = grep( />gcc</m, @reports );
  is( $num_gcc_reports, 2, "2 gcc reports found" );
  my $num_ssl_reports = grep( />openssl</m, @reports );
  is( $num_ssl_reports, 2, "2 ssl reports found" );

  # test file with cron and suspend
  $ENV{INCA_TEST_SUSPEND} = "load1>0.000001";
  ( $numReports, @reports ) = testRM( 2, "t/cron_suite.xml" );
  ok( -f $SCHEDULE_FILENAME, "schedule written" );
  unlink $SCHEDULE_FILENAME;
  is( $numReports, 2, "2 reports from suite file - cron & suspend mode" );
  like( $reports[0], qr/high load/m, "high load found in cron report" );
  delete $ENV{INCA_TEST_SUSPEND};

  # test add/delete capability
  ( $numReports, @reports ) = testRM( 2, "t/cron_suite.xml", "t/delete_cron_suite.xml"  );
  ok( -f $SCHEDULE_FILENAME, "schedule written" );
  unlink $SCHEDULE_FILENAME;
  $num_gcc_reports = grep( />gcc</m, @reports );
  $num_ssl_reports = grep( />openssl</m, @reports );
  is( $num_gcc_reports, 0, "0 gcc reports found" );
  is( $num_ssl_reports, 2, "2 ssl reports found" );

  # test cron with 2 groups
  ( $numReports, @reports ) = testRM( 2, "t/cron_suite2.xml" );
  ok( -f $SCHEDULE_FILENAME, "schedule written" );
  unlink $SCHEDULE_FILENAME;
  $num_gcc_reports = grep( />gcc</m, @reports );
  is( $num_gcc_reports, 1, "one gcc report found" );
  $num_ssl_reports = grep( />openssl</m, @reports );
  is( $num_ssl_reports, 1, "one ssl report found" );
  my @gmt = grep( /gmt/, @reports );
  my ($minute1) = $gmt[0] =~ /(\d\d):\d\dZ/g;
  my ($minute2) = $gmt[1] =~ /(\d\d):\d\dZ/g;
  is( $minute2 - $minute1, 1, "time difference is one minute" );

  # test mixed
  ( $numReports, @reports ) = testRM( 2, "t/mixed_suite.xml" );
  $num_gcc_reports = grep( />gcc</m, @reports );
  is( $num_gcc_reports, 1, "one gcc report found for mixed suite" );
  $num_ssl_reports = grep( />openssl</m, @reports );
  is( $num_ssl_reports, 1, "one ssl report found for mixed suite" );

  ( $numReports, @reports ) = testRM( 1 );
  $num_gcc_reports = grep( />gcc</m, @reports );
  is( $num_gcc_reports, 1, "one gcc report after restart" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub dispatch {
  my ( $self, $suite, $isNew ) = validate_pos( @_, $SELF_PARAM_REQ, $SUITE_PARAM_REQ, $BOOLEAN_PARAM_REQ );

  my $i = 0;
  my $errored_reporter_groups = [];
  my @schedulers_to_start;
  for my $config ( $suite->getSeriesConfigs() ) {
    $self->{logger}->info( 
      "Received suite change request '" . $config->getAction() .  "'"
    );
    if ( $config->hasScheduler() ) {
      my $scheduler = $self->_getScheduler( $config->getSchedulerName() );
      if ( ! defined $scheduler ) {
        $self->{logger}->fatal
          ( "Unable to get/create scheduler " .  $config->getSchedulerName() ); 
        next;
      }
      $self->{schedule}->addSeriesConfig($config) 
        if $isNew && defined $self->{scheduleFilename} && 
           $scheduler->isRecurring();
      $self->{logger}->info(  "Scheduler is " .  $scheduler->isRunning() );
      if ( $scheduler->isRunning() ) {
        $self->{logger}->info( 
          "Stopping " .  $config->getSchedulerName() . " scheduler"
        );
        $scheduler->stop();
        push( @schedulers_to_start, $scheduler );
      }
      $self->{logger}->info( 
        "Dispatching config $i to " . $config->getSchedulerName() . " scheduler"
      );
      if ( ! $scheduler->submit($config->getAction(), 
                                $config->getSchedulerArgs(),
                                $config) ) {
        $self->{logger}->error( "Scheduler rejected reporters" );
        push( @{$errored_reporter_groups}, $i );
      } 
    } else {
      $self->{logger}->error( "No scheduler found for config $i " );
    }
    $i++;
  }
  $self->{schedule}->write( $self->{scheduleFilename} ) 
    if defined $self->{scheduleFilename};
  for my $scheduler ( @schedulers_to_start ) {
    $self->{logger}->info( "Restarting " .  ref($scheduler) . " scheduler" );
    $scheduler->start();
  }

  return $errored_reporter_groups;
}

#-----------------------------------------------------------------------------#

=head2 getCheckPeriod( )

Get the period for how often to check the reporter for exceeding its limits.

=over 2

B<Returns>:

A positive integer indicating the period in seconds of which to check for
resource limits.

=back

=cut 
#-----------------------------------------------------------------------------#
sub getCheckPeriod {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{check_period};
}

#-----------------------------------------------------------------------------#

=head2 getCredentials( )

Retrieve the security credentials.

=over 2

B<Returns>:

A reference to a hash array containing the paths to the security 
credentials.

=back

=cut
#-----------------------------------------------------------------------------#
sub getCredentials {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{credentials};
}

#-----------------------------------------------------------------------------#

=head2 getDepots( )

Retrieve the destination depots that are used to send the report on
completion.

=over 2

B<Returns>:

An array of strings containing uris to depots [host:port, ...]

=back

=cut 
#-----------------------------------------------------------------------------#
sub getDepots {        
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  if ( exists $self->{depots} and defined $self->{depots} ) {
    return @{$self->{depots}};
  } else {
    return;
  }
}

#-----------------------------------------------------------------------------#

=head2 getDepotTimeout( )

Get the period in seconds of which to time out the connection to the depot

=over 2

B<Returns>:

A positive integer indicating the period in seconds of which to time out
thec connection to the depot 

=back

=cut
#-----------------------------------------------------------------------------#
sub getDepotTimeout {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );
  
  return $self->{depot_timeout};
}

#-----------------------------------------------------------------------------#

=head2 getId( )

Get the resource identifier supplied by the reporter agent that the reporter
manager will use to identify itself to the depot.

=over 2

B<Returns>:

A string indicating the id of the resource.

=back

=cut
#-----------------------------------------------------------------------------#
sub getId {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{id};
}

#-----------------------------------------------------------------------------#

=head2 getReporterCache( )

Retrieve the reporter administrator to use in order to find a path to a local
copy of a reporter (from its URI).

=over 2

B<Returns>:

An object of type Inca::ReporterManager::ReporterCache which is used to map
reporters to a local path.

=back

=cut
#-----------------------------------------------------------------------------#
sub getReporterCache {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{reporter_cache};
}

#-----------------------------------------------------------------------------#

=head2 getRimPath( )

Retrieve the path to the reporter-instance-manager script.

=over 2

B<Returns>:

A string containing the path to the reporter-instance-manager script.

=back

=cut
#-----------------------------------------------------------------------------#
sub getRimPath {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{rim_path};
}

#-----------------------------------------------------------------------------#

=head2 getScheduleFilename( )

Retrieve the path to the schedule.

=over 2

B<Returns>:

A string containing the path to the schedule.

=back

=cut
#-----------------------------------------------------------------------------#
sub getScheduleFilename {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{scheduleFilename};
}

#-----------------------------------------------------------------------------#

=head2 getSuspend( )

Get the load condition for suspending execution of a reporter and issuing a 
special suspend report.

=over 2

B<Returns>:

A string indicating the condition of which to suspend execution of a reporter.

=back

=cut
#-----------------------------------------------------------------------------#
sub getSuspend {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{suspend};
}

#-----------------------------------------------------------------------------#

=head2 getTmpDirectory( )

Retrieve the path to a temporary file space that Inca can use while executing
reporters

=over 2

B<Returns>:

A string containing a path to a temporary file space.

=back

=cut
#-----------------------------------------------------------------------------#
sub getTmpDirectory {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{tmpdir};
}

#-----------------------------------------------------------------------------#

=head2 hasScheduler( $scheduler )

Return true if the reporter manager is currently running the provided scheduler.

=over 2

B<Returns>:

A boolean indicating whether or not the reporter manager is running the
provided scheduler.

=back

=cut
#-----------------------------------------------------------------------------#
sub hasScheduler {
  my ( $self, $scheduler ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  return exists $self->{schedulers}->{$scheduler};
}

#-----------------------------------------------------------------------------#

=head2 setCheckPeriod( $secs )

Set the period for how often to check the reporter for exceeding its limits.

=over 2

B<Arguments>:

=over 13

=item secs

A positive integer indicating the period in seconds of which to check for
resource limits.

=back

=back

=begin testing


  use Inca::ReporterManager;

  my $scheduler = new Inca::ReporterManager( checkPeriod => 3 );
  is( $scheduler->getCheckPeriod(), 3, 'set check period from constructor' );
  $scheduler->setCheckPeriod( 10 );
  is( $scheduler->getCheckPeriod(), 10, 'set check period' );
  $scheduler = new Inca::ReporterManager();
  is( $scheduler->getCheckPeriod(), 2, 'default check period is 2' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setCheckPeriod {
  my ( $self, $secs ) =
     validate_pos( @_, $SELF_PARAM_REQ, $POSITIVE_INTEGER_PARAM_REQ );

  $self->{check_period} = $secs;
}

#-----------------------------------------------------------------------------#

=head2 setCredentials( \%credentials )

Specify the credentials to use.

=over 2

B<Arguments>:

=over 13

=item credentials

A reference to a hash array containing the credential information.

=back

=back

=begin testing

  use Inca::ReporterManager;
  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;

  my $credentials = { cert => 't/cert.pem', key => 't/key.pem',
                      passphrase => 'secret', trusted => 't/trusted' };
  my $credentials_new = { cert => 't/new/cert.pem', key => 't/key.pem',
                      passphrase => 'supersecret', trusted => 't/trusted' };
  my $scheduler = new Inca::ReporterManager( 
                  credentials => $credentials );
  ok( eq_hash($scheduler->getCredentials(), $credentials), 
          'set credentials worked from constructor' );
  $scheduler->setCredentials( $credentials_new );
  ok( eq_hash($scheduler->getCredentials(), $credentials_new), 
          'set/get credentials worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setCredentials {
  my ( $self, $credentials ) =
     validate_pos( @_, $SELF_PARAM_REQ, HASHREF );

  $self->{credentials} = $credentials;
}

#-----------------------------------------------------------------------------#

=head2 setDepots( @depot_uris )

Specify the destination depots that are used to send the report on completion.
The report will be sent to the first depot in the list.  If the first depot is
unreachable, the next depots in the list will be tried.

=over 2

B<Arguments>:

=over 13

=item depot_uris

Any number of strings containing a uri to a depot [host:port, ...]

=back

=back
     
=begin testing         

  use Inca::ReporterManager;
  use Test::Exception;

  my $scheduler = new Inca::ReporterManager(depotURIs=>["cuzco.sdsc.edu:8234"]);
  my @depots = $scheduler->getDepots();
  is( $depots[0], "cuzco.sdsc.edu:8234", 'Set depots works from constructor' );
  $scheduler->setDepots( qw(inca.sdsc.edu:8235 inca.sdsc.edu:8236) );
  @depots = $scheduler->getDepots();
  is( $depots[0], "inca.sdsc.edu:8235", 'Set depots works from function (1)' );
  is( $depots[1], "inca.sdsc.edu:8236", 'Set depots works from function (2)' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setDepots {
  my ( $self, @depot_uris ) =
     validate_pos( @_, $SELF_PARAM_REQ, $URI_PARAM_REQ,
                       ($URI_PARAM_REQ) x (@_ - 2) );

  @{$self->{depots}} = @depot_uris;
}

#-----------------------------------------------------------------------------#

=head2 setDepotTimeout( $secs )

Set the period for when the time has exceeded for sending the report to the
depot. 

=over 2

B<Arguments>:

=over 13

=item secs

A positive integer indicating the period in seconds of which to time out
the connection to the depot. 

=back

=back

=begin testing

  use Inca::ReporterManager;

  my $rm = new Inca::ReporterManager( depotTO => 3 );
  is( $rm->getDepotTimeout(), 3, 'set depot timeout from constructor' );
  $rm->setDepotTimeout( 10 );
  is( $rm->getDepotTimeout(), 10, 'set depot timeout' );
  $rm = new Inca::ReporterManager( );
  is( $rm->getDepotTimeout(), 120, 'default check period is 120' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setDepotTimeout {
  my ( $self, $secs ) =
     validate_pos( @_, $SELF_PARAM_REQ, $POSITIVE_INTEGER_PARAM_REQ );

  $self->{depot_timeout} = $secs;
}

#-----------------------------------------------------------------------------#

=head2 setId( $resourceId )

Set the resource identifier supplied by the reporter agent that the reporter
manager will use to identify itself to the depot.

=over 2

B<Arguments>:

=over 13

=item resourceId

A string indicating the id of the resource.

=back

=back

=begin testing

  use Inca::ReporterManager;

  my $rm = new Inca::ReporterManager( id => "resourceA" );
  is( $rm->getId(), "resourceA", 'set id from constructor' );
  $rm->setId( "resourceB" );
  is( $rm->getId(), "resourceB", 'set id' );
  $rm = new Inca::ReporterManager( );
  is( $rm->getId(), undef, 'default id is undef' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setId {
  my ( $self, $id ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );
  
  $self->{id} = $id;
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

=head2 setReporterCache( $reporter_cache )

Specify the reporter administrator to use in order to find a path to a local
copy of a reporter (from its URI).

=over 2

B<Arguments>:

=over 13

=item reporter_cache

An object of type Inca::ReporterManager::ReporterCache which is used to map
reporters to a local path.

=back

=back

=begin testing

  use Inca::ReporterManager;
  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;
  use Cwd;

  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  my $rc_new = new Inca::ReporterManager::ReporterCache( "/tmp" );
  my $scheduler = new Inca::ReporterManager( reporterCache => $rc );
  is( $scheduler->getReporterCache->getLocation(), getcwd() . "/t",
          'set reporter admin worked from constructor' );
  $scheduler->setReporterCache( $rc_new );
  is( $scheduler->getReporterCache->getLocation(), "/tmp",
          'set reporter admin worked from set/get functions' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setReporterCache {
  my ( $self, $reporter_cache) =
     validate_pos( @_, $SELF_PARAM_REQ, $REPORTER_CACHE_PARAM_REQ );


  $self->{reporter_cache} = $reporter_cache;
}

#-----------------------------------------------------------------------------#

=head2 setRimPath( $path )

Specify the path to the reporter-instance-manager script.

=over 2

B<Arguments>:

=over 13

=item path

A string containing the path to the reporter-instance-manager script.

=back

=back

=begin testing

  use Inca::ReporterManager;
  use Test::Exception;

  my $sched = new Inca::ReporterManager(); 
  is( $sched->getRimPath(), undef, 'default rim_path set' );
  dies_ok { $sched->setRimPath( "bin/reporter-instance-manager" ) }
          'setRimPath fails when not executable';
  $sched->setRimPath( "sbin/reporter-instance-manager" );
  is( $sched->getRimPath(), "sbin/reporter-instance-manager", 
      'set/getRimPath work' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setRimPath {
  my ( $self, $path ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  if ( ! -x $path ) {
    $self->{logger}->logdie( 
      "Path '$path' to reporter-instance-manager is not executable" 
    );
  }
  $self->{rim_path} = $path;
}

#-----------------------------------------------------------------------------#

=head2 setScheduleFilename( $path )

Specify the path to the persistent schedule.

=over 2

B<Arguments>:

=over 13

=item path

A string containing the path to the schedule file.

=back

=back

=begin testing

  use Inca::ReporterManager;
  use Test::Exception;

  my $sched = new Inca::ReporterManager(); 
  is( $sched->getScheduleFilename(), undef, 'undef schedule file by default' );
  is( $sched->{scheduleFilename}, undef, 'undef schedule by default' );
  unlink "schedule.xml";
  $sched->setScheduleFilename( "schedule.xml" );
  is( $sched->getScheduleFilename(), "schedule.xml", 
      'set/getScheduleFilname work' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setScheduleFilename {
  my ( $self, $path ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  # read schedule if it exists
  $self->{scheduleFilename} = $path;

  $self->{logger}->info( "Loading existing schedule from $path" );
  $self->{schedule}->read( $path ) if defined $path && -s $path;
  
  for my $suite ( $self->{schedule}->getSuites() ) {
    $suite->resolveReporters( $self->getReporterCache() );
    $self->dispatch( $suite, 0 );
  }
}

#-----------------------------------------------------------------------------#

=head2 setSuspend( $condition )

Set the load condition for suspending execution of the reporter and issuing
a special suspend report.

=over 2

B<Arguments>:

=over 13

=item condition

A string of the format load[1,5,15]>[0-9]+ indicating to suspend
execution of a reporter when the load is great than specified.

=back

=back

=begin testing

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::Constants qw(:defaults);

  my $rim = 
    new Inca::ReporterManager::ReporterInstanceManager( suspend => 'load1>5'
);
  is( $rim->getSuspend(), 'load1>5', 'set suspend from constructor' );
  $rim->setSuspend( 'load5>10' );
  is( $rim->getSuspend(), 'load5>10', 'set suspend' );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( );
  is( $rim->getSuspend(), undef, 'default suspend is undef');

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setSuspend {
  my ( $self, $condition ) =
     validate_pos( @_, $SELF_PARAM_REQ, $SUSPEND_PARAM_REQ );

  $self->{logger}->debug( "Suspend condition set to '$condition'" );
  $self->{suspend} = $condition;
}

#-----------------------------------------------------------------------------#

=head2 setTmpDirectory( $path )

Specify a temporary file space that Inca can use while executing reporters.

=over 2

B<Arguments>:

=over 13

=item path

A string containing a path to a temporary file space 

=back

=back

=begin testing

  use Inca::ReporterManager;
  use Test::Exception;
  use File::Spec;

  my $sched = new Inca::ReporterManager(); 
  is( $sched->getTmpDirectory(), File::Spec->tmpdir(), 'default tmp set' );
  $sched->setTmpDirectory( "/scratch" );
  is( $sched->getTmpDirectory(), "/scratch", 'set/getTmpDirectory work' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setTmpDirectory {
  my ( $self, $path ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{tmpdir} = $path;
}

#-----------------------------------------------------------------------------#

=head2 start( )

Start execution of the schedulers that require specific start/stop.

=begin testing

  untie *STDOUT;
  untie *STDERR;

  use Inca::ReporterManager;
  use Inca::ReporterManager::ReporterCache;
  use Inca::Config::Suite;

  my $rc = new Inca::ReporterManager::ReporterCache( "t",
    errorReporterPath => "bin/inca-null-reporter"
  );

  my $cron_suite = new Inca::Config::Suite();
  $cron_suite->read( "t/cron_suite.xml" );
  $cron_suite->resolveReporters( $rc, "incas://localhost:8787" );
  $rm = new Inca::ReporterManager();
  $rm->setId( "resourceA" );
  $rm->setCheckPeriod( 1 );
  $rm->setReporterCache( $rc );
  $rm->setDepots( "file:./depot.tmp.$$" );
  $rm->setTmpDirectory( "/tmp" );
  $rm->dispatch( $cron_suite, 1 );
  ok( $rm->start(), "rm started" );
  ok( $rm->stop(), "rm stopped" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub start {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  my $num_schedulers_started = 0;
  my ( $name, $scheduler );
  while( ($name, $scheduler) = each %{$self->{schedulers}} ) {
    if ( $scheduler->isRunning() ) {
      $self->{logger}->info( "$name scheduler already running" );
      $num_schedulers_started++;
    } elsif ( $scheduler->start() ) {
      $self->{logger}->info( "$name scheduler started" );
      $num_schedulers_started++;
    } else {
      $self->{logger}->error( "Error starting $name scheduler" );
    }
  }
  if ( $num_schedulers_started < scalar(keys %{$self->{schedulers}}) ) {
    return 0;
  } else {
    return 1;
  }
}

#-----------------------------------------------------------------------------#

=head2 stop( )

Stop execution of the schedulers that require specific start/stop.

=cut
#-----------------------------------------------------------------------------#
sub stop {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  my $num_schedulers_stopped = 0;
  my ( $name, $scheduler );
  while( ($name, $scheduler) = each %{$self->{schedulers}} ) {
    if ( $scheduler->stop() ) {
      $self->{logger}->info( "$name scheduler stopped" );
      $num_schedulers_stopped++;
    } else {
      $self->{logger}->error( "Error stopping $name scheduler" );
    }
  }
  if ( $num_schedulers_stopped < scalar(keys %{$self->{schedulers}}) ) {
    return 0;
  } else {
    return 1;
  }
}

#-----------------------------------------------------------------------------#

=head2 storePackage( $name, $filename, $version, $installpath, $dependencies,
                     $tmpfilename )

Store the specified package into the reporter cache.  

=over 2

B<Arguments>:

=over 13

=item name

A string containing the name of the reporter. 

=item filename

A string containing the filename that the package should be stored under.

=item version

A string containing the version of the package.

=item installpath

A string containing the directory the package should be stored under
relative to the reporter cache location.

=item perms

A string of format "[0-7][0-7][0-7]" that can be used to set the permissions
of the stored file after its written to disk.  A value of undef means that
no change of permissions is needed.

=item dependencies

A string containing a whitespace delimited list of dependencies or undef if
there are none.

=item tmpfilename

A string containing the name of a temporary file that is storing the content 
of the package.

=back

B<Returns>:
True if the package is succesfully cached in the reporter cache and 
false otherwise.

=back

=begin testing

  use Cwd;
  use Inca::ReporterManager;
  use Inca::ReporterManager::ReporterCache;

  my $rm = new Inca::ReporterManager();
  my $rc = new Inca::ReporterManager::ReporterCache( "var/cache" );
  $rm->setReporterCache( $rc );
  my $file = "/tmp/incatest.$$";
  `touch $file`;
  my $result = $rm->storePackage( 
    "Inca::Reporter",
    "Reporter.pm",
    "1.5",
    "lib/perl",
    undef,
    undef,
    $file 
  );
  is( $rm->getReporterCache()->getPath( 
        "Inca::Reporter", "1.5"
      ),
      getcwd() . "/var/cache/lib/perl/Reporter.pm",
      "getPath returned okay for Reporter.pm" );
  `rm -fr var/cache`;

=end testing

=cut
#-----------------------------------------------------------------------------#
sub storePackage {
  my ($self, $name, $filename, $version, $installpath, $perms, $dependencies,
      $tmpfilename) =
    validate_pos(
      @_, $SELF_PARAM_REQ, SCALAR, SCALAR, SCALAR, SCALAR,
          SCALAR|UNDEF, SCALAR|UNDEF, SCALAR
    );

  return $self->getReporterCache()->storePackage( 
    $name, $filename, $version, $installpath, $perms, $dependencies,$tmpfilename
  );
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _createScheduler( $scheduler_class )
#
# Return a new scheduler object based on the scheduler class name
#
# Arguments:
#
# scheduler_class   A string containing the name of a scheduler class
#
# Returns: 
#
# A new scheduler object 
#

=begin testing

  use Inca::ReporterManager;
  use Inca::ReporterManager::ReporterCache;

  # create scheduler object
  my $scheduler = new Inca::ReporterManager();
  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  $scheduler->setReporterCache( $rc );

  # test for sequential
  my $scheduler_object = $scheduler->_createScheduler(
                           "Inca::ReporterManager::Scheduler::Sequential" );
  isa_ok( $scheduler_object, "Inca::ReporterManager::Scheduler::Sequential",
          '_createScheduler returns sequential object' );

  # test for cron
  $scheduler_object = $scheduler->_createScheduler(
                           "Inca::ReporterManager::Scheduler::Cron" );
  isa_ok( $scheduler_object, "Inca::ReporterManager::Scheduler::Cron",
          '_createScheduler returns cron object' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _createScheduler {
  my ( $self, $scheduler_class ) = 
     validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  eval "require $scheduler_class";
  if ( $@ ) {
    $self->{logger}->error( 
      "Unable to find scheduler class '$scheduler_class': $@"
    );
    return undef;
  }
  my $new_scheduler = $scheduler_class->new( 
    undef, # no scheduler args now
    $self->getCheckPeriod(),
    $self->getCredentials(),
    [$self->getDepots()],
    $self->getDepotTimeout(),
    $self->getId(),
    $self->getReporterCache(),
    $self->getRimPath(),
    $self->getSuspend(),
    $self->getTmpDirectory()
  );
  $new_scheduler->setLogger( $self->{logfile} ) if ( defined $self->{logfile} );
  $self->{logger}->info( "Added scheduler object $scheduler_class" );
  return $new_scheduler;
}

#-----------------------------------------------------------------------------#
# _getScheduler( $scheduler )
#
# Return a scheduler object based on the scheduler type requested for the
# reporter group
#
# Arguments:
#
# scheduler  A string containing the name of a reporter scheduler (e.g.,
#            sequential, cron)
#
# Returns: 
#
# A scheduler object for the reporter group.
#

=begin testing

  tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
  tie *STDERR, 'Catch', '_STDERR_' or die $!;

  use Inca::Logger;
  use Inca::ReporterManager;
  use Inca::ReporterManager::ReporterCache;

  Inca::Logger->screen_init( "ERROR" );

  # test for sequential
  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  my $scheduler = new Inca::ReporterManager();
  $scheduler->setReporterCache( $rc );
  my $scheduler_object = $scheduler->_getScheduler( "sequential" );
  isa_ok( $scheduler_object, "Inca::ReporterManager::Scheduler::Sequential" );

  # test for non-existent scheduler
  $scheduler_object = $scheduler->_getScheduler( "notsched" );
  is( $scheduler_object, undef, 'fails when not a scheduler' );
  like( $_STDERR_, qr/Unknown scheduler type/, "warns correctly when not a scheduler" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _getScheduler {
  my ( $self, $scheduler ) = 
     validate_pos( @_, $SELF_PARAM_REQ, $ALPHANUMERIC_PARAM_REQ );

  # check to see if we have the scheduler instance already
  if ( exists $self->{schedulers}->{$scheduler} ) {
    return $self->{schedulers}->{$scheduler};
  }

  # otherwise, we need to create one
  my $scheduler_class = $self->_getSchedulerClassName( $scheduler );
  my $new_scheduler = $self->_createScheduler( $scheduler_class );
  if ( ! defined $new_scheduler ) {
    $self->{logger}->error( 
      "Unknown scheduler type '$scheduler' found for reporter group"
    );
    return undef;
  }
  # store for later use
  $self->{schedulers}->{$scheduler} = $new_scheduler;
  return $new_scheduler;
}

#-----------------------------------------------------------------------------#
# _getSchedulerClassName( $scheduler_id )
#
# Return the name of the scheduler class given a scheduler identifier. 
#
# Arguments:
#
# scheduler_id   A string containing the identifier of the scheduler.
#
# Returns: 
#
# A string containing the name of the scheduler sub class
#

=begin testing

  use Inca::ReporterManager;

  is( Inca::ReporterManager->_getSchedulerClassName('sequential'),
      "Inca::ReporterManager::Scheduler::Sequential",
      '_getSchedulerClassName returns correct class for sequential' );
  is( Inca::ReporterManager->_getSchedulerClassName('cron'),
      "Inca::ReporterManager::Scheduler::Cron",
      '_getSchedulerClassName returns correct class for cron' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _getSchedulerClassName {
  my ( $self, $scheduler_id ) = 
     validate_pos( @_, $SELF_PARAM_REQ, $ALPHANUMERIC_PARAM_REQ );

  my $this_classname = __PACKAGE__;
  return $this_classname . "::Scheduler::" . ucfirst( $scheduler_id );
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

Does not recognize storage policies yet.

=cut
