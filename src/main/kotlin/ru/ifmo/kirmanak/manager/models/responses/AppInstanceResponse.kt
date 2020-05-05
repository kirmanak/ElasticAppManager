package ru.ifmo.kirmanak.manager.models.responses

import ru.ifmo.kirmanak.elasticappclient.AppInstance

data class AppInstanceResponse(
        private val CPULoad: Double,
        private val RAMLoad: Double,
        private val name: String
) : AppInstance {

    override fun getCPULoad(): Double = CPULoad

    override fun getName(): String = name

    override fun getRAMLoad(): Double = RAMLoad
}
