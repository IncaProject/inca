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

my $Original_File = 'lib/Inca/Process/SessionFork.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 112 lib/Inca/Process/SessionFork.pm

  use Inca::Process::SessionFork;
  use Test::Exception;
  lives_ok { new Inca::Process::SessionFork() } 'object created';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 202 lib/Inca/Process/SessionFork.pm

  use Inca::Process::SessionFork;

  my $fork = new Inca::Process::SessionFork();
  ok( ! $fork->hasStderr(), 'hasStderr - false value' );
  $fork->setStderr( *STDOUT );
  ok( $fork->hasStderr(), 'hasStderr - true value' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 235 lib/Inca/Process/SessionFork.pm

  use Inca::Process::SessionFork;

  my $fork = new Inca::Process::SessionFork();
  ok( ! $fork->hasStdout(), 'hasStdout - false value' );
  $fork->setStdout( *STDOUT );
  ok( $fork->hasStdout(), 'hasStdout - true value' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 318 lib/Inca/Process/SessionFork.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 387 lib/Inca/Process/SessionFork.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 496 lib/Inca/Process/SessionFork.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 540 lib/Inca/Process/SessionFork.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Process/SessionFork.pm

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

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

