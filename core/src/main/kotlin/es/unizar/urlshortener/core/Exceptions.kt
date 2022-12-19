package es.unizar.urlshortener.core

class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class NotValidatedUrl (url: String): Exception("[$url] has not validated yet")

class NotSafeUrl (url: String): Exception("[$url] is not secure")

class NotSafeId (key: String): Exception("[$key] has an insecure URL")

class TooManyRedirections (key: String) : Exception(" [$key] has too many redirections")

class NotReachable (url: String) : Exception(" [$url] is not reachable")
