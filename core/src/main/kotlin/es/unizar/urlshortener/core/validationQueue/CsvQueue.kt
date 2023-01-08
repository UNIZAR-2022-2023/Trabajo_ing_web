package es.unizar.urlshortener.core.validationQueue

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.BlockingQueue

/**
 * [ValidationQueue] is where the incoming URLs are going to be checked if they are safe or not
 * More info about blocking queue in https://www.baeldung.com/spring-async
 */
@Component
open class CsvQueue (
) {
    @Autowired
    private val csvQueue : BlockingQueue<MultipartFile> ?= null

    @Autowired
    private val validationQueue : BlockingQueue<String>?= null

    @Async("executorCsv")
    @Scheduled(fixedDelay = 300L)
    open fun executor () {
        try {
            val file: MultipartFile = csvQueue!!.take()
            println("Taking a new CSV: $file")

            file.inputStream.bufferedReader().forEachLine {
                validationQueue?.put(it)
            }

        } catch (e: InterruptedException) {
            println(e.message)
        }
    }
}
