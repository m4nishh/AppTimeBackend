package com.apptime.code.cockroachs

class CockroachService(private val repository: CockroachRepository) {

    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun saveCockroach(request: CockroachRequest): CockroachResponse {
        // 1. Structural Validations
        require(request.city.isNotBlank()) { "City cannot be blank" }
        require(request.email.isNotBlank()) { "Email cannot be blank" }
        require(emailRegex.matches(request.email)) { "Invalid email format" }
        require(request.exact_lat in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(request.exact_lng in -180.0..180.0) { "Longitude must be between -180 and 180" }
        require(request.joinedAt > 0) { "JoinedAt must be a positive timestamp" }
        require(request.name.isNotBlank()) { "Name cannot be blank" }
        require(request.phone.isNotBlank()) { "Phone cannot be blank" }

        // 2. Uniqueness Validations
        if (repository.isEmailRegistered(request.email)) {
            throw IllegalArgumentException("Email already exists")
        }
        if (repository.isPhoneRegistered(request.phone)) {
            throw IllegalArgumentException("Phone number already exists")
        }

        return repository.saveCockroach(request)
    }
}
