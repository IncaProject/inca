#!/bin/bash

cp -R j2ssh/ temp/
cp -R src/com/ temp/src/main/java/com/
cd temp/
mvn clean package

if [ $? -eq 0 ]; then
    mv target/j2ssh-maverick-gsissh-1.5.5.jar ../build/
fi

cd ..
rm -rf temp/
