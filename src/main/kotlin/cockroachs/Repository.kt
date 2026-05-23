package com.apptime.code.cockroachs

import com.apptime.code.common.dbTransaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

class CockroachRepository {

    fun isEmailRegistered(email: String): Boolean {
        return dbTransaction {
            Cockroachs.select { Cockroachs.email eq email }.count() > 0
        }
    }

    fun isPhoneRegistered(phone: String): Boolean {
        return dbTransaction {
            Cockroachs.select { Cockroachs.phone eq phone }.count() > 0
        }
    }

    fun saveCockroach(request: CockroachRequest): CockroachResponse {
        return dbTransaction {
            val insertedId = Cockroachs.insert {
                it[city] = request.city
                it[email] = request.email
                it[exactLat] = request.exact_lat
                it[exactLng] = request.exact_lng
                it[handle] = request.handle
                it[joinedAt] = request.joinedAt
                it[name] = request.name
                it[phone] = request.phone
                it[pincode] = request.pincode
            } get Cockroachs.id

            CockroachResponse(
                id = insertedId,
                city = request.city,
                email = request.email,
                exact_lat = request.exact_lat,
                exact_lng = request.exact_lng,
                handle = request.handle,
                joinedAt = request.joinedAt,
                name = request.name,
                phone = request.phone,
                pincode = request.pincode
            )
        }
    }
}
