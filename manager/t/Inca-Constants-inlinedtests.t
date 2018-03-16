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

my $Original_File = 'lib/Inca/Constants.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 82 lib/Inca/Constants.pm

  use Test::Exception;
  use strict;

  use Inca::Constants ":bytes";
  lives_ok {my $secs = 50 * $KILOS_TO_MEGA} 'bytes imports okay';

  use Inca::Constants ":params";
  use Inca::Validate;
  my @args = ( "localhost" );
  my ( $host ) = validate_pos( @args, $HOST_PARAM_REQ );
  is( $host, "localhost", "params export ok" );
  my $param_hash = { regex => qr/^[\S\.]+:\S+$/, optional => 1 };
  ok( eq_hash($URI_PARAM_OPT, $param_hash), 'addOptional works' );
  $param_hash = { regex => qr/^[\S\.]+:\S+$/ };
  ok( eq_hash($URI_PARAM_REQ, $param_hash), 
      'addOptional does not effect required value' );
  ok( defined($POSITIVE_INTEGER_PARAM_REQ), 
      'POSITIVE_INTEGER_PARAM_REQ defined' );
  ok( defined($POSITIVE_INTEGER_PARAM_OPT), 
      'POSITIVE_INTEGER_PARAM_OPT defined' );
  ok( defined($IO_PARAM_OPT), 'IO_PARAM_OPT defined' );
  ok( defined($URI_PARAM_OPT), 'URI_PARAM_OPT defined' );

  @args = ( 0 );
  my ( $boolean ) = validate_pos( @args, $BOOLEAN_PARAM_REQ );
  is( $boolean, 0, "params - boolean exports ok" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Constants.pm

  use Inca::Constants ":all";
  my $megabytes = 1024 / $KILOS_TO_MEGA;

  use Inca::Constants qw($SECS_TO_MIN);
  my $secs = 5 * $SECS_TO_MIN;




;

  }
};
is($@, '', "example from line 12");

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 12 lib/Inca/Constants.pm

  use Inca::Constants ":all";
  my $megabytes = 1024 / $KILOS_TO_MEGA;

  use Inca::Constants qw($SECS_TO_MIN);
  my $secs = 5 * $SECS_TO_MIN;




  is( $megabytes, 1, 'all import check' );
  is( $secs, 300, 'explicit import' );

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

