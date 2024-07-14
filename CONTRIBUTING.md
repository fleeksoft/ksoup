# Contributing Guidelines

You can contribute to this project by reporting issues or submitting changes via a pull request.

## Reporting issues

For filing feature requests and bug reports, please use our [GitHub issues](https://github.com/fleeksoft/ksoup/issues).

For questions about usage and general inquiries, consider asking on StackOverflow or participating in
our [GitHub Discussions](https://github.com/fleeksoft/ksoup/discussions).

## Submitting changes

To submit pull requests, please visit [this page](https://github.com/fleeksoft/ksoup/pulls).
Keep in mind that maintainers will have to support the code resulting from your contribution. Therefore, please
familiarize yourself with the following guidelines:

* All development (both new features and bug fixes) should be performed in the `develop` branch.
    * The `main` branch hosts the sources of the most recently released version.
    * Base your pull requests against the `develop` branch.
    * The `develop` branch is merged into the `main` branch during releases.
    * Ensure to [Build the project](#building) to verify that everything works and passes the tests.
* If you are fixing a bug:
    * Write the test that reproduces the bug.
    * Fixes without tests are accepted only under exceptional circumstances, such as when writing a corresponding test
      is too hard or impractical.
    * Follow the project's style for writing tests: name test functions as testXxx. Avoid using backticks in test names.
* If you wish to work on an existing issue, comment on it first. Ensure that the issue clearly describes a problem and a
  solution that has received positive feedback. Propose a solution if none is suggested.

## Building

This library is built with Gradle.

* Run `./gradlew build` to build the entire project. It also runs all the tests.
* Run `./gradlew <module>:check` to test only the module you are working on.
* Run `./gradlew <module>:jvmTest` to perform only the fast JVM tests of a multiplatform module.