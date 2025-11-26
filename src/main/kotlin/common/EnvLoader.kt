package com.apptime.code.common

import java.io.File

object EnvLoader {
    private val envMap = mutableMapOf<String, String>()
    private var loaded = false

    /**
     * Load environment variables from .env file
     * Looks for .env file in the project root directory
     */
    fun loadEnvFile() {
        if (loaded) return

        // Try multiple possible locations for .env file
        val possiblePaths = listOf(
            File(".env"),  // Current directory
            File("../.env"),  // Parent directory
            File("../../.env"),  // Two levels up
            File(System.getProperty("user.dir"), ".env")  // User's working directory
        )

        var envFile: File? = null
        for (path in possiblePaths) {
            if (path.exists() && path.isFile) {
                envFile = path
                break
            }
        }

        if (envFile == null) {
            println("⚠️  .env file not found. Using system environment variables only.")
            loaded = true
            return
        }

        println("📄 Loading environment variables from: ${envFile.absolutePath}")

        try {
            envFile.readLines().forEach { line ->
                // Skip comments and empty lines
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return@forEach
                }

                // Parse key=value
                val equalsIndex = trimmed.indexOf('=')
                if (equalsIndex > 0) {
                    val key = trimmed.substring(0, equalsIndex).trim()
                    var value = trimmed.substring(equalsIndex + 1).trim()

                    // Remove surrounding quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))
                    ) {
                        value = value.substring(1, value.length - 1)
                    }

                    // Store in map
                    envMap[key] = value
                    println("  ✓ Loaded: $key")
                }
            }
            println("✅ Loaded ${envMap.size} environment variables from .env file")
            loaded = true
        } catch (e: Exception) {
            println("❌ Error loading .env file: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Get environment variable value
     * First checks the loaded .env map, then falls back to System.getenv()
     */
    fun getEnv(key: String): String? {
        if (!loaded) {
            loadEnvFile()
        }
        return envMap[key] ?: System.getenv(key)
    }

    /**
     * Get environment variable value with default
     */
    fun getEnv(key: String, default: String): String {
        return getEnv(key) ?: default
    }
}

