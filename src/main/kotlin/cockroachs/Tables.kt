package com.apptime.code.cockroachs

import org.jetbrains.exposed.sql.Table

object Cockroachs : Table("cockroachs") {
    val id = long("id").autoIncrement()
    val city = varchar("city", 255)
    val email = varchar("email", 255).uniqueIndex() // enforces database-level uniqueness
    val exactLat = double("exact_lat")
    val exactLng = double("exact_lng")
    val handle = varchar("handle", 255).nullable() // nullable (can be blank/omitted)
    val joinedAt = long("joined_at")
    val name = varchar("name", 255)
    val phone = varchar("phone", 50).uniqueIndex() // enforces database-level uniqueness
    val pincode = varchar("pincode", 50).nullable() // nullable (can be blank/omitted)

    override val primaryKey = PrimaryKey(id)
}
