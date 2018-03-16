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

my $Original_File = 'lib/Inca/Net/Protocol/Statement.pm';

package main;

# pre-5.8.0's warns aren't caught by a tied STDERR.
$SIG{__WARN__} = sub { $main::_STDERR_ .= join '', @_; };
tie *STDOUT, 'Catch', '_STDOUT_' or die $!;
tie *STDERR, 'Catch', '_STDERR_' or die $!;

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 128 lib/Inca/Net/Protocol/Statement.pm

  use Inca::Net::Protocol::Statement;
  use Test::Exception;
  lives_ok { new Inca::Net::Protocol::Statement() } 'object created';


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 232 lib/Inca/Net/Protocol/Statement.pm

  use Inca::Net::Protocol::Statement;
  use Socket qw(:crlf);

  my $statement = new Inca::Net::Protocol::Statement();
  is( $statement->getErrorStatement( "to err is human" )->toString(), 
      "ERROR to err is human$CRLF", "getErrorStatement()" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 279 lib/Inca/Net/Protocol/Statement.pm

  use Inca::Net::Protocol::Statement;
  use Socket qw(:crlf);

  is( Inca::Net::Protocol::Statement->getOkStatement()->toString(), 
      "OK$CRLF", "getOkStatement()" );
  is( Inca::Net::Protocol::Statement->getOkStatement( data=>200 )->toString(), 
      "OK 200$CRLF", "getOkStatement(data=>200)" );
  is( Inca::Net::Protocol::Statement->getOkStatement( data=>"str")->toString(), 
      "OK str$CRLF", "getOkStatement(data=>str)" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 320 lib/Inca/Net/Protocol/Statement.pm

  use Inca::Net::Protocol::Statement;
  use Socket qw(:crlf);

  my $statement = new Inca::Net::Protocol::Statement();
  is( $statement->getStartStatement()->toString(), "START 1$CRLF", 
      "getStartStatement()" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 384 lib/Inca/Net/Protocol/Statement.pm

  use Inca::Net::Protocol::Statement;
  use Test::Exception;

  my $statement = new Inca::Net::Protocol::Statement( cmd => "PING" );
  is( $statement->getCmd(), "PING", "set CMD works from constructor" );
  $statement->setCmd( "START" );
  is( $statement->getCmd(), "START", "set/getCmd works" );
  

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 424 lib/Inca/Net/Protocol/Statement.pm

  use Inca::Net::Protocol::Statement;
  use Test::Exception;

  my $statement = new Inca::Net::Protocol::Statement( data => "blech" );
  is( $statement->getData(), "blech", "set DATA works from constructor" );
  $statement->setData( "yuck" );
  is( $statement->getData(), "yuck", "set/getData works" );
  

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 464 lib/Inca/Net/Protocol/Statement.pm

  use Inca::Net::Protocol::Statement;
  use Test::Exception;

  my $statement = new Inca::Net::Protocol::Statement( 
     cmd => "tree",
     data => "is in a forrest" 
  );
  my $statement2 = new Inca::Net::Protocol::Statement( 
     cmd => "cactus",
     data => "is in a desert" 
  );
  my $statement3 = new Inca::Net::Protocol::Statement( 
     cmd => "multiline",
     data => "it was the winter of discontent\n it was the something\nand something else" 
  );
  my $statement4 = new Inca::Net::Protocol::Statement( 
     cmd => "cactus"
  );
  my $echo = new Inca::Net::Protocol::Statement(stmt => $statement->toString());
  is( $echo->getData(), $statement->getData(), 
      "setStatement works from constructor(1)" );
  is( $echo->getData(), $statement->getData(), 
      "setStatement works from constructor(2)" );
  $echo->setStatement( $statement2->getStatement() );
  is( $echo->getCmd(), $statement2->getCmd(), 
      "set/getStatement works (1)" );
  is( $echo->getData(), $statement2->getData(), 
      "setStatement works from constructor(2)" );
  $echo->setStatement( $statement3->getStatement() );
  is( $echo->getCmd(), $statement3->getCmd(), 
      "set/getStatement works for multiline data (cmd)" );
  is( $echo->getData(), $statement3->getData(), 
      "set/getStatement works for multiline data (data)" );
  $echo->setStatement( $statement4->getStatement() );
  is( $echo->getCmd(), $statement4->getCmd(), 
      "set/getStatement works for cmd only" );
  is( $echo->getData(), $statement4->getData(), 
      "set/getStatement works for cmd only (data undef)" );
  

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 529 lib/Inca/Net/Protocol/Statement.pm

  use Inca::Net::Protocol::Statement;
  use Socket qw(:crlf);

  my $statement = new Inca::Net::Protocol::Statement();
  $statement->setCmd( "PING" );
  $statement->setData( "testdata" );
  is( $statement->toString(), "PING testdata$CRLF",
      "toString works with complete statement" );

  my $bad_statement = new Inca::Net::Protocol::Statement();
  $bad_statement->setCmd( "PING" );
  is( $bad_statement->toString(), "PING$CRLF", 
      "toString works for cmd only" );


    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
eval q{
  my $example = sub {
    local $^W = 0;

#line 12 lib/Inca/Net/Protocol/Statement.pm

  use Inca::Net::Protocol::Statement;
  my $statement = new Inca::Net::Protocol::Statement();
  $statement->setCmd( "PING" );
  $statement->setData( "me" );
  print $statement->toString();




;

  }
};
is($@, '', "example from line 12");

{
    undef $main::_STDOUT_;
    undef $main::_STDERR_;
#line 12 lib/Inca/Net/Protocol/Statement.pm

  use Inca::Net::Protocol::Statement;
  my $statement = new Inca::Net::Protocol::Statement();
  $statement->setCmd( "PING" );
  $statement->setData( "me" );
  print $statement->toString();




  use Socket qw(:crlf);
  is( $_STDOUT_, "PING me$CRLF", "ping example" );

    undef $main::_STDOUT_;
    undef $main::_STDERR_;
}

    undef $main::_STDOUT_;
    undef $main::_STDERR_;

