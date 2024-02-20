package eu.symmetrysought.gothbuzz

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import io.micronaut.http.uri.UriBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


class EmailHandler() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\$")
        return emailRegex.matches(email)
    }

    fun hasEmail(email: String): Boolean {
        val blob = Glob.bucket.get("${Glob.GOTHBUZZ_ENVIRONMENT_NAME}/${GATHERED_EMAILS}")!!
        val jsonData = String(blob.getContent()!!)

        Glob.logDebug(logger, "jsonData=$jsonData")
        val type = object : TypeToken<Map<String, Signup>>() {}.type
        val signups: Map<String, Signup> = Gson().fromJson(jsonData, type)

        return signups.containsKey(email)
    }

    fun addEmail(email: String, code: String) {
        val blob = Glob.bucket.get("${Glob.GOTHBUZZ_ENVIRONMENT_NAME}/${GATHERED_EMAILS}")!!
        val jsonDataIn = String(blob.getContent()!!)
        val type = object : TypeToken<MutableMap<String, Signup>>() {}.type
        val signups: MutableMap<String, Signup> = Gson().fromJson(jsonDataIn, type)
        signups[email] = Signup(email, Status.UNVERIFIED, code)
        val jsonDataOut = Gson().toJson(signups)

        Glob.logDebug(logger, "jsonDataOut=$jsonDataOut")

        Glob.bucket.create("${Glob.GOTHBUZZ_ENVIRONMENT_NAME}/$GATHERED_EMAILS", jsonDataOut.encodeToByteArray())
    }

    fun verifyCode(code: String): Result<String> {
        val blob = Glob.bucket.get("${Glob.GOTHBUZZ_ENVIRONMENT_NAME}/${GATHERED_EMAILS}")!!
        val jsonDataIn = String(blob.getContent()!!)
        val type = object : TypeToken<MutableMap<String, Signup>>() {}.type
        val signups: MutableMap<String, Signup> = Gson().fromJson(jsonDataIn, type)

        val signupFiltered = signups.filter { code == it.value.code }.toList()
        return when (signupFiltered.size) {
            0 -> Result.failure(Exception("Code was not found in the database!"))
            1 -> {
                val signup = Signup(signupFiltered[0].second.email, Status.VERIFIED, "")
                signups[signup.email] = signup
                val jsonDataOut = Gson().toJson(signups)

                Glob.logDebug(logger, "jsonDataOut=$jsonDataOut")

                Glob.bucket.create("${Glob.GOTHBUZZ_ENVIRONMENT_NAME}/$GATHERED_EMAILS", jsonDataOut.encodeToByteArray())

                Result.success(signup.email)
            }
            else -> Result.failure(Exception("More then one email address with the given code was found!"))
        }


    }
    fun sendGridDynamicTemplate(toAddress: String): Result<String> {
        val code = Glob.generateRandomString()
        val from = Email(Glob.GOTHBUZZ_NO_REPLY)
        from.name = "goth buzz"
        val subject = "goth.buzz verification email!"
        val to = Email(toAddress)
        val content = Content("html", EmailHandler::class.java.getResource("/web/verify_email.html")!!.readText().replace("{{code}}", code))
        val mail = Mail(from, subject, to, content)
        mail.personalization.get(0).addSubstitution("code", code);

        val sg = SendGrid(Glob.GOTHBUZZ_SENDGRID_API_KEY)
        val request = Request()

        return try {
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            val response = sg.api(request)
            if (response.statusCode.toString().startsWith("2")) {
                logger.info(response.statusCode.toString())
                logger.info(response.body)
                logger.info(response.headers.toString())

                Result.success(code)
            }
            else {
                Result.failure(Exception("Sending email failed! Status code ${response.statusCode}"))
            }
        } catch (ex: IOException) {
            Result.failure(Exception(ex.message))
        }
    }
    fun sendVerificationEmail(toAddress: String): Result<String> {
        logger.info("Entered sendVerificationEmail... ")
        val code = Glob.generateRandomString()
        val from = Email(Glob.GOTHBUZZ_NO_REPLY)
        from.name = "goth buzz"
        val subject = "goth.buzz verification email!"
        val to = Email(toAddress)
        val content = Content("text/html", EmailHandler::class.java.getResource("/web/verify_email.html")!!.readText().replace("{{code}}", code))
        val mail = Mail(from, subject, to, content)

        val sg = SendGrid(Glob.GOTHBUZZ_SENDGRID_API_KEY)
        val request = Request()
        return try {
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            val response = sg.api(request)
            if (response.statusCode.toString().startsWith("2")) {
                logger.info(response.statusCode.toString())
                logger.info(response.body)
                logger.info(response.headers.toString())

                Result.success(code)
            }
            else {
                Result.failure(Exception("Sending email failed! Status code ${response.statusCode}"))
            }
        } catch (ex: IOException) {
            Result.failure(Exception(ex.message))
        }
    }
    fun sendVerificationEmailMailerSendSux(email: String): Result<String> {
        val uri: URI = UriBuilder.of("https://api.mailersend.com/v1/email")
            .build()
        val code = Glob.generateRandomString()
        val link = if ("local" == Glob.GOTHBUZZ_ENVIRONMENT_NAME) "localhost:8080/verify/code=$code"
            else "goth.buzz/verify/code=$code"

        val body = """{
            "subject": "Welcome to goth.buzz - Please verify your email!",
    "from": {
        "email": "${Glob.GOTHBUZZ_NO_REPLY}",
        "name": "goth buzz"
    },
    "to": [
        {
            "email": "$email"
        }
    ],
    "variables": [
      {
        "email": "$email",
        "substitutions": [
          {
            "var": "link",
            "value": "$link"
          }
        ]
      }
    ],
    "template_id": "neqvygmjw8wg0p7w"}"""

        val body2 = """{
    "from": {
      "email": "${Glob.GOTHBUZZ_NO_REPLY}",
      "name": "goth buzz"
    },
    "to": [
      {
        "email": "$email",
      }
    ],
    "subject": "Hello from goth.buzz!",
    "text": "This is just a friendly hello from your friends at goth.buzz to verify this email over at $link",
    "html": "<b>This is just a friendly hello from your friends at goth.buzz to verify this email over at <a href="$link">goth.buzz</a>.</b>",
    "variables": [
      {
        "email": "$email",
        "substitutions": [
      
        ]
      }
    ]
  }"""
        logger.info(body)

        val httpClient = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type","application/json")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Authorization","Bearer <token>")
            .uri(uri)
            .build();

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("response=$response")
        logger.info(response.body())

        if (response.statusCode().toString().startsWith("2")) {
            return Result.success(code)
        }
        else {
            return Result.failure(Exception("Error in request to email API!"))
        }

    }


    companion object {
        private val GATHERED_EMAILS = "gathered_emails.json"
    }
}