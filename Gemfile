# frozen_string_literal: true

source 'https://rubygems.org'

gem 'danger-dangermattic', '~> 1.3'
gem 'fastlane', '~> 2.236'
# This comment avoids typing to switch to a development version for testing.
#
# gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', ref: ''
gem 'fastlane-plugin-wpmreleasetoolkit', '~> 13.8'

# Security: https://github.com/lostisland/faraday/pull/1665
# Faraday 2.0 is not compatible with Fastlane
gem 'faraday', '~> 1.10'

# Pinned to pull in the fix for GHSA-c4rq-3m3g-8wgx (CSS selector ReDoS).
# Drop once `fastlane-plugin-wpmreleasetoolkit` moves to >= 14.4.1, whose
# gemspec carries this floor transitively.
gem 'nokogiri', '~> 1.19'

# To avoid errors like:
#
# SSL_connect returned=1 errno=0 peeraddr=3.5.132.155:443 state=error: certificate verify failed (unable to get certificate CRL)
#
# See:
#
# - https://github.com/ruby/openssl/issues/949
# - https://linear.app/a8c/issue/AINFRA-2538/upgrade-simplenote-release-tooling-to-at-least-address-ruby-ssl-issue
gem 'openssl', '~> 4.0'
