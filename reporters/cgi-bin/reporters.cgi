#!/usr/bin/perl

#############################################################################

=head1 NAME

reporters.cgi

=head1 SYNOPSIS

Displays a reporter or its help info

=cut
#############################################################################

#=============================================================================#
# Usage
#=============================================================================#
use strict;
use warnings;
use Carp;
use CGI;
use CGI qw(:all);

#=============================================================================#
# Functions
#=============================================================================#

#-----------------------------------------------------------------------------#
# viewCmd( $cmd, $link )
#
# display the reporter script
#-----------------------------------------------------------------------------#
sub viewCmd {
	my $cmd = shift;
	my $rep = shift;
	my $action = shift;
	my $text = shift;

	my @output = `$cmd`;
	print "<pre>";
	foreach(@output) {
		$_ =~ s/</&lt\;/g;
		print $_;
	}
	print "</pre><hr><br><p>
		<a href=\"reporters.cgi?reporter=$rep&action=$action\">Click here</a> 
		to $text the $rep reporter.</p>";
}

#=============================================================================#
# Main
#=============================================================================#

my $action = param('action');
my $rep = param('reporter');
my $reploc = "../bin/";
my $file = $reploc . $rep;

print header;
print "<html><head><link href=\"http://inca.sdsc.edu/www/css/style.css\"
rel=\"stylesheet\" type=\"text/css\" /></head><body>";

my $cmd;
my $alt;
my $text;

if ((!defined($rep)) || (!defined($action))){
	print "<p>This script requires a reporter name and action (view|help) as input
	parameters.</p><p>e.g. \"reporters.cgi?reporter=reporter.name.unit&action=view\"</p>";
	exit;
} elsif ($rep =~ /\//){
	print "<p>Invalid reporter name.</p>";
	exit;
} elsif (! -e $file){
	print "<p>The reporter <b>" . $file . "</b> cannot be located.</p>";
	exit;
} elsif ($action eq "view"){
	$cmd = "cat " . $file;
	$alt = "help";
	$text = "see help information for";
} elsif ($action eq "help"){
	$cmd = $file . " -help=yes -verbose=0";
	$alt = "view";
	$text = $alt;
} else {
	print "<p>The \"action\" input parameter must be (view|help).</p>";
	exit;
}

viewCmd( $cmd, $rep, $alt, $text );

__END__

=head1 AUTHOR

Kate Ericson <kericson@sdsc.edu>

=cut

