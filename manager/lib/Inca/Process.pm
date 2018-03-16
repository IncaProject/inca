package Inca::Process;

################################################################################

=head1 NAME

Inca::Process - Run a process but abort if process exceeds specified resources

=head1 SYNOPSIS

=for example begin

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

=for example end

=for example_testing
  is( $stdout, "hello\n", "simple echo hello worked" );

=head1 DESCRIPTION

Execute one process and monitor its system usage (memory, CPU time, and wall
clock time).  If the system usage is exceeded, the process will get killed.
The system usage of the process and it's children are available after
execution through the getUsage() call.  

Note, that usage is calculated from either getrusage or a 'ps' call during
execution.  However, on some systems, getrusage does not report memory (e.g.,
on linux).  In this case, memory will be determined from 'ps' if the check
period is small enough to be called during process execution.

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
use Inca::Constants qw(:bytes :params);
use Inca::IO;
use Inca::Logger;
use Inca::Process::Profiler;
use Inca::Process::SessionFork;
use Inca::Process::Usage;
use Inca::Validate qw(:all);

# Perl standard
use Carp;
use POSIX qw(:errno_h :signal_h);

my ($bsdResourceAvailable, $timeHiResAvailable);
BEGIN {
  $bsdResourceAvailable = $timeHiResAvailable = 0;
  eval {
    use BSD::Resource;
    $bsdResourceAvailable = 1;
  };
  eval {
    require Time::HiRes; 
    $timeHiResAvailable = 1;
  };
}

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $DEFAULT_CHECK_PERIOD = 5;
my $TMPFILE_TEMPLATE = "inca.process.$$";

my $SELF_PARAM_REQ = { isa => __PACKAGE__ };
my $INTEGER_WITH_DEFAULT = { %{$POSITIVE_INTEGER_PARAM_OPT}, 
                             default => $DEFAULT_CHECK_PERIOD };

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( %Options )

Class constructor which returns a new Inca::Process object.  The
constructor may be called with the following attributes.

=over 2 

B<Options>:

=over 13

=item limits

An object of type Inca::Process::Usage which indicates the maximum
resource utilization this reporter can use.  If any limit is exceeded,
the reporter will be killed.

=item checkPeriod

A positive integer indicating the period in seconds of which to check for
resource limits [default: 5]

=item tmpDir

A path to a directory where temporary files can be placed.

=back

=back

