package es.unizar.urlshortener.core.validationQueue

import es.unizar.urlshortener.core.SecurityService
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque

@Component
class Queue : BlockingQueue<String> by LinkedBlockingDeque()

/**
 * [SecurityQueue] is where the incoming URLs are going to be checked if they are safe or not
 * More info about blocking queue in https://www.baeldung.com/spring-async
 */
@Component
class SecurityQueue (
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val securityService: SecurityService,
    private val securityQueue: BlockingQueue<String>
) {
    @Async("executorConfig") // Background thread pool
    @Scheduled(fixedDelay = 200L) // Run this every 200 milliseconds
    fun executor () {
        try {
            // Get the URL from the queue
            val url: String = securityQueue.take()
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

        } catch (e: Exception) {
            println(e.toString())
        }
    }
}