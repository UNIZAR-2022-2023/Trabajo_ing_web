package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.springframework.web.multipart.MultipartFile

/**
 * Given the name of a csv file with a URL per line generates another csv file
 * with the returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CsvUseCase {
    fun create(file: MultipartFile, data: ShortUrlProperties): String
}

/**
 * Implementation of [CsvUseCase].
 */
class CsvUseCaseImpl( private val createShortUrlUseCase: CreateShortUrlUseCase
) : CsvUseCase {
    override fun create(file: MultipartFile, data: ShortUrlProperties): String {
        var csv = String()

        file.inputStream.bufferedReader().forEachLine {
            csv += it
            try{
                val shortUrl = createShortUrlUseCase.create(
                    url = it,
                    data = data
                )
                csv += ",http://localhost:8080/${shortUrl.hash}\n"
            }catch (e: Exception) {
                csv += ",fallo,,Invalid URL\n"
            }



        }
        return csv
    }
}
