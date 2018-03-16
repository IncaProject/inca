#!/usr/bin/perl -w

use Test::More 'no_plan';

package Catch;

sub TIEHANDLE {
	my($class, $var) = @_;
	return bless { var => $var }, $class;
}

sub PRINT  {
	my($self) = shift;
	${'main::'.$self->{var}} .= join '', @_;
}

sub OPEN  {}    # XXX Hackery in case the user redirects
sub CLOSE {}    # XXX STDERR/STDOUT.  This is not the behavior we want.

sub READ {}
sub READLINE {}
sub GETC {}
sub BINMODE {}

my $Original_File = 'lib/Inca/ReporterManager/Scheduler/Cron.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 162 lib/Inca/ReporterManager/Scheduler/Cron.pm

  use Inca::ReporterManager::Scheduler::Cron;
  use Test::Exception;
  untie *STDOUT;
  untie *STDERR;

  dies_ok { new Inca::ReporterManager::Scheduler::Cron() } 
           'object dies with no args';

  lives_ok { Inca::ReporterManager::Scheduler::Cron->_createTestCron() }
           'object created with undef scheduler_args';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 262 lib/Inca/ReporterManager/Scheduler/Cron.pm

  use Inca::ReporterManager::Scheduler::Cron;

  my $sched = Inca::ReporterManager::Scheduler::Cron->_createTestCron();
  ok( $sched->start(), "scheduler started" );
  ok( $sched->isRunning(), "scheduler start verified" );
  ok( $sched->stop(), "scheduler stopped" );
  ok( ! $sched->isRunning(), "scheduler stop verified" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 366 lib/Inca/ReporterManager/Scheduler/Cron.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 536 lib/Inca/ReporterManager/Scheduler/Cron.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 604 lib/Inca/ReporterManager/Scheduler/Cron.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 700 lib/Inca/ReporterManager/Scheduler/Cron.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 771 lib/Inca/ReporterManager/Scheduler/Cron.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 861 lib/Inca/ReporterManager/Scheduler/Cron.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 14 lib/Inca/ReporterManager/Scheduler/Cron.pm

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

;

  }
};
is($@, '', "example from line 14");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

