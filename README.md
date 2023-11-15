# Ksoup: Kotlin Multiplatform HTML Parser

**Ksoup** is a Kotlin Multiplatform library for working with real-world HTML and XML. It's a port of the renowned Java library, **jsoup**, and offers an easy-to-use API for URL fetching, data parsing, extraction, and manipulation using DOM, CSS, and xpath selectors.

Ksoup implements the [WHATWG HTML5](https://html.spec.whatwg.org/multipage/) specification, parsing HTML to the same DOM as modern browsers do, but with support for Android, JVM, and native platforms.

## Features
- Scrape and parse HTML from a URL, file, or string
- Find and extract data using DOM traversal or CSS selectors
- Manipulate HTML elements, attributes, and text
- Clean user-submitted content against a safe-list to prevent XSS attacks
- Output tidy HTML

Ksoup is adept at handling all varieties of HTML found in the wild.

## Current Limitations
As of now, Ksoup does not implement the connection cookies and servlet-related features of jsoup. This is an area under consideration for future development.

## Multiplatform Support
- **Android**: Extensive support for Android development.
- **JVM**: Compatible with Java Virtual Machine environments.
- **Native**: Supports native platform development.

## Open source
Ksoup is an open source project, a Kotlin Multiplatform port of jsoup, distributed under the MIT license. The source code of Ksoup is available on [GitHub](https://github.com/fleeksoft/ksoup).

## Getting started
- Add the library to dependencies:

## Gradle
```kotlin
// for kotlin multiplatform
commonMain {
    dependencies {
        implementation("com.fleeksoft:ksoup:0.0.1")
    }
}
```

## Development and Support
For questions, ideas, or contributions regarding Ksoup, please contact us via [email](mailto:fleeksoft@gmail.com) or create new pull requests.

Report any issues on [our GitHub page](https://github.com/fleeksoft/ksoup/issues), ensuring to check for duplicates beforehand.

## Status
Ksoup is in a stable release phase, continually evolving from its jsoup origins.



## Status with Jsoup
Updated with the main branch of Jsoup on GitHub [c46870c266b0c1112f4b1d423cf6dd9290d04d2f] as of 14 November 2023.