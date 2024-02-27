#!/usr/bin/env bash
set -euo pipefail

# This script takes a file path and echos its contents into an analysis-token.txt file used by BasicBuildTracker.

if [[ "$#" -ne "1" ]]; then
  echo "usage: $0 <tokenfile>"
  exit 1
fi

tokenfile=$1

echo "Creating analysis-token.txt..."
cat $tokenfile > objectbox-code-modifier/src/main/resources/analysis-token.txt
echo "DONE"