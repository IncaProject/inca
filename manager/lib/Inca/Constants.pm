package Inca::Constants;

###############################################################################

=head1 NAME

Inca::Constants - Exports common related constants into module

=head1 SYNOPSIS

=for example begin

  use Inca::Constants ":all";
  my $megabytes = 1024 / $KILOS_TO_MEGA;

  use Inca::Constants qw($SECS_TO_MIN);
  my $secs = 5 * $SECS_TO_MIN;

=for example end
   
=for example_testing
  is( $megabytes, 1, 'all import check' );
  is( $secs, 300, 'explicit import' );

=head1 DESCRIPTION

This module will export time related constants into a module.  Current list
includes time and byte related constants:

Time:
  SECS_TO_MIN = 60
  SECS_TO_HOUR = 3600
  SECS_TO_DAY = 86400

Bytes:
  KILOS_TO_MEGA = 1024;

Params: to be used in Params::Validate functions
  URI_PARAM_REQ 
  URI_PARAM_OPT
  HOST_PARAM_REQ
  HOST_PARAM_OPT
  PORT_PARAM_REQ
  PORT_PARAM_OPT
  PATH_PARAM_REQ
  PATH_PARAM_OPT
  BOOLEAN_PARAM_REQ
  BOOLEAN_PARAM_OPT
  POSITIVE_INTEGER_PARAM_REQ
  POSITIVE_INTEGER_PARAM_OPT
  INTEGER_PARAM_REQ
  INTEGER_PARAM_OPT
  ALPHANUMERIC_PARAM_REQ
  ALPHANUMERIC_PARAM_OPT
  IO_PARAM_REQ
  IO_PARAM_OPT
  CLIENT_PARAM_REQ
  CLIENT_PARAM_OPT
  SERIESCONFIG_PARAM_REQ
  SERIESCONFIG_PARAM_OPT
  STORAGE_POLICY_PARAM_REQ
  STORAGE_POLICY_PARAM_OPT
  SUSPEND_PARAM_OPT
  SUSPEND_PARAM_REQ
  USAGE_PARAM_REQ
  USAGE_PARAM_OPT
  REPORTER_CACHE_PARAM_REQ
  REPORTER_CACHE_PARAM_OPT
  PROCESS_PARAM_REQ
  PROCESS_PARAM_OPT
  PROFILER_PARAM_REQ
  PROFILER_PARAM_OPT
  SCALAR_PARAM_OPT
  HASHREF_PARAM_OPT
  ARRAYREF_PARAM_OPT
  SUITE_PARAM_REQ
  SUITE_PARAM_OPT
  STATEMENT_PARAM_REQ
  STATEMENT_PARAM_OPT

=begin testing

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

=end testing

=cut
###############################################################################


#=============================================================================#
# Usage
#=============================================================================#

# pragmas
use strict;
use warnings;
use vars qw(@ISA @EXPORT);

# Inca
use Inca::Validate ':all';

# Perl standard
use Carp;
require Exporter;

#=============================================================================#
# Export help
#=============================================================================#
@ISA = qw(Exporter);

