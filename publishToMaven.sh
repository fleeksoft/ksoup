#!/bin/bash

# Stop the script if any command fails
set -e

./gradlew clean
./gradlew :ksoup-engine-common:publishAllPublicationsToMavenCentralRepository
./gradlew clean
./gradlew :ksoup-engine-kotlinx:publishAllPublicationsToMavenCentralRepository -PisKorlibs=false
./gradlew clean
./gradlew :ksoup-engine-korlibs:publishAllPublicationsToMavenCentralRepository -PisKorlibs=true

./gradlew clean
./gradlew :ksoup:publishAllPublicationsToMavenCentralRepository -PisKorlibs=false
./gradlew :ksoup-network:publishAllPublicationsToMavenCentralRepository -PisKorlibs=false

./gradlew clean
./gradlew :ksoup:publishAllPublicationsToMavenCentralRepository -PisKorlibs=true
./gradlew :ksoup-network-korlibs:publishAllPublicationsToMavenCentralRepository -PisKorlibs=true

echo "Publishing completed successfully."