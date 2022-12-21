package es.unizar.urlshortener.core.validationQueue

import es.unizar.urlshortener.core.ReachableService
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
    private val reachableService: ReachableService
) {
    @Autowired
    private val validationQueue : BlockingQueue<String> ?= null

    @Async("executorValidation")
    @Scheduled(fixedDelay = 300L)
    open fun executor () {
        try {
            // Get the URL from the queue
            val url: String = validationQueue!!.take()
            println("Taking a new URL: $url")

            // Verify if the URL is safe or not
            val isSafe = securityService.isSecureUrl(url)

            // Verify if is reachable
            val isRechable = reachableService.isReachableUrl(url)

            // Update the database
            val shortUrlData = shortUrlRepository.findByUrl(url)!!
            shortUrlData.properties.safe = isSafe
            shortUrlData.properties.reachable = isRechable

            shortUrlRepository.save(shortUrlData)

        } catch (e: InterruptedException) {
            println(e.message)
        }
    }
}
