#!/bin/bash

# Stop the script if any command fails
set -e

./gradlew clean
./gradlew :ksoup-engine-common:publishAllPublicationsToMavenCentralRepository
./gradlew clean
./gradlew :ksoup-engine-kotlinx:publishAllPublicationsToMavenCentralRepository -PlibBuildType=kotlinx
./gradlew clean
./gradlew :ksoup-engine-korlibs:publishAllPublicationsToMavenCentralRepository -PlibBuildType=korlibs

./gradlew clean
./gradlew :ksoup:publishAllPublicationsToMavenCentralRepository -PlibBuildType=korlibs
./gradlew :ksoup-network-korlibs:publishAllPublicationsToMavenCentralRepository -PlibBuildType=korlibs

./gradlew clean
./gradlew :ksoup-network:publishAllPublicationsToMavenCentralRepository -PlibBuildType=kotlinx
./gradlew :ksoup:publishAllPublicationsToMavenCentralRepository -PlibBuildType=kotlinx

echo "Publishing completed successfully."