#!/bin/bash

# Stop the script if any command fails
set -e

./gradlew clean
./gradlew :ksoup-engine-common:publishToMavenLocal
./gradlew :ksoup-engine-kotlinx:publishToMavenLocal
./gradlew :ksoup-engine-korlibs:publishToMavenLocal

./gradlew clean
./gradlew :ksoup:publishToMavenLocal -PlibBuildType=kotlinx
./gradlew :ksoup-network:publishToMavenLocal -PlibBuildType=kotlinx

./gradlew clean
./gradlew :ksoup:publishToMavenLocal -PlibBuildType=korlibs
./gradlew :ksoup-network-korlibs:publishToMavenLocal -PlibBuildType=korlibs

echo "Publishing completed successfully."