package ru.ifmo.kirmanak.manager.configuration

import org.opennebula.client.Client
import ru.ifmo.kirmanak.elasticappclient.AppClient
import ru.ifmo.kirmanak.elasticappclient.AppClientFactory

data class OpenNebulaConfig(
        private val address: String,
        private val login: String,
        private val password: String,
        private val role: Int,
        private val template: Int,
        private val vmgroup: Int
) : PlatformConfiguration {

    override fun getAppClient(): AppClient {
        val client = Client("$login:$password", address)
        return AppClientFactory.getClient(client, vmgroup, role, template)
    }

}
