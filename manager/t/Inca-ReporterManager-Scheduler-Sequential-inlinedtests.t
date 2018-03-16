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

my $Original_File = 'lib/Inca/ReporterManager/Scheduler/Sequential.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 140 lib/Inca/ReporterManager/Scheduler/Sequential.pm

  use Inca::ReporterManager::Scheduler::Sequential;
  use Test::Exception;
  untie *STDOUT;
  untie *STDERR;

  dies_ok { new Inca::ReporterManager::Scheduler::Sequential() } 
          'dies with empty args';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 183 lib/Inca/ReporterManager/Scheduler/Sequential.pm

  use Test::Exception;


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 264 lib/Inca/ReporterManager/Scheduler/Sequential.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 14 lib/Inca/ReporterManager/Scheduler/Sequential.pm

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

;

  }
};
is($@, '', "example from line 14");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

