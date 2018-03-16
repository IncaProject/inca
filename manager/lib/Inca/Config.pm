package Inca::Config;

################################################################################

=head1 NAME

Inca::Config - Utility class for dealing with Inca config files

=head1 SYNOPSIS

=for example begin

  use Inca::Config;
  my $config = new Inca::Config();
  $config->read( "inca.xml" );
  my @suites = $config->getSuites();

=for example end

=head1 DESCRIPTION

An Inca configuration file represents the configuration of resources, suites,
and repositories currently running in an Inca deployment.  Note, currently
this module only provides functions to setting and accessing suites.

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
use Inca::Constants qw(:all);
use Inca::Logger;
use Inca::Config::Suite;
use Inca::Validate qw(:all);

# Perl standard
use Carp;

# CPAN
use XML::Simple;

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $INCA_NAMESPACE = "xmlns:inca=\"http://inca.sdsc.edu/dataModel/inca_2.0\"";
my $SELF_PARAM_REQ = { isa => __PACKAGE__ };
my %XML_OPTIONS = ( ForceArray => [ qw(seriesConfig suite arg) ],
                    KeyAttr => { }, 
                    SuppressEmpty => undef );

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( )

Class constructor which returns a new Inca::Config object.  

=begin testing

  use Inca::Config;
  use Test::Exception;
  lives_ok { new Inca::Config() } 'object created';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  # initialize vars
  $self->{repositories} = [];
  $self->{resourceConfig} = [];
  $self->{suites} = {};
  $self->{logger} = Inca::Logger->get_logger( $class );

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 addSeriesConfig( $sc )

Add the specified series configs to the suite in the configuration.

=over 2

B<Arguments>:

=over 5

=item sc 

An object of type Inca::Config::Suite::SeriesConfig.  

=back

=back

=cut
#-----------------------------------------------------------------------------#
sub addSeriesConfig {
  my ($self, $sc) = validate_pos(@_, $SELF_PARAM_REQ, $SERIESCONFIG_PARAM_REQ );

  if ( ! exists $self->{suites}->{$sc->getGuid()} ) {
      $self->{suites}->{$sc->getGuid()} = new Inca::Config::Suite();
      $self->{suites}->{$sc->getGuid()}->setGuid( $sc->{guid} );
  }
  $self->{suites}->{$sc->getGuid()}->addSeriesConfig( $sc );
}

#-----------------------------------------------------------------------------#

=head2 addSuite( $suite )

Add the specified suite to the configuration.

=over 2

B<Arguments>:

=over 5

=item suite

An object of type Inca::Config::Suite.  

=back

=back

=cut
#-----------------------------------------------------------------------------#
sub addSuite{
  my ($self, $suite ) = validate_pos(@_, $SELF_PARAM_REQ, $SUITE_PARAM_REQ );

  $self->{suites}->{$suite->getGuid()} = $suite;
}

#-----------------------------------------------------------------------------#

=head2 getSuites( )

Return the list of suites in the configuration.

=over 2

B<Returns>:

An array of Inca::Config::Suite objects.  

=back

=begin testing

  use Inca::Config;
  use Test::Exception;

  my $config = new Inca::Config();
  $config->read( "t/config.xml" ); 
  my @suites = $config->getSuites();
  is( scalar(@suites), 1, "found 1 suite" );
  isa_ok( $suites[0], "Inca::Config::Suite", "suite is a suite object" );
  is( scalar($suites[0]->getSeriesConfigs()), 10, "found 10 series" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getSuites {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return values %{$self->{suites}};
}

#-----------------------------------------------------------------------------#

=head2 read( $xmlOrFile )

Read an Inca configuration file into the object.  The configuration is
structured as follows:

<inca>
  <suites>
    <suite>
      <seriesConfig>
        <!-- see L<Inca::Config::Suite::SeriesConfig> for content here -->
      </reporterGroup>
      ...
    </suite>
  </suites>
</inca>

=over 2

B<Arguments>:

=over 13

=item xmlorFile

A string containing Inca configuration xml or a path to an XML file containing
an Inca configuration

=back

B<Returns>:

Returns 1 if there are no errors reading the config file; otherwise 
returns 0.

=back

=begin testing

  use Inca::Config;
  use Test::Exception;

  my $config = new Inca::Config();
  my $result = $config->read( "t/config.xml" ); 
  is( $result, 1, "read on good config - returns true" );
  is( ($config->getSuites())[0]->getGuid(),
      "incas://rocks-101.sdsc.edu:6323/sampleSuite",
      "read on good config - found first suite" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub read {
  my ( $self, $xmlOrFileOrHash ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  my $incaDoc;
  eval {
    $incaDoc = XMLin( $xmlOrFileOrHash, %XML_OPTIONS );
  };
  if ( $@ ) {
    $self->{logger}->error( "Problem reading inca configuration: $@" );
    return 0;
  }

  if ( ! exists $incaDoc->{suites} || ! exists $incaDoc->{suites}->{suite} ) {
    $self->{logger}->warn( "Unable to find any suites inca config file" );
    return 0;
  }

  for my $suiteXml ( @{$incaDoc->{suites}->{suite}} ) {
    my $suite = new Inca::Config::Suite(); 
    if ( ! $suite->read( $suiteXml ) ) {
      next; # error printed out in Inca::Config::Suite
    }
    $self->addSuite( $suite );
  }

  return 1;
}

#-----------------------------------------------------------------------------#

=head2 write( $filename )

Write an Inca configuration object to file following the inca configuration
file schema

B<Arguments>:

=over 13

=item filename

A string containing a path to an file to which the Inca configuration XML
will be written to.

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub write {
  my ( $self, $filename ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  # create a inca configuration document with suites as the only top level
  # element
  my $inca = { inca => { suites => { suite => [] } } };
  my $suites = $inca->{inca}->{suites};

  # populate the configuration with the series configs in cron
  for my $suite ( values %{$self->{suites}} ) {
    push( @{$suites->{suite}}, $suite->getXmlHashArray() );
  }

  # write to disk
  if ( open( my $fh, ">$filename" ) ) {
    my $xml = XMLout 
      ( $inca, AttrIndent => 1, KeyAttr => [], KeepRoot => 1, NoAttr => 1, 
        RootName => "inca" );
    # write namespace information -- no method in XML::Simple
    $xml =~ s/<inca>/<inca:inca $INCA_NAMESPACE>/;
    $xml =~ s/<\/inca>/<\/inca:inca>/; 
    print $fh $xml;
    close $fh;
    return 1;
  } else {
    $self->{logger}->error( "Unable to open $filename to write schedule" );
    return 0;
  }
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

No known problems.

=head1 SEE ALSO

L<Inca::Config::Suite>
L<Inca::Config::Suite::SeriesConfig>

=cut
