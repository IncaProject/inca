package Inca::Reporter::Version;
use Inca::Reporter;
@ISA = ('Inca::Reporter');

################################################################################

=head1 NAME

Inca::Reporter::Version - Convenience module for creating version reporters

=head1 SYNOPSIS

  use Inca::Reporter::Version;
  my $reporter = new Inca::Reporter::Version();
  my $command = 'somecommand -version';
  my $pattern = '^version "(.*)"';
  ...
  $reporter->setPackageName('packageX');
  $reporter->setVersionByExecutable($command, $pattern);
  $reporter->print();

or

  $reporter->setVersionByGptQuery('packageX');

or

  $reporter->setVersionByRpmQuery('packageX');

or

  $reporter->setPackageVersion('x.x.x');

or

  foreach my $subpackage(@subpackages) {
    $reporter->setSubpackageVersion($subpackage, $version);
  }

=head1 DESCRIPTION

This module is a subclass of Inca::Reporter which provides convenience methods
for creating version reporters.  A version reporter reports the version
information for a package in the following schema (i.e., this is the body of
the Inca report):

  <packageVersion>
    <ID>packageX</ID>
    <version>x.x.x</version>
  </packageVersion>

or

  <packageVersion>
    <ID>packageX</ID>
    <subpackage>
      <ID>subpackageX</ID>
      <version>x.x.x</version>
    </subpackage>
    <subpackage>
      <ID>subpackageY</ID>
      <version>x.x.x</version>
    </subpackage>
  </packageVersion>

Version information can be set using one of the basic methods setPackageVersion
(for the first example) or setSubpackageVersion (for the second). In this case,
the user retrieves a package's version information directly and uses one of
these two methods to report it.  This module also provides convenience methods
that retrieve a package version using conventional methods of querying version
information.

=cut

################################################################################

use strict;
use warnings;
use Carp;

#=============================================================================#

=head1 CLASS METHODS

=cut

#=============================================================================#

#-----------------------------------------------------------------------------#

=head2 new

Class constructor which returns a new Inca::Reporter::Version object.  The
constructor supports the following parameters in addition to those supported by
L<Inca::Reporter>.

=over 13

=item package_name

the name of the package for which a version is being determined; default ''.

=item package_version

the version of the package.

=back

=cut

#-----------------------------------------------------------------------------#
sub new {
  my ($this, %attrs) = @_;
  my $class = ref($this) || $this;
  my $name = defined($attrs{package_name}) ? $attrs{package_name} : '';
  my $version = $attrs{package_version};
  delete $attrs{package_name};
  delete $attrs{package_version};
  my $self = $class->SUPER::new(%attrs);
  $self->{package_name} = $name;
  $self->{package_version} = $version;
  $self->{subpackage_versions} = {};
  $self->addDependency(__PACKAGE__);
  bless ($self, $class);
  return $self;
}

#-----------------------------------------------------------------------------#

=head2 getPackageName

Returns the name of the package

=cut

#-----------------------------------------------------------------------------#
sub getPackageName {
  my $self = shift;
  return $self->{package_name};
}

#-----------------------------------------------------------------------------#

=head2 getPackageVersion

Returns the version of the package

=cut

#-----------------------------------------------------------------------------#
sub getPackageVersion {
  my $self = shift;
  return $self->{package_version};
}

#-----------------------------------------------------------------------------#

=head2 getSubpackageNames

Returns an array of all the names of all subpackages with a set version

=cut

#-----------------------------------------------------------------------------#
sub getSubpackageNames {
  my $self = shift;
  return keys %{$self->{subpackage_versions}};
}

#-----------------------------------------------------------------------------#

=head2 getSubpackageVersion($name)

Returns the version of subpackage $name

=cut

#-----------------------------------------------------------------------------#
sub getSubpackageVersion {
  my ($self, $name) = @_;
  return $self->{subpackage_versions}->{$name};
}

#-----------------------------------------------------------------------------#

=head2 reportBody

Constructs and returns the body of the reporter.

=cut

