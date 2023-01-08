package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 *  Information of the ID returned to the client
 */
data class InfoID(
    val id: String,
    val url: String,
    val isValidated: Boolean = true,
    val isReachable: Boolean = true,
    val isSafe: Boolean = true
)

interface InfoUseCase {
    fun getInfo(hash: String): InfoID
}

class InfoUseCaseImpl (
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val redirectionLimitService: RedirectionLimitService,
    private val reachableService: ReachableService,
    private val securityService: SecurityService
) : InfoUseCase {
    override fun getInfo (hash: String) : InfoID {
        val url = shortUrlRepository.findByHash(hash)!!.redirection.target

        if (reachableService.isValidated(hash) && securityService.isValidated(hash)) {
            //First we prove the limit of redirections
            redirectionLimitService.proveLimit(hash)
            // URL has been validated
            if (!reachableService.isReachableUrl(url)) {
                throw NotReachable(url)
            }
            else if (!securityService.isSecureUrl(url)) {
                throw NotSafe(url)
            } else {
                // URL is reachable and safe
                return InfoID (
                    id = hash,
                    url = url,
                )
            }
        } else {
            throw NotValidated(url, RETRY_AFTER_VALIDATION)
        }
    }
}
