package Inca::AgentClient;
use base qw(Inca::Net::Client);
@ISA = ( "Inca::Net::Client" );

################################################################################

=head1 NAME

Inca::AgentClient - Handles communication with Inca Agent.

=head1 SYNOPSIS

=for example begin

  use Inca::AgentClient;
  use Inca::ReporterManager;

  my $rm = new Inca::ReporterManager();
  # configure $rm
  my $client = new Inca::AgentClient( $rm, "inca://localhost:8090", %creds );
  my $status = $client->register();
  # will not return until Agent closes connection or fatal error

=for example end

=head1 DESCRIPTION

Handles communication between the Inca reporter manager and the Inca agent.
Specifically this module provides the ability to connect to an Inca agent and
register the provided reporter manager via the register method.  Then it
waits for commands from the agent (e.g., execute suite, install reporter
packages, etc.) and relays the agent's requests to the passed
Inca::ReporterManager object.

=cut
################################################################################

#=============================================================================#
# Usage
#=============================================================================#

# pragmas
use strict;
use warnings;
use vars qw($VERSION);

# perl standard
use Carp;

# CPAN
use MIME::Base64;

# Inca
use Inca::Constants qw(:all);
use Inca::IO;
use Inca::Net::Protocol::Statement;
use Inca::Config::Suite;
use Inca::Validate qw(:all);

#=============================================================================#
# Global Vars
#=============================================================================#

our ( $VERSION ) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $SELF_PARAM_REQ = { isa => "Inca::AgentClient" };

#=============================================================================#

=head1 CLASS METHODS

=cut
#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( $rm, $uri, %args )

Class constructor which returns a new Inca::AgentClient object.  The
constructor must be called with a $uri to the server.  The constructor
may be called with any of the following attributes.

=over 2

B<Arguments>:

=over 7

=item rm

An object of type Inca::ReporterManager which will be used to service
the requests from the Inca agent.

=item uri

A string in the format of <scheme>://<path> containing the uri to the Inca
agent where

=over 13

=item scheme

The type of URI being represented by the string (either I<file>, I<inca>, or
I<incas>)

=item path

The location of the URI being represented (e.g., localhost:7070)

=back

=item args

A list of follow on arguments for the uri type (e.g., cert, key, and
trusted certificate directory for the SSL connection).

=back

=back

