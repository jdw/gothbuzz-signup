package eu.symmetrysought.gothbuzz

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Controller("/")
class IndexController {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    @Produces(MediaType.TEXT_HTML)
    @Get
    fun index(): String {
        logger.info("Got a visit...")
        return IndexController::class.java.getResource("/web/index.html")!!.readText()
    }
}
