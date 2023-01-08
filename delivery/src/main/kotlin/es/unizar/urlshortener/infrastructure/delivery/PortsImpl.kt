package es.unizar.urlshortener.infrastructure.delivery


import com.google.common.hash.*
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import io.github.g0dkar.qrcode.*
import org.apache.commons.validator.routines.*
import org.springframework.core.io.*
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpEntity
import org.springframework.util.MimeTypeUtils.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.net.URI
import java.nio.charset.*
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.servlet.http.HttpServletRequest


/**
 * Implementation of the port [ValidatorService].
 */
class ValidatorServiceImpl : ValidatorService {
    override fun isValid(url: String) = urlValidator.isValid(url)

    companion object {
        val urlValidator = UrlValidator(arrayOf("http", "https"))
    }
}

/**
 * Implementation of the port [HashService].
 */
@Suppress("UnstableApiUsage")
class HashServiceImpl : HashService {
    override fun hasUrl(url: String) = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
}

/**
 * Implementation of the port [ReachableService]
 */
class ReachableServiceImpl (
    private val shortUrlRepository: ShortUrlRepositoryService
) : ReachableService {

    private var restTemplate: RestTemplate = RestTemplate()

    /**
     * Returns true if the URL is reachable. Otherwise false
     */
    override fun isReachableUrl(url: String) : Boolean {
        val resp = restTemplate.getForEntity(url, String::class.java)
        return resp.statusCode.is2xxSuccessful
    }

    /**
     * We have a blocking queue that is reading the URLs and checking if they are safe or not.
     * How to see if the URL has been validated or not? Check the column "safe". If it matches
     * with null, it hasn't been validated yet
     */
    override fun isValidated(hash: String): Boolean {
        val shortUrlData = shortUrlRepository.findByHash(hash)!!
        return shortUrlData.properties.reachable != null
    }
}

/**
 * Implementation of [SecurityService]
 */
class SecurityServiceImpl (
    private val shortUrlRepository: ShortUrlRepositoryService
): SecurityService {

    private var restTemplate: RestTemplate = RestTemplate()

    private val API_URL =
        "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=AIzaSyCbSLMEHHQWcbQoETUAO30D_9KGmYyZ5iQ"

    /**
     * Verifies if a URL is secure according to the Google Safe Browsing Service
     */
    override fun isSecureUrl(url: String): Boolean {

        val conn = URI(API_URL)

        // Body will be as described in the Google Safe Browsing API: https://developers.google.com/safe-browsing/v4/lookup-api
        // In other words, body will be a list of features.
        val body = HttpEntity(BodyBuilder(url).getBody())

        // Make the request to the API
        val response = restTemplate.postForObject(conn, body, String::class.java)
        print("Response from Google Safe Browsing: $response")

        // If response returns a response with length 3, URL is safe. In other case, is unsafe
        if (response!!.length == 3) {
            println("The URL $url is secure")
        } else {
            println("Warning: The URL $url is insecure")
        }
        return response.length == 3
    }

    /**
     * We have a blocking queue that is reading the URLs and checking if they are safe or not.
     * How to see if the URL has been validated or not? Check the column "safe". If it matches
     * with null, it hasn't been validated yet
     */
    override fun isValidated(hash: String): Boolean {
        val shortUrlData = shortUrlRepository.findByHash(hash)!!
        return shortUrlData.properties.safe != null
    }

    /**
     * [BodyBuilder] is a helper to build the body for the request to the
     * Google Safe Browsing API.
     */
    private class BodyBuilder(url: String) : Serializable {

        val bodyJson: String = """
            {
                "client": {
                    "clientId": "IngWeb",
                    "clientVersion": "1.5.2"
                },
                "threatInfo": {
                    "threatTypes": ["THREAT_TYPE_UNSPECIFIED", "MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", 
                                    "POTENTIALLY_HARMFUL_APPLICATION"],
                    "platformTypes": ["ALL_PLATFORMS"],
                    "threatEntryTypes": ["URL"],
                    "threatEntries": [{"url": "$url"}
                    ]
                }
            }
        """

        fun getBody(): String {
            return bodyJson
        }
    }
}

 /**
 * Implementation of the port [ValidatorService].
 */
class RedirectionLimitServiceImpl : RedirectionLimitService {
     private val buckets : ConcurrentHashMap<String, Bucket> = ConcurrentHashMap()
     override fun addLimit(hash: String, limit: Int) {

         val limited  = Bandwidth.classic(
             limit.toLong(), Refill.intervally(limit.toLong(), Duration.ofMinutes(60)))
         buckets[hash] = Bucket.builder().addLimit(limited).build()
     }

     override fun proveLimit(hash: String) {
         if (buckets[hash] != null) {
             val prove = buckets[hash]?.tryConsumeAndReturnRemaining(1)
             if (!prove?.isConsumed!!) {
                 throw TooManyRedirections(hash, RETRY_AFTER_REDIRECTIONS)
             }
         }
     }
}

/**
 * Implementation of [CsvService]
 */
class CsvServiceImpl (
    private val createShortUrlUseCase: CreateShortUrlUseCase
) : CsvService {
    override fun create(file: MultipartFile, data: ShortUrlProperties, request: HttpServletRequest): String {
        var csv = String()

        file.inputStream.bufferedReader().forEachLine {
            csv += it
            try{
                val shortUrl = createShortUrlUseCase.create(
                    url = it,
                    data = data
                )
                val url = linkTo<UrlShortenerControllerImpl> { redirectTo(shortUrl.hash, request) }.toUri()
                csv += ",$url\n"
            }catch (e: Exception) {
                csv += ",fallo,,Invalid URL\n"
            }
        }
        return csv
    }
}

/**
 * Implementation of the port [QRService].
 */
class QRServiceImpl : QRService {
    override fun qr(url: String): ByteArrayResource =
        ByteArrayOutputStream().let{
            QRCode(url).render().writeImage(it)
            ByteArrayResource(it.toByteArray(), IMAGE_PNG_VALUE)
        }
}
