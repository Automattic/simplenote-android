#!/usr/bin/env bash

set -euo pipefail

secrets_dir="$HOME/.configure/simplenote-android/secrets"

echo "Agent: ${BUILDKITE_AGENT_NAME:-unknown} on $(hostname)"
echo "Now: $(date -u)"

if [[ ! -d "$secrets_dir" ]]; then
  echo "FAIL: $secrets_dir does not exist on this agent"
  exit 1
fi

ls -la "$secrets_dir"

# This job never decodes anything, so a file older than a day outlived every cleanup since it was written.
leftovers=$(find "$secrets_dir" -type f -mtime +1)

if [[ -z "$leftovers" ]]; then
  echo "FAIL: nothing older than a day in $secrets_dir"
  exit 1
fi

echo "PASS: files outlived the cleanup of earlier jobs:"
echo "$leftovers"
