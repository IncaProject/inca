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

my $Original_File = 'lib/Inca/Process/Usage.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 77 lib/Inca/Process/Usage.pm

  use Inca::Process::Usage;
  use Test::Exception;
  lives_ok { new Inca::Process::Usage() } 'object created';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 180 lib/Inca/Process/Usage.pm

  my $a = new Inca::Process::Usage();
  $a->setCpuTime(5);
  $a->setWallClockTime(10);
  $a->setMemory(10);
  my $b = new Inca::Process::Usage();
  $b->setCpuTime(5);
  $b->setWallClockTime(12);
  $b->setMemory(10);
  ok( $b->greaterThan($a), "basic - greaterThan with true value" );
  ok( ! $a->greaterThan($b), "basic - greaterThan with false value" );

  my $c = new Inca::Process::Usage();
  $c->setCpuTime(.51);
  $c->setWallClockTime(1);
  $c->setMemory(46.16796875);
  my $d = new Inca::Process::Usage();
  $d->setCpuTime(4.84);
  $d->setWallClockTime(13);
  $d->setMemory(64);
  ok( ! $c->greaterThan($d), "real - greaterThan with false value" );
  ok( $d->greaterThan($c), "real - greaterThan with true value" );

  my $e = new Inca::Process::Usage();
  $e->setMemory(10);
  my $f = new Inca::Process::Usage();
  $f->setMemory(20);
  ok( ! $e->greaterThan($f), "partial - greaterThan with false value" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 251 lib/Inca/Process/Usage.pm

  use Inca::Process::Usage;

  my $usage = new Inca::Process::Usage();
  ok( ! $usage->hasCpuTime(), 'hasCpuTime - false value' );
  $usage->setCpuTime( -1 );
  ok( ! $usage->hasCpuTime(), 'hasCpuTime - false for neg value' );
  $usage->setCpuTime( 10 );
  ok( $usage->hasCpuTime(), 'hasCpuTime - true value' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 288 lib/Inca/Process/Usage.pm

  use Inca::Process::Usage;

  my $usage = new Inca::Process::Usage();
  ok( ! $usage->hasMemory(), 'hasMemory - false value' );
  $usage->setMemory( -1 );
  ok( ! $usage->hasMemory(), 'hasMemory - false for neg value' );
  $usage->setMemory( 10 );
  ok( $usage->hasMemory(), 'hasMemory - true value' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 325 lib/Inca/Process/Usage.pm

  use Inca::Process::Usage;

  my $usage = new Inca::Process::Usage();
  ok( ! $usage->hasWallClockTime(), 'hasWallClockTime - false value' );
  $usage->setWallClockTime( -1 );
  ok( ! $usage->hasWallClockTime(), 'hasWallClockTime - false for neg value' );
  $usage->setWallClockTime( 10 );
  ok( $usage->hasWallClockTime(), 'hasWallClockTime - true value' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 374 lib/Inca/Process/Usage.pm

  my $a = new Inca::Process::Usage();
  $a->setCpuTime(5);
  $a->setWallClockTime(10);
  $a->setMemory(10);
  my $b = new Inca::Process::Usage();
  $b->setCpuTime(5);
  $b->setWallClockTime(12);
  $b->setMemory(10);
  is( $b->limitsExceededString($a), 
      "wall clock time limit exceeded 10 secs",
      "basic - greaterThanStr with wallclocktime" );

  my $c = new Inca::Process::Usage();
  $c->setCpuTime(.51);
  $c->setWallClockTime(1);
  $c->setMemory(46.16796875);
  my $d = new Inca::Process::Usage();
  $d->setCpuTime(4.84);
  $d->setWallClockTime(13);
  $d->setMemory(64);
  is( $d->limitsExceededString($c), 
      "cpu time limit exceeded 0.51 secs",
      "real - greaterThan with true value" );

  my $e = new Inca::Process::Usage();
  $e->setMemory(10);
  my $f = new Inca::Process::Usage();
  $f->setMemory(20);
  is( $f->limitsExceededString($e), 
      "memory limit exceeded 10 MB",
      "partial - greaterThan with false value" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 455 lib/Inca/Process/Usage.pm

  use Inca::Process::Usage;
  use Test::Exception;
  my $usage = new Inca::Process::Usage();
  $usage->setCpuTime( 5 );
  is( $usage->getCpuTime(), 5, 'set/getCpuTime functions' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 494 lib/Inca/Process/Usage.pm

  use Inca::Process::Usage;
  use Test::Exception;
  my $usage = new Inca::Process::Usage();
  $usage->setMemory( 100 );
  is( $usage->getMemory(), 100, 'set/getMemory functions' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 534 lib/Inca/Process/Usage.pm

  use Inca::Process::Usage;
  use Test::Exception;
  my $usage = new Inca::Process::Usage();
  $usage->setWallClockTime( 10 );
  is( $usage->getWallClockTime(), 10, 'set/getWallClockTime functions' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 559 lib/Inca/Process/Usage.pm

  use Inca::Process::Usage;

  my $usage = new Inca::Process::Usage();
  $usage->zeroValues();
  is( $usage->getWallClockTime(), 0, 'zeroValues - wall clock' );
  is( $usage->getCpuTime(), 0, 'zeroValues - CPU' );
  is( $usage->getMemory(), 0, 'zeroValues - memory' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 595 lib/Inca/Process/Usage.pm

  use Inca::Process::Usage;

  my $printed = <<TEST;
cpu_secs=5
memory_mb=10
TEST
  my $usage = new Inca::Process::Usage();
  $usage->setCpuTime(5);
  $usage->setMemory(10);
  is( $usage->toString(), $printed, "toString - partial displays ok" );

  $printed = <<TEST;
cpu_secs=5
wall_secs=10
memory_mb=10
TEST
  $usage->setWallClockTime(10);
  is( $usage->toString(), $printed, "toString - full object" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Process/Usage.pm

  use Inca::Process::Usage;
  my $usage = new Inca::Process::Usage();
  $usage->setMemory( 100 );
  $usage->setCpuTime( 2 );
  $usage->setWallClockTime( 5 );




;

  }
};
is($@, '', "example from line 12");

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 12 lib/Inca/Process/Usage.pm

  use Inca::Process::Usage;
  my $usage = new Inca::Process::Usage();
  $usage->setMemory( 100 );
  $usage->setCpuTime( 2 );
  $usage->setWallClockTime( 5 );




  is( $usage->getMemory(), 100, 'getMemory from example' );
  is( $usage->getCpuTime(), 2, 'getCpuTime from example' );
  is( $usage->getWallClockTime(), 5, 'getWallClockTime from example' );

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

