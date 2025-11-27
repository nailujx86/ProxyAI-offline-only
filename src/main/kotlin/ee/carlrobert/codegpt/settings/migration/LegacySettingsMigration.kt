package ee.carlrobert.codegpt.settings.migration

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import ee.carlrobert.codegpt.settings.GeneralSettings
import ee.carlrobert.codegpt.settings.models.ModelRegistry
import ee.carlrobert.codegpt.settings.models.ModelSettingsState
import ee.carlrobert.codegpt.settings.service.FeatureType
import ee.carlrobert.codegpt.settings.service.ServiceType
import ee.carlrobert.codegpt.settings.service.custom.CustomServicesSettings
import ee.carlrobert.codegpt.settings.service.llama.LlamaSettings
import ee.carlrobert.codegpt.settings.service.ollama.OllamaSettings

object LegacySettingsMigration {

    private val logger = thisLogger()

    fun migrateIfNeeded(): ModelSettingsState? {
        return try {
            val generalState = GeneralSettings.getCurrentState()
            val selectedService = generalState.selectedService

            if (selectedService != null) {
                generalState.selectedService = null
                createMigratedState(selectedService)
            } else {
                null
            }
        } catch (exception: Exception) {
            logger.error("Failed to migrate legacy settings", exception)
            null
        }
    }

    private fun createMigratedState(selectedService: ServiceType): ModelSettingsState {
        return ModelSettingsState().apply {
            val chatModel = getLegacyChatModelForService(selectedService)

            setModelSelection(FeatureType.CHAT, chatModel, selectedService)
            setModelSelection(FeatureType.AUTO_APPLY, chatModel, selectedService)
            setModelSelection(FeatureType.COMMIT_MESSAGE, chatModel, selectedService)
            setModelSelection(FeatureType.INLINE_EDIT, chatModel, selectedService)
            setModelSelection(FeatureType.LOOKUP, chatModel, selectedService)

            val codeModel = getLegacyCodeModelForService(selectedService)
            setModelSelection(FeatureType.CODE_COMPLETION, codeModel, selectedService)

            setModelSelection(FeatureType.NEXT_EDIT, null, selectedService)
        }
    }

    private fun getLegacyChatModelForService(serviceType: ServiceType): String {
        return try {
            when (serviceType) {
                ServiceType.OLLAMA -> {
                    val settings = service<OllamaSettings>()
                    settings.state.model ?: ModelRegistry.LLAMA_3_2
                }

                ServiceType.LLAMA_CPP -> {
                    val llamaSettings = LlamaSettings.getCurrentState()
                    if (llamaSettings.isUseCustomModel) {
                        llamaSettings.customLlamaModelPath
                    } else {
                        llamaSettings.huggingFaceModel.name
                    }
                }

                ServiceType.CUSTOM_OPENAI -> {
                    service<CustomServicesSettings>().state.services
                        .map { it.name }
                        .lastOrNull()
                        ?.takeIf { it.isNotBlank() } ?: "Default"
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to get legacy chat model for $serviceType", e)
            throw e
        }
    }

    private fun getLegacyCodeModelForService(serviceType: ServiceType): String? {
        return try {
            when (serviceType) {
               ServiceType.OLLAMA -> {
                    service<OllamaSettings>().state.model
                }

                ServiceType.LLAMA_CPP -> {
                    val llamaSettings = LlamaSettings.getCurrentState()
                    if (llamaSettings.isUseCustomModel) {
                        llamaSettings.customLlamaModelPath
                    } else {
                        llamaSettings.huggingFaceModel.name
                    }
                }

                ServiceType.CUSTOM_OPENAI -> {
                    service<CustomServicesSettings>().state.services
                        .map { it.name }
                        .lastOrNull() ?: ""
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to get legacy code model for $serviceType", e)
            null
        }
    }
}
