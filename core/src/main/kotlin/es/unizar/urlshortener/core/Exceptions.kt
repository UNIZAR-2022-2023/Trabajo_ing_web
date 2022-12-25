package es.unizar.urlshortener.core

class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class NotValidated (url: String): Exception("[$url] has not validated yet")

class NotSafe (url: String): Exception("[$url] is not secure")

class TooManyRedirections (key: String) : Exception(" [$key] has too many redirections")

class NotReachable (url: String) : Exception(" [$url] is not reachable")

class QrNotFound(val hash: String) : Exception("Destination URI [$hash] doesn't exist")