# list of variables defined below
my @DEFAULTS = qw( $DEFAULT_DEPOT_TIMEOUT $DEFAULT_CHECK_PERIOD );
my @TIME = qw( $SECS_TO_MIN $SECS_TO_HOUR $SECS_TO_DAY );
my @BYTES = qw( $KILOS_TO_MEGA );
my @PARAMS = qw( $URI_PARAM_REQ $URI_PARAM_OPT $HOST_PARAM_REQ $HOST_PARAM_OPT $PORT_PARAM_REQ $PORT_PARAM_OPT $PATH_PARAM_REQ $PATH_PARAM_OPT $BOOLEAN_PARAM_REQ $BOOLEAN_PARAM_OPT $POSITIVE_INTEGER_PARAM_REQ $POSITIVE_INTEGER_PARAM_OPT $INTEGER_PARAM_REQ $INTEGER_PARAM_OPT $ALPHANUMERIC_PARAM_REQ $ALPHANUMERIC_PARAM_OPT $IO_PARAM_REQ $IO_PARAM_OPT $CLIENT_PARAM_REQ $CLIENT_PARAM_OPT $SERIESCONFIG_PARAM_REQ $SERIESCONFIG_PARAM_OPT $STORAGE_POLICY_PARAM_REQ $STORAGE_POLICY_PARAM_OPT $USAGE_PARAM_REQ $USAGE_PARAM_OPT $REPORTER_CACHE_PARAM_REQ $REPORTER_CACHE_PARAM_OPT $PROCESS_PARAM_REQ $PROCESS_PARAM_OPT $PROFILER_PARAM_REQ $PROFILER_PARAM_OPT $SCALAR_PARAM_OPT $HASHREF_PARAM_OPT $ARRAYREF_PARAM_OPT $SUITE_PARAM_REQ $SUITE_PARAM_OPT $STATEMENT_PARAM_REQ $STATEMENT_PARAM_OPT $SUSPEND_REGEX $SUSPEND_PARAM_OPT $SUSPEND_PARAM_REQ); 

# export tags
our %EXPORT_TAGS = ( 'all' => [ @DEFAULTS, @TIME, @BYTES, @PARAMS ],
                     'defaults' => [ @DEFAULTS],
                     'time' => [ @TIME ],
                     'bytes' => [ @BYTES ],
                     'params' => [ @PARAMS ] );

# export on request
our @EXPORT_OK = ( @{ $EXPORT_TAGS{'all'} } );

our ( $VERSION ) = '$Revision: 1.3 $' =~ 'Revision: (.*) ';

#=============================================================================#
# Config Constants
#=============================================================================#
our $DEFAULT_DEPOT_TIMEOUT = 120;
our $DEFAULT_CHECK_PERIOD = 2;

#=============================================================================#
# Time Constants
#=============================================================================#
our $SECS_TO_MIN = 60;
our $SECS_TO_HOUR = 60 * $SECS_TO_MIN;
our $SECS_TO_DAY = 24 * $SECS_TO_HOUR;

#=============================================================================#
# Byte Constants
#=============================================================================#
our $KILOS_TO_MEGA = 1024;

#=============================================================================#
# Constants for common parameters used in functions where Inca::Validate is
# used to verify parameters
#=============================================================================#

# simple types
my $URI_REGEX = qr/^[\S\.]+:\S+$/;
our $URI_PARAM_REQ = { regex => $URI_REGEX };
our $URI_PARAM_OPT = _addOptional( $URI_PARAM_REQ ); 

my $HOST_REGEX = qr/^[\S\.]+$/;
our $HOST_PARAM_REQ = { regex => $HOST_REGEX };
our $HOST_PARAM_OPT = _addOptional( $HOST_PARAM_REQ );

my $PORT_REGEX = qr/^\d+$/;
our $PORT_PARAM_REQ = { regex => $PORT_REGEX };
our $PORT_PARAM_OPT = _addOptional( $PORT_PARAM_REQ );

our $PATH_PARAM_REQ = { type => SCALAR };
our $PATH_PARAM_OPT = _addOptional( $PATH_PARAM_REQ );

my $BOOLEAN_REGEX = qr/^0|1$/;
our $BOOLEAN_PARAM_REQ = { regex => $BOOLEAN_REGEX };
our $BOOLEAN_PARAM_OPT = _addOptional( $BOOLEAN_PARAM_REQ );

my $POSITIVE_INTEGER_REGEX = qr/\d+/;
our $POSITIVE_INTEGER_PARAM_REQ = { regex => $POSITIVE_INTEGER_REGEX };
our $POSITIVE_INTEGER_PARAM_OPT = _addOptional( $POSITIVE_INTEGER_PARAM_REQ );

