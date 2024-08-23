#!/bin/bash

# Stop the script if any command fails
set -e

./gradlew clean
./gradlew :ksoup-engine-common:publishToMavenLocal
./gradlew :ksoup-engine-kotlinx:publishToMavenLocal
./gradlew :ksoup-engine-korlibs:publishToMavenLocal

./gradlew clean
./gradlew :ksoup:publishToMavenLocal -PisKorlibs=false
./gradlew :ksoup-network:publishToMavenLocal -PisKorlibs=false

./gradlew clean
./gradlew :ksoup:publishToMavenLocal -PisKorlibs=true
./gradlew :ksoup-network-korlibs:publishToMavenLocal -PisKorlibs=true

echo "Publishing completed successfully."