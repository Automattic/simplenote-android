#!/usr/bin/env bash

set -euo pipefail

decrypted_paths=(
  Simplenote/gradle.properties
  .configure-files/release.jks
  .configure-files/debug_a8c.keystore
  "$HOME/.configure/simplenote-android/secrets"
)

assert_no_decrypted_files() {
  local found=0
  for path in "${decrypted_paths[@]}"; do
    if [[ -e "$path" ]]; then
      echo "FAIL: decrypted path exists: $path"
      found=1
    fi
  done
  [[ "$found" -eq 0 ]] || exit 1
  echo "PASS: no decrypted files present"
}

echo "--- :rubygems: Setting up Gems"
install_gems

export GIT_AUTHOR_NAME=validation GIT_AUTHOR_EMAIL=validation@example.com
export GIT_COMMITTER_NAME=validation GIT_COMMITTER_EMAIL=validation@example.com

echo "--- :mag: The changed paths no longer decode secrets"
for file in \
  .buildkite/commands/release-create-on-github.sh \
  .buildkite/release-pipelines/complete-code-freeze.yml \
  .buildkite/release-pipelines/new-beta-release.yml \
  fastlane/lanes/release.rb; do
  if grep -n configure_apply "$file"; then
    echo "FAIL: $file still calls configure_apply"
    exit 1
  fi
done
echo "PASS: no configure_apply left in the four changed paths"

echo "--- :no_entry: Remove the encryption key"
unset CONFIGURE_ENCRYPTION_KEY
assert_no_decrypted_files

echo "--- :x: Negative case: decoding must be impossible in this job"
if output="$(bundle exec fastlane run configure_apply 2>&1)"; then
  echo "$output"
  echo "FAIL: configure_apply succeeded, so this job still has access to the key"
  exit 1
fi
echo "$output"
if ! grep -q -E 'Decryption key could not be found|keys\.json' <<<"$output"; then
  echo "FAIL: configure_apply failed, but not because the key is missing"
  exit 1
fi
echo "PASS: configure_apply failed because the key is missing"
assert_no_decrypted_files

start_commit="$(git rev-parse HEAD)"

echo "--- :shipit: complete_code_freeze work"
bundle exec fastlane update_play_store_strings
assert_no_decrypted_files

echo "--- :shipit: new_beta_release and finalize_release work"
bundle exec fastlane download_translations run_lint:false
bundle exec fastlane download_metadata_strings
assert_no_decrypted_files

echo "--- :mag: Local commits the lanes made, never pushed"
git log --oneline "$start_commit..HEAD"
git diff --stat "$start_commit" HEAD | tail -5