#-----------------------------------------------------------------------------#
sub reportBody {
  my $self = shift;
  my @packageXml;
  push(@packageXml, $self->xmlElement('ID', 1, $self->getPackageName()));
  if($self->getCompleted()) {
    if(defined($self->getPackageVersion())) {
      push(@packageXml,
           $self->xmlElement('version', 1, $self->getPackageVersion()));
    }
    foreach my $subpackage(sort $self->getSubpackageNames()) {
      push(@packageXml,
           $self->xmlElement('subpackage', 0,
             $self->xmlElement('ID', 1, $subpackage),
             $self->xmlElement
               ('version', 1, $self->getSubpackageVersion($subpackage))
           )
      );
    }
  }
  return $self->xmlElement('package', 0, @packageXml);
}

#-----------------------------------------------------------------------------#

=head2 setPackageName($packageName)

Set the name of the package.

=cut

#-----------------------------------------------------------------------------#
sub setPackageName {
  my ($self, $name) = @_;
  carp "Missing name argument" and return if !defined($name);
  $self->{package_name} = $name;
}

#-----------------------------------------------------------------------------#

=head2 setPackageVersion($version)

Report the version of a package as $version.

=cut

#-----------------------------------------------------------------------------#
sub setPackageVersion {
  my $self = shift;
  $self->{package_version} = shift;
  $self->setCompleted(1);
}

#-----------------------------------------------------------------------------#

=head2 setSubpackageVersion($name, $version)

Report the version of subpackage $name as $version.

=cut

#-----------------------------------------------------------------------------#
sub setSubpackageVersion {
  my ($self, $name, $version) = @_;
  $self->{subpackage_versions}->{$name} = $version;
  $self->setCompleted(1);
}

#-----------------------------------------------------------------------------#

=head2 setVersionByCompiledProgramOutput(%attrs)

Retrieve the package version by compiling and running a program and matching
its output against a pattern.  Returns 1 if successful, else 0.  The function
recognizes the following parameter in addition to those supported by the
compiledProgramOutput method of L<Inca::Reporter>:

=over 13

=item pattern

pattern to search for in program output; default '(.+)'

=back

=cut

#-----------------------------------------------------------------------------#
sub setVersionByCompiledProgramOutput {
  my ($self, %attrs) = @_;
  my $failMessage;
  my $pattern = defined($attrs{pattern}) ? $attrs{pattern} : '(.+)';
  delete $attrs{pattern};
  my $output = $self->compiledProgramOutput(%attrs);
  if(! $output) {
    $failMessage = 'program compilation/execution failed';
  } elsif($output !~ $pattern) {
    $failMessage = "'$pattern' not in '$output'";
  } else {
    $self->setPackageVersion($1);
  }
  $self->setResult(defined($failMessage) ? 0 : 1, $failMessage);
  return $self->getCompleted();
}

#-----------------------------------------------------------------------------#

=head2 setVersionByExecutable($command, $pattern, $timeout)

Retrieve package version information by executing $command and greping the
output for $pattern.  $command is the executable and argument string to
retrieve the version (e.g., command_name -version) and $pattern is a pattern
containing one grouping (i.e., memory parentheses) to retrieve the version
from the output.  $pattern defaults to '([\d\.]+)' if not specified.  Fails if
$timeout is specified and $command does not complete within $timeout seconds.
Returns 1 if successful, else 0.

=cut

#-----------------------------------------------------------------------------#
sub setVersionByExecutable {
  my ($self, $command, $pattern, $timeout) = @_;
  $pattern = '([\d\.]+)' if !defined($pattern);
  my $failMessage;
  my $output = $self->loggedCommand($command, $timeout);
  if(!defined($output) || $output =~ /not found/) {
    $failMessage = "'$command' failed: " . (defined($output) ? $output : $!);
  } else {
    my @matches = $output =~ /$pattern/;
    my $version;
    foreach my $match(@matches) {
      $version = $match and last if defined($match);
    }
    if(defined($version)) {
      $self->setPackageVersion($version);
    } else {
      $failMessage = "'$pattern' not in '$output'";
    }
  }
  $self->setResult(defined($failMessage) ? 0 : 1, $failMessage);
  return $self->getCompleted();
}

#-----------------------------------------------------------------------------#

=head2 setVersionByFileContents($path, $pattern)

Retrieve the package version by grep'ing the file $path for $pattern.  $pattern
defaults to '([\d\.]+)' if not specified.  Returns 1 if successful, else 0.

=cut

