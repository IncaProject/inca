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

my $Original_File = 'lib/Inca/ReporterManager/Scheduler.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 141 lib/Inca/ReporterManager/Scheduler.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 255 lib/Inca/ReporterManager/Scheduler.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 316 lib/Inca/ReporterManager/Scheduler.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 358 lib/Inca/ReporterManager/Scheduler.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 416 lib/Inca/ReporterManager/Scheduler.pm

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



    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 487 lib/Inca/ReporterManager/Scheduler.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/ReporterManager/Scheduler.pm

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

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

