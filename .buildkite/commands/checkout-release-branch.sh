#!/bin/bash -eu

if [[ -z "${RELEASE_VERSION}" ]]; then
    echo "RELEASE_VERSION is not set."
    exit 1
fi

# Buildkite, by default, checks out a specific commit.
# For many release actions, we need to be on a release branch instead.
BRANCH_NAME="release/${RELEASE_VERSION}"
git fetch origin "$BRANCH_NAME"
git checkout "$BRANCH_NAME"
# Buildkite can reuse a working copy where "$BRANCH_NAME" was left at an older commit by a previous job,
# so force the local branch to the fetched commit. `reset --hard` rather than
# `git pull`, to avoid merging if the two diverged; `FETCH_HEAD` rather than
# `origin/$BRANCH_NAME`, which `git fetch <branch>` only updates opportunistically.
git reset --hard FETCH_HEAD
