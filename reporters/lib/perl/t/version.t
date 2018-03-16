#!/usr/bin/perl

use strict;
use warnings;
use Test::More 'no_plan';

my $exe = "rep$$";
my $src = "rep$$.c";

# A hash that associates version reporters with the command to produce the
# version and the pattern to search.
my %reporters = (
  'cluster.accounting.tgusage.version' =>
    {cmd => 'tgusage version', pat => 'version ([\d\.]+)'},
  'cluster.admin.ant.version' =>
    {cmd => 'ant -version', pat => 'version ([\d\.]+)'},
  'cluster.admin.bash.version' =>
    {cmd => 'bash --version', pat => 'version (\S+)'},
  'cluster.admin.pacman.version' =>
    {cmd => 'pacman -version', pat => 'Pacman version:\s+([^\-a-z\n]+)'},
  'cluster.admin.pvfs.version' =>
    {cmd => 'cat {DIR}/include/pvfs_config.h',
     pat => 'PVFS_RELEASE_NR\s+(\d\d\d\d\d)'},
  'cluster.admin.softenv.version' =>
    {cmd => 'softenv --version', pat => 'version ([\d\.]+)'},
  'cluster.admin.tgpolicy.version' =>
    {cmd => 'tg-policy -v', pat => 'tg-policy v([\w\.]+)'},
  'cluster.admin.tgproxy.version' =>
    {cmd => "grep set-user-proxy $ENV{'HOME'}/.soft.v3.cache.sh",
     pat => '.*X509_USER_PROXY=.*tgproxy-([\d\.]*)'},
  'cluster.admin.tgresid.version' =>
    undef, # subpackages
  'cluster.admin.xcat.version' =>
    {cmd => 'cat {DIR}/RELEASE', pat => '(.+)'},
  'cluster.communication.mpich-gm.version' =>
    {cmd => 'mpi.inca.version', pat => '([\d\.]+)'},
  'cluster.communication.mpich-p4.version' =>
    {cmd => 'mpi.inca.version', pat => '([\d\.]+)'},
  'cluster.communication.mpich-vmi.version' =>
    {cmd => 'mpiversion', pat => 'MPICH (.+)'},
  'cluster.communication.vmi-crm.version' =>
    {cmd => 'crm -v', pat => '\(CRM(.+)\)'},
  'cluster.compiler.gcc.version' =>
    {cmd => 'gcc -dumpversion', pat => '([\d\.]+)'},
  'cluster.compiler.hp.version' =>
    {cmd => 'cc -V 2>&1', pat => 'Compaq C V((.[^\s]*))(\s.*)'},
  'cluster.compiler.intel.version' =>
    undef, # subpackages
  'cluster.compiler.xlc.version' =>
    {cmd => 'xlc -qversion', pat => '(.+)'},
# uname -r on non-SuSE
#  'cluster.dist.version' =>
#    {cmd => 'cat /etc/SuSE-release', pat => 'VERSION = ([\d\.]+)'},
  'cluster.driver.myricom_gm.version' =>
    {cmd => 'gm_board_info', pat => '([\d\.]+)_Linux'},
  'cluster.driver.qlogic.version' =>
    {cmd => 'strings /proc/scsi/qla2300/2',
     pat => 'Driver version\s+([\d\.]+)'},
  'cluster.filesystem.gpfs.version' =>
    undef, # rpm
  'cluster.firmware.qlogic.version' =>
    {cmd => 'strings /proc/scsi/qla2300/2',
     pat => 'Firmware version:\s+([\d\.]+)'},
  'cluster.interactive_access.openssh.version' =>
    {cmd => 'ssh -V', pat => 'OpenSSH_([\w\.]+)|GSI ([\w\.]+)'},
  'cluster.java.sun.version' =>
    {cmd => 'java -version', pat => 'version "(.+)"'},
  'cluster.lang.perl.version' =>
    {cmd => 'perl -v', pat => 'perl, v([\d\w\.\-]+)'},
  'cluster.lang.perlmod.version' =>
    undef, # subpackages
  'cluster.libraries.glibc.version' =>
    {cmd => 'ldconfig -V', pat => '\(GNU libc\) ([\d\.]+)'},
  'cluster.libraries.goto.version' =>
    undef, # subpackages
  'cluster.math.atlas.version' =>
    {cmd => "cc -o $exe $src -L{DIR}/lib -latlas && ./$exe",
     pat => 'ATLAS version (\S+)'},
  'cluster.math.atlas.version' =>
    {cmd => "cc -o $exe $src -L{DIR}/lib -lmkl && ./$exe",
     pat => 'Version (\S+)'},
  'cluster.mgmt.rocks.version' =>
    {cmd => 'cat /etc/motd', pat => 'Rocks\\s+([\\d\\.]+)'},
  'cluster.monitoring.ganglia.version' =>
    {cmd => 'gmond -V', pat => 'gmond ([\d.]+)'},
  'cluster.os.kernel.version' =>
    {cmd => 'uname -r', pat => '(.+)'},
  'cluster.os.redhat.version' =>
    {cmd => 'cat /etc/redhat-release', pat => '(.+)'},
  'cluster.os.suse.version' =>
    {cmd => 'cat /etc/SuSE-release', pat => 'VERSION = ([\d\.]+)'},
  'cluster.rpms' =>
    undef, # rpm
  'cluster.scheduler.maui.version' =>
    {cmd => 'showres --version', pat => 'version (.+)'},
  'cluster.scheduler.pbs.version' =>
    {cmd => 'echo list server | qmgr', pat => 'version = (.+)'},
  'cluster.scripting.python.version' =>
    {cmd => qq#python -c"import sys; print '.'.join([str(t) for t in sys.version_info [0:3]])"#, pat => '([\d.]+)'},
  'cluster.scripting.tcl.version' =>
    {cmd => 'echo \'puts $tcl_version\' | tclsh', pat => '([\d\.]+)'},
  'cluster.security.openssl.version' =>
    {cmd => 'echo version | openssl', pat => 'OpenSSL (\S+)'},
  'cluster.web.httpd.version' =>
    {cmd => 'httpd -V', pat => 'Apache/([\S\.]+)'},
  'data.access.db2client.version' =>
    undef, # subpackages
  'data.access.oracleClient.version' =>
    {cmd => 'sqlplus -V', pat => 'Release\s([\S]+)'},
  'data.access.srb.version' =>
    {cmd => 'Sinit -v && Sexit', pat => 'SRB-([\d\.]+)'},
  'data.db.postgres.version' =>
    {cmd => 'psql -V', pat => 'PostgreSQL.\s+(.+)'},
  'data.filtering.datacutter.version' =>
    {cmd => 'dird -version', pat => '(\w+)'},
  'data.format.hdf4.version' =>
    {cmd => 'hdp -V', pat => 'Version ([^,]+)'},
  'data.format.hdf5.version' =>
    {cmd => 'h5dump -V', pat => 'Version (.+)'},
  'data.transfer.tgcp.version' =>
    {cmd => 'tgcp -version', pat => 'tgcp (.+)'},
  'data.transfer.uberftp.version' =>
    {cmd => 'uberftp -version', pat => 'Version (\S+)'},
  'grid.admin.gpt-wizard.version' =>
    {cmd => 'gpt-wizard -version', pat => 'release (\S+)'},
  'grid.admin.gpt.version' =>
    {cmd => 'gpt-verify -version', pat => 'Version (\S+)'},
  'grid.information_service.mds.version' =>
    undef, # gpt
  'grid.interactive_access.condorg.version' =>
    undef, # gpt
  'grid.interactive_access.gsincftp.version' =>
    undef, # gpt
  'grid.interactive_access.gsissh.version' =>
    undef, # gpt
  'grid.interactive_access.myproxy.version' =>
    undef, # gpt
  'grid.middleware.cog.version' =>
    {cmd => 'grid-proxy-init -version', pat => 'globus_proxy_utils-([\d\.]+)'},
  'grid.middleware.globus.version' =>
    undef, # gpt
  'grid.middleware.globus4.version' =>
    {cmd => 'globus-version', pat => '(.+)'},
  'grid.middleware.rlsclient.version' =>
    {cmd => 'globus-rls-cli', pat => 'Version: (\S+)'},
  'grid.monitoring.nws.version' =>
    {cmd => 'nws_extract -V', pat => 'version ([\d\.]+)'},
  'grid.parallel_tools.mpich.version' =>
    {cmd => 'mpicc -v', pat => 'mpicc for ([\d\.]+)'},
  'grid.parallel_tools.mpichg.version' =>
    undef, # subpackages
  'grid.scheduling.gridshell.version' =>
    {cmd => 'cat {DIR}/package_version',
     pat => 'Version.(.*)'},
  'grid.resource_management.gram.version' =>
    undef, # gpt
  'grid.scheduling.condorg.version' =>
    {cmd => 'condor_version', pat => 'Version: ([\d\.]+)'},
  'grid.security.gsi.version' =>
    undef, # gpt
  'grid.security.gx-map.version' =>
    {cmd => 'gx-map -version', pat => 'version (.+)'},
  'perfeval.api.papi.version' =>
    {cmd => "cc -o $exe $src -I{DIR}/include -L{DIR}/lib -lpapi && ./$exe",
     pat => 'PAPI VERSION (\S+)'},
  'viz.bin.paraview-mesa.version' =>
    {cmd => 'cat {DIR}/version', pat => '(\S+)'},
  'viz.bin.paraview-nvgl.version' =>
    {cmd => 'cat {DIR}/version', pat => '(\S+)'},
  'viz.bin.xfree86.version' =>
    {cmd => 'grep "XFree86 Version" /var/log/XFree86.0.log',
    pat => 'Version ([\w\.]+)'},
  'viz.lib.chromium.version' =>
    {cmd => 'grep CR_VERSION_STRING {DIR}/src/include/cr_version.h',
     pat => '"(\S+)"'},
  'viz.lib.mesa.version' =>
    {cmd => 'cat {DIR}/version', pat => '(\S+)'},
  'viz.lib.nvidia.version' =>
    {cmd => 'grep NVIDIA /var/log/XFree86.0.log',
     pat => 'Driver\s+([\w\.\-]+)'},
  'viz.lib.vtk-mesa.version' =>
    {cmd => 'cat {DIR}/version', pat => '(\S+)'},
  'viz.lib.vtk-nvgl.version' =>
    {cmd => 'cat {DIR}/version', pat => '(\S+)'},
  'viz.tools.cmake.version' =>
    {cmd => 'cmake --version', pat => 'version (\S+)'},
  'viz.tools.imagemagick.version' =>
    {cmd => 'convert -help', pat => 'ImageMagick (\S+)'},
  'viz.tools.netpbm.version' =>
    {cmd => 'pnmcat --version', pat => 'Version: Netpbm (\S+)'},
  'ws.container.tomcat.version' =>
    {cmd => 'wget -q -O - http://localhost8080',
     pat => 'Apache Tomcat\/([\w\.]+)'},
  'ws.portlet.gridsphere.version' =>
    undef, # complex search of /opt/gridsphere/build.xml
  'ws.soap.axis.version' =>
    {cmd => 'java -classpath /opt/axis/axis.jar:/opt/axis/lib/commons-logging.jar:/opt/axis/lib/commons-discovery.jar org.apache.axis.Version',
     pat => 'Apache Axis version: ([\S\.]+)'}
);
my %code = (
  'cluster.math.atlas.version' => '
int main(int argc, char **argv) {
  void ATL_buildinfo(void);
  ATL_buildinfo();
  return 0;
}',
  'cluster.math.mkl.version' => '
#include <stdio.h>
int main(void)  {
   char buf[100];
   MKLGetVersionString(buf, sizeof(buf));
   printf("%s\n", buf);
   return 0;
}',
  'perfeval.api.papi.version' => '
#include <papi.h>
#include <stdio.h>
int main(int argc, char **argv) {
  if(PAPI_library_init(PAPI_VER_CURRENT) != PAPI_VER_CURRENT)
    return 1;
  printf("PAPI VERSION %d\n", PAPI_VER_CURRENT);
  return 0;
}'
);

my %versions;
$ENV{PATH} = "/usr/sbin:$ENV{PATH}";

my $i;
# For each reporter, exec the cmd from %reporter to generate the version #.
foreach my $reporter(sort keys %reporters) {
  next if !defined($reporters{$reporter});
  my ($cmd, $pat) =
    ($reporters{$reporter}->{cmd}, $reporters{$reporter}->{pat});
  # A few reporters require compiling source code first
  if(defined($code{$reporter})) {
    open(OUT, ">$src");
    print OUT "$code{$reporter}\n";
    close(OUT);
  }
  # If cmd contains an install dir reference, see if we can find it
  if($cmd =~ /{DIR}/) {
    my $dir = '/notthere';
    my ($software) = $reporter =~ /([^\.]+)\.version$/;
    foreach my $place($ENV{uc($software) . "_LOCATION"},
                      $ENV{uc($software) . "_HOME"},
                      glob("/usr/local/apps/$software*"),
                      glob("/opt/$software*"),
                      glob("/usr/local/$software*")) {
      $dir = $place and last if defined($place) && -d $place;
    }
    if($cmd =~ /-L{DIR}/) {
      $ENV{LD_LIBRARY_PATH} = defined($ENV{LD_LIBRARY_PATH}) ?
        "$ENV{LD_LIBRARY_PATH}:$dir/lib" : "$dir/lib";
    }
    $cmd =~ s/{DIR}/$dir/g;
  }
  my $cmdOut = `($cmd) 2>&1`;
  chomp($cmdOut) if defined($cmdOut);
  if(defined($cmdOut) && $cmdOut =~ /$pat/) {
    # For a non-zero exit status (which some programs use when asked for
    # version), consider the version valid if the pattern matches context as
    # well as the version #
    $versions{$reporter} = $1 if $? == 0 || $pat !~ /^\(.*\)$/;
  }
}
unlink($exe, $src);

# Replicate any manipulation the reporters do to the version #
if(defined($versions{'cluster.admin.pvfs.version'})) {
  $versions{'cluster.admin.pvfs.version'} =~ s/(\d)(\d\d)(\d\d)/$1.$2.$3/;
}

$ENV{PERL5LIB} = defined($ENV{PERL5LIB}) ? "$ENV{PERL5LIB}:lib/perl:.." : 'lib/perl:..';
my $bindir = -d 'bin' ? 'bin' : -d '../../../bin' ? '../../../bin' : 'bin';
foreach my $reporter(sort keys %reporters) {
  SKIP: {
    skip("No test written for $reporter", 1)
      if !defined($reporters{$reporter});
    my $firstLine = `head -1 $bindir/$reporter`;
    chomp($firstLine);
    $firstLine =~ /^!#\s*(.*)/;
    my $exec = defined($1) ? $1 : 'perl';
    my $output = `$exec $bindir/$reporter -verbose=0`;
    my $expected = defined($versions{$reporter}) ? 'completed' : 'failed';
    like($output, "/^$expected/", $reporter);
  }
}

foreach my $reporter(sort keys %reporters) {
  SKIP: {
    skip("$reporter version not available\n", 1)
      if !defined($versions{$reporter});
    my $firstLine = `head -1 $bindir/$reporter`;
    chomp($firstLine);
    $firstLine =~ /^!#\s*(.*)/;
    my $exec = defined($1) ? $1 : 'perl';
    my $output = `$exec $bindir/$reporter -verbose=1`;
    $output =~ /<body>.*<version>(.*?)<\/version>/s;
    fail("$reporter defined") if !defined($1);
    is($1, $versions{$reporter}, $reporter);
  }
}
