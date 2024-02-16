package eu.symmetrysought.gothbuzz
import io.micronaut.http.*
import org.junit.jupiter.api.Test
import io.micronaut.gcp.function.http.*
import org.junit.jupiter.api.Assertions

class SignupFunctionTest {

    @Test
    fun testFunction() {
        HttpFunction().use { function ->
           val response = function.invoke(HttpMethod.GET, "/signup")
            Assertions.assertEquals(HttpStatus.OK, response.status)
        }
    }


    @Test
    fun testPost(){
        HttpFunction().use { function ->
            val input = SampleInputMessage("Test Name")
            val request = HttpRequest.POST("/signup", input).contentType(MediaType.APPLICATION_JSON_TYPE)
            val response = function.invoke(request)
            Assertions.assertEquals(HttpStatus.OK, response.status)
        }
    }
}
