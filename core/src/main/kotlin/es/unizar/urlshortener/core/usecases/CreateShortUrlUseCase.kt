package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, data: ShortUrlProperties): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService,
    private val RedirectionLimitService: RedirectionLimitService
) : CreateShortUrlUseCase {
    override fun create(url: String, data: ShortUrlProperties): ShortUrl {
        //primero mirar si ya la tenemos guardada
        //sino comprobamos primero que es valida segura y alcanzable
        if (validatorService.isValid(url) && validatorService.isSecure(url) && validatorService.isReachable(url) {
                val id: String = hashService.hasUrl(url)
                val su = ShortUrl(
                    hash = id,
                    redirection = Redirection(target = url),
                    properties = ShortUrlProperties(
                        safe = data.safe,
                        ip = data.ip,
                        sponsor = data.sponsor
                    )
                )
                //Indicamos el limite de redirecciones
                if ( data.limit != null ) {
                    redirectionLimitService.addLimit(id, data.limit)
                }
                shortUrlRepository.save(su)

                //Desde aqui lanzamos el servicio del codigo qr

                return ShortUrl

            } else {
            throw InvalidUrlException(url)
            }
    }

}
