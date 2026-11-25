#! /bin/bash

set -eu

echo "--- :rubygems: Setting up Gems"
install_gems

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- :hammer_and_wrench: Build and Test"
bundle exec fastlane build_and_upload_prototype_build
