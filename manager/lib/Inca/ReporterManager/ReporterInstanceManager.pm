package Inca::ReporterManager::ReporterInstanceManager;

################################################################################

=head1 NAME

Inca::ReporterManager::ReporterInstanceManager - Manages reporter execution 

=head1 SYNOPSIS

=for example begin

  use Inca::ReporterManager::ReporterInstanceManager;
  my $rim = new Inca::ReporterManager::ReporterInstanceManager();
  my $sc = new Inca::Config::Suite::SeriesConfig();
  $sc->setPath( "var/reporter-packages/bin/cluster.compiler.gcc.version-3" );
  $sc->setName( "cluster.compiler.gcc.version" );
  $sc->setVersion( "3" );
  $sc->setContext( "cluster.compiler.gcc.version -verbose=1 -log=3" );
  my $credentials = { cert => "t/certs/client_ca1cert.pem",
                      key => "t/certs/client_ca1keynoenc.pem", 
                      passphrase => undef,
                      trusted => "t/certs/trusted" };
  $rim = new Inca::ReporterManager::ReporterInstanceManager();
  $rim->setSeriesConfig( $sc );
  $rim->setId( "resourceA" );
  $rim->setCredentials( $credentials );
  $rim->setCheckPeriod(1);
  $rim->setPath( "t/stream_report" );
  $rim->setDepots( "incas://localhost:$port" );
  $rim->setReporterCache( $rc );
  my $success = $rim->runReporter();

=for example end

=head1 DESCRIPTION

The Reporter Instance Manager (RIM) is responsible for launching a reporter
instance and monitoring its execution.  Specifically, it forks/execs the 
reporter and then monitors its system usage (CPU time, wall clock time, and
memory) and if it exceeds a specified limit set for either wall clock time,
CPU time, or memory, it will kill the reporter and formulate an error report.
Otherwise upon reporter exit, the RIM will gather stderr, stdout, and usage
statistics and send that to the first depot in its depot list.  The depot
will then get passed to all interested parties.

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
use Inca::Constants qw(:all);
use Inca::IO;
use Inca::Logger;
use Inca::Net::Protocol::Statement;
use Inca::Process;
use Inca::ReporterManager::ReporterCache;
use Inca::Validate qw(:all);
use Inca::Net::Client;
use Inca::GridProxy;

# Perl standard
use Carp;
use Cwd;
use File::Basename;
use File::Spec;
use Socket qw(:crlf);

#=============================================================================#
# Global Vars
#=============================================================================#

our $ERROR_PREFIX = "Internal Inca error";
our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ = { isa => "Inca::ReporterManager::ReporterInstanceManager"};

my $CHECK_PERIOD_PARAM_OPT = 
  { %{$POSITIVE_INTEGER_PARAM_OPT}, default => $DEFAULT_CHECK_PERIOD };
my $DEPOT_TIMEOUT_PARAM_OPT = 
  { %{$POSITIVE_INTEGER_PARAM_OPT}, default => $DEFAULT_DEPOT_TIMEOUT};
my $DEFAULT_TMP_DIR = "/tmp";
my $TMPFILE_TEMPLATE = "inca.rm.$$";
my $PROXY_MACRO = "@.incaProxy@";
 
#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( %Options )

Class constructor which returns a new 
Inca::ReporterManager::ReporterInstanceManager object.  The constructor may be
called with any of the following attributes.

=over 2 

B<Options>:

=over 15

=item id

The resource identifier supplied by the reporter agent that the reporter
manager will use to identify itself to the reporter depot.

=item config

An object of type Inca::Config::Suite::SeriesConfig which contains information
about the reporter to execute.

=item checkPeriod

A positive integer indicating the period in seconds of which to check the
reporter for exceeding resource limits [default: 5]

=item depotTimeout 

A positive integer indicating the period in seconds of which to time out
the connection to the depot [default: 120]

=item depotURIs

A list of depot uris.  The report will be sent to the first depot in the list.
If the first depot is unreachable, the next depots in the list will be tried.

=item reporterCache 

An object of type Inca::ReporterManager::ReporterCache which is used to map
reporter uris to a local path.

=item credentials

A reference to a hash array containing the credential information.

=item tmpDir

A string containing a path to a temporary file space that Inca can use while
executing reporters

=back

=back

=begin testing

  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;
  lives_ok { new Inca::ReporterManager::ReporterInstanceManager() } 
           'object created';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless $self, $class;

  # defaults
  $self->{logger} = Inca::Logger->get_logger( $class );
  $self->setTmpDirectory( $DEFAULT_TMP_DIR );

  # class variables
  my %options = validate( @_, { agent => $SCALAR_PARAM_OPT,
                                checkPeriod => $CHECK_PERIOD_PARAM_OPT,
                                config => $SERIESCONFIG_PARAM_OPT,
                                credentials => $HASHREF_PARAM_OPT,
                                depotTO => $DEPOT_TIMEOUT_PARAM_OPT,
                                depotURIs => $ARRAYREF_PARAM_OPT,
                                id => $SCALAR_PARAM_OPT,
                                reporterCache => $REPORTER_CACHE_PARAM_OPT,
                                suspend => $SUSPEND_PARAM_OPT,
                                tmpDir => $SCALAR_PARAM_OPT
                              } );

  $self->setAgent( $options{agent} ) if exists $options{agent};
  $self->setCheckPeriod($options{checkPeriod}) if exists $options{checkPeriod};
  $self->setCredentials($options{credentials}) if exists $options{credentials};
  $self->setDepots( @{$options{depotURIs}} ) if exists $options{depotURIs}; 
  $self->setDepotTimeout( $options{depotTO} ) if exists $options{depotTO}; 
  $self->setId( $options{id} ) if exists $options{id}; 
  $self->setSeriesConfig( $options{config} ) if exists $options{config};
  $self->setSuspend( $options{suspend} ) if exists $options{suspend};
  $self->setReporterCache( $options{reporterCache} ) 
    if exists $options{reporterCache};
  $self->setTmpDirectory( $options{tmpDir} ) if exists $options{tmpDir}; 

  $self->{archiveDir} = File::Spec->catfile
    ( $self->getTmpDirectory(), "archive" );
  mkdir( $self->{archiveDir} ) if ! -d $self->{archiveDir};
  return $self;
}

#-----------------------------------------------------------------------------#

=head2 checkSuspendCondition( )

Check to see if any of the conditions for high load warrant suspending 
execution of this reporter.  If true will return the load condition that
is true.  Otherwise, if no high load conditions are true, will return undef.
Currently, only load can only be specified by uptime load.

=over 2

B<Returns>:

A string containing the load condition is true or undef is high load is
not detected.

=back