=cut
#-----------------------------------------------------------------------------#
sub new {
  my $this = shift;
  my $class = ref($this) || $this;
  my $rm = shift;
  my $self = $class->SUPER::new( @_ );

  bless ($self, $class);
  $self->{rm} = $rm;

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 register( $id )

Register with the designated reporter agent and service commands.

=over 2

B<Returns>:

Returns true if the connection with the agent was terminated without 
error; false otherwise.

=back

=begin testing

  use File::Copy;
  use Inca::AgentClient;
  use Inca::Net::MockAgent;
  use Inca::Net::MockDepot;
  use Inca::ReporterManager::ReporterCache;
  use Inca::ReporterManager;
  use MIME::Base64;
  untie *STDOUT; #cron won't start otherwise -- not sure why
  untie *STDERR; #cron won't start otherwise -- not sure why

  Inca::Logger->screen_init( "FATAL" );

  my $SCHEDULE_FILENAME = "/tmp/scheduleAC.xml";

  sub createAgentClient {
    my $port = shift;
    my $depot_port = shift;
    my $credentials = shift;

    unlink "/tmp/rm.log";

    if ( ! defined $credentials ) {
      $credentials = { cert => "t/certs/client_ca1cert.pem",
                       key => "t/certs/client_ca1keynoenc.pem",
                       passphrase => undef,
                       trusted => "t/certs/trusted" };
    }
    my $rc = new Inca::ReporterManager::ReporterCache( "t",
      errorReporterPath => "bin/inca-null-reporter"
    );
    my $rm = new Inca::ReporterManager();
    $rm->setId( "resourceA" );
    $rm->setCheckPeriod( 1 );
    $rm->setReporterCache( $rc );
    $rm->setDepots( "incas://localhost:$depot_port" );
    $rm->setTmpDirectory( "/tmp" );
    $rm->setCredentials( $credentials );
    $rm->setRimPath( "sbin/reporter-instance-manager" );
    $rm->setLogger( "/tmp/rm.log" );
    my $client = new Inca::AgentClient( 
      $rm, 
      "incas://localhost:$port",
      %{$rm->getCredentials()}
    );
    unlink( $SCHEDULE_FILENAME ) if -f $SCHEDULE_FILENAME;
    $rm->setScheduleFilename( $SCHEDULE_FILENAME );

    return $client;
  }

  #---------------------------------------------
  # register
  #---------------------------------------------
  my $port = 8529;
  my $pid;
  my $agent = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
  if ( $pid = fork() ) {
    my $client = createAgentClient( $port, 8888 );
    is( $client->register("id0"), 1, "registration with agent was successful" );
  } else {
    waitpid( $pid, 0 );
    my ($conn, $numStmts, @responses) = $agent->accept(
      "REGISTER NEW" => [ 'OK', undef ],
    );
    $conn->close();
    exit;
  }

  #---------------------------------------------
  # try encrypted creds
  #---------------------------------------------
  my $credentials = { cert => "t/certs/client_ca1cert.pem",
                      key => "t/certs/client_ca1key.pem",
                      passphrase => "test",
                      trusted => "t/certs/trusted" };
  if ( $pid = fork() ) {
    my $client = createAgentClient( $port, 8888, $credentials );
    is( $client->register("id0"), 1, 
        "registration with encrypted keys to agent was successful" );
  } else {
    waitpid( $pid, 0 );
    my ($conn, $numStmts, @responses) = $agent->accept(
      "REGISTER NEW" => [ 'OK',  undef ],
    );
    $conn->close();
    exit;
  }

  #---------------------------------------------
  # try to send it an error
  #---------------------------------------------
  if ( $pid = fork() ) {
    my ($conn, $numStmts, @responses ) = $agent->accept(
      "REGISTER NEW" => ['OK', 'boogedyboo'],
      "ERROR" => [undef]
    );
    is( $numStmts, 3, "3 statements received from rm" );
    is( $responses[2]->getCmd(), "ERROR", "Received error" );
    like($responses[2]->getData(), qr/unknown command/,  "Received error msg");
    $conn->close();
    waitpid( $pid, 0 );
  } else {
    my $client = createAgentClient( $port, 8888 );
    $client->register("id0");
    exit;
  }

  #---------------------------------------------
  # try to ping it
  #---------------------------------------------
  if ( $pid = fork() ) {
    my ($conn, $numStmts, @responses ) = $agent->accept(
      "REGISTER NEW" => [ "OK", "PING ME" ],
      "OK ME" => [ undef ]
    );
    $conn->close();
    is( $numStmts, 3, "3 statements received from rm" );
    is( $responses[2]->getCmd(), "OK", "Received ping ack" );
    is( $responses[2]->getData(), "ME",  "Received ping data");
    waitpid( $pid, 0 );
  } else {
    my $client = createAgentClient( $port, 8888 );
    $client->register("id0");
    exit;
  }

  #---------------------------------------------
  # send a suite
  #---------------------------------------------
  my $depot_port = $port + 1;

  # store a proxy in the myproxy server
  ok( -f "$ENV{HOME}/.inca.myproxy.info", "proxy information found" );
  open( FD, "<$ENV{HOME}/.inca.myproxy.info" ) ||
    fail( "Unable to open proxy info" );
  my ($mp_hostname, $mp_port, $mp_username, $mp_password);
  while( <FD> ) {
    eval $_;
  }
  close FD;
  `grid-proxy-info 2>/dev/null`;
  cmp_ok( $?, "!=", 0, "grid-proxy-info returned error" );

  # launch the reporter manager's agent client
  my $rmpid;
  if ( ! ($rmpid=fork()) ) {
    my $client = createAgentClient( $port, $depot_port );
    $client->register("id0");
    exit; 
  } 

  # setup the mock agent to receive one register and 2 proxy requests
  my ($mapid1, $mapid2);
  if ( ! ($mapid1=fork()) ) {
    if ( ! ($mapid2=fork()) ) {
      sleep 2;
      for ( my $i = 0; $i < 2; $i++ ) {
        my ($conn, $numStmts, @responses ) = $agent->accept(
          "PROXY" => [ "HOSTNAME $mp_hostname", "PORT $mp_port", "USERNAME $mp_username", "PASSWORD $mp_password", "LIFETIME 1"],
          "OK" => []
        );
      }
      exit;
    }
    my ($mixed_suite, $grid_suite);
    { 
      local $/;
      open( FD, "t/mixed_suite.xml" );
      $mixed_suite = <FD>;
      close FD;
      open( FD, "t/grid_suite.xml" );
      $grid_suite = <FD>;
      close FD;
    }
    my ($conn, $numStmts, @responses ) = $agent->accept(
      "REGISTER NEW" => [ "OK", "SUITE $mixed_suite" ],
      "OK incas://localhost:6323/mixedSuite" => [ "SUITE $grid_suite" ],
      "OK incas://localhost:6323/gridSuite" => []
    );
    exit;
  }

  # verify the reports that come in - should be one openssl report from run
  # now and 1-2 gcc reports and 1-2 validproxy reports from cron
  my $i = 0;
  my $gccFound = 0, $opensslFound = 0, $gridFound = 0;
  my $depot = new Inca::Net::MockDepot();
  my $numReports = $depot->readReportsFromSSL(
    4, $depot_port, "ca1", "t/certs/trusted"
  );
  is( $numReports, 4, "4 reports read from mock depot" );
  my @reports = $depot->getReports();
  for my $report ( @reports ) {
    $i++;
    like($report, qr/completed>true/, "depot received completed report $i");
    $gccFound++ if ( $report =~ /gcc/ );
    $opensslFound++ if ( $report =~ /openssl/ );
    $gridFound++ if ( $report =~ /validproxy/ );
  }
  is( $opensslFound, 1, "1 openssl report received" );
  cmp_ok( $gccFound, ">=", 1, "at least 1 gcc report received" );
  cmp_ok( $gccFound, "<=", 2, "no more than 2 gcc reports received" );
  cmp_ok( $gridFound, ">=", 1, "at least 1 grid reports received" );
  cmp_ok( $gridFound, "<=", 2, "no more than 1 grid reports received" );
  for my $spid ( ($mapid1, $rmpid) ) {
    sleep 2;
    is( kill(3, $spid), 1, "$spid killed" );
    is( waitpid($spid, 0), $spid, "$spid waited on" );
  }
  ok( -f $SCHEDULE_FILENAME, "schedule written" );
  my $sched;
  {
    local $/;
    open ( FD, "<$SCHEDULE_FILENAME" );
    $sched = <FD>;
    close FD;
  }
  ok( grep( /<proxy>incas/, $sched ), "proxy found in schedule" );

  #---------------------------------------------
  # try to send it a package
  #---------------------------------------------
  if ( $pid = fork() ) {
    # regular reporter
    my ($reporter, $package);
    {
      local $/;
      open( FD, "t/cluster.java.sun.version" );
      $reporter = <FD>;
      close FD;
      open( FD, "t/appleseeds-2.2.1.tar.gz" );
      $package = <FD>;
      $package = encode_base64($package);
      close FD;
    }
    my ($conn, $numStmts, @responses ) = $agent->accept(
      "REGISTER NEW" => [ "OK", "PACKAGE cluster.java.sun.version", "FILENAME cluster.java.sun.version-1.5", "VERSION 1.5", "INSTALLPATH tmp", "PERMISSION 755", "CONTENT $reporter" ],
     "OK cluster.java.sun.version" => [ "PACKAGE appleseeds", "FILENAME appleseeds-2.2.1.tar.gz", "VERSION 2.2.1", "INSTALLPATH tmp", "PERMISSION 755", "CONTENT $package" ],
     "OK appleseeds" => [ undef ]
    );
    $conn->close();
    waitpid( $pid, 0 );
    ok( -f "t/tmp/cluster.java.sun.version-1.5", "reporter stored correctly" );
    ok( -f "t/include/appleseeds/ipseed.h", "appleseeds installed" );
    copy( "t/tmp/appleseeds-2.2.1.tar.gz", "t/appleseeds-2.2.1.tar.gz" );
    `rm -fr t/tmp t/include t/lib t/examples t/build`;
  } else {
    my $client = createAgentClient( $port, $depot_port );
    $client->register("id0");
    exit;
  }
  $agent->stop();

=end testing

=cut
#-----------------------------------------------------------------------------#
sub register {
  my ($self, $id) = validate_pos(@_, $SELF_PARAM_REQ, SCALAR );

  $self->{logger}->info( "Attempting to register" );
  # try pinging 
  if ( ! $self->_ping() ) {
    return 0;
  }

  # try to register
  if ( ! $self->_register($id) ) {
    return 0;
  }

  while( 1 ) { # until we get closed connection
    $self->{logger}->debug( "Waiting for command from agent '$self->{uri}'" );
    my ($cmd, $data) = $self->readStatement();
    if ( ! defined $cmd || $cmd eq '' ) { # we may have lost contact
      # for some unknown reason, we sometimes get a false positive that the 
      # socket closed on read, but if we try again it succeeds.  However, if
      # it fails again, then we assume the agent is down
      $self->{logger}->warn( 
        "Detected closed connection from agent...attempting read to confirm" 
      );
      ($cmd, $data) = $self->readStatement();
      if ( ! defined $cmd || $cmd eq '' ) { # okay, we did lose contact
        $self->{logger}->error( "Connection closed with agent '$self->{uri}'" );
        last;
      } 
    }
    $self->{logger}->debug("Received command '$cmd' from agent " .$self->{uri});
    if ( $cmd eq "ERROR" ) {
      $self->{logger}->error( "Received error from reporter agent '$data'" );
    } elsif ( $cmd eq "PING" ) {
      $self->_acceptPing( $data );
      for my $scheduler ( keys %{$self->{rm}->{schedulers}} ) {
        $self->{logger}->debug( "RM is running scheduler '$scheduler'" );
        my $sched = $self->{rm}->_getScheduler($scheduler);
        if ( defined $sched ) {
          if ( $sched->isRunning() ) {
            $self->{logger}->debug( "$scheduler scheduler is running" );
          } else {
            $self->{logger}->debug( "$scheduler scheduler is NOT running " . $sched->{'cronpid'} );
            my $pscmd = "ps -p " . $sched->{'cronpid'};
            for( my $i = 0; $i < 5; $i++ ) {
              $self->{logger}->debug( `$pscmd` );
              sleep 3;
            }
            $sched->stop();
            $sched->start();
          }
        }
      }
    } elsif ( $cmd eq "SUITE" ) {
      $self->_acceptSuite( $data );
    } elsif ( $cmd eq "PACKAGE" ) {
      $self->_acceptPackage( $data );
    } else {
      $self->writeAndLogError( 
        "Received unknown command '$cmd' from $self->{uri}"
      );
    }
  }

  $self->close();

  return 1;
}

#e----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _acceptPackage( $name )
#
# Read the package from the agent and  store it.  Protocol goes like this:
#
# PACKAGE <name>
# FILE <name>
# VERSION <version>
# INSTALLPATH <installpath>
# (opt) PERMISSION 755
# CONTENT <package text>
#
# Arguments:
#
# name     A string that contains the name of the package.
#
# Returns: 
#
# Returns true if the package was succesfully stored and false otherwise.
#-----------------------------------------------------------------------------#
sub _acceptPackage {
  my ( $self, $name ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  my ( $cmd1, $filename ) = $self->readStatement();
  if ( $cmd1 ne "FILENAME" ) {
    $self->writeAndLogError( "Expected FILENAME; received $cmd1" );
    return 0;
  }
  my ( $cmd2, $version ) = $self->readStatement();
  if ( $cmd2 ne "VERSION" ) {
    $self->writeAndLogError( "Expected VERSION; received $cmd2" );
    return 0;
  }
  my ( $cmd3, $installpath ) = $self->readStatement();
  if ( $cmd3 ne "INSTALLPATH" ) {
    $self->writeAndLogError( "Expected INSTALLPATH; received $cmd3" );
    return 0;
  }
  my $permission = undef;
  my ( $cmd4, $content ) = $self->readStatement();
  if ( $cmd4 eq "PERMISSION" ) {
    $permission = $content;
    ( $cmd4, $content ) = $self->readStatement();
  } 
  my $dependencies = undef;
  if ( $cmd4 eq "DEPENDENCIES" ) {
    $dependencies = $content;
    ( $cmd4, $content ) = $self->readStatement();
  } 
  if ( $cmd4 ne "CONTENT" ) {
    $self->writeAndLogError( "Expected CONTENT; received $cmd4" );
    return 0;
  }
  my ($tmpfh, $tmpfilename) = Inca::IO->tempfile( "incapkgtmp" );
  if ( $filename =~ /\.tar\.gz$/ ) {
    print $tmpfh decode_base64($content);
  } else {
    print $tmpfh $content;
  }
  $tmpfh->close();
  $self->writeStatement(
    Inca::Net::Protocol::Statement->getOkStatement(data=>$name)
  );
  my $success = $self->{rm}->storePackage( 
    $name, $filename, $version, $installpath, $permission, $dependencies, 
    $tmpfilename 
  );
  unlink( $tmpfilename );
}

#-----------------------------------------------------------------------------#
# _acceptPing( $message )
#
# Accept the PING command from the connected Agent.  
#
#
# Arguments:
#
# message The message of the ping statement that should be returned to the
#         agent
#-----------------------------------------------------------------------------#
sub _acceptPing {
  my ( $self, $message ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  # return ping
  $self->{logger}->debug( "Acking ping from agent: $message" );
  $self->writeStatement( "OK", $message );
}

#-----------------------------------------------------------------------------#
# _acceptSuite( $suiteXml, $client )
#
# Read the suite in $suite and reply to it.
#
# Arguments:
#
# suiteXml  A string containing suite XML
#
# client    An object of Inca::Net::Client connected to the agent
#
# Returns: 
#
# Returns true if the register succeeds and false if it does not.
#
#-----------------------------------------------------------------------------#
sub _acceptSuite {
  my ( $self, $suiteXml ) = validate_pos( @_, $SELF_PARAM_REQ, SCALAR );

  my $suite = new Inca::Config::Suite();
  if ( ! $suite->read( $suiteXml ) ) {
    $self->writeAndLogError(
      "Error in suite detected; see manager log for more details"
    );
    return;
  }
  # reply immediately if xml reads well
  $self->{logger}->debug( "Acking receipt of suite update ".$suite->getGuid() );
  $self->writeStatement(
    Inca::Net::Protocol::Statement->getOkStatement( data => $suite->getGuid() )
  );

  # dispatch
  $suite->resolveReporters
    ( $self->{rm}->getReporterCache(), $self->{uri} );
  my $errored_configs = $self->{rm}->dispatch( $suite, 1 );
  my $num_configs = scalar( $suite->getSeriesConfigs() );
  if ( scalar(@{$errored_configs}) > 0 ) {
    $self->{logger}->error(
      scalar(@{$errored_configs}) .  " out of " .
      "$num_configs configs failed to be scheduled: " .
      join( " ", @{$errored_configs}) 
    );
  } else {
    $self->{logger}->info( "$num_configs configs scheduled" );
  }
  if ( ! $self->{rm}->start() ) {
    $self->{logger}->error( "Unable to start schedulers" );
  }
}

#-----------------------------------------------------------------------------#
# _ping( )
#
# Send the PING command to the connected Agent.  If succeeds, return true.
#
# Returns:
#
# Returns true if ping command succeeds with Agent; false otherwise.

=begin testing

  use Inca::AgentClient;
  use Inca::Net::MockAgent;
  use Test::Exception;

  my $port = 8519;
  my $agent = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
  my $pid;
  if ( $pid = fork() ) {
    my $client = new Inca::AgentClient( 
      undef, 
      "incas://localhost:$port", 
      cert => "t/certs/client_ca1cert.pem",
      key => "t/certs/client_ca1keynoenc.pem",
      passphrase => undef,
      trusted => "t/certs/trusted" 
    );
    ok( $client->_ping(), "client pings" );
    $client->close();
  } else { 
    sleep 2;
    my ($conn, $numStmts, @responses ) = $agent->accept(
      "REGISTER NEW" => [ "OK", "PING ME" ],
      "OK ME" => [ undef ]
    );
    $conn->close();
    exit;
  }                     
  $agent->stop();

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _ping {
  my ( $self ) = validate_pos( @_, $SELF_PARAM_REQ );

  # try pinging 
  if ( !  $self->writeAndReadStatement( "PING", "ME", "OK", "ME" ) ) {
    $self->close();
    return 0;
  }
  $self->{logger}->info( "Succesfully pinged agent" );
  return 1;
}

#-----------------------------------------------------------------------------#
# _register( $id )
#
# Send the REGISTER command to the connected Agent.  If succeeds, return true.
#
# Returns:
#
# Returns true if register command succeeds with Agent; false otherwise.

=begin testing

  use Inca::AgentClient;
  use Inca::Net::MockAgent;
  use Inca::ReporterManager;
  use Inca::ReporterManager::ReporterCache;
  use Test::Exception;

  my $port = 8539;
  my $agent = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
  my $pid;

  unlink "afile";
  for my $type ( qw(NEW EXISTING) ) {
    if ( $pid = fork() ) {
      my $rm = new Inca::ReporterManager( id => "x" );
      my $rc = new Inca::ReporterManager::ReporterCache( "t",
        errorReporterPath => "bin/inca-null-reporter"
      );
      $rm->setReporterCache( $rc );
      $rm->setScheduleFilename( "afile" );
      my $client = new Inca::AgentClient( 
        $rm, 
        "incas://localhost:$port", 
        cert => "t/certs/client_ca1cert.pem",
        key => "t/certs/client_ca1keynoenc.pem",
        passphrase => undef,
        trusted => "t/certs/trusted" 
      );
      ok( $client->_register( "id0" ), "client registered $type" );
      $rm->stop();
      $client->close();
    } else { 
      sleep 2;
      my ($conn, $numStmts, @responses) = $agent->accept(
        "REGISTER $type" => [ 'OK', undef ],
      );
      $conn->close();
      exit;
    }                     
    open( FD, ">afile" );
    print FD "<schedule/>\n";
    close FD;
  }
  $agent->stop();
  unlink "afile";

=end testing

=cut
#-----------------------------------------------------------------------------#
sub _register {
  my ($self, $id) = validate_pos(@_, $SELF_PARAM_REQ, SCALAR );

  my $registerType = "REGISTER ";
  if ( -s $self->{rm}->getScheduleFilename() ) {
    $registerType .= "EXISTING";
  } else {
    $registerType .= "NEW";
  }
  # try to register
  if ( ! $self->writeAndReadStatement( $registerType, $id, "OK" ) ) {
    $self->close();
    return 0;
  }
  $self->{logger}->info( "Successfully registered $id with Agent" );
  return 1;
}

1;  # need to return a true value from the file


__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

Describe any known problems.

=cut
