# Ksoup: Kotlin Multiplatform HTML & XML Parser

**Ksoup** is a Kotlin Multiplatform library for working with real-world HTML and XML. It's a port of the renowned Java library, **jsoup**, and offers an easy-to-use API for URL fetching, data parsing, extraction, and manipulation using DOM and CSS selectors.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE.md)
[![Maven Central](https://img.shields.io/maven-central/v/com.fleeksoft.ksoup/ksoup.svg)](https://central.sonatype.com/artifact/com.fleeksoft.ksoup/ksoup)

![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)
![badge-mac](http://img.shields.io/badge/platform-macos-111111.svg?style=flat)
![badge-tvos](http://img.shields.io/badge/platform-tvos-808080.svg?style=flat)
![badge-jvm](http://img.shields.io/badge/platform-jvm-DB413D.svg?style=flat)
![badge-linux](http://img.shields.io/badge/platform-linux-2D3F6C.svg?style=flat)
![badge-windows](http://img.shields.io/badge/platform-windows-4D76CD.svg?style=flat)
![badge-js](https://img.shields.io/badge/platform-js-F8DB5D.svg?style=flat)
![badge-wasm](https://img.shields.io/badge/platform-wasm-F8DB5D.svg?style=flat)

## 🚨 Deprecation Notice

> The following extension libraries are **deprecated** and will be removed in a future release:
> - `ksoup-network-ktor2` (Network extension)
>
> **Recommendation:** 
> - For I/O capabilities: Use `ksoup-kotlinx` extension
> - For network capabilities: Use `ksoup-network` extension (based on Ktor 3)

Ksoup implements the [WHATWG HTML5](https://html.spec.whatwg.org/multipage/) specification, parsing HTML to the same DOM as modern browsers do, but with support for Android, JVM, and native platforms.

## Features
- Scrape and parse HTML from a URL, file, or string
- Find and extract data using DOM traversal or CSS selectors
- Manipulate HTML elements, attributes, and text
- Clean user-submitted content against a safe-list to prevent XSS attacks
- Output tidy HTML

Ksoup is adept at handling all varieties of HTML found in the wild.

## Getting started
### Library Structure
Ksoup follows a modular architecture:
- **Core Library (`com.fleeksoft.ksoup:ksoup`)**: The main library that provides HTML/XML parsing from strings
- **Optional I/O Extensions**: Add capabilities for parsing from files and other sources
- **Optional Network Extensions**: Add capabilities for fetching and parsing from URLs

### Installation
Include the dependencies in your `commonMain`. Latest version [![Maven Central](https://img.shields.io/maven-central/v/com.fleeksoft.ksoup/ksoup.svg)](https://central.sonatype.com/artifact/com.fleeksoft.ksoup/ksoup)

### 1. Core Library
**Start with the core library. This is all you need if you're only parsing HTML/XML from strings.**
```kotlin
// Required core library
implementation("com.fleeksoft.ksoup:ksoup:<version>")
```

### 2. I/O Extensions (Optional)
**Add one of these extensions only if you need to parse HTML/XML from files or other sources.**

Choose one of the following I/O libraries:

1. **[kotlinx-io](https://github.com/Kotlin/kotlinx-io) (Recommended)**
   ```kotlin
   // Optional: Add this if you need file parsing capabilities
   // Provides Ksoup.parseFile, Ksoup.parseSource & Other InputStream APIs
   implementation("com.fleeksoft.ksoup:ksoup-kotlinx:<version>")
   ```

2. **[okio](https://github.com/square/okio)**
   ```kotlin
   // Optional: Add this if you need file parsing capabilities
   // Provides Ksoup.parseFile, Ksoup.parseSource & Other InputStream APIs
   implementation("com.fleeksoft.ksoup:ksoup-okio:<version>")
   ```

### 3. Network Extensions (Optional)
**Add one of these extensions only if you need to fetch and parse HTML/XML directly from URLs.**

Choose one of the following network libraries:

1. **[Ktor 3](https://github.com/ktorio/ktor) (Recommended)**
   ```kotlin
   // Optional: Add this if you need to fetch HTML/XML from URLs
   // Provides Ksoup.parseGetRequest, Ksoup.parseSubmitRequest, Ksoup.parsePostRequest
   implementation("com.fleeksoft.ksoup:ksoup-network:<version>")
   ```

2. ~~**[Ktor 2](https://github.com/ktorio/ktor)**~~ **(DEPRECATED: Use Ktor 3 instead)**
   ```kotlin
   // Deprecated: Not recommended for new projects
   // Provides Ksoup.parseGetRequest, Ksoup.parseSubmitRequest, Ksoup.parsePostRequest
   implementation("com.fleeksoft.ksoup:ksoup-network-ktor2:<version>")
   ```

#### Ksoup supports [Charsets](https://github.com/fleeksoft/fleeksoft-io/blob/main/CharsetsReadme.md)
- Standard charsets are already supported by **Ksoup IO**, but for extended charsets, please add `com.fleeksoft.charset:charset-ext`, For more details, visit the [Charsets Documentation](https://github.com/fleeksoft/fleeksoft-io/blob/main/CharsetsReadme.md)

### Parsing HTML from a String with Ksoup
For API documentation you can check [Jsoup](https://jsoup.org/). Most of the APIs work without any changes.
```kotlin
val html = "<html><head><title>One</title></head><body>Two</body></html>"
val doc: Document = Ksoup.parse(html = html)

println("title => ${doc.title()}") // One
println("bodyText => ${doc.body().text()}") // Two
```
This snippet demonstrates how to use `Ksoup.parse` for parsing an HTML string and extracting the title and body text.

### Fetching and Parsing HTML from a URL using Ksoup
```kotlin
//Please note that the com.fleeksoft.ksoup:ksoup-network library is required for Ksoup.parseGetRequest.
val doc: Document = Ksoup.parseGetRequest(url = "https://en.wikipedia.org/") // suspend function
// or
val doc: Document = Ksoup.parseGetRequestBlocking(url = "https://en.wikipedia.org/")

println("title: ${doc.title()}")
val headlines: Elements = doc.select("#mp-itn b a")

headlines.forEach { headline: Element ->
    val headlineTitle = headline.attr("title")
    val headlineLink = headline.absUrl("href")

    println("$headlineTitle => $headlineLink")
}
```

### Parsing XML
```kotlin
    val doc: Document = Ksoup.parse(xml, parser = Parser = Parser.xmlParser())
```

### Parsing Metadata from Website
```kotlin
//Please note that the com.fleeksoft.ksoup:ksoup-network library is required for Ksoup.parseGetRequest.
val doc: Document = Ksoup.parseGetRequest(url = "https://en.wikipedia.org/") // suspend function
val metadata: Metadata = Ksoup.parseMetaData(element = doc) // suspend function
// or
val metadata: Metadata = Ksoup.parseMetaData(html = HTML)

println("title: ${metadata.title}")
println("description: ${metadata.description}")
println("ogTitle: ${metadata.ogTitle}")
println("ogDescription: ${metadata.ogDescription}")
println("twitterTitle: ${metadata.twitterTitle}")
println("twitterDescription: ${metadata.twitterDescription}")
// Check com.fleeksoft.ksoup.model.MetaData for more fields
```

In this example, `Ksoup.parseGetRequest` fetches and parses HTML content from Wikipedia, extracting and printing news headlines and their corresponding links.
### Ksoup Public functions
  - **Ksoup.parse(html: String, baseUri: String = ""): Document**
  - **Ksoup.parse(html: String, parser: Parser, baseUri: String = ""): Document**
  - **Ksoup.parse(reader: Reader, parser: Parser, baseUri: String = ""): Document**
  - **Ksoup.clean( bodyHtml: String, safelist: Safelist = Safelist.relaxed(), baseUri: String = "", outputSettings: Document.OutputSettings? = null): String**
  - **Ksoup.isValid(bodyHtml: String, safelist: Safelist = Safelist.relaxed()): Boolean**
### Ksoup I/O Public functions
  - **Ksoup.parseInput(input: InputStream, baseUri: String, charsetName: String? = null, parser: Parser = Parser.htmlParser())** from (ksoup-io, ksoup-okio, ksoup-kotlinx)
  - **Ksoup.parseFile** from (ksoup-okio, ksoup-kotlinx)
  - **Ksoup.parseSource** from (ksoup-okio, ksoup-kotlinx)

### Ksoup Network Public functions
- Suspend functions
    - **Ksoup.parseGetRequest**
    - **Ksoup.parseSubmitRequest**
    - **Ksoup.parsePostRequest**
- Blocking functions
  - **Ksoup.parseGetRequestBlocking**
  - **Ksoup.parseSubmitRequestBlocking**
  - **Ksoup.parsePostRequestBlocking**

#### For further documentation, please check here: [Jsoup](https://jsoup.org/)

### Ksoup vs. Jsoup Benchmarks: Parsing & Selecting 448KB HTML File [test.tx](https://github.com/fleeksoft/ksoup/blob/develop/ksoup-test/testResources/test.txt)
![Ksoup vs Jsoup](benchmark1.png)

## Open source
Ksoup is an open source project, a Kotlin Multiplatform port of jsoup, distributed under the MIT License, Version 2.0. The source code of Ksoup is available on [GitHub](https://github.com/fleeksoft/ksoup).


## Development and Support
For questions about usage and general inquiries, please refer to [GitHub Discussions](https://github.com/fleeksoft/ksoup/discussions).

If you wish to contribute, please read the [Contributing Guidelines](CONTRIBUTING.md).

To report any issues, visit our [GitHub issues](https://github.com/fleeksoft/ksoup/issues), Please ensure to check for duplicates before submitting a new issue.


## License

Ksoup is open source software licensed under the [MIT License](LICENSE.md).

This project is a Kotlin Multiplatform port of [Jsoup](https://jsoup.org), created by Jonathan Hedley.  
Portions of this library are derived from jsoup and retain their original [MIT License](https://jsoup.org/license),  
© 2009–2025 Jonathan Hedley.  
