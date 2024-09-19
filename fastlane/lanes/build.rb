# frozen_string_literal: true

platform :android do
  lane :build_for_distribution do
    version = VERSION_FILE.read_version_name
    build_code = build_code_current

    user_friendly_output_dir = File.join(PROJECT_ROOT_FOLDER, 'build')
    user_friendly_output_apk_name = "simplenote-#{version}.apk"
    user_friendly_output_path = File.join(user_friendly_output_dir, user_friendly_output_apk_name)

    build_type = 'Release'

    UI.message('Cleaning build folder...')
    gradle(task: 'clean')

    UI.message('Linting...')
    gradle(task: 'lint', build_type: build_type)

    UI.message("Building #{version} (#{build_code}) to #{user_friendly_output_path}...")
    gradle(task: 'assemble', build_type: build_type)
    FileUtils.copy(GRADLE_APK_OUTPUT_PATH, user_friendly_output_path)

    if File.exist? user_friendly_output_path
      UI.success("APK ready at #{user_friendly_output_path}")
    else
      UI.user_error!("Unable to find a build artifact at #{user_friendly_output_path}")
    end

    user_friendly_output_path
  end

  lane :upload_build_to_play_store do |apk_path:, track: nil, beta: true|
    # While transitioning between the current approach and the new one where the upload is a dedicated lane (this) support both beta flag and explicit track name.
    if track.nil?
      track = beta ? 'beta' : 'production'
    end

    upload_to_play_store(
      package_name: APP_PACKAGE_NAME,
      apk: apk_path,
      track: track,
      rollout: beta ? '1.0' : '0.1', # Rollout to 100% for betas, start at 10% for production
      release_status: beta ? 'completed' : 'inProgress',
      skip_upload_metadata: beta,
      skip_upload_changelogs: beta,
      skip_upload_images: true,
      skip_upload_screenshots: true,
      json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY
    )
  end

  # Legacy build + upload lanes

  desc 'Builds and updates for distribution'
  lane :build_pre_releases do |create_release: true|
    build_and_upload_beta(create_release: create_release)
  end

  desc 'Builds and updates a beta for distribution'
  lane :build_and_upload_beta do |create_release: true|
    build_and_upload(beta: true, create_release: create_release)
  end

  desc 'Builds and updates for distribution'
  lane :build_and_upload_release do |create_release:|
    build_and_upload(beta: false, create_release: create_release)
  end

  lane :build_and_upload do |beta:, create_release:|
    ensure_git_status_clean unless is_ci
    ensure_git_branch_is_release_branch!

    # We want the full version name, including the rc prefix
    version = VERSION_FILE.read_version_name

    build_and_upload_apk(
      upload_track: beta ? 'beta' : 'production'
    )

    next unless create_release

    # TODO: Add step to downalod universal APK etc. once the app uses bundles

    create_release_on_github(
      version: version,
      beta: beta
    )
  end

  desc 'Builds an app apk and upload it'
  lane :build_and_upload_apk do |upload_track: nil|
    apk_path = build_for_distribution

    if upload_track.nil?
      UI.message('Skipping upload to Google Play Console because on `upload_track` given.')
    else
      upload_build_to_play_store(apk_path: apk_path, track: upload_track)
    end

    apk_path
  end
end
