package Inca::ReporterManager::ReporterCache;

################################################################################

=head1 NAME

Inca::ReporterManager::ReporterCache - Manages the local cache of reporter packages

=head1 SYNOPSIS

=for example begin

  use Inca::ReporterManager::ReporterCache;
  my $rc = new Inca::ReporterManager::ReporterCache( "var/cache" );
  $rc->storePackage( "Inca::Reporter", "Reporter.pm", "2.0", "lib/perl", undef,
                     undef, "/tmp/inca.tmp.434" );
=for example end

=head1 DESCRIPTION

The Reporter Cache (RC) handles the maintenance and installation of
reporter packages (e.g., reporters, reporter libraries, and reporter
dependencies) which it receives from the Reporter Agent (who downloads it from
one or more Reporter Repositories).  Reporters will be stored in the "bin"
directory and libraries in the "lib/perl" directory.

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
use Inca::Constants qw(:params);
use Inca::Logger;
use Inca::Validate qw(:all);

# cpan
use XML::Simple;

# Perl standard
use Carp;
use Cwd;
use File::Copy;
use File::Spec;
use File::Path;

#=============================================================================#
# Global Vars
#=============================================================================#

our ($VERSION) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ         = { isa => "Inca::ReporterManager::ReporterCache" };
my $DEFAULT_ERROR_REPORTER = "INCA_ERROR_REPORTER";
my $REPOSITORY_FILE        = "repository.xml";
my %XML_OPTIONS            = (
  KeyAttr       => { },
  SuppressEmpty => undef
);
my %XML_OPTIONS_IN         = (
  ForceArray    => [ 'package' ],
  %XML_OPTIONS
);

#=============================================================================#

=head1 CLASS METHODS

=cut

#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( $location )

Class constructor which returns a new Inca::ReporterManager::ReporterCache
object.  The constructor must be called with $location and may be called with
any of the following attributes.

B<Arguments>:

=over 2 

=item location

A string containing the path to the local cache of reporter and libraries.
Reporters will be stored in the <location>/bin directory and libraries in
the <location>/lib/perl directory.

=back

B<Options>:

=over 13

=item errorReporterPath

A string containing the path to the special reporter used when generating
error reports

=back

=begin testing

  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;
  dies_ok { new Inca::ReporterManager::ReporterCache() } 'object created';
  lives_ok { new Inca::ReporterManager::ReporterCache( "var/cache" ) } 
           'object created';

=end testing

=cut

#-----------------------------------------------------------------------------#
sub new {
  my $this  = shift;
  my $class = ref($this) || $this;
  my $self  = {};

  bless( $self, $class );

  # set up defaults
  $self->{error_reporter} = undef;
  $self->{logger}         = Inca::Logger::get_logger();

  # set the location of the cache
  my $path_location = shift;
  if ( ! defined $path_location ) {
    $self->{logger}->logdie( "No path provided for reporter cache location" );
  }
  $self->setLocation( File::Spec->rel2abs($path_location) );

  # read options
  my %options = validate( @_, { errorReporterPath => $PATH_PARAM_OPT } );
  $self->setErrorReporterPath( $options{errorReporterPath} )
    if exists $options{errorReporterPath};

  $self->{catalogPath} = $self->{location} . "/" . $REPOSITORY_FILE;
  $self->{catalog} = { package => [] };
  if ( -e $self->{catalogPath} ) {
    # XMLin had problems reading in repository.xml on one system -- would
    # read in half the file and return parse error -- fix by reading in the
    # file ourselves and passing the string to XMLin
    open( FD, "<", $self->{catalogPath} );
    my $line; 
    my $catalogContent = "";
    while ( ($line=<FD>) ) {
      $catalogContent .= $line;
    }
    close FD;
    eval {
      $self->{catalog} = XMLin( 
        $catalogContent, 
        %XML_OPTIONS_IN,
        ForceArray => [qw(package dependency)] 
      );
    };
    if ($@) {
      $self->{logger}->error(
        "Problem reading repository catalog " .  $self->{catalogPath} . 
        ": $@; creating new file"
      );
    }
  }
  return $self;
}

#-----------------------------------------------------------------------------#

