package es.unizar.urlshortener.core.security

import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.usecases.SecurityUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 *  [Queue] is the class that implements the queue where URLs are going to be stored, so we can insert new
 *  URLs from one class and read them from another (Passing the correct argument to the constructor
 *  of both classes).
 */
@Component
class Queue : BlockingQueue<String> by LinkedBlockingQueue()

/**
 * [SecurityQueue] is where the incoming URLs are going to be checked if they are safe or not
 * More info about blocking queue in https://www.baeldung.com/spring-async
 */
@Component
class SecurityQueue (
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val securityUseCase: SecurityUseCase,
    private val queue: BlockingQueue<String>
) {
    @Async("executorConfig") // Background thread pool
    @Scheduled(fixedDelay = 200L) // Run this every 200 milliseconds
    fun executor () {
        try {
            // Get the URL from the queue
            val url = queue.take()
            println("Taking a new URL: $url")

            // Verify if the URL is safe or not
            val isSafe = securityUseCase.isSecureUrl(url)

            //verify if the URL is safe or not
            securityUseCase.isReachable(url)

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
