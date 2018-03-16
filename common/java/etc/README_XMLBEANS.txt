Here are some simple instructions for generating the XMLBeans jar
from our schema files.

First I have already added all of the required jar files to contrib and
to the Dependencies file in this directory.  Start out by running
ant populate-depends.  Then from this directory, run

% java -cp ../lib/xbean-2.4.jar:lib/xbean_xpath-2.4.jar org.apache.xmlbeans.impl.tool.SchemaCompiler -out ../incaXmlBeans.jar *xsd*
Time to build schema type system: 0.844 seconds
Time to generate code: 0.432 seconds
Time to compile code: 4.34 seconds
Compiled types to: ../incaXmlBeans.jar

