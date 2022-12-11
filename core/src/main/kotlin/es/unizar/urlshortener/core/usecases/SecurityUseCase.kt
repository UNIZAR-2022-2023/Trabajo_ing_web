package es.unizar.urlshortener.core.usecases

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.springframework.http.HttpEntity
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * [SecurityUseCase] is the service that validates if an url is safe or not
 * according the Google Safe Browsing service.
 */
interface SecurityUseCase {
    fun isSecureUrl(url: String): Boolean

    fun isValidated(hash: String): Boolean

    fun isSecureHash(hash: String): Boolean

    fun isReachableURL(hash: String): Boolean
}

/**
 * Implementation of [SecurityUseCase]
 */
class SecurityUseCaseImpl (
    private val shortUrlRepository: ShortUrlRepositoryService
): SecurityUseCase {

    lateinit var restTemplate: RestTemplate

    private val API_URL = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=AIzaSyCbSLMEHHQWcbQoETUAO30D_9KGmYyZ5iQ"

    /**
     * Verifies if a URL is secure according to the Google Safe Browsing Service
     */
    override fun isSecureUrl(url: String): Boolean {

        val conn = URI(API_URL)

        // Body will be as described in the Google Safe Browsing API: https://developers.google.com/safe-browsing/v4/lookup-api
        // In other words, body will be a list of features.
        // We can ignore the client.
        val body = HttpEntity(jacksonObjectMapper().writeValueAsString(ThreatInfo (url)))

        // Make the request to the API
        val response = restTemplate.postForObject(conn, body, String::class.java)

        // If response returns an empty object in the response body, URL is safe
        return response.isNullOrEmpty()
    }

    /**
     * We have a blocking queue that is reading the URLs and checking if they are safe or not.
     * How to see if the URL has been validated or not? Check the column "safe". If it matches
     * with "not processed", it hasn't been validated yet
     */
    override fun isValidated(hash: String): Boolean {
        val shortUrlData = shortUrlRepository.findByHash(hash)!!
        return shortUrlData.properties.safe != "not validated"
    }

    /**
     * Returns true only if the URL associated to the hash is secure
     */
    override fun isSecureHash(hash: String): Boolean {
        val shortUrlData = shortUrlRepository.findByHash(hash)!!
        return shortUrlData.properties.safe == "safe"
    }

    /**
     * Returns true only if the URL associated is reachable
     */
    override fun isReachableURL(url: String): Boolean {
        return try {
            val resp = restTemplate.getForEntity(url, String::class.java)
            resp.statusCode.is2xxSuccessful
        } catch (e: Exception){
            false
        }
    }
}

/**
 * [ThreatInfo] is a helper to build the body for the request to the
 * Google Safe Browsing API.
 * @JsonProperty is very helpful to build the body for a request when parsing to JSON:
 * Example: https://stackoverflow.com/questions/12583638/when-is-the-jsonproperty-property-used-and-what-is-it-used-for
 */

class ThreatInfo (url: String) {

    @JsonProperty("threatTypes")
    private val threatTypes: List<String> = listOf(
        "THREAT_TYPE_UNSPECIFIED",
        "MALWARE",
        "SOCIAL_ENGINEERING",
        "UNWANTED_SOFTWARE",
        "POTENTIALLY_HARMFUL_APPLICATION"
    )

    @JsonProperty("platformTypes")
    private val platformTypes: List<String> = listOf(
        "ALL_PLATFORMS"
    )

    @JsonProperty("threatEntryTypes")
    private val threatEntryTypes: List<String> = listOf(
        "URL"
    )

    @JsonProperty("threatEntries")
    private val threatEntries: List<ThreatEntries> = listOf(ThreatEntries(url))
}

class ThreatEntries (url: String) {

    @JsonProperty("url")
    private var url: String = url
}