=head2 getDependencies( $name, $version )

Return the dependencies for the given reporter. 

=over 2

B<Arguments>:

=over 13

=item name

A string containing the name of the reporter to query the dependencies

=item version 

A string containing the version of the reporter to query the dependencies

=back

=back

=begin testing

  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;
  use Cwd;

  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  my @depends = $rc->getDependencies( 
    "grid.middleware.globus.unit.proxy", "1.5" 
  );
  is( scalar(@depends), 1, 'got one dependency' );
  is( $depends[0], "Inca::Reporter::GridProxy", 'got grid dependency' );

=end testing

=cut

#-----------------------------------------------------------------------------#
sub getDependencies {
  my ( $self, $name, $version ) =
    validate_pos( @_, $SELF_PARAM_REQ, SCALAR, SCALAR );

  my $package = $self->_getEntry( $name, $version );
  if ( defined $package && defined $package->{dependency} ) {
    return @{$package->{dependency}};
  } else {
    return ();
  }
}

#-----------------------------------------------------------------------------#

=head2 getErrorReporterPath( )

Get the error_reporter of the reporter repository on the local machine.

=over 2

B<Returns>:

A string containing the path to the local cache of reporter and libraries.

=back

=cut

#-----------------------------------------------------------------------------#
sub getErrorReporterPath {
  my ($self) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{error_reporter};
}

#-----------------------------------------------------------------------------#

=head2 getLocation( )

Get the location of the reporter repository on the local machine.

=over 2

B<Returns>:

A string containing the path to the local cache of reporter and libraries.

=back

=cut

#-----------------------------------------------------------------------------#
sub getLocation {
  my ($self) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{location};
}

#-----------------------------------------------------------------------------#

=head2 getPath( $name, $version )

Return a path to a local copy of a reporter using the reporter name and
version.

=over 2

B<Arguments>:

=over 13

=item name

A string containing the name of the reporter to execute

=item version 

A string containing the version of the reporter to execute

=back

=back

=begin testing

  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;
  use Cwd;

  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  my $path = $rc->getPath( "echo_report", "1" );
  is( $path, getcwd() . "/t/echo_report", 'getPath works for uri' );
  $path = $rc->getPath( "bogus_reporter", "1" );
  is( $path, undef, 'getPath returns undef for non-existent reporter' );

=end testing

=cut

#-----------------------------------------------------------------------------#
sub getPath {
  my ( $self, $name, $version ) =
    validate_pos( @_, $SELF_PARAM_REQ, SCALAR, SCALAR );

  my $package = $self->_getEntry( $name, $version );
  if ( defined $package ) {
    return $self->{location} . "/" . $package->{path};
  } else { 
    return undef;
  }
}

#-----------------------------------------------------------------------------#

=head2 setErrorReporterPath( $path )

Set the path to the special reporter that the reporter manager will use to
generate an error report if a reporter execution fails.  If the
path points to a non-existent file or is not executable, it errors out with a
'die' call.

=over 2

B<Arguments>:

=over 13

=item path

A string containing the path to the reporter.

=back

=back

=begin testing

  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;

  my $rc = new Inca::ReporterManager::ReporterCache( "var/cache",
                  errorReporterPath => "sbin/reporter-manager" );
  is($rc->getErrorReporterPath(), "sbin/reporter-manager", 
                  'set ErrorReporterPath works from init');
  $rc->setErrorReporterPath( "bin/inca-null-reporter" );
  is( $rc->getErrorReporterPath(), "bin/inca-null-reporter", 
      'set/getErrorReporterPath works' );
  dies_ok { $rc->setErrorPath( "/blah" ); } 'dies when points to no file';
  dies_ok { $rc->setErrorPath( "$ENV{HOME}/.ssh/known_hosts" ); } 
          'dies when points to non-executable file';

=end testing

=cut

#-----------------------------------------------------------------------------#
sub setErrorReporterPath {
  my ( $self, $path ) = validate_pos( @_, $SELF_PARAM_REQ, $PATH_PARAM_REQ );

  if ( !-f $path ) {
    $self->{logger}->logconfess("error reporter path '$path' is not a file");
  }
  elsif ( !-x $path ) {
    $self->{logger}->logconfess("error reporter at '$path' not executable");
  }
  $self->{error_reporter} = $path;
}

