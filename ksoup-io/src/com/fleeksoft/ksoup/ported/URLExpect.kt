package com.fleeksoft.ksoup.ported

interface URLExpect {
    fun resolveOrNull(base: String, access: String): String?
}