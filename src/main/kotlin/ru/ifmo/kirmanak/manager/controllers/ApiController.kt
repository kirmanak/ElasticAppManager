package ru.ifmo.kirmanak.manager.controllers

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.bind.annotation.*
import ru.ifmo.kirmanak.elasticappclient.AppClient
import ru.ifmo.kirmanak.elasticappclient.AppClientException
import ru.ifmo.kirmanak.elasticappclient.AppInstance
import ru.ifmo.kirmanak.manager.models.exceptions.InvalidPlatformException
import ru.ifmo.kirmanak.manager.models.exceptions.NoPlatformConnectionException
import ru.ifmo.kirmanak.manager.models.exceptions.NoSuchConfigurationException
import ru.ifmo.kirmanak.manager.models.requests.CreateOpenNebulaRequest
import ru.ifmo.kirmanak.manager.models.responses.CreateKubernetesResponse
import ru.ifmo.kirmanak.manager.models.responses.CreateOpenNebulaResponse
import ru.ifmo.kirmanak.manager.models.responses.GetPlatformResponse
import ru.ifmo.kirmanak.manager.storage.entities.ApplicationEntity
import ru.ifmo.kirmanak.manager.storage.entities.KubernetesConfigEntity
import ru.ifmo.kirmanak.manager.storage.entities.OpenNebulaConfigEntity
import ru.ifmo.kirmanak.manager.storage.entities.PlatformConfiguration

@RestController
class ApiController {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var appRepository: ApplicationRepository

    @Autowired
    private lateinit var kubernetesConfigRepo: KubernetesConfigRepository

    @Autowired
    private lateinit var openNebulaConfigRepo: OpenNebulaConfigRepository

    @PostMapping("/api/v1/kubernetes/{namespace}/{deployment}")
    fun createKubernetes(
            @RequestBody yaml: String,
            @PathVariable("namespace") namespace: String,
            @PathVariable("deployment") deployment: String
    ): CreateKubernetesResponse {
        logger.info("createKubernetes(namespace: \"$namespace\", deployment: \"$deployment\", yaml: \"$yaml\")")

        val config = KubernetesConfigEntity(deployment, namespace, yaml)
        val client = checkPlatform(config)
        val id = putConfiguration(config)
        val result = CreateKubernetesResponse(id)

        logger.info("createKubernetes result: $result")
        return result
    }

    @PostMapping("/api/v1/opennebula")
    fun createOpenNebula(
            @RequestBody request: CreateOpenNebulaRequest
    ): CreateOpenNebulaResponse {
        logger.info("createOpenNebula(request = $request)")

        val config = OpenNebulaConfigEntity(request.address, request.login, request.password, request.role, request.template, request.vmgroup)
        val client = checkPlatform(config)
        val id = putConfiguration(config)
        val result = CreateOpenNebulaResponse(id)

        logger.info("createOpenNebula result: $result")
        return result
    }

    @GetMapping("/api/v1/app/{id}")
    fun getPlatformInfo(@PathVariable("id") id: Long): Array<GetPlatformResponse> {
        logger.info("getPlatformInfo(id: \"$id\")")
        val app = appRepository.findByIdOrNull(id) ?: throw NoSuchConfigurationException(id)
        val client = app.getAppClient()
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

    private fun putConfiguration(config: OpenNebulaConfigEntity): Long {
        val saved = openNebulaConfigRepo.save(config)
        val app = ApplicationEntity(openNebulaConfig = saved)
        return putApplication(app)
    }

    private fun putConfiguration(config: KubernetesConfigEntity): Long {
        val saved = kubernetesConfigRepo.save(config)
        val app = ApplicationEntity(kubernetesConfig = saved)
        return putApplication(app)
    }

    private fun putApplication(app: ApplicationEntity): Long {
        return appRepository.save(app).id ?: throw IllegalStateException("Id for a new application was not generated")
    }
}