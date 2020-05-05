package ru.ifmo.kirmanak.manager.controllers

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.bind.annotation.*
import ru.ifmo.kirmanak.elasticappclient.AppClientException
import ru.ifmo.kirmanak.elasticappclient.AppInstance
import ru.ifmo.kirmanak.manager.models.exceptions.InvalidPlatformException
import ru.ifmo.kirmanak.manager.models.exceptions.NoPlatformConnectionException
import ru.ifmo.kirmanak.manager.models.exceptions.NoSuchConfigurationException
import ru.ifmo.kirmanak.manager.models.requests.OpenNebulaRequest
import ru.ifmo.kirmanak.manager.models.responses.AppIdResponse
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
    ): AppIdResponse {
        logger.info("createKubernetes(namespace: \"$namespace\", deployment: \"$deployment\", yaml: \"$yaml\")")

        val config = KubernetesConfigEntity(deployment, namespace, yaml)
        checkPlatform(config)
        val id = putConfiguration(config)

        return result("createKubernetes", AppIdResponse(id))
    }

    @PostMapping("/api/v1/opennebula")
    fun createOpenNebula(
            @RequestBody request: OpenNebulaRequest
    ): AppIdResponse {
        logger.info("createOpenNebula(request = $request)")

        val config = OpenNebulaConfigEntity(request.address, request.login, request.password, request.role, request.template, request.vmgroup)
        checkPlatform(config)
        val id = putConfiguration(config)

        return result("createOpenNebula", AppIdResponse(id))
    }

    @GetMapping("/api/v1/app/{id}")
    fun getPlatformInfo(@PathVariable("id") id: Long): Array<GetPlatformResponse> {
        logger.info("getPlatformInfo(id: \"$id\")")

        val app = appRepository.findByIdOrNull(id) ?: throw NoSuchConfigurationException(id)
        val client = app.getAppClient()

        val result = client.getAppInstances().map {
            try {
                GetPlatformResponse(it.getCPULoad(), it.getRAMLoad(), it.getName())
            } catch (e: AppClientException) {
                logger.error("getPlatformInfo: unable to get instance info", e)
                throw e
            }
        }.toTypedArray()

        return result("getPlatformInfo", result)
    }

    @DeleteMapping("/api/v1/app/{id}")
    fun removePlatform(@PathVariable("id") id: Long): AppIdResponse {
        logger.info("removePlatform(id: \"$id\")")

        if (appRepository.existsById(id))
            appRepository.deleteById(id)
        else
            throw NoSuchConfigurationException(id)

        return result("removePlatform", AppIdResponse(id))
    }

    @PutMapping("/api/v1/kubernetes/{namespace}/{deployment}/{id}")
    fun updateKubernetes(
            @RequestBody yaml: String,
            @PathVariable("namespace") namespace: String,
            @PathVariable("deployment") deployment: String,
            @PathVariable("id") id: Long
    ) {
        logger.info("updateKubernetes(namespace: \"$namespace\", deployment: \"$deployment\", id = $id, yaml: \"$yaml\")")

        val app = appRepository.findByIdOrNull(id) ?: throw NoSuchConfigurationException(id)
        val currentConfig = app.kubernetesConfig ?: throw NoSuchConfigurationException(id)
        val updated = KubernetesConfigEntity(deployment, namespace, yaml, app, currentConfig.id)

        checkPlatform(updated)
        putConfiguration(updated)
    }

    @PutMapping("/api/v1/opennebula/{id}")
    fun updateOpenNebula(
            @RequestBody request: OpenNebulaRequest,
            @PathVariable("id") id: Long
    ) {
        logger.info("updateOpenNebula(request = $request, id = $id)")

        val app = appRepository.findByIdOrNull(id) ?: throw NoSuchConfigurationException(id)
        val currentConfig = app.openNebulaConfig ?: throw NoSuchConfigurationException(id)
        val config = OpenNebulaConfigEntity(
                request.address, request.login, request.password, request.role,
                request.template, request.vmgroup, app, currentConfig.id
        )

        checkPlatform(config)
        putConfiguration(config)
    }

    private fun checkPlatform(config: PlatformConfiguration) {
        logger.info("checkPlatform(config = $config)")

        val instances: Array<AppInstance>
        try {
            instances = config.getAppClient().getAppInstances()
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

    private fun <Result> result(method: String, result: Result): Result {
        logger.info("$method result: $result")
        return result
    }
}