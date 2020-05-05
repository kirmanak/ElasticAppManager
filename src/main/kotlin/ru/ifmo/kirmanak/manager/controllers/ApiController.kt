package ru.ifmo.kirmanak.manager.controllers

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import ru.ifmo.kirmanak.elasticappclient.AppClient
import ru.ifmo.kirmanak.elasticappclient.AppClientException
import ru.ifmo.kirmanak.elasticappclient.AppInstance
import ru.ifmo.kirmanak.manager.configuration.KubernetesConfiguration
import ru.ifmo.kirmanak.manager.configuration.PlatformConfiguration
import ru.ifmo.kirmanak.manager.models.exceptions.InvalidPlatformException
import ru.ifmo.kirmanak.manager.models.exceptions.NoPlatformConnectionException
import ru.ifmo.kirmanak.manager.models.exceptions.NoSuchConfigurationException
import ru.ifmo.kirmanak.manager.models.responses.CreateKubernetesResponse
import ru.ifmo.kirmanak.manager.models.responses.GetPlatformResponse
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap

@RestController
class ApiController {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val configurations: MutableMap<Int, AppClient> = Collections.synchronizedMap(HashMap())
    private val mNextKey = AtomicInteger()

    @PostMapping("/api/v1/kubernetes/{namespace}/{deployment}")
    fun createKubernetes(
            @RequestBody yaml: String,
            @PathVariable("namespace") namespace: String,
            @PathVariable("deployment") deployment: String
    ): CreateKubernetesResponse {
        logger.info("createKubernetes(namespace: \"$namespace\", deployment: \"$deployment\", yaml: \"$yaml\")")

        val config: PlatformConfiguration = KubernetesConfiguration(yaml, namespace, deployment)
        val client = checkPlatform(config)
        val id = putConfiguration(client)
        val result = CreateKubernetesResponse(id)

        logger.info("createKubernetes result: $result")
        return result
    }

    private fun putConfiguration(client: AppClient): Int {
        val key = mNextKey.getAndIncrement()
        configurations[key] = client
        return key
    }

    @GetMapping("/api/v1/app/{id}")
    fun getPlatformInfo(@PathVariable("id") id: Int): Array<GetPlatformResponse> {
        logger.info("getPlatformInfo(id: \"$id\")")
        val client = configurations[id] ?: throw NoSuchConfigurationException(id)
        return client.getAppInstances().map {
            try {
                GetPlatformResponse(it.getCPULoad(), it.getRAMLoad(), it.getName())
            } catch (e: AppClientException) {
                logger.error("getPlatformInfo: unable to get instance info", e)
                throw e
            }
        }.toTypedArray()
    }

    private fun checkPlatform(config: PlatformConfiguration): AppClient {
        logger.info("checkPlatform(config = $config)")

        val instances: Array<AppInstance>
        val appClient: AppClient
        try {
            appClient = config.getAppClient()
            instances = appClient.getAppInstances()
        } catch (e: AppClientException) {
            logger.error("checkPlatform: no connection", e)
            throw NoPlatformConnectionException(e)
        }

        logger.info("checkPlatform: instances count = ${instances.size}")
        for (instance in instances) {
            try {
                logger.info("checkPlatform: instance(name = \"${instance.getName()}\", CPU = ${instance.getCPULoad()}), RAM = ${instance.getRAMLoad()}")
            } catch (e: AppClientException) {
                logger.error("checkPlatform: unable to get instance info", e)
                throw InvalidPlatformException(e)
            }
        }

        return appClient
    }
}