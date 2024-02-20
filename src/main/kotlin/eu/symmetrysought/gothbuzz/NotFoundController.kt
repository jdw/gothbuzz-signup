package eu.symmetrysought.gothbuzz

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import org.slf4j.Logger
import org.slf4j.LoggerFactory


@Controller("/404")
class NotFoundController() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    @Get
    fun notFound(request: HttpRequest<String>): HttpResponse<*> {
        logger.info("Inside notfound...")
        val body = IndexController::class.java.getResource("/web/base.html")!!.readText()
        val jsonData = IndexController::class.java.getResource("/web/404.json")!!.readText()
        return HttpResponse.badRequest(body).contentType(MediaType.TEXT_HTML)
    }
}