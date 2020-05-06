package ru.ifmo.kirmanak.manager.controllers

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.bind.annotation.*
import ru.ifmo.kirmanak.elasticappclient.AppClient
import ru.ifmo.kirmanak.elasticappclient.AppClientException
import ru.ifmo.kirmanak.elasticappclient.AppInstance
import ru.ifmo.kirmanak.manager.models.exceptions.ExistingApplicationException
import ru.ifmo.kirmanak.manager.models.exceptions.InvalidAppConfigException
import ru.ifmo.kirmanak.manager.models.exceptions.NoAppConnectionException
import ru.ifmo.kirmanak.manager.models.exceptions.NoSuchApplicationException
import ru.ifmo.kirmanak.manager.models.requests.OpenNebulaRequest
import ru.ifmo.kirmanak.manager.models.requests.ScaleRequest
import ru.ifmo.kirmanak.manager.models.responses.*
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
        val id = saveConfiguration(config)

        return result("createKubernetes", AppIdResponse(id))
    }

    @PostMapping("/api/v1/opennebula")
    fun createOpenNebula(
            @RequestBody request: OpenNebulaRequest
    ): AppIdResponse {
        logger.info("createOpenNebula(request = $request)")

        val config = OpenNebulaConfigEntity(request.address, request.login, request.password, request.role, request.template, request.vmgroup)
        checkAppConfig(config)
        val id = saveConfiguration(config)

        return result("createOpenNebula", AppIdResponse(id))
    }

    @GetMapping("/api/v1/app/{id}")
    fun getApplicationInfo(@PathVariable("id") id: Long): Array<AppInstanceResponse> {
        logger.info("getApplicationInfo(id: \"$id\")")

        val client = getApp(id).getAppClient()

        val result = getAppInfo(client)

        return result("getApplicationInfo", result)
    }

    @GetMapping("/api/v1/app")
    fun getAppConfigurations(): AppConfigResponse {
        logger.info("getAppConfigurations()")

        val apps = appRepository.findAll()
        val kubernetesApps = apps.mapNotNull {
            KubernetesConfigResponse(
                    namespace = it.kubernetesConfig?.namespace ?: return@mapNotNull null,
                    deployment = it.kubernetesConfig.deployment,
                    appId = it.id ?: throw IllegalStateException("Id must not be null if data is retrieved from DB")
            )
        }

        val openNebulaApps = apps.mapNotNull {
            OpenNebulaConfigResponse(
                    address = it.openNebulaConfig?.address ?: return@mapNotNull null,
                    appId = it.id ?: throw IllegalStateException("Id must not be null if data is retrieved from DB"),
                    role = it.openNebulaConfig.role,
                    template = it.openNebulaConfig.template,
                    vmgroup = it.openNebulaConfig.vmgroup
            )
        }

        return result("getAppConfigurations", AppConfigResponse(openNebulaApps, kubernetesApps))
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
        saveConfiguration(updated, app)
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
        saveConfiguration(config, app)
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

    private fun saveConfiguration(config: KubernetesConfigEntity, oldApp: ApplicationEntity? = null): Long {
        val saved: KubernetesConfigEntity

        try {
            saved = kubernetesConfigRepo.save(config)
        } catch (e: DataIntegrityViolationException) {
            val existing = kubernetesConfigRepo.findByNamespaceAndDeploymentAndYaml(
                    config.namespace, config.deployment, config.yaml
            ) ?: throw e
            val id = existing.application?.id ?: throw e
            throw ExistingApplicationException(id)
        }

        val app = ApplicationEntity(kubernetesConfig = saved, id = oldApp?.id)
        return saveApplication(app)
    }

    private fun saveConfiguration(config: OpenNebulaConfigEntity, oldApp: ApplicationEntity? = null): Long {
        val saved: OpenNebulaConfigEntity

        try {
            saved = openNebulaConfigRepo.save(config)
        } catch (e: DataIntegrityViolationException) {
            val existing = openNebulaConfigRepo.findByAddressAndLoginAndPasswordAndRoleAndTemplateAndVmgroup(
                    config.address, config.login, config.password, config.role, config.template, config.vmgroup
            ) ?: throw e
            val id = existing.application?.id ?: throw e
            throw ExistingApplicationException(id)
        }

        val app = ApplicationEntity(openNebulaConfig = saved, id = oldApp?.id)
        return saveApplication(app)
    }

    private fun saveApplication(app: ApplicationEntity): Long {
        return appRepository.save(app).id
                ?: throw IllegalStateException("Id for a new application was not generated")
    }

    private fun <Result> result(method: String, result: Result): Result {
        logger.info("$method result: $result")
        return result
    }

    private fun <Result> result(method: String, result: Array<Result>): Array<Result> {
        logger.info("$method result: ${result.contentToString()}")
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