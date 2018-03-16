package Inca::Config::Suite::SeriesConfig;

################################################################################

=head1 NAME

Inca::Config::Suite::SeriesConfig - A series config as specified in an Inca suite 

=head1 SYNOPSIS

=for example begin

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  $sc->read( $parsedXML, $guid );
  my $name = $sc->getName();
  my $version  = $sc->getVersion();
  my $args = $sc->getArgumentsAsCmdLine();
  my $niced = $sc->getNiced();
  my $limits = $sc->getLimits();

=for example end

=head1 DESCRIPTION

A convenience module for working with series config from an Inca suite
object, Inca::Config::Suite.


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
use Inca::Process::Usage;
use Inca::Logger;
use Inca::Validate qw(:all);

# Perl standard
use Carp;
use Data::Dumper;

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ = { isa => __PACKAGE__ };
my $SET_PREFIX = "set";
my @RESOURCES = qw(wallClockTime cpuTime memory);
my $DEFAULT_SCHEDULER = "sequential";

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( %Options )

Class constructor which returns a new Inca::Config::Suite::SeriesConfig object.
The constructor may be called with any of the following attributes.

=over 2 

B<Options>:

=over 15

=item action

The action indicating whether the provided reporter should be added or
deleted

=item name

The string indicating the reporter name to execute 

=item version

The string indicating the reporter version to execute 

=item path

The string indicating the path to the reporter to execute 

=item storagePolicy

An object of type Inca::Config::Suite::StoragePolicy that gets passed
to the depot.

=item niced 

Lower the execution priority of the reporter by invoking the reporter with 
the Unix nice command [default: 0]

=item context 
    
Execution context string containing the reporter uri, context, and arguments.

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;
  lives_ok { new Inca::Config::Suite::SeriesConfig() } 'object created';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $self = {};
  
  bless ($self, $class);

  # set up defaults
  $self->{action} = undef;
  $self->{args} = [];
  $self->{context} = undef;
  $self->{guid} = undef;
  $self->{limits} = undef;
  $self->{logger} = Inca::Logger->get_logger( $class );
  $self->{niced} = undef;
  $self->{nickname} = undef;
  $self->{scheduler_args} = undef;
  $self->{scheduler_name} = undef;
  $self->{storage_policy} = undef;
  $self->{targetResource} = undef;
  $self->{uri} = undef;
  $self->{version} = undef;

  my %options = validate( @_, { action => $SCALAR_PARAM_OPT,
                                context => $SCALAR_PARAM_OPT,
                                guid => $SCALAR_PARAM_OPT,
                                name => $SCALAR_PARAM_OPT,
                                niced => $BOOLEAN_PARAM_OPT,
                                nickname => $SCALAR_PARAM_OPT,
                                path => $SCALAR_PARAM_OPT,
                                proxyContact => $URI_PARAM_OPT,
                                storagePolicy => $STORAGE_POLICY_PARAM_OPT,
                                targetResource => $SCALAR_PARAM_OPT,
                                uri => $SCALAR_PARAM_OPT,
                                version => $SCALAR_PARAM_OPT
                              } );

  $self->setAction( $options{action} ) if exists $options{action};
  $self->setContext( $options{context} ) if exists $options{context}; 
  $self->setGuid( $options{guid} ) if exists $options{guid};
  $self->setName( $options{name} ) if exists $options{name};
  $self->setNiced( $options{niced} ) if exists $options{niced}; 
  $self->setNickname( $options{nickname} ) if exists $options{nickname}; 
  $self->setPath( $options{path} ) if exists $options{path};
  $self->setProxyContact( $options{proxyContact} ) 
    if exists $options{proxyContact}; 
  $self->setStoragePolicy( $options{storagePolicy} ) 
    if exists $options{storagePolicy}; 
  $self->setTargetResource( $options{targetResource} ) 
    if exists $options{targetResource};
  $self->setUri( $options{uri} ) if exists $options{uri};
  $self->setVersion( $options{version} ) if exists $options{version};

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 addArgument( $argname, $argvalue );

Add a command-line argument to the reporter.  

=over 2

B<Arguments>:

=over 13

=item argname

The name of the argument.  E.g., 'verbose'.

=item argvalue

