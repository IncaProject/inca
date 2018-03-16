use strict;
use warnings;
use Inca::Reporter::Version;
use Test::More 'no_plan';

my $package = 'myPack';
my $version = '1.7.3';

# empty constructor
my $reporter = new Inca::Reporter::Version();
ok(defined($reporter), 'new');
ok($reporter->getPackageName() eq '' &&
   !defined($reporter->getPackageVersion()) &&
   scalar($reporter->getSubpackageNames()) == 0, 'default values');

# get/set methods
$reporter->setPackageName($package);
$reporter->setPackageVersion($version);
ok($reporter->getPackageName() eq $package &&
   $reporter->getPackageVersion() eq $version, 'get/set');

# constructor attributes
$reporter = new Inca::Reporter::Version(
  package_name => $package,
  package_version => $version,
  completed => 1
);
ok($reporter->getPackageName() eq $package &&
   $reporter->getPackageVersion() eq $version, 'constructor attrs');

# reportBody, no subpackages
my $body = $reporter->reportBody();
ok($body =~ m#<ID>$package</ID>\s*<version>$version</version>#, 'package body');

# subpackages
$reporter = new Inca::Reporter::Version(
  package_name => $package,
  completed => 1
);
for(my $i = 0; $i < 5; $i++) {
  $reporter->setSubpackageVersion("sub$i", "$i");
}
ok(scalar($reporter->getSubpackageNames()) == 5 &&
   $reporter->getSubpackageVersion('sub3') eq '3', 'subpackage');

# reportBody, subpackages
$body = $reporter->reportBody();
my $allThere = 1;
for(my $i = 0; $i < 5; $i++) {
  $allThere = 0 if $body !~ m#<ID>sub$i</ID>\s*<version>$i</version>#;
}
ok($allThere, 'subpackage body');

# setVersionBy ... CompiledProgramOutput
my $pat = 'V([\d\.]+)';
$reporter = new Inca::Reporter::Version(package_name => $package);
my $cCode = '
#include <stdio.h>
int main(int argc, char **argv) {
  printf("V' . $version . '\n");
}
';
$reporter->setVersionByCompiledProgramOutput(code => $cCode, pattern => $pat);
ok($reporter->getPackageVersion() eq $version, 'ByCompiledProgramOutput');

# ... Executable
$reporter = new Inca::Reporter::Version(package_name => $package);
$reporter->setVersionByExecutable("/bin/sh -c 'echo V$version'", $pat);
ok($reporter->getPackageVersion() eq $version, 'ByExecutable');

# ... FileContents
$reporter = new Inca::Reporter::Version(package_name => $package);
my $path = "/tmp/vrtest$$";
SKIP: {
  skip "Unable to open $path", 1 if !open(OUT, ">$path");
  for(my $i = 0; $i < 200; $i++) {
    print OUT "$i\n";
    print OUT "  V$version XYZ\n" if $i == 100;
  }
  close(OUT);
  $reporter->setVersionByFileContents($path, $pat);
  unlink($path);
  ok($reporter->getPackageVersion() eq $version, 'ByFileContents');
}

# ... GptQuery
$reporter = new Inca::Reporter::Version(package_name => $package);
my $output = `gpt-query 2>&1`;
SKIP: {
  skip 'gpt-query unavailable', 1 if $? != 0 || $output !~ /version:/;
  my @packs = split(/\n/, $output);
  my $i;
  for($i = 0; $packs[$i] !~ /version:/; $i++) { }
  my ($p, $v) = $packs[$i] =~ /^\s*([^-]+).*version:\s+(.*)$/;
  $reporter->setVersionByGptQuery($p);
  ok($reporter->getSubpackageVersion($p) eq $v, 'ByGptQuery');
}

# ... RpmQuery
$reporter = new Inca::Reporter::Version(package_name => $package);
$output = `rpm -qa --qf='%{NAME} version:%{VERSION}\\n'`;
SKIP: {
  skip 'rpm query unavailable', 1 if $? != 0 || $output !~ / version:/;
  my @packs = split(/\n/, $output);
  my $i;
  for($i = 0; $packs[$i] !~ / version:\d+\.\d+/; $i++) { }
  my ($p, $v) = split(/ version:/, $packs[$i]);
  $reporter->setVersionByRpmQuery($p);
  ok($reporter->getSubpackageVersion($p) eq $v, 'ByRpmQuery');
}
