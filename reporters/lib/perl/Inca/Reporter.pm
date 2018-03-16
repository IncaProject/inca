package Inca::Reporter;

################################################################################

=head1 NAME

Inca::Reporter - Module for creating Inca reporters

=head1 SYNOPSIS

  use Inca::Reporter;
  my $reporter = new Inca::Reporter(
    name => 'hack.version',
    version => 0.1,
    description => 'A really helpful reporter description',
    url => 'http://url.to.more.reporter.info'
  );

=head1 DESCRIPTION

This module creates Inca reporters--objects that produce XML that follows the
Inca Report schema.  The constructor may be called with a number of reporter
attributes that can later be set and queried with their corresponding get/set
functions (described below).  For example,

  my $reporter = new Inca::Reporter();
  $reporter->setUrl('http://url.to.more.reporter.info');
  $reporter->setVersion(0.1);

=cut
################################################################################

use strict;
use warnings;
use Carp;
use Cwd;
use File::Basename 'basename';
use POSIX 'EINTR';
use Sys::Hostname;

#=============================================================================#

=head1 CLASS METHODS

=cut

#=============================================================================#

#-----------------------------------------------------------------------------#

=head2 new(%attributes)

Class constructor that returns a new Inca::Reporter object.  The constructor
may be called with any of the following named attributes as parameters.

=over 13

=item body

the XML body of the report.  See the Inca Report schema for format.

=item completed

boolean (0 or 1) indicating whether or not the reporter has completed
generating the information it is intended to produce

=item description

a verbose description of the reporter

=item fail_message

a message describing why the reporter failed to complete its task

=item name

the name that identifies this reporter

=item url

URL to get more information about the reporter

=item version

the version of the reporter; defaults to 0

=back

=cut

#-----------------------------------------------------------------------------#
sub new {

  my ($this, %attributes) = @_;
  my $class = ref($this) || $this;
  my $self = {
    args => {},
    argv => [],
    body => undef,
    completed => 0,
    cwd => cwd(),
    description => undef,
    dependencies => [],
    fail_message => undef,
    log_entries => [],
    log_pat => '^$',
    name => basename($0),
    temp_paths => [],
    url => undef,
    version => '0'
  };
  bless($self, $class);

  $self->addArg('help', 'display usage information (no|yes)', 'no', 'no|yes');
  $self->addArg
    ('log', 'log message types included in report', 0,
     '[012345]|debug|error|info|system|warn');
  $self->addArg('verbose', 'verbosity level (0|1|2)', '1', '[012]');
  $self->addArg('version', 'show reporter version (no|yes)', 'no', 'no|yes');
  $self->addDependency( __PACKAGE__ );

  # check to see if user specified any attributes in arguments
  foreach my $attr(keys %attributes) {
    carp "'$attr' is an invalid attribute" if !exists($self->{$attr});
    $self->{$attr} = $attributes{$attr};
  }

  return $self;

}

#-----------------------------------------------------------------------------#

=head2 addArg($name, $description, $default, $pattern)

Adds a command line argument (invocation syntax -name=value) to the reporter.
If supplied, the optional $description will be included in the reporter help
XML and display.  If supplied, $default indicates that the argument is
optional; the argValue method will return $default if the command line does not
include a value for the argument.  The optional $pattern specifies a pattern
for recognizing valid argument values; the default is '.*', which means that
any text is acceptable for the argument value.

=cut

#-----------------------------------------------------------------------------#
sub addArg {
  my ($self, $name, $description, $default, $pattern) = @_;
  carp "Missing name argument" and return if !defined($name);
  $self->{args}->{$name} = {
    description => $description,
    default => $default,
    pat => defined($pattern) ? $pattern : '.*'
  };
}

#-----------------------------------------------------------------------------#

=head2 addDependency($dependency [, ...])

