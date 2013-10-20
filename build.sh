#!/bin/sh
set -e

if [ "$TRAVIS_PULL_REQUEST" = false ]; then
  if [ "$TRAVIS_BRANCH" = "master" ]; then
  	./gradlew uberjar
  else
    python addServer.py && mvn deploy --settings ~/.m2/mySettings.xml
  fi
else
  ./gradlew uberjar
fi