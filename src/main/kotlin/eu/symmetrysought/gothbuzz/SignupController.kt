package eu.symmetrysought.gothbuzz

import io.micronaut.http.MediaType
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.server.util.HttpClientAddressResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

@Controller
class SignupController(private val addressResolver: HttpClientAddressResolver) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Post("/signup")
    fun signup(@Body inputMessage: InputMessage, request: HttpRequest<*>): HttpResponse<String> {
        val client = addressResolver.resolve(request)
        logger.info("Got a visit to /signup from $client...")

        val email = inputMessage.email
        if (!EmailHandler.isValidEmail(email)) {
            Glob.notifications.propagateError("$client tried to use invalid email $email!")
            return HttpResponse.badRequest("Please supply a valid email address!").contentType(MediaType.TEXT_PLAIN)
        }

        val emailHandler = EmailHandler()
        if (emailHandler.hasEmail(email)) {
            Glob.notifications.propagateError("$client tried to use already known email $email!")
            return HttpResponse.badRequest("Your email address has been added previously!").contentType(MediaType.TEXT_PLAIN)
        }

        val res = emailHandler.sendVerificationEmail(email)

        return if (res.isSuccess) {
            emailHandler.addEmail(email, res.getOrThrow())
            Glob.notifications.propagateAnnouncement("$client got signed up with email $email!")
            HttpResponse.ok("OK").contentType(MediaType.TEXT_PLAIN)
        } else {
            HttpResponse.badRequest("Something went wrong, please try again at a later time!").contentType(MediaType.TEXT_PLAIN)
        }
    }


    @Get("/signedup")
    fun signedup(): HttpResponse<*> {
        logger.info("Got a visit to /signedup...")
        val base = IndexController::class.java.getResource("/web/base.html")!!.readText()
        val data = IndexController::class.java.getResource("/web/signedup.json")!!.readText()
        val body = Glob.applyTemplate(base, data)

        return HttpResponse.ok(body).contentType(MediaType.TEXT_HTML)
    }
}

@Introspected
data class InputMessage(val email: String)