Add one or more dependencies to the list of modules on which this reporter
depends.  Dependencies are reported as part of reporter help output to assist
reporter repository tools in their retrievals.  NOTE: dependencies on the
standard Inca reporter library modules are added by the modules themselves,
so a reporter only needs to invoke this method to report external dependencies.
The Inca Reporter Instance Manager presently only supports dependencies on
Inca repository packages, not packages from, e.g., CPAN.

=cut

#-----------------------------------------------------------------------------#
sub addDependency {
  my ($self, @dependencies) = @_;
  foreach my $dependency(@dependencies) {
    push(@{$self->{dependencies}}, $dependency);
  }
}

#-----------------------------------------------------------------------------#

=head2 argValue($name, $position)

Called after the processArgv method, this returns the value of the position'th
instance (starting with 1) of the $name command-line argument.  Returns the
value of the last instance if position is not supplied.  Returns undef if $name
is not a recognized argument.  Returns the default value for $name if it has
one and $name is included fewer than $position times on the command line.

=cut

#-----------------------------------------------------------------------------#
sub argValue {
  my ($self, $name, $position) = @_;
  carp "Missing name argument" and return undef if !defined($name);
  carp "'$name' is not a valid command line argument name" and return undef
    if !exists($self->{args}->{$name});
  my @argv = @{$self->{argv}};
  if(!defined($position)) {
    @argv = reverse(@argv);
    $position = 1;
  }
  foreach my $arg(@argv) {
    return $1 if $arg =~ /^$name=(.*)$/ && --$position < 1;
  }
  return $self->{args}->{$name}->{default};
}

#-----------------------------------------------------------------------------#

=head2 argValues($name)

Called after the processArgv method, this returns an array of all values
specified for the $name command-line argument.  Returns undef if $name is not a
recognized argument.  Returns a single-element array containing the default
value for $name if it has one and $name does not appear on the command line.

=cut

#-----------------------------------------------------------------------------#
sub argValues {
  my ($self, $name) = @_;
  carp "Missing name argument" and return undef if !defined($name);
  carp "'$name' is not a valid command line argument name" and return undef
    if !exists($self->{args}->{$name});
  my $default = $self->{args}->{$name}->{default};
  my @result;
  foreach my $arg(@{$self->{argv}}) {
    push(@result, $1) if $arg =~ /^$name=(.*)$/;
  }
  push(@result, $default) if $#result < 0 && defined($default);
  return @result;
}

#-----------------------------------------------------------------------------#

=head2 compiledProgramOutput(%params)

A convenience; compiles and runs a program, removes the source and exec files,
and returns the program's combined stderr/out output.  Recognized params:

=over 13

=item code

the code to compile; required

=item compiler

the compiler to invoke; defaults to cc

=item language

source file language--one of 'c', 'c++', 'fortran', or 'java'; defaults to 'c'.

=item out_switch

the switch to use to specify the compiler output file; defaults to '-o '

=item switches

additional switches to pass to the compiler; defaults to ''

=item timeout

max seconds compilation/execution may take; sets $? to a non-zero value and
returns any partial program output on time-out

=back

=cut

#-----------------------------------------------------------------------------#
sub compiledProgramOutput {
  my ($self, %params) = @_;
  my $code = $params{code};
  my $compiler = defined($params{compiler}) ? $params{compiler} : 'cc';
  my $lang = defined($params{language}) ? $params{language} : 'c';
  my $extension = $lang eq 'c++' ? 'C' : $lang eq 'fortran' ? 'f' :
                  $lang eq 'java' ? 'java' : 'c';
  my $prefix = "src$$";
  my $timeout = $params{timeout};
  if($lang eq 'java') {
    $code =~ s/class\s+\w+/class $prefix/;
    $ENV{CLASSPATH} .= ':' if defined $ENV{CLASSPATH};
    $ENV{CLASSPATH} .= '.';
  }
  my $path = "$prefix.$extension";
  return if !open(OUTPUT, ">$path");
  print OUTPUT $code;
  close OUTPUT;
  my $out = defined($params{out_switch}) ? $params{out_switch} : '-o ';
  my $switches = defined($params{switches}) ? $params{switches} : '';
  my $cmd =
    $lang eq 'java' ? "($compiler $path $switches && java $prefix)" :
                      "($compiler $path $out$prefix $switches && ./$prefix)";
  # Allow for dynamic libraries in any link directories.
  my $oldLd = $ENV{LD_LIBRARY_PATH};
  if($switches =~ /-L\s*\S+/) {
    my @paths = $switches =~ /-L\s*(\S+)/g;
    $ENV{LD_LIBRARY_PATH} = join(':', @paths);
    $ENV{LD_LIBRARY_PATH} .= ":$oldLd" if defined($oldLd);
  }
  my $output = $self->loggedCommand($cmd, $timeout);
  delete $ENV{LD_LIBRARY_PATH};
  $ENV{LD_LIBRARY_PATH} = $oldLd if defined($oldLd);
  $self->loggedCommand("/bin/rm -f $prefix*");
  return $output;
}

