#!/bin/sh
set -e

if [ "$TRAVIS_PULL_REQUEST" = false ]; then
  if [ "$TRAVIS_BRANCH" = "master" ]; then
    ./gradlew uploadArchives
  else
  	./gradlew uberjar
  fi
else
  ./gradlew uberjar
fi