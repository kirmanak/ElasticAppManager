package ru.ifmo.kirmanak.manager.storage.entities

import ru.ifmo.kirmanak.elasticappclient.AppClient

/**
 * Represents configuration of connection to a virtualized infrastructure platfrom
 */
interface PlatformConfiguration {
    /**
     * Creates elastic application client out of the configuration
     */
    fun getAppClient(): AppClient
}