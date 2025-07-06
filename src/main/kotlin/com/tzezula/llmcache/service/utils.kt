package com.tzezula.llmcache.service

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

fun hash(prompt: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(prompt.toByteArray(StandardCharsets.UTF_8))
    return hashBytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
}
