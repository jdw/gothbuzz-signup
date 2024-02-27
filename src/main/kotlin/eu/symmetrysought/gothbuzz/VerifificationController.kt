package eu.symmetrysought.gothbuzz

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.util.HttpClientAddressResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI


@Controller
class VerifyController(private val addressResolver: HttpClientAddressResolver) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val emailHandler = EmailHandler()


    @Get(value = "/verify/{code}")
    fun verify(@NonNull code: String, request: HttpRequest<*>): HttpResponse<*> {
        val client = addressResolver.resolve(request)
        logger.info("Got a visit to /verify/$code from $client...")


        if (!Glob.isValidVerificationCode(code)) {
            Glob.notifications.propagateError("""${client} tried to verify code "$code" which was invalid!""")

            val ret = VerificationFailedReturnMessage("The supplied code is not valid!")
            return HttpResponse.badRequest(ret).contentType(MediaType.APPLICATION_JSON)
        }
        val result = emailHandler.verifyCode(code)

        return if (result.isSuccess) {
            Glob.notifications.propagateAnnouncement("""${client} verified code "$code"!""")
            HttpResponse.redirect<HttpResponse<String>>(URI.create("/verified/")).status(HttpStatus.MOVED_PERMANENTLY)
        }
        else {
            Glob.notifications.propagateError("""${client} tried to verify code '$code' which was not found!""")
            val ret = VerificationFailedReturnMessage("We could not find the code supplied in our database!")
            HttpResponse.badRequest(ret).contentType(MediaType.APPLICATION_JSON)
        }
    }

    @Produces(MediaType.TEXT_HTML)
    @Get("/verified/")
    fun verified(request: HttpRequest<*>): String {
        val client = addressResolver.resolve(request)
        logger.info("Got a visit to /verified by $client...")
        val base = IndexController::class.java.getResource("/web/base.html")!!.readText()
        val data = IndexController::class.java.getResource("/web/verified.json")!!.readText()
        return Glob.applyTemplate(base, data)
    }
}


@Introspected
data class VerificationFailedReturnMessage(val returnMessage: String)
