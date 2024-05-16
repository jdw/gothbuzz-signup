package eu.symmetrysought.gothbuzz

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

		try {
			Glob
		}
		catch (e: ExceptionInInitializerError) {
			logger.warn("Exception caught while initializing Glob!")
			NotificationHandler.notifyInitializationError(e)
			throw e
		}
		logger.info("We're in the pipe - Five by five!")
	}
}

fun main(args: Array<String>) {
	run(*args)
}