=begin testing

  use Inca::Process;
  use Test::Exception;
  lives_ok { new Inca::Process() } 'object created';

  my $process = new Inca::Process( tmpDir => $ENV{HOME} );
  my @files = glob( "$ENV{HOME}/inca.process.*" );
  is( $#files, 1, 'temp file created' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  # set defaults
  $self->{usage} = new Inca::Process::Usage();
  $self->{limits_status} = undef;
  $self->{logger} = Inca::Logger->get_logger( $class );

  # get options
  my %options = validate( @_, { limits => $USAGE_PARAM_OPT,
                                checkPeriod => $INTEGER_WITH_DEFAULT,
                                commandLine => $SCALAR_PARAM_OPT,
                                tmpDir => $SCALAR_PARAM_OPT
                               } );
  $self->setLimits( $options{limits} ) if exists $options{limits};
  $self->setCheckPeriod($options{checkPeriod}) if exists $options{checkPeriod};
  $self->setCommandLine($options{commandLine}) if exists $options{commandLine};

  # set up temporary files
  ($self->{stdout_fh}, $self->{stdout_filename}) = Inca::IO->tempfile( 
    "$TMPFILE_TEMPLATE.out", $options{tmpDir} 
  );
  ($self->{stderr_fh}, $self->{stderr_filename}) = Inca::IO->tempfile(
    "$TMPFILE_TEMPLATE.err", $options{tmpDir} 
  );
  if ( ! defined $self->{stdout_fh} || ! defined $self->{stderr_fh} ) {
    $self->{logger}->error( "Unable to create temporary files to store stdout/stderr for process execution" );
    return undef;
  }

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 getCheckPeriod( )

Get the period for how often to check the process for exceeding its limits.

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

=head2 getCommandLine( )

Get the process to run on the command-line.

=over 2

B<Returns>:

A string indicating an executable and its arguments to be executed. E.g.,
myprocess -x 20 -y 50 -file big.dat.

=back

=cut
#-----------------------------------------------------------------------------#
sub getCommandLine {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{command_line};
}

#-----------------------------------------------------------------------------#

=head2 getExceededLimits( )

Should be called after hasExceededLimits has returned true to return the
string representation of the exceeded limits.

=over 2

B<Returns>:

A string the process limits that were exceeded.

=back

=cut
#-----------------------------------------------------------------------------#
sub getExceededLimits {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{limits_status};
}

#-----------------------------------------------------------------------------#

=head2 getExitCode( )

Return the exit code of the process.

=over 2

B<Returns>:

An integer containing the exit code of the process.

=back

=cut
#-----------------------------------------------------------------------------#
sub getExitCode {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{exit_status};
}

#-----------------------------------------------------------------------------#

=head2 getLimits( )

Get the maxiumum system usage or limits for the process.  

=over 2

B<Returns>:

An object of type Inca::Process::Usage which indicates the maximum
resource utilization this process can use.  

=back

=cut
#-----------------------------------------------------------------------------#
sub getLimits {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{limits};
}

#-----------------------------------------------------------------------------#

=head2 getStderr( )

Return the stderr returned by the process as a string.

=over 2

B<Returns>:

A string containing the contents of stderr for the executed process.

=back

=cut
#-----------------------------------------------------------------------------#
sub getStderr {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  if ( ! open(RESULTS, "<", $self->{stderr_filename}) ) {
    return undef;
  }
  local $/; # file slurp mode
  my $results = <RESULTS>;
  close RESULTS;
  return $results;
}

#-----------------------------------------------------------------------------#

=head2 getStdout( )

Return the stdout returned by the process as a string.

=over 2

B<Returns>:

A string containing the contents of stdout for the executed process.

=back

=cut
#-----------------------------------------------------------------------------#
sub getStdout {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  if ( ! open(RESULTS, "<", $self->{stdout_filename}) ) {
    return undef;
  }
  local $/; # file slurp mode
  my $results = <RESULTS>;
  close RESULTS;
  return $results;
}

#-----------------------------------------------------------------------------#

=head2 getUsage( )

Return the system usage of the process.

=over 2

B<Returns>:

An object of type Inca::Process::Usage indicating the system usage of the
process.

=back

=cut
#-----------------------------------------------------------------------------#
sub getUsage {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{usage};
}

#-----------------------------------------------------------------------------#

=head2 hasCheckPeriod( )

Returns true if a check period has been specified and it's value is greater
than 0.

=over 2

B<Returns>:

Returns true if a check period has been specified and it's value is greater
than 0; otherwise returns false

=back

=begin testing

  use Inca::Process;
  my $proc = new Inca::Process();
  ok( $proc->hasCheckPeriod(), "hasCheckPeriod true for default" );
  $proc->setCheckPeriod( 0 );
  ok( ! $proc->hasCheckPeriod(), "hasCheckPeriod false for 0" );
  $proc->setCheckPeriod( 5 );
  ok( $proc->hasCheckPeriod(), "hasCheckPeriod for true" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasCheckPeriod {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return (exists $self->{check_period} and $self->{check_period} > 0);
}

#-----------------------------------------------------------------------------#

=head2 hasExceededLimits( )

Return true if the process was killed during execution because it exceeded
its limits.  Otherwise, return false.

=cut
#-----------------------------------------------------------------------------#
sub hasExceededLimits {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return defined $self->{limits_status};
}

#-----------------------------------------------------------------------------#

=head2 hasLimits( )

Verify whether resource limits have been set for the object.

=over 2

B<Returns>:

Returns true if a limits value has been set; otherwise returns false.

=back

=cut
#-----------------------------------------------------------------------------#
sub hasLimits {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return exists( $self->{limits} );
}

#-----------------------------------------------------------------------------#

=head2 run( )

Execute the specified process.  If a resource limit is specified, then the
process will be periodically checked every "check period" seconds and will be
killed if any resource has been exceeded.

=over 2

B<Returns>:

Returns 1 if process was started without errors; otherwise returns 0. 

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub run {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  $self->_initUsage();

  my $forker = new Inca::Process::SessionFork( stdout => $self->{stdout_fh},
                                               stderr => $self->{stderr_fh} );

  my $pgid = $forker->run( $self->getCommandLine() );
  my $profiler = new Inca::Process::Profiler();
  $profiler->addProcessGroups( $pgid );

  # run with limits or normal execution
  my $usage;
  if ( $self->hasCheckPeriod() ) {

    # We save the previous action and restore it after we've finished.  This
    # is important if we want this object to be garbage collected
    # appropriately.  I.e., otherwise a reference is kept for this object
    # and it won't get garbage collected until program exit
    my $prev_action = new POSIX::SigAction(); 
    sigaction( SIGALRM, undef, $prev_action );

    eval {
      # we want to wait for the process to finish executing but periodically
      # check its usage for profiling.  If limits are set, we will can
      # use the profile information to check for limits.  So, we do a wait
      # and set an alarm to periodically interrupt and profile.
      sigaction( 
        SIGALRM, 
        POSIX::SigAction->new( 
          sub { $usage = &_monitorProcess($self, $profiler) } 
        )
      ) or die "Error setting SIGALRM handler: $!\n";
      alarm( $self->getCheckPeriod() );

      # On some systems, wait will return because of an interrupt.  In that
      # case, wait will return -1 and the errno will be set to EINTR.  On
      # other systems when an alarm is called, a system call like wait gets
      # automatically restarted and will not return until the function
      # completes.  So, the reason for the check of wait_return in the
      # do/while is to check for the former case.  
      my $wait_return; 
      do {
        $wait_return = $forker->wait(); 
      } while ( $wait_return < 0 and POSIX::errno() == EINTR );
      alarm( 0 );
    };
    # check to see if we exitted due to signal setup problems
    if ( $@ && $@ =~ /Error setting/ ) {
      $self->{logger}->error( $@ );
      close( $self->{stdout_fh} ); close( $self->{stderr_fh} ); return 0;
    }
    # restore previous signal handler
    sigaction( SIGALRM, $prev_action) or die "Error restoring SIGALRM handler: $!\n";
  } else {
    $forker->wait();
  }

  $self->_setExitCode( $? );

  # if any limit has been exceeded, kill it
  if ( $self->hasExceededLimits() ) {
    $self->{logger}->info( "The following process exceeded process limits: " . 
                           $self->getCommandLine()  );
    my $num_procs = $forker->kill();
    $self->{logger}->info( "Successfully killed $num_procs processes\n" );
  }

  $self->_calcUsage( $usage );

  close( $self->{stdout_fh} ); 
  close( $self->{stderr_fh} ); 

  if ( $self->hasExceededLimits() ) {
    return 0;
  } else {
    return 1;
  }
}

#-----------------------------------------------------------------------------#

=head2 setCheckPeriod( $secs )

Set the period for how often to check the process for exceeding its limits.

=over 2

B<Arguments>:

=over 13

=item secs

A positive integer indicating the period in seconds of which to check for
resource limits.

=back

=back

=begin testing

  use Inca::Process;
  use Inca::Process::Usage;
  use Test::Exception;

  my $proc = new Inca::Process( );
  is( $proc->getCheckPeriod(), 5,
      'set default check period from constructor' );
  my $another_proc = new Inca::Process( checkPeriod => 3 );
  is( $another_proc->getCheckPeriod(), 3,
      'set explicit check period from constructor' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setCheckPeriod {
  my ( $self, $secs ) =
     validate_pos( @_, $SELF_PARAM_REQ, $POSITIVE_INTEGER_PARAM_REQ );

  $self->{check_period} = $secs;
}

#-----------------------------------------------------------------------------#

=head2 setCommandLine( $executable_n_args )

Set the process to run on the command-line.

=over 2

B<Arguments>:

=over 13

=item executable_n_args

A string indicating an executable and its arguments to be executed. E.g.,
myprocess -x 20 -y 50 -file big.dat.

=back

=back

=begin testing

  use Inca::Process;

  my $proc = new Inca::Process( commandLine => "echo hello" );
  is( $proc->getCommandLine(), "echo hello",
      'set command line from constructor' );
  $proc->setCommandLine( "echo hi" );
  is( $proc->getCommandLine(), "echo hi",
      'set/get command line' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setCommandLine {
  my ( $self, $executable_n_args ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR);

  $self->{command_line} = $executable_n_args;
}

#-----------------------------------------------------------------------------#

=head2 setLimits( $limits )

Set the maxiumum system usage or limits for the process.  The limits are
based on one or more of the following resources:  CPU time, wall clock time,
memory.  If any limit is exceeded, the process will be killed.

=over 2

B<Arguments>:

=over 13

=item limits

An object of type Inca::Process::Usage which indicates the maximum
resource utilization this process can use.  

=back

=back

=begin testing

  use Inca::Process;
  use Inca::Process::Usage;
  use Test::Exception;

  my $limits = new Inca::Process::Usage();
  my $proc = new Inca::Process( limits => $limits );
  isa_ok( $proc->getLimits(), "Inca::Process::Usage", 
          'set limits worked from constructor' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setLimits {
  my ( $self, $limits ) =
     validate_pos( @_, $SELF_PARAM_REQ, $USAGE_PARAM_REQ );

  $self->{limits} = $limits;
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _calcUsage( $last_usage )
#
# Find the total system usage of the child processes that this object has 
# forked.  We compare the results from getrusage to the last measurement
# obtained from the profiler (i.e., ps output) and return the largest
# values.
#
# Arguments:
#
#   last_usage - Holds the last measurement of process usage.
#
# Returns: 
#
# An object of type Inca::Process::Usage that indicates the amount of system
# resources forked processes have accumulated.
#
#-----------------------------------------------------------------------------#
sub _calcUsage {
  my ( $self, $last_usage ) = 
     validate_pos( @_, $SELF_PARAM_REQ,
                       { type => OBJECT | UNDEF, optional => 1 } );
  if ( defined $last_usage ) {
    my @args = ( $last_usage );
    validate_pos( @args, $USAGE_PARAM_REQ );
  }

  # set wall clock time
  $self->{usage}->setWallClockTime( $self->_getElapsedTime() );

  ### get current usage
  my $rusage = $self->_findUsage();

  # subtract any cpu time at start of run from current cputime; compare that
  # value to that returned by getrusage and return largest of cpu values
  my $cputime = $rusage->getCpuTime() - $self->{startusage}->getCpuTime();
  if ( defined $last_usage and $last_usage->getCpuTime() > $cputime ) {
    $self->{usage}->setCpuTime( $last_usage->getCpuTime() );
  } else {
    $self->{usage}->setCpuTime( $cputime );
  }

  # on some systems, sometimes getrusage does not return memory used (i.e.,
  # just 0).  In this case, use data from ps if we have it.  Otherwise, if
  # memory was set at start of run and the current memory is bigger, then we
  # know maxrss belongs to our run.  Otherwise, we take what we got from ps if
  # we have it. 
  if ( defined $last_usage and $rusage->getMemory() <= 0 ) {
    $self->{usage}->setMemory( $last_usage->getMemory() );
  } elsif ( $rusage->getMemory() > $self->{startusage}->getMemory() ) {
    $self->{usage}->setMemory( $rusage->getMemory() );
  } else {
    if ( defined $last_usage ) {
      $self->{usage}->setMemory( $last_usage->getMemory() );
    } else {
      $self->{usage}->setMemory( $rusage->getMemory() );
    }
  }
}

#-----------------------------------------------------------------------------#
# _findUsage( )
#
# Find the system usage of the child processes that this object has 
# forked from system's getrusage call.  
#
# Returns: 
#
# An object of type Inca::Process::Usage that indicates the amount of system
# resources forked processes have accumulated.
#
#-----------------------------------------------------------------------------#
sub _findUsage {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  my $usage = new Inca::Process::Usage();
  if($bsdResourceAvailable) {
    my $rusage = getrusage( RUSAGE_CHILDREN );
    $usage->setCpuTime( $rusage->{utime} + $rusage->{stime} );
    $usage->setMemory( $rusage->{maxrss} / $KILOS_TO_MEGA );
  } else {
    my ($user, $system, $cuser, $csystem) = times();
    $usage->setCpuTime( $cuser + $csystem );
    $usage->setMemory( 0 );
  }
  return $usage;
}

#-----------------------------------------------------------------------------#
# _getElapsedTime()
#
# Return the elapsed time from the time initUsage was called.
#
# Returns:
#
# The elapsed time from the time initUsage was called
#-----------------------------------------------------------------------------#
sub _getElapsedTime {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $timeHiResAvailable ? Time::HiRes::tv_interval($self->{starttime}) : 
                               time() - $self->{starttime}; 
}

#-----------------------------------------------------------------------------#
# _getStartTime( )
#
# Return the start time of a process.
#
# Returns:
#
# An integer indicating the start time of a process in number of non-leap
# seconds since system epoch.
#-----------------------------------------------------------------------------#
sub _getStartTime {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{starttime};
}

#-----------------------------------------------------------------------------#
# _initUsage( )
#
# Record how much usage from previous children if any.  Start a timer for a
# process to be used in measuring wall clock time.
#-----------------------------------------------------------------------------#
sub _initUsage {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  if($timeHiResAvailable) {
    $self->{starttime} = [ Time::HiRes::gettimeofday() ];
  } else {
    $self->{starttime} = time();
  }
  $self->{startusage} = $self->_findUsage();
}

#-----------------------------------------------------------------------------#
# _monitorProcess( $profiler )
#
# Called from alarm to log system usage and possibly check if a process has
# exceeded its process limits
#
# Arguments:
#
#   profiler - An object of type Inca::Process::Profiler that has been set
#              up to watch the process.
#
# Returns:
#
# An object of type Inca::Process::Usage containing the currently measured
# usage of the process or undef if there is an error.
#-----------------------------------------------------------------------------#
sub _monitorProcess {
  my ( $self, $profiler ) = 
     validate_pos( @_, $SELF_PARAM_REQ, $PROFILER_PARAM_REQ );

  my $cur_usage = $profiler->getUsage();
  if ( ! defined $cur_usage ) {
    $self->{logger}->error( "Unable to determine current process usage" );
    return undef;
  }
  # wall clock times gathered from ps can sometimes be fishy (e.g., days
  # off) -- if we are getting the right value from ps, then it should be 
  # greater than 0 and less than the limit + (2x the check period).  If not,
  # let's use the value from our own timestamp
  if ( $self->hasLimits() && $self->getLimits->hasWallClockTime() ) { 
    my $wallUpperBound = $self->getLimits->getWallClockTime() + 
                         ( 2 * $self->getCheckPeriod() );
    if ( $cur_usage->getWallClockTime() <= 0 ||
         $cur_usage->getWallClockTime() > $wallUpperBound ) {
      $cur_usage->setWallClockTime( $self->_getElapsedTime() );
    }
  }
  if ( $self->hasLimits() && $cur_usage->greaterThan($self->getLimits()) ) {
    $self->_setExceededLimits( 
      $cur_usage->limitsExceededString($self->getLimits()) 
    );
    croak "Process limits exceeded";
  } else {
    alarm( $self->getCheckPeriod() ); # set it again
  }
  return $cur_usage;
}

#-----------------------------------------------------------------------------#
# _setExceededLimits( )
#
# Set the time out status for the process to true
#-----------------------------------------------------------------------------#
sub _setExceededLimits {
  my ( $self, $limits_msg ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{limits_status} = $limits_msg;
}

#-----------------------------------------------------------------------------#
# _setExitCode( $status )
#
# Set the exit code for the process
#
# Arguments:
#
#   status - an integer containing the exit code of the process
#-----------------------------------------------------------------------------#
sub _setExitCode {
  my ( $self, $status ) = validate_pos(@_, $SELF_PARAM_REQ, $INTEGER_PARAM_REQ);

  $self->{exit_status} = $status;
}

#-----------------------------------------------------------------------------#
# destructor to clean up temporary files
#-----------------------------------------------------------------------------#
sub DESTROY {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  unlink( $self->{stdout_filename}, $self->{stderr_filename} );
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

This module utilizes temporary files to store stdout and stderr.

Cannot be used to execute more than one command-line; use one object per
process.

=cut
