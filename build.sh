#!/bin/sh
set -e

if [ "$TRAVIS_PULL_REQUEST" = false ]; then
  if [ "$TRAVIS_BRANCH" = "master" ]; then
  	./gradlew uberjar
  else
    ./gradlew uploadArchives
  fi
else
  ./gradlew uberjar
fi