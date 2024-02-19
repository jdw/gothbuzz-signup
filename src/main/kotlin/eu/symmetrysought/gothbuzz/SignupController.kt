package eu.symmetrysought.gothbuzz

import com.google.gson.Gson
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Produces
import io.micronaut.http.MediaType
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Consumes
import jdk.jfr.ContentType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Controller("/signup")
class SignupController() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Post
    fun post(@Body inputMessage: SampleInputMessage): HttpResponse<*> {
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
            val ret = ReturnMessage("Your email has been added. Please, follow the instructions in the email!", ReturnCode.ALL_OK)
            HttpResponse.ok(ret).contentType(MediaType.APPLICATION_JSON)
        } else {
            val ret = ReturnMessage("Something went wrong, please try again at a later time!", ReturnCode.MAILSEND_ERROR)
            HttpResponse.badRequest(ret).contentType(MediaType.APPLICATION_JSON)
        }
    }
}

@Introspected
data class SampleInputMessage(val email: String)

