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

my $Original_File = 'lib/Inca/Net/MockDepot.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 76 lib/Inca/Net/MockDepot.pm

  use Inca::Net::MockDepot;
  use Test::Exception;
  lives_ok { new Inca::Net::MockDepot() } 'object created';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 239 lib/Inca/Net/MockDepot.pm

  use Inca::Net::MockDepot;
  use Test::Exception;

  my $depot = new Inca::Net::MockDepot();
  $depot->readReportsFromSSL( 0, 8080, "ca1", "etc/trusted");


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Net/MockDepot.pm

  use Inca::Net::MockDepot;
  my $depot = new Inca::Net::MockDepot();
  my $numReports = $depot->readReportsFromSSL(
    4, 8434, "ca1", "t/certs/trusted"
  );
  $numReports = $depot->readReportsFromFile("./depot.tmp.$$", 0)

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

