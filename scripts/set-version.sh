#!/usr/bin/env bash
set -euo pipefail

# This script has a regular and a `--release` mode.
#
# In regular mode this script will:
# - update `versionNumber` in `/build.gradle.kts`
# - on confirmation, commit the changes
# - suggest a git command to push the changes
#
# In release mode:
# - ask to confirm `versionNumber` in `/build.gradle.kts`
# - on confirmation, commit any changes, create a release tag
# - suggest git commands to push the changes

# Parse optional --release flag
releaseFlag=false
for arg in "$@"; do
    case "$arg" in
        --release) releaseFlag=true ;;
    esac
done

# macOS includes BSD versions of sed and grep, which have different options and syntax than the
# expected GNU versions. So require users of this script to install gsed and ggrep.
if [[ "$OSTYPE" == "darwin"* ]]; then
  if ! command -v gsed &>/dev/null; then
    echo "Error: gsed is required but not installed. Install it, for example using 'brew install gnu-sed'."
    exit 1
  fi
  if ! command -v ggrep &>/dev/null; then
    echo "Error: ggrep is required but not installed. Install it, for example using 'brew install grep'."
    exit 1
  fi
  sed="gsed"
  grep="ggrep"
else
  sed="sed"
  grep="grep"
fi

buildScriptFile="../build.gradle.kts"
propVersionNumber="versionNumber"

# Extract the value of `versionNumber` in build.gradle.kts and store it in the versionCurrent variable
buildScriptPath="$(dirname "$0")/$buildScriptFile"
# Regex matches 'versionNumber = "..."', \K to only capture the version string inside the quotes
versionCurrent=$($grep --only-matching --perl-regexp "$propVersionNumber"' = "\K[^"]+' "$buildScriptPath" || true)
if [[ -z "$versionCurrent" ]]; then
    echo "Error: could not find '$propVersionNumber' in '$buildScriptFile'"
    exit 1
fi

echo ""
echo "Maven artifacts version ($propVersionNumber in $buildScriptFile)"
echo "Current:    $versionCurrent"

if $releaseFlag; then
    # Release: Confirm the current version
    read -r -p "Press enter to confirm the current version, or enter a custom one: " versionInput
    if [[ -n "$versionInput" ]]; then
        versionNew="$versionInput"
    else
        versionNew="$versionCurrent"
    fi
else
    # Suggest a next version
    # Increment the last number in the version string (like 5.4.2 -> 5.4.3, 5.4.2-preview1 -> 5.4.2-preview2)
    # Regex: captures the trailing digits and replaces them with a value increased by 1
    [[ "$versionCurrent" =~ ^(.*[^0-9])([0-9]+)$ ]] || { echo "Error: $propVersionNumber '$versionCurrent' does not end with a number"; exit 1; }
    versionSuggested="${BASH_REMATCH[1]}$(( BASH_REMATCH[2] + 1 ))"
    echo "Suggested:  $versionSuggested"
    read -r -p "Press enter to use the suggested version, or enter a custom one: " versionInput
    if [[ -n "$versionInput" ]]; then
        versionNew="$versionInput"
    else
        versionNew="$versionSuggested"
    fi
fi
echo "Version will be $versionNew"

# Change the value of `versionNumber` in build.gradle.kts to the value of versionNew
$sed --in-place "s/$propVersionNumber = \"$versionCurrent\"/$propVersionNumber = \"$versionNew\"/" "$buildScriptPath"

# After confirmation commit any changes and for a release create a tag
# git commit --all --message="Publishing: increase version 5.4.0 -> 5.4.1"
# Also for a release:
# git tag V5.4.0
commitMessage="Publishing: increase version $versionCurrent -> $versionNew"
tagRelease="V$versionNew"

versionChanged=true
if [[ "$versionNew" == "$versionCurrent" ]]; then
    versionChanged=false
fi

echo ""
echo "About to run git commands:"
if $versionChanged; then
    echo "  git commit --all --message=\"$commitMessage\""
fi
if $releaseFlag; then
    echo "  git tag $tagRelease"
fi

read -r -p "Reviewed changes? Proceed? [y/N] " confirm
if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Aborted."
    exit 1
fi

if $versionChanged; then
    git commit --all --message="$commitMessage"
fi
if $releaseFlag; then
    git tag "$tagRelease"
fi

# Print suggested git commands to push the changes
# Note: add `--push-option=ci.skip` to avoid running CI
# Examples:
# git push origin publish --push-option=ci.skip
# git push origin V5.4.0 --push-option=ci.skip

currentBranch=$(git rev-parse --abbrev-ref HEAD)
echo ""
echo "Suggested commands to push these changes:"
echo "  git push origin $currentBranch --push-option=ci.skip"
if $releaseFlag; then
    echo "  git push origin $tagRelease --push-option=ci.skip"
fi
