package ru.ifmo.kirmanak.manager.configuration

import io.kubernetes.client.util.ClientBuilder
import io.kubernetes.client.util.KubeConfig
import ru.ifmo.kirmanak.elasticappclient.AppClient
import ru.ifmo.kirmanak.elasticappclient.AppClientFactory
import java.io.StringReader

data class KubernetesConfiguration(
        private val config: String,
        private val namespace: String,
        private val deployment: String
) : PlatformConfiguration {

    override fun getAppClient(): AppClient {
        val configReader = StringReader(config)
        val client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(configReader)).build()
        return AppClientFactory.getClient(client, namespace, deployment)
    }

}
