package com.fleeksoft.ksoup.ported

import korlibs.io.net.URL

object URL : URLExpect {
    override fun resolveOrNull(base: String, access: String): String? = URL.resolveOrNull(base = base, access = access)
}