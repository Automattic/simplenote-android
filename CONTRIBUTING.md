# Contributing

Here's a quick guide to create a pull request for your simplenote-android patch:

1. Fork the github project by visiting this URL: https://github.com/Automattic/simplenote-android/fork
2. Clone the git repository.

        $ git clone git@github.com:YOUR-GITHUB-USERNAME/simplenote-android.git
        
3. Create a new branch in your git repository (branched from `develop` - see [Notes about branching](#notes-about-branching) below).

        $ cd simplenote-android/
        $ git checkout develop
        $ git checkout -b issue/123-fix-for-123 # use a better title
        
4. Setup your build environment (see [build instructions in our README][build-instructions]) and start hacking the project.
5. When your patch is ready, [submit a pull request][pr]. Add some comments or screen shots to help us.
6. Wait for us to review your pull request. If something is wrong or if we want you to make some changes before the merge, we'll let you know through commit comments or pull request comments.

[build-instructions]: https://github.com/Automattic/simplenote-android/blob/develop/README.md#how-to-configure
[pr]: https://github.com/Automattic/simplenote-android/compare/

# Branching

* `develop` branch represents the cutting edge version. This is probably the one you want to fork from and base your patch on. This is the default github branch.
* Fix or feature branches. Proposed new features and bug fixes should live in their own branch. Use the following naming convention: if a github issue exists for this feature/bugfix, the branch will be named `issue/ISSUEID-comment` where ISSUEID is the corresponding github issue id. If a github issue doesn't exist, branch will be named `feature/comment`. These branches will be merged in `develop`.

[git-flow]: http://nvie.com/posts/a-successful-git-branching-model/