#!/usr/bin/env perl

use strict;
use warnings;
use Inca::Reporter::SimpleUnit;

my $reporter = new Inca::Reporter::SimpleUnit(
  name => 'data.access.srb.unit.sls',
  version => 4,
  description => 'This reporter tests the Sls command',
  url => 'http://www.sdsc.edu/DICE/SRB',
  unit_name => 'sls'
);
$reporter->processArgv(@ARGV);

my $MDIR = "$ENV{HOME}/.srb/srb_test.$$";
mkdir $MDIR || die "couldn't make $MDIR";
$reporter->tempFile($MDIR);

`cp $ENV{HOME}/.srb/.MdasEnv $MDIR/.MdasEnv.$$`;
`cp $ENV{HOME}/.srb/.MdasAuth $MDIR/.MdasAuth.$$`;
$ENV{"mdasEnvFile"} = "$MDIR/.MdasEnv.$$";
$ENV{"mdasAuthFile"} = "$MDIR/.MdasAuth.$$";

my $output = $reporter->loggedCommand('Sinit');
if($?) {
  $reporter->unitFailure("Sinit failed: $output $!");
} else {
  $output = $reporter->loggedCommand('Sls');
  if($?) {
    $reporter->unitFailure("Sls failed: $output $!");
  } else {
    $reporter->unitSuccess();
  }
}
unlink "$MDIR/.MdasEnv.$$", "$MDIR/.MdasAuth.$$";
$reporter->loggedCommand('Sexit');
$reporter->print();
