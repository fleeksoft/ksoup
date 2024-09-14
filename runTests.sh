#!/bin/bash

# Stop the script if any command fails
set -e

# Function to add wasm to platforms list if not already present
add_wasm_platform() {
    local module_file="ksoup/module.yaml"
    local test_module_file="ksoup-test/module.yaml"


    if grep -q 'platforms: \[.*wasm' "$module_file"; then
        echo "wasm is already in the platforms list in $module_file"
    else
        echo "Adding wasm to platforms list in $module_file"
        cp "$module_file" "$module_file.bak"
        sed -i.bak 's/\(platforms: \[\)/\1wasm, /' "$module_file"
    fi

    if grep -q 'platforms: \[.*wasm' "$test_module_file"; then
        echo "wasm is already in the platforms list in $test_module_file"
    else
        echo "Adding wasm to platforms list in $test_module_file"
        cp "$test_module_file" "$test_module_file.bak"
        sed -i.bak 's/\(platforms: \[\)/\1wasm, /' "$test_module_file"
    fi
}

# Function to restore the original module.yaml files
restore_module_yaml() {
    if [ -f "ksoup/module.yaml.bak" ]; then
      echo "ksoup/module.yaml restored"
      mv "ksoup/module.yaml.bak" "ksoup/module.yaml"
    fi
    if [ -f "ksoup-test/module.yaml.bak" ]; then
      echo "ksoup-test/module.yaml restored"
      mv "ksoup-test/module.yaml.bak" "ksoup-test/module.yaml"
    fi
}

# Error handler function to restore module.yaml on failure
error_handler() {
    echo "Error detected, restoring module.yaml if necessary..."
    restore_module_yaml
    exit 1
}

# Function to safely remove a directory if it exists
safe_remove_dir() {
    local dir="$1"
    if [ -d "$dir" ]; then
        rm -rf "$dir"
    fi
}

# Set trap to catch any errors and call the error_handler function
trap 'error_handler' ERR

# Function to run tests for a specific configuration
run_tests() {
    local libBuildType="$1"
    shift
    local tasks=("$@")

    if [ ${#tasks[@]} -eq 0 ]; then
      echo "No specific tasks provided, running all default tests..."
     # tasks=("jvmTest" "testDebugUnitTest" "testReleaseUnitTest" "jsTest" "wasmJsTest" "iosX64Test" "iosSimulatorArm64Test" "macosX64Test" "macosArm64Test" "tvosX64Test" "tvosSimulatorArm64Test")
    tasks=("jvmTest" "jsTest" "wasmJsTest")
   fi

     echo "Running tests with libBuildType=$libBuildType and tasks=${tasks[*]}..."

    # Only add/remove wasm for kotlinx and korlibs
    if [[ "$libBuildType" == "kotlinx" || "$libBuildType" == "korlibs" || "$libBuildType" == "lite" ]]; then
        add_wasm_platform
    fi

    # Remove build directories if they exist
    echo "remove build dirs if exists"
    safe_remove_dir ".kotlin"
    safe_remove_dir "build"
    safe_remove_dir ".gradle"

    ./gradlew clean -PlibBuildType="$libBuildType" --quiet --warning-mode=none

    for task in "${tasks[@]}"; do
      safe_remove_dir "kotlin-js-store" #remove it every task to avoid lock issue
      start_time=$(date +%s)
      echo "Running $task... $libBuildType"
      ./gradlew "$task" -PlibBuildType="$libBuildType" --quiet --warning-mode=none
      end_time=$(date +%s)
      duration=$((end_time - start_time))
      echo "Task $task completed in $duration seconds."
    done

    # Restore original module.yaml if wasm was added
    restore_module_yaml
}

# Supported parameters
SUPPORTED_PARAMS=("lite" "korlibs" "okio" "kotlinx" "ktor2")

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
    libBuildType="$1"
    shift

    if is_supported_param "$libBuildType"; then
        run_tests "$libBuildType" "$@"
    else
        echo "Error: Unsupported parameter '$libBuildType'. Supported parameters are: ${SUPPORTED_PARAMS[*]}"
        exit 1
    fi
else
    for param in "${SUPPORTED_PARAMS[@]}"; do
        run_tests "$param"
    done
fi

echo "All tests ran successfully!"
