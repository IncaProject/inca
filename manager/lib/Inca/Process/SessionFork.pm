package Inca::Process::SessionFork;

################################################################################

=head1 NAME

Inca::Process::SessionFork - Fork off a process asynchronously in a new session

=head1 SYNOPSIS

=for example begin

  use Inca::Process::SessionFork;
  my $forker = new Inca::Process::SessionFork();
  $forker->setStdout( *STDOUT );
  $forker->setStderr( *STDERR );
  $forker->run( "some executable" ); 
  $forker->wait();

  # or if you want to kill it before it completes

  $forker = new Inca::Process::SessionFork();
  $forker->setStdout( *STDOUT );
  $forker->setStderr( *STDERR );
  $forker->run( "some executable" ); 
  $forker->kill();

=for example end

=head1 DESCRIPTION

Will fork off a new process asynchronously in a new session.  The new process
will be the process group leader of a new process group and will have no
controlling terminal.  This is useful for when you may need to kill a process,
that may in turn have forked processes, and you want all processes cleaned up
(since they will all have the same group id).  This module also provides
mechanisms for extracting stdout and stderr from a process but does not
provide mechanisms for writing to the process.

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
use Inca::Validate qw(:all);

# Perl standard
use Carp;
use POSIX qw(:sys_wait_h setsid);
use IO::Handle; # for redirecting

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ = { isa => __PACKAGE__ };
my $BOOLEAN_DEFAULT_0 = { %{$BOOLEAN_PARAM_OPT}, default => 0 };
my $INTEGER_DEFAULT_MIN = { %{$POSITIVE_INTEGER_PARAM_REQ},
                            default => $SECS_TO_MIN };

my $KILL_SIGNAL_DELAY = 1; # Secs between kill signals

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( %Options )

Class constructor which returns a new Inca::Process::SessionFork object.  The
constructor may be called with any of the following attributes.

=over 2 

B<Options>:

=over 13

=item stdout

Set the STDOUT stream for the process that will be forked [default STDOUT]

=item stderr

Set the STDERR stream for the process that will be forked [default STDERR]

=back

=back

=begin testing

  use Inca::Process::SessionFork;
  use Test::Exception;
  lives_ok { new Inca::Process::SessionFork() } 'object created';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  $self->{logger} = Inca::Logger->get_logger( $class );

  # read options
  my %options = validate( @_, { stdout => $IO_PARAM_OPT,
                                stderr => $IO_PARAM_OPT
                              } );

  $self->setStdout( $options{stdout} ) if exists $options{stdout};
  $self->setStderr( $options{stderr} ) if exists $options{stderr};

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 getStderr( )

Get the STDERR stream for the process that will be forked.

=over 2

B<Returns>:

An object of type GLOB or GLOBREF that will be used to send the process's
stderr.

=back

=cut
#-----------------------------------------------------------------------------#
sub getStderr {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{stderr};
}

#-----------------------------------------------------------------------------#

=head2 getStdout( )

Get the STDOUT stream for the process that will be forked.

=over 2

B<Returns>:

An object of type GLOB or GLOBREF that will be used to send the process's
stdout.

=back

=cut
#-----------------------------------------------------------------------------#
sub getStdout {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{stdout};
}

#-----------------------------------------------------------------------------#

=head2 hasStderr( )

Check to see if a stderr stream has been set.

=over 2

B<Returns>:

Returns true if a stderr stream has been specified; otherwise returns false.

=back

