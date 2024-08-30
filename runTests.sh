#!/bin/bash

# Stop the script if any command fails
set -e

# Function to add wasm to platforms list if not already present
add_wasm_platform() {
    local module_file="ksoup/module.yaml"

    if grep -q 'platforms: \[.*wasm' "$module_file"; then
        echo "wasm is already in the platforms list in $module_file"
        return 0
    else
        echo "Adding wasm to platforms list in $module_file"
        cp "$module_file" "$module_file.bak"
        sed -i.bak 's/\(platforms: \[\)/\1wasm, /' "$module_file"
        return 1
    fi
}

# Function to restore the original module.yaml file
restore_module_yaml() {
    echo "Restoring original module.yaml..."
    mv "ksoup/module.yaml.bak" "ksoup/module.yaml"
}

# Error handler function to restore module.yaml on failure
error_handler() {
    echo "Error detected, restoring module.yaml if necessary..."
    if [ -f "ksoup/module.yaml.bak" ]; then
        restore_module_yaml
    fi
    exit 1
}

# Set trap to catch any errors and call the error_handler function
trap 'error_handler' ERR

# Function to run tests for a specific configuration
run_tests() {
    local libBuildType="$1"

    echo "Running tests with libBuildType=$libBuildType..."

    # Only add/remove wasm for kotlinx and korlibs
    local wasm_added=0
    if [[ "$libBuildType" == "kotlinx" || "$libBuildType" == "korlibs" ]]; then
        trap - ERR # Temporarily disable the trap
        set +e  # Disable exit on error
        add_wasm_platform
        wasm_added=$?
        set -e  # Re-enable exit on error
        trap 'error_handler' ERR # Re-enable the trap
    fi

    ./gradlew clean --quiet --warning-mode=none

    echo "Running JVM tests... $libBuildType"
    ./gradlew jvmTest testDebugUnitTest testReleaseUnitTest -PlibBuildType="$libBuildType" --quiet --warning-mode=none

    echo "Running JS and WASM tests... $libBuildType"
    rm -rf kotlin-js-store
    ./gradlew jsTest wasmTest -PlibBuildType="$libBuildType" --quiet --warning-mode=none

    echo "Running iOS, macOS, and tvOS tests... $libBuildType"
    ./gradlew iosX64Test iosSimulatorArm64Test macosX64Test macosArm64Test tvosX64Test tvosSimulatorArm64Test -PlibBuildType="$libBuildType" --quiet --warning-mode=none

    # Restore original module.yaml if wasm was added
    if [ "$wasm_added" -eq 1 ]; then
        restore_module_yaml
    fi
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
