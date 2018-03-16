package Inca::Process::Profiler;

################################################################################

=head1 NAME

Inca::Process::Profiler - Reports a process's resource usage 

=head1 SYNOPSIS

=for example begin

  use Inca::Process::Profiler;
  my $profiler = new Inca::Process::Profiler();
  $profiler->addProcess( $$ );
  $profiler->getUsage(); # returns object of type Inca::Process::Usage

=for example end

=head1 DESCRIPTION

Will query the system's ps command for current resource usage for a process
or process group.  This code is based on a code sample from Jim Hayes'.

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
use Inca::Logger;
use Inca::Process::Usage;
use Inca::Validate qw(:all);

# Perl standard
use Carp;
use Time::Local;

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $PSFORMAT = qr/(\d+)\s+(\d+)\s+(\S+)\s+([\d\.]+[KkMm]?)\s+(.+)/;
my $CPUFORMAT = qr/(\d+:){0,1}(\d+):(\d\d)(\.\d\d){0,1}/;
my $MEMFORMAT = qr/([\d\.]+)([KkMm]?)/;
my $ELAPSEDFORMAT_MINSEC = qr/(\d+):(\d+)$/;
my $ELAPSEDFORMAT_HOUR = qr/(\d+):\d+:\d+$/;
my $ELAPSEDFORMAT_DAY = qr/(\d+)-\d+:\d+:\d+$/;

my $SELF_PARAM_REQ = { isa => __PACKAGE__ };
my $CPU_PARAM_REQ = { regex => $CPUFORMAT };
my $MEM_PARAM_REQ = { regex => qr/\d+/ };
my $DEFAULT_CUMULATIVE = { %{$BOOLEAN_PARAM_REQ}, default => 1 };

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( )

Class constructor which returns a new Inca::Process::Profiler object.  

=begin testing

  use Inca::Process::Profiler;
  use Test::Exception;
  lives_ok { new Inca::Process::Profiler() } 'object created';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  # no options
  my %options = validate_pos( @_, 0 );

  # initialize private vars
  $self->{pids} = []; # processes to monitor
  $self->{pgids} = []; # process groups to monitor
  $self->{logger} = Inca::Logger->get_logger();

  # determine type of ps on system.  most systems use BSD format.  on aix, 
  # BSD is not available but the sysv version of ps is more compliant to BSD
  # than the default in /bin/ps.  the sysv version uses elapsed time instead
  # of start time
  my $ps = "ps";
  $ps = "/usr/sysv/bin/ps" if ( -e "/usr/sysv/bin/ps" );
  local $SIG{CHLD} = 'DEFAULT'; # make sure system can reap its own children
  system("$ps -o lstart >/dev/null 2>&1");
  my $walltime;
  if ( $? != 0 ) {
    $self->{elapsed} = 1;
    $walltime = "etime";
  } else {
    $self->{elapsed} = 0;
    $walltime = "lstart";
  }
  my $user = exists $ENV{USER} ? $ENV{USER} : $ENV{LOGNAME};
  $self->{pscommand} = "$ps -U $user -o pid,pgid,time,rss,$walltime"; 
  $self->{logger}->debug( "Using ps command: " . $self->{pscommand} );

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 addProcessGroups( @pgids )

Add any number of pgids (process group ids) to gather usage data from.

=over 2

B<Arguments>:

=over 13

=item pgids

An array of pgids that will used to collect usage data from.

=back

=back 

