#!/bin/bash

# Stop the script if any command fails
set -e

# Function to run tests for a specific configuration
run_tests() {
    local libBuildType="$1"

    echo "Running tests with libBuildType=$libBuildType..."

    ./gradlew clean --quiet --warning-mode=none

    echo "Running JVM tests... $libBuildType"
    ./gradlew jvmTest testDebugUnitTest testReleaseUnitTest -PlibBuildType="$libBuildType" --quiet --warning-mode=none

    echo "Running JS and WASM tests... $libBuildType"
    rm -rf kotlin-js-store
    ./gradlew jsTest wasmTest -PlibBuildType="$libBuildType" --quiet --warning-mode=none

     echo "Running iOS, macOS, and tvOS tests... $libBuildType"
     ./gradlew iosX64Test iosSimulatorArm64Test macosX64Test macosArm64Test tvosX64Test tvosSimulatorArm64Test -PlibBuildType="$libBuildType" --quiet --warning-mode=none
}

# Supported parameters
SUPPORTED_PARAMS=("korlibs" "okio" "kotlinx" "ktor2")

# Function to check if the provided parameter is supported
is_supported_param() {
    local param="$1"
    for supported_param in "${SUPPORTED_PARAMS[@]}"; do
        if [ "$supported_param" == "$param" ]; then
            return 0
        fi
    done
    return 1
}

# Main script logic
if [ "$#" -ge 1 ]; then
    for param in "$@"; do
        if is_supported_param "$param"; then
            run_tests "$param"
        else
            echo "Error: Unsupported parameter '$param'. Supported parameters are: ${SUPPORTED_PARAMS[*]}"
            exit 1
        fi
    done
else
    for param in "${SUPPORTED_PARAMS[@]}"; do
        run_tests "$param"
    done
fi

echo "All tests ran successfully!"
