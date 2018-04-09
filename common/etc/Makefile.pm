package Inca::Makefile;

###############################################################################

=head1 NAME

Inca::Makefile - Allows for sharing of common Inca Makefile.PL features

=head1 SYNOPSIS

  use ExtUtils::MakeMaker;
  use File::Basename;
  require "" . dirname( $0 ) . "/MakefileCommonInca.PM";

  WriteMakefile(
    Inca::Makefile::getCommonOptions( "Inca-Component" )
  );
   
=head1 DESCRIPTION

Encapsulates most of the functionality needed in the Makefile.PL for
Inca components so they can be shared rather than duplicated in each
Inca component directory.  This file will be checked into the common
area of the Inca source tree and copied to the Inca component directory
during distribution.  Note, this file has a .PM suffix so that it will be
ignored by Makefile.PL (i.e., not recognized as a module that needs
to be installed).

=cut
###############################################################################


#=============================================================================#
# Usage
#=============================================================================#
use strict;
use warnings;
use Carp;
use Cwd;
use File::Spec;

#=============================================================================#
# Global Vars
#=============================================================================#
our ( $VERSION ) = '$Revision: 1.3 $' =~ 'Revision: (.*) ';

my @DEPENDENCY_FILE_LOCATIONS = qw(Dependencies contrib/Dependencies);
my @CONTRIB_TYPES = qw(AUTOCONF PERLSTANDARD PERLNON PERLSSL PERLINCAREPORTER);
my $VERSION_FILE = "version";

#=============================================================================#

=head1 METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#

=head2 getCommonOptions( $name )

Return options to ExtUtils::MakeMaker that are common across all Inca
Perl components.

=over 2 

B<Arguments>:

=over 13

=item name

A string containing the name of the Inca component that will be built

=back

B<Returns>:

Returns a hash array of options that can be fed directly into WriteMakefile

B<Notes>:

Will issue a croak command if a fatal error has occurred

=back

