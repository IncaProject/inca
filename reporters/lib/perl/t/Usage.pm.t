use strict;
use warnings;
use Inca::Reporter::Usage;
use Test::More 'no_plan';

my $msg = 'I failed';

# empty constructor
my $reporter = new Inca::Reporter::Usage();
ok(defined($reporter), 'new');
is(scalar($reporter->getEntries()), 0, 'no entries');

# failure/success
$reporter->success();
ok($reporter->getCompleted(), 'success');
$reporter->fail($msg);
ok(!$reporter->getCompleted() &&
   $reporter->getFailMessage() eq $msg, 'failure');
$reporter->success();

# report body w/no entries
my $body = $reporter->reportBody();
ok($body =~ m#<usage/>|<usage>\s*</usage>#, 'empty body');

# addEntry
$reporter->addEntry(
  {type => 'anEntry',
   name => 'first',
   stats => {
     height => 11,
     width => 22
   }
  }
);
is(scalar($reporter->getEntries()), 1, 'one entry');

# report body w/entries
$body = $reporter->reportBody();
ok($body =~
   m#<statistic>\s*<name>width</name>\s*<value>22</value>\s*</statistic>#,
   'non-empty body');
$reporter->addEntry(
  {type => 'anEntry',
   name => 'second',
   stats => {
     height => 33,
     width => 44
   }
  }
);
$reporter->addEntry(
  {type => 'anEntry',
   name => 'third',
   stats => {
     height => 55,
     width => 66
   }
  }
);
is(scalar($reporter->getEntries()), 3, 'multiple entries');
$body = $reporter->reportBody();
ok($body =~
   m#<statistic>\s*<name>width</name>\s*<value>44</value>\s*</statistic># &&
   $body =~
   m#<statistic>\s*<name>width</name>\s*<value>66</value>\s*</statistic>#,
   'non-empty body');