#-----------------------------------------------------------------------------#

=head2 failPrintAndExit($msg)

A convenience; calls setResult(0, $msg) and print( ) before exiting the
reporter.

=cut

#-----------------------------------------------------------------------------#
sub failPrintAndExit {
  my ($self, $msg) = @_;
  $self->setResult(0, $msg);
  $self->print();
  exit;
}

#-----------------------------------------------------------------------------#

=head2 getBody( )

Returns the body of the report.

=cut

#-----------------------------------------------------------------------------#
sub getBody {
  my $self = shift;
  return $self->{body};
}

#-----------------------------------------------------------------------------#

=head2 getCompleted( )

Returns the completion indicator of the reporter.

=cut

#-----------------------------------------------------------------------------#
sub getCompleted {
  my $self = shift;
  return $self->{completed};
}

#-----------------------------------------------------------------------------#

=head2 getCwd( )

Returns the initial working directory of the reporter.

=cut

#-----------------------------------------------------------------------------#
sub getCwd {
  my $self = shift;
  return $self->{cwd};
}

#-----------------------------------------------------------------------------#

=head2 getDescription( )

Returns the description of the reporter.

=cut

#-----------------------------------------------------------------------------#
sub getDescription {
  my $self = shift;
  return $self->{description};
}

#-----------------------------------------------------------------------------#

=head2 getFailMessage( )

Returns the failure message of the reporter.

=cut

#-----------------------------------------------------------------------------#
sub getFailMessage {
  my $self = shift;
  return $self->{fail_message};
}

#-----------------------------------------------------------------------------#

=head2 getName( )

Returns the name that identifies this reporter.

=cut

#-----------------------------------------------------------------------------#
sub getName {
  my $self = shift;
  return $self->{name};
}

#-----------------------------------------------------------------------------#

=head2 getUrl( )

Returns the url which describes the reporter in more detail.

=cut

#-----------------------------------------------------------------------------#
sub getUrl {
  my $self = shift;
  return $self->{url};
}

#-----------------------------------------------------------------------------#

=head2 getVersion( )

Returns the version of the reporter.

=cut

#-----------------------------------------------------------------------------#
sub getVersion {
  my $self = shift;
  return $self->{version};
}

#-----------------------------------------------------------------------------#

=head2 log($type, @msgs)

Appends each element of @msgs to the list of $type log messages stored in the
reporter. $type must be one of 'debug', 'error', 'info', 'system', or 'warn'.

=cut

#-----------------------------------------------------------------------------#
sub log {
  my ($self, $type, @msgs) = @_;
  return if $self->{log_pat} !~ /$type/;
  foreach my $msg(@msgs) {
    print STDERR "$type: $msg\n" if $self->argValue('verbose') eq '0';
    push(@{$self->{log_entries}}, {type => $type, time => time(), msg => $msg});
  }
}

#-----------------------------------------------------------------------------#

=head2 loggedCommand($cmd, $timeout)

A convenience; appends $cmd to the 'system'-type log messages stored in the
reporter, then returns `$cmd 2>&1`.  If $timeout is specified and the command
doesn't complete within $timeout seconds, aborts the execution of $cmd, sets $!
to POSIX::EINTR and $? to a non-zero value, and returns any partial output.

