package eu.symmetrysought.gothbuzz

import io.micronaut.http.MediaType
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

@Controller
class SignupController() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Post("/signup")
    fun signup(@Body inputMessage: InputMessage): HttpResponse<*> {
        logger.info("Got a visit to /signup...")
        val emailHandler = EmailHandler()
        val email = inputMessage.email
        if (!emailHandler.isValidEmail(email)) {
            val ret = ReturnMessage("Please supply a valid email address!", ReturnCode.BAD_EMAIL)
            return HttpResponse.badRequest(ret).contentType(MediaType.APPLICATION_JSON)
        }

        if (emailHandler.hasEmail(email)) {
            val ret = ReturnMessage("Your email address has been added previously!", ReturnCode.NOT_NEW_EMAIL)
            return HttpResponse.ok(ret).contentType(MediaType.APPLICATION_JSON)
        }

        val res = emailHandler.sendVerificationEmail(email)
        logger.info(res.toString())

        return if (res.isSuccess) {
            emailHandler.addEmail(email, res.getOrThrow())
            HttpResponse.redirect<HttpResponse<String>>(URI.create("/signedup")).status(HttpStatus.NO_CONTENT)
        } else {
            val ret = ReturnMessage("Something went wrong, please try again at a later time!", ReturnCode.MAILSEND_ERROR)
            HttpResponse.badRequest(ret).contentType(MediaType.APPLICATION_JSON)
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

