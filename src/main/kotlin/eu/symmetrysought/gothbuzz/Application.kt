package eu.symmetrysought.gothbuzz

import com.google.gson.JsonObject
import com.google.gson.Gson
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.Micronaut.run
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName


@Singleton
class CheckEnvironmentAndConfiguration : ApplicationEventListener<StartupEvent> {
	private val logger: Logger = LoggerFactory.getLogger(javaClass)
	private val secretManagerServiceClient: SecretManagerServiceClient by lazy {
		SecretManagerServiceClient.create()
	}
	override fun onApplicationEvent(event: StartupEvent) {
		logger.info("Checking if environment is OK...")
		val implementedEnvironmentVariables = listOf("GOTHBUZZ_ENVIRONMENT_NAME", "GOTHBUZZ_BUCKET_NAME", "GOTHBUZZ_BUCKET_SA_KEY")
		val quitValues = implementedEnvironmentVariables.associateWith { 0 }.toMutableMap()
		val okValues = mapOf("GOTHBUZZ_ENVIRONMENT_NAME" to listOf("local", "prod"))
		val environmentVariableNameToJsonParser: Map<String, (String) -> Result<JsonObject>> =
			mapOf("GOTHBUZZ_BUCKET_SA_KEY" to { environmentVariableData: String -> checkIfEnvironmentVariableDataIsJson(environmentVariableData)})

		implementedEnvironmentVariables.forEach { envVarName ->
			getFromEnvironmentVariableOrGoogleSecretData(envVarName).let { envVarValue ->
				when (envVarValue) {
					null -> {
						logger.warn("$envVarName was not set!")
						quitValues[envVarName] = 2
					}
					"" -> {
						quitValues[envVarName] = 1
						logger.warn("$envVarName was found to be empty!")
					}
				}

				if (0 != quitValues[envVarName]) return@forEach

				okValues[envVarName]?.let {
					if (!it.any { it == envVarValue }) {
						logger.warn("$envVarName had a non-OK value!")
						quitValues[envVarName] = 3
					}
				}

				environmentVariableNameToJsonParser[envVarName]?.let { parser -> parser.invoke(envVarValue!!).run {
					if (this.isFailure) {
						logger.warn("Failed making a JSON object of $envVarName!")
						quitValues[envVarName] = 4
						return@run
					}

					when (envVarName) {
						"GOTHBUZZ_BUCKET_SA_KEY" -> {
							serializeJsonObjectToServiceAccountPrivateKey(this.getOrThrow()).exceptionOrNull()?.let {
								quitValues[envVarName] = 5
								logger.warn("$envVarName: ${it.message}")
							}
						}
						else -> logger.warn("Serializer unset for $envVarName")
					}

				}}
			}
		}

		quitValues.values.filter { 0 != it }.map { throw Exception("Error in environment configuration!") }
		logger.info("Environment was found to be OK!")
	}

	private fun getFromEnvironmentVariableOrGoogleSecretData(name: String): String? {
		return try {
			System.getenv(name)!!
		}
		catch (_: Exception) {
			val secretVersionName =
				SecretVersionName.of(Glob.projectId, name, "latest") ?:
					return null
				val accessResponse = secretManagerServiceClient.accessSecretVersion(secretVersionName)
			accessResponse.payload.data.toStringUtf8()
		}

	}

	companion object {
		private fun checkIfEnvironmentVariableDataIsJson(
			data: String
		): Result<JsonObject> = data.run {
				try {
					val jsonObject = Gson().fromJson(this, JsonObject::class.java)
					Result.success(jsonObject)
				} catch (e: Exception) {
					Result.failure(e)
				}
			}



		private fun serializeJsonObjectToServiceAccountPrivateKey(jsonObject: JsonObject): Result<Boolean> {
			return try {
				val keys = setOf(
					"type",
					"project_id",
					"private_key_id",
					"private_key",
					"client_email",
					"client_id",
					"auth_uri",
					"token_uri",
					"auth_provider_x509_cert_url",
					"client_x509_cert_url",
					"universe_domain"
				)
				if (jsonObject.keySet().containsAll(keys))
					Result.success(true)
				else
					Result.failure(Exception("jsonObject was not a service account private key!"))
			} catch (e:Exception) {
				Result.failure(e)
			}
		}
	}
}

fun main(args: Array<String>) {
	run(*args)
}

