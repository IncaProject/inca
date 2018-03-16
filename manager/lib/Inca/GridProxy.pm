package Inca::GridProxy;

################################################################################

=head1 NAME

Inca::GridProxy - Manages the renewal of a globus proxy credential via MyProxy
      
=head1 SYNOPSIS

=for example begin

  use Inca::GridProxy;
  eval {
    my $proxy = new Inca::GridProxy( "id", "inca://localhost:6666" ); 
    my $params = $proxy->requestProxyInformation();
    $proxy->getProxy( $params );
  };

=for example end

=head1 DESCRIPTION

Used to connect to the Inca agent to request the MyProxy proxy information and
then connect to a MyProxy server to retrieve a short-term proxy credential.

=cut 

################################################################################

#=============================================================================#
# Usage
#=============================================================================#

# pragmas
use strict;
use warnings;
use vars qw($VERSION);

# Perl standard
use Carp;
use Cwd 'abs_path';
use File::Basename;

# Inca
use Inca::Constants qw(:all);
use Inca::Logger;
use Inca::Net::Client;
use Inca::Validate qw(:all);

#=============================================================================#
# Global Vars
#=============================================================================#

our ($VERSION) = '$Revision: 1.2 $' =~ 'Revision: (.*) ';

my $MINIMUM_AGE = 4 * $SECS_TO_HOUR;

#=============================================================================#

=head1 CLASS METHODS

=cut

#=============================================================================#

#-----------------------------------------------------------------------------#
# Public methods (documented with pod markers)
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#

=head2 new( )

Class constructor which returns a new Inca::GridProxy object.  If unable to
locate the executables grid-proxy-info and myproxy-get-delegation, will issue
'die'. 

=begin testing

  use Inca::GridProxy;
  use Test::Exception;
  lives_ok { new Inca::GridProxy() }  'object created';
  delete $ENV{LD_LIBRARY_PATH};
  ok( ! exists $ENV{LD_LIBRARY_PATH}, "LD_LIBRARY_PATH not defined" );
  new Inca::GridProxy();
  ok( exists $ENV{LD_LIBRARY_PATH}, "LD_LIBRARY_PATH defined" );

=end testing

=cut

#-----------------------------------------------------------------------------#
sub new {
  my $this  = shift;
  my $class = ref($this) || $this;
  my $self  = {};

  bless( $self, $class );

  # initialize
  $self->{logger}   = Inca::Logger->get_logger($class);

  # test for myproxy
  $self->{execs} = {};
  for my $exec (qw(myproxy-get-delegation grid-proxy-info)) {
    my $testexec = `which $exec`;
    if ( $? == 0 || (! -z "$testexec" && "$testexec" != " ") ) {
      local $/ = "";
      chomp($testexec);
      $self->{execs}->{$exec} = $testexec;
    } elsif ( -x "$ENV{GLOBUS_LOCATION}/bin/$exec" ) {
      $self->{execs}->{$exec} = "$ENV{GLOBUS_LOCATION}/bin/$exec";
    } elsif ( -x "$ENV{MYPROXY_LOCATION}/bin/$exec" ) {
      $self->{execs}->{$exec} = "$ENV{MYPROXY_LOCATION}/bin/$exec";
    } else {
      $self->{logger}->error("$exec not found in path");
      die "$exec not found in path";
    }
    $self->{logger}->info( "Found " . $self->{execs}->{$exec} );
    my $d = abs_path( (dirname($self->{execs}->{$exec}) . "/../lib") );
    for my $dynlibpath ( qw(LD_LIBRARY_PATH LIBPATH) ) {
      if ( exists $ENV{$dynlibpath} ) {
        if ( $ENV{$dynlibpath} !~ /$d/ ) {
          $ENV{$dynlibpath} = "$d:$ENV{$dynlibpath}";
        }
      } else {
        $ENV{$dynlibpath} = $d;
      }
    }
  }
  $self->{logger}->debug( "LD_LIBRARY_PATH=$ENV{LD_LIBRARY_PATH}" );
  $self->{logger}->debug( "LIBPATH=$ENV{LIBPATH}" );

  return $self;
}

#-----------------------------------------------------------------------------#

=head2 getProxy( $params, $filename )

Use the MyProxy server information passed in $params to retrieve a proxy
and store it in $filename.
 
=over 2

B<Arguments>:

=over 13

=item params

A reference to a hash array containing information about how to connect to
a MyProxy server.

=item filename

A string containing the path to where the proxy should be stored.

=back

B<Returns>:

True on successful retrieval; false otherwise.

=back

