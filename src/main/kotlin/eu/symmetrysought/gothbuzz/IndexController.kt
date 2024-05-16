package eu.symmetrysought.gothbuzz

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.util.HttpClientAddressResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Controller("/")
class IndexController(private val addressResolver: HttpClientAddressResolver) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    @Produces(MediaType.TEXT_HTML)
    @Get
    fun index(request: HttpRequest<*>): HttpResponse<*> {
        val client = addressResolver.resolve(request)
        logger.info("Got a visit to / from $client...")

        val base = IndexController::class.java.getResource("/web/base.html")!!.readText()
        val data = IndexController::class.java.getResource("/web/index.json")!!.readText()
        val form = IndexController::class.java.getResource("/web/form.html")!!.readText()
        val body = Glob.applyTemplate(base, data, form)

        return HttpResponse.ok(body).contentType(MediaType.TEXT_HTML)
    }
}
