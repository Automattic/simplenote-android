# frozen_string_literal: true

source "https://rubygems.org"
gem 'fastlane', '~> 2'
gem 'nokogiri'
# See https://stackoverflow.com/a/60491254/809944
gem 'bigdecimal', '~> 1.4'

plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)
