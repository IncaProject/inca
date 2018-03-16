use strict;
use warnings;
use Inca::Reporter::Performance;
use Test::More 'no_plan';
use Test::Exception;

my $msg = 'I failed';

# empty constructor
my $reporter = new Inca::Reporter::Performance();
ok(defined($reporter), 'new');

# report body w/no entries
my $body = $reporter->reportBody();
ok($body =~ m#<performance/>|<performance>\s*<ID/>\s*</performance>#, 'empty body');

# addEntry
my $benchmark = $reporter->addNewBenchmark("bench1"); 
$benchmark->setStatistic( "stat1", 5 );
$body = $reporter->reportBody();
ok($body =~ m#<ID>stat1</ID>\s*<value>5</value>#, 'stat added');

dies_ok { $benchmark->setStatistic("some stat name", 5); } 'spaces in stat name fails';
dies_ok { $benchmark->setStatistic("_name", 5); } 'first char should be alpha';
lives_ok { $benchmark->setStatistic("some-name", 5); } '- allowed in stat name';
lives_ok { $benchmark->setStatistic("some_name", 5); } '_ allowed in stat name';
dies_ok { $benchmark->setParameter("some param name", 5); } 'spaces in param name fails';

