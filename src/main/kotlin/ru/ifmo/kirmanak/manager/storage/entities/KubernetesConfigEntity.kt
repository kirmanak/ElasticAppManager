package ru.ifmo.kirmanak.manager.storage.entities

import io.kubernetes.client.util.ClientBuilder
import io.kubernetes.client.util.KubeConfig
import ru.ifmo.kirmanak.elasticappclient.AppClient
import ru.ifmo.kirmanak.elasticappclient.AppClientFactory
import java.io.StringReader
import javax.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["deployment", "namespace", "yaml"])])
data class KubernetesConfigEntity(
        @Column(nullable = false)
        val deployment: String,

        @Column(nullable = false)
        val namespace: String,

        @Column(nullable = false, length = 4096)
        val yaml: String,

        @OneToOne(optional = false, mappedBy = "kubernetesConfig")
        val application: ApplicationEntity? = null,

        @Id
        @GeneratedValue
        val id: Long? = null
) : AppConfiguration {

    override fun getAppClient(): AppClient {
        val configReader = StringReader(yaml)
        val client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(configReader)).build()
        return AppClientFactory.getClient(client, namespace, deployment)
    }

    override fun toString(): String {
        return "KubernetesConfigEntity(deployment='$deployment', namespace='$namespace', yaml='$yaml', application=${application?.id}, id=$id)"
    }
}