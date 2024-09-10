# frozen_string_literal: true

SUPPORTED_LOCALES = [
  { glotpress: 'ar', android: 'ar', google_play: 'ar', promo_config: {} },
  { glotpress: 'de', android: 'de', google_play: 'de-DE',  promo_config: {} },
  { glotpress: 'es', android: 'es', google_play: 'es-ES',  promo_config: {} },
  { glotpress: 'fr', android: 'fr', google_play: 'fr-FR',  promo_config: {} },
  { glotpress: 'he', android: 'he', google_play: 'iw-IL',  promo_config: {} },
  { glotpress: 'id', android: 'id', google_play: 'id', promo_config: {} },
  { glotpress: 'it', android: 'it', google_play: 'it-IT',  promo_config: {} },
  { glotpress: 'ja', android: 'ja', google_play: 'ja-JP',  promo_config: {} },
  { glotpress: 'ko', android: 'ko', google_play: 'ko-KR',  promo_config: {} },
  { glotpress: 'nl', android: 'nl', google_play: 'nl-NL',  promo_config: {} },
  { glotpress: 'pt-br', android: 'pt-rBR', google_play: 'pt-BR', promo_config: {} },
  { glotpress: 'ru', android: 'ru', google_play: 'ru-RU',  promo_config: {} },
  { glotpress: 'sv', android: 'sv', google_play: 'sv-SE',  promo_config: {} },
  { glotpress: 'tr', android: 'tr', google_play: 'tr-TR',  promo_config: {} },
  { glotpress: 'zh-cn', android: 'zh-rCN', google_play: 'zh-CN',  promo_config: {} },
  { glotpress: 'zh-tw', android: 'zh-rTW', google_play: 'zh-TW',  promo_config: {} }
].freeze

platform :android do
  desc 'Updates the PlayStoreStrings.pot file'
  lane :update_appstore_strings do |version: release_version_current|
    UI.important('This has been renamed to update_play_store_strings – Here only for backwards compatibility with checklists. Will be removed soon.')
    UI.important('Forwarding to update_play_store_strings....')
    update_play_store_strings(version: version)
  end

  desc 'Updates the PlayStoreStrings.pot file'
  lane :update_play_store_strings do |version: release_version_current|
    files = {
      release_note: RELEASE_NOTES_PATH,
      play_store_promo: File.join(METADATA_FOLDER, 'short_description.txt'),
      play_store_desc: File.join(METADATA_FOLDER, 'full_description.txt'),
      play_store_app_title: File.join(APP_SOURCES_FOLDER, 'metadata', 'title.txt')
    }

    pot_path = File.join(METADATA_FOLDER, 'PlayStoreStrings.pot')

    an_update_metadata_source(
      po_file_path: pot_path,
      source_files: files,
      release_version: version
    )

    git_add(path: pot_path)
    git_commit(
      path: pot_path,
      message: "Update `#{File.basename(pot_path)}` for #{version}",
      allow_nothing_to_commit: true
    )
  end

  lane :check_translation_progress_all do
    check_translation_progress_strings
    check_translation_progress_release_notes
  end

  lane :check_translation_progress_strings do
    UI.message('Checking app strings translation status...')
    check_translation_progress(
      glotpress_url: GLOTPRESS_APP_STRINGS_PROJECT_URL,
      abort_on_violations: false
    )
  end

  lane :check_translation_progress_release_notes do
    UI.message('Checking release notes strings translation status...')
    check_translation_progress(
      glotpress_url: GLOTPRESS_STORE_METADATA_PROJECT_URL,
      abort_on_violations: false
    )
  end

  # TODO: Drop build_number like https://github.com/wordpress-mobile/WordPress-Android/pull/21103
  desc 'Downloads translated metadata from GlotPress'
  lane :download_metadata_strings do |version: release_version_current, build_number: build_code_current|
    values = version.split('.')
    files = {
      "release_note_#{values[0].to_s.rjust(2, '0')}#{values[1]}" => { desc: "changelogs/#{build_number}.txt", max_size: 0 },
      play_store_promo: { desc: 'short_description.txt', max_size: 80 },
      play_store_desc: { desc: 'full_description.txt', max_size: 0 },
      play_store_app_title: { desc: 'title.txt', max_size: 50 }
    }

    delete_old_changelogs(build: build_number)
    download_path = "#{Dir.pwd}/metadata/android"
    gp_downloadmetadata(
      project_url: GLOTPRESS_STORE_METADATA_PROJECT_URL,
      target_files: files,
      locales: SUPPORTED_LOCALES.map { |hsh| [hsh[:glotpress], hsh[:google_play]] },
      source_locale: 'en-US',
      download_path: download_path
    )

    # TODO: These two can be removed once we have a lane to upload the metadata to the Google Play Store.
    #       upload_to_play_store uses the individual changelos without needing a merged one.
    android_create_xml_release_notes(
      download_path: download_path,
      build_number: build_number.to_s,
      locales: SUPPORTED_LOCALES.to_h { |hsh| [hsh[:glotpress], hsh[:google_play]] }
    )
    add_us_release_notes(
      release_notes_path: File.join(download_path, 'release_notes.xml'),
      version_name: version
    )

    # We need to explicitly call `git_add`, despite the path being passed to `git_commit` as well.
    # That's because we might have new files, that the commit command would otherwise miss.
    git_add(path: download_path)
    git_commit(
      path: download_path,
      message: "Update metadata translations for #{version}",
      allow_nothing_to_commit: true
    )
  end

  desc 'Download the latest app translations from GlotPress and update the strings.xml files accordingly'
  lane :download_translations do
    # android_update_release_notes requires a relative path
    res_folder_relative_path = RES_FOLDER.delete_prefix(PROJECT_ROOT_FOLDER)

    android_download_translations(
      res_dir: res_folder_relative_path,
      glotpress_url: GLOTPRESS_APP_STRINGS_PROJECT_URL,
      locales: SUPPORTED_LOCALES,
      lint_task: 'lintRelease'
    )
  end
end
