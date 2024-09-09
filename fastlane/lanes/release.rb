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

    message = <<~MESSAGE
      Code Freeze:
      - New release branch from #{DEFAULT_BRANCH}: #{release_branch_name(release_version: new_version_final)}
      - Current release version and build code: #{release_version_current} (#{build_code_current}).
      - New beta version and build code: #{new_version_with_beta} (#{new_build_code}).
    MESSAGE
    UI.important(message)

    unless skip_confirm || UI.confirm('Do you want to continue?')
      UI.user_error!("Terminating as requested. Don't forget to run the remainder of this automation manually.")
      next
    end

    UI.message 'Creating release branch...'
    Fastlane::Helper::GitHelper.create_branch(release_branch_name(release_version: new_version_final), from: DEFAULT_BRANCH)
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
    android_update_release_notes(
      new_version: new_version,
      release_notes_file_path: RELEASE_NOTES_SOURCE_PATH
    )

    update_strings_for_translation_automation

    unless skip_confirm || UI.confirm('Ready to push changes to remote to let the automation configure it on GitHub?')
      UI.user_error!("Terminating as requested. Don't forget to run the remainder of this automation manually.")
      next
    end

    push_to_git_remote(
      tags: false,
      set_upstream: true
    )

    copy_branch_protection(
      repository: GITHUB_REPO,
      from_branch: DEFAULT_BRANCH,
      to_branch: release_branch_name
    )
    set_milestone_frozen_marker(
      repository: GITHUB_REPO,
      milestone: new_version_final,
      freeze: true
    )
  end
end
