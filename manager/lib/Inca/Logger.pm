package Inca::Logger;

###############################################################################

=head1 NAME

Inca::Logger - Wrapper to encapsulate dependence on Logger::Log4perl

=head1 SYNOPSIS

=for example begin

  use Inca::Logger ":all";

=for example end

=head1 DESCRIPTION

This module wraps the functions exported by Log::Log4perl, allowing access
to them when the module is installed and preventing clients from puking
otherwise.

=for example_testing
  is( $INFO, 20000, "INFO imported" );

=end testing

=cut
###############################################################################


#=============================================================================#
# Usage
#=============================================================================#

# pragmas
use strict;
use warnings;
use vars qw($AUTOLOAD @ISA @EXPORT @EXPORT_OK);

# Perl standard
require Exporter;

#=============================================================================#
# Export help
#=============================================================================#
@ISA = qw(Exporter);

# export tags

our %EXPORT_TAGS = (
  'all' => ['get_logger', 'init', '$INFO', '$WARN', '$ERROR']
);
@EXPORT_OK = ( @{ $EXPORT_TAGS{all} } );
@EXPORT = qw( );
my $log4perlAvailable;

BEGIN {
  $log4perlAvailable = 0;
  eval {
    require Log::Log4perl;
    $log4perlAvailable = 1;
  };
}

our ($INFO, $WARN, $ERROR);
if($log4perlAvailable) {
  $INFO = $Log::Log4perl::INFO;
  $WARN = $Log::Log4perl::WARN;
  $ERROR = $Log::Log4perl::ERROR;
} else {
  $INFO = 20000;
  $WARN = 30000;
  $ERROR = 40000;
}

sub file_init {
  my ($class, $level, $logfile) = @_;
  if($log4perlAvailable) {
    Log::Log4perl->init( get_file_config($level, $logfile) );
  }
}

sub screen_init {
  my ($class, $level) = @_;
  if($log4perlAvailable) {
    Log::Log4perl->init( get_screen_config($level) );
  }
}

sub get_file_config {
  my ($level, $logfile) = @_;
  return {
    'log4j.appender.file' => 'Log::Dispatch::File::Rolling',
    'log4j.appender.file.name' => "reporter-manager",
    'log4j.appender.file.filename' => $logfile,
    'log4j.appender.file.TZ' => 'GMT',
    'log4j.appender.file.mode' => 'append',
    'log4j.appender.file.layout' => 'org.apache.log4j.PatternLayout',
    'log4j.appender.file.layout.ConversionPattern' => '%d{ISO8601} %5p %c{1}:%L - %m%n',
    'log4j.rootLogger' => "$level, file"
  };
}

sub get_screen_config {
  my ($level) = shift;
  return {
    'log4perl.rootLogger' => "$level, Screen",
    'log4perl.appender.Screen' => 'Log::Log4perl::Appender::Screen',
    'log4perl.appender.Screen.stderr' => 1,
    'log4j.appender.Screen.layout' => 'org.apache.log4j.PatternLayout',
    'log4j.appender.Screen.layout.ConversionPattern' => '%d{ABSOLUTE} %5p %c{1}:%L - %m%n'
  };
}

sub get_logger {
  my($class, $tag) = @_;
  if($log4perlAvailable) {
    Log::Log4perl->init_once( get_screen_config("DEBUG") );
    return Log::Log4perl->get_logger($tag);
  }
  my $self = { };
  return bless($self, $class);
}

sub init {
  if($log4perlAvailable) {
    Log::Log4perl->init(@_);
  }
}

sub appenders {
  if($log4perlAvailable) {
    return Log::Log4perl->appenders();
  }
}

sub AUTOLOAD {
  if($AUTOLOAD =~ /fatal|die|croak/) {
    my ($self, $msg) = @_;
    die $msg;
  }
}

1;

__END__

=head1 EXAMPLE
  
  use Inca::Logger ":all";
  Inca::Logger->init();
  $logger = Inca::Logger->get_logger('mylog');
  $logger->info("Any message");
  $logger->fatal("Any message");

=head1 AUTHOR

Jim Hayes <jhayes@sdsc.edu>

=cut
