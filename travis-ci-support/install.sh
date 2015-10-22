#!/bin/bash
cd $TRAVIS_BUILD_DIR/travis-ci-support
mvn install:install-file -Dfile=Discord4J-1.0-SNAPSHOT.jar -DpomFile=Discord4J-1.0-SNAPSHOT.pom
