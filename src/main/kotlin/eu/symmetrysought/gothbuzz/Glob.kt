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
    private val environmentVariablesValue: MutableMap<String, String?> = mutableMapOf()
    val bucket: Bucket
    val notifications: NotificationHandler
    val GOTHBUZZ_SENDGRID_API_KEY: String by environmentVariablesValue
    val GOTHBUZZ_ENVIRONMENT_NAME: String by environmentVariablesValue
    private val GOTHBUZZ_BUCKET_NAME: String by environmentVariablesValue
    private val GOTHBUZZ_BUCKET_SA_KEY: String by environmentVariablesValue
    val GOTHBUZZ_NO_REPLY: String by environmentVariablesValue
    private val GOTHBUZZ_VERIFICATION_CODE_LENGTH: String by environmentVariablesValue
    val GOTHBUZZ_GOOGLE_LOCATION_ID: String by environmentVariablesValue
    val GOTHBUZZ_PROJECT_ID: String by environmentVariablesValue
    val GOTHBUZZ_WORKFLOW_EXEC: String by environmentVariablesValue
    private val GOTHBUZZ_ERRORS_WORKFLOW: String by environmentVariablesValue
    private val GOTHBUZZ_ANNOUNCEMENTS_WORKFLOW: String by environmentVariablesValue
    private val GOTHBUZZ_BUZZ_WORKFLOW: String by environmentVariablesValue

    enum class EnvironmentVariableError() {
        NOT_SET, EMPTY, PARSER_FAILED, NOT_CONFIGURED
    }
    init {
        logger.info("Initializing the almighty Glob...")
        val implementedEnvironmentVariables = setOf("GOTHBUZZ_BUZZ_WORKFLOW", "GOTHBUZZ_ANNOUNCEMENTS_WORKFLOW", "GOTHBUZZ_ERRORS_WORKFLOW", "GOTHBUZZ_WORKFLOW_EXEC", "GOTHBUZZ_PROJECT_ID", "GOTHBUZZ_GOOGLE_LOCATION_ID", "GOTHBUZZ_ENVIRONMENT_NAME", "GOTHBUZZ_BUCKET_NAME", "GOTHBUZZ_BUCKET_SA_KEY", "GOTHBUZZ_SENDGRID_API_KEY", "GOTHBUZZ_NO_REPLY", "GOTHBUZZ_VERIFICATION_CODE_LENGTH")
        val environmentVariablesErrors: MutableMap<String, MutableList<EnvironmentVariableError>> = implementedEnvironmentVariables.associateWith { mutableListOf<EnvironmentVariableError>() }.toMutableMap()
        val environmentVariableParsers: Map<String, (String) -> Boolean> = mapOf(
            "GOTHBUZZ_ENVIRONMENT_NAME" to { value -> "prod" == value || "local" == value },
            "GOTHBUZZ_SENDGRID_API_KEY" to { value -> value.startsWith("SG.") && 69 == value.length },
            "GOTHBUZZ_NO_REPLY" to { value -> EmailHandler.isValidEmail(value) },
            "GOTHBUZZ_VERIFICATION_CODE_LENGTH" to { value -> try { value.toInt(); true} catch(_: Exception) { false } }
        )

        implementedEnvironmentVariables.forEach { name ->
            val value = System.getenv(name)
            if (null == value) {
                environmentVariablesErrors[name]?.add(EnvironmentVariableError.NOT_SET)
                return@forEach
            }

            environmentVariablesValue[name] = value

            if ("" == value) {
                environmentVariablesErrors[name]?.add(EnvironmentVariableError.EMPTY)
            }

            environmentVariableParsers[name]?.let { parser ->
                if (!parser.invoke(value)) {
                    environmentVariablesErrors[name]?.add(EnvironmentVariableError.PARSER_FAILED)
                }
            }
        }

        System.getenv().filter { (name, _) ->
                name.startsWith("GOTHBUZZ_")
            }.map { (name, value) ->
                if (!implementedEnvironmentVariables.contains(name)) {
                    environmentVariablesErrors[name] = mutableListOf()
                    environmentVariablesErrors[name]?.add(EnvironmentVariableError.NOT_CONFIGURED)

                    if ("" == value) {
                        environmentVariablesErrors[name]?.add(EnvironmentVariableError.EMPTY)
                    }
                }
        }

        // Throws exception (and so exits application) if there are any errors found
        environmentVariablesErrors.entries.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }?.let {
            var msg = ""
            it.forEach { (name, errors) -> msg +=  "$name : $errors" }
            throw ExceptionInInitializerError(msg)
        }

        logger.info("All environment variables configured and found to be OK!")

        // Load Google Cloud Storage credentials
        val credentials =  try {
            GoogleCredentials.fromStream(GOTHBUZZ_BUCKET_SA_KEY.byteInputStream())
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
            bucket = storage.get(GOTHBUZZ_BUCKET_NAME)
        }
        catch (_: Exception) {
            throw ExceptionInInitializerError("Could not get bucket with GOTHBUZZ_BUCKET_NAME!")
        }

        try {
            notifications = NotificationHandler.newBuilder()
                .setChannelWorkflowId(NotificationHandler.NotificationChannel.ERRORS, GOTHBUZZ_ERRORS_WORKFLOW)
                .setChannelWorkflowId(NotificationHandler.NotificationChannel.ANNOUNCEMENTS, GOTHBUZZ_ANNOUNCEMENTS_WORKFLOW)
                .setChannelWorkflowId(NotificationHandler.NotificationChannel.BUZZ, GOTHBUZZ_BUZZ_WORKFLOW)
                .build()
        }
        catch (_: Exception) {
            throw ExceptionInInitializerError("Could not start notifications handler!")
        }

        logger.info("Glob initialized OK...")
    }

    fun generateRandomString(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..GOTHBUZZ_VERIFICATION_CODE_LENGTH.toInt())
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }


    fun isValidVerificationCode(code: String): Boolean {
        if (GOTHBUZZ_VERIFICATION_CODE_LENGTH.toInt() != code.length) return false

        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        return !code.any { !charPool.contains(it) }
    }

    fun logDebug(logger: Logger, message: String) {
        if ("prod" != GOTHBUZZ_ENVIRONMENT_NAME)
            logger.info(message)
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