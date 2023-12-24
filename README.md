# Ksoup: Kotlin Multiplatform HTML & XML Parser

**Ksoup** is a Kotlin Multiplatform library for working with real-world HTML and XML. It's a port of the renowned Java library, **jsoup**, and offers an easy-to-use API for URL fetching, data parsing, extraction, and manipulation using DOM and CSS selectors.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/com.fleeksoft.ksoup/ksoup.svg)](https://mvnrepository.com/artifact/com.fleeksoft.ksoup)

![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)
![badge-jvm](http://img.shields.io/badge/platform-jvm-DB413D.svg?style=flat)
![badge-linux](http://img.shields.io/badge/platform-linux-2D3F6C.svg?style=flat)
![badge-nodejs](https://img.shields.io/badge/platform-jsNode-F8DB5D.svg?style=flat)

Ksoup implements the [WHATWG HTML5](https://html.spec.whatwg.org/multipage/) specification, parsing HTML to the same DOM as modern browsers do, but with support for Android, JVM, and native platforms.

## Features
- Scrape and parse HTML from a URL, file, or string
- Find and extract data using DOM traversal or CSS selectors
- Manipulate HTML elements, attributes, and text
- Clean user-submitted content against a safe-list to prevent XSS attacks
- Output tidy HTML

Ksoup is adept at handling all varieties of HTML found in the wild.

## Getting started
### Ksoup is published on Maven Central
Include the dependency in `commonMain`. Latest version [![Maven Central](https://img.shields.io/maven-central/v/com.fleeksoft.ksoup/ksoup.svg)](https://mvnrepository.com/artifact/com.fleeksoft.ksoup)
```Kotlin
commonMain.dependencies {
    implementation("com.fleeksoft.ksoup:ksoup:<version>")

// Optional: Include only if you need to use network request functions such as
// Ksoup.parseGetRequest, Ksoup.parseSubmitRequest, and Ksoup.parsePostRequest
    implementation("com.fleeksoft.ksoup:ksoup-network:<version>")
}
```

### Parsing HTML from a String with Ksoup
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
In this example, `Ksoup.parseGetRequest` fetches and parses HTML content from Wikipedia, extracting and printing news headlines and their corresponding links.

#### For further documentation, please check here: [Ksoup](https://fleeksoft.github.io/ksoup/)

## Open source
Ksoup is an open source project, a Kotlin Multiplatform port of jsoup, distributed under the Apache License, Version 2.0. The source code of Ksoup is available on [GitHub](https://github.com/fleeksoft/ksoup).


## Development and Support
For questions about usage and general inquiries, please refer to [GitHub Discussions](https://github.com/fleeksoft/ksoup/discussions).

If you wish to contribute, please read the [Contributing Guidelines](CONTRIBUTING.md).

To report any issues, visit our [GitHub issues](https://github.com/fleeksoft/ksoup/issues), Please ensure to check for duplicates before submitting a new issue.

## Library Status

| Platform       | Status       | Notes                                         |
|----------------|--------------|-----------------------------------------------|
| Android        | Beta         |                                               |
| JVM            | Beta         |                                               |
| iOS            | Alpha        | Does not support few charsets.                |
| Linux          | Experimental | Does not support gzip files and few charsets. |
| JS             | Alpha        | Does not support few charsets.                |
| Native MacOS   | Alpha        |                                               |
| Native Windows | Experimental | Does not support gzip file.                   |
| Wasm           | Coming Soon  |                                               |



## Licence

    Copyright 2023 Sabeeh Ul Hussnain

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.