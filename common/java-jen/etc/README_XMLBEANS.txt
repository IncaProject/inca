Here are some simple instructions for generating the XMLBeans jar
from our schema files.

First I have already added all of the required jar files to contrib and
to the Dependencies file in this directory.  Start out by running
ant populate-lib

After you have the jar files make sure that IntelliJ knows about them
by adding them to your classpath.  Just add everything under lib as well
as adding the etc directory.

Next make a new run item with these settings:

name -> generate XML Beans
Main class -> org.apache.xmlbeans.impl.tool.SchemaCompiler
Program Parameters -> -out ../contrib/incaXmlBeans.jar  common/java/etc/incaCommon.xsd common/java/etc/incaCommon.xsdconfig common/java/etc/suite.xsd common/java/etc/suite.xsdconfig common/java/etc/reporter.xsd common/java/etc/reporter.xsdconfig common/java/etc/suiteExpanded.xsd common/java/etc/suiteExpanded.xsdconfig common/java/etc/resourceConfig.xsd common/java/etc/resourceConfig.xsdconfig
(make sure there are no newlines in the above line)
Working Directory -> whatever it should be up to devel
choose the common/util project jdk and classpath as the one to use - this
last setting is kinda up to how you set up your projects.

OK now you are set and you should have this task saved and can remake the
xmlbean jar whenever you change the xsd or xsdconfig files that are
in etc.
