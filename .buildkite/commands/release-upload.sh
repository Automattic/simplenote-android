#!/bin/bash -eu

echo "--- :arrow_down: Downloading Artifacts"
ARTIFACT_PATH='build/simplenote.apk' # Must be the same as release-build.sh
STEP=build
buildkite-agent artifact download "$ARTIFACT_PATH" . --step $STEP

echo "--- :rubygems: Setting up Gems"
install_gems

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- :android: Upload to Play Store"
bundle exec fastlane upload_build_to_play_store \
  apk_path:"$ARTIFACT_PATH" \
  "beta:${1:-true}" # use first call param, default to true for safety
