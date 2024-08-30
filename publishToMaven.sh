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
fi

# projectModule:libBuildType
projects=(
  "ksoup-engine-common:"
  "ksoup-engine-kotlinx:"
  "ksoup-engine-korlibs:"
  "ksoup-engine-ktor2:"
  "ksoup-engine-okio:"
  "ksoup:kotlinx"
  "ksoup-network:kotlinx"
  "ksoup:korlibs"
  "ksoup-network-korlibs:korlibs"
  "ksoup:ktor2"
  "ksoup-network-ktor2:ktor2"
  "ksoup:okio"
)


# Function to add wasm to platforms list if not already present
add_wasm_platform() {
  local module_file="$1/module.yaml"

  # Check if 'platforms:' line contains only 'wasm'
  if grep -q 'platforms: \[.*wasm' "$module_file"; then
    echo "wasm is already in the platforms list in $module_file"
    return 0  # Return 0 (indicating wasm was not added)
  else
    echo "Adding wasm to platforms list in $module_file"
    cp "$module_file" "$module_file.bak"
    # Add 'wasm' to the beginning of the platforms list
    sed -i.bak '/platforms:/ s/\[wasm, /&/' "$module_file"
    return 1  # Return 1 (indicating wasm was added)
  fi
}

# Function to restore the original module.yaml file
restore_module_yaml() {
  echo "Restoring original module.yaml in $1"
  mv "$1/module.yaml.bak" "$1/module.yaml"
}

# Loop through all projects and publish them
for project in "${projects[@]}"; do
  # Split the project name and build type
  IFS=":" read -r projectName buildType <<< "$project"

  if [ "$ADD_WASM" = true ] && [[ "$buildType" == "kotlinx" || "$buildType" == "korlibs" ]]; then
    # Add wasm to platforms list if buildType is kotlinx or korlibs
    set +e  # Disable exit on error
    add_wasm_platform "$projectName"
    wasm_added=$?  # Capture the return value indicate if wasm added
    set -e  # Re-enable exit on error
  else
    wasm_added=0  # Set to false if wasm wasn't added
  fi

  ./gradlew clean --quiet --warning-mode=none
  if [ -n "$buildType" ]; then
    echo "Publishing $projectName with libBuildType=$buildType"
    ./gradlew ":$projectName:$PUBLISH_TASK" -PlibBuildType="$buildType" --quiet --warning-mode=none
  else
    echo "Publishing $projectName"
    ./gradlew ":$projectName:$PUBLISH_TASK" --quiet --warning-mode=none
  fi

  # Restore the original module.yaml file after publishing if wasm was added
  if [ "$wasm_added" -eq 1 ]; then
    restore_module_yaml "$projectName"
  fi
done

echo "Publishing completed successfully."