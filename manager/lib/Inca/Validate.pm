package Inca::Validate;

###############################################################################

=head1 NAME

Inca::Validate - Wrapper to encapsulate dependence on Params::Validate

=head1 SYNOPSIS

=for example begin

  use Inca::Validate ":all";

=for example end
   
=head1 DESCRIPTION

This module wraps the functions exported by Params::Validate, allowing access
to them when the module is installed and preventing clients from puking
otherwise.

=for example_testing
  is( SCALAR, 1, "SCALAR imported" );

=cut
###############################################################################


#=============================================================================#
# Usage
#=============================================================================#

# pragmas
use strict;
use warnings;
use vars qw(@ISA @EXPORT @EXPORT_OK);

# Perl standard
require Exporter;

#=============================================================================#
# Export help
#=============================================================================#
@ISA = qw(Exporter);

# export tags

our %EXPORT_TAGS = (
  'all' => [
    'validate', 'validate_pos', 'validation_options', 'validate_with',
    'SCALAR', 'ARRAYREF', 'HASHREF', 'CODEREF', 'GLOB', 'GLOBREF', 'SCALARREF',
    'UNKNOWN', 'UNDEF', 'OBJECT', 'HANDLE', 'BOOLEAN'
  ]
);
@EXPORT_OK = ( @{ $EXPORT_TAGS{all} } );
@EXPORT = qw( validate validate_pos );
my ($pvalidate, $pvalidation_options, $pvalidate_pos, $pvalidate_with);
my ($pSCALAR, $pARRAYREF, $pHASHREF, $pCODEREF, $pGLOB, $pGLOBREF, $pSCALARREF,
    $pUNKNOWN, $pUNDEF, $pOBJECT, $pHANDLE, $pBOOLEAN);

BEGIN {
  ($pvalidate, $pvalidation_options, $pvalidate_pos, $pvalidate_with) =
    (\&ignore, \&ignore, \&ignore, \&ignore);
  ($pSCALAR, $pARRAYREF, $pHASHREF, $pCODEREF, $pGLOB, $pGLOBREF, $pSCALARREF,
   $pUNKNOWN, $pUNDEF, $pOBJECT, $pHANDLE, $pBOOLEAN) =
    (\&zero, \&zero, \&zero, \&zero, \&zero, \&zero, \&zero,
     \&zero, \&zero, \&zero, \&zero, \&zero);
  eval {
    require Params::Validate;
    $pvalidate = \&Params::Validate::validate;
    $pvalidation_options = \&Params::Validate::validation_options;
    $pvalidate_pos = \&Params::Validate::validate_pos;
    $pvalidate_with = \&Params::Validate::validate_with;
    $pSCALAR = \&Params::Validate::SCALAR;
    $pARRAYREF = \&Params::Validate::ARRAYREF;
    $pHASHREF = \&Params::Validate::HASHREF;
    $pCODEREF = \&Params::Validate::CODEREF;
    $pGLOB = \&Params::Validate::GLOB;
    $pGLOBREF = \&Params::Validate::GLOBREF;
    $pSCALARREF = \&Params::Validate::SCALARREF;
    $pUNKNOWN = \&Params::Validate::UNKNOWN;
    $pUNDEF = \&Params::Validate::UNDEF;
    $pOBJECT = \&Params::Validate::OBJECT;
    $pHANDLE = \&Params::Validate::HANDLE;
    $pBOOLEAN = \&Params::Validate::BOOLEAN;
    &Params::Validate::validation_options(stack_skip => 2);
  };
}

#=============================================================================#
# A default do-nothing fn for when Params::Validate is unavailable, and
# wrappers for each Parmams::Validate export when it is.  The clever prototype
# of validate{,_pos,_with} is copied from the Validate module; it forces the
# first param to be an array reference, preventing the flattening of the
# caller's @_ into the remaining params.
#=============================================================================#
sub ignore {
  print "Ignore called\n";
}
sub validate(\@@) {
  my $argref = shift;
  &{$pvalidate}($argref, @_);
}
sub validation_options {
  &{$pvalidation_options}(@_);
}
sub validate_pos(\@@) {
  my $argref = shift;
  &{$pvalidate_pos}($argref, @_);
}
sub validate_with  {
  my $argref = shift;
  &{$pvalidate_with}($argref, @_);
}

sub zero {
  return 0;
}
sub SCALAR {
  return &{$pSCALAR}();
}
sub ARRAYREF {
  return &{$pARRAYREF}();
}
sub HASHREF {
  return &{$pHASHREF}();
}
sub CODEREF {
  return &{$pCODEREF}();
}
sub GLOB {
  return &{$pGLOB}();
}
sub GLOBREF {
  return &{$pGLOBREF}();
}
sub SCALARREF {
  return &{$pSCALARREF}();
}
sub UNKNOWN {
  return &{$pUNKNOWN}();
}
sub UNDEF {
  return &{$pUNDEF}();
}
sub OBJECT {
  return &{$pOBJECT}();
}
sub HANDLE {
  return &{$pHANDLE}();
}
sub BOOLEAN {
  return &{$pBOOLEAN}();
}

1;

__END__

=head1 EXAMPLE
  
  use Inca::Validate ":all";
  sub mysub {
    # Two required, one optional param.
    validate_pos(1, 1, 0);
  }

=head1 AUTHOR

Jim Hayes <jhayes@sdsc.edu>

=cut
