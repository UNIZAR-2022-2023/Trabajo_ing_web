package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 * **Note**: This is an example of functionality.
 */
interface RedirectUseCase {
    fun redirectTo(key: String): Redirection
}

/**
 * Implementation of [RedirectUseCase].
 */
class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val redirectionLimitService: RedirectionLimitService
) : RedirectUseCase {
    override fun redirectTo(key: String) : Redirection {
        val redirection = shortUrlRepository
            .findByHash(key)
            ?.redirection
            ?: throw RedirectionNotFound(key)
        // check de number of redirections
        redirectionLimitService.proveLimit(key)
        val urlShort = shortUrlRepository.findByHash(key)
        if (urlShort != null) {
            if (urlShort.properties.reachable != null && urlShort.properties.safe != null) {
                //is checked
                if (urlShort.properties.safe!!) {
                    throw NotSafe(key)
                }
            } else {
                // no validated
                throw NotValidated(key)
            }
        }
        return redirection
    }
}