=cut

#-----------------------------------------------------------------------------#
sub loggedCommand {
  my ($self, $cmd, $timeout) = @_;
  $self->log('system', $cmd);
  return `$cmd 2>&1` if !defined $timeout;
  # fork a child to run the command, sending stderr/out through a pipe.  Set
  # the pgrp of the child so that we can kill it and any processes it spawns.
  pipe(READHANDLE, WRITEHANDLE);
  my $childPid = fork();
  if($childPid == 0) {
    close(READHANDLE);
    open(STDERR, ">&WRITEHANDLE");
    open(STDOUT, ">&WRITEHANDLE");
    setpgrp(0, 0);
    exec($cmd) or die "exec failed $!";
  }
  close(WRITEHANDLE);
  my $timedOut = 0;
  # Install a local alarm handler to interrupt reading the pipe and set a flag.
  local $SIG{ALRM} = sub {$timedOut = 1; die;};
  my $output = '';
  alarm $timeout;
  eval {
    $output .= $_ while $_ = <READHANDLE>;
  };
  alarm 0;
  kill(-9, $childPid) if $timedOut; # Kill the process group $childPid
  waitpid($childPid, 0);
  if($timedOut) {
    $? = 1;
    $! = EINTR;
  }
  close(READHANDLE);
  return $output;
}

#-----------------------------------------------------------------------------#

=head2 loggedCommandWithRetries($cmd, $timeout, $numtries, $sleepwait 
                                [,$exitcode[, $regex]] )

A convenience function for commands suseptible to transient failures; executes 
loggedCommand again after $sleepwait seconds if $cmd fails or times out until
$numtries is exceeded.  By default, assumes $cmd fails if the exit code is
non-zero.  This can be overriden by either setting an optional command
$exitcode to another value or setting $exitcode to undef indicating it should
be ignored.  An optional $regex can be specified as an additional criteria for
success to search for in the output.  If $timeout is specified and the command
doesn't complete within $timeout seconds, aborts the execution of $cmd, sets
$! to POSIX::EINTR and $? to a non-zero value, and returns any partial output.

=cut

#-----------------------------------------------------------------------------#
sub loggedCommandWithRetries {
  my ($self, $cmd, $timeout, $numtries, $sleepwait, $exitcode, $regex) = @_;

  # error checking
  if( ! defined $numtries || ! defined $sleepwait ) {
    return $self->loggedCommand($cmd, $timeout);
  }
  # check for optional exitcode 
  if ( scalar(@_) <=5 ) {
    $exitcode = 0;
  } 

  my $cmdSucceeded = 0;
  my $output;
  for ( my $i = 0; $i < $numtries && ! $cmdSucceeded; $i++ ) {
    $output = $self->loggedCommand($cmd, $timeout);
    my $interrupted = ($? == 1 && $! == EINTR) ? 1 : 0;
    my $cmdexitcode = $? >> 8;
    if ( ! $interrupted &&
         (! defined $exitcode || (defined $exitcode && $cmdexitcode==$exitcode)) &&
         (! defined $regex || (defined $output && $output =~ /$regex/)) ) {
      $cmdSucceeded = 1 
    } else {
      sleep( $sleepwait );
    }
  }
  return $output;

}

#-----------------------------------------------------------------------------#

=head2 print($verbose)

A convenience; prints report($verbose) to stdout.

=cut

#-----------------------------------------------------------------------------#
sub print {
  my ($self, $verbose) = @_;
  print $self->report($verbose) . "\n";
}

#-----------------------------------------------------------------------------#

=head2 processArgv(@ARGV)

Processes @ARGV which is a list of command-line arguments of the form:

-name1=value1 -name2=value2 ...

The following options are predefined:

=over 5

=item help

=over 5

=item yes

Prints help information describing the reporter inputs, then forces the
reporter to exit.  If the verbose level is 0, the output will be text;
otherwise, it will be Inca Report XML.

