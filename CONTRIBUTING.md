# Contributing

Here's a quick guide to create a pull request for your simplenote-android patch:

1. Fork the github project by visiting this URL: https://github.com/Automattic/simplenote-android/fork
2. Clone the git repository.

        $ git clone git@github.com:YOUR-GITHUB-USERNAME/simplenote-android.git

3. Create a new branch in your git repository (branched from `trunk` - see [Notes about branching](#notes-about-branching) below).

        $ cd simplenote-android/
        $ git checkout trunk
        $ git checkout -b issue/123-fix-for-123 # use a better title

4. Setup your build environment (see [build instructions in our README][build-instructions]) and start hacking the project.
5. When your patch is ready, [submit a pull request][pr]. Add some comments or screen shots to help us.
6. Wait for us to review your pull request. If something is wrong or if we want you to make some changes before the merge, we'll let you know through commit comments or pull request comments.

[build-instructions]: https://github.com/Automattic/simplenote-android/blob/trunk/README.md#how-to-configure
[pr]: https://github.com/Automattic/simplenote-android/compare/

## Branching

* `trunk` branch represents the cutting edge version. This is probably the one you want to fork from and base your patch on. This is the default github branch.
* Fix or feature branches. Proposed new features and bug fixes should live in their own branch. Use the following naming convention: if a github issue exists for this feature/bugfix, the branch will be named `issue/ISSUEID-comment` where ISSUEID is the corresponding github issue id. If a github issue doesn't exist, branch will be named `feature/comment`. These branches will be merged in `trunk`.

[git-flow]: http://nvie.com/posts/a-successful-git-branching-model/

## Testing

Use the [testing checklist][testing-checklist] as a guide for smoke testing the app after making code changes. Looking for other ways to contribute? You can also use the checklist as a guide for beta testing the app.

You can install a beta version in one of two ways:

1. Go to the [Simplenote testing opt-in page][simplenote-testing] and click the "Become a Tester" button. (You can also open [Simplenote in the Play Store][simplenote-play-store] on your Android device, scroll down to the “Join the beta” section, and tap “Join.”)  Once you're a tester, the app will update automatically through the Play Store just like any other app when a new beta is released.
2. Alternately, install the latest pre-release version from the [Releases][releases] page. (This is good for one-off testing, or beta testing without the Google Play Store on your device.)

[testing-checklist]: TESTING-CHECKLIST.md
[simplenote-testing]: https://play.google.com/apps/testing/com.automattic.simplenote
[simplenote-play-store]: https://play.google.com/store/apps/details?id=com.automattic.simplenote
[releases]: https://github.com/Automattic/simplenote-android/releases
