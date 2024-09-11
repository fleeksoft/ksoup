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
  shift
fi

# Default build types if none are passed
default_build_types=("common" "kotlinx" "korlibs" "ktor2" "okio")

# If build types are passed, use them; otherwise, use the default list
if [ "$#" -ge 1 ]; then
  build_types=("$@")
else
  build_types=("${default_build_types[@]}")
fi

# Function to add projects based on the key
add_projects_based_on_key() {
  local key="$1"
  case "$key" in
    "common")
      projects=("ksoup-engine-common")
      ;;
    "kotlinx")
      projects=("ksoup-engine-kotlinx" "ksoup" "ksoup-network")
      ;;
    "korlibs")
      projects=("ksoup-engine-korlibs" "ksoup" "ksoup-network-korlibs")
      ;;
    "ktor2")
      projects=("ksoup-engine-ktor2" "ksoup" "ksoup-network-ktor2")
      ;;
    "okio")
      projects=("ksoup-engine-okio" "ksoup")
      ;;
    *)
      echo "Unknown key: $key"
      exit 1
      ;;
  esac
}


# Function to add wasm to platforms list if not already present
add_wasm_platform() {
  local module_file="$1/module.yaml"

  # Check if 'platforms:' line already contains 'wasm'
  if grep -q 'platforms: \[.*wasm' "$module_file"; then
    echo "wasm is already in the platforms list in $module_file"
  else
    echo "Adding wasm to platforms list in $module_file"
    cp "$module_file" "$module_file.bak"
    # Add 'wasm' to the beginning of the platforms list
    sed -i.bak 's/\(platforms: \[\)/\1wasm, /' "$module_file"
  fi
}

# Function to restore the original module.yaml file
restore_module_yaml() {
  if [ -f "$1/module.yaml.bak" ]; then
      echo "Restoring original module.yaml in $1"
      mv "$1/module.yaml.bak" "$1/module.yaml"
  fi
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
for buildType in "${build_types[@]}"; do
  add_projects_based_on_key "$buildType"

  # Remove build directories if they exist
  echo "remove build dirs if exists"
  safe_remove_dir ".kotlin"
  safe_remove_dir "build"
  safe_remove_dir ".gradle"
  safe_remove_dir "kotlin-js-store"

  if [ "$ADD_WASM" = true ] && [[ "$buildType" == "kotlinx" || "$buildType" == "korlibs" ]]; then
    echo "check and add wasm to projects"
    for projectName in "${projects[@]}"; do
      add_wasm_platform "$projectName"
    done
  fi

  ./gradlew clean -PlibBuildType="$buildType" --quiet --warning-mode=none

  for projectName in "${projects[@]}"; do
    echo "*****buildType: $buildType, project: $projectName"
    echo "Publishing $projectName with libBuildType=$buildType"
    ./gradlew ":$projectName:$PUBLISH_TASK" -PlibBuildType="$buildType" --quiet --warning-mode=none --no-configuration-cache
  done

  echo "check and restore module.yaml if required"
  for projectName in "${projects[@]}"; do
    restore_module_yaml "$projectName"
  done
done

echo "Publishing completed successfully."
