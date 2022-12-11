package es.unizar.urlshortener.core

class InvalidUrlException(val url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(private val key: String) : Exception("[$key] is not known")

class NotSafeURL (val url: String) : Exception("[$url] is not safe")
class NotValidated (val url: String): Exception("[$url] has not validated yet")

class NotSafeId (private val key: String): Exception("[$key] has an insecure URL")
