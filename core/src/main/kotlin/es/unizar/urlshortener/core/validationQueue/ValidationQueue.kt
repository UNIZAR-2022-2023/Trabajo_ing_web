package es.unizar.urlshortener.core.validationQueue

import es.unizar.urlshortener.core.SecurityService
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque

/**
 * [ValidationQueue] is where the incoming URLs are going to be checked if they are safe or not
 * More info about blocking queue in https://www.baeldung.com/spring-async
 */
@Component
open class ValidationQueue (
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val securityService: SecurityService,
) {
    @Autowired
    private val queue : BlockingQueue<String> ?= null

    @Async("executorConfig")
    @Scheduled(fixedDelay = 200L)
    open fun executor () {
        try {
            println("COMIENZO")
            // Get the URL from the queue
            println(queue?.size)

            val url: String = queue!!.take()
            println("Taking a new URL: $url")

            // Verify if the URL is safe or not
            val isSafe = securityService.isSecureUrl(url)

            // Update the database
            val shortUrlData = shortUrlRepository.findByUrl(url)!!
            if (isSafe)
                shortUrlData.properties.safe = "safe"
            else
                shortUrlData.properties.safe = "not safe"

            shortUrlRepository.save(shortUrlData)

        } catch (e: InterruptedException) {
            println("Waiting for URL...")
        }
    }
}
