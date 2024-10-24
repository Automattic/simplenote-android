#!/bin/bash -eu

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Building app"
bundle exec fastlane build_for_distribution

echo "--- :computer: Use deterministic APK name for next steps"

OUTPUT_DIR="build"
if [ "$(find "$OUTPUT_DIR" -maxdepth 1 -name "*.apk" | wc -l)" -gt 1 ]; then
  echo "Found more than one APK in $OUTPUT_DIR."
  exit 1
fi

ORIGINAL_APK_PATH=$(find "$OUTPUT_DIR" -name "*.apk" -maxdepth 1 | head -1)

set -x
mv "$ORIGINAL_APK_PATH" $OUTPUT_DIR/simplenote.apk
set +x
