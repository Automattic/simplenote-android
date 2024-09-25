# frozen_string_literal: true

def create_backmerge_pr!
  pr_urls = create_backmerge_prs!

  return pr_urls unless pr_urls.length > 1

  backmerge_error_message = UI.user_error! <<~ERROR
    Unexpectedly opened more than one backmerge pull request. URLs:
    #{pr_urls.map { |url| "- #{url}" }.join("\n")}
  ERROR
  buildkite_annotate(style: 'error', context: 'error-creating-backmerge', message: backmerge_error_message) if is_ci
  UI.user_error!(backmerge_error_message)
end

# Notice the plural in the name.
# The action this method calls may create multiple backmerge PRs, depending on how many release branches with version greater than the source are in the remote.
def create_backmerge_prs!
  version = release_version_current

  create_release_backmerge_pull_request(
    repository: GITHUB_REPO,
    source_branch: release_branch_name(release_version: version),
    labels: ['Releases'],
    milestone_title: release_version_next
  )
rescue StandardError => e
  error_message = <<-MESSAGE
    Error creating backmerge pull request(s):

    #{e.message}

    If this is not the first time you are running the release task, the backmerge PR(s) for the version `#{version}` might have already been previously created.
    Please close any pre-existing backmerge PR for `#{version}`, delete the previous merge branch, then run the release task again.
  MESSAGE

  buildkite_annotate(style: 'error', context: 'error-creating-backmerge', message: error_message) if is_ci

  UI.user_error!(error_message)
end