=item no (default)

Normal reporter execution.

=back

=item log

=over 5

=item 0 (default)

log no messages

=item 1

log error messages

=item 2

log error and warning messages

=item 3

log error, warning, and system messages

=item 4

log error, warning, system, and info messages

=item 5

log error, warning, system, info, and debug messages

=item debug

log only debug messages

=item error

log only error messages

=item info

log only info messages

=item system

log only system messages

=item warn

log only warning messages

=back

=item verbose

=over 5

=item 0

print will only produce "completed" or "failed".

=item 1 (default)

print will produce Inca Report XML.

=item 2

print will produce Inca Report XML that includes help information.

=back

=item version

=over 5

=item yes

Prints the reporter version number and exits.

=item no (default)

Normal reporter execution.

=back

=back

=cut

#-----------------------------------------------------------------------------#
sub processArgv {
  my ($self, @argv) = @_;

  my ($arg, @argValues, @missing, @patterns);

  if($#argv == 0) {
    # we have a single argument; check to see if the input is URL-style query
    # string, e.g.,  -file=test.pl&help=no&verbose=1
    @argv = split(/&/, $argv[0]) if $argv[0] =~ /&/;
  } elsif($#argv == -1) {
    # maybe we're running as a CGI script
    @argv = split(/&/, $ENV{'QUERY_STRING'}) if defined $ENV{'QUERY_STRING'};
  }

  my $badArg;
  foreach $arg(@argv) {
    my ($name, $value) = split(/=/, $arg, 2);
    $value = 'yes' if !defined($value);
    $name =~ s/^--?//;
    if(!exists($self->{args}->{$name})) {
      $badArg = "unknown argument '$name'";
    } elsif($value !~ /$self->{args}->{$name}->{pat}/) {
      $badArg = "'$value' is not a valid value for -$name";
    }
    push(@argValues, "$name=$value");
  }
  $self->{argv} = \@argValues;
  $self->failPrintAndExit($badArg) if defined($badArg);

  if($self->argValue('help') ne 'no') {
    if($self->argValue('verbose') eq '0') {
      my $description = $self->getDescription();
      my $version = $self->getVersion();
      my $url = $self->getUrl();
      my $text = "";
      my $usage = basename($0);
      foreach $arg(sort(keys %{$self->{args}})) {
        my $argDefault = $self->{args}->{$arg}->{default};
        my $argDescription = $self->{args}->{$arg}->{description};
        $text .= "  -$arg\n";
        $text .= "\t$argDescription\n" if defined($argDescription);
        $usage .= " -$arg" . (defined($argDefault) ? "=$argDefault" : "");
      }
      print "NAME:\n  " . $self->getName() . "\n" .
            "VERSION:\n  " .
              (defined($version) ? $version : "No version") . "\n" .
            "DESCRIPTION:\n  " .
              (defined($description) ? $description : "No description") . "\n" .
            "URL:\n  " .
              (defined($url) ? $url : "No URL") . "\n" .
            "SYNOPSIS:\n  $usage\n$text\n";
    } else {
      print $self->_reportXml($self->_helpXml()) . "\n";
    }
    exit;
  }
  if($self->argValue('version') ne 'no') {
    print $self->getName() . ' ' . $self->getVersion() . "\n";
    exit;
  }

  foreach $arg(keys %{$self->{args}}) {
    push(@missing, $arg) if !defined($self->argValue($arg));
  }
  if($#missing == 0) {
    $self->failPrintAndExit("Missing required argument '$missing[0]'");
  } elsif($#missing > 0) {
    $self->failPrintAndExit
      ("Missing required arguments '" . join("', '", @missing) . "'")
  }

  foreach $arg($self->argValues('log')) {
    if($arg =~ /^[012345]$/) {
      my @allTypes = ('', 'error', 'warn', 'system', 'info', 'debug');
      $arg = join('|', @allTypes[1 .. $arg]);
    }
    push(@patterns, $arg);
  }
  $self->{log_pat} = '^(' . join('|', @patterns) . ')$' if $#patterns >= 0;

}

