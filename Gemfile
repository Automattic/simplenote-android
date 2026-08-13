# frozen_string_literal: true

source 'https://rubygems.org'

gem 'danger-dangermattic', '~> 1.4'
gem 'fastlane', '~> 2.237'
# This comment avoids typing to switch to a development version for testing.
#
# gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', ref: ''
gem 'fastlane-plugin-wpmreleasetoolkit', '~> 14.11'

# Security: https://github.com/lostisland/faraday/pull/1665
# Faraday 2.0 is not compatible with Fastlane
gem 'faraday', '~> 2.14'

# To avoid errors like:
#
# SSL_connect returned=1 errno=0 peeraddr=3.5.132.155:443 state=error: certificate verify failed (unable to get certificate CRL)
#
# See:
#
# - https://github.com/ruby/openssl/issues/949
# - https://linear.app/a8c/issue/AINFRA-2538/upgrade-simplenote-release-tooling-to-at-least-address-ruby-ssl-issue
gem 'openssl', '~> 4.0'
