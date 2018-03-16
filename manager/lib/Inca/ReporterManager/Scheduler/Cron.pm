package Inca::ReporterManager::Scheduler::Cron;
use base qw(Inca::ReporterManager::Scheduler);
@ISA = ( "Inca::ReporterManager::Scheduler" );

################################################################################

=head1 NAME

Inca::ReporterManager::Scheduler::Cron - Executes reporters on a periodic schedule

=head1 SYNOPSIS

=for example begin

  use Inca::ReporterManager::Scheduler::Cron;
  use Inca::Config::Suite::SeriesConfig;
  use Inca::ReporterManager::ReporterCache;
  use Cwd;
  use File::Spec;
  my $file_depot = "file://" . File::Spec->catfile(getcwd(), "depot.$$");
  my $cron = { min => "0-59/2" }; # execute every 2 mins
  my $sched = new Inca::ReporterManager::Scheduler::Cron( 
                    1,
                    [ $file_depot ],
                    120,
                    "resourceA",
                    new Inca::ReporterManager::ReporterCache( 't',
                      errorReporterPath => "bin/inca-null-reporter" 
                    ),
                    undef, 
                    $cron );
  $sched->start();
  my $config = new Inca::Config::Suite::SeriesConfig();
  $config->setName( "echo_report" );
  $config->setVersion( "1" );
  $config->setPath( getcwd() . "/t/echo_report" );
  $config->setContext( $config->getName() );
  $sched->submit( 'add', $config );
  # ...
  $sched->stop();

=for example end

A scheduler for the reporter manager which executes reporters on a periodic
schedule.  It takes as its parameter a cron specification and uses the 
Schedule::Cron module from CPAN to handle the scheduling.

=cut 
################################################################################

#=============================================================================#
# Usage
#=============================================================================#

# pragmas
use strict;
use warnings;
use vars qw($VERSION);

# Perl standard
use Carp;
use Cwd;
use File::Spec;
use POSIX ":sys_wait_h";

# CPAN
use Date::Manip;
use Schedule::Cron;
use XML::Simple;

# Inca
use Inca::Config;
use Inca::Constants qw(:params);
use Inca::Logger qw(:all);
use Inca::ReporterManager::ReporterCache;
use Inca::ReporterManager::ReporterInstanceManager;
use Inca::Validate qw(:all);

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ = { isa => __PACKAGE__ };
my $ACTION_PARAM_REQ = { regex => qr/add|delete/ };
my $KILL_WAIT = 1;
my $MAX_LOGFILE_AGE = 14;
sub _execCronEntry;
sub _logCronEntry;
sub _nullCronEntry;

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

  use Inca::ReporterManager::Scheduler::Cron;
  use Test::Exception;
  untie *STDOUT;
  untie *STDERR;

  dies_ok { new Inca::ReporterManager::Scheduler::Cron() } 
           'object dies with no args';

  lives_ok { Inca::ReporterManager::Scheduler::Cron->_createTestCron() }
           'object created with undef scheduler_args';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = $class->SUPER::new( @_ );

  bless ($self, $class);

  my $log_method = sub {
    my ($level,$msg) = @_;
    my $DBG_MAP = { 0 => $INFO, 1 => $WARN, 2 => $ERROR };

    $self->{logger}->log($DBG_MAP->{$level},$msg);
  };
  
  $self->{cron} = new Schedule::Cron( \&_execCronEntry, log => $log_method );
  $self->{cwd} = getcwd();
  $self->{cron}->add_entry( "5 0 * * *", \&_logCronEntry, $self ); # manage logs

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

  return 1;
}

#-----------------------------------------------------------------------------#

=head2 isRunning( )

Check if the cron scheduler is running.

=over 2

B<Returns>:

Returns true if the cron scheduler is running and false otherwise.

=back

=cut
#-----------------------------------------------------------------------------#
sub isRunning {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  if ( exists $self->{'cronpid'} && defined $self->{'cronpid'} ) {
    return kill(0, $self->{'cronpid'});
  } else {
    return 0;
  }
}

#-----------------------------------------------------------------------------#

=head2 start( )

Starts cron scheduler up.

