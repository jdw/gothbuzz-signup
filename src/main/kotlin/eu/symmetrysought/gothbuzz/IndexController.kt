package eu.symmetrysought.gothbuzz

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Produces
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

@Controller
class IndexController {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    @Produces(MediaType.TEXT_HTML)
    @Get("/")
    fun index(request: HttpRequest<*>): HttpResponse<*> {
        logger.info("Got a visit...")

        return when (request.path) {
            "/", "/index.html", "/index.htm" -> {
                val body = IndexController::class.java.getResource("/web/index.html")!!.readText()
                HttpResponse.ok(body).contentType(MediaType.TEXT_HTML)
            }
            else -> HttpResponse.redirect<HttpResponse<String>>(URI.create("/404/")).status(HttpStatus.MOVED_PERMANENTLY)
        }
    }

    @Produces(MediaType.TEXT_HTML)
    @Get("/{path}")
    fun index(@PathVariable("path") path: String, request: HttpRequest<*>): HttpResponse<*> {
        logger.info("Got a visit to /{path}...")
        logger.info("${request.path}")
        logger.info("${request.cookies}")
        logger.info("${request.method}")
        logger.info("${request.methodName}")
        logger.info("${request.origin}")
        logger.info("${request.uri}")
        logger.info("${request.toString()}")
        logger.info("Got a visit to /{path}...2")

        return when (request.path) {
            "/", "/index.html", "/index.htm" -> {
                val body = IndexController::class.java.getResource("/web/index.html")!!.readText()
                HttpResponse.ok(body).contentType(MediaType.TEXT_HTML)
            }
            "/verified.html" -> {
                val body = IndexController::class.java.getResource("/web/verified.html")!!.readText()
                HttpResponse.ok(body).contentType(MediaType.TEXT_HTML)
            }
            else -> {
                val body = IndexController::class.java.getResource("/web/404.html")!!.readText()
                HttpResponse.notFound(body).contentType(MediaType.TEXT_HTML)
            }
        }
    }
}