my $INTEGER_REGEX = qr/-*\d+/;
our $INTEGER_PARAM_REQ = { regex => $INTEGER_REGEX };
our $INTEGER_PARAM_OPT = _addOptional( $INTEGER_PARAM_REQ );

my $ALPHANUMERIC_REGEX = qr/^[\w-]+$/;
our $ALPHANUMERIC_PARAM_REQ = { regex => $ALPHANUMERIC_REGEX };
our $ALPHANUMERIC_PARAM_OPT = _addOptional( $ALPHANUMERIC_PARAM_REQ );

our $IO_PARAM_REQ = { type => GLOB | GLOBREF };
our $IO_PARAM_OPT = _addOptional( $IO_PARAM_REQ );

our $SCALAR_PARAM_OPT = { type => SCALAR, optional => 1 };
our $HASHREF_PARAM_OPT = { type => HASHREF, optional => 1 };
our $ARRAYREF_PARAM_OPT = { type => ARRAYREF, optional => 1 };

# Inca defined objects
our $CLIENT_PARAM_REQ = { isa => "Inca::Net::Client" };
our $CLIENT_PARAM_OPT = _addOptional( $CLIENT_PARAM_REQ );

our $SERIESCONFIG_PARAM_REQ = { isa => "Inca::Config::Suite::SeriesConfig" };
our $SERIESCONFIG_PARAM_OPT = _addOptional( $SERIESCONFIG_PARAM_REQ );

our $STORAGE_POLICY_PARAM_REQ = { isa => "Inca::Config::Suite::StoragePolicy" };
our $STORAGE_POLICY_PARAM_OPT = _addOptional( $STORAGE_POLICY_PARAM_REQ );

our $USAGE_PARAM_REQ = { isa => "Inca::Process::Usage" };
our $USAGE_PARAM_OPT = _addOptional( $USAGE_PARAM_REQ );

our $REPORTER_CACHE_PARAM_REQ = {isa => "Inca::ReporterManager::ReporterCache"};
our $REPORTER_CACHE_PARAM_OPT = _addOptional( $REPORTER_CACHE_PARAM_REQ );

our $PROCESS_PARAM_REQ = { isa => "Inca::Process" };
our $PROCESS_PARAM_OPT = _addOptional( $PROCESS_PARAM_REQ );

our $PROFILER_PARAM_REQ = { isa => "Inca::Process::Profiler" };
our $PROFILER_PARAM_OPT = _addOptional( $PROCESS_PARAM_REQ );

our $SUITE_PARAM_REQ = { isa => "Inca::Config::Suite" };
our $SUITE_PARAM_OPT = _addOptional( $SUITE_PARAM_REQ );

our $STATEMENT_PARAM_REQ = { isa => "Inca::Net::Protocol::Statement" };
our $STATEMENT_PARAM_OPT = _addOptional( $STATEMENT_PARAM_REQ );

my $SUSPEND_REGEX = qr/^load\d+\s*>\s*[\d\.]+$/;
our $SUSPEND_PARAM_REQ = { regex => $SUSPEND_REGEX };
our $SUSPEND_PARAM_OPT = _addOptional( $SUSPEND_PARAM_REQ ); 

#=============================================================================#
# Tiny helper functions
#=============================================================================#

#-----------------------------------------------------------------------------#
# _addOptional( $parameter )
#
# Creates an optional parameter description based on that contained in 
# $parameter.  E.g., if the following was in parameter
#
# { regex => qr/^[\w\.]+:\S+$/ }
#
# the function would return
#
# { regex => qr/^[\w\.]+:\S+$/, optional => 1 }
#
#-----------------------------------------------------------------------------#
sub _addOptional {
  my $required = shift;
  my $optional = {};
 
  # copy values
  %{$optional} = %{$required};

  # add optional
  $optional->{optional} = 1;

  return $optional;
}



__END__

=head1 EXAMPLE
  
  use Inca::Constants ":all";
  my $secs = 50 * $SECS_TO_MIN;

=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=cut