=over 2

B<Returns>:

Returns true if there were no errors starting the scheduler; returns false
otherwise.

=back

=begin testing

  use Inca::ReporterManager::Scheduler::Cron;

  my $sched = Inca::ReporterManager::Scheduler::Cron->_createTestCron();
  ok( $sched->start(), "scheduler started" );
  ok( $sched->isRunning(), "scheduler start verified" );
  ok( $sched->stop(), "scheduler stopped" );
  ok( ! $sched->isRunning(), "scheduler stop verified" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub start {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  # *Special note*: do not remove quotes around cronpid.  On TG aix machine
  # copper, the value of cronpid is undef w/o quotes...we suspect some
  # wierd compiler problem with the perl on that machine.
  $self->{'cronpid'} = $self->{cron}->run( detach => 1 ); 

  # print out the current schedule
  my @entries = $self->{cron}->list_entries();
  $self->{logger}->debug( "Begin current cron schedule: " . (scalar(@entries)-1) . " entries" );
  for my $entry ( @entries ) {
    next if scalar(@{$entry->{args}}) <= 1; # ignore non-RIM cron jobs
    $self->{logger}->debug( 
      $entry->{time} . " " . $self->{rim_path} . " " .  
      join(" ", $self->_getRIMCmdArgs( $entry->{args}->[1] )) 
    );
  }
  $self->{logger}->debug( "End current cron schedule" ); 
  return 1;
}

#-----------------------------------------------------------------------------#

=head2 stop( )

Stops cron scheduler.

=over 2

B<Returns>:

Returns true if there were no errors starting the scheduler; returns false
otherwise.

=back

=cut
#-----------------------------------------------------------------------------#
sub stop {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  if ( $self->isRunning() ) {
    my $pid;
    do {
      my $procs_signalled = kill( 'KILL', -$self->{'cronpid'} );
      sleep $KILL_WAIT;
      $pid = waitpid( $self->{'cronpid'}, WNOHANG);
    } until $pid != 0;
    if ( $pid == $self->{'cronpid'} || $pid == -1 ) {
      $self->{'cronpid'} = undef;
      return 1;
    } else {
      $self->{'cronpid'} = undef;
      return 0;
    }
  }
  return 0;
}

#-----------------------------------------------------------------------------#

=head2 submit( $action, @configs )

Accepts an action (currently only 'add') and any number of reporters.  These 
reporters will then be executed in a sequential order.

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

Returns true if at least one reporter is executed; otherwise returns false.

=back

=begin testing

  untie *STDOUT;
  untie *STDERR;
  use Inca::Logger;
  use Inca::Net::MockDepot;
  use Inca::ReporterManager::Scheduler;
  use Inca::ReporterManager::Scheduler::Cron;

  Inca::Logger->screen_init( "ERROR" );

  my $sched;

  sub runSeriesConfigs {
    my $sleep = shift;
    my $scs = shift;
    my $crons = shift;

    $sched = Inca::ReporterManager::Scheduler::Cron->_createTestCron();
    for ( my $i = 0; $i < scalar(@{$scs}); $i++ ) {
      $scs->[$i]->setSchedulerName( "cron" );
      $scs->[$i]->setSchedulerArgs( $crons->[$i] );
      ok( $sched->submit('add', $crons->[$i], $scs->[$i]), 
          'submit returned positive status');
    }
    ok( $sched->start(), "Scheduler started" );
    my $depot = new Inca::Net::MockDepot;
    my $numReports = $depot->readReportsFromFile( "./depot.tmp.$$", $sleep );
    ok( $sched->stop(), "killed Schedule::Cron" );
    return ( $numReports, $depot->getReports() );
  }

  #---------------------------------------------
  # test quick reporter
  #---------------------------------------------
  my $sc = Inca::ReporterManager::Scheduler->_createTestConfig( 
    "echo_report" 
  );
  my $args = { second => "0-59/5" };
  my ( $numReports, @reports ) = runSeriesConfigs( 20, [$sc], [$args] );  
  cmp_ok( $numReports, ">=", 1, "at least 1 report printed to file" );
  cmp_ok( $numReports, "<=", 4, "no more than 4 reports printed to file" );
  my $procs_leftover = `ps x | grep "(perl)" | wc -l`;
  chomp( $procs_leftover );
  # 2 for grep command and possibly one extra
  cmp_ok($procs_leftover, "<=", 3, "no more than 3 procs found");

  #---------------------------------------------
  # test benchmark reporter
  #---------------------------------------------
  my $start_time = time();
  `t/stream`;
  my $elapsed_time = (time() - $start_time) + 1;
  ok( $elapsed_time, "positive time $elapsed_time for stream" );
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( 
    "stream_report" 
  );
  $sc->setLimits( { 'cpuTime' => 20 } );
  ($numReports, @reports) = runSeriesConfigs( $elapsed_time+5, [$sc], [$args] );
  cmp_ok( $numReports, ">=", 1, "at least 1 report printed to file" );
  cmp_ok( $numReports, "<=", 2, "no more than 2 reports printed to file" );
  for my $report ( @reports ) {
    my ($memory) = $report =~ /^memory_mb=(.*)/m;
    cmp_ok( $memory, ">=", 45, "submit for stream - memory is reasonable" );
  }

  #---------------------------------------------
  # test limits
  #---------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( 
    "stream_report" 
  );
  $sc->setLimits( { 'wallClockTime' => 2 } );
  ($numReports, @reports) = runSeriesConfigs( 10, [$sc], [$args] );
  ok( $reports[0] =~ /.*Reporter exceeded usage limits.*/m, 
      "killed by wall clock time" );
  my ($wall) = $reports[0] =~ /^wall_secs=(.*)/m;
  cmp_ok( $wall, ">=", 2, "limit for stream - wall is reasonable" );

  #---------------------------------------------
  # test mixed
  #---------------------------------------------
  my $sleep = (time() % 20) >= 15 ? 5 : 0;
  sleep $sleep;
  $sched = Inca::ReporterManager::Scheduler::Cron->_createTestCron();
  my $gcc_reporter = Inca::ReporterManager::Scheduler->_createTestConfig( 
    "cluster.compiler.gcc.version" 
  );
  $gcc_reporter->setContext( $gcc_reporter->getName() . ' -verbose="1"' );
  $gcc_reporter->addArgument( "verbose", "1" );
  my $gcc_freq = { second => "0-59/20" };
  my $ssl_reporter = Inca::ReporterManager::Scheduler->_createTestConfig( 
    "cluster.security.openssl.version"
  );
  $ssl_reporter->setContext($ssl_reporter->getName() . ' -verbose="1"' );
  $ssl_reporter->addArgument( "verbose", "1" );
  my $ssl_freq = { second => "2-59/30" };
  ($numReports, @reports) = runSeriesConfigs( 
    61, [$gcc_reporter, $ssl_reporter], [$gcc_freq, $ssl_freq] 
  );
  my @num_gcc_reports = grep( />gcc</, @reports );
  cmp_ok( scalar(@num_gcc_reports), ">=", 2, "at least 2 gcc reports" );
  cmp_ok( scalar(@num_gcc_reports), "<=", 3, "no more than 3 gcc reports" );
  my @num_ssl_reports = grep( />openssl</, @reports );
  cmp_ok( scalar(@num_ssl_reports), ">=", 1, "at least 1 ssl reports" );
  cmp_ok( scalar(@num_ssl_reports), "<=", 2, "no more than 2 gcc reports" );

  #---------------------------------------------
  # delete ssl reporter
  #---------------------------------------------
  ok( $sched->submit( 'delete', $ssl_freq, $ssl_reporter ),
      "delete submit returned positive status" );;
  ok( $sched->start(), "Scheduler restarted" );
  $depot = new Inca::Net::MockDepot;
  ok( 
    $depot->readReportsFromFile( "./depot.tmp.$$", 61 ),
    "submit for stream limits - depot file created"
  );
  ok( $sched->stop(), "killed Schedule::Cron" );
  @num_gcc_reports = grep( />gcc</, $depot->getReports() );
  cmp_ok( scalar(@num_gcc_reports), ">=", 2, "at least 2 gcc reports" );
  cmp_ok( scalar(@num_gcc_reports), "<=", 3, "no more than 3 gcc reports" );
  @num_ssl_reports = grep( />openssl</, $depot->getReports() );
  is( scalar(@num_ssl_reports), 0, "0 ssl reports" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub submit {
  my ( $self, $action, $args, @configs ) = 
    validate_pos( @_, $SELF_PARAM_REQ, 
                      $ACTION_PARAM_REQ, 
                      HASHREF,
                      ($SERIESCONFIG_PARAM_REQ) x (@_ - 3) );

  my $result;
  if ( $action eq $Inca::ReporterManager::Scheduler::ADD ) {
    $result = $self->_addReportersToCron( $args, @configs );
  } elsif ( $action eq $Inca::ReporterManager::Scheduler::DELETE ) {
    $result = $self->_deleteReportersFromCron( $args, @configs );
  } else {
    $self->{logger}->error( 
      "Uknown action '$action' to submit in " . __PACKAGE__ 
    );
    return 0;
  }
  
  return $result;
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _addReporterstoCron( $cron_desc, @configs )
#
# Add the specified reporters to the cron table.
#
# Arguments:
#
#  cron_desc  A reference to a hash array containing cron fields (e.g.,
#             minutes, hours, etc.)
#             executing a reporter
#
#  configs    An array of Inca::Config::Suite::SeriesConfig objects containing
#             the reporters to execute.
#

=begin testing

  untie *STDOUT;
  untie *STDERR;
  use Inca::ReporterManager::Scheduler::Cron;

  my $cron = Inca::ReporterManager::Scheduler::Cron->_createTestCron();
  my @entries = $cron->{cron}->list_entries();
  is( @entries, 1, "number of starting entries correct" );
  my $desc = { min => 5 };
  my $sc = Inca::ReporterManager::Scheduler->_createTestConfig( 
    "echo_report" 
  );
  $cron->_addReportersToCron( $desc, $sc );
  @entries = $cron->{cron}->list_entries();
  is( @entries, 2, "number of entries after add is correct" );
  ok( $entries[1]->{time} eq "5 * * * *", "add reporter - time correct" );
  ok( ref($entries[1]->{args}[0] ) eq "Inca::ReporterManager::Scheduler::Cron", 
      "added reporter to cron - self correct" );
  ok( ref($entries[1]->{args}[1] ) eq "Inca::Config::Suite::SeriesConfig", 
      "added reporter to cron - reporter correct" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _addReportersToCron {
  my ( $self, $cron_desc, @configs ) = 
    validate_pos( @_, $SELF_PARAM_REQ, 
                      HASHREF,
                      ($SERIESCONFIG_PARAM_REQ) x (@_ - 2) );

  my $num_configs_added = 0;
  my $crontime = $self->_convertToCronSyntax( $cron_desc );
  for my $config ( @configs ) {
    my $idx;
    eval {
      $idx = $self->{cron}->add_entry( $crontime, $self, $config );
    };
    if ( $@ ) {
      $self->{logger}->error( "Error adding entry $num_configs_added\n" );
    } else {
      $self->{logger}->info( 
        "Added reporter " . $config->getName() . " to cron $idx: $crontime" 
      );
      $num_configs_added++;
    }
  }
  $self->{logger}->info( "Added $num_configs_added reporters to cron" );
  return $num_configs_added;
}

#-----------------------------------------------------------------------------#
# _convertToCronSyntax( $cron_desc )
#
# Return a string that describes when to execute the cron job using the 
# parameters contained in $cron_desc.
#
# Arguments:
#
#  cron_desc  A reference to a hash array containing cron fields (e.g.,
#             minutes, hours, etc.)
#             executing a reporter
#
# Returns:
# 
# A string containing the time specification for the cron job

=begin testing

  untie *STDOUT;
  untie *STDERR;
  use Inca::ReporterManager::Scheduler::Cron;
  my $tm = { min => "0-59/5" };
  my $cron = Inca::ReporterManager::Scheduler::Cron->_convertToCronSyntax($tm);
  is( $cron, "0-59/5 * * * * ", "minutes converted correctly" ); 
  $tm = { hour => "0-23/5" };
  $cron = Inca::ReporterManager::Scheduler::Cron->_convertToCronSyntax($tm);
  is( $cron, "* 0-23/5 * * * ", "hours converted correctly" ); 
  $tm = { mday => "6" };
  $cron = Inca::ReporterManager::Scheduler::Cron->_convertToCronSyntax($tm);
  is( $cron, "* * 6 * * ", "mday converted correctly" ); 
  $tm = { month => "6" };
  $cron = Inca::ReporterManager::Scheduler::Cron->_convertToCronSyntax($tm);
  is( $cron, "* * * 6 * ", "month converted correctly" ); 
  $tm = { wday => "6" };
  $cron = Inca::ReporterManager::Scheduler::Cron->_convertToCronSyntax($tm);
  is( $cron, "* * * * 6 ", "wday converted correctly" ); 
  $tm = { second => "8" };
  $cron = Inca::ReporterManager::Scheduler::Cron->_convertToCronSyntax($tm);
  is( $cron, "* * * * * 8", "second converted correctly" ); 
  $tm = { min => 0, hour => 4, month => 3, wday => "6" };
  $cron = Inca::ReporterManager::Scheduler::Cron->_convertToCronSyntax($tm);
  is( $cron, "0 4 * 3 6 ", "mixed converted correctly" ); 

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _convertToCronSyntax {
  my ( $self, $cron_desc ) = validate_pos( @_, $SELF_PARAM_REQ, HASHREF );

  my $crontab = ""; 
  foreach my $field ( qw( min hour mday month wday ) ) {
    if ( defined $cron_desc->{$field} ) {
      $crontab = $crontab . $cron_desc->{$field} . " ";
    } else {
      $crontab .= "* ";
    }
  }
  if ( exists $cron_desc->{second} ) {
    $crontab .= $cron_desc->{second};
  }
  return $crontab;
}

#-----------------------------------------------------------------------------#
# _createTestCron( $depot, $creds )
#
# Create a sample cron scheduler object.
#
# Arguments:
#
#  depot  A string containing the name of the depot to use.  If undefined a
#         local file will be used
#
#  creds  A reference to a hash containing the credentials to use if the
#         the depot is a SSL server.  Otherwise will be undefined.
#
# Note:  For testing purposes only.
#-----------------------------------------------------------------------------#
sub _createTestCron {
  my $class = shift;
  my $depot = shift;
  my $creds = shift;

  if ( ! defined $depot ) {
    $depot = "file://" . File::Spec->catfile( getcwd(), "depot.tmp.$$" ); 
  }
  my $rc = new Inca::ReporterManager::ReporterCache( 
    't',
    errorReporterPath => "bin/inca-null-reporter"
  );
  return new Inca::ReporterManager::Scheduler::Cron( 
    undef, 1, $creds, [ $depot ], 120, "resourceA", $rc, 
    "sbin/reporter-instance-manager", undef, "/tmp"
  );
}

#-----------------------------------------------------------------------------#
# _deleteReportersFromCron( $cron_desc, @configs )
#
# Delete the specified reporters with $cron_desc from the cron table.
#
# Arguments:
#
#  cron_desc  A reference to a hash array containing cron fields (e.g.,
#             minutes, hours, etc.)
#             executing a reporter
#
#  configs    An array of Inca::Config::Suite::SeriesConfig objects containing
#             the reporters to execute.
#

=begin testing

  untie *STDOUT;
  untie *STDERR;
  use Inca::ReporterManager::Scheduler;
  use Inca::ReporterManager::Scheduler::Cron;

  my $cron = Inca::ReporterManager::Scheduler::Cron->_createTestCron();
  my @entries = $cron->{cron}->list_entries();
  is( @entries, 1, "log entry exists" );
  my $desc = { min => 5 };
  my $config = Inca::ReporterManager::Scheduler->_createTestConfig( 
    "echo_report" 
  );
  is( $cron->_addReportersToCron($desc, $config), 1,
      "1 reporter added" );
  @entries = $cron->{cron}->list_entries();
  is( scalar(@entries), 2, "number of entries after add correct" );
  is( $cron->_deleteReportersFromCron($desc, $config), 1,
      "1 reporter deleted" );
  @entries = $cron->{cron}->list_entries();
  is( @entries, 1, "number of entries after delete correct" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _deleteReportersFromCron {
  my ( $self, $cron_desc, @configs ) = 
    validate_pos( @_, $SELF_PARAM_REQ, 
                      HASHREF,
                      ($SERIESCONFIG_PARAM_REQ) x (@_ - 2) );

  my $num_configs_deleted = 0;
  my $crontime = $self->_convertToCronSyntax( $cron_desc );
  for my $config ( @configs ) {
    my $idx = -1;
    my $config_found = 0;
    for my $entry ( $self->{cron}->list_entries() ) {
      $idx++;
      next if scalar(@{$entry->{args}}) <= 1; # ignore non-RIM cron jobs
      next if ( ! $entry->{time} eq $crontime );
      my ( $self_entry, $config_entry ) = @{$entry->{args}};
      if ( $config->equals($config_entry) ) {
        $config_found = 1;
        my $result = $self->{cron}->delete_entry( $idx );
        if ( ! defined $result ) {
          $self->{logger}->error( "Error deleting entry $idx from cron" );
        } else {
          $self->{logger}->info( 
            "Deleted reporter " . $config->getName() . 
            " from cron $idx: $crontime" 
          );
          $num_configs_deleted++;
        }
        last;
      }
    }
    if ( ! $config_found ) {
      $self->{logger}->info(  $config->getName() . " @ $crontime not found" );
    }
  }
  $self->{logger}->info( "Deleted $num_configs_deleted reporters from cron" );
  return $num_configs_deleted;
}

#-----------------------------------------------------------------------------#
# _execCronEntry( $config )
#
# Called by Schedule::Cron when a reporter is scheduled to execute.

=begin testing

  untie *STDOUT;
  untie *STDERR;
  use Inca::Net::MockDepot;
  use Inca::ReporterManager::Scheduler;
  use Inca::ReporterManager::Scheduler::Cron;

  my $sched = Inca::ReporterManager::Scheduler::Cron->_createTestCron();
  my $sc = Inca::ReporterManager::Scheduler->_createTestConfig( 
    "echo_report" 
  );
  my $pid;
  if ( $pid = fork() ) {
    waitpid( $pid, 0 );
    my $depot = new Inca::Net::MockDepot;
    ok( $depot->readReportsFromFile( "./depot.tmp.$$", 0 ),
        "_execCronEntry - looks like reporter was printed");
  } else {
    $sched->_execCronEntry( $sc );
  }

  my $port = 6429;
  my @clientCredentials = ( { cert => "t/certs/client_ca1cert.pem",
                              key => "t/certs/client_ca1keynoenc.pem",
                              passphrase => undef,
                              trusted => "t/certs/trusted" },
                            { cert => "t/certs/client_ca1cert.pem",
                              key => "t/certs/client_ca1key.pem",
                              passphrase => "test",
                              trusted => "t/certs/trusted" }
                          );
  for ( my $i = 0; $i <= $#clientCredentials; $i++ ) {
    sleep 5;
    if ( $pid = fork() ) {
      my $depot = new Inca::Net::MockDepot();
      my $numReports = $depot->readReportsFromSSL(
        1, $port, "ca1", "t/certs/trusted"
      );
      is( $numReports, 1, "1 report read from mock depot" );
    } else {
      sleep 2;
      my $sched = Inca::ReporterManager::Scheduler::Cron->_createTestCron(
        "incas://localhost:$port", $clientCredentials[$i]
      );
      $sched->_execCronEntry( $sc );
      exit;
    }
  }

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _execCronEntry {
  my ( $self, $config ) = 
    validate_pos( @_, $SELF_PARAM_REQ, $SERIESCONFIG_PARAM_REQ );

  if ( ! chdir( $self->{cwd} ) ) {
    $self->{logger}->error( 
      "Executing reporter: unable to change to " .  $self->{cwd} 
    );
    return;
  }
  my @args = $self->_getRIMCmdArgs( $config );
  if ( exists $self->{credentials} && exists $self->{credentials}->{passphrase}
       && defined $self->{credentials}->{passphrase} ){
    my ($piperead, $pipewrite);
    pipe $piperead, $pipewrite;
    open( STDIN, "<&" . fileno($piperead) ) || 
      $self->{logger}->error( "cannot redirect stdin: $!" );
    print $pipewrite $self->{credentials}->{passphrase};
    close $pipewrite;
    unshift( @args, "--passphrase" );
  } 
  $self->{logger}->info( $self->{rim_path}, " ",  join(" ", @args) );
  $self->{logger}->info( getcwd() );
  my $cmd = $self->{rim_path} . join( " ", @args );
  exec $self->{rim_path}, @args or 
    $self->{logger}->error( 
      "Error executing " . $self->{rim_path} . " " . join( " ", @args ) .": $!"
    );
}

#-----------------------------------------------------------------------------#
# _logCronEntry( )
#
# A special entry in the cron table to regularly clean out 
# Schedule::Cron doesn't error out when it starts up with no entries)

=begin testing

  use Inca::ReporterManager::Scheduler::Cron;
  use Test::Exception;
  use Inca::Logger;
  use Date::Manip;
  untie *STDOUT;
  untie *STDERR;

  my $today = ParseDate("today");
  my $tmpLog = "/tmp/inca_test_logfile";

  sub createLogFiles {
    my ($start, $end) = @_;
    for ( my $i = $start; $i < $end; $i++ ) {
      my $logdate = DateCalc($today, ParseDateDelta("-" . $i . "d") );
      my $logdateString = UnixDate($logdate, "%Y-%m-%d");
      `touch "$tmpLog-$logdateString"`;
    }
  }

  Inca::Logger->file_init( 'INFO', $tmpLog );
  my $sched = Inca::ReporterManager::Scheduler::Cron->_createTestCron();
  my $logger = Inca::Logger->get_logger();
  $logger->info( "something" );

  # create 25 log files and check that 15 remain
  createLogFiles( 0, 25 );
  my @files = glob("$tmpLog*");
  is( scalar(@files), 25, "25 files created" );
  $sched->_logCronEntry();
  @files = glob("$tmpLog*");
  is( scalar(@files), 15, "15 files left" );
  unlink @files;

  # check that 14 files remain if space in dates 
  createLogFiles( 0, 5 );
  createLogFiles( 14, 36 );
  @files = glob("$tmpLog*");
  is( scalar(@files), 27, "25 files created" );
  $sched->_logCronEntry();
  @files = glob("$tmpLog*");
  is( scalar(@files), 14, "14 files left" );
  unlink @files;

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _logCronEntry {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  $self->{logger}->debug( "Checking log files" );
  for my $appender ( values(%{Inca::Logger->appenders}) ) {
    if (  ref($appender->{appender}) eq "Log::Dispatch::File::Rolling" ) {
      my $dir = $appender->{appender}->{_path};
      my $file = $appender->{appender}->{_name};
      next if ! defined $dir || ! defined $file;
      $file =~ s/\.log// if $file =~ /\.log$/;
      my $logPattern = File::Spec->catfile($dir, $file) . '*';
      my @files = glob( $logPattern );
      for my $file ( sort @files ) {
        my ($date) = $file =~ m/(\d{4}-\d{2}-\d{2})/;
        my $filedate = ParseDate($date);
        my $expiredate = 
          DateCalc(ParseDate("today"),ParseDateDelta("-".$MAX_LOGFILE_AGE."d"));
        if ( Date_Cmp($filedate, $expiredate) == -1 ) {
          my @files = glob( $logPattern );
          $self->{logger}->info( "Deleting log file $file" );
          unlink $file;
        }
        @files = glob( $logPattern );
        last if scalar(@files) <= $MAX_LOGFILE_AGE;
      }
    }
  }
}

#-----------------------------------------------------------------------------#
# _nullCronEntry( )
#
# A null function to be called by the null cron entry (needed so that 
# Schedule::Cron doesn't error out when it starts up with no entries)
#-----------------------------------------------------------------------------#
sub _nullCronEntry {
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
