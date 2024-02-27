package eu.symmetrysought.gothbuzz

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.workflows.executions.v1.*
import com.google.gson.Gson
import com.sun.nio.sctp.NotificationHandler
import io.micronaut.context.annotation.Requires
import io.micronaut.core.execution.ExecutionFlow
import io.micronaut.scheduling.annotation.Async
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ExecutionException

class NotificationHandler private constructor(private val executionSettings: ExecutionsSettings) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)


    enum class NotificationChannel {
        ANNOUNCEMENTS, ERRORS, BUZZ
    }


    fun propagateAnnouncement(message: String) {
        workflowsExecution(Glob.envvar.GOTHBUZZ_PROPAGATOR, mapOf("message" to message, "channel" to NotificationChannel.ANNOUNCEMENTS.name.lowercase()))
    }


    fun propagateBuzz(message: String) {
        workflowsExecution(Glob.envvar.GOTHBUZZ_PROPAGATOR, mapOf("message" to message, "channel" to NotificationChannel.BUZZ.name.lowercase()))
    }


    fun propagateError(message: String) {
        workflowsExecution(Glob.envvar.GOTHBUZZ_PROPAGATOR, mapOf("message" to message, "channel" to NotificationChannel.ERRORS.name.lowercase()))
    }


    fun propagateError(t: Throwable) {
        val message = t.message
        val lineNumber = t.stackTrace.get(0).lineNumber.toString()
        val classname = t.stackTrace.get(0).className.toString()
        val filename = t.stackTrace.get(0).fileName
        val function = t.stackTrace.get(0).methodName

        propagateError("""$classname.$function threw message "$message" in $filename:$lineNumber""")
    }

    @Async(value = "notifications")
    //@Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    private fun workflowsExecution(workflowId: String, fields: Map<String, String>) {
        //https://cloud.google.com/workflows/docs/executing-workflow#client-libraries
        ExecutionsClient.create(executionSettings).use { executionsClient ->
            // Construct the fully qualified location path.
            val parent: WorkflowName = WorkflowName.of(Glob.envvar.GOTHBUZZ_PROJECT_ID, Glob.envvar.GOTHBUZZ_GOOGLE_LOCATION_ID, workflowId)
            val body = Gson().toJson(fields)

            Glob.logDebug(logger, "$workflowId: body=$body", Throwable())
            val request: CreateExecutionRequest = CreateExecutionRequest.newBuilder()
                .setParent(parent.toString())
                .setExecution(Execution
                    .newBuilder()
                    .setArgument(body)
                    .build())
                .build()

            val response: Execution = executionsClient.createExecution(request)
            val executionName: String = response.getName()
            var backoffTime: Long = 0
            var backoffDelay: Long = 1000 // Start wait with delay of 1,000 ms
            val backoffTimeout = (10 * 60 * 1000).toLong() // Time out at 10 minutes

            // Wait for execution to finish, then print results.
            var finished = false
            while (!finished && backoffTime < backoffTimeout) {
                val execution: Execution = executionsClient.getExecution(executionName)
                finished = execution.getState() !== Execution.State.ACTIVE

                // If we haven't seen the results yet, wait.
                if (!finished) {
                    Thread.sleep(backoffDelay)
                    backoffTime += backoffDelay
                    backoffDelay *= 2 // Double the delay to provide exponential backoff.
                } else {
                    val stateName = execution.getState().name
                    val executionResult = execution.result

                    if ("SUCCEEDED" != stateName) {
                        // TODO send to prod-errors webhook
                        val executionId = executionName.split("/").last()
                        val errorMessage = """{"content": "Workflow '$workflowId' failed! See https://console.cloud.google.com/workflows/workflow/europe-west1/dev-propagator/execution/$executionId for more!"}"""

                        webhookHttpPost(errorMessage)
                        logger.warn("Execution finished with state: $stateName")
                        logger.warn("Execution results: $executionResult")
                        logger.warn(errorMessage)
                    }
                }
            }
        }
    }


    companion object {
        fun newBuilder(): Builder {
            return Builder()
        }


        @Async(value = "notifications")
        // THis function should not take envvars from Glob
        fun notifyInitializationError(e: ExceptionInInitializerError) {
            val logger: Logger = LoggerFactory.getLogger(NotificationHandler::class.java)
            val env = System.getenv("GOTHBUZZ_ENVIRONMENT_NAME")

            if ("local" == env) return

            val message = e.message
            val lineNumber = e.stackTrace.get(0).lineNumber.toString()
            val classname = e.stackTrace.get(0).className.toString()
            val filename = e.stackTrace.get(0).fileName
            val function = e.stackTrace.get(0).methodName
            val content = """$classname.$function threw message "$message" in $filename:$lineNumber"""

            webhookHttpPost("""{"content": "${content.replace("\"", "'")}"}""")
        }

        @Async(value = "notifications")
        private fun webhookHttpPost(body: String) {
            val prodErrorsWebhook = System.getenv("ERRORS_WEBHOOK")

            if (null == prodErrorsWebhook) {
                println("Could not initialize properly! ERRORS_WEBHOOK not set! Quiting!")
                return
            }

            val request = HttpRequest.newBuilder()
                .uri(URI.create(prodErrorsWebhook))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            val client = HttpClient.newHttpClient()
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
    }

    class Builder {
        private var executionSettings: ExecutionsSettings? = null
        fun build() = NotificationHandler(executionSettings!!)

        fun addCredentials(serviceAccountKey: String): Builder {
            val credentials = GoogleCredentials.fromStream(serviceAccountKey.byteInputStream())
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

            executionSettings = ExecutionsSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build()

            return this
        }
    }
}