=begin testing

  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;

  my $rim = new Inca::ReporterManager::ReporterInstanceManager();
  is( $rim->checkSuspendCondition(), undef, "no suspend when no condition" );
  $rim->setSuspend( "load1>15" );
  is( $rim->checkSuspendCondition(), undef, "no suspend when no load" );
  $rim->setSuspend( "load1>0.000001" );
  is( $rim->checkSuspendCondition(), 'load1>0.000001', "suspend when load" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub checkSuspendCondition {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return undef if ! defined $self->{suspend};

  my $uptime = `uptime`;
  if ( ! defined $uptime ) {
    $self->{logger}->error( "Error: unable to run uptime" );
    return undef;
  }
  my ($load1,$load5,$load15) = 
    $uptime =~ /load averages*: ([\d\.]+),*\s+([\d\.]+),*\s+([\d\.]+)/;
  my $suspendNow = eval( '$' . $self->{suspend} );
  if ( ! defined $suspendNow ) {
    $self->{logger}->error( "Error: unable to evaluate load" );
    return undef;
  } 
  if ( $suspendNow ) {
    return $self->{suspend};
  } else {
    return undef;
  }
}

#-----------------------------------------------------------------------------#

=head2 getAgent( )

Retrieve the uri of the Inca agent to contact in order to retrive a proxy 
credential to use for the reporter.

=over 2

B<Returns>:

A string containing the uri of the Inca agent to contact to retrieve a 
proxy credential.

=back

=cut
#-----------------------------------------------------------------------------#
sub getAgent {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{agent};
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

Retrieve the destination depots that are used to send the report on completion.

=over 2

B<Returns>:

An array of strings containing uris to depots [host:port, ...]

=back

=cut
#-----------------------------------------------------------------------------#
sub getDepots {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  @{$self->{depots}};
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
reporter uris to a local path.

=back

=cut
#-----------------------------------------------------------------------------#
sub getReporterCache {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{reporter_cache};
}

#-----------------------------------------------------------------------------#

=head2 getSeriesConfig( )

Retrieve the series config that will be executed.

=over 2

B<Returns>:

An object of type Inca::Config::Suite::SeriesConfig which contains information
about the reporter to execute.

=back

=cut
#-----------------------------------------------------------------------------#
sub getSeriesConfig {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{config};
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

=head2 hasAgent( )

Returns true if an agent uri has been specified and false otherwise.

=over 2

B<Returns>:

True if an agent uri has been specified and false otherwise.

=back

=begin testing

  use Inca::ReporterManager::ReporterInstanceManager();
  my $rim = new Inca::ReporterManager::ReporterInstanceManager();
  ok( ! $rim->hasAgent(), 'hasAgent for false' );
  $rim->setAgent( "incas://inca.sdsc.edu:6233" );
  ok( $rim->hasAgent(), 'hasAgent for true' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasAgent {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return ( exists $self->{agent} && defined $self->{agent} );
}

#-----------------------------------------------------------------------------#

=head2 hasCredentials( )

Return true if credentials have been specified; otherwise return false.

=over 2

B<Returns>:

A boolean indicating true if credentials have been specified and false if they
has not.

=back

=begin testing

  use Inca::ReporterManager::ReporterInstanceManager();
  my $rim = new Inca::ReporterManager::ReporterInstanceManager();
  ok( ! $rim->hasCredentials(), 'hasCredentials for false' );
  my $credentials = { cert => 't/cert.pem', key => 't/key.pem',
                      passphrase => 'secret', trusted => 't/trusted' };
  $rim->setCredentials( $credentials );
  ok( $rim->hasCredentials(), 'hasCredentials for true' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasCredentials {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return exists $self->{credentials};
}

#-----------------------------------------------------------------------------#

=head2 hasSeriesConfig( )

Return true if a series config has been specified; otherwise return
false.

=over 2

B<Returns>:

Returns true if a series config has been specified and false if it has not.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Inca::ReporterManager::ReporterInstanceManager();
  my $rim = new Inca::ReporterManager::ReporterInstanceManager();
  ok( ! $rim->hasSeriesConfig(), 'hasSeriesConfig for false' );
  my $sc = new Inca::Config::Suite::SeriesConfig();
  $rim->setSeriesConfig( $sc );
  ok( $rim->hasSeriesConfig(), 'hasSeriesConfig for true' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasSeriesConfig {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return exists( $self->{'config'} ) && defined( $self->{'config'} );
}

#-----------------------------------------------------------------------------#

=head2 runReporter( )

Contact the reporter administrator, retrieve the local path to the
reporter, and execute it.

B<Returns:>

Returns false if there was a problem either retrieving the reporter, running it,
or sending it to the depot.  Otherwise returns true for success.

=begin testing

  untie *STDOUT;
  untie *STDERR;
  use Cwd;
  use File::Spec;
  use Inca::Logger;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::Scheduler;
  use Inca::Net::MockAgent;
  use Inca::Net::MockDepot;

  Inca::Logger->screen_init( "FATAL" );

  my $DEPOT = "file://" . File::Spec->catfile( getcwd(), "depot.tmp.$$" );
  my $PORT = 8519;
  my $RETURN_CODE = 1;
  my $VERIFY = 1;

  sub runConfig {
    my $desc = shift;
    my $sc = shift;
    my @regexs = @_;

    my $rim = Inca::ReporterManager::ReporterInstanceManager->_createTestRM();
    if ( exists $ENV{INCA_TEST_SUSPEND} ) {
      $rim->setSuspend( $ENV{INCA_TEST_SUSPEND} );
    }
    $rim->setDepots( $DEPOT );
    $rim->setSeriesConfig( $sc );
    $rim->setAgent( "incas://localhost:$PORT" ) if $desc =~ /proxy/;
    my $success = $rim->runReporter();
    if ( $VERIFY ) {
      is( $success, $RETURN_CODE, "runReporter($desc) returned $RETURN_CODE" ); 
    }
    my $depot = new Inca::Net::MockDepot;
    if ( $VERIFY ) {
      ok($depot->readReportsFromFile("./depot.tmp.$$", 0), "$desc report read");
      is($depot->{lastTargetResource}, undef, "target resource is undef" );
    }
    my @reports = $depot->getReports();
    for my $regex ( @regexs ) {
      like( $reports[0], qr/$regex/ms, "$desc report matches $regex" );
    }
    return $reports[0];
  }

  #-------------------------------------------
  # simple execution to file
  #-------------------------------------------
  my $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "echo_report" );
  $sc->setNiced( 1 );
  $sc->setContext( "sh -c \"echo_report\"" );
  $sc->setLimits( { "wallClockTime" => 10,
                    "cpuTime" => 10,
                    "memory" => 20 } );
  my $report = runConfig( "basic", $sc, "^REPORT resourceA\nsh -c \".*" );
  my ($wall_secs) = $report =~ /^wall_secs=(.*)/m;
  cmp_ok( $wall_secs, "<=", 1, "wall secs is reasonable" );

  #-------------------------------------------
  # test suspend is working
  #-------------------------------------------
  $ENV{INCA_TEST_SUSPEND} = "load1>0.00000001";
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "echo_report" );
  $report = runConfig( "suspend", $sc, "high load" );
  delete $ENV{INCA_TEST_SUSPEND};

  #-------------------------------------------
  # reporter that uses INSTALL_DIR
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "repo_report" );
  $sc->setContext( $sc->getName() . ' -verbose="1" -help="no"' );
  my $perl5lib = $ENV{PERL5LIB};
  delete $ENV{INSTALLDIR};
  runConfig( "INSTALL_DIR", $sc, ".*<dir>" . getcwd() . "\/t<\/dir>.*" );
  delete $ENV{PYTHONPATH};
  runConfig( "PYTHONPATH", $sc, ".*<dir>" . getcwd() . "\/t/lib/python<\/dir>.*" );
  delete $ENV{PERL5LIB};
  runConfig( "PERL5LIB", $sc, ".*<dir>" . getcwd() . "\/t/lib/perl<\/dir>.*" );
  delete $ENV{LD_LIBRARY_PATH};
  runConfig( "LD_LIBRARY_PATH", $sc, ".*<dir>" . getcwd() . "\/t/lib<\/dir>.*" );

  $ENV{PYTHONPATH} = "somedir";
  runConfig
    ("PYTHONPATH", $sc, ".*<dir>".getcwd() . "\/t/lib/python:somedir<\/dir>.*");
  $ENV{PERL5LIB} = "somedir";
  runConfig
    ( "PERL5LIB", $sc, ".*<dir>" . getcwd() . "\/t/lib/perl:somedir<\/dir>.*" );
  $ENV{LD_LIBRARY_PATH} = "somedir";
  runConfig
    ("LD_LIBRARY_PATH", $sc, ".*<dir>" . getcwd() . "\/t/lib:somedir<\/dir>.*");
  $ENV{PERL5LIB} = $perl5lib;
  
  #-------------------------------------------
  # reporter with env var as arg
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "var_reporter" );
  $sc->setContext( $sc->getName() . ' -verbose="1" -help="no" -args="$HOME"' );
  runConfig( 
    "env var in arg", $sc, 
    "<completed>true<\/completed>.*",
    "<value>-verbose=1 -help=no -args=$ENV{HOME}<\/value>.*"
  );

  #-------------------------------------------
  # reporter with non-existent env var as arg
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "var_reporter" );
  $sc->setContext( $sc->getName() . ' -verbose="1" -help="no" -args="$BLAH"' );
  runConfig( 
    "non-existent env var in arg", $sc, 
    "<completed>true<\/completed>.*",
    "<value>-verbose=1 -help=no -args=<\/value>.*"
  );
 
  #-------------------------------------------
  # not a valid reporter
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "not_a_reporter" );
  $sc->setContext( $sc->getName() . ' -verbose="1" -help="no"' );
  $sc->addArgument( "verbose", "1" );
  $sc->addArgument( "help", "no" );
  my $errorPrefix =
    $Inca::ReporterManager::ReporterInstanceManager::ERROR_PREFIX;
  runConfig( 
    "invalid", $sc, 
    "<completed>false<\/completed>.*",
    "<errorMessage>$errorPrefix:  Error, no report produced to stdout.*<\/errorMessage>.*",
    "<name>verbose<\/name>.*"
  );

  #-------------------------------------------
  # faulty reporter
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "bad_reporter" );
  runConfig( 
    "faulty", $sc, 
    "<completed>false<\/completed>.*",
    "<errorMessage>$errorPrefix:  Exec of reporter .*<\/errorMessage>.*"
  );

  #-------------------------------------------
  # badly written reporter
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "bad_reporter2" );
  runConfig( 
    "poor", $sc, 
    "<completed>false<\/completed>.*",
    "<errorMessage>.*no report.*"
  );

  #-------------------------------------------
  # no report but there is stderr
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "no_report" );
  runConfig( 
    "null", $sc, 
    "<completed>false<\/completed>.*",
    "<errorMessage>.*no report.*"
  );

  #-------------------------------------------
  # report to stdout with extra junk before the xml 
  #-------------------------------------------
  $sc =Inca::ReporterManager::Scheduler->_createTestConfig("xtraStdout_report");
  runConfig( "extra stdout", $sc, "^<?" );

  #-------------------------------------------
  # report to stdout with no proxy
  #-------------------------------------------
  ok( -f "$ENV{HOME}/.inca.myproxy.info", "proxy information found" );
  open( FD, "<$ENV{HOME}/.inca.myproxy.info" ) ||
    fail( "Unable to open proxy info" );
  my ($mp_hostname, $mp_port, $mp_username, $mp_password);
  while( <FD> ) {
    eval $_;
  }
  close FD;
  
  SKIP: {
    skip "proxy info not found", 1 unless -f "$ENV{HOME}/.inca.myproxy.info";
    local $SIG{CHLD} = 'DEFAULT';

    my $agent = new Inca::Net::MockAgent( $PORT, "ca1", "t/certs/trusted" );
    my $proxy_reporter = Inca::ReporterManager::Scheduler->_createTestConfig(
      "grid.middleware.globus.unit.proxy"
    );
    $proxy_reporter->setContext( $proxy_reporter->getName() . " -verbose=1 -log=3" );
    if ( fork() ) {
      runConfig( "proxy", $proxy_reporter, "completed>false" );
    } else {
      sleep 2;
      $agent->accept(
        "PROXY" => [ "ERROR No proxy information registered for resource" ],
        OK => []
      );
      exit;
    }
    $agent->stop();
  };

  #-------------------------------------------
  # report to stdout with proxy
  #-------------------------------------------
  SKIP: {
    skip "myproxy not found", 2 unless -f "$ENV{HOME}/.inca.myproxy.info";
    local $SIG{CHLD} = 'DEFAULT';
    `grid-proxy-destroy >/dev/null 2>&1`;
    `grid-proxy-info >/dev/null 2>&1`;
    cmp_ok( $?, "!=", 0, "grid-proxy-info returned error" );

    my $agent = new Inca::Net::MockAgent( $PORT, "ca1", "t/certs/trusted" );
    my $proxy_reporter = Inca::ReporterManager::Scheduler->_createTestConfig(
      "grid.middleware.globus.unit.proxy"
    );
    $proxy_reporter->setContext( "env X509_USER_PROXY=@.incaProxy@ " .
      $proxy_reporter->getName() . " -verbose=1 -log=3" );
    if ( fork() ) {
      runConfig( "proxy", $proxy_reporter, "completed>true" );
    } else {
      sleep 2;
      $agent->accept(
        "PROXY" => [ "HOSTNAME $mp_hostname", "PORT $mp_port", "USERNAME $mp_username", "PASSWORD $mp_password", "LIFETIME 1"],
        "OK" => []
      );
      exit;
    }
    $agent->stop();
  };

  #-------------------------------------------
  # report to stdout with proxy (myproxy server down)
  #-------------------------------------------
  $RETURN_CODE = 0;
  $agent = new Inca::Net::MockAgent( $PORT, "ca1", "t/certs/trusted" );
  $proxy_reporter = Inca::ReporterManager::Scheduler->_createTestConfig(
    "grid.middleware.globus.unit.proxy"
  );
  $proxy_reporter->setContext( $proxy_reporter->getName() . " -verbose=1 -log=3" );
  if ( fork() ) {
    runConfig( "proxy down", $proxy_reporter, "errorMessage>Internal" );
  } else {
    sleep 2;
    $agent->accept(
      "PROXY" => [ "HOSTNAME fake_host", "PORT $mp_port", "USERNAME $mp_username", "PASSWORD $mp_password", "LIFETIME 1"],
      "OK" => []
    );
    exit;
  }
  $agent->stop();
  $RETURN_CODE = 1;

  #-------------------------------------------
  # Limits
  #-------------------------------------------
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "stream_report" );
  $sc->setLimits( { "cpuTime" => 1, "memory" => 100 } );
  $report = runConfig( 
    "long - limits exceeded", $sc, 
    "<completed>false<\/completed>.*",
    "<errorMessage>.*exceeded usage limits.*"
  );
  my ($memory) = $report=~ /^memory_mb=(.*)/m;
  cmp_ok( $memory, ">=", 45, "runReporter(limits) - memory is reasonable" );

  #-------------------------------------------
  # more resource intensive reporter to file
  #-------------------------------------------
  $sc->setLimits( 
    { "wallClockTime" => 100, "cpuTime" => 10, "memory" => 100 } 
  );
  $report = runConfig( "long", $sc, "completed>true" );
  ($memory) = $report =~ /^memory_mb=(.*)/m;
  cmp_ok( $memory, ">=", 45, "memory is reasonable" );

  #-------------------------------------------
  # more resource intensive reporter to ssl server
  #-------------------------------------------
  my @clientCredentials = ( { cert => "t/certs/client_ca1cert.pem",
                              key => "t/certs/client_ca1keynoenc.pem", 
                              passphrase => undef,
                              trusted => "t/certs/trusted" },
                            { cert => "t/certs/client_ca1cert.pem",
                              key => "t/certs/client_ca1key.pem", 
                              passphrase => "test",
                              trusted => "t/certs/trusted" }
                          );
  $depot = new Inca::Net::MockDepot();
  my $pid;
  if ( $pid = fork() ) {
    my $numReports = $depot->readReportsFromSSL(
      2, $PORT, "ca1", "t/certs/trusted"
    );
    is( $numReports, 2, "2 reports received from mock depot" );
    is( $depot->{lastTargetResource}, "resourceA", "target resource received" );
  } else {
    $VERIFY = 0;
    for ( my $i = 0; $i <= $#clientCredentials; $i++ ) {
      $sc = Inca::ReporterManager::Scheduler->_createTestConfig("echo_report");
      $sc->setTargetResource( "resourceA" );
      $DEPOT = "incas://localhost:$PORT";
      runConfig( "quick ssl", $sc );
    }
    exit;
  }

