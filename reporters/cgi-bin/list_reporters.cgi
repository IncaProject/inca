#!/usr/bin/perl

#############################################################################

=head1 NAME

list_reporters.cgi

=head1 SYNOPSIS

Lists all the reporters in a Packages.gz file

=cut
#############################################################################

use strict;
use warnings;
use Carp;
use CGI;
use CGI qw(:all);

my $packages = param('packages');
if (!defined $packages){
  $packages = "../Packages.gz";
}

print header;
print "<html><head><link href=\"http://inca.sdsc.edu/www/css/style.css\"
        rel=\"stylesheet\" type=\"text/css\" /></head><body>";

if (! -e $packages){
  print "<p>The Packages.gz file <b>" . $packages . "</b> cannot be located.</p>";
  exit;
} 
my $pks = `zcat $packages`;
if (!$pks){
  $pks = `gzcat $packages`;
}
my @pks = split("arguments:", $pks);
my @names;
my @descriptions;
for my $pk(@pks){  
  my ($pre, $desc, $file, $name, $post) = $pk =~ m/(.*description: )(.[^\n]*)(\nfile: .[^\n]*\nname: )(.[^\n]*)(.*)/;
  push (@names, "<p><a href=\"reporters.cgi?action=help&reporter=" . $name . "\">" . $name . "</a></p>");
  push (@descriptions, "<p>$desc</p>");
}
my $numReporters = scalar(@names);
print "<p>This repository contains $numReporters reporters.</p>";
print "<table border=1 cellpadding=6><tr><td><p><b>Name</b></p></td><td><p><b>Description</b></p></td></tr>";
for (my $i=1; $i<$numReporters; $i++){
  print "<tr><td>$names[$i]</td><td>$descriptions[$i]</td></tr>";
}
print "</table>";

__END__

=head1 AUTHOR

Kate Ericson <kericson@sdsc.edu>

=cut

