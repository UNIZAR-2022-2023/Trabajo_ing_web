package es.unizar.urlshortener.core

const val RETRY_AFTER_VALIDATION : Long = 60

const val RETRY_AFTER_REDIRECTIONS : Long = 60

class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class NotValidated (url: String, retryAfter: Long): Exception("[$url] has not validated yet") {
    val retryAfter : Long = retryAfter
}

class TooManyRedirections (key: String, retryAfter: Long) : Exception(" [$key] has too many redirections") {
    val retryAfter : Long = retryAfter
}

class NotReachable (url: String) : Exception(" [$url] is not reachable")

class NotSafe (url: String): Exception("[$url] is not secure")

class QrNotFound(val hash: String) : Exception("Destination URI [$hash] doesn't exist")
