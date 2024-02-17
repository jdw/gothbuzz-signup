package eu.symmetrysought.gothbuzz

import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.Micronaut.run
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Singleton
class CheckEnvironmentAndConfiguration : ApplicationEventListener<StartupEvent> {
	val logger: Logger = LoggerFactory.getLogger(javaClass)
	override fun onApplicationEvent(event: StartupEvent) {
		logger.info("Checking if environment is OK...")
		val implementedEnvVars = listOf("GOTHBUZZ_ENVIRONMENT_NAME", "GOTHBUZZ_BUCKET_NAME")
		val quitValues = implementedEnvVars.associateWith { 0 }.toMutableMap()
		val okValues = mapOf("GOTHBUZZ_ENVIRONMENT_NAME" to listOf("local", "prod"))
		implementedEnvVars.forEach { envVarName ->
			System.getenv(envVarName).let { envVarValue ->
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
			}
		}

		quitValues.values.filter { 0 != it }.map { throw Exception("Error in environment configuration!") }
		logger.info("Environment was found to be OK!")
	}
}

fun main(args: Array<String>) {
	run(*args)
}

