package es.unizar.urlshortener.core.validationQueue

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue

/**
 * [CsvQueueConfig] is the configuration for the queue where the incoming CSVs
 * More info about blocking queue in https://www.baeldung.com/spring-async
 */

@Configuration
@EnableAsync
@EnableScheduling
open class CsvQueueConfig {

    /**
     * Because of the inheritance, all the variables of type BlockingQueue with the
     * annotation of @Autowired, will be injected
     */
    @Bean("csv")
    open fun queue(): BlockingQueue<MultipartFile> = LinkedBlockingQueue()

    @Bean("executorCsv")
    open fun executor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.maxPoolSize = 20
        executor.corePoolSize = 20
        executor.queueCapacity = 1000
        executor.initialize()
        return executor
    }
}
