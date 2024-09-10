# frozen_string_literal: true

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
