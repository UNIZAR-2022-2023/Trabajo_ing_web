package es.unizar.urlshortener.infrastructure.delivery

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorService
import es.unizar.urlshortener.core.usecases.ThreatInfo
import org.apache.commons.validator.routines.UrlValidator
import org.springframework.http.HttpEntity
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Implementation of the port [ValidatorService].
 */
class ValidatorServiceImpl : ValidatorService {
    override fun isValid(url: String) = urlValidator.isValid(url)

    lateinit var restTemplate: RestTemplate

    private val API_URL = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=AIzaSyCbSLMEHHQWcbQoETUAO30D_9KGmYyZ5iQ"


    override fun isReachable(url: String) : Boolean {
        return try {
            val resp = restTemplate.getForEntity(url, String::class.java)
            resp.statusCode.is2xxSuccessful
        } catch (e: Exception){
            false
        }
    }

    override fun isSecure(url: String) : Boolean {
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
 * Implementation of the port [ValidatorService].
 */
class RedirectionLimitServiceImpl : RedirectionLimitService {
    override fun addLimit(hash: String, limit: Int) {

    }
    override fun proveLimit(hash: String) {
        if(){
            throw TooManyRedirections(hash)
        }


    }
}