=begin testing

  use Test::Exception;
  use POSIX ":sys_wait_h";
  use Inca::GridProxy;
  use Inca::Net::MockAgent;
  untie *STDOUT;
  untie *STDERR;

  Inca::Logger->screen_init( "FATAL" );

  my $filename = "/tmp/inca.gridproxytest.$$";
  ok( -f "$ENV{HOME}/.inca.myproxy.info", "proxy information found" );
  open( FD, "<$ENV{HOME}/.inca.myproxy.info" ) ||
    fail( "Unable to open proxy info" );
  my ($mp_hostname, $mp_port, $mp_username, $mp_password);
  while( <FD> ) {
    eval $_;
  }
  close FD;
  `grid-proxy-info 2> /dev/null`;
  cmp_ok( $?, "!=", 0, "grid-proxy-info returned error" );
  
  my $port = 8519;
  my $agent = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
  my $pid;
  if ( $pid = fork() ) {
    my $proxy = new Inca::GridProxy();
    my $params = $proxy->requestProxyInformation( 
      "resourceA", 
      "incas://localhost:$port", 
      cert => "t/certs/client_ca1cert.pem",
      key => "t/certs/client_ca1keynoenc.pem",
      passphrase => undef,
      trusted => "t/certs/trusted" 
    );
    ok( defined $params, "proxy info retrieved" );
    ok( $proxy->getProxy( $params, $filename ), 
        "proxy retrieved from myproxy server" );
    `grid-proxy-info -file $filename >/dev/null 2>&1`;
    cmp_ok( $?, "==", 0, "proxy verified" );
    unlink $filename;
   } else {
    sleep 2;
    $agent->accept(
      "PROXY" => [ "HOSTNAME $mp_hostname", "PORT $mp_port", "USERNAME $mp_username", "PASSWORD $mp_password", "LIFETIME 1"],
      "OK" => []
    );
    exit;
  }
  $agent->stop();
   `myproxy-destroy -s localhost -l test`;

=end testing

=cut

#-----------------------------------------------------------------------------#
sub getProxy {
  my ( $self, $myproxy, $filename ) =
    validate_pos( @_, { isa => "Inca::GridProxy" }, HASHREF, SCALAR );

  # somewhere else in the manager, the CHLD signal handler is being set.
  # by setting it back to the default here, we allow open and `` to reap
  # their own children; otherwise they will error with 'No child processes'
  local $SIG{CHLD} = 'DEFAULT';

  # download proxy from myproxy server
  if ( exists $myproxy->{dn} ) {
    $self->{logger}->info( "Setting server dn to '$myproxy->{dn}'" );
    $ENV{MYPROXY_SERVER_DN} = $myproxy->{dn};
  }
  my $myproxy_cmd = "$self->{execs}->{'myproxy-get-delegation'} " . 
                    "-s $myproxy->{hostname} " .
                    "-p $myproxy->{port} " . 
                    "-t $myproxy->{lifetime} " .
                    "-S -l $myproxy->{username}";
  $myproxy_cmd .= " -o $filename";
  $self->{logger}->info( "Attempting to retrieve proxy: $myproxy_cmd" );
  pipe( STDIN_READ, STDIN_WRITE );
  pipe( STDOUT_READ, STDOUT_WRITE );
  my $pid;
  if ( ($pid=fork()) == 0 ) { # child
    open(STDIN, '<&STDIN_READ') || $self->{logger}->logdie( "Could not redirect STDIN" );
    open( STDOUT, ">&STDOUT_WRITE" ) || $self->{logger}->logdie( "Could not redirect STDOUT" );
    open( STDERR, ">&STDOUT" ) || $self->{logger}->logdie( "Cannot redirect stderr to stdout" );
    exec $myproxy_cmd || $self->{logger}->logdie( "'$myproxy_cmd' failed: $!" );
  } 
  local $SIG{CHLD} = sub {
    $self->{logger}->debug("wait: status $? on $pid") if waitpid($pid, 0) > 0
  };
  print STDIN_WRITE $myproxy->{password}, "\n";
  close STDIN_WRITE; 
  close STDIN_READ;
  close STDOUT_WRITE;
  my $myproxyOut = "";
  while ( <STDOUT_READ> ) {
    $myproxyOut .= $_;
  }
  close STDOUT_READ;
  $self->{logger}->info( "myproxy>$myproxyOut" );

  # reset child handler to default and verify proxy
  local $SIG{CHLD} = 'DEFAULT';
  chmod( 0600, $filename ) if ( -f $filename ); 
  my $gridinfo_cmd = $self->{execs}->{"grid-proxy-info"} . 
                     " -file $filename -exists";
  $self->{logger}->debug( $gridinfo_cmd );
  my $result = `$gridinfo_cmd 2>&1`;
  if ( $? != 0 ) {
    $self->{logger}->error( 
      "Proxy existence not verified by grid-proxy-info: $! $result" 
    );
    return 0;
  }
  $self->{logger}->info( "Proxy successfully retrieved" );
  return 1;
}

#-----------------------------------------------------------------------------#

=head2 requestProxyInformation( $id, $uri, @args )

Contact the agent and request the MyProxy information to be sent to it.

=over 2 

B<Arguments>:

=over 13

=item id

A string containing the reporter manager identifier to use when contacting
the agent.

=item uri

A string in the format of <scheme>://<path> containing the uri of the agent
where

=over 13

=item scheme