#-----------------------------------------------------------------------------#

=head2 setLocation( $path )

Set the location of the reporter repository on the local machine.  If the
path points to a non-existent directory, it errors out with a 'die' call.

=over 2

B<Arguments>:

=over 13

=item path

A string containing the path to the local cache of reporter and libraries.

=back

=back

=begin testing

  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;

  my $rc = new Inca::ReporterManager::ReporterCache( $ENV{HOME} );
  is( $rc->getLocation(), $ENV{HOME}, 'set Location works from constructor' );
  $rc->setLocation( "/tmp" );
  is( $rc->getLocation(), "/tmp", 'set/getLocation works' );
  dies_ok { $rc->getLocation( "blah" ); } 'dies when bad dir specified';

=end testing

=cut

#-----------------------------------------------------------------------------#
sub setLocation {
  my ( $self, $path ) = validate_pos( @_, $SELF_PARAM_REQ, $PATH_PARAM_REQ );

  if ( !-d $path ) {
    if ( !mkpath($path) ) {
      $self->{logger}->logconfess(
        "specified cache '$path' is not a directory; failed to create dir: $!"
      );
    }
  }

  $self->{location} = $path;
}

#-----------------------------------------------------------------------------#

=head2 storePackage( $name, $filename, $version, $installpath, $perms, 
                     $dependencies, $tmpfilename )

Store the specified package into the reporter cache.  

=over 2

B<Arguments>:

=over 13

=item name

A string containing the name of the reporter package.

=item filename

A string containing the filename that the package should be stored under.

=item version

A string containing the version of the package.

=item installpath

A string containing the directory the package should be stored under
relative to the reporter cache location.

=item perms

A string of format "[0-7][0-7][0-7]" that can be used to set the permissions
of the stored file after its written to disk.  A value of undef means that
no change of permissions is needed.

=item dependencies

A string containing a whitespace delimited list of dependencies or undef if
there are none.

=item content

A string containing the name of a temporary file that is storing the content 
of the package.

=back

=back

=begin testing

  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;
  use Cwd;
  untie *STDOUT;
  untie *STDERR;
  
  Inca::Logger->screen_init( "FATAL" );

  `rm -fr var/cache`;
  my $rc = new Inca::ReporterManager::ReporterCache( "var/cache" );
  my $filename = "/tmp/incatesttmp.$$";
  my $content = <<'FILE';
#!/usr/bin/perl -Icontrib/incareporters/lib/perl

use strict;
use warnings;
use Inca::Reporter::Version;

my $reporter = new Inca::Reporter::Version(
  version => 1.5,
  description => 'Reports the version of gcc',
  url => 'http://gcc.gnu.org',
  package_name => 'gcc'
);
$reporter->processArgv(@ARGV);

$reporter->setVersionByExecutable('gcc -dumpversion', '(.+)');
$reporter->print();
FILE
  open( FILE, ">$filename" ) || die "cannot open $filename";
  print FILE $content;
  close FILE;
  $rc->storePackage( 
    "cluster.compiler.gcc.version",
    "cluster.compiler.gcc.version-1.5",
    "1.5",
    "bin",
    "755",
    "Inca::Reporter::Version",
    $filename
  );
  ok( -x "var/cache/bin/cluster.compiler.gcc.version-1.5", "gcc executable" );
  
  $filename = "/tmp/incatesttmp.$$";
  $content = <<'FILE';
#!/usr/bin/perl -Icontrib/incareporters/lib/perl

use strict;
use warnings;
use Inca::Reporter::SimpleUnit;
use Inca::GridProxy;

my $reporter = new Inca::Reporter::SimpleUnit(
  version => 1.5,
  description => 'Verifies that user has valid proxy',
  url => 'http://www.globus.org/security/proxy.html',
  unit_name => 'validproxy'
);
$reporter->processArgv(@ARGV);