=begin testing

  use Inca::Process::Profiler;
  use Test::Exception;

  my $profiler = new Inca::Process::Profiler();
  my $pgids = [ 87823, 82343, 89834 ];
  $profiler->addProcessGroups( 87823, 82343, 89834 );
  my @returned_pgids = $profiler->getProcessGroups();
  is_deeply( $pgids, \@returned_pgids, 'add/getProcessGroups works' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub addProcessGroups {
  my ( $self, @pgids )  = 
     validate_pos( @_, $SELF_PARAM_REQ,
                       $POSITIVE_INTEGER_PARAM_REQ,
                       ($POSITIVE_INTEGER_PARAM_REQ) x (@_ - 2) );

  push ( @{$self->{pgids}}, @pgids );
}

#-----------------------------------------------------------------------------#

=head2 addProcesses( @pids )

Add any number of pids to gather usage data from.

=over 2

B<Arguments>:

=over 10

=item pids

An array of pids that will used to collect usage data from.

=back

=back

=begin testing

  use Inca::Process::Profiler;
  use Test::Exception;

  my $profiler = new Inca::Process::Profiler();
  my $pids = [ 87823, 82343, 89834 ];
  $profiler->addProcesses( 87823, 82343, 89834 );
  my @returned_pids = $profiler->getProcesses();
  is_deeply( $pids, \@returned_pids, 'add/getProcesses works' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub addProcesses {
  my ( $self, @pids )  = 
     validate_pos( @_, $SELF_PARAM_REQ,
                       $POSITIVE_INTEGER_PARAM_REQ,
                       ($POSITIVE_INTEGER_PARAM_REQ) x (@_ - 2) );

  push ( @{$self->{pids}}, @pids );
}

#-----------------------------------------------------------------------------#

=head2 getProcessGroups( )

Return the pgids being monitored for usage.

=over 2

B<Returns>:

An array of pgids that is being used to collect usage data from.

=back

=cut
#-----------------------------------------------------------------------------#
sub getProcessGroups {
  my ( $self )  = validate_pos( @_, $SELF_PARAM_REQ );

  return @{$self->{pgids}};
}

#-----------------------------------------------------------------------------#

=head2 getProcesses( )

Return the pids being monitored for usage.

=over 2

B<Returns>:

An array of pids that is being used to collect usage data from.

=back

=cut
#-----------------------------------------------------------------------------#
sub getProcesses {
  my ( $self )  = validate_pos( @_, $SELF_PARAM_REQ );

  return @{$self->{pids}};
}

#-----------------------------------------------------------------------------#

=head2 getUsage( cumulative => 1 )

Get the current resource usage of all of the pids and pgids that have been
added to the object.  

=over 2

B<Options>:

=over 10

=item cumulative

If true, getUsage() will return the cumulative resource usage of all monitored
pids and pgids.  Otherwise, getUsage() will return a reference to a hash
array where the keys are the pids and the values are Inca::Process::Usage
objects.  [default: 1]

=back

=back

B<Returns>:

Returns an object of type Inca::Process::Usage if the cumulative option is
set.  Otherwise, returns a reference to a hash array where the keys are pids
and the values are Inca::Process::Usage objects.  

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getUsage {
  my ( $self, @options )  = 
     validate_with( params => \@_, 
                    spec => [ $SELF_PARAM_REQ ], 
                    allow_extra => 1 );
  my %options = validate( @options, { cumulative => $DEFAULT_CUMULATIVE } );

  local $SIG{CHLD} = 'DEFAULT'; # make sure `` can reap its own children
  my $raw_psout = `$self->{pscommand}`;
  # we put parsing in an eval in case there is a unexpected case that causes
  # fatal parsing error
  my $usage; 
  eval { $usage = $self->_parsePS( $raw_psout ); };
  if ( $@ || ! defined $usage ) {
    $self->{logger}->error( "Unable to parse ps output: $raw_psout" ); 
    return undef;
  }

  if ( $options{cumulative} ) {
    my $cum_usage = new Inca::Process::Usage();
    $cum_usage->zeroValues();
    $self->_addUsage( $usage, $cum_usage );
    return $cum_usage;
  } else {
    return $usage;
  }
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _addUsage( $pids_usage, $cum_usage )
#
# Add up the usage of several pids and return the result in $cum_usage.
#
# Arguments:
#
# pids_usage  A reference to a hash array where the keys are pids and the
#             values are objects of type Inca::Process::Usage from which the
#             usage info will be accumulated.
#
# cum_usage   An object of type Inca::Process::Usage in which the usage info
#             will be placed.
#-----------------------------------------------------------------------------#
sub _addUsage {
  my ( $self, $pids_usage, $cum_usage )  = 
     validate_pos( @_, $SELF_PARAM_REQ, HASHREF, $USAGE_PARAM_REQ );

  my ($pid, $pid_usage);
  while ( ($pid, $pid_usage) = each %{$pids_usage} ) {
    # add up all cpu time
    my $total_cpu = $cum_usage->getCpuTime();
    $cum_usage->setCpuTime( $total_cpu + $pid_usage->getCpuTime() );

    # add up all memory
    my $total_memory = $cum_usage->getMemory();
    $cum_usage->setMemory( $total_memory + $pid_usage->getMemory() );

    # take largest wall clock time
    if ( $pid_usage->getWallClockTime() > $cum_usage->getWallClockTime() ) {
      $cum_usage->setWallClockTime( $pid_usage->getWallClockTime() );
    }
  }
  return;
}

#-----------------------------------------------------------------------------#
# _parsePS( $raw_psout )
#
# Parse the output of PS and place in Inca::Process::Usage object $usage.
#
# Arguments:
#
# raw_psout  A string containing the output of 'ps' command
#
# Returns
#
# Returns a reference to a hash array representing the parsed ps, where the
# pids of processes are the names and the values are Inca::Process::Usage
# objects.

=begin testing

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
  
=end testing

=cut
#-----------------------------------------------------------------------------#
sub _parsePS {
  my ( $self, $raw_psout ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  my $usage = {};

  my @psout = split( /\n/, $raw_psout );
  for my $proc ( @psout[1 .. $#psout] ) { # skim off header
    my ( $pid, $pgid, $cpu, $memory, $start ) = $proc =~ $PSFORMAT;
    if ( grep(/^$pid$/, @{$self->{pids}}) or 
         grep(/^$pgid$/, @{$self->{pgids}})) {
      $usage->{$pid} = new Inca::Process::Usage();

      # first deal with cpu which is in the format 
      # [hours:]min:secs[.milliseconds]
      my ( $hours, $mins, $secs, $milli ) = $cpu =~ $CPUFORMAT;
      if ( ! defined $hours ) {
        $hours = 0 
      } else {
        $hours =~ s/:$//;
      }
      $milli = 0 if ( ! defined $milli );
      # mins and secs could be undefined if ?? returned
      $mins = 0 if ( ! defined $mins );
      $secs = 0 if ( ! defined $secs );
      $usage->{$pid}->setCpuTime( 
        ($hours * $SECS_TO_HOUR) + ($mins * $SECS_TO_MIN) + $secs + $milli 
      );
    
      # next deal with memory which is in kilobytes; we want megabytes
      my ( $mem_num, $mem_units ) = $memory =~ $MEMFORMAT;
      if ( ! defined $mem_units || $mem_units eq "" || $mem_units =~ /k|K/ ) {
        $usage->{$pid}->setMemory( $mem_num/ $KILOS_TO_MEGA );
      } elsif ( $mem_units =~ /M|m/ ) {
        $usage->{$pid}->setMemory( $mem_num );
      } else {
        $self->{logger}->error( "Unable to parse memory format: $memory" ); 
        return undef;
      }
    
      # finally deal with wall clock time; we are given the start time
      if ( $self->{elapsed} ) {
        my $elapsed_time = 0;
        my ($min,$sec) = $start =~ $ELAPSEDFORMAT_MINSEC;
        $elapsed_time += $sec;
        $elapsed_time += $min * $SECS_TO_MIN;
        my ($hour) = $start =~ $ELAPSEDFORMAT_HOUR;
        $elapsed_time += $hour * $SECS_TO_HOUR if defined $hour;
        my ($day) = $start =~ $ELAPSEDFORMAT_DAY;
        $elapsed_time += $day * $SECS_TO_DAY if defined $day;
        $usage->{$pid}->setWallClockTime( $elapsed_time );
      } else {
        if ( $start =~ /-/ ) {
          $usage->{$pid}->setWallClockTime( 0 );
        } else {
          my($weekday, $month, $monthDay, $dayTime, $year) = split(/\s+/, $start);
          # look for month is this string and divide by 3 because 
          $month = index('JanFebMarAprMayJunJulAugSepOctNovDec', $month) / 3;
          my($dayHours, $dayMinutes, $daySeconds) = split(/:/, $dayTime);
          my $start_time_secs =
           timelocal($daySeconds,$dayMinutes,$dayHours,$monthDay,$month,$year);
          $usage->{$pid}->setWallClockTime( time() - $start_time_secs );
        }
      }
    }
  }

  return $usage; 
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

Only works on unix variants where ps option -o is supported.

=cut
