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

my $Original_File = 'lib/Inca/Config/Suite/SeriesConfig.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 123 lib/Inca/Config/Suite/SeriesConfig.pm

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;
  lives_ok { new Inca::Config::Suite::SeriesConfig() } 'object created';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 213 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 261 lib/Inca/Config/Suite/SeriesConfig.pm

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



    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 376 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 429 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 466 lib/Inca/Config/Suite/SeriesConfig.pm

  use Inca::Config::Suite::SeriesConfig;
  use Test::Exception;
  my $sc = new Inca::Config::Suite::SeriesConfig(); 
  $sc->getArgumentsFromCmdLine( 
    '-verbose="1"', '-log="system"', '-help="no"', '-blah="hi there"'
  );
  is( $sc->getArgumentsAsCmdLine(),
      '-verbose="1" -log="system" -help="no" -blah="hi there"',
      'getArgumentsFromCmdLine works' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 691 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 739 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 877 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 994 lib/Inca/Config/Suite/SeriesConfig.pm

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasArguments(), 'hasArguments for false' );
  $sc->addArgument( "arg", "value" );
  ok( $sc->hasArguments(), 'hasArguments for true' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1028 lib/Inca/Config/Suite/SeriesConfig.pm

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasContext(), 'hasContext for false' );
  $sc->setContext( '@@; echo $?' );
  ok( $sc->hasContext(), 'hasContext for true' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1062 lib/Inca/Config/Suite/SeriesConfig.pm

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasLimits(), 'hasLimits for false' );
  my $limits = { "wallClockTime" => 30 };
  $sc->setLimits( $limits );
  ok( $sc->hasLimits(), 'hasLimits for true' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1096 lib/Inca/Config/Suite/SeriesConfig.pm

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasNiced(), 'hasNiced for false' );
  $sc->setNiced( 1 );
  ok( $sc->hasNiced(), 'hasNiced for true' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1130 lib/Inca/Config/Suite/SeriesConfig.pm

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasPath(), 'hasPath for false' );
  $sc->setPath( '/tmp/reporterA' );
  ok( $sc->hasPath(), 'hasPath for true' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1164 lib/Inca/Config/Suite/SeriesConfig.pm

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasProxyContact(), 'hasProxyContact for false' );
  $sc->setProxyContact( "incas://localhost:3234" );
  ok( $sc->hasProxyContact(), 'hasProxy for true' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1198 lib/Inca/Config/Suite/SeriesConfig.pm

  use Inca::Config::Suite::SeriesConfig;

  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasScheduler(), 'hasScheduler for false' );
  $sc->setSchedulerName( "sequential" );
  ok( $sc->hasScheduler(), 'hasScheduler for true' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1233 lib/Inca/Config/Suite/SeriesConfig.pm

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  ok( ! $sc->hasTargetResource(), 'hasTargetResource for false' );
  $sc->setTargetResource( "resourceB" );
  ok( $sc->hasTargetResource(), 'hasTargetResource for true' );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1273 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1474 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1522 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1568 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1615 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1675 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1722 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1767 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1811 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1868 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1915 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1960 lib/Inca/Config/Suite/SeriesConfig.pm

  # see getSchedulerArgs


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 1994 lib/Inca/Config/Suite/SeriesConfig.pm

  # see getSchedulerName


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 2031 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 2079 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 2122 lib/Inca/Config/Suite/SeriesConfig.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Config/Suite/SeriesConfig.pm

  use Inca::Config::Suite::SeriesConfig;
  my $sc = new Inca::Config::Suite::SeriesConfig();
  $sc->read( $parsedXML, $guid );
  my $name = $sc->getName();
  my $version  = $sc->getVersion();
  my $args = $sc->getArgumentsAsCmdLine();
  my $niced = $sc->getNiced();
  my $limits = $sc->getLimits();

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

