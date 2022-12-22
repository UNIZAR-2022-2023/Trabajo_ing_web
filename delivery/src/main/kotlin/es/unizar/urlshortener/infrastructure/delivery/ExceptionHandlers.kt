package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.annotation.Resource

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ResponseBody
    @ExceptionHandler(value = [InvalidUrlException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun invalidUrls(ex: InvalidUrlException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [RedirectionNotFound::class])
    @ResponseStatus(HttpStatus.NOT_FOUND)
    protected fun redirectionNotFound(ex: RedirectionNotFound) = ErrorMessage(HttpStatus.NOT_FOUND.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [NotSafe::class])
    @ResponseStatus(HttpStatus.FORBIDDEN)
    protected fun notSafeUrls(ex: NotSafe) = ErrorMessage(HttpStatus.FORBIDDEN.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [NotValidated::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun notValidatedUrls(ex: NotValidated) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [TooManyRedirections::class])
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    protected fun TooManyRedirections(ex: TooManyRedirections) = ErrorMessage(HttpStatus.TOO_MANY_REQUESTS.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [NotReachable::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun notReachable(ex: NotReachable) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)
}

data class ErrorMessage(
    val statusCode: Int,
    val message: String?,
    val timestamp: String = DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now())
)
