package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.springframework.core.io.*

interface GenerateQRUseCase {
    fun generateQR(hash: String) : ByteArrayResource
}

class GenerateQRUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val qrService: QRService
) : GenerateQRUseCase {
    override fun generateQR(hash: String) : ByteArrayResource =
        shortUrlRepository.findByUrl(hash)?.let {
            qrService.qr(hash)
        } ?: throw QrNotFound(hash)
}