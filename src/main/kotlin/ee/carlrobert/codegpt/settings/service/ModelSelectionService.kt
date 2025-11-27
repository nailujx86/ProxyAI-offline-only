package ee.carlrobert.codegpt.settings.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import ee.carlrobert.codegpt.settings.models.ModelRegistry
import ee.carlrobert.codegpt.settings.models.ModelSelection
import ee.carlrobert.codegpt.settings.models.ModelSettings

@Service
class ModelSelectionService {

    fun getModelSelectionForFeature(
        featureType: FeatureType
    ): ModelSelection {
        return try {
            val modelSettings = service<ModelSettings>()
            val modelDetailsState = modelSettings.state.getModelSelection(featureType)
            
            if (modelDetailsState != null && modelDetailsState.model != null && modelDetailsState.provider != null) {
                val foundModel = service<ModelRegistry>().findModel(modelDetailsState.provider!!, modelDetailsState.model!!)
                if (foundModel != null) {
                    return foundModel
                }
            }
            
            service<ModelRegistry>().getDefaultModelForFeature(featureType)
        } catch (exception: Exception) {
            logger.warn(
                "Error getting model selection for feature: $featureType, using default",
                exception
            )
            service<ModelRegistry>().getDefaultModelForFeature(featureType)
        }
    }

    fun getServiceForFeature(featureType: FeatureType): ServiceType {
        return try {
            getModelSelectionForFeature(featureType).provider
        } catch (exception: Exception) {
            logger.warn("Error getting service for feature: $featureType, using default", exception)
            ServiceType.CUSTOM_OPENAI
        }
    }

    fun getModelForFeature(featureType: FeatureType): String {
        return try {
            getModelSelectionForFeature(featureType).model
        } catch (exception: Exception) {
            logger.warn("Error getting model for feature: $featureType, using default", exception)
            service<ModelRegistry>().getDefaultModelForFeature(featureType).model
        }
    }

    fun syncWithAvailableCustomOpenAIModels(preferredServiceId: String? = null) {
        val registry = service<ModelRegistry>()
        val settings = service<ModelSettings>()

        FeatureType.entries.forEach { featureType ->
            if (!registry.isFeatureSupportedByProvider(featureType, ServiceType.CUSTOM_OPENAI)) return@forEach

            val current = settings.getModelSelection(featureType)
            if (current?.provider != ServiceType.CUSTOM_OPENAI) return@forEach

            val available = registry.getAllModelsForFeature(featureType)
                .filter { it.provider == ServiceType.CUSTOM_OPENAI }

            val isCurrentValid = available.any { it.model == current.model }

            if (!isCurrentValid) {
                val newId = when {
                    !preferredServiceId.isNullOrBlank() && available.any { it.model == preferredServiceId } -> preferredServiceId
                    available.isNotEmpty() -> available.first().model
                    else -> null
                }
                settings.setModelWithProvider(featureType, newId, ServiceType.CUSTOM_OPENAI)
            }
        }
    }

    companion object {

        private val logger = thisLogger()

        @JvmStatic
        fun getInstance(): ModelSelectionService {
            return ApplicationManager.getApplication().getService(ModelSelectionService::class.java)
        }
    }
}
