#!/bin/bash

# Stop the script if any command fails
set -e

# Define an array of all projects to publish with their corresponding build types
projects=(
  "ksoup-engine-common:"
  "ksoup-engine-kotlinx:"
  "ksoup-engine-korlibs:"
  "ksoup-engine-ktor2:"
  "ksoup-engine-okio:"
  "ksoup:kotlinx"
  "ksoup-network:kotlinx"
  "ksoup:korlibs"
  "ksoup-network-korlibs:korlibs"
  "ksoup:ktor2"
  "ksoup-network-ktor2:ktor2"
  "ksoup:okio"
)

# Loop through all projects and publish them
for project in "${projects[@]}"; do
  # Split the project name and build type
  IFS=":" read -r projectName buildType <<< "$project"

  ./gradlew clean --quiet --warning-mode=none
  if [ -n "$buildType" ]; then
    echo "Publishing $projectName with libBuildType=$buildType"
    ./gradlew ":$projectName:publishAllPublicationsToMavenCentralRepository" -PlibBuildType=$buildType --quiet --warning-mode=none
  else
    echo "Publishing $projectName"
    ./gradlew ":$projectName:publishAllPublicationsToMavenCentralRepository" --quiet --warning-mode=none
  fi
done

echo "Publishing completed successfully."