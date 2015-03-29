#!/bin/sh
set -e

if [ "$TRAVIS_PULL_REQUEST" = false ]; then
  if [ "$TRAVIS_BRANCH" = "master" ]; then
    ./gradlew uploadArchives
  elif [ "$TRAVIS_BRANCH" = "develop" ]; then
    ./gradlew uploadArchives
  else
    ./gradlew fatJar
  fi
else
  ./gradlew fatJar
fi
