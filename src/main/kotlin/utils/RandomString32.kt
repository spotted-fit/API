package utils

import java.util.UUID

fun generateRandom32(): String {
    return UUID.randomUUID().toString().replace("-", "")
}