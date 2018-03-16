#!/usr/bin/perl

use strict;
use warnings;
use File::Basename 'basename';
use Inca::Reporter;
use Net::Domain 'hostfqdn';
use POSIX;
use Test::More 'no_plan';

my $body = '<stuff><ID>a</ID></stuff>';
my $description = 'documentation';
my $failMessage = 'Something went wrong';
my $name = basename($0);
my $url = 'www.blue.ufo.edu';
my $version = '0.5beta';

# new with no params
my $reporter = new Inca::Reporter();
ok(defined($reporter), 'new');
ok(!defined($reporter->getBody()) &&
   !defined($reporter->getDescription()) &&
   !defined($reporter->getFailMessage()) &&
   !$reporter->getCompleted() &&
   !defined($reporter->getUrl()) &&
   $reporter->getVersion() eq '0', 'default values');

# get/set methods
$reporter->setBody($body);
$reporter->setDescription($description);
$reporter->setFailMessage($failMessage);
$reporter->setCompleted(1);
$reporter->setUrl($url);
$reporter->setVersion($version);
ok($reporter->getBody() eq $body &&
   $reporter->getDescription() eq $description &&
   $reporter->getFailMessage() eq $failMessage &&
   $reporter->getCompleted() &&
   $reporter->getUrl() eq $url &&
   $reporter->getVersion() eq $version, 'set/get');

# set to undef; name, version should reject
$reporter->setBody(undef);
$reporter->setDescription(undef);
$reporter->setFailMessage(undef);
$reporter->setCompleted(undef);
$reporter->setUrl(undef);
$reporter->setVersion(undef);
ok(!defined($reporter->getBody()) &&
   !defined($reporter->getDescription()) &&
   !defined($reporter->getFailMessage()) &&
   !$reporter->getCompleted() &&
   !defined($reporter->getUrl()) &&
   $reporter->getVersion() eq $version, 'set undef');

# setResult convenience method
$reporter->setResult(1, $failMessage);
ok($reporter->getCompleted() &&
   $reporter->getFailMessage() eq $failMessage, 'setResult');

# new with params
$reporter = new Inca::Reporter(
  body => $body,
  completed => 1,
  description => $description,
  fail_message => $failMessage,
  url => $url,
  version => $version
);
ok($reporter->getBody() eq $body &&
   $reporter->getDescription() eq $description &&
   $reporter->getFailMessage() eq $failMessage &&
   $reporter->getCompleted() &&
   $reporter->getUrl() eq $url &&
   $reporter->getVersion() eq $version, 'new w/params');

# report XML
$reporter->setFailMessage(undef);
my $host = hostfqdn();
is($reporter->report(0), 'completed', 'verbose=0 output');
my $report = $reporter->report(1);
ok($report =~ m#<body>\s*$body\s*</body># &&
   $report =~ m#<completed>true</completed># &&
   $report =~ m#<hostname>$host</hostname># &&
   $report =~ m#<name>\s*$name\s*</name># &&
   $report =~ m#<version>$version</version># &&
   1, 'verbose=1 output');

# argv parsing
my @argv = ();
$reporter->addArg('hasdef', 'has default', 'def');
$reporter->addArg('hasdef2', 'also has default', 'def2');
$reporter->processArgv(@argv);
ok($reporter->argValue('hasdef') eq 'def' &&
   $reporter->argValue('hasdef2') eq 'def2' &&
   $reporter->argValue('help') eq 'no' &&
   $reporter->argValue('version') eq 'no' &&
   $reporter->argValue('verbose') eq '1', 'default arguments');
$reporter->addArg('nodef', 'no default');
@argv = ('nodef=1', 'hasdef=2', 'hasdef=3', 'nodef=4', 'hasdef=5', 'verbose=1');
$reporter->processArgv(@argv);
ok($reporter->argValue('hasdef') eq '5' &&
   $reporter->argValue('help') eq 'no' &&
   $reporter->argValue('nodef') eq '4' &&
   $reporter->argValue('version') eq 'no' &&
   $reporter->argValue('verbose') eq '1', 'specified arguments');
ok($reporter->argValue('hasdef', 1) eq '2' &&
   $reporter->argValue('hasdef', 2) eq '3' &&
   $reporter->argValue('hasdef', 3) eq '5' &&
   $reporter->argValue('hasdef', 4) eq 'def' &&
   $reporter->argValue('hasdef', 5) eq 'def' &&
   $reporter->argValue('hasdef2', 1) eq 'def2' &&
   $reporter->argValue('nodef', 1) eq '1' &&
   $reporter->argValue('nodef', 2) eq '4' &&
   !defined($reporter->argValue('nodef', 3)), 'positioned arguments');
my @hasdefVals = $reporter->argValues('hasdef');
my @hasdef2Vals = $reporter->argValues('hasdef2');
my @nodefVals = $reporter->argValues('nodef');
ok($#hasdefVals == 2 && $#hasdef2Vals == 0 && $#nodefVals == 1 &&
   $hasdefVals[0] eq '2' && $hasdefVals[1] eq '3' && $hasdefVals[2] eq '5' &&
   $hasdef2Vals[0] eq 'def2' &&
   $nodefVals[0] eq '1' && $nodefVals[1] eq '4', 'argValues');

