package eu.symmetrysought.gothbuzz

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Produces
import io.micronaut.http.MediaType
import io.micronaut.core.annotation.Introspected

@Controller("/")
class IndexController {

    @Produces(MediaType.TEXT_PLAIN)
    @Get
    fun index(): String {
        return "Example Response"
    }
}
