#!/usr/bin/env bash
set -euo pipefail

# This script takes a string token and replaces the placeholder value in BasicBuildTracker.TOKEN with it.

if [[ "$#" -ne "1" ]]; then
  echo "usage: $0 <token>"
  exit 1
fi

token=$1

echo "Setting TOKEN in BasicBuildTracker..."
sed -i "s/REPLACE_WITH_TOKEN/${token}/g" objectbox-code-modifier/src/main/kotlin/io/objectbox/reporting/BasicBuildTracker.kt
echo "DONE"