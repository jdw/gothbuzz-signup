package eu.symmetrysought.gothbuzz

import io.micronaut.core.annotation.Introspected

enum class Status {
    VERIFIED, UNVERIFIED
}
data class Signup(val email: String, val status: Status, val code: String)

enum class ReturnCode {
    BAD_EMAIL, ALL_OK, NOT_NEW_EMAIL, MAILSEND_ERROR
}
@Introspected
data class ReturnMessage(val returnMessage: String, val code: ReturnCode)
