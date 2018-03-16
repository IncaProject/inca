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

my $Original_File = 'lib/Inca/ReporterManager.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 150 lib/Inca/ReporterManager.pm

  use Test::Exception;
  use Inca::ReporterManager;
  lives_ok { new Inca::ReporterManager() } 'object created';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 235 lib/Inca/ReporterManager.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 721 lib/Inca/ReporterManager.pm


  use Inca::ReporterManager;

  my $scheduler = new Inca::ReporterManager( checkPeriod => 3 );
  is( $scheduler->getCheckPeriod(), 3, 'set check period from constructor' );
  $scheduler->setCheckPeriod( 10 );
  is( $scheduler->getCheckPeriod(), 10, 'set check period' );
  $scheduler = new Inca::ReporterManager();
  is( $scheduler->getCheckPeriod(), 2, 'default check period is 2' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 764 lib/Inca/ReporterManager.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 815 lib/Inca/ReporterManager.pm

  use Inca::ReporterManager;
  use Test::Exception;

  my $scheduler = new Inca::ReporterManager(depotURIs=>["cuzco.sdsc.edu:8234"]);
  my @depots = $scheduler->getDepots();
  is( $depots[0], "cuzco.sdsc.edu:8234", 'Set depots works from constructor' );
  $scheduler->setDepots( qw(inca.sdsc.edu:8235 inca.sdsc.edu:8236) );
  @depots = $scheduler->getDepots();
  is( $depots[0], "inca.sdsc.edu:8235", 'Set depots works from function (1)' );
  is( $depots[1], "inca.sdsc.edu:8236", 'Set depots works from function (2)' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 862 lib/Inca/ReporterManager.pm

  use Inca::ReporterManager;

  my $rm = new Inca::ReporterManager( depotTO => 3 );
  is( $rm->getDepotTimeout(), 3, 'set depot timeout from constructor' );
  $rm->setDepotTimeout( 10 );
  is( $rm->getDepotTimeout(), 10, 'set depot timeout' );
  $rm = new Inca::ReporterManager( );
  is( $rm->getDepotTimeout(), 120, 'default check period is 120' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 905 lib/Inca/ReporterManager.pm

  use Inca::ReporterManager;

  my $rm = new Inca::ReporterManager( id => "resourceA" );
  is( $rm->getId(), "resourceA", 'set id from constructor' );
  $rm->setId( "resourceB" );
  is( $rm->getId(), "resourceB", 'set id' );
  $rm = new Inca::ReporterManager( );
  is( $rm->getId(), undef, 'default id is undef' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 962 lib/Inca/ReporterManager.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1010 lib/Inca/ReporterManager.pm

  use Inca::ReporterManager;
  use Test::Exception;

  my $sched = new Inca::ReporterManager(); 
  is( $sched->getRimPath(), undef, 'default rim_path set' );
  dies_ok { $sched->setRimPath( "bin/reporter-instance-manager" ) }
          'setRimPath fails when not executable';
  $sched->setRimPath( "sbin/reporter-instance-manager" );
  is( $sched->getRimPath(), "sbin/reporter-instance-manager", 
      'set/getRimPath work' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1058 lib/Inca/ReporterManager.pm

  use Inca::ReporterManager;
  use Test::Exception;

  my $sched = new Inca::ReporterManager(); 
  is( $sched->getScheduleFilename(), undef, 'undef schedule file by default' );
  is( $sched->{scheduleFilename}, undef, 'undef schedule by default' );
  unlink "schedule.xml";
  $sched->setScheduleFilename( "schedule.xml" );
  is( $sched->getScheduleFilename(), "schedule.xml", 
      'set/getScheduleFilname work' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1112 lib/Inca/ReporterManager.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1158 lib/Inca/ReporterManager.pm

  use Inca::ReporterManager;
  use Test::Exception;
  use File::Spec;

  my $sched = new Inca::ReporterManager(); 
  is( $sched->getTmpDirectory(), File::Spec->tmpdir(), 'default tmp set' );
  $sched->setTmpDirectory( "/scratch" );
  is( $sched->getTmpDirectory(), "/scratch", 'set/getTmpDirectory work' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1185 lib/Inca/ReporterManager.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1320 lib/Inca/ReporterManager.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1382 lib/Inca/ReporterManager.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1452 lib/Inca/ReporterManager.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1516 lib/Inca/ReporterManager.pm

  use Inca::ReporterManager;

  is( Inca::ReporterManager->_getSchedulerClassName('sequential'),
      "Inca::ReporterManager::Scheduler::Sequential",
      '_getSchedulerClassName returns correct class for sequential' );
  is( Inca::ReporterManager->_getSchedulerClassName('cron'),
      "Inca::ReporterManager::Scheduler::Cron",
      '_getSchedulerClassName returns correct class for cron' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/ReporterManager.pm

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

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

