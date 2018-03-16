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

my $Original_File = 'lib/Inca/Process/Profiler.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 83 lib/Inca/Process/Profiler.pm

  use Inca::Process::Profiler;
  use Test::Exception;
  lives_ok { new Inca::Process::Profiler() } 'object created';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 151 lib/Inca/Process/Profiler.pm

  use Inca::Process::Profiler;
  use Test::Exception;

  my $profiler = new Inca::Process::Profiler();
  my $pgids = [ 87823, 82343, 89834 ];
  $profiler->addProcessGroups( 87823, 82343, 89834 );
  my @returned_pgids = $profiler->getProcessGroups();
  is_deeply( $pgids, \@returned_pgids, 'add/getProcessGroups works' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 195 lib/Inca/Process/Profiler.pm

  use Inca::Process::Profiler;
  use Test::Exception;

  my $profiler = new Inca::Process::Profiler();
  my $pids = [ 87823, 82343, 89834 ];
  $profiler->addProcesses( 87823, 82343, 89834 );
  my @returned_pids = $profiler->getProcesses();
  is_deeply( $pids, \@returned_pids, 'add/getProcesses works' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 293 lib/Inca/Process/Profiler.pm

  use Inca::Process::Profiler;
  use POSIX qw(setsid ceil floor);
  use BSD::Resource;
  use strict;
  use warnings;

  my $cpumem_program = "t/stream 1>/dev/null";
  my $check_time = 3;
  my $slack_factor = 1.25;
  my $pid;
  my $pid_usage; 

  # get some rough numbers by doing a sample run of program and parsing time
  my $start_time = time();
  if ( $pid = fork() ) {
    waitpid( $pid, 0 );
    $pid_usage = getrusage( RUSAGE_CHILDREN );
  } else {
    exec $cpumem_program;
  }
  my $sample_walltime = time() - $start_time;
  my $sample_cputime = $pid_usage->{'utime'} + $pid_usage->{'stime'};
  my $check_fraction = $sample_walltime / $check_time;
  my $check_cputime = $sample_cputime / $check_fraction;
  my $longest_wait_time = ceil( $check_time * $slack_factor );
  my $shortest_cputime = floor( $check_cputime / $slack_factor ); 
  my $highest_cputime = ceil( $check_cputime * $slack_factor ); 

  my $profiler = new Inca::Process::Profiler();
  isa_ok( $profiler->getUsage(), "Inca::Process::Usage" );
  if ( $pid = fork() ) {
    $profiler->addProcessGroups( $pid );
    sleep $check_time;
    my $usage = $profiler->getUsage( cumulative => 0 );
    isa_ok( $usage, 'HASH' );
    my @pids = sort( keys %{$usage} );
    is( scalar @pids, 2, "right amount of pids found" );
    cmp_ok( $usage->{$pids[0]}->getWallClockTime(), ">=", $check_time, 
            "shell process - wall clock time >= $check_time secs" );
    cmp_ok( $usage->{$pids[1]}->getWallClockTime(), "<=", $longest_wait_time,
            "shell process - wall clock time <= $longest_wait_time secs" );
    cmp_ok( $usage->{$pids[1]}->getWallClockTime(), ">=", $check_time,
            "$cpumem_program - wall clock time >= $check_time secs" );
    cmp_ok( $usage->{$pids[0]}->getWallClockTime(), "<=", $longest_wait_time,
            "$cpumem_program - wall clock time <= $longest_wait_time secs" );
    cmp_ok( $usage->{$pids[1]}->getCpuTime(), ">=", $shortest_cputime, 
            "$cpumem_program - cpu time >= $shortest_cputime" );
    cmp_ok( $usage->{$pids[1]}->getCpuTime(), "<=", $highest_cputime,
            "$cpumem_program - cpu time <= $highest_cputime" );
    cmp_ok( $usage->{$pids[1]}->getMemory(), ">", 10, 
            "$cpumem_program - memory > 10 MB" );

    my $cum_usage = $profiler->getUsage( cumulative => 1 );
    isa_ok( $cum_usage, "Inca::Process::Usage" );
    cmp_ok( $cum_usage->getWallClockTime(), ">=", $check_time, 
            "cumulative - wall clock time >= $check_time" );
    cmp_ok( $cum_usage->getCpuTime(), ">=", $shortest_cputime, 
            "cumulative - cpu time >= $shortest_cputime secs" );
    cmp_ok( $cum_usage->getMemory(), ">", 10, "cumulative - memory > 10 MB");
    waitpid( $pid, 0 );
  } else {
    setsid();
    exec $cpumem_program;
  }


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 447 lib/Inca/Process/Profiler.pm

  use Inca::Process::Profiler;

  my $profiler = new Inca::Process::Profiler();
  $profiler->{elapsed} = 0;
  my $macosx = <<MAC;
  PID  PGID      TIME    RSS STARTED
  106   106   0:28.48   5636 Wed May 18 09:21:14 2005    
  108   108   0:10.10  14340 Wed May 18 09:21:17 2005    
  199   199   0:00.80   4336 Wed May 18 09:21:50 2005    
  210   103   0:12.96   9828 Wed May 18 09:21:57 2005    
  213   103   0:15.27   9248 Wed May 18 09:22:01 2005    
  223   103  19:11.53   8652 Wed May 18 09:22:17 2005    
  224   103  12:12.65  10016 Wed May 18 09:22:21 2005    
  225   103   0:03.25   7952 Wed May 18 09:22:21 2005    
  227   103  19:21.56   9404 Wed May 18 09:22:21 2005    
  229   103   0:00.48   5468 Wed May 18 09:22:22 2005    
  230   103   0:11.16  13432 Wed May 18 09:22:22 2005    
MAC
  $profiler->addProcessGroups( 103 );
  my $usage = $profiler->_parsePS( $macosx );
  is( $usage->{210}->getCpuTime(), '12.96', 'macosx - extracted CPU correctly');
  is( $usage->{210}->getMemory(), '9.59765625',
      'macosx - extracted memory correctly');
  is( $usage->{227}->getCpuTime(), '1161.56', 
      'macosx - extracted CPU correctly' );
  is( $usage->{227}->getMemory(), '9.18359375',
      'macosx - extracted memory correctly');
  
  my $ia64_linux = <<LINUX;
  PID  PGID     TIME  RSS                  STARTED
19709 18232 00:09:20 87776 Wed May 11 17:24:06 2005
19711 18232 00:16:06 87776 Wed May 11 17:24:06 2005
19712 18232 00:17:42 87776 Wed May 11 17:24:06 2005
19713 18232 00:03:32 87776 Wed May 11 17:24:06 2005
19714 18232 00:23:19 87776 Wed May 11 17:24:06 2005
19715 18232 00:21:54 87776 Wed May 11 17:24:06 2005
19725 18232 00:05:52 87776 Wed May 11 17:24:06 2005
23847 18232 00:10:04 87776 Wed May 11 17:29:34 2005
23848 18232 01:19:57 87776 Wed May 11 17:29:34 2005
23849 18232 00:07:17 87776 Wed May 11 17:29:34 2005
31618 18232 00:30:44 87776 Wed May 11 17:53:40 2005
31749 18232 00:08:15 87776 Wed May 11 17:54:25 2005
31803 18232 00:09:14 87776 Wed May 11 17:54:26 2005
31805 18232 00:12:09 87776 Wed May 11 17:54:26 2005
LINUX
  $profiler->addProcessGroups( 18232 );
  $usage = $profiler->_parsePS( $ia64_linux );
  ok ( defined $usage, "output was parsed" );
  is( $usage->{31805}->getCpuTime(), '729', 
      'ia64Linux - extracted CPU correctly' );
  is( $usage->{31805}->getMemory(), '85.71875', 
      'ia64Linux - extracted memory correctly' );
  is( $usage->{23848}->getCpuTime(), '4797', 
      'ia64Linux - extracted CPU correctly' );
  is( $usage->{23848}->getMemory(), '85.71875', 
      'ia64Linux - extracted memory correctly' );

  $profiler = new Inca::Process::Profiler();
  $profiler->{elapsed} = 0;
  my $alpha = <<ALPHA;
       PID       PGID        TIME  RSS STARTED
    540460     540460     0:00.04   0K     Wed Jun 22 14:01:00 2005
    799419     804040     0:00.02 432K     Fri Jul  1 15:28:50 2005
    803917     803917     0:00.24 712K     Fri Jul  1 15:28:01 2005
    804040     804040     0:00.02 224K     -
    788495     788495     0:00.21 680K     Fri Jul  1 14:23:38 2005
    793574     793574     0:00.02 544K     Fri Jul  1 14:55:11 2005
    595203     595203     0:04.18 704K     Wed Jun 22 19:57:21 2005
    790894     790894     0:00.28 680K     Fri Jul  1 14:27:35 2005
    781875     781875     0:00.21 632K     Fri Jul  1 13:43:00 2005
    787137     787137     1:23.09 552.5M     Fri Jul  1 13:43:02 2005
ALPHA
  $profiler->addProcessGroups( 804040 );
  $profiler->addProcesses( 787137 );
  $usage = $profiler->_parsePS( $alpha );
  is( $usage->{804040}->getCpuTime(), '0.02', 
      'alpha - extracted CPU correctly' );
  is( $usage->{804040}->getMemory(), '0.21875',
      'alpha - extracted memory correctly');
  is( $usage->{787137}->getCpuTime(), '83.09', 
      'alpha - extracted CPU correctly' );
  is( $usage->{787137}->getMemory(), '552.5', 
      'alpha - extracted memory correctly');
  
  $profiler = new Inca::Process::Profiler();
  $profiler->{elapsed} = 1;
  my $aix = <<AIX;
   PID   PGID        TIME   RSS     ELAPSED
467186 467186    00:00:00  1476  1-00:13:17
360886 668204    00:00:00  2192    01:58:33
573742 647786    00:00:00  2188  1-00:13:18
586014 586014    00:00:00  1660  1-00:12:56
643606 467186    00:00:03  1812  1-00:13:16
709286 467186    00:00:00   856  1-00:13:16
729800 467186    00:00:00   868  1-00:13:12
209918 209918    00:00:00  1980    01:58:33
517020 517020    00:27:47 716260       28:11
159946 712802    00:02:53 223456    03:32:45
AIX
  $profiler->addProcessGroups( 467186 );
  $profiler->addProcesses( 517020 );
  $usage = $profiler->_parsePS( $aix );
  is( $usage->{467186}->getCpuTime(), '0',
      'aix - extracted CPU correctly' );
  is( $usage->{467186}->getMemory(), '1.44140625',
      'aix - extracted memory correctly');
  is( $usage->{467186}->getWallClockTime(), '87197',
      'aix - extracted wall clock correctly');
  is( $usage->{517020}->getCpuTime(), '1667',
      'aix - extracted CPU correctly' );
  is( $usage->{517020}->getMemory(), '699.47265625',
      'aix - extracted memory correctly');
  

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Process/Profiler.pm

  use Inca::Process::Profiler;
  my $profiler = new Inca::Process::Profiler();
  $profiler->addProcess( $$ );
  $profiler->getUsage(); # returns object of type Inca::Process::Usage

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

