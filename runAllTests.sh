#!/bin/bash

# Stop the script if any command fails
set -e

# Function to run tests for a specific configuration
run_tests() {
    local isKorlibs=$1

    echo "Running tests with isKorlibs=$isKorlibs..."

    ./gradlew clean
    ./gradlew jvmTest testDebugUnitTest testReleaseUnitTest -PisKorlibs=$isKorlibs
    ./gradlew iosX64Test iosSimulatorArm64Test macosX64Test macosArm64Test tvosX64Test tvosSimulatorArm64Test -PisKorlibs=$isKorlibs
    ./gradlew jsTest wasmTest -PisKorlibs=$isKorlibs
}

# Run tests for isKorlibs=false
run_tests false

# Run tests for isKorlibs=true
run_tests true

echo "All tests run successfully!"