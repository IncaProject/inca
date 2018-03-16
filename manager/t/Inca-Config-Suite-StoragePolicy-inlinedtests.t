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

my $Original_File = 'lib/Inca/Config/Suite/StoragePolicy.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 109 lib/Inca/Config/Suite/StoragePolicy.pm

  use Inca::Config::Suite::StoragePolicy;
  use Test::Exception;
  my $sp = new Inca::Config::Suite::StoragePolicy();
  lives_ok { $sp->addDepots( "cuzco.sdsc.edu:8234" ) } 'basic addDepots';
  my @depots = qw( cuzco.sdsc.edu:8234 inca.sdsc.edu:8235 inca.sdsc.edu:8236 );
  lives_ok { $sp->addDepots(@depots) } 'addDepots w/ 3 depots';
  dies_ok { $sp->addDepots() } 'addDepots w/ no depots dies';
  dies_ok { $sp->addDepots( "cuzco.sdsc.edu" ) } 'addDepots w/o port dies';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 152 lib/Inca/Config/Suite/StoragePolicy.pm

  use Inca::Config::Suite::StoragePolicy;
  use Test::Exception;
  my $sp = new Inca::Config::Suite::StoragePolicy();
  $sp->addDepots( "cuzco.sdsc.edu:8234" );
  my $sp2 = new Inca::Config::Suite::StoragePolicy();
  my @depots = qw( cuzco.sdsc.edu:8234 inca.sdsc.edu:8235 inca.sdsc.edu:8236 );
  $sp2->addDepots(@depots); 
  ok( ! $sp->equals($sp2), "equals returns false when different length" );
  $sp->addDepots( qw(inca.sdsc.edu:8235 inca.sdsc.edu:8236) );
  ok( $sp->equals($sp2), "equals returns true when true" );
  my $sp3 = new Inca::Config::Suite::StoragePolicy();
  $sp3->addDepots(qw(cuzco.sdsc.edu:8234 inca.sdsc.edu:8235 inca.sdsc.edu:823));
  ok( ! $sp->equals($sp3), "equals returns false when different" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Config/Suite/StoragePolicy.pm

  use Inca::Config::Suite::StoragePolicy;
  my $sp = new Inca::Config::Suite::StoragePolicy();
  $sp->addDepots( "cuzco.sdsc.edu:8258", "inca.sdsc.edu:8235" );

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

