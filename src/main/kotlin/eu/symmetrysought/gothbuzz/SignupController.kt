package eu.symmetrysought.gothbuzz

import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Controller("/signup")
class SignupController {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    @Post
    fun post(@Body inputMessage: SampleInputMessage): SampleReturnMessage {
        return SampleReturnMessage("Hello ${inputMessage.name}, thank you for sending the message")
    }
}

@Introspected
data class SampleInputMessage(val name: String)

@Introspected
data class SampleReturnMessage(val returnMessage: String)