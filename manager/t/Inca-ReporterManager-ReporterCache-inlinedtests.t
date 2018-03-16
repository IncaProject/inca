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

my $Original_File = 'lib/Inca/ReporterManager/ReporterCache.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 117 lib/Inca/ReporterManager/ReporterCache.pm

  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;
  dies_ok { new Inca::ReporterManager::ReporterCache() } 'object created';
  lives_ok { new Inca::ReporterManager::ReporterCache( "var/cache" ) } 
           'object created';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 207 lib/Inca/ReporterManager/ReporterCache.pm

  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;
  use Cwd;

  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  my @depends = $rc->getDependencies( 
    "grid.middleware.globus.unit.proxy", "1.5" 
  );
  is( scalar(@depends), 1, 'got one dependency' );
  is( $depends[0], "Inca::Reporter::GridProxy", 'got grid dependency' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 308 lib/Inca/ReporterManager/ReporterCache.pm

  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;
  use Cwd;

  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  my $path = $rc->getPath( "echo_report", "1" );
  is( $path, getcwd() . "/t/echo_report", 'getPath works for uri' );
  $path = $rc->getPath( "bogus_reporter", "1" );
  is( $path, undef, 'getPath returns undef for non-existent reporter' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 360 lib/Inca/ReporterManager/ReporterCache.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 414 lib/Inca/ReporterManager/ReporterCache.pm

  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;

  my $rc = new Inca::ReporterManager::ReporterCache( $ENV{HOME} );
  is( $rc->getLocation(), $ENV{HOME}, 'set Location works from constructor' );
  $rc->setLocation( "/tmp" );
  is( $rc->getLocation(), "/tmp", 'set/getLocation works' );
  dies_ok { $rc->getLocation( "blah" ); } 'dies when bad dir specified';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 494 lib/Inca/ReporterManager/ReporterCache.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 771 lib/Inca/ReporterManager/ReporterCache.pm
  
  use Inca::ReporterManager::ReporterCache;

  `rm -fr var/cache`;
  my $rc = new Inca::ReporterManager::ReporterCache( "var/cache" );
  $rc->_updateCatalog(
    "reporter1", "bin", "1.0", "Inca::Reporter::Version"
  );
  ok( defined $rc->getPath( "reporter1", "1.0" ), "entry added" );
  $rc->_deleteEntry( "reporter1", "1.0" );
  ok( ! defined $rc->getPath( "reporter1", "1.0" ), "entry deleted" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/ReporterManager/ReporterCache.pm

  use Inca::ReporterManager::ReporterCache;
  my $rc = new Inca::ReporterManager::ReporterCache( "var/cache" );
  $rc->storePackage( "Inca::Reporter", "Reporter.pm", "2.0", "lib/perl", undef,
                     undef, "/tmp/inca.tmp.434" );
;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

