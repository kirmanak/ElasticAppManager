package ru.ifmo.kirmanak.manager.controllers

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.bind.annotation.*
import ru.ifmo.kirmanak.elasticappclient.AppClient
import ru.ifmo.kirmanak.elasticappclient.AppClientException
import ru.ifmo.kirmanak.elasticappclient.AppInstance
import ru.ifmo.kirmanak.manager.models.exceptions.InvalidAppConfigException
import ru.ifmo.kirmanak.manager.models.exceptions.NoAppConnectionException
import ru.ifmo.kirmanak.manager.models.exceptions.NoSuchApplicationException
import ru.ifmo.kirmanak.manager.models.requests.OpenNebulaRequest
import ru.ifmo.kirmanak.manager.models.requests.ScaleRequest
import ru.ifmo.kirmanak.manager.models.responses.AppIdResponse
import ru.ifmo.kirmanak.manager.models.responses.AppInstanceResponse
import ru.ifmo.kirmanak.manager.storage.entities.AppConfiguration
import ru.ifmo.kirmanak.manager.storage.entities.ApplicationEntity
import ru.ifmo.kirmanak.manager.storage.entities.KubernetesConfigEntity
import ru.ifmo.kirmanak.manager.storage.entities.OpenNebulaConfigEntity

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
        checkAppConfig(config)
        val id = putConfiguration(config)

        return result("createKubernetes", AppIdResponse(id))
    }

    @PostMapping("/api/v1/opennebula")
    fun createOpenNebula(
            @RequestBody request: OpenNebulaRequest
    ): AppIdResponse {
        logger.info("createOpenNebula(request = $request)")

        val config = OpenNebulaConfigEntity(request.address, request.login, request.password, request.role, request.template, request.vmgroup)
        checkAppConfig(config)
        val id = putConfiguration(config)

        return result("createOpenNebula", AppIdResponse(id))
    }

    @GetMapping("/api/v1/app/{id}")
    fun getApplicationInfo(@PathVariable("id") id: Long): Array<AppInstanceResponse> {
        logger.info("getApplicationInfo(id: \"$id\")")

        val client = getApp(id).getAppClient()

        val result = getAppInfo(client)

        return result("getApplicationInfo", result)
    }

    @DeleteMapping("/api/v1/app/{id}")
    fun removeApplication(@PathVariable("id") id: Long): AppIdResponse {
        logger.info("removeApplication(id: \"$id\")")

        if (appRepository.existsById(id))
            appRepository.deleteById(id)
        else
            throw NoSuchApplicationException(id)

        return result("removeApplication", AppIdResponse(id))
    }

    @PutMapping("/api/v1/kubernetes/{namespace}/{deployment}/{id}")
    fun updateKubernetes(
            @RequestBody yaml: String,
            @PathVariable("namespace") namespace: String,
            @PathVariable("deployment") deployment: String,
            @PathVariable("id") id: Long
    ) {
        logger.info("updateKubernetes(namespace: \"$namespace\", deployment: \"$deployment\", id = $id, yaml: \"$yaml\")")

        val app = getApp(id)
        val currentConfig = app.kubernetesConfig ?: throw NoSuchApplicationException(id)
        val updated = KubernetesConfigEntity(deployment, namespace, yaml, app, currentConfig.id)

        checkAppConfig(updated)
        putConfiguration(updated)
    }

    @PutMapping("/api/v1/opennebula/{id}")
    fun updateOpenNebula(
            @RequestBody request: OpenNebulaRequest,
            @PathVariable("id") id: Long
    ) {
        logger.info("updateOpenNebula(request = $request, id = $id)")

        val app = getApp(id)
        val currentConfig = app.openNebulaConfig ?: throw NoSuchApplicationException(id)
        val config = OpenNebulaConfigEntity(
                request.address, request.login, request.password, request.role,
                request.template, request.vmgroup, app, currentConfig.id
        )

        checkAppConfig(config)
        putConfiguration(config)
    }

    @PatchMapping("/api/v1/app/{id}")
    fun scaleApplication(@PathVariable("id") id: Long, @RequestBody request: ScaleRequest): Array<AppInstanceResponse> {
        logger.info("scaleApplication(id = $id, request = $request)")

        val client = getApp(id).getAppClient()

        client.scaleInstances(request.incrementBy)

        val result = getAppInfo(client)

        return result("scaleApplication", result)
    }

    private fun checkAppConfig(config: AppConfiguration) {
        logger.info("checkAppConfig(config = $config)")

        val instances: Array<AppInstance>
        try {
            instances = config.getAppClient().getAppInstances()
        } catch (e: AppClientException) {
            logger.error("checkAppConfig: no connection", e)
            throw NoAppConnectionException(e)
        }

        logger.info("checkAppConfig: instances count = ${instances.size}")
        for (instance in instances) {
            try {
                logger.info("checkAppConfig: instance(name = \"${instance.getName()}\", CPU = ${instance.getCPULoad()}), RAM = ${instance.getRAMLoad()}")
            } catch (e: AppClientException) {
                logger.error("checkAppConfig: unable to get instance info", e)
                throw InvalidAppConfigException(e)
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
        logger.info("$method result: ${result.toString()}")
        return result
    }

    private fun getApp(id: Long) = appRepository.findByIdOrNull(id) ?: throw NoSuchApplicationException(id)

    private fun getAppInfo(client: AppClient) = client.getAppInstances().map {
        try {
            AppInstanceResponse(it.getCPULoad(), it.getRAMLoad(), it.getName())
        } catch (e: AppClientException) {
            logger.error("getAppInfo: unable to get instance info", e)
            throw e
        }
    }.toTypedArray()
}