my $output = $reporter->loggedCommand('grid-proxy-info -timeleft');
if(!$output) {
  $reporter->unitFailure("grid-proxy-info failed: $!");
} else {
  $reporter->unitSuccess();
}
$reporter->print();
FILE
  open( FILE, ">$filename" ) || die "cannot open $filename";
  print FILE $content;
  close FILE;
  $rc->storePackage( 
    "grid.middleware.globus.unit.proxy",
    "grid.middleware.globus.unit.proxy-1.5",
    "1.5",
    "bin",
    "755",
    "Inca::Reporter::SimpleUnit Inca::GridProxy",
    $filename
  );
  ok( -x "var/cache/bin/grid.middleware.globus.unit.proxy-1.5", 
      "proxy executable" );
  
  open( FILE, ">$filename" ) || die "cannot open $filename";
  print FILE "";
  close FILE; 
  $rc->storePackage( 
    "Inca::Reporter",
    "Reporter.pm",
    "1.5",
    "lib/perl",
    undef,
    undef,
    $filename 
  );
  ok( ! -x "var/cache/lib/perl/Reporter.pm", "Reporter.pm not executable" );
  is( $rc->getPath( "Inca::Reporter", "1.5" ),
      getcwd() . "/var/cache/lib/perl/Reporter.pm",
      "getPath returned okay for Reporter.pm" );
  
  # test tar.gz with configure, make, make install   
  $rc->storePackage( 
    "appleseeds",
    "appleseeds-2.2.1.tar.gz",
    "2.2.1",
    "",
    undef,
    undef,
    "t/appleseeds-2.2.1.tar.gz" 
  );  
  ok( -f "var/cache/lib/libappleseeds.a", "libappleseeds.a exists" );
  ok( -f "var/cache/include/appleseeds/appleseeds.h", "appleseeds.h exists" );
  ok( ! -d "var/cache/build/appleseeds-2.2.1", "dir cleaned up" );
  ok( ! -f "var/cache/build/appleseeds-2.2.1.tar", "tar cleaned up" ); 
 
  # test tar.gz with make, make install   
  $rc->storePackage( 
    "makedist",
    "makedist.tar.gz",
    "1",
    "",
    undef,
    undef,
    "t/makedist.tar.gz" 
  );  
  ok( -f "var/cache/include/somefile.h", "somefile exists" );
 
   # test tar.gz with Makefile.PL  
   $rc->storePackage( 
    "Schedule::Cron",
    "Schedule-Cron-0.9.tar.gz",
    "0.9",
    "",
    undef,
    undef,
    "t/Schedule-Cron-0.9.tar.gz" 
  );  
  ok( -f "var/cache/lib/perl/Schedule/Cron.pm", "somefile exists" );
 
   # test tar.gz with Build.PL  
  $rc->storePackage(
    "Module::Build",
    "Module-Build-0.4210.tar.gz",
    "0.4210",
    "",
    undef,
    undef,
    "t/Module-Build-0.4210.tar.gz"
  );
  ok( -f "var/cache/lib/perl/Module/Build.pm", "Build.pm exists" );

   # test tar.gz with setup.py
  $rc->storePackage(
    "beautifulsoup4",
    "beautifulsoup4-4.3.2.tar.gz",
    "4.3.2",
    "",
    undef,
    undef,
    "t/beautifulsoup4-4.3.2.tar.gz"
  );
  ok( -f "var/cache/lib/python/bs4/__init__.py", "BeautifulSoup.py exists" );
  `chmod u+w var/cache/lib/python/bs4/__init__.py`;
  `echo > var/cache/lib/python/bs4/__init__.py`;
  my $size = (stat("var/cache/lib/python/bs4/__init__.py"))[7];

   # test tar.gz with setup.py and force
  $rc->storePackage(
    "beautifulsoup4",
    "beautifulsoup4-4.3.2.tar.gz",
    "4.3.2",
    "",
    undef,
    undef,
    "t/beautifulsoup4-4.3.2.tar.gz"
  );
  ok( -f "var/cache/lib/python/bs4/__init__.py", "BeautifulSoup.py exists" );
  my $newsize = (stat("var/cache/lib/python/bs4/__init__.py"))[7];
  ok( $newsize > $size, "BeautifulSoup.py file forced" );

  # test tar.gz with bad target  
  $rc->storePackage( 
    "baddist",
    "baddist.tar.gz",
    "1",
    "",
    undef,
    undef,
    "t/baddist.tar.gz" 
  );  
  my ($dir) = glob( "var/cache/build/baddist-*");
  ok( $dir =~ /baddist/, "bad dist directory kept" );
  ok( -f "$dir/incaBuildInstall.log", "build log file kept" );
    
  my $rc2 = new Inca::ReporterManager::ReporterCache( "var/cache" );
  is( $rc2->getPath( "Inca::Reporter", "1.5" ),
      getcwd() . "/var/cache/lib/perl/Reporter.pm",
      "getPath returned okay for Reporter.pm" );
  my @depends = $rc2->getDependencies( "cluster.compiler.gcc.version", "1.5" );
  is( scalar(@depends), 1, "got 1 dependency for gcc reporter" );
  is( $depends[0], "Inca::Reporter::Version", "got version for gcc reporter" );
  @depends = $rc2->getDependencies( "grid.middleware.globus.unit.proxy", "1.5");
  is( scalar(@depends), 2, "got 2 dependencies for proxy reporter" );
  `rm -fr var/cache`;

