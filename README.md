# Ksoup: Kotlin Multiplatform HTML & XML Parser

**Ksoup** is a Kotlin Multiplatform library for working with real-world HTML and XML. It's a port of the renowned Java library, **jsoup**, and offers an easy-to-use API for URL fetching, data parsing, extraction, and manipulation using DOM and CSS selectors.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)
![badge-mac](http://img.shields.io/badge/platform-macos-111111.svg?style=flat)
![badge-jvm](http://img.shields.io/badge/platform-jvm-DB413D.svg?style=flat)
![badge-windows](http://img.shields.io/badge/platform-windows-4D76CD.svg?style=flat)

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
```Kotlin
commonMain.dependencies {
    implementation("com.fleeksoft.ksoup:ksoup:0.0.6")

// Optional: Include only if you need to use network request functions such as
// Ksoup.parseGetRequest, Ksoup.parseSubmitRequest, and Ksoup.parsePostRequest
    implementation("com.fleeksoft.ksoup:ksoup-network:0.0.6")
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
val doc: Document = Ksoup.parseGetRequest(url = "https://en.wikipedia.org/")
println("title: ${doc.title()}")
val headlines: Elements = doc.select("#mp-itn b a")

headlines.forEach { headline: Element ->
    val headlineTitle = headline.attr("title")
    val headlineLink = headline.absUrl("href")

    println("$headlineTitle => $headlineLink")
}
```
In this example, `Ksoup.parseGetRequest` fetches and parses HTML content from Wikipedia, extracting and printing news headlines and their corresponding links.

## Open source
Ksoup is an open source project, a Kotlin Multiplatform port of jsoup, distributed under the MIT license. The source code of Ksoup is available on [GitHub](https://github.com/fleeksoft/ksoup).


## Development and Support
For questions, ideas, or contributions regarding Ksoup, please contact us via [email](mailto:fleeksoft@gmail.com) or create new pull requests.

Report any issues on [our GitHub page](https://github.com/fleeksoft/ksoup/issues), ensuring to check for duplicates beforehand.

## Status
Ksoup is in a stable release phase, continually evolving from its jsoup origins.

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