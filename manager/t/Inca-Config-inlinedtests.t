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

my $Original_File = 'lib/Inca/Config.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 80 lib/Inca/Config.pm

  use Inca::Config;
  use Test::Exception;
  lives_ok { new Inca::Config() } 'object created';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 180 lib/Inca/Config.pm

  use Inca::Config;
  use Test::Exception;

  my $config = new Inca::Config();
  $config->read( "t/config.xml" ); 
  my @suites = $config->getSuites();
  is( scalar(@suites), 1, "found 1 suite" );
  isa_ok( $suites[0], "Inca::Config::Suite", "suite is a suite object" );
  is( scalar($suites[0]->getSeriesConfigs()), 10, "found 10 series" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 240 lib/Inca/Config.pm

  use Inca::Config;
  use Test::Exception;

  my $config = new Inca::Config();
  my $result = $config->read( "t/config.xml" ); 
  is( $result, 1, "read on good config - returns true" );
  is( ($config->getSuites())[0]->getGuid(),
      "incas://rocks-101.sdsc.edu:6323/sampleSuite",
      "read on good config - found first suite" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 302 lib/Inca/Config.pm

  use Inca::Config;
  use Inca::ReporterManager::Scheduler;

  my $config = new Inca::Config();
  my $suite = new Inca::Config::Suite();
  my $sc = Inca::ReporterManager::Scheduler->_createTestConfig(
    "echo_report" 
  );
  $sc->addArgument( "help", "no" );
  $sc->addArgument( "version", "yes" );
  $config->addSeriesConfig( $sc );
  $sc = Inca::ReporterManager::Scheduler->_createTestConfig(
    "echo_report" 
  );
  $sc->addArgument( "help", "no" );
  $config->addSeriesConfig( $sc );
  my $schedFile = "schedule.xml";
  $config->write( $schedFile );
  ok( -f $schedFile, "sched written to disk" );
  local $/; 
  # enable localized slurp mode  
  open( my $fh, "<$schedFile" );  
  my $content = <$fh>;  
  close $fh;  
  my $expected = "<inca:inca xmlns:inca=\"http://inca.sdsc.edu/dataModel/inca_2.0\">
  <suites>
    <suite>
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
    </suite>
  </suites>
</inca:inca>
";
  is( $content, $expected, "sched content checks out" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Config.pm

  use Inca::Config;
  my $config = new Inca::Config();
  $config->read( "inca.xml" );
  my @suites = $config->getSuites();

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