#-----------------------------------------------------------------------------#

=head2 report($verbose)

Returns report text or XML, depending on the value (0, 1, 2) of $verbose.  Uses
the value of the -verbose switch if $verbose is undef.

=cut

#-----------------------------------------------------------------------------#
sub report {
  my ($self, $verbose) = @_;
  my $result;
  my $completed = $self->getCompleted();
  my $msg = $self->getFailMessage();
  $verbose = $self->argValue('verbose') if !defined($verbose);
  if($verbose == '0') {
    $result = $completed ? 'completed' : 'failed';
    $result .= ": $msg" if defined($msg);
  } else {
    my ($completedXml, $messageXml);
    $self->setBody($self->reportBody())
      if $completed && !defined($self->getBody());
    $messageXml = $self->xmlElement('errorMessage', 1, $msg) if defined($msg);
    $completedXml =
      $self->xmlElement('completed', 1, $completed ? 'true' : 'false');
    $result = $self->_reportXml(
      $self->xmlElement('body', 0, $self->getBody()),
      $self->xmlElement('exitStatus', 0, $completedXml, $messageXml),
      $verbose == '2' ? $self->_helpXml() : undef
    );
  }
  return $result;
}

#-----------------------------------------------------------------------------#

=head2 reportBody( )

Constructs and returns the XML contents of the report body.  Child classes
should override the default implementation, which returns undef.

=cut

#-----------------------------------------------------------------------------#
sub reportBody {
  return undef;
}

#-----------------------------------------------------------------------------#

=head2 setBody($body)

Sets the body of the report to $body.

=cut

#-----------------------------------------------------------------------------#
sub setBody {
  my $self = shift;
  $self->{body} = shift;
}

#-----------------------------------------------------------------------------#

=head2 setCompleted($completed)

Sets the completion indicator of the reporter to $completed.

=cut

#-----------------------------------------------------------------------------#
sub setCompleted {
  my $self = shift;
  $self->{completed} = shift;
}

#-----------------------------------------------------------------------------#

=head2 setCwd($cwd)

Sets the initial working directory of the reporter to $cwd.

=cut

#-----------------------------------------------------------------------------#
sub setCwd {
  my $self = shift;
  $self->{cwd} = shift;
}

#-----------------------------------------------------------------------------#

=head2 setDescription($description)

Sets the description of the reporter to $description.

=cut

#-----------------------------------------------------------------------------#
sub setDescription {
  my $self = shift;
  $self->{description} = shift;
}

#-----------------------------------------------------------------------------#

=head2 setFailMessage($msg)

Sets the failure message of the reporter to $msg.

=cut

#-----------------------------------------------------------------------------#
sub setFailMessage {
  my $self = shift;
  $self->{fail_message} = shift;
}

#-----------------------------------------------------------------------------#

=head2 setName($name)

Sets the name that identifies this reporter to $name.

=cut

#-----------------------------------------------------------------------------#
sub setName {
  my $self = shift;
  $self->{name} = shift;
}

#-----------------------------------------------------------------------------#

=head2 setResult($completed, $msg)

A convenience; calls setCompleted($completed) and setFailMessage($msg).

=cut

#-----------------------------------------------------------------------------#
sub setResult {
  my ($self, $completed, $msg) = @_;
  $self->setCompleted($completed);
  $self->setFailMessage($msg);
}

#-----------------------------------------------------------------------------#

=head2 setUrl($url)

Sets the url for the reporter to $url.

=cut

#-----------------------------------------------------------------------------#
sub setUrl {
  my $self = shift;
  $self->{url} = shift;
}

#-----------------------------------------------------------------------------#

=head2 setVersion($version)

Sets the version of the reporter to $version.  Recognizes and parses CVS
revision strings.

=cut

#-----------------------------------------------------------------------------#
sub setVersion {
  my ($self, $version) = @_;
  carp 'Undefined version not allowed' and return if !defined($version);
  $self->{version} = $version =~ 'Revision: (.+) ' ? $1 : $version;
}

