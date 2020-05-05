package ru.ifmo.kirmanak.manager.storage.entities

import ru.ifmo.kirmanak.elasticappclient.AppClient

/**
 * Represents configuration of connection to an application hosted in a virtualized infrastructure
 */
interface AppConfiguration {
    /**
     * Creates elastic application client out of the configuration
     */
    fun getAppClient(): AppClient
}