# compiledProgramOutput
my $str = 'Test output';
my $cProg = '
#include <stdio.h>
int main(int argc, char **argv) {
  printf("' . $str . '\n");
  return 0;
}
';
my $output = $reporter->compiledProgramOutput(code => $cProg);
is($output, "$str\n", 'compiledProgramOutput');

# xmlElement
my $xml = $reporter->xmlElement('tag');
ok($xml eq '<tag/>' || $xml eq '<tag></tag>', 'empty xmlElement');
$xml = $reporter->xmlElement('tag', 0, 'ABC<>&<>&ABC');
is($xml, "<tag>ABC<>&<>&ABC</tag>", 'unescaped xmlElement');
$xml = $reporter->xmlElement('tag', 1, 'ABC<>&<>&ABC');
is($xml, "<tag>ABC&lt;&gt;&amp;&lt;&gt;&amp;ABC</tag>", 'escaped xmlElement');

# loggedCommand
$output = $reporter->loggedCommand('sleep 5 && echo hello');
is($output, "hello\n", 'untimed loggedCommand');
$output = $reporter->loggedCommand('sleep 5 && echo hello', 2);
my $errno = $! - 0;
ok(defined($output), 'timed loggedCommand output');
ok($? != 0, 'timed loggedCommand exit code');
$output = $reporter->loggedCommand('echo hello && echo hello', 20);
is("hello\nhello\n", $output, 'multi-line loggedCommand output');
$output = $reporter->loggedCommand
  ('echo hello && echo hello && sleep 30 && echo hello', 10);
is("hello\nhello\n", $output, 'partial loggedCommand output');

# loggedCommandWithRetries
use Data::Dumper;
my $logPat = $reporter->{log_pat};
$reporter->{log_pat} = "system";
$output = $reporter->loggedCommandWithRetries('sleep 5 && echo hello', undef, 3, 30);
is($output, "hello\n", 'untimed loggedCommandWithRetries w/o retries (out)');
ok(scalar(@{$reporter->{log_entries}}) == 1, 
   'untimed loggedCommandWithRetries w/o retries (log)' );
$reporter->{log_entries} = [];

$output = $reporter->loggedCommandWithRetries('sleep 5 && echo hello', 2, 2, 3);
ok(defined($output), 'timed loggedCommandWithRetries output');
ok($? != 0, 'timed loggedCommandWithRetries exit code');
ok($! = EINTR, 'timed loggedCommandWithRetries $!');
ok(scalar(@{$reporter->{log_entries}}) == 2, 
   'timed loggedCommandWithRetries (log)' );
$reporter->{log_entries} = [];

my $pid = fork();
`rm -f .testfile`;
if ( $pid == 0 ) {
  sleep 5;
  `touch .testfile`;
  exit;
} else {
  $output = $reporter->loggedCommandWithRetries('ls .testfile', undef, 5, 3);
  ok( $? == 0, 'loggedCommandWithRetries succeeds on 3rd try exit code' );
  ok(scalar(@{$reporter->{log_entries}}) == 3, 
     'loggedCommandWithRetries succeeds on 3rd try exit code logs' );
  `rm -f .testfile`;
  waitpid($pid, 0);
}
$reporter->{log_entries} = [];

$pid = fork();
`touch .testfile`;
if ( $pid == 0 ) {
  sleep 5;
  `rm -f .testfile`;
  exit;
} else {
  $output = $reporter->loggedCommandWithRetries('ls .testfile', undef, 5, 3, 1);
  my $cmdexitcode = $? >> 8;
  is( $cmdexitcode, 1, 'loggedCommandWithRetries succeeds with specified exit code (ec)' );
  ok(scalar(@{$reporter->{log_entries}}) == 3, 
   'loggedCommandWithRetries succeeds with specified exit code (log)' );
  `rm -f .testfile`;
  waitpid($pid, 0);
}
$reporter->{log_pat} = $logPat;

$pid = fork();
`touch .testfile`;
if ( $pid == 0 ) {
  sleep 5;
  `rm -f .testfile`;
  exit;
} else {
  $output = $reporter->loggedCommandWithRetries('ls .testfile', undef, 5, 3, undef, "No such");
  my $cmdexitcode = $? >> 8;
  is( $cmdexitcode, 1, 'loggedCommandWithRetries succeeds with specified regex (ec)' );
  ok(scalar(@{$reporter->{log_entries}}) == 3, 
   'loggedCommandWithRetries succeeds with specified regex (log)' );
  `rm -f .testfile`;
  waitpid($pid, 0);
}

$reporter->{log_pat} = $logPat;
# tempFile
open(TEMP, ">tmp1");
open(TEMP, ">tmp2");
open(TEMP, ">tmp3");
mkdir("tmp4");
open(TEMP, ">tmp4/tmp5");
close(TEMP);
$reporter = new Inca::Reporter;
$reporter->tempFile("tmp1");
$reporter->tempFile("tmp2", "tmp3", "tmp4");
$reporter = undef;
ok(!open(TEMP, "<tmp1") && !open(TEMP, "<tmp2") && !open(TEMP, "<tmp3") &&
   !open(TEMP, "<tmp4/tmp5"), 'tempFile');

# untested (at least directly)
# failPrintAndExit($msg)
# log($type, $msg)
# print($verbose)
# reportBody()
