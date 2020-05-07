package ru.ifmo.kirmanak.manager.storage.entities

import ru.ifmo.kirmanak.elasticappclient.AppClient
import ru.ifmo.kirmanak.manager.storage.constraints.OnlyOneSetConstraint
import javax.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["open_nebula_config", "kubernetes_config"])])
@OnlyOneSetConstraint(fields = ["openNebulaConfig", "kubernetesConfig"])
data class ApplicationEntity(
        @OneToOne(optional = true, orphanRemoval = true)
        @JoinColumn(name = "kubernetes_config")
        val kubernetesConfig: KubernetesConfigEntity? = null,

        @OneToOne(optional = true, orphanRemoval = true)
        @JoinColumn(name = "open_nebula_config")
        val openNebulaConfig: OpenNebulaConfigEntity? = null,

        @Id
        @GeneratedValue
        val id: Long? = null
) : AppConfiguration {
    override fun getAppClient(): AppClient {
        val configuration = arrayOf(kubernetesConfig, openNebulaConfig).first { it != null }
                ?: throw IllegalStateException("Exactly one configuration must not be null")

        return configuration.getAppClient()
    }
}