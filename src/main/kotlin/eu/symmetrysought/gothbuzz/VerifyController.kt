package eu.symmetrysought.gothbuzz

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Produces
import io.micronaut.http.MediaType
import io.micronaut.core.annotation.Introspected
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Controller("/verify")
class VerifyController {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    @Produces(MediaType.TEXT_PLAIN)
    @Get
    fun index(): String {
        return "Example Response"
    }

    @Post
    fun post(@Body inputMessage: SampleInputMessage): SampleReturnMessage {
        return SampleReturnMessage("Hello ${inputMessage.name}, thank you for sending the message")
    }
}

@Introspected
data class VerifyInputMessage(val code: String)

@Introspected
data class VerificationFailedReturnMessage(val returnMessage: String)

@Introspected
data class VerificationSuccessReturnMessage(val returnMessage: String)