=end testing

=cut
#-----------------------------------------------------------------------------#
sub runReporter {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  my $reporter_executed_n_recorded = 0;

  my $cmdline = $self->_createCmdLine();

  my $condition = $self->checkSuspendCondition();
  if ( defined $condition ) {
    $self->{logger}->warn
      ( "Execution of reporter skipped due to high load: $condition" );
    $self->{logger}->warn( "  $cmdline" );
    $self->_generateErrorReport
      ( "Execution of reporter skipped due to high load:  $condition", 0 );
    $self->_sendReportToDepots();
    return 1;
  }

  $self->{logger}->info( 
    "Begin executing " . $self->getSeriesConfig()->getContext() 
  );
  $self->{logger}->info( "  $cmdline" );
  if ( ! defined $cmdline ) {
    $self->_generateErrorReport
      ( "Unable to retrieve local copy of reporter for execution", 1 );
    my $success = $self->_sendReportToDepots();
    if ( ! $success ) {
      $self->{logger}->error( "  Unable to send report to available depots" );
    }
    $self->{logger}->info( "End " . $self->getSeriesConfig()->getContext() );
    return 0;
  }

  # tell the reporter where it can find the top level repository cache
  # to find any dependencies it may have
  $ENV{INSTALL_DIR} = $self->getReporterCache()->getLocation();
  $ENV{PYTHONPATH} = File::Spec->catfile
    ( $self->getReporterCache()->getLocation(), "lib", "python" ) .
    ( exists $ENV{PYTHONPATH} ? ":$ENV{PYTHONPATH}" : "" );
  $ENV{PERL5LIB} = File::Spec->catfile
    ( $self->getReporterCache()->getLocation(), "lib", "perl" ) .
    ( exists $ENV{PERL5LIB} ? ":$ENV{PERL5LIB}" : "" );
  $ENV{LD_LIBRARY_PATH} = File::Spec->catfile
    ( $self->getReporterCache()->getLocation(), "lib" ) .
    ( exists $ENV{LD_LIBRARY_PATH} ? ":$ENV{LD_LIBRARY_PATH}" : "" );

  # continue executing the reporter
  my $process = new Inca::Process( tmpDir => $self->getTmpDirectory() );
  if ( ! defined $process ) {
    $self->_generateErrorReport
      ( "Unable to create temp files", 1 );
    my $success = $self->_sendReportToDepots();
    if ( ! $success ) {
      $self->{logger}->error( "  Unable to send report to available depots" );
    }
    $self->{logger}->info( "End " . $self->getSeriesConfig()->getContext() );
    return 0;
  }
  $process->setCheckPeriod( $self->getCheckPeriod() );
  if ( $self->getSeriesConfig()->hasLimits() ) {
    $process->setLimits( $self->getSeriesConfig()->getLimits() );
  }
  $process->setCommandLine( $cmdline );
  my $success;
  if ( $self->hasAgent() && defined (my $params = $self->_requiresProxy()) ) {
    my ($tfh, $tempfile) = Inca::IO->tempfile( "$TMPFILE_TEMPLATE.p" ); 
    if ( ! defined $tempfile ) {
      $self->_generateErrorReport
        ("Unable to create temp file for storing proxy for reporter exec", 1);
      my $success = $self->_sendReportToDepots();
      if ( ! $success ) {
        $self->{logger}->error( "  Unable to send report to available depots" );
      }
      $self->{logger}->info( "End " . $self->getSeriesConfig()->getContext() );
      return 0;
    }
    if ( $self->_fetchProxy( $params, $tempfile ) ) {
      $cmdline =~ s/$PROXY_MACRO/$tempfile/g;
      $self->{logger}->debug( "cmdline with proxy file '$cmdline'" );
      $process->setCommandLine( $cmdline );
      $success = $process->run(); 
    } else {
      $self->_generateErrorReport
        ( "Unable to fetch proxy for reporter execution", 1 );
      my $success = $self->_sendReportToDepots();
      if ( ! $success ) {
        $self->{logger}->error( "  Unable to send report to available depots" );
      }
      $self->{logger}->info( "End " . $self->getSeriesConfig()->getContext() );
      return 0;
    }
    unlink $tempfile if -e $tempfile;
  } else {
    $success = $process->run(); 
  }
  if ( ! $success ) {
    if ( $process->hasExceededLimits() ) {
      $self->{usage} = $process->getUsage()->toString();
      $self->_generateErrorReport
        ( "Reporter exceeded usage limits --  " . $process->getExceededLimits(),
          0 );
      $reporter_executed_n_recorded = 1;
    } else { # must have been error
      $self->_generateErrorReport
        ( "Unable to execute reporter: error '$!'", 1 );
    }
  } else {
    $reporter_executed_n_recorded = 1;
    $self->{stdout} = $process->getStdout();
    # strip off an leading junk to the report
    if ( defined $self->{stdout} ) {
      $self->{stdout} =~ s/^.*?(<!--.*?-->|<\?.*?\?>|<[A-Za-z:_].*?>)/$1/s;
    }
    $self->{stderr} = $process->getStderr();
    if ( ! defined $self->{stdout} || $self->{stdout} eq "" ) {
      if ( ! defined $self->{stderr} || $self->{stderr} eq "" ) {
        if ( $? ne 0 ) {
          $self->_generateErrorReport("Exec of reporter failed: error '$!'", 1);
        } else {
          $self->_generateErrorReport
            ("Error running reporter: no stdout, stderr, nor system error", 1);
        }
      } else {
        $self->_generateErrorReport
          ( "Error, no report produced to stdout.  " .
            "The following was printed to stderr: \n" .  $self->{stderr}, 1 );
      }
    } else {
      $self->{usage} = $process->getUsage()->toString();
    }
  }

  $success = $self->_sendReportToDepots();

  if ( ! $success ) {
    $reporter_executed_n_recorded = 0;
    $self->{logger}->error( "  Unable to send report to available depots" );
  }

  $self->{logger}->info( "End " . $self->getSeriesConfig()->getContext() );
  return $reporter_executed_n_recorded;
}

