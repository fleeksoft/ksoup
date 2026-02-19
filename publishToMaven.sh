#!/bin/bash

# Stop the script if any command fails
set -e

# Default publishing task
PUBLISH_TASK="publishToMavenLocal"

# Check for the --remote flag
if [ "$1" == "--remote" ]; then
  PUBLISH_TASK="publishAllPublicationsToMavenCentralRepository"
  shift
fi

# Default build types if none are passed
default_build_types=("core" "io" "kotlinx" "okio" "ktor2")

# If build types are passed, use them; otherwise, use the default list
if [ "$#" -ge 1 ]; then
  build_types=("$@")
else
  build_types=("${default_build_types[@]}")
fi

# Function to add projects based on the key
add_projects_based_on_key() {
  local key="$1"
  case "$key" in
    "core")
      projects=("ksoup")
      ;;
    "io")
      projects=("ksoup-io")
      ;;
    "kotlinx")
      projects=("ksoup-kotlinx" "ksoup-network")
      ;;
    "ktor2")
      projects=("ksoup-network-ktor2")
      ;;
    "okio")
      projects=("ksoup-okio")
      ;;
    *)
      echo "Unknown key: $key"
      exit 1
      ;;
  esac
}

# Loop through all projects and publish them
for buildType in "${build_types[@]}"; do
  add_projects_based_on_key "$buildType"

  # clean build
#  echo "clean build"
#  ./gradlew clean --quiet --warning-mode=none

  for projectName in "${projects[@]}"; do
    echo "*****buildType: $buildType, project: $projectName"
    echo "Publishing $projectName with libBuildType=$buildType"
    ./gradlew ":$projectName:$PUBLISH_TASK" -PlibBuildType="$buildType" --quiet --warning-mode=none --no-configuration-cache
  done

done

echo "Publishing completed successfully."
