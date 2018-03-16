#!/bin/sh

libs=`ls lib/*`
classpath=`echo $libs | sed 's/ /:/g'`
echo $classpath

java -cp etc:etc/common:inca-common.jar:$classpath edu.sdsc.inca.util.SuiteStagesWrapper $*
