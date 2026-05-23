package com.apptime.code.cockroachs

import com.apptime.code.common.EncryptionUtil
import com.apptime.code.common.respondApi
import com.apptime.code.common.respondError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.configureCockroachRoutes() {
    val repository = CockroachRepository()
    val service = CockroachService(repository)
    val jsonDecoder = Json { ignoreUnknownKeys = true }

    routing {
        route("/api/cockroachs") {
            post {
                try {
                    val encryptedRequest = call.receive<EncryptedCockroachRequest>()
                    
                    // Decrypt the payload using global AES configuration
                    val decryptedJson = try {
                        @Suppress("DEPRECATION")
                        EncryptionUtil.decrypt(encryptedRequest.encryptedData)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Failed to decrypt payload: ${e.message}")
                    }

                    // Parse decrypted json string into CockroachRequest object
                    val cockroachRequest = try {
                        jsonDecoder.decodeFromString<CockroachRequest>(decryptedJson)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid payload content after decryption: ${e.message}")
                    }

                    val response = service.saveCockroach(cockroachRequest)
                    call.respondApi(response, "Cockroach saved successfully", HttpStatusCode.Created)
                } catch (e: IllegalArgumentException) {
                    call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                } catch (e: kotlinx.serialization.SerializationException) {
                    call.respondError(HttpStatusCode.BadRequest, "Invalid JSON format: ${e.message}")
                } catch (e: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, "Failed to save cockroach: ${e.message}")
                }
            }
        }
    }
}