#-----------------------------------------------------------------------------#
sub setVersionByFileContents {
  my ($self, $path, $pattern) = @_;
  $pattern = '([\d\.]+)' if !defined $pattern;
  my ($failMessage, $line);
  if(! -e $path) {
    $failMessage = "file '$path' not present";
  } elsif(!open(VERSION, "<$path")) {
    $failMessage = "file '$path' not readable";
  } else {
    $line = <VERSION>;
    $line = <VERSION> while defined($line) && $line !~ /$pattern/;
    if(!defined($line)) {
      $failMessage = "'$pattern' not found in file '$path'";
    } else {
      $self->setPackageVersion($1);
    }
    close(VERSION);
  }
  $self->setResult(defined($failMessage) ? 0 : 1, $failMessage);
  return $self->getCompleted();
}

#-----------------------------------------------------------------------------#

=head2 setVersionByGptQuery(@prefixes)

Set subpackage version information by querying GPT for packages prefixed with
any element of @prefixes.  Returns 1 if successful, else 0.

=cut

#-----------------------------------------------------------------------------#
sub setVersionByGptQuery {
  my ($self, @prefixes) = @_;

  my $output = $self->loggedCommand('gpt-query');
  if(!defined($output)) {
    $self->setResult(0, 'gpt-query failed');
    return 0;
  }
  my $pat = '^\s*(' . join('|', @prefixes) . ')';
  my $lastDefined;
  foreach my $pkg(split(/\n/, $output)) {
    next if $pkg !~ /$pat/;
    my ($id, $version) = $pkg =~ /^\s*([^-]+).*version:\s+(.*)$/;
    next if !defined($version);
    $self->setSubpackageVersion($id, $version);
    $lastDefined = $id;
  }

  if(!defined($lastDefined)) {
    $self->setResult(0, "package not installed (no GPT packages located)");
  } else {
    $self->setResult(1);
  }
  return $self->getCompleted();

}

#-----------------------------------------------------------------------------#

=head2 setVersionByRpmQuery($pattern)

Set subpackage version information by querying GPT for packages that contain
the regular expression $pattern.  Returns 1 if successful, else 0.

=cut

#-----------------------------------------------------------------------------#
sub setVersionByRpmQuery {
  my ($self, $pattern) = @_;
  my $rpmCommand =
    "(rpm -qa --qf='%{NAME} version:%{VERSION}\\n' | grep '^[^ ]*$pattern')";
  my @rpms = split(/\n/, $self->loggedCommand("$rpmCommand"));
  if($#rpms < 0) {
    $self->setResult(0, "no rpm packages found for $pattern");
    return 0;
  }
  foreach my $rpm(@rpms) {
    my ($subpackage, $version) = split(/ version:/, $rpm);
    $self->setSubpackageVersion($subpackage, $version);
  }
  $self->setResult(1);
  return 1;
}

1;

__END__

=head1 EXAMPLES

The following example demonstrates the usage of setVersionByGptQuery:

  my $reporter = new Inca::Reporter::Version(
    name => 'grid.middleware.globus.version',
    version => 1.25,
    description => 'Reports globus package versions',
    url => 'http://www.globus.org',
    package_name => 'globus'
  );
  $reporter->processArgv(@ARGV);
  $reporter->setVersionByGptQuery('globus');
  $reporter->print();

The following example demonstrates the usage of setVersionByRpmQuery:

  my $reporter = new Inca::Reporter::Version(
    name => 'cluster.compiler.gcc.version',
    version => 1.25,
    description => 'Reports the version of gcc',
    url => 'http://gcc.gnu.org',
    package_name => 'gcc'
  );
  $reporter->processArgv(@ARGV);
  $reporter->setVersionByRpmQuery('gcc');
  $reporter->print();

The following example demonstrates the usage of setVersionByExecutable:

  my $command = "java -version";
  my $pattern = '^java version \"(.*)\"[.\n]*';
  my $reporter = new Inca::Reporter::Version(
    name => 'cluster.java.sun.version',
    version => 1.25,
    description => 'Reports the version of java in the user\'s path',
    url => 'http://java.sun.com',
    package_name => 'java'
  );
  $reporter->processArgv(@ARGV);
  $reporter->setVersionByExecutable($command, $pattern);
  $reporter->print();

=head1 AUTHOR

Shava Smallen <ssmallen@sdsc.edu>

=head1 SEE ALSO

L<Inca::Reporter>

=cut
