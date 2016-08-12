#!/bin/bash
set -euo pipefail
echo "Running for $CI_BUILD_NUMBER"
  
  #deploy the version built by travis
  CURRENT_VERSION=`mvn help:evaluate -Dexpression="project.version" | grep -v '^\[\|Download\w\+\:'`

  if [[ $CURRENT_VERSION =~ "-SNAPSHOT" ]]; then
    echo "======= Found SNAPSHOT version ======="
    # Do not deploy a SNAPSHOT version but the release version related to this build
    RELEASE_VERSION=`echo $CURRENT_VERSION | sed "s/-.*//g"`
    NEW_VERSION="$RELEASE_VERSION-build$CI_BUILD_NUMBER"
    echo $NEW_VERSION  
  else
    echo "======= Found RELEASE version ======="
    NEW_VERSION=`echo $CURRENT_VERSION | sed "s/-.*//g"`
  fi

  mkdir -p target
  cd target
  curl --user $ARTIFACTORY_QA_READER_USERNAME:$ARTIFACTORY_QA_READER_PASSWORD -sSLO https://repox.sonarsource.com/sonarsource-public-qa/org/sonarsource/sonarlint/sonarlint-cli/$NEW_VERSION/sonarlint-cli-$NEW_VERSION.zip
  cd ..
 
  mvn install:install-file -Dfile=target/sonarlint-cli-$NEW_VERSION.zip -DgroupId=org.sonarsource.sonarlint \
    -DartifactId=sonarlint-cli -Dversion=$NEW_VERSION -Dpackaging=zip

  sonarlintVersion=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -Ev '(^\[|Download\w+:)'`

  # Run ITs
  cd it
  mvn test -Dsonarlint.version=${sonarlintVersion} -Dsonar.runtimeVersion=LATEST_RELEASE -B -e -V
