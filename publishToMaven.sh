#!/bin/bash

# Stop the script if any command fails
set -e

# Flag to determine whether to add wasm or not
ADD_WASM=true

# Default publishing task
PUBLISH_TASK="publishToMavenLocal"

# Check for the --remote flag
if [ "$1" == "--remote" ]; then
  PUBLISH_TASK="publishAllPublicationsToMavenCentralRepository"
  shift # Remove the --remote argument
fi

# projectModule:libBuildType
default_projects=(
  "ksoup-engine-common:"

  "ksoup-engine-kotlinx:kotlinx"
  "ksoup:kotlinx"
  "ksoup-network:kotlinx"

  "ksoup-engine-korlibs:korlibs"
  "ksoup:korlibs"
  "ksoup-network-korlibs:korlibs"

  "ksoup-engine-ktor2:ktor2"
  "ksoup:ktor2"
  "ksoup-network-ktor2:ktor2"

  "ksoup-engine-okio:okio"
  "ksoup:okio"
)

  # Check if projects were passed as arguments
  if [ "$#" -ne 0 ]; then
    projects=("$@")
  else
    projects=("${default_projects[@]}")
  fi

# Function to add wasm to platforms list if not already present
add_wasm_platform() {
  local module_file="$1/module.yaml"

  # Check if 'platforms:' line already contains 'wasm'
  if grep -q 'platforms: \[.*wasm' "$module_file"; then
    echo "wasm is already in the platforms list in $module_file"
    return 0  # Return 0 (indicating wasm was not added)
  else
    echo "Adding wasm to platforms list in $module_file"
    cp "$module_file" "$module_file.bak"
    # Add 'wasm' to the beginning of the platforms list
    sed -i.bak 's/\(platforms: \[\)/\1wasm, /' "$module_file"
    return 1  # Return 1 (indicating wasm was added)
  fi
}

# Function to restore the original module.yaml file
restore_module_yaml() {
  echo "Restoring original module.yaml in $1"
  mv "$1/module.yaml.bak" "$1/module.yaml"
}

# Function to handle errors and restore the original file if needed
error_handler() {
  echo "Error detected, check for restore of module.yaml.bak if needed for $projectName"
  if [ -f "$projectName/module.yaml.bak" ]; then
    restore_module_yaml "$projectName"
  fi
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

# Loop through all projects and publish them
for project in "${projects[@]}"; do
  # Split the project name and build type
  IFS=":" read -r projectName buildType <<< "$project"

  wasm_added=0

  if [ "$ADD_WASM" = true ] && [[ "$buildType" == "kotlinx" || "$buildType" == "korlibs" ]]; then
    trap - ERR # Temporarily disable the trap
    set +e  # Disable exit on error
    # Add wasm to platforms list if buildType is kotlinx or korlibs
    add_wasm_platform "$projectName"
    wasm_added=$?  # Capture the return value indicate if wasm added
    set -e  # Re-enable exit on error
    trap 'error_handler' ERR # Re-enable the trap
  else
    wasm_added=0  # Set to false if wasm wasn't added
  fi

  if [ -n "$buildType" ]; then

    # Remove build directories if they exist
    echo "remove build dirs if exists"
    safe_remove_dir ".kotlin"
    safe_remove_dir "build"
    safe_remove_dir ".gradle"
    safe_remove_dir "kotlin-js-store"

    ./gradlew clean -PlibBuildType="$buildType" --quiet --warning-mode=none
    echo "Publishing $projectName with libBuildType=$buildType"
    ./gradlew ":$projectName:$PUBLISH_TASK" -PlibBuildType="$buildType" --quiet --warning-mode=none
  else
    ./gradlew clean --quiet --warning-mode=none
    echo "Publishing $projectName"
    ./gradlew ":$projectName:$PUBLISH_TASK" --quiet --warning-mode=none
  fi

  # Restore the original module.yaml file after publishing if wasm was added
  if [ "$wasm_added" -eq 1 ]; then
    restore_module_yaml "$projectName"
  fi
done

echo "Publishing completed successfully."
