package eu.symmetrysought.gothbuzz

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import org.slf4j.Logger
import org.slf4j.LoggerFactory


@Controller
class NotFoundController() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    @Get("/404")
    fun notFound(request: HttpRequest<String>): HttpResponse<*> {
        logger.info("Got a visit to /404...")
        val body = IndexController::class.java.getResource("/web/base.html")!!.readText()
        val jsonData = IndexController::class.java.getResource("/web/404.json")!!.readText()
        return HttpResponse.badRequest(body).contentType(MediaType.TEXT_HTML)
    }
}