=cut
#-----------------------------------------------------------------------------#
sub getCommonOptions{
  my $component = shift;

  my $version = _getVersionFromFile( _getFilenameFromScriptDir($VERSION_FILE) );
  my $log = _getFilenameFromScriptDir( "build.log" );

  # get dependencies from file
  my $dep_file = _searchForFileFromPaths( @DEPENDENCY_FILE_LOCATIONS );
  croak "Unable to locate Dependency file\n" if ! defined $dep_file;
  my ( $dep_ordered, $dep_by_type ) = 
    _extractDependenciesFromFile( $dep_file, @CONTRIB_TYPES );

  # construct the tarball names from dep_ordered
  my @dirnames;
  for my $dep ( @{$dep_ordered} ) {
    my ($dirname) = $dep =~ /^\"(\S+)\.tar\.gz.*/;
    push( @dirnames, $dirname );
  }

  # create variables for each of the module types to place in the makefile
  # need to be " " separated
  my %dep_by_type_formatted;
  for my $type ( @CONTRIB_TYPES ) {
    $dep_by_type_formatted{$type} = join( " ", @{$dep_by_type->{$type}} );
  }

  my @etc_files;
  for my $file ( glob( "etc/* etc/common/*" ) ) {
    push( @etc_files, $file ) if -f $file;
  }

  return (
     'NAME' => $component,

    'VERSION' => $version,

    'EXE_FILES' => [ glob("bin/*") ],

    'MAKEFILE' => 'Makefile.perl.inc',

    'macro' => { 
      DEPEND => join(" ",@{$dep_ordered}),
      packageName => $component,
      SBIN_FILES => join(" ", glob("sbin/*")),
      ETC_FILES => join(" ", @etc_files),
      LOG => $log,
      %dep_by_type_formatted
    },

    'realclean' => { FILES => "MANIFEST *.tar.gz META.yml MANIFEST.bak $log"},

    'depend' => { 
      makemakerdflt => join(" ",@dirnames)
    }
  );
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _getFilenameFromScriptDir( $file )
#
# Construct a path to the $file by pre-pending the directory of the executing
# script. 
#
# exists.
#
# Arguments:
#
# file    a string containing the name of the file
#-----------------------------------------------------------------------------#
sub _getFilenameFromScriptDir {
  my $file = shift;

  return File::Spec->catdir( getcwd(), $file );
}

#-----------------------------------------------------------------------------#
# _searchForFileFromPaths( @candidate_paths )
#
# Iterate through @candidate_paths and return the first path where the file
# exists.
#
# Arguments:
#
# candidate_paths     a list of paths indicating possible locations for
#                         the desired file.
#-----------------------------------------------------------------------------#
sub _searchForFileFromPaths {
  my @candidate_paths = @_;
  my $file_path = undef;

  for my $candidate_path ( @candidate_paths ) {
    if ( -f $candidate_path && -r $candidate_path ) {
      $file_path = $candidate_path;
      last;
    }
  }
  return $file_path;
}

#-----------------------------------------------------------------------------#
# _extractDependenciesFromFile( $file )
#
# Iterate through the contrib file dependencies and return a reference to list
# of ordered module names and a reference to a hash of module types.
#
# Arguments:
#
# file    a string containing a path to the dependency file
# 
# Returns:
#
# An array containing:
# 
#   A reference to an array containing the module name dependencies ordered
# 
#   A reference to a hash array where the keys are the module types and the
#   values are a reference to an array of module names.
#-----------------------------------------------------------------------------#
sub _extractDependenciesFromFile {
  my $file = shift;
  my @contrib_types = @_;
  my $dependencies_ordered = [];
  my $contrib_dependency = {};

  map( $contrib_dependency->{$_} = [], @contrib_types );
  open( FD, "<$file" ) or croak "Unable to open contrib dependency file $file";
  while ( <FD> ) {
    my ($moduleName, $module, $type) = $_ =~ /(\S+)\s+(\S+)\s+(\S+)/;
    my ($module_version) = $module =~ /-([\w\.]*)$/; # grab version 
    $module_version =~ s/[A-Za-z]//g; # perl doesn't understand letters 
    eval "require $moduleName; $moduleName->VERSION($module_version)";
    next if ! $@;
    print "Will install Inca prerequisite $moduleName\n";
    push( @{$dependencies_ordered}, "\"$module.tar.gz\"" );
    push( @{$contrib_dependency->{$type}}, $module );
  }
  return ( $dependencies_ordered, $contrib_dependency );
}

#-----------------------------------------------------------------------------#
# _getVersionFromFile( $file )
#
# Extract the version of the module from a file.
#
# Arguments:
#
# file    a string containing a path to the version file
# 
# Returns:
#
# A string containing the version from the file or undefined if unable to.
#-----------------------------------------------------------------------------#
sub _getVersionFromFile {
  my $file = shift;
  my ($version, $minorversion);

  open VERSION, "<$file" or croak "Unable to open version file";

  # read major version first in the format majorVersion=<VERSION>
  my $line = <VERSION>;
  ($version) = $line =~ /\S+=(\S+)/;

  return "$version";
}

#-----------------------------------------------------------------------------#
# Override MakeMaker methods
#-----------------------------------------------------------------------------#
package MY; # so that "SUPER" works right

#-----------------------------------------------------------------------------#
# dist_core( )
#
# Add pod2html and populate-depends targets to dist command
#
# Returns:
#
# A string containing the dist_core generated content
#-----------------------------------------------------------------------------#
sub dist_core {
  my $dist_core = shift->SUPER::dist_core(@_);
  $dist_core =~ s/dist\s*: /dist : doc populate-depends /;
  return $dist_core;
}


__END__

=head1 EXAMPLE
  
  use ExtUtils::MakeMaker;
  use File::Basename;
  require "" . dirname( $0 ) . "/MakefileCommonInca.PM";

  WriteMakefile(
    Inca::Makefile::getCommonOptions( "Inca-ReporterManager" )
  );
   
=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=cut
