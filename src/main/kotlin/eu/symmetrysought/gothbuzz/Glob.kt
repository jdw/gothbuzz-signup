package eu.symmetrysought.gothbuzz

import java.io.BufferedReader
import java.io.InputStreamReader

object Glob {
    var environment: String = ""
    var projectId: String = ""
        get() {
            if ("" == field) {
                val process = Runtime.getRuntime().exec("gcloud config get-value project")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                field = reader.readLine().trim()
                if ("" == field) throw Exception("Could not get project id!")
            }

            return field
        }
}