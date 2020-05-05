package ru.ifmo.kirmanak.manager.storage.entities

import org.opennebula.client.Client
import ru.ifmo.kirmanak.elasticappclient.AppClient
import ru.ifmo.kirmanak.elasticappclient.AppClientFactory
import javax.persistence.*

@Entity
data class OpenNebulaConfigEntity(
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
        val vmgroup: Int,

        @OneToOne(optional = false, mappedBy = "openNebulaConfig")
        val application: ApplicationEntity? = null,

        @Id
        @GeneratedValue
        val id: Long? = null
) : PlatformConfiguration {

    override fun getAppClient(): AppClient {
        val client = Client("$login:$password", address)
        return AppClientFactory.getClient(client, vmgroup, role, template)
    }

}