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

my $Original_File = 'lib/Inca/GridProxy.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 80 lib/Inca/GridProxy.pm

  use Inca::GridProxy;
  use Test::Exception;
  lives_ok { new Inca::GridProxy() }  'object created';
  delete $ENV{LD_LIBRARY_PATH};
  ok( ! exists $ENV{LD_LIBRARY_PATH}, "LD_LIBRARY_PATH not defined" );
  new Inca::GridProxy();
  ok( exists $ENV{LD_LIBRARY_PATH}, "LD_LIBRARY_PATH defined" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 169 lib/Inca/GridProxy.pm

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


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 341 lib/Inca/GridProxy.pm

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
  

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/GridProxy.pm

  use Inca::GridProxy;
  eval {
    my $proxy = new Inca::GridProxy( "id", "inca://localhost:6666" ); 
    my $params = $proxy->requestProxyInformation();
    $proxy->getProxy( $params );
  };

;

  }
};
is($@, '', "example from line 12");

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

