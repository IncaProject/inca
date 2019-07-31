#!/bin/sh

path_prefix="../lib"
classpath="$path_prefix/jsr173_1.0_api.jar:$path_prefix/saxon9.jar:$path_prefix/xbean-2.4.jar:$path_prefix/xbean_xpath-2.4.jar"

java -cp "$classpath" org.apache.xmlbeans.impl.tool.SchemaCompiler -javasource 1.8 -out incaXmlBeans.jar *.xsd*