#-----------------------------------------------------------------------------#

=head2 tempFile(@paths)

A convenience.  Adds each element of @paths to a list of temporary files that
will be deleted automatically when the reporter is destroyed.

=cut

#-----------------------------------------------------------------------------#
sub tempFile {
  my $self = shift;
  push(@{$self->{temp_paths}}, @_);
}

#-----------------------------------------------------------------------------#

=head2 xmlElement($name, $escape, [$attrs,] @contents)

Returns the XML element $name surrounding @contents.  $escape should be true
only for leaf elements; in this case, each special XML character (<>&) in
@contents is replaced by the equivalent XML entity.  $attrs is an optional
element specifying element attributes.

=cut

#-----------------------------------------------------------------------------#
sub xmlElement {
  my ($self, $name, $escape, @contents) = @_;
  my $innards = '';
  my $attrs;
  if ( scalar(@contents) > 0 && ref($contents[0]) eq "HASH" ) {
    $attrs = shift @contents;
  }
  foreach my $content(@contents) {
    next if !defined($content);
    if($escape) {
      $content =~ s/&/&amp;/g;
      $content =~ s/</&lt;/g;
      $content =~ s/>/&gt;/g;
    }
    $content =~ s/^</\n</;
    $innards .= $content;
  }
  $innards =~ s/^( *)</  $1</gm;
  $innards =~ s/>$/>\n/;
  my $attrString = "";
  while ( my ($attrName, $attrValue) = each(%{$attrs}) ) {
    $attrString .= " $attrName=\"$attrValue\"";
  }
  if ( $innards eq '' ) {
    return "<$name" . $attrString . "/>";
  } else {
    return "<$name" . $attrString . ">$innards</$name>";
  }
}

#-----------------------------------------------------------------------------#
# Private methods (not documented with pod markers and prefixed with '_' )
#-----------------------------------------------------------------------------#

#-----------------------------------------------------------------------------#
# _helpXml
#
# Returns help information formatted as the body of an Inca report.
#-----------------------------------------------------------------------------#
sub _helpXml {
  my $self = shift;
  my (@argsXml, $arg, @depsXml, $dep);
  foreach $arg(sort(keys %{$self->{args}})) {
    my $info = $self->{args}->{$arg};
    my $defaultXml;
    $defaultXml = $self->xmlElement('default', 1, $info->{default})
      if defined $info->{default};
    my $description = defined($info->{description}) ? $info->{description} : '';
    push(@argsXml,
         $self->xmlElement('argDescription', 0,
           $self->xmlElement('ID', 1, $arg),
           $self->xmlElement('accepted', 1, $info->{pat}),
           $self->xmlElement('description', 1, $description),
           $defaultXml
         )
      );
  }
  foreach $dep(@{$self->{dependencies}}) {
    push(@depsXml,
         $self->xmlElement('dependency', 0, $self->xmlElement('ID', 1, $dep))
        );
  }
  return $self->xmlElement('help', 0,
    $self->xmlElement('ID', 1, 'help'),
    $self->xmlElement('name', 1, $self->getName()),
    $self->xmlElement('version', 1, $self->getVersion()),
    $self->xmlElement('description', 1, $self->getDescription()),
    $self->xmlElement('url', 1, $self->getUrl()),
    @argsXml,
    @depsXml
  );
}

#-----------------------------------------------------------------------------#
# _iso8601Time($time)
#
# Returns the UTC time for the time() return value $time in ISO 8601 format:
# CCMM-MM-DDTHH:MM:SSZ
#-----------------------------------------------------------------------------#
sub _iso8601Time {
  my ($self, $time) = @_;
  my ($sec, $min, $hour, $mday, $mon, $year, $wday, $yday) = gmtime($time);
  return sprintf("%04d-%02d-%02dT%02d:%02d:%02dZ",
                 $year + 1900, $mon + 1, $mday, $hour, $min, $sec);
}

