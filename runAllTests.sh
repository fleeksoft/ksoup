#!/bin/bash

# Stop the script if any command fails
set -e

# Function to run tests for a specific configuration
run_tests() {
    local libBuildType=$1

    echo "Running tests with libBuildType=$libBuildType..."

    ./gradlew clean
    ./gradlew jvmTest testDebugUnitTest testReleaseUnitTest -PlibBuildType="$libBuildType"
    rm -rf kotlin-js-store
    ./gradlew jsTest wasmTest -PlibBuildType="$libBuildType"
    ./gradlew iosX64Test iosSimulatorArm64Test macosX64Test macosArm64Test tvosX64Test tvosSimulatorArm64Test -PlibBuildType="$libBuildType"
}

# Run tests for kotlinx
run_tests kotlinx

# Run tests for korlibs
run_tests korlibs

echo "All tests run successfully!"