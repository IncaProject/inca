package Inca::Config::Suite;

################################################################################

=head1 NAME

Inca::Config::Suite - Utility class for dealing with Inca suites

=head1 SYNOPSIS

=for example begin

  use Inca::Config::Suite;
  my $suite = new Inca::Config::Suite();
  $suite->read( "suite.xml" );
  my @scs = $suite->getSeriesConfigs();

=for example end

=head1 DESCRIPTION

An Inca suite is a request to change (i.e., add or delete) the data
collection on a resource.  It is an XML document that contains a list of
seriesConfigs.  Each seriesConfig contains an action (add or delete), 
a scheduler, and a reporter.  For each reporter, there is a name, version,
context, and an optional list of input arguments, limits, and execution
priority.

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
use Inca::Config::Suite::SeriesConfig;
use Inca::Validate qw(:all);

# Perl standard
use Carp;

# CPAN
use XML::Simple;

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ = { isa => __PACKAGE__ };
my $INCA_NAMESPACE = "xmlns:suite=\"http://inca.sdsc.edu/dataModel/suite_2.0\"";
my %XML_OPTIONS = ( ForceArray => [ qw(seriesConfig arg) ],
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

Class constructor which returns a new Inca::Config::Suite object.  

=begin testing

  use Inca::Config::Suite;
  use Test::Exception;
  lives_ok { new Inca::Config::Suite() } 'object created';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  # initialize vars
  $self->{configs} = [];
  $self->{guid} = undef;
  $self->{logger} = Inca::Logger->get_logger( $class );

  my %options = validate( @_, { guid => $SCALAR_PARAM_OPT } );
  
  $self->setGuid( $options{guid} ) if exists $options{guid};

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 addSeriesConfig( $sc )

Add the specified series configs to the suite.

=over 2

B<Arguments>:

=over 5

=item sc 

An object of type Inca::Config::Suite::SeriesConfig.  

=back

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub addSeriesConfig {
  my ($self, $sc) = validate_pos(@_, $SELF_PARAM_REQ, $SERIESCONFIG_PARAM_REQ);

  if ( $sc->getAction() eq "delete" ) {
    my $length = scalar(@{$self->{configs}});
    for ( my $i = 0; $i < $length; $i++ ) {
      if ( $sc->equals($self->{configs}->[$i]) ) {
        splice( @{$self->{configs}}, $i, 1);
        last;
      } 
    }
  } else {
    push( @{$self->{configs}}, $sc );
  }
}

#-----------------------------------------------------------------------------#

=head2 getGuid( )

Return the guid of the suite.

=over 2

B<Returns>:

A string containing the guid of the suite.

=back

=begin testing

  use Inca::Config::Suite;
  use Test::Exception;

  my $suite = new Inca::Config::Suite();
  $suite->read( "t/suite.xml" ); 
  is( $suite->getGuid(), "incas://localhost:6323/testSuite", "got guid" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getGuid {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{guid};
}

#-----------------------------------------------------------------------------#

=head2 getSeriesConfigs( )

Return the list of series configs in the suite.

=over 2

B<Returns>:

An array of Inca::Config::Suite::SeriesConfig objects.  

=back

=begin testing

  use Inca::Config::Suite;
  use Test::Exception;

  my $suite = new Inca::Config::Suite();
  $suite->read( "t/suite.xml" ); 
  my @scs = $suite->getSeriesConfigs();
  is( scalar(@scs), 2, "found 2 series configs" );
  isa_ok( $scs[0], "Inca::Config::Suite::SeriesConfig", 
          "series config is a series config" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getSeriesConfigs {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return @{$self->{configs}};
}

#-----------------------------------------------------------------------------#

=head2 getXmlHashArray( )

Return a hash array representation of the suite object that is appropriate for
incorporating into a larger hash array that will be formatted into XML by
XML::Simple.

=over 2

B<Returns>:

A hash array representation of the suite object.

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getXmlHashArray {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  # required parts of series config
  my $suiteXml = {
    seriesConfigs => { seriesConfig => [] },
    guid => $self->getGuid()
  };

  for my $sc ( $self->getSeriesConfigs() ) {
    my $scs = $suiteXml->{seriesConfigs}->{seriesConfig};
    push( @{$scs}, $sc->getXmlHashArray() );
  }

  return $suiteXml;
}

#-----------------------------------------------------------------------------#

=head2 read( $file )

Read an Inca suite file into the object.  The suite is
structured as follows:

<suite>
  <seriesConfig>
    <!-- see L<Inca::Config::Suite::SeriesConfig> for content here -->
  </reporterGroup>
  ...
</suite>

=over 2

B<Arguments>:

=over 13

=item xmlorFile

A string containing Inca suite xml or a path to an XML file containing an Inca 
suite

=back

B<Returns>:

Returns 1 if there are no errors reading the suite file; otherwise 
returns 0.

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub read {
  my ( $self, $xmlOrFileOrHash ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  my $suite;
  if ( ref($xmlOrFileOrHash) eq "HASH" ) {
    $suite = $xmlOrFileOrHash;
  } else {
    eval {
      $suite = XMLin( $xmlOrFileOrHash, %XML_OPTIONS );
    };
    if ( $@ ) {
      $self->{logger}->error( "Problem reading suite: $@" );
      return 0;
    }
    if ( (ref($xmlOrFileOrHash) eq "SCALAR" || ref($xmlOrFileOrHash) eq "") 
       && $xmlOrFileOrHash =~ /^inca\.run_now.\w{4}$/ && -f $xmlOrFileOrHash ) {
      unlink $xmlOrFileOrHash;
    }
  }
  my $success = 1;
  if ( ref($suite->{guid}) eq "HASH" ) {
    $self->{guid} = $suite->{guid}->{content};
  } else {
    $self->{guid} = $suite->{guid};
  }
  if ( exists $suite->{seriesConfigs} && 
       exists $suite->{seriesConfigs}->{seriesConfig} ) {
    my $i = 0;
    for my $scHash ( @{$suite->{seriesConfigs}->{seriesConfig}} ) {
      my $sc = new Inca::Config::Suite::SeriesConfig();
      if ( $sc->read($scHash, $self->{guid} ) ) {
        push( @{$self->{configs}}, $sc );
      } else {
        $success = 0;
        $self->{logger}->error( "Error reading config $i from suite" );
      }
      $i++;
    }
  }
  return $success;
}

#-----------------------------------------------------------------------------#

=head2 resolveReporters( $rc, $uri )

Look up the existing reporters listed in this suite and locate
their local copies.  Set the path attribute and resolve context.

=over 2

B<Arguments>:

=over 13

=item rc

A reference to a Inca::ReporterManager::ReporterCache object to look up
local reporter copies.

=item uri

A string containing the uri for the reporter instance manager to contact
to request proxy information in order to retrieve a proxy credential for
reporter execution.

=back

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub resolveReporters {
 my ( $self, $rc, $uri ) = validate_pos( 
   @_, $SELF_PARAM_REQ, $REPORTER_CACHE_PARAM_REQ, $URI_PARAM_OPT 
 );

  my @good_sc;
  for my $sc ( $self->getSeriesConfigs() ) {
    my $path = $rc->getPath( $sc->getName(), $sc->getVersion() );
    if ( ! defined $path ) { 
      $self->{logger}->error(
        "Unable to locate " . $sc->getName() . ", version=" .
        $sc->getVersion() .  " in local cache...skipping"
      );
      next;
    }
    my @depends = $rc->getDependencies( $sc->getName(), $sc->getVersion() );
    if ( grep(/Inca::Reporter::GridProxy/, @depends) ||
         grep(/inca\.GridProxyReporter/, @depends) ) {
      if ( defined $uri ) {
        $self->{logger}->debug( $sc->getName() . " requires a proxy" );
        $sc->setProxyContact( $uri );
      } else {
        $self->{logger}->debug( 
          $sc->getName() . " requires a proxy but no uri available" 
        );
      }
    }
    $sc->setPath( $path );
    $self->{logger}->info( $sc->getPath() );
    push( @good_sc, $sc );
  }
  $self->{configs} = [ @good_sc ];
}

#-----------------------------------------------------------------------------#

=head2 setGuid( $guid )

Set the guid of the suite.

=over 2

B<Arguments>:

=over 13

=item guid

A string indicating the guid of the suite:  inca[s]://host:port/suiteName

=back

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setGuid {
  my ( $self, $guid ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{guid} = $guid;
}

#-----------------------------------------------------------------------------#

=head2 write( $filename )

Write an suite object to file following the suite schema

B<Arguments>:

=over 13

=item filename

A string containing a path to an file to which the Inca suite XML
will be written to.

=back

=begin testing

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

=end testing

=cut
#-----------------------------------------------------------------------------#
sub write {
  my ( $self, $filename ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  # write to disk
  if ( open( my $fh, ">$filename" ) ) {
    my $xml = XMLout 
      ( $self->getXmlHashArray, AttrIndent => 1, KeyAttr => [], KeepRoot => 1, 
        NoAttr => 1, RootName => "suite" );
    # write namespace information -- no method in XML::Simple
    $xml =~ s/<suite>/<suite:suite $INCA_NAMESPACE>/;
    $xml =~ s/<\/suite>/<\/suite:suite>/; 
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

L<Inca::Config::Suite::SeriesConfig>

=cut
