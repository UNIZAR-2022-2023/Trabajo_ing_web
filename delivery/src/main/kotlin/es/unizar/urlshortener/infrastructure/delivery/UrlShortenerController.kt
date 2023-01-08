package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.net.HttpHeaders.*
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.*
import org.springframework.hateoas.server.mvc.*
import org.springframework.http.*
import org.springframework.http.MediaType.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.*
import java.util.concurrent.BlockingQueue
import javax.servlet.http.*

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
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    /**
     * Gets info from an URL that has been posted.
     *
     *
     */
    fun getInfo(id: String, request: HttpServletRequest): ResponseEntity<InfoID>

    /**
     * Creates a qr
     * from hash.
     *
     * **Note**: Delivery of use case [GenerateQRUseCase].
     */
    fun generateQR(hash: String, request: HttpServletRequest) : ResponseEntity<ByteArrayResource>

     /* Creates a short url from details provided in [file].
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
    val limit: Int? = null,
    val wantQr: Boolean
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val qr: URI? = null
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
    val generateQRUseCase: GenerateQRUseCase,
    val infoUseCase: InfoUseCase,
    val reachableService: ReachableService,
    val securityService: SecurityService,
    val csvService: CsvService
) : UrlShortenerController {

    @Autowired
    private val validationQueue: BlockingQueue<String>? = null

    @Autowired
    private val csvQueue: BlockingQueue<MultipartFile>? = null

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()

            if (reachableService.isValidated(id) && securityService.isValidated(id)) {
                // URL has been validated
                if (!reachableService.isReachableUrl(it.target)) {
                    throw NotReachable(it.target)
                } else if (!securityService.isSecureUrl(it.target)) {
                    throw NotSafe(it.target)
                } else {
                    // URL is reachable and safe
                    h.location = URI.create(it.target)
                    ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
                }
            } else {
                throw NotValidated(it.target, es.unizar.urlshortener.core.RETRY_AFTER_VALIDATION)
            }
        }

    @PostMapping("/api/link", consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                limit = data.limit ?: 0
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            var qr: URI? = null

            if (data.wantQr) {
                qr = URI.create(url.toString() + "/qr")
            }

            // Send the URL to the validation queue
            println("Adding new URL to the validation queue: ${data.url}")
            validationQueue?.put(data.url)

            val response = ShortUrlDataOut(
                url = url,
                qr = qr
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @GetMapping("/api/link/{id}")
    override fun getInfo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<InfoID> {
        val info = infoUseCase.getInfo(id)

        return ResponseEntity<InfoID>(info, HttpStatus.OK)
    }

    @GetMapping("/{hash}/qr")
    override fun generateQR(@PathVariable hash: String, request: HttpServletRequest) : ResponseEntity<ByteArrayResource> =
        generateQRUseCase.generateQR(hash).let {
            val h = HttpHeaders()
            h.set(CONTENT_TYPE, IMAGE_PNG_VALUE)
            ResponseEntity<ByteArrayResource>(it, h, HttpStatus.OK)
        }

    @PostMapping("/api/bulk", consumes = [MULTIPART_FORM_DATA_VALUE])
    override fun csv(@RequestParam("file") file: MultipartFile, request: HttpServletRequest): ResponseEntity<String> {
        val h = HttpHeaders()
        if(file.isEmpty)  {
            h.add("Warning", "Empty file")
            return ResponseEntity<String>(h, HttpStatus.OK)
        } else {
            try {
                // Send the CSV to the CSV queue
                println("Adding new CSV to the CSV queue: ${file.originalFilename}")
                csvQueue?.put(file)

                val csv = csvService.create(
                    file = file,
                    data = ShortUrlProperties(
                        ip = request.remoteAddr,
                        sponsor = null
                    ),
                    request = request
                )

                val hash = csv.split(",")[1].split("/")[3].split("\n")[0]

                h.location = linkTo<UrlShortenerControllerImpl> { redirectTo(hash, request) }.toUri()
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
