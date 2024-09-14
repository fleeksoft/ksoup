plugins {
    alias(libs.plugins.mavenPublish)
}

group = "com.fleeksoft.ksoup"
version = libs.versions.libraryVersion.get()

val artifactId = "ksoup-engine-lite"
mavenPublishing {
    coordinates("com.fleeksoft.ksoup", artifactId, libs.versions.libraryVersion.get())
    pom {
        name.set(artifactId)
        description.set("Ksoup is a Kotlin Multiplatform library for working with HTML and XML, and offers an easy-to-use API for URL fetching, data parsing, extraction, and manipulation using DOM and CSS selectors.")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://opensource.org/licenses/Apache-2.0")
            }
        }
        url.set("https://github.com/fleeksoft/ksoup")
        issueManagement {
            system.set("Github")
            url.set("https://github.com/fleeksoft/ksoup/issues")
        }
        scm {
            connection.set("https://github.com/fleeksoft/ksoup.git")
            url.set("https://github.com/fleeksoft/ksoup")
        }
        developers {
            developer {
                name.set("Sabeeh Ul Hussnain Anjum")
                email.set("fleeksoft@gmail.com")
                organization.set("Fleek Soft")
            }
        }
    }
}