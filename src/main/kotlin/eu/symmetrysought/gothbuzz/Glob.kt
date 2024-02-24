package eu.symmetrysought.gothbuzz


import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        logger.info("All environment variables configured and found to be OK!")

        // Load Google Cloud Storage credentials
        val credentials =  try {
            GoogleCredentials.fromStream(envvar.GOTHBUZZ_BUCKET_SA_KEY.byteInputStream())
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"));
        }
        catch (_: Exception) {
            throw ExceptionInInitializerError("Failed getting Google credentials with GOTHBUZZ_BUCKET_SA_KEY!")
        }

        // Create Storage client
        val storage = try {
            StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .service
        }
        catch (_: Exception) {
            throw ExceptionInInitializerError("Failed getting storage!")
        }

        try {
            bucket = storage.get(envvar.GOTHBUZZ_BUCKET_NAME)
        }
        catch (_: Exception) {
            throw ExceptionInInitializerError("Could not get bucket with GOTHBUZZ_BUCKET_NAME!")
        }

        try {
            notifications = NotificationHandler.newBuilder().build()
        }
        catch (_: Exception) {
            throw ExceptionInInitializerError("Could not start notifications handler!")
        }

        logger.info("Glob initialized OK...")
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
        if (!listOf("local").contains(Glob.envvar.GOTHBUZZ_ENVIRONMENT_NAME)) return

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

        if ("" != bodyOverride) {
            ret = ret.replace("{{body}}", bodyOverride)
        }

        return ret
    }
}