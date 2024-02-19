package eu.symmetrysought.gothbuzz

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Produces
import io.micronaut.http.MediaType
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.PathVariable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Controller("/verify")
class VerifyController {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val emailHandler = EmailHandler()

    @Produces(MediaType.TEXT_PLAIN)
    @Get
    fun index(): String {
        return "Example Response"
    }

    @Get("/code={code}")
    fun get(@PathVariable("code") code: String): HttpResponse<*> {
        val result = emailHandler.verifyCode(code)

        return if (result.isSuccess) {
            val ret = VerificationSuccessReturnMessage("Your email address ${result.getOrNull()} is now verified!")
            HttpResponse.ok(ret).contentType(MediaType.APPLICATION_JSON)
        }
        else {
            val ret = VerificationFailedReturnMessage("We could not find the code supplied in our database!")
            HttpResponse.badRequest(ret).contentType(MediaType.APPLICATION_JSON)
        }
    }
}

@Introspected
data class VerifyInputMessage(val code: String)

@Introspected
data class VerificationFailedReturnMessage(val returnMessage: String)

@Introspected
data class VerificationSuccessReturnMessage(val returnMessage: String)