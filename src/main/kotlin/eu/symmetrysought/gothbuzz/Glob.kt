package eu.symmetrysought.gothbuzz


import com.google.auth.oauth2.AccessToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import java.io.FileInputStream
import java.io.InputStream
import kotlin.random.Random

object Glob {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val storage: Storage
    val sendGridVerifyEmailTemplateId: String
    val environmentName: String
    val bucket: Bucket
    val sendGridApiKey: String
    val projectId: String
    val environment: String

    init {
        logger.info("Initializing the almighty Glob...")
        // Load Google Cloud Storage credentials
        val bucketSaKey = System.getenv("GOTHBUZZ_BUCKET_SA_KEY")!!
        val credentials = GoogleCredentials.fromStream(bucketSaKey.byteInputStream())
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

        // Create Storage client
        storage = StorageOptions.newBuilder()
            .setCredentials(credentials)
            .build()
            .service

        sendGridApiKey = System.getenv("GOTHBUZZ_SENDGRID_API_KEY")!!
        sendGridVerifyEmailTemplateId = System.getenv("GOTHBUZZ_SENDGRID_VERIFY_EMAIL_TEMPLATE_ID")!!
        environmentName = System.getenv("GOTHBUZZ_ENVIRONMENT_NAME")
        bucket = storage.get(System.getenv("GOTHBUZZ_BUCKET_NAME")!!)
        projectId = System.getenv("GOTHBUZZ_PROJECT_ID")!!
        environment = System.getenv("GOTHBUZZ_ENVIRONMENT_NAME")

        logger.info("Glob initialized OK...")
    }

    fun generateRandomString(length: Int = 16): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}