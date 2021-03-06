#!/bin/bash


if [ ! -f rsrc/j2ssh-maverick-RELEASE_1.5.5a.tar.gz ]; then
    echo "downloading j2ssh source"

    curl -s -S -L "https://github.com/sshtools/j2ssh-maverick/archive/RELEASE_1.5.5a.tar.gz" > rsrc/j2ssh-maverick-RELEASE_1.5.5a.tar.gz
fi

if [ ! -d j2ssh/ ]; then
    tar -xzf rsrc/j2ssh-maverick-RELEASE_1.5.5a.tar.gz
    mv j2ssh-maverick-RELEASE_1.5.5a/j2ssh-maverick/ j2ssh/
    rm -rf j2ssh-maverick-RELEASE_1.5.5a/
    patch -p0 < rsrc/j2ssh.patch
fi

if [ ! -d lib/ ]; then
    mkdir lib/
fi

if [ ! -f lib/bcprov-jdk15on-168.jar ]; then
    echo "downloading bcprov jar"

    curl -s -S -L "https://downloads.bouncycastle.org/java/bcprov-jdk15on-168.jar" > lib/bcprov-jdk15on-168.jar
fi

if [ ! -f lib/jzlib-1.1.3.jar ]; then
    echo "downloading jzlib jar"

    curl -s -S "https://repo1.maven.org/maven2/com/jcraft/jzlib/1.1.3/jzlib-1.1.3.jar" > lib/jzlib-1.1.3.jar
fi

if [ ! -f lib/oro-2.0.8.jar ]; then
    echo "downloading oro jar"

    curl -s -S "https://repo1.maven.org/maven2/oro/oro/2.0.8/oro-2.0.8.jar" > lib/oro-2.0.8.jar
fi
