package eu.symmetrysought.gothbuzz

import kotlin.reflect.full.memberProperties

class EnvironmentVariablesHandler private constructor(private val environmentVariablesValue: Map<String, String>) {
    val GOTHBUZZ_PROPAGATOR: String by environmentVariablesValue
    val GOTHBUZZ_ENVIRONMENT_NAME: String by environmentVariablesValue
    val GOTHBUZZ_SENDGRID_API_KEY: String by environmentVariablesValue
    val GOTHBUZZ_BUCKET_NAME: String by environmentVariablesValue
    val GOTHBUZZ_BUCKET_SA_KEY: String by environmentVariablesValue
    val GOTHBUZZ_NO_REPLY: String by environmentVariablesValue
    val GOTHBUZZ_VERIFICATION_CODE_LENGTH: String by environmentVariablesValue
    val GOTHBUZZ_GOOGLE_LOCATION_ID: String by environmentVariablesValue
    val GOTHBUZZ_PROJECT_ID: String by environmentVariablesValue
    val GOTHBUZZ_WORKFLOW_EXEC: String by environmentVariablesValue

    enum class Errors() {
        NOT_SET, EMPTY, PARSER_FAILED, NOT_CONFIGURED, NOT_INITIALIZED // Loads of NOT_INITIALIZED = Forgot to run .addImplementedEnvironmentVariables()
    }
    data class Builder(private val envvars: MutableMap<String, String> = mutableMapOf()) {
        private var environmentVariablesErrors: MutableMap<String, MutableList<Errors>> = mutableMapOf()
        private val environmentVariableParsers: Map<String, (String) -> Boolean> = mapOf(
            "GOTHBUZZ_ENVIRONMENT_NAME" to { value -> "prod" == value || "local" == value },
            "GOTHBUZZ_SENDGRID_API_KEY" to { value -> value.startsWith("SG.") && 69 == value.length },
            "GOTHBUZZ_NO_REPLY" to { value -> EmailHandler.isValidEmail(value) },
            "GOTHBUZZ_VERIFICATION_CODE_LENGTH" to { value -> try { value.toInt(); true} catch(_: Exception) { false } }
        )


        private var implementedEnvironmentVariables: List<String>? = null
            get() {
                if (null == field) {
                    field = EnvironmentVariablesHandler::class.memberProperties.filter { it.name.startsWith("GOTHBUZZ_") }.map{ it.name }.toList()
                }

                return field
            }


        fun addImplementedEnvironmentVariables(): Builder {
            implementedEnvironmentVariables?.forEach { name ->
                val value = System.getenv(name)
                if (null == value) {
                    addEnvironmentVariableError(name, Errors.NOT_SET)
                    return@forEach
                }

                setEnvironmentVariable(name, value)

                if ("" == value) {
                    addEnvironmentVariableError(name, Errors.EMPTY)
                }
            }

            return this
        }


        fun addEnvironmentVariablesFromEnvironment(): Builder {
            System.getenv().filter { (name, _) ->
                name.startsWith("GOTHBUZZ_")
            }.map { (name, value) ->
                if (!implementedEnvironmentVariables!!.contains(name)) {
                    addEnvironmentVariableError(name, Errors.NOT_CONFIGURED)
                    if ("" == value) {
                        addEnvironmentVariableError(name, Errors.EMPTY)
                    }
                }
            }

            return this
        }


        private fun setEnvironmentVariable(name: String, value: String): Builder {
            implementedEnvironmentVariables?.filter {
                it == name
            }.takeIf { it?.size == 1 } ?: {
                addEnvironmentVariableError(name, Errors.NOT_CONFIGURED)
            }

            envvars[name] = value

            environmentVariableParsers[name]?.let { parser ->
                if (!parser.invoke(value)) {
                    addEnvironmentVariableError(name, Errors.PARSER_FAILED)
                }
            }

            return this
        }


        private fun addEnvironmentVariableError(name: String, error: Errors) {
            if (!environmentVariablesErrors.containsKey(name))
                environmentVariablesErrors[name] = mutableListOf()
            environmentVariablesErrors[name]?.add(error)
        }


        fun checkErrors(): Result<Boolean> {
            implementedEnvironmentVariables?.forEach { name ->
                if (!envvars.containsKey(name))
                    addEnvironmentVariableError(name, Errors.NOT_INITIALIZED)
            }

            environmentVariablesErrors.entries.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }?.let {
                var msg = ""
                it.forEach { (name, errors) -> msg +=  "$name : $errors" }
                return Result.failure(Exception(msg))
            }

            return Result.success(true)
        }


        fun build(): EnvironmentVariablesHandler {
            return EnvironmentVariablesHandler(envvars.toMap())
        }
    }

    companion object {
        fun newBuilder(): Builder = Builder()
    }
}