=begin testing

  use Inca::Process::SessionFork;

  my $fork = new Inca::Process::SessionFork();
  ok( ! $fork->hasStderr(), 'hasStderr - false value' );
  $fork->setStderr( *STDOUT );
  ok( $fork->hasStderr(), 'hasStderr - true value' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasStderr {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return exists $self->{stderr};
}

#-----------------------------------------------------------------------------#

=head2 hasStdout( )

Check to see if a stdout stream has been set.

=over 2

B<Returns>:

Returns true if a stdout stream has been specified; otherwise returns false.

=back

=begin testing

  use Inca::Process::SessionFork;

  my $fork = new Inca::Process::SessionFork();
  ok( ! $fork->hasStdout(), 'hasStdout - false value' );
  $fork->setStdout( *STDOUT );
  ok( $fork->hasStdout(), 'hasStdout - true value' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasStdout {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return exists $self->{stdout}; 
}

#-----------------------------------------------------------------------------#

=head2 isRunning( ) 

Check to see if the process is still running.

=over 2

B<Returns>:

Returns 1 if the process is still running; otherwise returns 0.

=back

=cut
#-----------------------------------------------------------------------------#
sub isRunning {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  my $result = waitpid( $self->{pid}, WNOHANG ); 
  if ( $result == 0 ) {
    return 1;
  } elsif ( $result == $self->{pid} || $result == -1 ) {
    return 0;
  } else {
    $self->{logger}->warn( 
      "Suspicious result '$result' returned from waitpid in isRunning" 
    );
    return undef;
  }
}

#-----------------------------------------------------------------------------#

=head2 kill( )

Send the kill signal to the process group that is running.  Then wait for 
the processes to exit.  By default, if the wait takes longer than a minute,
we assume something went wrong and exit.

=over 2

B<Options>:

=over 13

=item nowait

Return immediately after the kill signal and do not wait.

=item wait

Change the timeout for the wait after the kill signal in seconds.  A value of
0 is the equivalent of the 'nowait' option above. [default: 1 minute]

=back

B<Returns>:

Returns the value returned by the kill command indicating the number of
processes signaled.

=back

=begin testing

  untie *STDOUT;
  untie *STDERR;

  my $forker = new Inca::Process::SessionFork();
  my $num_killed;
  my $pid;
  eval {
    local $SIG{ALRM} = sub { $num_killed = $forker->kill(); die };
    alarm(5);
    $pid =  $forker->run( "t/spawn_lots_of_processes 4 30 1>/dev/null" );
    $forker->wait();
    alarm( 0 );
  };
  is( $num_killed, 1, "kill group of processes using alarm" );
  my $procs_left = `ps x -o pgid | grep $pid`;
  is ( $procs_left, "", "all processes killed" ); 

=end testing

=cut
#-----------------------------------------------------------------------------#
sub kill {
  my ( $self, @params ) = validate_pos( @_, $SELF_PARAM_REQ );
  my %options = 
     validate( @params, { nowait => $BOOLEAN_DEFAULT_0,
                          wait => $INTEGER_DEFAULT_MIN
                        } );

  # Send the kill signal to the process group (the '-' indicates it's a pgid)
  my $num = kill( 'KILL', -$self->{pid});

  # wait for processes to exit
  if ( $options{nowait} == 0 and $options{wait} > 0 ) {
    eval {
      local $SIG{ALRM} = sub { _killWaitTimesOut( $self, $options{wait} ); };
      alarm( $options{wait} );
      $self->wait();
      alarm( 0 );
    };
  }

  return $num;
}

#-----------------------------------------------------------------------------#

=head2 run( $executable_n_args ) 

Fork off a process under a new session group with its own process group id
and return immediately.  Use the wait() call to wait for the process to
complete.

=over 2

B<Arguments>:

=over 13

=item executable_n_args

A string indicating an executable and its arguments to be executed. E.g.,
myprocess -x 20 -y 50 -file big.dat.

=back

=back

=begin testing

  untie *STDOUT; # for the inline testing framework 
  untie *STDERR; # for the inline testing framework 

  use Inca::Process::SessionFork;
  use File::Temp qw(tempfile);
  use strict;
  use warnings;

  # test stdout
  my ($fh1, $filename1) = tempfile( UNLINK => 1 );
  my $forker1 = new Inca::Process::SessionFork();
  $forker1->setStdout( $fh1 );
  $forker1->run( "echo Hello" );
  $forker1->wait();
  open( VERIFY, "<", $filename1 );
  local $/;
  my $stdout = <VERIFY>;
  close VERIFY;
  is( $stdout, "Hello\n", "simple fork with output to STDOUT worked" );
  $forker1->run( "echo Hello" );
  $forker1->wait();
  open( VERIFY, "<", $filename1 );
  $stdout = <VERIFY>;
  close VERIFY;
  is( $stdout, "Hello\nHello\n", 
      "run more than once (check stdout)" );

  # test stderr
  my ($fh2, $filename2) = tempfile( UNLINK => 1 );
  my $forker2 = new Inca::Process::SessionFork();
  $forker2->setStderr( $fh2 );
  $forker2->run( "echo Hello 1>&2" );
  $forker2->wait();
  open( VERIFY, "<", $filename2 );
  my $stderr = <VERIFY>;
  close VERIFY;
  is( $stderr, "Hello\n", "simple fork with output to STDERR worked" );
  $forker2->run( "echo Hello 1>&2" );
  $forker2->wait();
  open( VERIFY, "<", $filename2 );
  $stderr = <VERIFY>;
  close VERIFY;
  is( $stderr, "Hello\nHello\n", "can run more than once (check stderr)" );

  # test stdout and stderr
  my ($fh3, $filename3) = tempfile( UNLINK => 1 );
  my ($fh4, $filename4) = tempfile( UNLINK => 1 );
  my $forker3 = new Inca::Process::SessionFork( stdout => $fh3,
                                                stderr => $fh4 );
  $forker3->run( "echo Hello; echo Hello 1>&2" );
  $forker3->wait();
  open( VERIFY, "<", $filename3 );
  $stdout = <VERIFY>;
  close VERIFY;
  open( VERIFY, "<", $filename4 );
  $stderr = <VERIFY>;
  close VERIFY;
  is( $stdout, "Hello\n", "simple fork with output to STDOUT/STDERR worked-1" );
  is( $stderr, "Hello\n", "simple fork with output to STDOUT/STDERR worked-2" );

  # test isRunning and something with more umph
  my ($fh5, $filename5) = tempfile( UNLINK => 1 );
  my $forker4 = new Inca::Process::SessionFork( stdout => $fh5 );
  $forker4->run( "t/stream" );
  is( $forker4->isRunning(), 1, "isRunning - true value" );
  $forker4->wait();
  is( $forker4->isRunning(), 0, "isRunning - false value" );
  open( VERIFY, "<", $filename5 );
  $stdout = <VERIFY>;
  close VERIFY;
  like( $stdout, qr/Solution Validates/, "stream fork with output to STDOUT" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub run {
  my ( $self, $executable_n_args ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR);

  if ( $self->{pid} = fork() ) {
    return $self->{pid};
  } else {
    $self->_initChild( $executable_n_args ); # returns if error only
  }

}

#-----------------------------------------------------------------------------#

=head2 setStderr( $stream )

Set the STDERR stream for the process that will be forked.

=over 2

B<Arguments>:

=over 13

=item stream

An object of type GLOB or GLOBREF that can be used to send the process's stderr.

=back

=back

=begin testing

  use Inca::Process::SessionFork;
  use File::Temp qw(tempfile);

  my $fh1 = tempfile( UNLINK => 1 );
  my $forker = new Inca::Process::SessionFork( stderr => $fh1 );
  is( $forker->getStderr()->fileno(), $fh1->fileno(),
      'set stderr from constructor worked' );
  my $fh2 = tempfile( UNLINK => 2 );
  $forker->setStderr( $fh2 );
  is( $forker->getStderr()->fileno(), $fh2->fileno(),
      'set stderr from set function worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setStderr {
  my ( $self, $stderr ) = validate_pos( @_, $SELF_PARAM_REQ, $IO_PARAM_REQ );

  $self->{stderr} = $stderr;
}

#-----------------------------------------------------------------------------#

=head2 setStdout( $stream )

Set the STDOUT stream for the process that will be forked.

=over 2

B<Arguments>:

=over 13

=item stream

An object of type GLOB or GLOBREF that can be used to send the process's stdout.

=back

=back

=begin testing

  use Inca::Process::SessionFork;
  use File::Temp qw(tempfile);
  use IO::File;

  my $fh1 = tempfile( UNLINK => 1 );
  my $forker = new Inca::Process::SessionFork( stdout => $fh1 );
  is( $forker->getStdout()->fileno(), $fh1->fileno(),
      'set stdout from constructor worked' );
  my $fh2 = tempfile( UNLINK => 2 );
  $forker->setStdout( $fh2 );
  is( $forker->getStdout()->fileno(), $fh2->fileno(),
      'set stdout from set function worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setStdout {
  my ( $self, $stdout ) = validate_pos( @_, $SELF_PARAM_REQ, $IO_PARAM_REQ );

  $self->{stdout} = $stdout;
}

#-----------------------------------------------------------------------------#

=head2 wait( ) 

Wait for a process to complete.

=over 2

B<Returns>:

An integer containing the exit status of the process or -1 if unable to get
the exit status.

=back

=cut
#-----------------------------------------------------------------------------#
sub wait {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return waitpid( $self->{pid}, 0 ); 
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _initChild( $executable_n_args )
#
# This function is called after a fork from the child process.  It will
# setup the process's STDERR, call setsid to set up the new process group, and
# finally exec the process.
#
# Arguments:
#
# executable_n_args   A string indicating an executable and its arguments to
#                     be executed. 
#
# Returns:
#
# If successful, will never return; otherwise returns 0.
#
#-----------------------------------------------------------------------------#
sub _initChild {
  my ( $self, $executable_n_args ) = 
     validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  # make this subprocess the leader of a new process group; this has the
  # effect of daemonizing the process.
  my $gid = setsid();
  if ( $gid != $$ ) {
    $self->{logger}->logdie( "process group not setup: $!" );
  }

  if ( $self->hasStdout() ) {
    if ( ! STDOUT->fdopen($self->{stdout}, "w") ) { 
      $self->{logger}->error( "unable to redirect process stdout: $!" );
    }
  }

  if ( $self->hasStderr() ) {
    if ( ! STDERR->fdopen($self->{stderr}, "w") ) { 
      $self->{logger}->error( "unable to redirect process stderr: $!" );
    }
  }

  exec $executable_n_args or 
       $self->{logger}->logdie( "unable to exec '$executable_n_args': $!" );

  return 0; 
}

#-----------------------------------------------------------------------------#
# _killWaitTimesOut( )
#
# This function will get called from the kill function if the wait time after
# the processes is signaled is exceeded.
#-----------------------------------------------------------------------------#
sub _killWaitTimesOut {
  my ( $self, $wait ) = validate_pos( @_, $SELF_PARAM_REQ, 
                                      $POSITIVE_INTEGER_PARAM_REQ );

  $self->{logger}->info( 
    "Waited $wait seconds for processes to clean up...exitting" 
  );
  croak;
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=cut