The type of URI being represented by the string (either I<file>, I<inca>, or
I<incas>)

=item path

The location of the URI being represented (e.g., localhost:7070)

=back

=item args

A list of follow on arguments for the stream type (e.g., cert, key, and
trusted certificate directory for the SSL connection).

=back

=back

=over 2

B<Returns>:

A reference to a hash containing the proxy renewal information if successful 
contact with the agent and undef otherwise.

=back

=begin testing

  use Inca::GridProxy;
  use Test::Exception;
  use Inca::IO;
  use Inca::Net::Client;

  Inca::Logger->screen_init( "FATAL" );

  ok( -f "$ENV{HOME}/.inca.myproxy.info", "proxy information found" );
  open( FD, "<$ENV{HOME}/.inca.myproxy.info" ) ||
    fail( "Unable to open proxy info" );
  my ($mp_hostname, $mp_port, $mp_username, $mp_password);
  while( <FD> ) {
    eval $_;
  }
  close FD;

  my $port = 8519;
  my $agent = new Inca::Net::MockAgent( $port, "ca1", "t/certs/trusted" );
  my $pid;
  if ( $pid = fork() ) {
    my $proxy = new Inca::GridProxy();
    my $params = $proxy->requestProxyInformation(
      "resourceA", 
      "incas://localhost:$port", 
      cert => "t/certs/client_ca1cert.pem",
      key => "t/certs/client_ca1keynoenc.pem",
      passphrase => undef,
      trusted => "t/certs/trusted" 
    );
    ok( defined $params, "params is defined" );   
    is( $params->{hostname}, $mp_hostname, "hostname received" );
    ok( defined $params, "params is defined" );
    is( $params->{port}, $mp_port, "port received" );
    is( $params->{username}, $mp_username, "username received" );
    is( $params->{password}, $mp_password, "password received" );
    is( $params->{lifetime}, 1, "lifetime received" );
  } else { 
    sleep 2;
    $agent->accept(
      "PROXY" => [ "HOSTNAME $mp_hostname", "PORT $mp_port", "USERNAME $mp_username", "PASSWORD $mp_password", "LIFETIME 1"],
      "OK" => []
    );
    exit;
  }                     
  $agent->stop();
  
=end testing

=cut

#-----------------------------------------------------------------------------#
sub requestProxyInformation {

  # validate the required params; the rest should be the agent contact info
  my @required_params = @_[0 .. 1];
  my ($self, $id ) = validate_pos( 
    @required_params, { isa => "Inca::GridProxy" }, SCALAR 
  );
   
  # store agent contact info in array
  my @agentInfo = (@_[2 .. $#_] ); 
  $self->{logger}->info( "Requesting proxy renewal info from agent" );
  my $client = new Inca::Net::Client( @agentInfo );
  if ( ! defined $client ) {
    $self->{logger}->error( 
      "Unable to contact agent to request proxy renew information" 
    );
    return undef;
  }

  if ( !$client->writeStatement( "PROXY_RENEW_INFO", $id ) ) {
    $self->{logger}->error( 
      "Unable to write proxy renew information request to agent" 
    );
    return undef;
  }
  my $proxyRenewInfo = {};
  my ( $cmd, $data ) = $client->readStatement();
  if ( $cmd eq "HOSTNAME" ) {
    $proxyRenewInfo->{hostname} = $data;
  } else {
    $client->writeAndLogError("Expected HOSTNAME; received '$cmd'");
    return undef;
  }
  ( $cmd, $data ) = $client->readStatement();
  if ( $cmd eq "DN" ) {
    $proxyRenewInfo->{dn} = $data;
    ( $cmd, $data ) = $client->readStatement();
  }
  if ( $cmd eq "PORT" ) {
    $proxyRenewInfo->{port} = $data;
  } else {
    $client->writeAndLogError("Expected PORT; received '$cmd'");
    return undef;
  }
  ( $cmd, $data ) = $client->readStatement();
  if ( $cmd eq "USERNAME" ) {
    $proxyRenewInfo->{username} = $data;
  } else {
    $client->writeAndLogError("Expected USERNAME; received '$cmd'");
    return undef;
  }
  ( $cmd, $data ) = $client->readStatement();
  if ( $cmd eq "PASSWORD" ) {
    $proxyRenewInfo->{password} = $data;
  } else {
    $client->writeAndLogError("Expected PASSWORD; received '$cmd'");
    return undef;
  }
  ( $cmd, $data ) = $client->readStatement();
  if ( $cmd eq "LIFETIME" ) {
    $proxyRenewInfo->{lifetime} = $data;
  } else {
    $client->writeAndLogError("Expected LIFETIME; received '$cmd'");
    return undef;
  }
  $client->writeStatement( "OK", undef );
  $client->close();
  $self->{logger}->info( "Proxy renewal info successfully retrieved" );

  return $proxyRenewInfo;
}

1;                             # need to return a true value from the file

__END__


=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 CAVEATS/WARNINGS

None so far.

=cut
