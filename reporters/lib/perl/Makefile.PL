use strict;
use warnings;
use ExtUtils::MakeMaker;
use File::Basename;
require "" . dirname( $0 ) . "/etc/common/Makefile.pm"; 

WriteMakefile(
    Inca::Makefile::getCommonOptions( "Inca-Reporter" )
);