#-----------------------------------------------------------------------------#

=head2 setAgent( $uri )

Set the uri of the Inca agent to contact in order to retrive a proxy 
credential to use for the reporter.

=over 2

B<Arguments>:

=over 13

=item uri

A string containing the uri of the Inca agent to contact to retrieve a 
proxy credential.

=back

=back

=begin testing

  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;

  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                            agent => "cuzco.sdsc.edu:8233" );
  my $agent = $rim->getAgent();
  is( $agent, "cuzco.sdsc.edu:8233", 'Set agent works from constructor' );
  $rim->setAgent( "inca.sdsc.edu:8235" );
  $agent = $rim->getAgent();
  is( $agent, "inca.sdsc.edu:8235", 'Set depots works from function' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setAgent {
  my ( $self, $uri ) =
     validate_pos( @_, $SELF_PARAM_REQ, $URI_PARAM_REQ );
                       

  $self->{agent} = $uri;
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

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::Constants qw(:defaults);

  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                                                           checkPeriod => 3 );
  is( $rim->getCheckPeriod(), 3, 'set check period from constructor' );
  $rim->setCheckPeriod( 10 );
  is( $rim->getCheckPeriod(), 10, 'set check period' );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( );
  is( $rim->getCheckPeriod(), $DEFAULT_CHECK_PERIOD,
     'default check period is $DEFAULT_CHECK_PERIOD');

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

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;

  my $credentials = { cert => 't/cert.pem', key => 't/key.pem',
                      passphrase => 'pwd', trusted => 't/trusted' };
  my $credentials_new = { cert => 't/new/cert.pem', key => 't/key.pem',
                      passphrase => undef, trusted => 't/trusted' };
  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                  credentials => $credentials );
  ok( eq_hash($rim->getCredentials(), $credentials), 
          'set credentials worked from constructor' );
  $rim->setCredentials( $credentials_new );
  ok( eq_hash($rim->getCredentials(), $credentials_new), 
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

  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;

  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                            depotURIs => ["cuzco.sdsc.edu:8234"] );
  my @depots = $rim->getDepots();
  is( $depots[0], "cuzco.sdsc.edu:8234", 'Set depots works from constructor' );
  $rim->setDepots( qw(inca.sdsc.edu:8235 inca.sdsc.edu:8236) );
  @depots = $rim->getDepots();
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

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::Constants qw(:defaults);

  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                                                           depotTO => 3 );
  is( $rim->getDepotTimeout(), 3, 'set depot timeout from constructor' );
  $rim->setDepotTimeout( 10 );
  is( $rim->getDepotTimeout(), 10, 'set depot timeout' );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( );
  is( $rim->getDepotTimeout(), $DEFAULT_DEPOT_TIMEOUT, 
      'default check period is $DEFAULT_DEPOT_TIMEOUT' );

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

  use Inca::ReporterManager::ReporterInstanceManager;

  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                                                           id => "resourceA" );
  is( $rim->getId(), "resourceA", 'set id from constructor' );
  $rim->setId( "resourceB" );
  is( $rim->getId(), "resourceB", 'set id' );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( );
  is( $rim->getId(), undef, 'default id is undef' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setId {
  my ( $self, $id ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{id} = $id;
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
reporter uris to a local path.

=back

=back

=begin testing

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;
  use Cwd;

  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  my $rc_new = new Inca::ReporterManager::ReporterCache( "/tmp" );
  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                  reporterCache => $rc );
  is( $rim->getReporterCache->getLocation(), getcwd() . "/t",
          'set reporter admin worked from constructor' );
  $rim->setReporterCache( $rc_new );
  is( $rim->getReporterCache->getLocation(), "/tmp",
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

=head2 setSeriesConfig( $config )

Set the series config to be executed.

=over 2

B<Arguments>:

=over 13

=item config

An object of type Inca::Config::Suite::SeriesConfig which contains information
about the reporter to execute.

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;

  my $config1 = new Inca::Config::Suite::SeriesConfig( path => "path1" );
  my $config2 = new Inca::Config::Suite::SeriesConfig( path => "path2" );
  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
    config => $config1
  );
  is ( $rim->getSeriesConfig()->getPath(), 'path1', 
       'set config worked from constructor' );
  $rim->setSeriesConfig( $config2 );
  is ( $rim->getSeriesConfig()->getPath(), 'path2', 'set/get config worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setSeriesConfig {
  my ( $self, $config ) = 
    validate_pos( @_, $SELF_PARAM_REQ, $SERIESCONFIG_PARAM_REQ );

  $self->{config} = $config;
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
    new Inca::ReporterManager::ReporterInstanceManager( suspend => 'load1>5' );
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

  use Inca::ReporterManager::ReporterInstanceManager;
  use Test::Exception;

  my $rim = new Inca::ReporterManager::ReporterInstanceManager(); 
  is( $rim->getTmpDirectory(), "/tmp", 'default tmp set' );
  $rim->setTmpDirectory( "/scratch" );
  is( $rim->getTmpDirectory(), "/scratch", 'set/getTmpDirectory work' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setTmpDirectory {
  my ( $self, $path ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{tmpdir} = $path;
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _acceptReport()
#
# Create a MockAgent (cause the API is more conducive for this) to accept
# a depot report.
#
# Note: for testing purposes only.
#-----------------------------------------------------------------------------#
sub _acceptReport {
  my ( $class, $port, $response, $delay ) = @_;

  eval { require Inca::Net::MockAgent; };
  my $depot = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
  my $responseCmd = [ undef ];
  $responseCmd =  [ "$response" ] if defined $response;
  sleep( $delay ) if defined $delay;
  my ($conn, $numStmts, @responses ) = $depot->accept(
    "REPORT" => [],
    "STDOUT" => [],
    "SYSUSAGE" => $responseCmd
  );
  $conn->close();
}

#-----------------------------------------------------------------------------#
# _createCmdLine( )
#
# Create the command-line string for executing the reporter.  If execution
# priority is set, then the process is run with nice.
#
# Returns: 
#
# A string containing the command to run the reporter with its arguments
# or undefined if there is an error locating the reporter in the local cache.
#
=begin testing

  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::ReporterCache;
  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;
  use Cwd;

  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  my $sc = new Inca::Config::Suite::SeriesConfig();
  $sc->setName( "echo_report" );
  $sc->setPath( "t/echo_report" );
  my $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                  config => $sc,
                  reporterCache => $rc );
  is( $rim->_createCmdLine(), undef,
      '_createCmdLine returns undef when context not specified' );
  $sc->setContext( $sc->getName() );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                  config => $sc,
                  reporterCache => $rc );
  is( $rim->_createCmdLine(), "t/echo_report",
          '_createCmdLine works for simple case' );
  $sc->setNiced( 1 );
  $rim = new Inca::ReporterManager::ReporterInstanceManager( 
                  config => $sc,
                  reporterCache => $rc );
  is( $rim->_createCmdLine(), "nice t/echo_report",
          '_createCmdLine works for simple case with nice' );
  
  # try set context
  $sc->setNiced( 0 );
  $sc->setContext( 
    'PATH=bin:${PATH}; ' . $sc->getName() . '; rm -f /tmp/blah' 
  );
  is( $rim->_createCmdLine(), 
      'PATH=bin:${PATH}; nice t/echo_report; rm -f /tmp/blah',
       '_createCmdLine works for context' );
  
  $sc->setContext( 
    'PATH=bin:${PATH}; ' . $sc->getName() . ' -name1="value1" -name1="value2"; rm -f /tmp/blah' 
  );
  is( $rim->_createCmdLine(), 
      'PATH=bin:${PATH}; nice t/echo_report -name1="value1" -name1="value2"; rm -f /tmp/blah',
      '_createCmdLine works for with args' );

  # special chars in reporter name
  $sc->setName( "grid.security++.unit" );
  $sc->setContext( 
    'PATH=bin:${PATH}; ' . $sc->getName() . ' -name1="value1" -name1="value2"; rm -f /tmp/blah' 
  );
  is( $rim->_createCmdLine(), 
      'PATH=bin:${PATH}; nice t/echo_report -name1="value1" -name1="value2"; rm -f /tmp/blah',
      '_createCmdLine works for special chars in reporter name' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _createCmdLine {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  # construct cmd line
  my $path = $self->getSeriesConfig()->getPath(); 
  if ( $self->getSeriesConfig()->hasContext() ) {
    if ( $self->getSeriesConfig()->hasNiced() ) {
      $path = "nice " . $path;
    }
    my $context = $self->getSeriesConfig()->getContext();
    my $name = $self->getSeriesConfig()->getName();
    $context =~ s/\Q$name\E/$path/;
    return $context; 
  } else {
    $self->{logger}->error( 
      "Reporter spec missing context field for " . 
      $self->getSeriesConfig()->getPath() 
    );
    return undef;
  }
}

#-----------------------------------------------------------------------------#
# _createTestRM()
#
# Create a sample Inca::ReporterManager::ReporterInstanceManager object.
#
# Note: for testing purposes only.
#-----------------------------------------------------------------------------#
sub _createTestRM {
  my $credentials = { 
    cert => "t/certs/client_ca1cert.pem",
    key => "t/certs/client_ca1keynoenc.pem", 
    passphrase => undef,
    trusted => "t/certs/trusted" 
  };
  my $rc = new Inca::ReporterManager::ReporterCache( "t",
    errorReporterPath => "bin/inca-null-reporter" 
  );
  my $rim = new Inca::ReporterManager::ReporterInstanceManager();
  $rim->setId( "resourceA" );
  $rim->setCredentials( $credentials );
  $rim->setCheckPeriod(1);
  $rim->setReporterCache( $rc );
  return $rim;
}

#-----------------------------------------------------------------------------#
# _fetchProxy( $tempfile )
#
# Fetch a proxy to the specified temporary file and set the X509_USER_PROXY
# environment variable.
#
# Arguments:
#
#  tempfile  A string indicating the path to store the proxy file
#-----------------------------------------------------------------------------#
sub _fetchProxy {
  my ( $self, $params, $tempfile ) = 
    validate_pos( @_, $SELF_PARAM_REQ, HASHREF, SCALAR );

  my $proxy; 
  eval {
    $proxy = new Inca::GridProxy();
  };
  if ( $@ ) {
    $self->{logger}->error("Problem finding myproxy executables");
    return 0;
  }
  if( ! $proxy->getProxy( $params, $tempfile ) && 
      ( ! -f $tempfile || -z $tempfile ) ) {
    $self->{logger}->error( "Unable to retrieve proxy from myproxy server" );
    return 0;
  }
  $self->{logger}->debug( "Proxy written to $tempfile" );
  $ENV{X509_USER_PROXY} = $tempfile;
  # explicitly clear from memory
  for my $param ( qw(dn hostname lifetime password port username) ) {
    $params->{$param} = undef;
  }
  return 1;
}

#-----------------------------------------------------------------------------#
# _generateErrorReport( $error_msg )
#
# Called after a reporter error has been detected.  Will generate a
# a special error report that can be sent to the depot.
#
# Arguments:
#
#  error_msg  A string indicating the reason for the reporter error.
#

=begin testing

  untie *STDOUT;
  untie *STDERR;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::Scheduler;
  use Inca::Config::Suite::SeriesConfig;
  use File::Spec;
  use Cwd;

  my $errorPrefix =
    $Inca::ReporterManager::ReporterInstanceManager::ERROR_PREFIX;
  my $sc = Inca::ReporterManager::Scheduler->_createTestConfig( "echo_report" );
  $sc->setLimits( { "wallClockTime" => 10,
                    "cpuTime" => 10,
                    "memory" => 20 } );
  my $rim = new Inca::ReporterManager::ReporterInstanceManager->_createTestRM();
  $rim->setDepots( "file://" . File::Spec->catfile(getcwd(), "depot.tmp.$$") );
  $rim->setSeriesConfig( $sc );
  my $success = $rim->_generateErrorReport( "An Error Message", 0 );
  like($rim->{stdout}, qr/^<rep:report/m, "_generateErrorReport: report found");
  like( $rim->{stdout}, qr/<errorMessage>An Error Message/m, 
        "_generateErrorReport - error found" );

  $success = $rim->_generateErrorReport( "An Error Message", 1 );
  like
    ($rim->{stdout}, qr/^<rep:report/m, "_generateErrorReport - report found");
  like( $rim->{stdout}, qr/<errorMessage>$errorPrefix:  An Error Message/m,
        "_generateErrorReport - error found" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _generateErrorReport {
  my ( $self, $error_msg, $internal ) = validate_pos
    ( @_, $SELF_PARAM_REQ, SCALAR, $BOOLEAN_PARAM_REQ );

  # get temp file options
  my $new_error_msg = $internal ? "$ERROR_PREFIX:  $error_msg" : $error_msg;
  my ($tfh, $tempfile) = Inca::IO->tempfile(
    "$TMPFILE_TEMPLATE.out", $self->getTmpDirectory() 
  ); 
  if ( ! defined $tfh ) {
    $self->{logger}->error( 
      "Unable to create temp file for creating error report"
    );
    return;
  }
  print $tfh $new_error_msg;

  my $cmdline = $self->getReporterCache()->getErrorReporterPath();
  $cmdline .= " --error_message_stdin=1";
  $cmdline .= " --name=" . $self->getSeriesConfig()->getName();
  $cmdline .= " --version=" . $self->getSeriesConfig()->getVersion();
  $cmdline .= " --workingDir=" . getcwd();
  $cmdline .= " --reporterPath=" . $self->getSeriesConfig()->getPath();
  $cmdline .= " --completed=false";
  if ( $self->getSeriesConfig()->hasArguments() ) {
    $cmdline .= " -- " . $self->getSeriesConfig()->getArgumentsAsCmdLine();
  }
  $cmdline .= " < $tempfile";
  my $report = `$cmdline`;
  close $tfh;
  unlink $tempfile;
  if ( $report eq "" ) {
    $self->{logger}->error( "  Unable to generate error report for " . 
                            $self->getSeriesConfig()->getName() . ": $!" );
  }
  $self->{logger}->info( "  Generating error report for " . 
                         $self->getSeriesConfig()->getName() . ": $error_msg ");
  $self->{stdout} = $report;
  $self->{stderr} = "";
  if ( ! exists $self->{usage} ) {
    my $zero_usage = new Inca::Process::Usage();
    $zero_usage->zeroValues();
    $self->{usage} = $zero_usage->toString();
  }
}

#-----------------------------------------------------------------------------#
# _requiresProxy()
#
# If a reporter requires a proxy, ideally will generate one using myproxy
# information we get from the agent.  Otherwise we  there is already a valid 
# proxy # on the system (less ideal).
#
# Returns: A reference to a hash containing myproxy arguments if a we need
# to fetch a proxy or undef otherwise.  
#-----------------------------------------------------------------------------#
sub _requiresProxy {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  my $proxy; 
  eval {
    $proxy = new Inca::GridProxy();
  };
  if ( $@ ) {
    $self->{logger}->error("Problem finding myproxy executables");
    return undef;
  }

  my %credentials;
  # grab credentials if they're there (and needed)
  %credentials = %{ $self->getCredentials() } if $self->hasCredentials();
  my $params = $proxy->requestProxyInformation(
    $self->getId(),
    $self->getAgent(),
    %credentials
  );
  if ( ! defined $params ) {
    return undef;
  }
  return $params;
}

#-----------------------------------------------------------------------------#
# _sendReportToDepot( $uri )
#
# Send the report to the given depot $uri.
#
# Arguments:
#
#  uri   The URI of the depot in the form inca[s]://<host>:<port>.
#
# Returns: 
#
# Returns success if able to connect to depot and successfully send report
# or error report (if something is wrong with the original report).  Returns 0
# otherwise;

=begin testing

  untie *STDOUT;
  tie *STDERR, 'Catch', '_STDERR_' or die $!;

  use Inca::IO;
  use Inca::ReporterManager::ReporterCache;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::Scheduler;
  use Inca::Net::Protocol::Statement;
  use Inca::Net::MockAgent;
  use Inca::Config::Suite::SeriesConfig;

  use Inca::Logger;

  Inca::Logger->screen_init( "ERROR" );

  my $PORT = 6329;

  sub sendReport {
    my $depotTimeout = shift;

    sleep( 2 );
    my $sc = Inca::ReporterManager::Scheduler->_createTestConfig("echo_report");
    my $rim = Inca::ReporterManager::ReporterInstanceManager->_createTestRM();
    $rim->setSeriesConfig( $sc );
    $rim->setDepotTimeout( $depotTimeout) if defined $depotTimeout;
    $rim->{stdout} = "stdout";
    $rim->{usage} = "usage";
    return $rim->_sendReportToDepot(  "incas://localhost:$PORT"  );
  }

  my $pid;
  if ( $pid = fork() ) {
    my $success = sendReport(10);
    is( $success, 1, "report accepted" );
    $_STDERR_ = "";

    $success = sendReport(10);
    is( $success, 0, "error report from depot" );
    like( $_STDERR_, qr/This is an error/, "error logged when depot error");
    like( $_STDERR_, qr/Error writing to depot/, 
          "message logged when report not sent to the depot" );

    $_STDERR_ = "";
    $success = sendReport(10);
    is( $success, 1, "error report issued" );
    like(  $_STDERR_, qr/Report rejected by Inca depot/, 
          "message logged when error report issued");

    $_STDERR_ = "";
    $success = sendReport(10);
    is( $success, 0, "error report rejected" );
    like( $_STDERR_, qr/Internal error: problem/,
          "message logged when depot rejects error report" );

    $_STDERR_ = "";
    $success = sendReport(10);
    is( $success, 0, "error sending error report" );
    like( $_STDERR_, qr/Detected depot error/,
          "message logged when depot errors on error report" );
  } else {
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport( $PORT );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR This is an error message from the depot" );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR Invalid report XML" );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport($PORT );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR Invalid report XML" );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR Invalid report XML" );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR Invalid report XML" );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR java.net.SocketTimeoutException: Read timed " );
    exit;
  }

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _sendReportToDepot {
  my ( $self, $uri ) = validate_pos( @_, $SELF_PARAM_REQ, $URI_PARAM_REQ );

  # grab credentials if they're there (and needed)
  my %credentials;
  %credentials = %{ $self->getCredentials() } if $self->hasCredentials();

  my $client = new Inca::Net::Client( $uri, %credentials ); 
  return 0 if ! defined $client;
  $self->{logger}->info( "  Sending report to $uri" );
  my $success = $self->_sendReportToStream( $client );
  $client->close();
  if ( $success == 1 ) {
    return 1;
  } elsif ( $success == 0 ) {
    # if bad report, an error report will be in stdout with the reason so we
    # try send this to the depot
    sleep(1);
    $client = new Inca::Net::Client( $uri, %credentials ); 
    return 0 if ! defined $client;
    $self->{logger}->info( "  Sending error report to $uri" );
    my $success = $self->_sendReportToStream( $client );
    $self->{logger}->error("  SUCCESS: $success");
    $client->close();
    if ( $success == 1 ) {
      return 1;
    } elsif ( $success == 0 ) {
      $self->{logger}->error("  Internal error: problem sending error report");
      return 0; 
    } else {
      $self->{logger}->error("  Detected depot error");
      return 0;
    }
  } else {
    return 0;
  }
}

#-----------------------------------------------------------------------------#
# _sendReportToDepots( $process )
#
# Called from runReporter after the reporter has finished running to handle
# sending data to the depot.  We send the report to the first accessible
# depot in our list. 
#
# Arguments:
#
#  process    An object of type Inca::Process that has just finished 
#             executing a reporter
#
# Returns: 
#
# Returns 1 if the report was sent to a depot; otherwise return 0. 

=begin testing

  untie *STDOUT;
  tie *STDERR, 'Catch', '_STDERR_' or die $!;

  use Inca::IO;
  use Inca::ReporterManager::ReporterCache;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::Scheduler;
  use Inca::Net::Protocol::Statement;
  use Inca::Net::MockAgent;
  use Inca::Net::MockDepot;
  use Inca::Config::Suite::SeriesConfig;

  use Inca::Logger;

  Inca::Logger->screen_init( "INFO" );

  my $PORT = 6329;

  sub sendReportToDepots {
    my $depotTimeout = shift;
    my @ports = @_;

    my @uris = map( "incas://localhost:$_", @ports );

    sleep( 2 );
    my $sc = Inca::ReporterManager::Scheduler->_createTestConfig("echo_report");
    my $rim = Inca::ReporterManager::ReporterInstanceManager->_createTestRM();
    $rim->setDepots( @uris );
    $rim->setSeriesConfig( $sc );
    $rim->setDepotTimeout( $depotTimeout) if defined $depotTimeout;
    $rim->{stdout} = "stdout";
    $rim->{usage} = "usage";
    `rm -f $rim->{archiveDir}/*`;
    return $rim->_sendReportToDepots();
  }

  my $pid;
  if ( $pid = fork() ) {
    my $success = sendReportToDepots(10, $PORT);
    is( $success, 1, "report accepted" );
    like( $_STDERR_, qr/Report successfully sent/, "success logged");
    $_STDERR_ = "";

    $success = sendReportToDepots(10, 20000, $PORT);
    is( $success, 1, "report accepted" );
    like( $_STDERR_, qr/Report successfully sent/, "success logged");
    like( $_STDERR_, qr/Unable to send report/, 
          "error logged when first depot not available");
    $_STDERR_ = "";

    $success = sendReportToDepots(10, $PORT);
    is( $success, 0, "error sending report" );
    like($_STDERR_, qr/Unable to send report/, "error logged when depot error");
    my @files = glob( "/tmp/archive/*" );
    is( $#files, 0, "report archived" );
 
    $success = sendReportToDepots(5, $PORT);
    is( $success, 0, "timeout of send worked" );
    like($_STDERR_, qr/exceeded/, "error logged when depot timeout exceeded");
    @files = glob( "/tmp/archive/*" );
    is( $#files, 0, "report archived" );

    sleep 6; # wait for mock depot to close


  } else {
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport( $PORT );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport( $PORT );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR This is an error message from the depot" );

    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR This is an error message from the depot", 8 );

    exit;
  }

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _sendReportToDepots {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  my $success = 0;

  # search for depot
  my $timeout = $self->getDepotTimeout();
  for my $uri ( $self->getDepots() ) {
    eval {
      local $SIG{ALRM} = sub { die "time exceeded" };
      alarm( $timeout ); 
      $self->{logger}->debug( "  Attempting to connect to depot $uri" );
      $success = $self->_sendReportToDepot( $uri );
      alarm(0);
    };
    if ( $@ ) {
      $success = 0;
      $self->{logger}->error("Send to depot '$uri' exceeded $timeout secs; $@");
    } elsif ( ! $success ) {
      $self->{logger}->error( "Unable to send report to depot '$uri'" );
    } else {
      $self->{logger}->info( "Report successfully sent to depot '$uri'" );
      last; # success!
    } 
  }
  if ( ! $success ) {
    my $archiveFile = File::Spec->catfile
      ( $self->{archiveDir}, "r" . time() . "-" . $$);
    $self->{logger}->info( "Archiving report to $archiveFile" );
    my $archiveClient = new Inca::Net::Client( "file://$archiveFile" );
    $self->_sendReportToStream( $archiveClient );
    $self->{logger}->info( "Report successfully archived to $archiveFile");
    $archiveClient->close();
  } 

  return $success;
}

#-----------------------------------------------------------------------------#
# _sendReportToStream( $process, $client )
#
# Called from runReporter after the reporter has finished running to handle
# sending data to the depot.  This method is called from _sendReportToDepot
# (i.e., we have an open connection to a depot already and now just need
# to send data).
#
# Arguments:
#
#  process    An object of type Inca::Process that has just finished 
#             executing a reporter
#
#  client     An object of type Inca::Net::Client which is the open connection
#             we can write the report to.
#
# Returns: 
#
# Returns 1 if the report was sent to a depot successfully.  Returns 0 if 
# received error and that error was related to a problem with the report XML
# in which case we log the report and issue an error report back to the depot.  
# Returns -1 if received error from the depot but we believe the report
# itself is okay and should eventually be reported back to the depot.

=begin testing

  untie *STDOUT;
  tie *STDERR, 'Catch', '_STDERR_' or die $!;

  use Inca::IO;
  use Inca::ReporterManager::ReporterCache;
  use Inca::ReporterManager::ReporterInstanceManager;
  use Inca::ReporterManager::Scheduler;
  use Inca::Net::Protocol::Statement;
  use Inca::Net::MockAgent;
  use Inca::Config::Suite::SeriesConfig;

  use Inca::Logger;

  Inca::Logger->screen_init( "ERROR" );

  my $PORT = 6329;

  sub runRIM {
    my $depotTimeout = shift;

    sleep( 2 );
    my $sc = Inca::ReporterManager::Scheduler->_createTestConfig("echo_report");
    my %credentials = ( cert => "t/certs/client_ca1cert.pem",
                        key => "t/certs/client_ca1keynoenc.pem", 
                        passphrase => undef,
                        trusted => "t/certs/trusted" );
    my $rim = Inca::ReporterManager::ReporterInstanceManager->_createTestRM();
    $rim->setDepots( "incas://localhost:$PORT" );
    $rim->setSeriesConfig( $sc );
    $rim->setDepotTimeout( $depotTimeout) if defined $depotTimeout;
    $rim->{stdout} = "stdout";
    $rim->{usage} = "usage";
    my $client = new Inca::Net::Client("incas://localhost:$PORT", %credentials);
    ok( defined $client, "client started" );
    my $success = $rim->_sendReportToStream( $client );
    $client->close();
    return $success;
  }

  my $pid;
  if ( $pid = fork() ) {
    my $success = runRIM(10);
    is( $success, 1, "report accepted" );
    $_STDERR_ = "";

    $success = runRIM(10);
    is( $success, 0, "invalid report xml -- error report sent" );
    like( $_STDERR_, qr/Report rejected by Inca depot/, 
          "error logged when report rejected");

    $_STDERR_ = "";
    $success = runRIM(10);
    is( $success, -1, "error received by depot" );
    like( $_STDERR_, qr/Error writing to depot/, 
          "error logged when error issued by depot");

    $_STDERR_ = "";
    $success = runRIM(10);
    is( $success, -1, "unknown response from depot" );
    like( $_STDERR_, qr/Unknown response from the depot/, 
          "message logged when unknown depot response" );

  } else {
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport( $PORT );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR Invalid report XML" );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "ERROR java.net.SocketTimeoutException: Read timed" );
    Inca::ReporterManager::ReporterInstanceManager->_acceptReport
      ( $PORT, "STARS shine at night" );
    exit;
  }

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _sendReportToStream {
  my ( $self, $client ) = validate_pos(@_, $SELF_PARAM_REQ, $CLIENT_PARAM_REQ);

  # send that a report is coming
  my $reportId = $self->getId() . "\n" . $self->getSeriesConfig()->getContext()
                 . "\n";
  $reportId .= $self->getSeriesConfig()->getTargetResource if
    $self->getSeriesConfig()->hasTargetResource();
  $client->writeStatement( "REPORT", $reportId );

  # send stderr from reporter if it exists
  if ( defined $self->{stderr} && $self->{stderr} ne "" ) {
    $self->{stderr} =~ s/$CRLF/\n/g;
    $client->writeStatement( "STDERR", $self->{stderr} );
  }

  # send stdout from reporter
  $self->{stdout} =~ s/$CRLF/\n/g;
  $client->writeStatement( "STDOUT", $self->{stdout} );

  # send reporter usage information
  $client->writeStatement( "SYSUSAGE", $self->{usage} );

  # depot will either close socket when it accepts a report or send an
  # error response on error
  my $stream = $client->{stream};
  my $depot_response = <$stream>;
  if ( ! defined $depot_response or $depot_response eq "" or
       $depot_response =~ /^OK/ ) {
    return 1;
  } else {
    my $depot_stmt = new Inca::Net::Protocol::Statement();
    $depot_stmt->setStatement( $depot_response );
    if ( $depot_stmt->getCmd() eq "ERROR" ) {
      if ( $depot_stmt->getData() =~ /report XML/ ) {
        $self->_generateErrorReport
          ("report rejected by Inca depot '" . $depot_stmt->getData() . "'", 1);
        $self->{logger}->error
          ( "  Report rejected by Inca depot: " .  $depot_stmt->getData() );
        return 0;
      } else {
        $self->{logger}->error
          ( "  Error writing to depot: " .  $depot_stmt->getData() );
        return -1;
      } 
    } else {
      $self->{logger}->error( 
        "  Unknown response from the depot: " .  $depot_stmt->getStatement()
      );
      return -1;
    }
  }
}


1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

None so far.

=cut
