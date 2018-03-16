#!/usr/bin/perl

use Cwd;
use Test::More qw(no_plan);

sub verifyError {
  my ( $command, @markers ) = @_;

  my $output = `$command 2>&1`;
  cmp_ok( $?, '>', 0, "$command returned non-zero" );
  
  my $marker;
  for $marker ( @markers ) {
    like( $output, qr/$marker/, "$command - found $marker" );
  }
}

sub verifyOutput {
  my ( $command, $outfile, @markers ) = @_;

  my $output =`$command`;
  is( $?, 0, "$command returned 0" );
  if ( defined $outfile ) {
    local $/;
    open( FD, "<$outfile" );
    $output .= <FD>;
    close FD;
  }
  
  my $marker;
  for $marker ( @markers ) {
    like( $output, qr/$marker/, "$command - found $marker" );
  }
}

sub checkTrusted {
  ok( -f "test/etc/trusted/agentcert.pem", "agent cert in trusted" );
  my $hostname = `hostname`;
  chomp( $hostname );
  ok( -f "test/etc/trusted/${hostname}cert.pem", "ca cert in trusted" );
}

ok( -f "inca.t", "we are in t directory" );

my $cwd = getcwd();

mkdir( "test" );
mkdir( "test/bin" );
mkdir( "test/sbin" );
mkdir( "test/etc" );
mkdir( "test/lib" );
link( "../sbin/inca", "test/sbin/inca" );

# Test: fails if no inca.conf file
unlink "test/etc/inca.conf";
verifyError( "test/sbin/inca", "Unable to locate" );

# recreate inca.conf file
open( FD, ">test/etc/inca.conf" );
print FD <<'CONF';
depot:inca.Depot:inca-depot.jar
reporter-manager
sample_perl
sample_java:IncaTest:incatest.jar
CONF
close FD;

# create sample_perl
my @files = qw(test/sbin/sample_perl test/bin/inca-test-me test/bin/inca-testing);
for my $file ( @files ) {
  open( FD, ">$file" );
  print FD <<'SAMPLE';
#!/usr/bin/perl
print $ENV{PERL5LIB}, "\n";
print join(' ', @ARGV), "\n";
exit 0;
SAMPLE
  close FD;
  chmod( oct("0744"), "$file" );
}

# create sample_java
open( FD, ">IncaTest.java" );
print FD <<'SAMPLE';
import java.util.Properties;
import java.util.Enumeration;
;
class IncaTest {
  public static void main(String[] args) {
    System.out.println(System.getProperty("java.class.path"));;
    for (int i = 0; i < args.length; i++) {
      System.out.println(args[i]);
    }
    Properties pros = System.getProperties();
    Enumeration names = pros.propertyNames();
    while ( names.hasMoreElements() ) {
      String name = (String)names.nextElement();
      String value = pros.getProperty( name );
      System.out.println( name + "=" + value );
    }
    System.exit(0);
  }
}
SAMPLE
close FD;
`javac IncaTest.java`;
`jar -cf test/lib/incatest.jar IncaTest.class`;
unlink "IncaTest.class";
unlink "IncaTest.java";

# Test: usage printed in 3 cases
verifyOutput( "test/sbin/inca", undef, "Usage", "Available" );
chdir( "test/sbin" );
verifyOutput( "./inca", undef, "Usage", "Available" );
chdir( "../../" );
verifyOutput( "test/sbin/inca help", undef, "Usage", "Available", "sample_perl", "sample_java" );

# Test: prints version
# Verify perl
open( FD, ">test/etc/inca-version" );
print FD <<'VERSION';
major=2
minor=1
VERSION
close FD;
verifyOutput( "test/sbin/inca version", undef, "Inca version", "2.1" );

# Test: unknown command
verifyError( 'echo | test/sbin/inca dummy', 'unknown Inca component' );

# Test: binscript of lowercaseUppercase
my $output = `test/sbin/inca testMe --arg=hi -barg=bhi`;
ok( $output =~ /--arg=hi -barg=bhi/, "found args in binscript" );

# Test: binscript of singleword
my $output = `test/sbin/inca testing --arg=hi -barg=bhi`;
ok( $output =~ /--arg=hi -barg=bhi/, "found args in binscript single" );

# Test: can execute a perl program
verifyOutput( "echo | test/sbin/inca sample_perl --blah", "test/var/sample_perl.out", "--blah" );
unlink "test/sbin/sample_perl";

# Test: can execute a java program
$ENV{http_proxy} = "http://inca.sdsc.edu:8983";
verifyOutput( "echo | test/sbin/inca sample_java --help", 
              "test/var/sample_java.out", "incatest.jar", 
              "--help", "http.proxyHost=inca.sdsc.edu", "http.proxySet=true", 
              "http.proxyPort=8983" );
$ENV{HTTPS_PROXY} = "https://inca.sdsc.edu:8983";
verifyOutput( "echo | test/sbin/inca sample_java --help",
              "test/var/sample_java.out", "incatest.jar", 
              "https.proxyHost=inca.sdsc.edu", "https.proxySet=true", 
              "https.proxyPort=8983" );
$ENV{GLOBUS_TCP_PORT_RANGE} = "6000,6060";
verifyOutput( "echo | test/sbin/inca sample_java --help", 
               "test/var/sample_java.out", "incatest.jar", 
              "org.globus.tcp.port.range=6000,6060" );
unlink "test/lib/incatest.jar";

# test create certs - encryption
`echo "test\ntest\n" | test/sbin/inca createauth -P stdin > /dev/null 2>&1`;
for my $component qw( agent incat depot consumer ) {
  ok( -f "test/etc/${component}cert.pem", "${component} cert created" );
  ok( -f "test/etc/${component}key.pem", "${component} key created" );
  my $encryption = `cat test/etc/${component}key.pem | grep ENCRYPTED`;
  chomp( $encryption );
  like( $encryption, qr/ENCRYPTED/, "key is encrypted" );
}
checkTrusted();
`rm -fr test/etc/*.pem test/etc/trusted`;

# test create certs - no encryption
`echo | test/sbin/inca createauth > /dev/null 2>&1`;
for my $component qw( agent incat depot consumer ) {
  ok( -f "test/etc/${component}cert.pem", "${component} cert created" );
  ok( -f "test/etc/${component}key.pem", "${component} key created" );
  my $encryption = `cat test/etc/${component}key.pem | grep ENCRYPTED`;
  chomp( $encryption );
  is( $encryption, "", "key is not encrypted" );
}
checkTrusted();

`rm -fr test/etc/*.pem test/etc/trusted`;
unlink "test/etc/inca.conf";
unlink "test/etc/inca-version";
unlink "test/sbin/inca";
rmdir( "test/sbin" );
rmdir( "test/etc" );
rmdir( "test/lib" );
`rm -fr test/var`;
rmdir( "test" );

