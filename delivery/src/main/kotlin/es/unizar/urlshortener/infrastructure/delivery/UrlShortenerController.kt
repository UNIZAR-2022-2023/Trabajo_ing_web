package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.BlockingQueue
import com.google.common.net.HttpHeaders.*
import es.unizar.urlshortener.core.usecases.*
import org.springframework.core.io.*
import org.springframework.hateoas.server.mvc.*
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import java.net.*
import javax.servlet.http.*
import org.springframework.http.MediaType.*

/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Void>

    /**
     * Creates a short url
     * from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    /**
     * Creates a qr
     * from hash.
     *
     * **Note**: Delivery of use case [GenerateQRUseCase].
     */
    fun generateQR(hash: String, request: HttpServletRequest) : ResponseEntity<ByteArrayResource>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val limit: Int? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val qr: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val securityService: SecurityService,
    val generateQRUseCase: GenerateQRUseCase
) : UrlShortenerController {

    @Autowired
    private val validationQueue : BlockingQueue<String> ?= null

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()

            if (securityService.isValidated(id)) {
                // URL has been validated
                if (securityService.isSecureHash(id)) {
                    // URL is safe
                    h.location = URI.create(it.target)
                    ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
                } else {
                    throw NotSafeUrl(id)
                }
            } else {
                throw NotValidatedUrl(id)
            }
        }

    @PostMapping("/api/link", consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor ,
                limit = data.limit?: 0
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val qr = URI.create("http://localhost" + url.path + "/qr")
            // Send the URL to the validation queue
            println("Añadiendo nueva URL: ${data.url}")
            println("Añadiendo nuevo QR: ${qr}")
            validationQueue?.put(data.url)

            val response = ShortUrlDataOut(
                url = url,
                qr = qr
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }
    @GetMapping("/{hash}/qr")
    override fun generateQR(@PathVariable hash: String, request: HttpServletRequest) : ResponseEntity<ByteArrayResource> =
        generateQRUseCase.generateQR(hash).let {
            val h = HttpHeaders()
            h.set(CONTENT_TYPE, IMAGE_PNG.toString())
            ResponseEntity<ByteArrayResource>(it, h, HttpStatus.OK)
        }
}
