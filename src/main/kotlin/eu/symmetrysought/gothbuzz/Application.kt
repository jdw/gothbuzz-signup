package eu.symmetrysought.gothbuzz

import com.google.gson.JsonObject
import com.google.gson.Gson
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.Micronaut.run
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory


@Singleton
class CheckEnvironmentAndConfiguration : ApplicationEventListener<StartupEvent> {
	private val logger: Logger = LoggerFactory.getLogger(javaClass)

	override fun onApplicationEvent(event: StartupEvent) {
		logger.info("Checking if environment is OK...")

		Glob
		logger.info("Environment was found to be OK!")
	}
}

fun main(args: Array<String>) {
	run(*args)
}

