package eu.symmetrysought.gothbuzz


import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object Glob {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    val envvar: EnvironmentVariablesHandler
    val bucket: Bucket
    val notifications: NotificationHandler

    init {
        logger.info("Initializing the almighty Glob...")

        val environmentVariablesHandlerBuilder = EnvironmentVariablesHandler
            .newBuilder()
            .addEnvironmentVariablesFromEnvironment()
            .addImplementedEnvironmentVariables()

        // Throws exception (and so exits application) if there are any errors found
        environmentVariablesHandlerBuilder.checkErrors().takeIf { it.isFailure }?.let { result ->
            throw ExceptionInInitializerError(result.exceptionOrNull()?.message)
        }

        envvar = environmentVariablesHandlerBuilder.build()
        logger.info("Environment variables - OK!")

        // Load Google Cloud Storage credentials
        val credentials =  try {
            val ret = GoogleCredentials.fromStream(envvar.GOTHBUZZ_BUCKET_SA_KEY.byteInputStream())
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"));
            logger.info("Credentials - OK!")
            ret
        }
        catch (_: Exception) {
            throw ExceptionInInitializerError("Failed getting Google credentials with GOTHBUZZ_BUCKET_SA_KEY!")
        }


        // Create Storage client
        val storage = try {
            val ret = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .service
            logger.info("Storage - OK!")
            ret
        }
        catch (_: Exception) {
            throw ExceptionInInitializerError("Failed getting storage!")
        }

        try {
            bucket = storage.get(envvar.GOTHBUZZ_BUCKET_NAME)
            logger.info("Buckets - OK!")
        }
        catch (_: Exception) {
            throw ExceptionInInitializerError("Could not get bucket from environment variable GOTHBUZZ_BUCKET_NAME!")
        }


        try {
            notifications = NotificationHandler.newBuilder()
                .addCredentials(envvar.GOTHBUZZ_WORKFLOW_EXEC)
                .build()
            logger.info("Notifications - OK!")
        }
        catch (_: Exception) {
            throw ExceptionInInitializerError("Could not start notifications handler!")
        }

    }

    fun generateRandomString(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..envvar.GOTHBUZZ_VERIFICATION_CODE_LENGTH.toInt())
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }


    fun isValidVerificationCode(code: String): Boolean {
        if (envvar.GOTHBUZZ_VERIFICATION_CODE_LENGTH.toInt() != code.length) return false

        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        return !code.any { !charPool.contains(it) }
    }

    fun logDebug(logger: Logger, message: String, throwable: Throwable) {
        if (!listOf("local").contains(envvar.GOTHBUZZ_ENVIRONMENT_NAME)) return

        val filename = throwable.stackTrace[0].fileName
        val lineNumber = throwable.stackTrace[0].lineNumber

        logger.info("($filename:$lineNumber) -> $message")
    }


    fun applyTemplate(base: String, data: String, bodyOverride: String = ""): String {
        val type = object : TypeToken<Map<String, String>>() {}.type
        val variables: Map<String, String> = Gson().fromJson(data, type)
        var ret = ""

        variables.forEach { (name, value) ->
            if ("" == value) return@forEach
            ret = if ("" == ret) base.replace("{{$name}}", value)
                else ret.replace("{{$name}}", value)
        }

        ret = ret.replace("{{surveymessage}}", renderSurveyMessage())

        if ("" != bodyOverride) {
            ret = ret.replace("{{body}}", bodyOverride)
        }

        return ret
    }

    private fun renderSurveyMessage(): String {
        var ret = "Please take our surveys and don't hesitate to suggest further questions! It really helps us understand how to develop the site and what wishes and demands the scene has!"

        val hrefs = arrayOf(
            "https://docs.google.com/forms/d/1PTLzNUi1IUWgZSxrwq2_R3EXp6Azl4kMTF2pUauwBBg/prefill",
            "https://docs.google.com/forms/d/10Qf9wkP1omDncP269fcMod1pG0KAlRoYPi83Jt7UdMY/prefill")
        val comments = arrayOf(
            "The survey for you as a professional in the goth scene!",
            "The survey for you as a private person in the goth scene!")
        val texts = arrayOf(
            "professionals",
            "General survey! \uD83E\uDEE1")

        ret += """<ul>"""
        for (idx in hrefs.indices) {
            val t = texts[idx]
            val c = comments[idx]
            val h = hrefs[idx]

            ret += """<li><a href="$h">$t</a> - $c</li>"""
        }
        ret += "</ul>"

        return ret
    }
}