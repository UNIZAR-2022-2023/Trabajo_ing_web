package es.unizar.urlshortener.infrastructure.delivery

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.hash.*
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.SecurityService
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.RedirectionLimitService
import es.unizar.urlshortener.core.TooManyRedirections
import es.unizar.urlshortener.core.ValidatorService
import org.apache.commons.validator.routines.UrlValidator
import org.springframework.http.HttpEntity
import org.springframework.web.client.RestTemplate
import java.io.Serializable
import java.lang.management.ThreadInfo
import java.net.URI
import java.nio.charset.StandardCharsets
import org.springframework.http.MediaType.IMAGE_PNG_VALUE
import io.github.g0dkar.qrcode.*
import org.apache.commons.validator.routines.*
import org.springframework.core.io.*
import org.springframework.util.MimeTypeUtils.*
import java.io.*
import java.nio.charset.*

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
        println("Response from Google Safe Browsing: $response")

        // If response returns a response with length 3, URL is safe. In other case, is unsafe
        if (response!!.length == 3) {
            println("The URL $url is secure")
        } else {
            println("Warning: The URL $url is insecure")
        }
        return response.length == 3
    }

    override fun isReachable(url: String) : Boolean {
        return try {
            val resp = restTemplate.getForEntity(url, String::class.java)
            resp.statusCode.is2xxSuccessful
        } catch (e: Exception) {
            false
        }
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
     * Returns true only if the URL associated to the hash is secure
     */
    override fun isSecureHash(hash: String): Boolean {
        val shortUrlData = shortUrlRepository.findByHash(hash)!!
        return shortUrlData.properties.safe == "safe"
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
    override fun addLimit(hash: String, limit: Int) {

    }
    override fun proveLimit(hash: String) {
       /* if(){
            throw TooManyRedirections(hash)
        }*/
    }
}

/**
 * Implementation of the port [QRService].
 */
class QRServiceImpl : QRService {
    override fun qr(url: String): ByteArrayResource =
        ByteArrayOutputStream().let{
            QRCode(url).render().writeImage(it)
            val imageBytes = it.toByteArray()
            ByteArrayResource(imageBytes, IMAGE_PNG_VALUE)
        }
}