=end testing

=cut

#-----------------------------------------------------------------------------#
sub storePackage {
  my ( $self, $name, $filename, $version, $installpath, $perms, $dependencies,
       $tmpfilename ) =
    validate_pos( @_, $SELF_PARAM_REQ, SCALAR, SCALAR, SCALAR, SCALAR,
    SCALAR | UNDEF, SCALAR | UNDEF, SCALAR );

  my $package_dir =  File::Spec->catfile( $self->{location}, $installpath );
  if ( !-d $package_dir && !mkpath($package_dir) ) {
    $self->{logger}->error( 
      "installpath '$package_dir' for $name is not a directory; "
      . "failed to create dir: $!" 
    );
    return 0;
  }

  my $package_path = $self->{location} . "/" . $installpath . "/" . $filename;
  if ( !copy($tmpfilename, $package_path) ) {
    $self->{logger}->error("Unable to copy $tmpfilename to $package_path");
    return 0;
  }
  
  if ( defined $perms ) {
    chmod( oct($perms), $package_path );
  }
  
  $self->{logger}->info("Package '$name' written to $package_path");
  
  if ( $filename =~ /tar.gz$/ ) {
    my $origDir = getcwd();
    if ( ! $self->_installTarGz($filename) ) {
      $self->{logger}->error("Build/Install failed for $filename");
      # if a package doesn't have the correct dir structure, we need to make
      # sure we get back to where we started
      if ( ! chdir $origDir ) {
        $self->{logger}->error( "Unable to chdir to $origDir" );
      }
      return 0;
    }
  }
  
  return $self->_updateCatalog( 
    $name, $installpath . "/" . $filename, $version, $dependencies
  );
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
#
# _deleteEntry( $name, $version )
#
# Delete the catalog entry for the given package name and version
#
# Arguments:
#
# name A string containing the name of the reporter to delete
#
# version A string containing the version of the reporter to delete 
#
# Returns:
#
# Returns true if entry deleted and false if entry not found.

=begin testing
  
  use Inca::ReporterManager::ReporterCache;

  `rm -fr var/cache`;
  my $rc = new Inca::ReporterManager::ReporterCache( "var/cache" );
  $rc->_updateCatalog(
    "reporter1", "bin", "1.0", "Inca::Reporter::Version"
  );
  ok( defined $rc->getPath( "reporter1", "1.0" ), "entry added" );
  $rc->_deleteEntry( "reporter1", "1.0" );
  ok( ! defined $rc->getPath( "reporter1", "1.0" ), "entry deleted" );

=end testing

=cut

#-----------------------------------------------------------------------------#
sub _deleteEntry {
  my ( $self, $name, $version ) =
    validate_pos( @_, $SELF_PARAM_REQ, SCALAR, SCALAR );

  my $i = 0;
  for my $package ( @{$self->{catalog}->{package}} ) {
    if ( $package->{name} eq $name && $package->{version} eq $version ) {
      splice( @{$self->{catalog}->{package}}, $i, 1 );
      return 1;
    }
    $i++;
  }
  return 0;
}

#-----------------------------------------------------------------------------#
#
# _getEntry( $name, $version )
#
# Return the catalog entry for the given package name and version
#
# Arguments:
#
# name A string containing the name of the reporter to execute
#
# version A string containing the version of the reporter to execute
#
# Returns:
#
# A reference to a hash containing the matching package or undef if not found
#-----------------------------------------------------------------------------#
sub _getEntry {
  my ( $self, $name, $version ) =
    validate_pos( @_, $SELF_PARAM_REQ, SCALAR, SCALAR );

  for my $package ( @{$self->{catalog}->{package}} ) {
    if ( $package->{name} eq $name && $package->{version} eq $version ) {
      return $package;
    }
  }
  return undef;
}

#-----------------------------------------------------------------------------#
# _installTarGz( $filename )
#
# Build and install the specified tar.gz file.  The build method will depend
# on the contents of the tar.gz.  Build methods attempted are:
#
# ./configure --prefix=<cache location>; make; make install
# make INSTALL_DIR; make install

# perl Makefile.PL --prefix=<cache location>...; make; make install
# perl Build.PL --prefix=<cache location>....; ./BUILD; 
#
# Arguments:
#
# filename  A string containing the name of the tar.gz to build and install
#
# Returns:
#
# Returns true if build and install commands returned true
#-----------------------------------------------------------------------------#
sub _installTarGz {
  my ( $self, $filename ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );
  
  # save where we are now so we can come back
  my $origDir = getcwd();
  
  my $buildDir = File::Spec->catfile( $self->{location}, "build" );
  if ( ! -d $buildDir && ! mkdir($buildDir) ) {
    $self->{logger}->error( "Unable to create temp build dir $buildDir" );
    return 0; 
  }
  my $tarGz = File::Spec->catfile( $self->{location}, $filename);
  my $destTarGz = File::Spec->catfile($buildDir, $filename);
  $self->{logger}->debug( "Moving $tarGz to $destTarGz" );
  if ( ! rename($tarGz, $destTarGz) ) {
    $self->{logger}->error( "Unable to move $tarGz to $destTarGz: $!" );
    return 0; 
  }
  
  # extract tar.gz
  $self->{logger}->debug( "Changing dir to $buildDir" );
  if ( ! chdir $buildDir ) {
    $self->{logger}->error( "Unable to chdir to $buildDir" );
    return 0; 
  }
  $self->{logger}->debug( "Unzipping $filename" );
  my $out = `gunzip -f $filename 2>&1`;
  my $tarFilename = $filename; $tarFilename =~ s/.gz$//g; # strip off .gz
  if ( $? != 0 && ! -f $tarFilename ) {
    $self->{logger}->error( "Unable to gunzip $filename: $! $out" );
    return 0; 
  }
  $self->{logger}->debug( "Extracting $tarFilename" );
  $out = `gtar xvf $tarFilename 2>&1`;
  my $dirname = $tarFilename; $dirname =~ s/.tar$//g;
  if ( $? != 0 && ! -d $dirname ) {
    $out = `tar xvf $tarFilename 2>&1`;
    if ( $? != 0 && ! -d $dirname ) {
      $self->{logger}->error( "Unable to extract $tarFilename: $! $out" );
      return 0;
    } 
  }
  $self->{logger}->debug( "Changing dir to $dirname" );
  if ( ! chdir $dirname ) {
    $self->{logger}->error( "Unable to chdir to $dirname" );
    return 0;
  }
  
  # build methods
  my %buildMethods = (
    'configure' => [ "./configure --prefix=$self->{location}",
                     "gmake || make",
                     "gmake install || make install" ],
    'Makefile' => [ "gmake INSTALL_DIR=$self->{location} || make INSTALL_DIR=$self->{location}",
                    "gmake INSTALL_DIR=$self->{location} install || make INSTALL_DIR=$self->{location} install" ],
    'makefile' => [ "gmake INSTALL_DIR=$self->{location} || make INSTALL_DIR=$self->{location}",
                    "gmake INSTALL_DIR=$self->{location} install || make INSTALL_DIR=$self->{location} install" ],
    'Makefile.PL' => [ "perl -I$self->{location}/lib/perl Makefile.PL " .
                       "PREFIX=$self->{location}/lib/perl " .
                       "LIB=$self->{location}/lib/perl " .
                       "INSTALLDIRS=perl " .
                       "INSTALLSCRIPT=$self->{location}/bin " .
                       "INSTALLMAN1DIR=$self->{location}/man/man1 " . 
                       "INSTALLMAN3DIR=$self->{location}/man/man3",  
                       "gmake || make",
                       "gmake install || make install" ],
    'Build.PL' => [ "perl -I$self->{location}/lib/perl Build.PL " .
                    "--install_path lib=$self->{location}/lib/perl " .
                    "--install_path libdoc=$self->{location}/man/man3 " .
                    "--install_path bindoc=$self->{location}/man/man1 " .
                    "--install_path bin=$self->{location}/bin " .
                    "--install_path script=$self->{location}/bin " .
                    "--install_path arch=$self->{location}/lib/perl",
                    "perl -I$self->{location}/lib/perl Build",
                    "perl -I$self->{location}/lib/perl Build install" ], 
    'setup.py' => [ "mkdir -p $self->{location}/lib/python",
                    "env PYTHONPATH=$self->{location}/lib/python python setup.py install --root=$self->{location} --install-lib=lib/python --install-scripts=bin --install-data=share --install-headers=include --force" ] 
   );
  
  for my $file ( sort(keys %buildMethods) ) {
    $self->{logger}->info("checking for $file\n");
    if ( -f $file ) {
      $self->{logger}->info( "Using build method $file for $filename" );
      for my $cmd ( @{$buildMethods{$file}} ) {
        $self->{logger}->info( "Executing command $cmd" );   
        system( "echo 'EXEC $cmd' >> incaBuildInstall.log 2>&1" );
        system( "$cmd >> incaBuildInstall.log 2>&1" );
      }
      last;
    }
  }
  
  # back to where we were
  if ( ! chdir $origDir ) {
    $self->{logger}->error( "Unable to chdir to $origDir" );
    return 0; 
  }
  
  # remove directory and tar if build succeeded
  my $dir = File::Spec->catfile($buildDir, $dirname);
  my $archiveDir = "$dir" . "-" . time();
  $self->{logger}->info( "Moving $dir to $archiveDir" );
  move( $dir, $archiveDir );
  unlink File::Spec->catfile($buildDir, "$tarFilename" );
  return 1;
}

#-----------------------------------------------------------------------------#
# _updateCatalog( $name, $relPath, $version, $dependencies )
#
# Update the repository cache catalog with the following information about
# a package.
#
# Arguments:
#
# name      A string that contains the name of the package.
#
# relPath   A string containing the relative path to the package location
#           in the cache.
#
# version   A string containing the version of the package.
#
# dependencies A string containing a whitespace delimited list of dependencies
#              or undef if there are none.
#
# Returns:
#
# Returns true if the catalog was successfully update and false otherwise.
#-----------------------------------------------------------------------------#
sub _updateCatalog {
  my ( $self, $name, $relPath, $version, $dependencies ) =
    validate_pos( @_, $SELF_PARAM_REQ, SCALAR, SCALAR, SCALAR, SCALAR | UNDEF );

  my $package = $self->_getEntry( $name, $version );
  $self->_deleteEntry( $name, $version ) if ( defined $package ); 
  my %dependenciesAttr;
  if ( defined $dependencies ) {
    my @depends = split( /\s+/, $dependencies );
    $dependenciesAttr{dependency} = [ @depends ];
  }
  my $entry = {
    name => $name,
    path => $relPath, 
    version => $version,
    %dependenciesAttr
  };
  push( @{$self->{catalog}->{package}}, $entry );
  eval {
    XMLout(
      $self->{catalog},
      %XML_OPTIONS,
      RootName   => 'catalog',
      NoAttr     => 1,
      OutputFile => $self->{catalogPath}
    );
  };
  if ($@) {
    $self->{logger}->error("Problem writing repository catalog: $@");
    return 0;
  }
  return 1;
}

1;    # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

None so far.

=cut
