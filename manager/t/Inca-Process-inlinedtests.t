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

my $Original_File = 'lib/Inca/Process.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 138 lib/Inca/Process.pm

  use Inca::Process;
  use Test::Exception;
  lives_ok { new Inca::Process() } 'object created';

  my $process = new Inca::Process( tmpDir => $ENV{HOME} );
  my @files = glob( "$ENV{HOME}/inca.process.*" );
  is( $#files, 1, 'temp file created' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 398 lib/Inca/Process.pm

  use Inca::Process;
  my $proc = new Inca::Process();
  ok( $proc->hasCheckPeriod(), "hasCheckPeriod true for default" );
  $proc->setCheckPeriod( 0 );
  ok( ! $proc->hasCheckPeriod(), "hasCheckPeriod false for 0" );
  $proc->setCheckPeriod( 5 );
  ok( $proc->hasCheckPeriod(), "hasCheckPeriod for true" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 471 lib/Inca/Process.pm

  use Inca::Process;
  use Test::Exception;
  use BSD::Resource;
  use POSIX qw(ceil floor);
  use Inca::Logger;
  use strict;
  use warnings;

  Inca::Logger->screen_init( "ERROR" );

  untie *STDOUT; # for the inline testing framework 
  untie *STDERR; # for the inline testing framework 

  my ($stdout_filename, $stderr_filename);
  { 
    my $simple_proc = new Inca::Process( commandLine => "echo hello" );
    ok( $simple_proc->run( ), 'simple run - run returns true' );
    ok( ! $simple_proc->hasExceededLimits(), "simple run - no limits" ); 
    is( $simple_proc->getStdout(), "hello\n", "simple run - get stdout" );
    is( $simple_proc->getStderr(), "", "simple run - get stderr" );
    $stdout_filename = $simple_proc->{stdout_filename};
    $stderr_filename = $simple_proc->{stderr_filename};
    my $usage = $simple_proc->getUsage();
    cmp_ok( $usage->getWallClockTime(), '<', 1, 
            'simple run - less than a second');
    cmp_ok( $usage->getWallClockTime(), '>', 0, 'simple run - more than 0');
  }
  ok( ! -e $stdout_filename, "stdout cleaned up" );
  ok( ! -e $stderr_filename, "stderr cleaned up" );
  {
    my $simple_proc2 = new Inca::Process( commandLine => "echo hello 1>&2" );
    ok( $simple_proc2->run( ), 'simple run 2 - run returns true' );
    ok( ! $simple_proc2->hasExceededLimits(), "simple run 2 - no limits" ); 
    is( $simple_proc2->getStdout(), "", "simple 2 run - get stdout" );
    is( $simple_proc2->getStderr(), "hello\n", "simple 2 run - get stderr" );
    $stdout_filename = $simple_proc2->{stdout_filename};
    $stderr_filename = $simple_proc2->{stderr_filename};
  }
  ok( ! -f $stdout_filename, "stdout cleaned up" );
  ok( ! -f $stderr_filename, "stderr cleaned up" );

  my $program = "t/stream";
  my $buffer = 1.2;

  # run process with no profiling
  {
    my $proc = new Inca::Process( checkPeriod => 0 );
    $proc->setCommandLine( $program );
    ok( $proc->run( ), 'run returns true' );
    is( $proc->getExitCode(), 0, 'check period 0 - return exit code was 0' );
    like($proc->getStdout(), qr/Solution Validates/, "check period 0 - stdout");
    $stdout_filename = $proc->{stdout_filename};
    $stderr_filename = $proc->{stderr_filename};
  }
  ok( ! -f $stdout_filename, "stdout cleaned up" );
  ok( ! -f $stderr_filename, "stderr cleaned up" );

  # check process limits 
  { 
    my $proc = new Inca::Process( checkPeriod => 1 );
    $proc->setCommandLine( $program );
    my $limits = new Inca::Process::Usage();
    $limits->setCpuTime( 1 );
    $proc->setLimits( $limits );
    ok( ! $proc->run(), 'limits exceeded - run returns false' );
    ok( $proc->hasExceededLimits(), "process times out" ); 
    $stdout_filename = $proc->{stdout_filename};
    $stderr_filename = $proc->{stderr_filename};
  }
  ok( ! -f $stdout_filename, "stdout cleaned up" );
  ok( ! -f $stderr_filename, "stderr cleaned up" );

  # get some rough numbers by doing a sample run of program and parsing time
  my $start_time = time();
  my ( $pid, $pid_usage );
  use POSIX qw(setsid);
  setsid();
  my $ps;
  if ( $pid = fork() ) {
    sleep 1;
    $ps = `ps x -o pid,pgid,time,rss,lstart,command|grep $$|grep "t/stream\$"`;
    waitpid( $pid, 0 );
    $pid_usage = getrusage( RUSAGE_CHILDREN );
  } else {
    exec "$program 1>/dev/null";
  }
  my $sample_walltime = time() - $start_time;
  my $sample_cputime = $pid_usage->{'utime'} + $pid_usage->{'stime'};
  my $sample_memory;
  if ( $pid_usage->{maxrss} == 0 ) {
    my ($psmem) = $ps =~ /\S+\s+\S+\s+\S+\s+(\S+)/;
    $sample_memory = $psmem / 1024;
  } else {
    $sample_memory = $pid_usage->{maxrss} / 1024;
  }
  my $memory_limits = $sample_memory || 64;
  my $buffer_ratio = 0.6;
 
  {
    my $proc = new Inca::Process( checkPeriod => 1 );
    $proc->setCommandLine( $program );
    my $limits = new Inca::Process::Usage();
    $limits->setWallClockTime( 2 * $sample_walltime );
    $limits->setCpuTime( 2 * $sample_cputime );
    $limits->setMemory( 2 * $memory_limits );
    $proc->setLimits( $limits );
    ok( $proc->run( ), 'normal run - run returns true' );
    is( $proc->getExitCode(), 0, 'return exit code was 0' );
    # test output correct
    ok( ! $proc->hasExceededLimits(), "normal run - no limits" ) or 
        diag("wtime=$sample_walltime,ctime=$sample_cputime,mem=$sample_memory");
    like($proc->getStdout(), qr/Solution Validates/, "normal run - get stdout");
    is( $proc->getStderr(), "", "normal run - get stderr" );
  
    my $usage = $proc->getUsage();
  
    # just assume the difference in times should be smaller than the samples
    my $walltime_diff = abs( $sample_walltime - $usage->getWallClockTime() );
    cmp_ok( $walltime_diff, '<=', ceil($buffer_ratio * $sample_walltime), 
            'normal run - wall clock ok' );
  
    my $cpu_diff = abs( $sample_cputime - $usage->getCpuTime() );
    cmp_ok( $cpu_diff, '<=', ceil($buffer_ratio * $sample_cputime), 
            'normal run - cpu time ok' );
  
    my $memory_diff = abs( $sample_memory - $usage->getMemory() );
    cmp_ok($usage->getMemory(), '>=', 10, 'normal run - memory at least 10 MB');
    cmp_ok( $memory_diff, '<=', $buffer_ratio * $sample_memory, 
            'normal run - memory ok' ) or 
            diag( "sample=$sample_memory, returned=" . $usage->getMemory() );
  
    $stdout_filename = $proc->{stdout_filename};
    $stderr_filename = $proc->{stderr_filename};
  }
  ok( ! -f $stdout_filename, "stdout cleaned up" );
  ok( ! -f $stderr_filename, "stderr cleaned up" );

  {
    my $error_proc = new Inca::Process();
    $error_proc->setCommandLine( "cat /directorydoesnotexist" );
    ok( $error_proc->run( ), "bad command - run returns 1" );;
    isnt($error_proc->getExitCode(), 0, "bad command returns non-zero status");
    is( $error_proc->getStdout(), "", "bad command - stdout blank" );
    like( $error_proc->getStderr(), qr/No such file or directory|cannot open/, 
          "bad command - have stderr" );
    $stdout_filename = $error_proc->{stdout_filename};
    $stderr_filename = $error_proc->{stderr_filename};
  }
  ok( ! -f $stdout_filename, "stdout cleaned up" );
  ok( ! -f $stderr_filename, "stderr cleaned up" );

  {
    my $error_proc = new Inca::Process();
    $error_proc->setCommandLine( "the black hole" );
    ok( $error_proc->run( ), "non-existent command - run returns true" );
    cmp_ok( $error_proc->getExitCode(), '!=', 0, 
            "non-existent command - non-zero exit code");
    is($error_proc->getStdout(), undef, "non-existent command - stdout is blank" );
    $stdout_filename = $error_proc->{stdout_filename};
    $stderr_filename = $error_proc->{stderr_filename};
  }
  ok( ! -f $stdout_filename, "stdout cleaned up" );
  ok( ! -f $stderr_filename, "stderr cleaned up" );

  {
    my $error_proc = new Inca::Process();
    $error_proc->setCommandLine( "t/crash" );
    ok( $error_proc->run( ), "bad exec command - run returns true" );
    cmp_ok( $error_proc->getExitCode(), '!=', 0,
            "bad exec command - non-zero exit code");
    is( $error_proc->getStdout(), undef, "bad exec command - stdout is blank" );
    $stdout_filename = $error_proc->{stdout_filename};
    $stderr_filename = $error_proc->{stderr_filename};
  }
  ok( ! -f $stdout_filename, "stdout cleaned up" );
  ok( ! -f $stderr_filename, "stderr cleaned up" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 756 lib/Inca/Process.pm

  use Inca::Process;
  use Inca::Process::Usage;
  use Test::Exception;

  my $proc = new Inca::Process( );
  is( $proc->getCheckPeriod(), 5,
      'set default check period from constructor' );
  my $another_proc = new Inca::Process( checkPeriod => 3 );
  is( $another_proc->getCheckPeriod(), 3,
      'set explicit check period from constructor' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 801 lib/Inca/Process.pm

  use Inca::Process;

  my $proc = new Inca::Process( commandLine => "echo hello" );
  is( $proc->getCommandLine(), "echo hello",
      'set command line from constructor' );
  $proc->setCommandLine( "echo hi" );
  is( $proc->getCommandLine(), "echo hi",
      'set/get command line' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 845 lib/Inca/Process.pm

  use Inca::Process;
  use Inca::Process::Usage;
  use Test::Exception;

  my $limits = new Inca::Process::Usage();
  my $proc = new Inca::Process( limits => $limits );
  isa_ok( $proc->getLimits(), "Inca::Process::Usage", 
          'set limits worked from constructor' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Process.pm

  use Inca::Process;
  use Inca::Process::Usage;

  my $proc = new Inca::Process();
  my $limits = new Inca::Process::Usage();
  $limits->setWallClockTime( 10 );
  $proc->setLimits( $limits );
  $proc->setCommandLine( "echo hello" );
  $proc->run();
  $proc->getUsage();
  my $stdout = $proc->getStdout();
  print "The value from stdout was $stdout\n";




;

  }
};
is($@, '', "example from line 12");

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 12 lib/Inca/Process.pm

  use Inca::Process;
  use Inca::Process::Usage;

  my $proc = new Inca::Process();
  my $limits = new Inca::Process::Usage();
  $limits->setWallClockTime( 10 );
  $proc->setLimits( $limits );
  $proc->setCommandLine( "echo hello" );
  $proc->run();
  $proc->getUsage();
  my $stdout = $proc->getStdout();
  print "The value from stdout was $stdout\n";




  is( $stdout, "hello\n", "simple echo hello worked" );

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