#-----------------------------------------------------------------------------#
# _reportXml($content1, $content2, ...)
#
# Returns XML report beginning with the header and input sections plus any
# contents specified in the arguments.
#-----------------------------------------------------------------------------#
sub _reportXml {
  my ($self, @contents) = @_;
  my $argsXml;
  my @argXmls;
  my $logXml;
  my @logXmls;

  my $hostname = hostname();
  if ( $hostname !~ /\./ ) {
    my $sys_hostname = `hostname -f 2> /dev/null`;
    if ( $? == 0 ) {
      chomp $sys_hostname;
      $hostname = $sys_hostname;
    }
    if ( $hostname !~ /\./ ) {
      # See if we can pick up the FQDN from Net::Domain
      eval {
        require Net::Domain;
        $hostname = Net::Domain::hostfqdn();
      };
    }
  }
  my @header = (
    $self->xmlElement('gmt', 1, $self->_iso8601Time(time())),
    $self->xmlElement('hostname', 1, $hostname ),
    $self->xmlElement('name', 1, $self->getName()),
    $self->xmlElement('version', 1, $self->getVersion()),
    $self->xmlElement('workingDir', 1, $self->getCwd()),
    $self->xmlElement('reporterPath', 1, $0)
  );
  foreach my $arg(sort(keys %{$self->{args}})) {
    foreach my $value($self->argValues($arg)) {
      push(@argXmls,
           $self->xmlElement('arg', 0,
             $self->xmlElement('name', 1, $arg),
             $self->xmlElement('value', 1, $value)
           )
      );
    }
  }
  $argsXml = $self->xmlElement('args', 0, @argXmls) if $#argXmls >= 0;
  foreach my $entry(@{$self->{log_entries}}) {
    push(@logXmls,
         $self->xmlElement($entry->{type}, 0,
           $self->xmlElement('gmt', 1, $self->_iso8601Time($entry->{time})),
           $self->xmlElement('message', 1, $entry->{msg})
         )
        );
  }
  $logXml = $self->xmlElement('log', 0, @logXmls) if $#logXmls >= 0;
  my $result = $self->xmlElement('rep:report', 0,
    @header,
    $argsXml,
    $logXml,
    @contents
  );
  $result =~ s#<rep:report#<rep:report xmlns:rep='http://inca.sdsc.edu/dataModel/report_2.1'#;
  return "<?xml version='1.0'?>\n" . $result;
}

sub DESTROY {
  my $self = shift;
  my $files = join(' ', @{$self->{temp_paths}});
  `/bin/rm -fr $files` if $#{$self->{temp_paths}} >= 0;
}

1;

__END__

=head1 EXAMPLE

  use strict;
  use warnings;
  use Inca::Reporter;
  my $reporter = new Inca::Reporter(
    name => 'selfPathReporter',
    version => '1.0.0',
    description => 'Reports the path to the reporter',
    url => 'http://mothership.ufo.edu'
  );
  $reporter->processArgv(@ARGV);
  $reporter->setBody($reporter->xmlElement('path', 1, $0));
  $reporter->print();

Running this reporter with -verbose=1 will produce output that looks something
like this.

  <?xml version='1.0'?>
  <rep:report xmlns:rep='http://inca.sdsc.edu/dataModel/report_2.1'>
    <gmt>Tue Jan  7 07:55:44 2005</gmt>
    <hostname>blue.ufo.edu</hostname>
    <name>selfPathReporter</name>
    <version>1.0.0</version>
    <workingDir>/home/inca</workingDir>
    <reporterPath>reporters/bin/selfPathReporter</reporterPath>
    <args>
      <arg>
        <name>help</name>
        <value>no</value>
      </arg>
      <arg>
        <name>log</name>
        <value></value>
      </arg>
      <arg>
        <name>verbose</name>
        <value>1</value>
      </arg>
      <arg>
        <name>version</name>
        <value>no</value>
      </arg>
    </args>
    <body>
      <path>sr</body>
    </body>
    <exitStatus>
      <completed>true</completed>
    </exitStatus>
  </rep:report>

=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=cut
