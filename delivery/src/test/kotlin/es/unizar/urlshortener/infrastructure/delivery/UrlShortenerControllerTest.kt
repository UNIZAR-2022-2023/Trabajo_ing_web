package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import es.unizar.urlshortener.core.usecases.GenerateQRUseCase
import org.junit.jupiter.api.Disabled
import org.springframework.core.io.*

@WebMvcTest
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class,
        RestResponseEntityExceptionHandler::class]
)
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var redirectUseCase: RedirectUseCase

    @MockBean
    private lateinit var logClickUseCase: LogClickUseCase

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @MockBean
    private lateinit var securityService: SecurityService

    @MockBean
    private lateinit var generateQRUseCase: GenerateQRUseCase

    @MockBean
    private lateinit var reachableService: ReachableService

    // Bean necessary for [UrlShortenerControllerImpl]
    @MockBean
    private lateinit var csvService: CsvService

    /**
     * Test de GET /{id}
     */
    @Test
    fun `redirectTo returns a redirect when the key exists and is secure`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))

        given(reachableService.isValidated("key")).willReturn(true)
        given(reachableService.isReachableUrl("http://example.com/")).willReturn(true)
        given(securityService.isValidated("key")).willReturn(true)
        given(securityService.isSecureUrl("http://example.com/")).willReturn(true)

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns forbidden when the key exists but is not secure`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw NotSafe("key") }

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.statusCode").value(403))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns bad request when the key exists but is not validated`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw NotValidated("key") }

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns a bad request when the key exists and the website is unreachable`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/health"))
        given(
            reachableService.isReachableUrl("http://example.com/health")
        ).willAnswer { throw NotReachable("http://example.com/healt") }

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isBadRequest)
    }

    @Disabled
    @Test
    fun `redirectTo returns a too many redirections when limit is reach`() {
        given(reachableService.isValidated("key")).willReturn(true)
        given(reachableService.isReachableUrl("http://example.com/")).willReturn(true)
        given(securityService.isValidated("key")).willReturn(true)
        given(securityService.isSecureUrl("http://example.com/")).willReturn(true)

        mockMvc.perform((post("/api/link")
            .param("url", "http://www.example.com/")
            .param("limit", "1")))

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isTemporaryRedirect)
            .andDo(print())

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isTooManyRequests)
    }

    /**
     * Test de POST /api/link
     */
    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("wantQr", "0")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
    }

    @Test
    fun `creates returns bad request if it cant compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "ftp://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willAnswer { throw InvalidUrlException("ftp://example.com/") }

        mockMvc.perform(
            post("/api/link")
                .param("url", "ftp://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `creates returns bad request if it cant reach the website`() {
        given(
                reachableService.isReachableUrl("http://example.com/health")
        ).willAnswer { throw NotReachable("http://example.com/healt") }

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/health")
                .param("qr", "false")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
    }

    /**
     * Test for the QRs
     */
    @Test
    fun `redirectTo returns a qr code when the key exists`() {
        given(generateQRUseCase.generateQR("key")).willReturn(ByteArrayResource("test".toByteArray()))
        mockMvc.perform(get("/{hash}/qr", "key"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(content().bytes("test".toByteArray()))
    }

    @Test
    fun `qr() returns a not found when the key does not exist`() {
        given(generateQRUseCase.generateQR("key")).willAnswer { throw QrNotFound("key") }
        mockMvc.perform(get("/{hash}/qr", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)
    }

}