The value of the argument.  E.g., 3.

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;
  my $sc = new Inca::Config::Suite::SeriesConfig(); 
  lives_ok { $sc->addArgument( 'host', 'cuzco.sdsc.edu' ) } 
           'add argument succeeds';
  dies_ok { $sc->addArgument( '!h5', 'blah' ) } 'add with bad name';
  lives_ok { $sc->addArgument( 'help' ) } 'add with just name';
  $sc->addArgument( 'file', 'big.dat' );
  $sc->addArgument( 'compiler', 'gcc' );
  $sc->addArgument( 'host', 'machu.sdsc.edu' );
  $sc->addArgument( 'file', 'huge.dat' );
  $sc->addArgument( 'compiler', 'xlc' );
  $sc->addArgument( 'phrase', 'hi there' );
  $sc->addArgument( 'host', 'pichu.sdsc.edu' );
  $sc->addArgument( 'host-i', 'pichu.sdsc.edu' );
  my $cmdline = $sc->getArgumentsAsCmdLine();
  my $verify = '-host="cuzco.sdsc.edu" -help -file="big.dat" -compiler="gcc" -host="machu.sdsc.edu" -file="huge.dat" -compiler="xlc" -phrase="hi there" -host="pichu.sdsc.edu" -host-i="pichu.sdsc.edu"';
  is( $cmdline, $verify, "getArgsAsCmdline() succeeded" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub addArgument {
  my ($self, $argname, $argvalue) = 
     validate_pos( @_, $SELF_PARAM_REQ, $ALPHANUMERIC_PARAM_REQ, 
                       $SCALAR_PARAM_OPT );

  my $arg = { name => $argname, value => $argvalue};
  push( @{$self->{args}}, $arg );
}

#-----------------------------------------------------------------------------#

=head2 equals( $sc )

Return true if $sc is equivalent to ourself; false otherwise.

=over 2

B<Returns>:

A boolean indicating true if series configs are equal and false if they are not.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  my $sc1 = new Inca::Config::Suite::SeriesConfig();
  my $sc2 = new Inca::Config::Suite::SeriesConfig();
  $sc1->setPath( "/tmp/reporter1" );
  $sc2->setPath( "/tmp/reporter2" );
  ok( ! $sc1->equals($sc2), "not equals (path)" );
  $sc2->setPath( "/tmp/reporter1" );
  ok( $sc1->equals($sc2), "equals (path)" );
  $sc1->setName( "reporter1" );
  $sc2->setName( "sc2" );
  ok( ! $sc1->equals($sc2), "not equals (name)" );
  $sc2->setName( "reporter1" );
  ok( $sc1->equals($sc2), "equals (name)" );
  $sc1->setVersion( "1" );
  $sc2->setVersion( "2" );
  ok( ! $sc1->equals($sc2), "not equals (version)" );
  $sc2->setVersion( "1" );
  ok( $sc1->equals($sc2), "equals (version)" );
  $sc1->setContext( "http://inca.sdsc.edu/reporterA" );
  ok( ! $sc1->equals($sc2), "not equals (context)" );
  $sc2->setContext( "http://inca.sdsc.edu/reporterA" );
  ok( $sc1->equals($sc2), "equals (context)" );
  $sc2->setNiced( 1 );
  ok( ! $sc1->equals($sc2), "not equals (niced)" );
  $sc1->setNiced( 1 );
  ok( $sc1->equals($sc2), "equals (niced)" );
  my $limits = { 'wallClockTime' => '30',
                 'cpuTime' => '40',
                 'memory' => '50' };
  $sc1->setLimits( $limits );
  ok( ! $sc1->equals($sc2), "not equals (limits)" );
  $sc2->setLimits( { 'wallClockTime' => '30' } );
  ok( ! $sc1->equals($sc2), "not equals (limits2)" );
  $sc2->setLimits( $limits );
  ok( $sc1->equals($sc2), "equals (limits)" );
  $sc1->addArgument( 'file', 'big.dat' );
  ok( ! $sc1->equals($sc2), "not equals (args)" );
  $sc2->addArgument( 'compiler', 'gcc' );
  ok( ! $sc1->equals($sc2), "not equals (args2)" );
  $sc1->addArgument( 'host', 'machu.sdsc.edu' );
  $sc2->addArgument( 'file', 'huge.dat' );
  ok( ! $sc1->equals($sc2), "not equals (args3)" );
  $sc1->addArgument( 'phrase', 'hi there' );
  ok( ! $sc1->equals($sc2), "not equals (args4)" );
  $sc2->{args} = [];
  $sc2->addArgument( 'file', 'big.dat' );
  $sc2->addArgument( 'host', 'machu.sdsc.edu' );
  $sc2->addArgument( 'phrase', 'hi dude' );
  ok( ! $sc1->equals($sc2), "not equals (args5)" );
  $sc2->{args} = [];
  $sc2->addArgument( 'file', 'big.dat' );
  $sc2->addArgument( 'host', 'machu.sdsc.edu' );
  $sc2->addArgument( 'phrase', 'hi there' );
  ok( $sc1->equals($sc2), "equals (args)" );
  my $sp = new Inca::Config::Suite::StoragePolicy();
  $sp->addDepots( "cuzco.sdsc.edu:8234" );
  $sc1->setStoragePolicy( $sp );
  ok( ! $sc1->equals($sc2), "not equals (sp)" );
  $sc2->setStoragePolicy( $sp );
  ok( $sc1->equals($sc2), "equals (sp)" );


=end testing

=cut
#-----------------------------------------------------------------------------#
sub equals {
  my ( $self, $sc ) = validate_pos( @_, $SELF_PARAM_REQ, $SELF_PARAM_REQ );

  return 0 if $self->{name} ne $sc->{name};
  return 0 if $self->{version} ne $sc->{version};
  return 0 if $self->{path} ne $sc->{path};
  return 0 if $self->{context} ne $sc->{context};
  return 0 if ( ! defined $self->{niced} && defined $sc->{niced} );
  return 0 if ( defined $self->{niced} && ! defined $sc->{niced} );
  return 0 if $self->{niced} != $sc->{niced};
  return 0 if ( defined($self->{limits}) && (! defined($sc->{limits})) );
  return 0 if ( (! defined($self->{limits})) && defined($sc->{limits}) );
  if ( defined($self->{limits}) && defined($sc->{limits}) ) {
    return 0 if $self->{limits}->getCpuTime() != $sc->{limits}->getCpuTime();
    return 0 if $self->{limits}->getMemory() != $sc->{limits}->getMemory();
    return 0 if $self->{limits}->getWallClockTime() != $sc->{limits}->getWallClockTime();
  }
  return 0 if ( scalar(@{$self->{args}}) != scalar(@{$sc->{args}}) );
  for( my $i = 0; $i < scalar(@{$self->{args}}); $i++ ) {
    return 0 if ($self->{args}[$i]->{name} ne $sc->{args}[$i]->{name});
    return 0 if ($self->{args}[$i]->{value} ne 
                 $sc->{args}[$i]->{value});
  }
  return 0 if ( defined($self->{storage_policy}) && 
                (! defined($sc->{storage_policy})) );
  return 0 if ( (! defined($self->{storage_policy})) && 
                defined($sc->{storage_policy}) );
  if (defined($self->{storage_policy}) && defined($sc->{storage_policy})) {
    return 0 if (! $self->{storage_policy}->equals($sc->{storage_policy}));
  }
  return 1;
}

#-----------------------------------------------------------------------------#

=head2 getAction( )

Return the action of subscription request: add, delete.

=over 2

B<Returns>:

A string containing the action of subscription request.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $sc = new Inca::Config::Suite::SeriesConfig();
  is( $sc->getAction(), undef, 'returns group action for undef' );

  my $sc_desc = {
    'action' => 'add',
    'schedule' => undef,
    'series' => {
         'name' => 'cluster.compiler.gcc.version',
         'version' => '1',
         'context' => 'file:///usr/local/bin/cluster.compiler.gcc.version',
         'limits' => { 'wallClockTime' => '300' },
    }
  }; 
  is( $sc->read( $sc_desc, "incas://a.sdsc.edu/suiteA" ), 1, 
      "read good config without error" );
  is( $sc->getName(), "cluster.compiler.gcc.version", 
      "read good group and found name" );

  $sc->read( $sc_desc, "incas://a.sdsc.edu/suiteA" ); 
  is( $sc->getAction(), 'add', 'returns group action' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getAction {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{action};
}

#-----------------------------------------------------------------------------#

=head2 getArgumentsAsCmdLine( );

Return the arguments for the reporter as a command-line string.  E.g.,
-file=big.datg

=over 2

B<Returns>:

If called in a array context, will return the arguments as an array.  If
called in a scalar context, will return the arguments as a string that can be
input to a reporter on the command-line.  

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;
  my $sc = new Inca::Config::Suite::SeriesConfig(); 
  $sc->addArgument( 'file', 'big.dat' );
  my @args = $sc->getArgumentsAsCmdLine();
  ok( eq_array( \@args, [ '-file="big.dat"' ] ),
      'getArgumentsAsCmdLine returns array of one element' );
  $sc->addArgument( 'compiler', 'gcc' );
  @args = $sc->getArgumentsAsCmdLine();
  ok( eq_array( \@args, [ '-file="big.dat"', '-compiler="gcc"' ] ),
      'getArgumentsAsCmdLine returns array of 2 element' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getArgumentsAsCmdLine {
  my $self = shift;

  my @args; 
  for my $arg ( @{$self->{args}} ) {
    my $argset = "-" . $arg->{name};
    $argset .= '="' . $arg->{value} . '"' if defined $arg->{value};
    push( @args, $argset );
  }
  return wantarray ? @args : join( " ", @args );
}

#-----------------------------------------------------------------------------#

=head2 getArgumentsFromCmdLine( @ARGV )

Parse the command-line for arguments to the reporter.  E.g.,
-file="big.dat" --host=localhost

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;
  my $sc = new Inca::Config::Suite::SeriesConfig(); 
  $sc->getArgumentsFromCmdLine( 
    '-verbose="1"', '-log="system"', '-help="no"', '-blah="hi there"'
  );
  is( $sc->getArgumentsAsCmdLine(),
      '-verbose="1" -log="system" -help="no" -blah="hi there"',
      'getArgumentsFromCmdLine works' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getArgumentsFromCmdLine {
  my $self = shift;
  my @argv = @_;

  for my $argset ( @argv ) {
    $argset =~ s/-//;
    my ($name, $value) = split( /=/, $argset );
    $value =~ s/^"//;
    $value =~ s/"$//;
    $self->addArgument( $name, $value );
  }
}

#-----------------------------------------------------------------------------#

=head2 getContext( )

Retrieve the context string for the reporter execution.

=over 2

B<Returns>:

A string specifying the context string for the reporter execution.

=back

=cut
#-----------------------------------------------------------------------------#
sub getContext {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{context};
}

#-----------------------------------------------------------------------------#

=head2 getGuid( )

Retrieve the suite guid that the series originated from.

=over 2

B<Returns>:

The string indicating the suite guid.

=back

=cut
#-----------------------------------------------------------------------------#
sub getGuid {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{guid};
}

#-----------------------------------------------------------------------------#

=head2 getLimits( )

Get the maxiumum system usage or limits for the reporter.  

=over 2

B<Returns>:

An object of type Inca::Process::Usage which indicates the maximum
resource utilization this reporter can use.  

=back

=cut
#-----------------------------------------------------------------------------#
sub getLimits {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{limits};
}

#-----------------------------------------------------------------------------#

=head2 getName( )

Retrieve the name of the reporter.

=over 2

B<Returns>:

The string indicating the reporter name to execute 

=back

=cut
#-----------------------------------------------------------------------------#
sub getName {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{name};
}

#-----------------------------------------------------------------------------#

=head2 getNiced( )

Retrieve the nice status of the reporter (i.e., whether the reporter will
be executed at a lower priority using the Unix nice command).

=over 2

B<Returns>:

A boolean value where 1 indicates to run the process with nice while 0
indicates not to nice the process.

=back

=cut
#-----------------------------------------------------------------------------#
sub getNiced {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{niced};
}

#-----------------------------------------------------------------------------#

=head2 getNickname( )

Retrieve the nickname of the reporter.

=over 2

B<Returns>:

The string indicating the reporter nickname to execute 

=back

=cut
#-----------------------------------------------------------------------------#
sub getNickname {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{nickname};
}

#-----------------------------------------------------------------------------#

=head2 getPath( )

Retrieve the path of the reporter.

=over 2

B<Returns>:

The string indicating the path to the reporter to execute 

=back

=cut
#-----------------------------------------------------------------------------#
sub getPath {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{path};
}

#-----------------------------------------------------------------------------#

=head2 getProxyContact( )

Retrieve the value of the server to contact to retrieve proxy credential
information.

=over 2

B<Returns>:

A string containing the uri of the server to contact to retrieve proxy
credential information.

=back

=cut
#-----------------------------------------------------------------------------#
sub getProxyContact {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{proxy};
}

#-----------------------------------------------------------------------------#

=head2 getSchedulerArgs( )

Return the configuration information for the group scheduler. 

=over 2

B<Returns>:

A reference to a hash containing the configuration information for the group
scheduler or undef (if there is none).

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $sc = new Inca::Config::Suite::SeriesConfig();
  is( $sc->getSchedulerArgs(), undef, 
      "undefined scheduler args from constructor" );
  $sc->setSchedulerArgs( { repeat => '60m' } );
  ok( eq_hash( $sc->getSchedulerArgs(), { repeat => '60m' }),
      'set/getSchedulerArgs works' );
  my $sc_desc = { 
    'action' => 'add', 
    'schedule' => { 'dumbcron' => { repeat => '60s' } },
    'series' => {
      'name' => "name",
      'version' => 'version',
      'context' => 'context'
    }
  };
  $sc->read( $sc_desc, "incas://a.sdsc.edu/suiteB" );
  ok( eq_hash( $sc->getSchedulerArgs(), { repeat => '60s' }),
      'scheduler args set from read' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getSchedulerArgs {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );
  
  return $self->{scheduler_args};
}

#-----------------------------------------------------------------------------#

=head2 getSchedulerName( )

Return the name of the scheduler class for this group.

=over 2

B<Returns>:

A string containing the scheduler identifier to use for this reporter group.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $sc_desc = { 
    'action' => 'add', 
    'schedule' => undef,
    'series' => {
      'name' => 'name',
      'version' => 'version',
      'context' => 'context'
    }
   };
  my $sc = new Inca::Config::Suite::SeriesConfig();
  is( $sc->getSchedulerName(), undef, 
      "undefined scheduler name from constructor" );
  $sc->setSchedulerName( "bogus" );
  is( $sc->getSchedulerName(), "bogus", "set/getSchedulerName worked" );
  $sc->read( $sc_desc, "incas://a.sdsc.edu/suiteA" );
  is( $sc->getSchedulerName(), 'sequential', "scheduler set from read" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getSchedulerName {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );
  
  return $self->{scheduler_name};
}

#-----------------------------------------------------------------------------#

=head2 getStoragePolicy( )

Retrieve the reporter storage policy.

=over 2

B<Returns>:

An object of type Inca::Config::Suite::StoragePolicy containing the
storage policy that will be passed to the depot along with the report.

=back

=cut
#-----------------------------------------------------------------------------#
sub getStoragePolicy {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{storage_policy};
}

#-----------------------------------------------------------------------------#

=head2 getTargetResource( )

Retrieve the target resource of the series if it exists

=over 2

B<Returns>:

The string indicating the target resource of the series or undef if there
is not one

=back

=cut
#-----------------------------------------------------------------------------#
sub getTargetResource {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{targetResource};
}

#-----------------------------------------------------------------------------#

=head2 getUri( )

Retrieve the uri of the reporter.

=over 2

B<Returns>:

The string indicating the uri of the reporter to execute 

=back

=cut
#-----------------------------------------------------------------------------#
sub getUri {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{uri};
}

#-----------------------------------------------------------------------------#

=head2 getVersion( )

Retrieve the version of the reporter.

=over 2

B<Returns>:

The string indicating the reporter version to execute 

=back

=cut
#-----------------------------------------------------------------------------#
sub getVersion {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return $self->{version};
}

#-----------------------------------------------------------------------------#

=head2 getXmlHashArray( )

Return a hash array representation of the series config object that is 
appropriate for incorporating into a larger hash array that will be formatted
into XML by XML::Simple.

=over 2

B<Returns>:

A hash array representation of the series config object.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig( 
    name => "reporterA",
    version => 5,
    context => "reporterA",
    action => 'add'
  );
  my $scXml = {
    series => {
      name => "reporterA",
      version => 5,
      context => "reporterA",
      args => { arg => [] }
    },
    schedule => {}, 
    action => 'add',
    'resourceSetName' => 'localhost',

  };
  ok(eq_hash($sc->getXmlHashArray(), $scXml),'bare bones series config converts');

  $sc->setNiced( 1 );
  $scXml->{series}->{nice} = 1;
  ok(eq_hash($sc->getXmlHashArray(), $scXml),'nice series config converts');

  my $limits = { 'wallClockTime' => '30',
                 'cpuTime' => '40',
                 'memory' => '50' };
  $sc->setLimits( $limits );
  $scXml->{series}->{limits} = $limits;
  ok(eq_hash($sc->getXmlHashArray(), $scXml),'limits series config converts');

  $sc->addArgument( 'compiler', 'gcc' );
  $sc->addArgument( 'host', 'machu.sdsc.edu' );
  $sc->addArgument( 'file', 'huge.dat' );
  $scXml->{series}->{args} = { arg => {} };
  $scXml->{series}->{args}->{arg} = [ { name => 'compiler', value => 'gcc' },
                               { name => 'host', value => 'machu.sdsc.edu' },
                               { name => 'file', value => 'huge.dat' } ];
  ok(eq_hash($sc->getXmlHashArray(), $scXml),'args series config converts');
 
  my $uri = "http://blahblah" ;
  $sc->setUri( $uri );
  $scXml->{series}->{uri} = $uri;
  ok(eq_hash($sc->getXmlHashArray(), $scXml),'uri series config converts');

  my $nickname = "golden_series";
  $sc->setNickname( $nickname );
  $scXml->{nickname} = $nickname;
  ok(eq_hash($sc->getXmlHashArray(), $scXml),'nickname series config converts');

=end testing

=cut
#-----------------------------------------------------------------------------#
sub getXmlHashArray {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  # required parts of series config
  my $scXml = {
    series => {
      name => $self->{name},
      version => $self->{version},
      context => $self->{context},
    },
    resourceSetName => 'localhost',
    action => $self->{action},
    schedule => {}
  };

  # optional parts of series config
  $scXml->{series}->{nice} = $self->getNiced() if $self->hasNiced();
  if ( defined $self->{limits} && defined $self->{limits} ) {
    $scXml->{series}->{limits} = {};
    if ( $self->{limits}->hasWallClockTime() ) {
      $scXml->{series}->{limits}->{wallClockTime} =
        $self->{limits}->getWallClockTime();
    }
    $scXml->{series}->{limits}->{cpuTime} = $self->{limits}->getCpuTime()
      if $self->{limits}->hasCpuTime();
    $scXml->{series}->{limits}->{memory} = $self->{limits}->getMemory()
      if $self->{limits}->hasMemory();
  }
  if ( exists $self->{scheduler_name} && defined $self->{scheduler_name} ) {
    $scXml->{schedule}->{$self->{scheduler_name}} = {};
  }
  if ( defined $self->{scheduler_args} ) {
    $scXml->{schedule}->{$self->{scheduler_name}} = 
      { %{$self->{scheduler_args}} };
  }
  $scXml->{series}->{args} = { arg => $self->{args} } if defined $self->{args};
  $scXml->{series}->{uri} = $self->{uri} if defined $self->{uri};
  $scXml->{proxy} = $self->getProxyContact() if $self->hasProxyContact();
  $scXml->{targetHostname} = $self->getTargetResource() if $self->hasTargetResource();
  $scXml->{nickname} = $self->{nickname} if defined $self->{nickname};

  return $scXml;
}

#-----------------------------------------------------------------------------#

=head2 hasArguments( )

Return true if reporter arguments have been specified; otherwise return
false.

=over 2

B<Returns>:

Returns true if reporter arguments have been specified and false if they have
not.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasArguments(), 'hasArguments for false' );
  $sc->addArgument( "arg", "value" );
  ok( $sc->hasArguments(), 'hasArguments for true' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasArguments {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return scalar(@{$self->{args}}) > 0; 
}

#-----------------------------------------------------------------------------#

=head2 hasContext( )

Return true if a context string has been specified for the reporter; otherwise
return false.

=over 2

B<Returns>:

A boolean indicating true if a context string has been specified and false if
it has not.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasContext(), 'hasContext for false' );
  $sc->setContext( '@@; echo $?' );
  ok( $sc->hasContext(), 'hasContext for true' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasContext {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return ( exists $self->{context} and defined $self->{context} ); 
}

#-----------------------------------------------------------------------------#

=head2 hasLimits( )

Return true if a reporter limits has been specified; otherwise return
false.

=over 2

B<Returns>:

A boolean indicating true if a reporter limits has been specified and
false if it has not.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasLimits(), 'hasLimits for false' );
  my $limits = { "wallClockTime" => 30 };
  $sc->setLimits( $limits );
  ok( $sc->hasLimits(), 'hasLimits for true' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasLimits {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return ( exists $self->{limits} and defined $self->{limits} ); 
} 

#-----------------------------------------------------------------------------#

=head2 hasNiced( )

Return true if running the reporter with nice has been specified;
otherwise return false.

=over 2

B<Returns>:

A boolean indicating true if niced has been specified and false if it has not.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasNiced(), 'hasNiced for false' );
  $sc->setNiced( 1 );
  ok( $sc->hasNiced(), 'hasNiced for true' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasNiced {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return ( exists $self->{niced} && defined $self->{niced} ); 
}

#-----------------------------------------------------------------------------#

=head2 hasPath( )

Return true if a path has been specified for the reporter; otherwise
return false.

=over 2

B<Returns>:

A boolean indicating true if a path has been specified and false if
it has not.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasPath(), 'hasPath for false' );
  $sc->setPath( '/tmp/reporterA' );
  ok( $sc->hasPath(), 'hasPath for true' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasPath {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return ( exists $self->{path} and defined $self->{path} ); 
}

#-----------------------------------------------------------------------------#

=head2 hasProxyContact( )

Return true if a proxy contact has been specified for the reporter; otherwise
return false.

=over 2

B<Returns>:

A boolean value indicating true if this reporter has a proxy contact and 
false otherwise.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasProxyContact(), 'hasProxyContact for false' );
  $sc->setProxyContact( "incas://localhost:3234" );
  ok( $sc->hasProxyContact(), 'hasProxy for true' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasProxyContact {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return exists $self->{proxy} && defined $self->{proxy}; 
}

#-----------------------------------------------------------------------------#

=head2 hasScheduler( )

Return true if a group scheduler has been specified; otherwise return
false.

=over 2

B<Returns>:

A boolean indicating true if a group scheduler has been specified and
false if it has not.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;

  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasScheduler(), 'hasScheduler for false' );
  $sc->setSchedulerName( "sequential" );
  ok( $sc->hasScheduler(), 'hasScheduler for true' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasScheduler {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );
  
  return ( exists $self->{scheduler_name} and defined $self->{scheduler_name} );
}

#-----------------------------------------------------------------------------#

=head2 hasTargetResource( )

Return true if a target resource has been specified for the series otherwise
return false.

=over 2

B<Returns>:

A boolean value indicating true if this series has a target resource and 
false otherwise.

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasTargetResource(), 'hasTargetResource for false' );
  $sc->setTargetResource( "resourceB" );
  ok( $sc->hasTargetResource(), 'hasTargetResource for true' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub hasTargetResource {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  return exists $self->{targetResource} && defined $self->{targetResource}; 
}

#-----------------------------------------------------------------------------#

=head2 read( $parsedXML )

Read in data for this reporter from the hashref which contains XML::Simple
parsed XML.

=over 2
  
B<Arguments>:

=over 13

=item parsedXML
  
A reference to a hash generated by XML::Simple containing information about
the series config.  

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;
  use Data::Dumper;

  Inca::Logger->screen_init( "ERROR" );

  my $sc = new Inca::Config::Suite::SeriesConfig();

  # good one
  my $scHash = {
    'resourceSetName' => 'localhost',
    'targetHostname' => 'resourceA',
    'action' => 'add',
    'schedule' => undef,
    'series' => {
      'limits' => {
        'wallClockTime' => 300 
      },  
      'version' => '1',
      'context' => 'PATH=./bin:${PATH}; cluster.compiler.gcc.version -verbose="0" -log=""; echo $?',
      'args' => { arg => [ { name => 'verbose', value => 0 },
                           { name => 'log', value => undef } ] },
      'name' => 'cluster.compiler.gcc.version',
      'niced' => 0,
    }
  };
  is( $sc->read($scHash, "incas://a.sdsc.edu/suiteA"), 1, 
      "read good config without error" );
  is( $sc->getName(), 'cluster.compiler.gcc.version', 'name set from read' );
  is( $sc->getVersion(), '1', 'version set from read' );
  is( $sc->getAction(), 'add', 'action set from read' );
  is( $sc->getTargetResource(), 'resourceA', 'target resource read' );
  is($sc->getArgumentsAsCmdLine(), '-verbose="0" -log=""','args set from read');
  is( $sc->getNiced(), 0, 'niced set from read');
  isa_ok( $sc->getLimits(), "Inca::Process::Usage", 
          'limits set from read');
  is( $sc->getContext(), 'PATH=./bin:${PATH}; cluster.compiler.gcc.version -verbose="0" -log=""; echo $?', 'context set from read' );

  # bad one -- no name
  my $sc_noname = eval Dumper $scHash;
  $sc_noname->{series}->{name} = undef;
  is( $sc->read($sc_noname, "incas://a.sdsc.edu/suiteA" ), 0, 
      "read failed with no name" );
  ok( $_STDERR_ =~ /no name located/, "no name error printed" );

  # bad one -- no version
  my $sc_noversion = eval Dumper $scHash;
  $sc_noversion->{series}->{version} = undef;
  is( $sc->read($sc_noversion, "incas://a.sdsc.edu/suiteA"), 0, 
      "read failed with no version" );
  ok( $_STDERR_ =~ /no version located/, "no version error printed" );

  # bad one -- no action
  my $sc_noaction = eval Dumper $scHash;
  $sc_noaction->{action} = undef;
  is( $sc->read($sc_noaction, "incas://a.sdsc.edu/suiteA"), 0, 
      "read failed with no action" );
  ok( $_STDERR_ =~ /no action located/, "no action error printed" );

  # bad one -- no scheduler
  my $sc_nosched = eval Dumper $scHash;
  delete $sc_nosched->{schedule}; 
  is( $sc->read($sc_nosched, "incas://a.sdsc.edu/suiteA"), 0, 
      "read failed with no scheduler" );
  ok( $_STDERR_ =~ /No schedule found/, "no schedule error printed" );

  # bad one -- more than one scheduler
  my $sc_mulsched = eval Dumper $scHash;
  $sc_mulsched->{schedule} = {
       'sequential' => undef,
       'cron' => undef };
  is( $sc->read($sc_mulsched, "incas://a.sdsc.edu/suiteA"), 0, 
      "read failed with multiple scheduler" );
  ok( $_STDERR_ =~ /multiple schedules/, "multiple schedule error printed" );

  # bad one -- bad sched obj
  my $sc_badsched = eval Dumper $scHash;
  $sc_badsched->{schedule} = 5;
  is( $sc->read($sc_badsched, "incas://a.sdsc.edu/suiteA"), 0, 
      "read failed with bad scheduler" );
  ok( $_STDERR_ =~ /hash not found/, "bad schedule error printed" );

  # bad one -- no context
  my $sc_nocontext =  eval Dumper $scHash;
  $sc_nocontext->{series}->{context} = undef;
  is( $sc->read($sc_nocontext, "incas://a.sdsc.edu/suiteA"), 0, 
      "read failed with no context" );
  ok( $_STDERR_ =~ /no context located/, "no context error printed" );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub read {
  my ($self, $sc, $guid) = validate_pos( @_, $SELF_PARAM_REQ, HASHREF, SCALAR );

  if ( ! exists $sc->{series} || ! defined $sc->{series} ) {
    $self->{logger}->error( "Error reading series config, no series located:" . 
                            Dumper $sc );
    return 0;
  }
  if ( ! exists $sc->{series}->{name} || ! defined $sc->{series}->{name} ) {
    $self->{logger}->error( "Error reading series config, no name located:" . 
                            Dumper $sc );
    return 0;
  }
  if ( !exists $sc->{series}->{version} || !defined $sc->{series}->{version}) {
    $self->{logger}->error( "Error reading series config, no version located:" .
                            Dumper $sc );
    return 0;
  }
  if ( ! exists $sc->{action} || ! defined $sc->{action} ) {
    $self->{logger}->error( "Error reading series config, no action located:" .
                            Dumper $sc );
    return 0;
  }

  if ( ! exists $sc->{schedule} ) {
    $self->{logger}->error( "No schedule found in series config" );
    return 0;
  }

  if ( ! exists $sc->{series}->{context} || ! defined $sc->{series}->{context}){
    $self->{logger}->error( "Error reading series config, no context located:" .
                            Dumper $sc );
    return 0;
  }

  $self->setName( $sc->{series}->{name} );
  $self->setVersion( $sc->{series}->{version} );
  $self->setAction( $sc->{action} ); 
  $self->setContext( $sc->{series}->{context} ); 
  $self->setNiced( $sc->{series}->{niced} ) if exists $sc->{series}->{niced};
  if ( ! defined $sc->{schedule} ) {
    $self->setSchedulerName( $DEFAULT_SCHEDULER );
  } elsif ( ref($sc->{schedule}) eq "HASH" ) {
    # check that there is just one schedule
    my @keys = keys %{$sc->{schedule}};
    if ( scalar(@keys) > 1 ) {
      $self->{logger}->error(
        "Error reading schedule in series config...multiple schedules"
      );
      return 0;
    }
    $self->setSchedulerName( $keys[0] );
    if ( defined $sc->{schedule}->{$keys[0]} ) {
      $self->setSchedulerArgs( $sc->{schedule}->{$keys[0]} );
    }
  } else {
    $self->{logger}->error(
      "Parse error reading schedule in series config...hash not found"
    );
    return 0;
  }
  if ( exists $sc->{series}->{limits} ) {
    $self->setLimits( $sc->{series}->{limits} );
  }
  $self->setNickname( $sc->{nickname} ) if exists $sc->{nickname};
  if ( exists $sc->{series}->{args} and exists $sc->{series}->{args}->{arg} ) {
    for my $arg ( @{$sc->{series}->{args}->{arg}} ) {
      if ( exists $arg->{value} && defined $arg->{value} ) {
        $self->addArgument( $arg->{name}, $arg->{value} );
      } elsif ( ! defined $arg->{value} ) {
        $self->addArgument( $arg->{name}, "" );
      } else {
        $self->addArgument( $arg->{name} );
      }
    }
  }
  $self->setProxyContact( $sc->{proxy} ) 
    if exists $sc->{proxy} && defined $sc->{proxy};
  $self->setTargetResource( $sc->{targetHostname} ) 
    if exists $sc->{targetHostname} && defined $sc->{targetHostname};

  $self->setGuid( $guid ); 

  return 1;
}

#-----------------------------------------------------------------------------#

=head2 setAction( $action )

Set the action of the series config.

=over 2

B<Arguments>:

=over 13

=item action

A string indicating an action of the specified series config (add or delete)

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $action1 = "add";
  my $action2 = "delete";
  my $sc = new Inca::Config::Suite::SeriesConfig();
  is ( $sc->getAction(), undef, 'get action was undef from constructor' );
  $sc = new Inca::Config::Suite::SeriesConfig( action => $action1 ); 
  is ( $sc->getAction(), $action1, 'set action worked from constructor' );
  $sc->setAction( $action2 );
  is ( $sc->getAction(), $action2, 'set/getAction worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setAction {
  my ( $self, $action ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{action} = $action;
}

#-----------------------------------------------------------------------------#

=head2 setContext( $string )

Adds an execution context string specifying the reporter uri, context, and
arguments to execute.

=over 2

B<Arguments>:

=over 13

=item string

A string that specifies the execution context string.  It specifies the
reporter uri (which will be replaced by the local path to the reporter on
execution), any context (e.g., setting of environ vars), and reporter
arguments.

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $sc = new Inca::Config::Suite::SeriesConfig();
  is( $sc->getContext(), undef, 'context undef after constructor' );
  $sc = new Inca::Config::Suite::SeriesConfig( 
    context => 'http://inca.sdsc.edu/reporterA' 
  );
  is( $sc->getContext(), 'http://inca.sdsc.edu/reporterA', 
      'context set after constructor' );
  $sc->setContext( 'http://inca.sdsc.edu/reporterB' );
  is( $sc->getContext(), 'http://inca.sdsc.edu/reporterB', 
      'set/getContext works' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setContext {
  my ( $self, $context ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{context} = $context;
}

#-----------------------------------------------------------------------------#

=head2 setGuid( $guid )

Set the suite guid that the series belongs to.  

=over 2

B<Arguments>:

=over 13

=item guid 

The string indicating the suite guid the series came from.

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $guid1 = "guidA";
  my $guid2 = "guidB";
  my $sc = new Inca::Config::Suite::SeriesConfig();
  is ($sc->getGuid(), undef, 'get guid was undef from constructor');
  $sc = new Inca::Config::Suite::SeriesConfig( guid => $guid1 ); 
  is ( $sc->getGuid(), $guid1, 'set guid worked from constructor' );
  $sc->setGuid( $guid2 );
  is ( $sc->getGuid(), $guid2 , 'set/getGuid worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setGuid {
  my ( $self, $guid ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{guid} = $guid;
}

#-----------------------------------------------------------------------------#

=head2 setLimits( $limits )

Set the maxiumum system usage or limits for the reporter.  The limits is
based on one or more of the following resources:  CPU time, wall clock time,
memory.  

=over 2

B<Arguments>:

=over 13

=item limits

A reference to an array of hashrefs.  Each hashref contains a name/value
pair.  Valid names include wallClockTime, CpuTime, memory.

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Inca::Process::Usage;
  use Test::Exception;

  my $sc = new Inca::Config::Suite::SeriesConfig();
  is( $sc->getLimits(), undef, 'limits undefined after constructor' );

  my $limits = { 'wallClockTime' => '30',
                 'cpuTime' => '40',
                 'memory' => '50' };
  $sc->setLimits( $limits );
  isa_ok( $sc->getLimits(), "Inca::Process::Usage", 
          'set/getLimits worked' );
  is( $sc->getLimits->getWallClockTime(), 30, 'wall clock read correctly' );
  is( $sc->getLimits->getCpuTime(), 40, 'cpu time read correctly' );
  is( $sc->getLimits->getMemory(), 50, 'memory read correctly' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setLimits {
  my ( $self, $limits ) = validate_pos( @_, $SELF_PARAM_REQ, HASHREF );

  $self->{limits} = new Inca::Process::Usage();
  for my $resource ( keys %{$limits} ) {
    if ( ! grep( /$resource/, @RESOURCES ) ) {
      $self->{logger}->error( 
        "Invalid resource " . $resource .  ' specified in limits' 
      );
      return 0;
    }
    my $function_name = $SET_PREFIX . ucfirst( $resource );
    $self->{limits}->$function_name( $limits->{$resource} );
  }
  return 1;
}

#-----------------------------------------------------------------------------#

=head2 setName( $name )

Set the name of the reporter to execute.

=over 2

B<Arguments>:

=over 13

=item name 

The string indicating the reporter name to execute 

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $name1 = "reporter1";
  my $name2 = "reporter2";
  my $sc = new Inca::Config::Suite::SeriesConfig();
  is ( $sc->getName(), undef, 'get name was undef from constructor' );
  $sc = new Inca::Config::Suite::SeriesConfig( name => $name1 ); 
  is ( $sc->getName(), $name1, 'set name worked from constructor' );
  $sc->setName( $name2 );
  is ( $sc->getName(), $name2, 'set/getName worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setName {
  my ( $self, $name) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{name} = $name;
}

#-----------------------------------------------------------------------------#

=head2 setNiced( $is_niced )

Lowers the execution priority of the reporter.  When the reporter is run, it 
will be executed with the nice command using the system's default altered
scheduling priority.

=over 2

B<Arguments>:

=over 13

=item is_niced

A boolean value where 1 indicates to run the process with nice while 0
indicates not to nice the process.

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $sc = new Inca::Config::Suite::SeriesConfig();
  is( $sc->getNiced(), undef, 'niced undef after constructor' );
  $sc = new Inca::Config::Suite::SeriesConfig( niced => 0 );
  is( $sc->getNiced(), 0, 'Set niced works from constructor' );
  $sc->setNiced( 1 );
  is( $sc->getNiced(), 1, 'Set/getNiced works' );
  dies_ok { $sc->setNiced(-10) } 
          'Set niced dies when non-boolean specified as arg';

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setNiced {
  my ( $self, $niced ) =
     validate_pos( @_, $SELF_PARAM_REQ, $BOOLEAN_PARAM_REQ );

  $self->{niced} = $niced;
}

#-----------------------------------------------------------------------------#

=head2 setNickname( $nickname )

Set the nickname of the reporter to execute.

=over 2

B<Arguments>:

=over 13

=item nickname 

The string indicating the nickname of the reporter that will execute 

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $nickname1 = "nick1";
  my $nickname2 = "nick2";
  my $sc = new Inca::Config::Suite::SeriesConfig();
  is ( $sc->getNickname(), undef, 'get nickname was undef from constructor' );
  $sc = new Inca::Config::Suite::SeriesConfig( nickname => $nickname1 ); 
  is ( $sc->getNickname(), $nickname1, 'set nickname worked from constructor' );
  $sc->setNickname( $nickname2 );
  is ( $sc->getNickname(), $nickname2, 'set/getNickname worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setNickname {
  my ( $self, $nickname) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{nickname} = $nickname;
}

#-----------------------------------------------------------------------------#

=head2 setPath( $path )

Set the path of the reporter to execute.

=over 2

B<Arguments>:

=over 13

=item name 

The string indicating the path of the reporter to execute 

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $path1 = "/tmp/reporter1";
  my $path2 = "/tmp/reporter2";
  my $sc = new Inca::Config::Suite::SeriesConfig();
  is ( $sc->getPath(), undef, 'get path was undef from constructor' );
  $sc = new Inca::Config::Suite::SeriesConfig( path => $path1 ); 
  is ( $sc->getPath(), $path1, 'set path worked from constructor' );
  $sc->setPath( $path2 );
  is ( $sc->getPath(), $path2, 'set/getPath worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setPath {
  my ( $self, $path) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{path} = $path;
}

#-----------------------------------------------------------------------------#

=head2 setProxyContact( $uri )

Set the value of the server to contact to retrieve proxy credential
information.

=over 2

B<Arguments>:

=over 13

=item uri

A string in the format of <scheme>://<path> where

=over 13

=item scheme

The type of URI being represented by the string (either I<file> or I<incas>)

=item path

The location of the URI being represented (e.g., localhost:7070)

=back

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $uri1 = "incas://localhost:3234";
  my $uri2 = "incas://localhost:3235";
  my $sc = new Inca::Config::Suite::SeriesConfig();
  is ( $sc->getProxyContact(), undef, 
       'get proxy contact was undef from constructor' );
  $sc= new Inca::Config::Suite::SeriesConfig( proxyContact => $uri1 ); 
  is( $sc->getProxyContact(), $uri1, 'set proxy worked from constructor' );
  $sc->setProxyContact( $uri2 );
  is( $sc->getProxyContact(), $uri2, 'set/getProxyContact worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setProxyContact {
  my ($self, $uri ) = validate_pos(@_, $SELF_PARAM_REQ, $URI_PARAM_REQ);

  $self->{proxy} = $uri;
}

#-----------------------------------------------------------------------------#

=head2 setTargetResource( $resource )

Set the value of the target resource for this series.

=over 2

B<Arguments>:

=over 13

=item resource

A string containing the name of the target resource

=back

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $resource1 = "resourceA";
  my $resource2 = "resourceB";
  my $sc = new Inca::Config::Suite::SeriesConfig();
  is ( $sc->getTargetResource(), undef, 
       'get target resource was undef from constructor' );
  $sc= new Inca::Config::Suite::SeriesConfig( targetResource => $resource1 ); 
  is( $sc->getTargetResource(), $resource1, 'set target resource worked from constructor' );
  $sc->setTargetResource( $resource2 );
  is( $sc->getTargetResource(), $resource2, 'set/getTargetResource worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setTargetResource {
  my ($self, $resource ) = validate_pos(@_, $SELF_PARAM_REQ, SCALAR );

  $self->{targetResource} = $resource;
}

#-----------------------------------------------------------------------------#

=head2 setSchedulerArgs( $args )

Set the configuration information for the group scheduler. 

=over 2

B<Arguments>:

=over 8

=item args

A reference to a hash array containing scheduler information.

=back

=back

=begin testing

  # see getSchedulerArgs

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setSchedulerArgs {
  my ( $self, $args ) = validate_pos( @_, $SELF_PARAM_REQ, HASHREF );
  
  $self->{scheduler_args} = $args;
}

#-----------------------------------------------------------------------------#

=head2 setSchedulerName( $name )

Set the name of the scheduler class for this group.

=over 2

B<Arguments>:

=over 8

=item name

A string containing the name of the scheduler for this reporter group.

=back

=back

=begin testing

  # see getSchedulerName

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setSchedulerName {
  my ( $self, $name ) =
     validate_pos( @_, $SELF_PARAM_REQ, $ALPHANUMERIC_PARAM_REQ );

  $self->{scheduler_name} = $name;
}

#-----------------------------------------------------------------------------#

=head2 setStoragePolicy( $storage_policy )

Set the storage policy for the reporter. 

=over 2

B<Arguments>:

=over 13

=item storage_policy

An object of type Inca::Config::Suite::StoragePolicy that will be
used once a reporter has completed execution and will be passed along with the
report to the depot. 

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Inca::Config::Suite::StoragePolicy;
  use Test::Exception;

  my $sp = new Inca::Config::Suite::StoragePolicy();
  my $sc = new Inca::Config::Suite::SeriesConfig( storagePolicy => $sp );
  isa_ok( $sc->getStoragePolicy(), 
          "Inca::Config::Suite::StoragePolicy",
          'set storage policy worked from constructor' );
  my $sp2 = new Inca::Config::Suite::StoragePolicy();
  $sp2->addDepots( "ssl:localhost:6323" );
  $sc->setStoragePolicy( $sp2 );
  is( $sc->getStoragePolicy()->{depots}[0], "ssl:localhost:6323", 
      'set/get storage policy works' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setStoragePolicy {
  my ( $self, $storage_policy ) =
     validate_pos( @_, $SELF_PARAM_REQ, $STORAGE_POLICY_PARAM_REQ );

  $self->{storage_policy} = $storage_policy;
}

#-----------------------------------------------------------------------------#

=head2 setUri( $uri )

Set the uri of the reporter to execute.

=over 2

B<Arguments>:

=over 13

=item uri 

The string indicating the uri of the reporter to execute 

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $uri1 = "http://inca.sdsc.edu/reporters/rep1";
  my $uri2 = "http://inca.sdsc.edu/reporters/rep1";
  my $sc = new Inca::Config::Suite::SeriesConfig();
  is ($sc->getUri(), undef, 'get uri was undef from constructor');
  $sc = new Inca::Config::Suite::SeriesConfig( uri => $uri1 ); 
  is ( $sc->getUri(), $uri1, 'set uri worked from constructor' );
  $sc->setUri( $uri2 );
  is ( $sc->getUri(), $uri2 , 'set/getUri worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setUri {
  my ( $self, $uri ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{uri} = $uri;
}
#-----------------------------------------------------------------------------#

=head2 setVersion( $version )

Set the version of the reporter to execute.

=over 2

B<Arguments>:

=over 13

=item version  

The string indicating the reporter version to execute 

=back

=back

=begin testing

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;

  my $version1 = "1";
  my $version2 = "2";
  my $sc = new Inca::Config::Suite::SeriesConfig();
  is ($sc->getVersion(), undef, 'get version was undef from constructor');
  $sc = new Inca::Config::Suite::SeriesConfig( version => $version1 ); 
  is ( $sc->getVersion(), $version1, 'set version worked from constructor' );
  $sc->setVersion( $version2 );
  is ( $sc->getVersion(), $version2 , 'set/getVersion worked' );

=end testing

=cut
#-----------------------------------------------------------------------------#
sub setVersion {
  my ( $self, $version) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  $self->{version} = $version;
}

1; # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

Does not recognize storage policies yet.

=head1 SEE ALSO

L<Inca::Config::Suite>

=cut
