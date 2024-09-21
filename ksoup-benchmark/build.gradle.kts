plugins {
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.allopen)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}


benchmark {
    targets {
        register("jvm")
    }

    configurations {
        named("main") {
//            exclude("org.jsoup.parser.JsoupBenchmark")
//            exclude("com.fleeksoft.ksoup.benchmark.KsoupBenchmark")
        }
    }

}
