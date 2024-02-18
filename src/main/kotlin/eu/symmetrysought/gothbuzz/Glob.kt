package eu.symmetrysought.gothbuzz

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Glob {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    var bucketSaKey: String = ""
        get() {
            if ("" == field) {
                val data = System.getenv("GOTHBUZZ_BUCKET_SA_KEY")
                if (null == data) {
                    logger.warn(""""GOTHBUZZ_BUCKET_SA_KEY" not set!""")
                }
                field = data
            }
            return field
        }

    var environment: String = ""
        get() {
            if ("" == field) {
                val data = System.getenv("GOTHBUZZ_ENVIRONMENT_NAME")
                if (null == data) {
                    logger.warn(""""GOTHBUZZ_ENVIRONMENT_NAME" not set!""")
                }
                field = data
            }
            return field
        }
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