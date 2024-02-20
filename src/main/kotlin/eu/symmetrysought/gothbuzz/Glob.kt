package eu.symmetrysought.gothbuzz


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.gson.Gson
import kotlin.random.Random

object Glob {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val environmentVariablesValue: MutableMap<String, String> = mutableMapOf()
    val bucket: Bucket
    val GOTHBUZZ_SENDGRID_API_KEY: String by environmentVariablesValue
    val GOTHBUZZ_ENVIRONMENT_NAME: String by environmentVariablesValue
    private val GOTHBUZZ_BUCKET_NAME: String by environmentVariablesValue
    private val GOTHBUZZ_BUCKET_SA_KEY: String by environmentVariablesValue
    val GOTHBUZZ_NO_REPLY: String by environmentVariablesValue

    enum class EnvironmentVariableError() {
        NOT_SET, EMPTY, PARSER_FAILED, NOT_CONFIGURED
    }
    init {
        logger.info("Initializing the almighty Glob...")
        val implementedEnvironmentVariables = setOf("GOTHBUZZ_ENVIRONMENT_NAME", "GOTHBUZZ_BUCKET_NAME", "GOTHBUZZ_BUCKET_SA_KEY", "GOTHBUZZ_SENDGRID_API_KEY", "GOTHBUZZ_NO_REPLY")
        val environmentVariablesErrors: MutableMap<String, MutableList<EnvironmentVariableError>> = implementedEnvironmentVariables.associateWith { mutableListOf<EnvironmentVariableError>() }.toMutableMap()
        val environmentVariableParsers: Map<String, (String) -> Boolean> = mapOf(
            "GOTHBUZZ_ENVIRONMENT_NAME" to { value -> "prod" == value || "local" == value },
            "GOTHBUZZ_SENDGRID_API_KEY" to { value -> value.startsWith("SG.") && 69 == value.length }
        )

        implementedEnvironmentVariables.forEach { name ->
            System.getenv(name) ?: {
                environmentVariablesErrors[name]?.add(EnvironmentVariableError.NOT_SET)
            }
        }

        System.getenv().filter { (name, _) ->
                name.startsWith("GOTHBUZZ_")
            }.map { (name, value) ->
                if (!implementedEnvironmentVariables.contains(name)) {
                    environmentVariablesErrors[name]?.add(EnvironmentVariableError.NOT_CONFIGURED)
                }
                if ("" == value) {
                    environmentVariablesErrors[name]?.add(EnvironmentVariableError.EMPTY)
                }
                if (environmentVariableParsers.containsKey(name)) {
                    if (!environmentVariableParsers[name]!!.invoke(value)) {
                        environmentVariablesErrors[name]?.add(EnvironmentVariableError.PARSER_FAILED)
                    }
            }
        }

        val totalErrors = environmentVariablesErrors.filter { (name, errors) ->
            errors.isNotEmpty()
        }

        if (totalErrors.isNotEmpty()) {
            throw Exception(Gson().toJson(totalErrors))
        }

        logger.info("All environment variables configured...")
        // "environmentVariablesErrors" now contains all possible GOTHBUZZ_ env vars and we have passed env vars checks
        environmentVariablesErrors.forEach { (name, _) ->
            environmentVariablesValue[name] = System.getenv(name) ?: ""
        }


        // Load Google Cloud Storage credentials
        val credentials = GoogleCredentials.fromStream(GOTHBUZZ_BUCKET_SA_KEY.byteInputStream())
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

        // Create Storage client
        val storage = StorageOptions.newBuilder()
            .setCredentials(credentials)
            .build()
            .service
        bucket = storage.get(GOTHBUZZ_BUCKET_NAME)
        logger.info("Glob initialized OK...")
    }

    fun generateRandomString(length: Int = 16): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    fun logDebug(logger: Logger, message: String) {
        if ("prod" != GOTHBUZZ_ENVIRONMENT_NAME)
            logger.info(message)
    }
}