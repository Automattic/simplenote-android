# frozen_string_literal: true

# Lanes related to the Release Process (Code Freeze, Betas, Final Build, App Store Submission…)

platform :android do
  desc 'Creates a new release branch from the current default branch'
  lane :code_freeze do |skip_prechecks: false, skip_confirm: false|
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
    UI.success("Done! New beta version: #{release_version_current}. New build code: #{build_code_current}.")

    extract_release_notes_for_version(
      version: new_version_with_beta,
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

    unless skip_confirm || UI.confirm('Ready to push changes to remote and trigger the beta build?')
      UI.message("Terminating as requested. Don't forget to run the remainder of this automation manually.")
      next
    end

    push_to_git_remote(tags: false)

    trigger_beta_build(branch_to_build: release_branch_name(release_version: version))

    pr_url = create_release_management_pull_request(
      release_version: version,
      base_branch: DEFAULT_BRANCH,
      title: "Merge #{version} code freeze"
    )

    next unless is_ci

    message = <<~MESSAGE
      Code freeze completed successfully. Next, review and merge the [integration PR](#{pr_url}).
    MESSAGE
    buildkite_annotate(context: 'code-freeze-completed', style: 'success', message: message)
  end

  lane :trigger_beta_build do |branch_to_build:|
    trigger_buildkite_release_build(branch: branch_to_build, beta: true)
  end

  lane :trigger_release_build do |branch_to_build:|
    trigger_buildkite_release_build(branch: branch_to_build, beta: false)
  end

  lane :create_release_on_github do |version: release_version_current, beta: true, apk_path: GRADLE_APK_OUTPUT_PATH.to_s|
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

def create_release_management_pull_request(release_version:, base_branch:, title:)
  # TODO: Adopt shared EnvManager ASAP. See https://github.com/wordpress-mobile/release-toolkit/pull/578
  # token = EnvManager.get_required_env!('GITHUB_TOKEN')
  token = ENV.fetch('GITHUB_TOKEN', nil)
  UI.user_error!('No GITHUB_TOKEN found in the environment.') if token.nil?

  pr_url = create_pull_request(
    api_token: token,
    repo: GITHUB_REPO,
    title: title,
    head: Fastlane::Helper::GitHelper.current_git_branch,
    base: base_branch,
    labels: 'Releases'
  )

  # Next, set the milestone for the PR
  #
  # The create_pull_request action has a 'milestone' parameter, but it expects the milestone id.
  # We don't know the id of the milestone, but we can use a different action to set it.
  #
  # PR URLs are in the format github.com/org/repo/pull/id
  pr_number = File.basename(pr_url)
  update_assigned_milestone(
    repository: GITHUB_REPO,
    numbers: [pr_number],
    to_milestone: release_version
  )

  # Return the PR URL
  pr_url
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
