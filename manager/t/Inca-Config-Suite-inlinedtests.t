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

my $Original_File = 'lib/Inca/Config/Suite.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 83 lib/Inca/Config/Suite.pm

  use Inca::Config::Suite;
  use Test::Exception;
  lives_ok { new Inca::Config::Suite() } 'object created';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 132 lib/Inca/Config/Suite.pm

  use Inca::Config::Suite;
  my $suite = new Inca::Config::Suite( guid => "guid1" );
  my $sc = new Inca::Config::Suite::SeriesConfig( 
    name => "reporterA",
    version => 5,
    context => "reporterA",
    action => 'add'
  );
  $suite->addSeriesConfig( $sc );
  $sc->setAction( "delete" );
  $suite->addSeriesConfig( $sc );
  is( scalar($suite->getSeriesConfigs()), 0, '0 configs');


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 181 lib/Inca/Config/Suite.pm

  use Inca::Config::Suite;
  use Test::Exception;

  my $suite = new Inca::Config::Suite();
  $suite->read( "t/suite.xml" ); 
  is( $suite->getGuid(), "incas://localhost:6323/testSuite", "got guid" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 214 lib/Inca/Config/Suite.pm

  use Inca::Config::Suite;
  use Test::Exception;

  my $suite = new Inca::Config::Suite();
  $suite->read( "t/suite.xml" ); 
  my @scs = $suite->getSeriesConfigs();
  is( scalar(@scs), 2, "found 2 series configs" );
  isa_ok( $scs[0], "Inca::Config::Suite::SeriesConfig", 
          "series config is a series config" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 252 lib/Inca/Config/Suite.pm

  use Inca::Config::Suite;
  my $suite = new Inca::Config::Suite( guid => "guid1" );
  my $sc = new Inca::Config::Suite::SeriesConfig( 
    name => "reporterA",
    version => 5,
    context => "reporterA",
    action => 'add'
  );
  $suite->addSeriesConfig( $sc );
  my $suiteXml = {
    guid => "guid1",
    seriesConfigs => { 
      seriesConfig => [ 
        { 
          series => {
            name => "reporterA",
            version => 5,
            context => "reporterA",
            args => { arg => [] }
          },
          schedule => {},
          action => 'add',
          'resourceSetName' => 'localhost'
        }
      ]
    }
  };
  ok( eq_hash($suite->getXmlHashArray(), $suiteXml),
      'bare bones suite converts');


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 339 lib/Inca/Config/Suite.pm

  use Inca::Config::Suite;
  use Test::Exception;

  my $suite = new Inca::Config::Suite();
  my $result;
  $result = $suite->read( "t/suite.xml" ); 
  is( $result, 1, "read on good suite - returns true" );
  is( ($suite->getSeriesConfigs())[0]->getName(),
      "echo_report",
      "read on good suite - found first name" );
  $result = $suite->read( "t/suitedash.xml" ); 
  is( $result, 1, "read on good suite - returns true" );
  my @files = qw(t/suite_badxml.xml t/suite_noaction.xml t/suite_badconfig.xml);
  for my $file ( @files ) {
    is( $suite->read( $file ), 0, "read on $file - returns false" );
  }


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 432 lib/Inca/Config/Suite.pm

  use Inca::Config::Suite;
  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;
  use Cwd;

  my $suite = new Inca::Config::Suite();
  my $rc = new Inca::ReporterManager::ReporterCache( "t" );
  $suite->read( "t/suite.xml" ); 
  for my $sc ( $suite->getSeriesConfigs() ) {
    is( undef, $sc->getPath(), 'no path set now' );
  }
  my $uri = "incas://localhost:3234";
  $suite->resolveReporters( $rc, $uri );
  my $cwd = getcwd();
  for my $sc ( $suite->getSeriesConfigs() ) {
    ok( defined($sc->getPath()), 'path set now' );
    like( 
      $sc->getPath(), 
      qr/$cwd/, 
      "series config has set path"
    );
    ok( ! $sc->hasProxyContact(), "proxy contact not found" );
  }

  $suite = new Inca::Config::Suite();
  $suite->read( "t/grid_suite.xml" ); 
  $suite->resolveReporters( $rc, $uri );
  for my $sc ( $suite->getSeriesConfigs() ) {
    ok( $sc->hasProxyContact(), "perl proxy contact found" );
    is( $sc->getProxyContact(), $uri, "perl proxy contact uri found" );
  }

  my $pythonSuite = '<?xml version = "1.0" encoding = "UTF-8"?>
<st:suite xmlns:st = "http://inca.sdsc.edu/dataModel/suite_2.0" xmlns:xsi = "http://www.w3.org/2001/XMLSchema-instance" >
  <seriesConfigs>
    <seriesConfig>
    <series>
      <name>grid.proxy</name>
      <version>1.5</version>
      <context>grid.proxy</context>
    </series>
    <resourceSetName>localhost</resourceSetName>
    <schedule/>
    <action>add</action>
  </seriesConfig>
  </seriesConfigs>
  <guid>incas://localhost:6323/gridSuite</guid>
</st:suite>
  ';
  $suite = new Inca::Config::Suite();
  $suite->read( $pythonSuite ); 
  $suite->resolveReporters( $rc, $uri );
  for my $sc ( $suite->getSeriesConfigs() ) {
    ok( $sc->hasProxyContact(), "python proxy contact found" );
    is( $sc->getProxyContact(), $uri, "python proxy contact uri found" );
  }


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 549 lib/Inca/Config/Suite.pm

  use Inca::Config::Suite;
  use Test::Exception;

  my $guid1 = "inca://localhost:9080/suiteA";
  my $guid2 = "incas://localhost:8232/suiteB";
  my $suite = new Inca::Config::Suite();
  is ( $suite->getGuid(), undef, 'get guid was undef from constructor' );
  $suite = new Inca::Config::Suite( guid => $guid1 ); 
  is ( $suite->getGuid(), $guid1, 'set guid worked from constructor' );
  $suite->setGuid( $guid2 );
  is ( $suite->getGuid(), $guid2, 'set/getGuid worked' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 590 lib/Inca/Config/Suite.pm

  use Inca::Config::Suite;
  use Inca::ReporterManager::Scheduler;
  use File::Temp qw(tempfile);
  
  my $suite = new Inca::Config::Suite( guid => "incas://a.sdsc.edu/suiteB" );
  my $sc = Inca::ReporterManager::Scheduler->_createTestConfig(
    "echo_report" 
  );
  $sc->addArgument( "help", "no" );
  $sc->addArgument( "version", "yes" );
  $suite->addSeriesConfig( $sc );
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig(
    "echo_report" 
  );
  $sc->addArgument( "help", "no" );
  $suite->addSeriesConfig( $sc );
  my ($fh, $filename) = tempfile();
  $suite->write( $filename );
  ok( -f $filename, "sched written to disk" );
  local $/; 
  # enable localized slurp mode  
  open( $fh, "<$filename" );  
  my $content = <$fh>;  
  close $fh;  
  my $expected = "<suite:suite xmlns:suite=\"http://inca.sdsc.edu/dataModel/suite_2.0\">
  <guid>incas://a.sdsc.edu/suiteB</guid>
  <seriesConfigs>
    <seriesConfig>
      <action>add</action>
      <nickname>testA</nickname>
      <resourceSetName>localhost</resourceSetName>
      <schedule></schedule>
      <series>
        <args>
          <arg>
            <name>help</name>
            <value>no</value>
          </arg>
          <arg>
            <name>version</name>
            <value>yes</value>
          </arg>
        </args>
        <context>echo_report</context>
        <name>echo_report</name>
        <uri>incas://b.sdsc.edu/reporters/echo_report</uri>
        <version>1</version>
      </series>
    </seriesConfig>
    <seriesConfig>
      <action>add</action>
      <nickname>testA</nickname>
      <resourceSetName>localhost</resourceSetName>
      <schedule></schedule>
      <series>
        <args>
          <arg>
            <name>help</name>
            <value>no</value>
          </arg>
        </args>
        <context>echo_report</context>
        <name>echo_report</name>
        <uri>incas://b.sdsc.edu/reporters/echo_report</uri>
        <version>1</version>
      </series>
    </seriesConfig>
  </seriesConfigs>
</suite:suite>
";
  is( $content, $expected, "suite content checks out" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Config/Suite.pm

  use Inca::Config::Suite;
  my $suite = new Inca::Config::Suite();
  $suite->read( "suite.xml" );
  my @scs = $suite->getSeriesConfigs();

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

