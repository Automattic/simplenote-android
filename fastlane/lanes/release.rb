# frozen_string_literal: true

# Lanes related to the Release Process (Code Freeze, Betas, Final Build, App Store Submission…)

platform :android do
  desc 'Creates a new release branch from the current default branch'
  lane :start_code_freeze do |skip_prechecks: false, skip_confirm: false|
    ensure_git_status_clean unless skip_prechecks || is_ci

    Fastlane::Helper::GitHelper.checkout_and_pull(DEFAULT_BRANCH)

    new_version_with_beta = release_version_for_code_freeze
    new_version_final = release_version_next
    new_build_code = build_code_next
    computed_release_branch_name = release_branch_name(release_version: new_version_final)

    message = <<~MESSAGE
      Code Freeze:
      - New release branch from #{DEFAULT_BRANCH}: #{computed_release_branch_name}
      - Current release version and build code: #{release_version_current} (#{build_code_current}).
      - New beta version and build code: #{new_version_with_beta} (#{new_build_code}).
    MESSAGE
    UI.important(message)

    unless skip_confirm || UI.confirm('Do you want to continue?')
      UI.user_error!("Terminating as requested. Don't forget to run the remainder of this automation manually.")
      next
    end

    UI.message 'Creating release branch...'
    Fastlane::Helper::GitHelper.create_branch(computed_release_branch_name)
    UI.success("Done! New release branch is: #{git_branch}.")

    UI.message 'Bumping beta version and build code...'
    VERSION_FILE.write_version(
      version_name: new_version_with_beta,
      version_code: new_build_code
    )
    commit_version_bump

    new_version = release_version_current
    UI.success("Done! New beta version: #{new_version}. New build code: #{build_code_current}.")

    extract_release_notes_for_version(
      version: new_version,
      release_notes_file_path: RELEASE_NOTES_SOURCE_PATH,
      extracted_notes_file_path: RELEASE_NOTES_PATH
    )
    add_version_section_to_dev_release_notes(version: new_version)

    update_strings_for_translation_automation

    unless skip_confirm || UI.confirm("Push the new #{computed_release_branch_name} branch the remote and let the automation configure branch protection and milestone on GitHub?")
      UI.user_error!("Terminating as requested. Don't forget to run the remainder of this automation manually.")
      next
    end

    push_to_git_remote(
      tags: false,
      set_upstream: is_ci == false # only set upstream when running locally, useless in transient CI builds
    )

    copy_branch_protection(
      repository: GITHUB_REPO,
      from_branch: DEFAULT_BRANCH,
      to_branch: computed_release_branch_name
    )

    freeze_milestone_and_move_assigned_prs_to_next_milestone(
      milestone_to_freeze: new_version_final,
      next_milestone: release_version_next
    )

    next unless is_ci

    message = <<~MESSAGE
      Code freeze started successfully.

      Next steps:

      - Checkout `#{release_branch_name}` branch locally
      - Update the simperium dependency to a stable version if needed
      - Update the release notes that were extracted from RELEASE-NOTES.txt if appropriate
      - Finalize the code freeze
    MESSAGE
    buildkite_annotate(context: 'code-freeze-success', style: 'success', message: message)
  end

  lane :complete_code_freeze do |skip_prechecks: false, skip_confirm: false|
    ensure_git_branch_is_release_branch! unless skip_prechecks || is_ci
    ensure_git_status_clean

    version = release_version_current

    UI.important("Completing code freeze for: #{version}")

    UI.user_error!('Aborted by user request') unless skip_confirm || UI.confirm('Do you want to continue?')

    update_play_store_strings
    delete_old_changelogs_and_commit(version: version)

    unless skip_confirm || UI.confirm('Ready to push changes to remote and trigger the beta build?')
      UI.message("Terminating as requested. Don't forget to run the remainder of this automation manually.")
      next
    end

    push_to_git_remote(tags: false)

    trigger_beta_build(branch_to_build: release_branch_name(release_version: version))

    pr_url = create_backmerge_pr!

    message = <<~MESSAGE
      Code freeze completed successfully. Next, review and merge the [integration PR](#{pr_url}).
    MESSAGE
    buildkite_annotate(context: 'code-freeze-completed', style: 'success', message: message) if is_ci
    UI.success(message)
  end

  desc 'Updates store metadata and runs the release checks'
  lane :finalize_release do |skip_confirm: false|
    UI.user_error!('Please use `finalize_hotfix_release` lane for hotfixes') if android_current_branch_is_hotfix(version_properties_path: VERSION_PROPERTIES_PATH)

    ensure_git_status_clean
    ensure_git_branch_is_release_branch!

    UI.important("Finalizing release: #{release_version_current}")
    UI.user_error!("Terminating as requested. Don't forget to run the remainder of this automation manually.") unless skip_confirm || UI.confirm('Do you want to continue?')

    configure_apply(force: is_ci)

    check_translation_progress_all unless is_ci
    download_translations

    UI.message 'Bumping final release version and build code...'
    VERSION_FILE.write_version(
      version_name: release_version_current,
      version_code: build_code_next
    )
    commit_version_bump

    # Print computed version and build to let user double-check outcome in logs
    version = release_version_current
    build_code = build_code_current
    UI.success("Done! Final release version: #{version}. Final build code: #{build_code}.")

    download_metadata_strings

    UI.important('Will push changes to remote and trigger the release build.')
    UI.user_error!("Terminating as requested. Don't forget to run the remainder of this automation manually.") unless skip_confirm || UI.confirm('Do you want to continue?')

    push_to_git_remote(tags: false)

    build_and_upload_release(create_release: true)

    pr_url = create_backmerge_pr!

    message = <<~MESSAGE
      Release finalized successfully. Next, review and merge the [integration PR](#{pr_url}).
    MESSAGE
    buildkite_annotate(context: 'finalize-release-completed', style: 'success', message: message) if is_ci
    UI.success(message)

    UI.message('Attempting to remove release branch protection in GitHub...')

    begin
      set_milestone_frozen_marker(
        repository: GITHUB_REPO,
        milestone: version,
        freeze: false
      )
      close_milestone(
        repository: GITHUB_REPO,
        milestone: version
      )
    rescue StandardError => e
      report_milestone_error(error_title: "Error in milestone finalization process for `#{version}`: #{e.message}")
    end
  end

  lane :publish_release do |skip_confirm: false|
    ensure_git_status_clean
    ensure_git_branch_is_release_branch!

    version_number = release_version_current

    current_branch = release_branch_name(release_version: version_number)
    next_release_branch = release_branch_name(release_version: release_version_next)

    UI.important <<~PROMPT
      Publish the #{version_number} release. This will:
      - Publish the existing draft `#{version_number}` release on GitHub
      - Which will also have GitHub create the associated Git tag, pointing to the tip of #{current_branch}
      - If the release branch for the next version `#{next_release_branch}` already exists, backmerge `#{current_branch}` into it
      - If needed, backmerge `#{current_branch}` back into `#{DEFAULT_BRANCH}`
      - Delete the `#{current_branch}` branch
    PROMPT
    UI.user_error!("Terminating as requested. Don't forget to run the remainder of this automation manually.") unless skip_confirm || UI.confirm('Do you want to continue?')

    UI.important "Publishing release #{version_number} on GitHub..."

    publish_github_release(
      repository: GITHUB_REPO,
      name: version_number
    )

    pr_urls = create_backmerge_prs!

    # It's possible that no backmerge was created when:
    #
    # - there are no hotfixes in development and the next release code freeze has not been started
    # - nothing changes in the current release branch since release finalization
    #
    # As a matter of fact, in the context of Simplenote Android, the above is the most likely scenario.
    style, message = if pr_urls.empty?
                       ['info', 'No backmerge PR was required']
                     else
                       [
                         'success', <<~MESSAGE
                           The following backmerge PR#{pr_urls.length > 1 ? '(s) were' : ' was'} created:
                           #{pr_urls.map { |url| "- #{url}" }}
                         MESSAGE
                       ]
                     end
    buildkite_annotate(style: style, context: 'backmerge-prs-outcome', message: message) if is_ci
    UI.success(message)

    # At this point, an intermediate branch has been created by creating a backmerge PR to a hotfix or the next version release branch.
    # This allows us to safely delete the `release/*` branch.
    # Note that if a hotfix or new release branches haven't been created, the backmerge PR won't be created as well.
    delete_remote_git_branch!(current_branch)
  end

  lane :trigger_beta_build do |branch_to_build:|
    trigger_buildkite_release_build(branch: branch_to_build, beta: true)
  end

  lane :trigger_release_build do |branch_to_build:|
    trigger_buildkite_release_build(branch: branch_to_build, beta: false)
  end

  lane :create_release_on_github do |version: VERSION_FILE.read_version_name, beta: true, apk_path: GRADLE_APK_OUTPUT_PATH.to_s|
    create_github_release(
      repository: GITHUB_REPO,
      version: version,
      release_notes_file_path: RELEASE_NOTES_PATH,
      prerelease: beta,
      release_assets: apk_path
    )
  end
end

def freeze_milestone_and_move_assigned_prs_to_next_milestone(
  milestone_to_freeze:,
  next_milestone:,
  github_repository: GITHUB_REPO
)
  # Notice that the order of execution is important here and should not be changed.
  #
  # First, we move the PR from milestone_to_freeze to next_milestone.
  # Then, we update milestone_to_freeze's tile with the frozen marker (traditionally ❄️ )
  #
  # If the order were to be reversed, the PRs lookup for milestone_to_freeze would yeld no value.
  # That's because the lookup uses the milestone title, which would no longer be milestone_to_freeze, but milestone_to_freeze + the frozen marker.
  begin
    # Move PRs to next milestone
    moved_prs = update_assigned_milestone(
      repository: github_repository,
      from_milestone: milestone_to_freeze,
      to_milestone: next_milestone,
      comment: "Version `#{milestone_to_freeze}` has entered code-freeze. The milestone of this PR has been updated to `#{next_milestone}`."
    )

    # Add ❄️ marker to milestone title to indicate we entered code-freeze
    set_milestone_frozen_marker(
      repository: github_repository,
      milestone: milestone_to_freeze
    )
  rescue StandardError => e
    moved_prs = []

    report_milestone_error(error_title: "Error during milestone `#{milestone_to_freeze}` freezing and PRs milestone updating process: #{e.message}")
  end

  UI.message("Moved the following PRs to milestone #{next_milestone}: #{moved_prs.join(', ')}")

  return unless is_ci

  moved_prs_info = if moved_prs.empty?
                     "No open PRs were targeting `#{milestone_to_freeze}` at the time of code-freeze."
                   else
                     "#{moved_prs.count} PRs targeting `#{milestone_to_freeze}` were still open at the time of code-freeze. They have been moved to `#{next_milestone}`:\n" \
                       + moved_prs.map { |pr_num| "[##{pr_num}](https://github.com/#{GITHUB_REPO}/pull/#{pr_num})" }.join(', ')
                   end

  buildkite_annotate(
    style: moved_prs.empty? ? 'success' : 'warning',
    context: 'code-freeze-milestone-updates',
    message: moved_prs_info
  )
end

def report_milestone_error(error_title:)
  error_message = <<-MESSAGE
    #{error_title}
    - If this is not the first time you are running the release task (e.g. retrying because it failed on first attempt), the milestone might have already been closed and this error is expected.
    - Otherwise, please investigate the error.
  MESSAGE

  UI.error(error_message)

  buildkite_annotate(style: 'warning', context: 'error-with-milestone', message: error_message) if is_ci
end

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

def trigger_buildkite_release_build(branch:, beta:)
  build_url = buildkite_trigger_build(
    buildkite_organization: BUILDKITE_ORGANIZATION,
    buildkite_pipeline: BUILDKITE_PIPELINE,
    branch: branch,
    environment: { BETA_RELEASE: beta },
    pipeline_file: 'release-build.yml'
  )

  return unless is_ci

  message = "This build triggered #{build_url} on <code>#{branch}</code>."
  buildkite_annotate(style: 'info', context: 'trigger-release-build', message: message)
end

def add_version_section_to_dev_release_notes(version:)
  android_update_release_notes(
    new_version: version,
    release_notes_file_path: RELEASE_NOTES_SOURCE_PATH
  )
end

def delete_old_changelogs_and_commit(version:)
  deleted_files = delete_old_changelogs

  git_add(path: deleted_files)
  git_commit(
    path: deleted_files,
    message: "Delete old changelogs post #{version} code freeze",
    allow_nothing_to_commit: true
  )
end

# Delete a branch from the GitHub remote, after having removed any GitHub branch protection.
#
def delete_remote_git_branch!(branch_name, remote: 'origin')
  remove_branch_protection(repository: GITHUB_REPO, branch: branch_name)

  Git.open(Dir.pwd).push(remote, branch_name, delete: true)
end
