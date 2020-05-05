package ru.ifmo.kirmanak.manager.storage

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class OpenNebulaConfigEntity(
        @Id
        @GeneratedValue
        var id: Long,

        @Column(nullable = false)
        val address: String,

        @Column(nullable = false)
        val login: String,

        @Column(nullable = false)
        val password: String,

        @Column(nullable = false)
        val role: Int,

        @Column(nullable = false)
        val template: Int,

        @Column(nullable = false)
        val vmgroup: Int
)