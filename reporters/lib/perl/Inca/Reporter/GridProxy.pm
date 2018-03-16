package Inca::Reporter::GridProxy;

################################################################################

=head1 NAME

Inca::Reporter::GridProxy - A pseudo-module indicating that a reporter requires
a proxy credential in order to execute.

=head1 SYNOPSIS

  my $reporter = new Inca::Reporter();
  $reporter->addDependency('Inca::Reporter::GridProxy');

=head1 DESCRIPTION

Declaring a dependency on this module is a signal to Inca's ReporterManager
that a grid proxy credential needs to be initialized before the reporter can
be run.

=cut
################################################################################

use strict;
use warnings;

1;

__END__

=head1 EXAMPLE

=head1 AUTHOR

Jim Hayes <jhayes@sdsc.edu>

=head1 SEE ALSO

L<Inca::Reporter>

=cut
