#! /bin/bash

set -eu

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- Installing Secrets"
# This is a hack until our AMI has a newer version of glibc and openSSL
./gradlew
chmod +x vendor/configure/configure
docker run -it --rm --workdir /app --env CONFIGURE_ENCRYPTION_KEY -v $(pwd):/app public.ecr.aws/automattic/android-build-image:4281c9e97b2d821df3de34c046b7c067499b35bb /bin/bash -c 'vendor/configure/configure apply'

echo "--- :hammer_and_wrench: Build and Test"
bundle exec fastlane build_and_upload_installable_build
