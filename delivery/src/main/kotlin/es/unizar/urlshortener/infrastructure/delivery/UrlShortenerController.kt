package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import org.springframework.beans.factory.annotation.Autowired
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.CsvUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.util.concurrent.BlockingQueue
import javax.servlet.http.HttpServletRequest

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
     * Creates a short url from details provided in [file].
     *
     * **Note**: Delivery of use case [ShortUrlDataOut].
     */
    fun csv(@RequestParam("file") file: MultipartFile, request: HttpServletRequest): ResponseEntity<String>
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
    val csvUseCase: CsvUseCase
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

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
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

            // Send the URL to the validation queue
            println("Añadiendo nueva URL: ${data.url}")
            validationQueue?.put(data.url)

            val response = ShortUrlDataOut(
                url = url
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @PostMapping("/api/bulk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun csv(@RequestParam("file") file: MultipartFile, request: HttpServletRequest): ResponseEntity<String> {
        val h = HttpHeaders()
        if(file.isEmpty)  {
            h.add("Warning", "Empty file")
            return ResponseEntity<String>(h, HttpStatus.OK)
        } else {
            try {
                val csv = csvUseCase.create(
                    file = file,
                    data = ShortUrlProperties(
                        ip = request.remoteAddr,
                        sponsor = null
                    )
                )
                h.set("Content-Type", "text/csv")
                h.set("Content-Disposition", "attachment; filename=shortURLs.csv")
                h.set("Content-Length", csv.length.toString())
                return ResponseEntity<String>(csv, h, HttpStatus.CREATED)
            } catch(e: Exception) {
                h.add("Error", "Cannot read csv")
                h.set("Content-Type", "application/json")
                return ResponseEntity<String>(h, HttpStatus.BAD_REQUEST)
            }
        }
    }
}
