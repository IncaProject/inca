use strict;
use warnings;
use Inca::Reporter::SimpleUnit;
use Test::More 'no_plan';

my $msg = 'I failed';
my $unit = 'myUnit';

# empty constructor
my $reporter = new Inca::Reporter::SimpleUnit();
ok(defined($reporter), 'new');
ok($reporter->getUnitName() eq '', 'default values');

# get/set methods
$reporter->setUnitName($unit);
ok($reporter->getUnitName() eq $unit, 'get/set');

# constructor attributes
$reporter = new Inca::Reporter::SimpleUnit(
  unit_name => $unit
);
ok($reporter->getUnitName() eq $unit, 'constructor attrs');

# unitFailure/Success
$reporter->unitSuccess();
ok($reporter->getCompleted(), 'unitSuccess');
$reporter->unitFailure($msg);
ok(!$reporter->getCompleted() &&
   $reporter->getFailMessage eq $msg, 'unitFailure');
$reporter->unitSuccess();
my $body = $reporter->reportBody();
ok($body =~ m#<ID>$unit</ID>#, 'reportBody');
