package eu.symmetrysought.gothbuzz

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Glob {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    var bucketSaKey: String = ""
        get() {
            if ("" == field) {
                val secretId = "GOTHBUZZ_BUCKET_SA_KEY"
                SecretManagerServiceClient.create().use { client ->
                    SecretVersionName.of(projectId, secretId, "latest").let { secretVersionName ->
                        val accessResponse = client.accessSecretVersion(secretVersionName)
                        field = accessResponse.payload.data.toStringUtf8()
                    }

                    if ("" == field) field = System.getenv(secretId)!!
                }
            }
            return field
        }
    var environment: String = ""
    var projectId: String = ""
        get() {
            if ("" == field) {
                val data = System.getenv("GOTHBUZZ_PROJECT_ID")
                if (null == data) {
                    logger.warn(""""GOTHBUZZ_PROJECT_ID" not set!""")
                }
                field = data
            }
            return field
        }
}