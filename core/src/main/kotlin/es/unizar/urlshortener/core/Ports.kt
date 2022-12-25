package es.unizar.urlshortener.core


import org.springframework.core.io.*
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletRequest


/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun save(cl: Click): Click
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByHash(hash: String): ShortUrl?
    fun findByUrl(url: String): ShortUrl?
    fun save(su: ShortUrl): ShortUrl
}

/**
 * [ValidatorService] is the port to the service that validates if an url can be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface ValidatorService {
    fun isValid(url: String): Boolean
}

/**
 * [ReachableService] is the port to the service that validates if an url can be reachable.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */

interface ReachableService {

    fun isReachableUrl(url: String): Boolean

    fun isValidated(hash: String): Boolean
}

/**
 * [SecurityService] is the port to the service that validates if an url is safe or not
 * according the Google Safe Browsing service.
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface SecurityService {

    fun isSecureUrl(url: String): Boolean

    fun isValidated(hash: String): Boolean
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface HashService {
    fun hasUrl(url: String): String
}

/**
 * [RedirectionLimitService] is the port that limit the number of redirection to a url.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface RedirectionLimitService {
    fun addLimit(hash : String, limit : Int)
    fun proveLimit(hash : String)
}

/**
 * [QRService] is the port that creates a QR code from a shortened URI.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface QRService {
    fun qr(url: String): ByteArrayResource
}
 /* Given the name of a csv file with a URL per line generates another csv file
 * with the returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CsvService {
    fun create(file: MultipartFile, data: ShortUrlProperties, request: HttpServletRequest): String
}
