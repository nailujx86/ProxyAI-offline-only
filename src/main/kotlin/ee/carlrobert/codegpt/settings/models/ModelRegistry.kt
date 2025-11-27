package ee.carlrobert.codegpt.settings.models

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import ee.carlrobert.codegpt.completions.llama.LlamaModel
import ee.carlrobert.codegpt.settings.service.FeatureType
import ee.carlrobert.codegpt.settings.service.ServiceType
import ee.carlrobert.codegpt.settings.service.custom.CustomServicesSettings
import ee.carlrobert.codegpt.settings.service.ollama.OllamaSettings
import ee.carlrobert.llm.client.codegpt.PricingPlan
import javax.swing.Icon

data class ModelSelection(
    val provider: ServiceType,
    val model: String,
    val displayName: String,
    val icon: Icon? = null,
    val pricingPlan: PricingPlan? = null
) {
    val fullDisplayName: String = if (provider == ServiceType.LLAMA_CPP) {
        displayName
    } else {
        "$provider • $displayName"
    }
}

data class ModelCapability(
    val provider: ServiceType,
    val supportedFeatures: Set<FeatureType>,
)

@Service
class ModelRegistry {

    private val logger = thisLogger()

    private val providerCapabilities = mapOf(
        ServiceType.OLLAMA to ModelCapability(
            ServiceType.OLLAMA,
            setOf(
                FeatureType.CHAT, FeatureType.CODE_COMPLETION, FeatureType.AUTO_APPLY,
                FeatureType.COMMIT_MESSAGE, FeatureType.INLINE_EDIT, FeatureType.LOOKUP
            )
        ),
        ServiceType.LLAMA_CPP to ModelCapability(
            ServiceType.LLAMA_CPP,
            setOf(
                FeatureType.CHAT, FeatureType.CODE_COMPLETION, FeatureType.AUTO_APPLY,
                FeatureType.COMMIT_MESSAGE, FeatureType.INLINE_EDIT, FeatureType.LOOKUP
            )
        ),
        ServiceType.CUSTOM_OPENAI to ModelCapability(
            ServiceType.CUSTOM_OPENAI,
            setOf(
                FeatureType.CHAT, FeatureType.CODE_COMPLETION, FeatureType.AUTO_APPLY,
                FeatureType.COMMIT_MESSAGE, FeatureType.INLINE_EDIT, FeatureType.LOOKUP
            )
        )
    )

    private val fallbackDefaults = mapOf(
        FeatureType.CHAT to ModelSelection(
            ServiceType.CUSTOM_OPENAI,
            GEMINI_FLASH_2_5,
            "Gemini Flash 2.5"
        ),
        FeatureType.AUTO_APPLY to ModelSelection(
            ServiceType.CUSTOM_OPENAI,
            MERCURY_CODER,
            "Mercury Coder"
        ),
        FeatureType.COMMIT_MESSAGE to ModelSelection(
            ServiceType.CUSTOM_OPENAI,
            GPT_5_MINI,
            "GPT-5 Mini"
        ),
        FeatureType.INLINE_EDIT to ModelSelection(
            ServiceType.CUSTOM_OPENAI,
            GEMINI_FLASH_2_5,
            "Gemini Flash 2.5"
        ),
        FeatureType.LOOKUP to ModelSelection(ServiceType.CUSTOM_OPENAI, GPT_5_MINI, "GPT-5 Mini"),
        FeatureType.CODE_COMPLETION to ModelSelection(
            ServiceType.CUSTOM_OPENAI,
            MERCURY_CODER,
            "Mercury Coder"
        ),
        FeatureType.NEXT_EDIT to ModelSelection(ServiceType.CUSTOM_OPENAI, MERCURY_CODER, "Mercury Coder")
    )

    fun getAllModelsForFeature(featureType: FeatureType): List<ModelSelection> {
        return when (featureType) {
            FeatureType.CHAT, FeatureType.COMMIT_MESSAGE,
            FeatureType.INLINE_EDIT, FeatureType.LOOKUP -> getAllChatModels()

            FeatureType.AUTO_APPLY -> getAllApplyModels()
            FeatureType.CODE_COMPLETION -> getAllCodeModels()
            FeatureType.NEXT_EDIT -> getNextEditModels()
        }
    }

    fun getDefaultModelForFeature(
        featureType: FeatureType,
    ): ModelSelection {
        return fallbackDefaults[featureType]!!
    }

    fun getProvidersForFeature(featureType: FeatureType): List<ServiceType> {
        return providerCapabilities.values
            .filter { it.supportedFeatures.contains(featureType) }
            .map { it.provider }
    }

    fun isFeatureSupportedByProvider(featureType: FeatureType, provider: ServiceType): Boolean {
        return providerCapabilities[provider]?.supportedFeatures?.contains(featureType) == true
    }

    fun findModel(provider: ServiceType, modelCode: String): ModelSelection? {
        return getAllModels()
            .filter { it.provider == provider }
            .find { it.model == modelCode }
    }

    fun getModelDisplayName(provider: ServiceType, modelCode: String): String {
        return findModel(provider, modelCode)?.displayName ?: modelCode
    }

    private fun getAllModels(): List<ModelSelection> {
        return buildList {
            addAll(getAllApplyModels())
            addAll(getAllChatModels())
            addAll(getAllCodeModels())
            addAll(getNextEditModels())
        }.distinctBy { "${it.provider}:${it.model}" }
    }

    private fun getAllChatModels(): List<ModelSelection> {
        return buildList {
            addAll(getLlamaModels())
            addAll(getOllamaModels())
            addAll(getCustomOpenAIModels())
        }
    }

    private fun getAllApplyModels(): List<ModelSelection> {
        return buildList {
            addAll(getLlamaModels())
            addAll(getOllamaModels())
            addAll(getCustomOpenAIModels())
        }
    }

    private fun getAllCodeModels(): List<ModelSelection> {
        return buildList {
            addAll(getLlamaModels())
            addAll(getCustomOpenAICodeModels())
            addAll(getOllamaModels())
        }
    }

    private fun getCustomOpenAICodeModels(): List<ModelSelection> {
        return try {
            val customServicesSettings = service<CustomServicesSettings>()
            customServicesSettings.state.services.mapNotNull { service ->
                val serviceId = service.id ?: return@mapNotNull null
                val serviceName = service.name ?: ""
                val modelFromBody = service.codeCompletionSettings.body["model"]
                val modelName = (modelFromBody as? String)
                val displayName = if (!modelName.isNullOrEmpty()) {
                    if (modelName.length > 20) "$serviceName (...${modelName.takeLast(20)})" else "$serviceName ($modelName)"
                } else serviceName

                ModelSelection(ServiceType.CUSTOM_OPENAI, serviceId, displayName)
            }
        } catch (e: Exception) {
            logger.error("Failed to get Custom OpenAI code models", e)
            emptyList()
        }
    }

    private fun getNextEditModels(): List<ModelSelection> {
        return listOf(
        )
    }


    private fun getOllamaModels(): List<ModelSelection> {
        return try {
            val ollamaSettings = service<OllamaSettings>()
            ollamaSettings.state.availableModels.map { model ->
                ModelSelection(ServiceType.OLLAMA, model, model)
            }
        } catch (e: Exception) {
            logger.error("Failed to get Ollama models", e)
            emptyList()
        }
    }

    fun getCustomOpenAIModels(): List<ModelSelection> {
        return try {
            val customServicesSettings = service<CustomServicesSettings>()
            customServicesSettings.state.services.mapNotNull { service ->
                val serviceId = service.id ?: return@mapNotNull null
                val serviceName = service.name ?: ""
                val modelName = service.chatCompletionSettings.body["model"] as? String
                val displayName = if (!modelName.isNullOrEmpty()) {
                    if (modelName.length > 20) "$serviceName (...${modelName.takeLast(20)})" else "$serviceName ($modelName)"
                } else serviceName

                ModelSelection(ServiceType.CUSTOM_OPENAI, serviceId, displayName)
            }
        } catch (e: Exception) {
            logger.error("Failed to get Custom OpenAI models", e)
            emptyList()
        }
    }

    private fun getLlamaModels(): List<ModelSelection> {
        return try {
            LlamaModel.entries.flatMap { llamaModel ->
                llamaModel.huggingFaceModels.map { hfModel ->
                    val displayName =
                        "${llamaModel.label} (${hfModel.parameterSize}B) / Q${hfModel.quantization}"
                    ModelSelection(ServiceType.LLAMA_CPP, hfModel.name, displayName)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to get llama.cpp models", e)
            emptyList()
        }
    }

    companion object {
        // ProxyAI Models
        const val GEMINI_PRO_2_5 = "gemini-pro-2.5"
        const val GEMINI_FLASH_2_5 = "gemini-flash-2.5"
        const val CLAUDE_4_SONNET = "claude-4-sonnet"
        const val CLAUDE_4_SONNET_THINKING = "claude-4-sonnet-thinking"
        const val CLAUDE_4_5_SONNET = "claude-sonnet-4-5"
        const val CLAUDE_4_5_SONNET_THINKING = "claude-sonnet-4-5-thinking"
        const val DEEPSEEK_R1 = "deepseek-r1"
        const val DEEPSEEK_V3 = "deepseek-v3"
        const val QWEN_2_5_32B_CODE = "qwen-2.5-32b-code"
        const val QWEN3_CODER = "qwen3-coder"
        const val RELACE = "relace"
        const val MORPH = "morph"

        // OpenAI Models
        const val GPT_3_5_TURBO_INSTRUCT = "gpt-3.5-turbo-instruct"
        const val O4_MINI = "o4-mini"
        const val O3_PRO = "o3-pro"
        const val O3 = "o3"
        const val O3_MINI = "o3-mini"
        const val O1_PREVIEW = "o1-preview"
        const val O1_MINI = "o1-mini"
        const val GPT_4_1 = "gpt-4.1"
        const val GPT_4_1_MINI = "gpt-4.1-mini"
        const val GPT_4_1_NANO = "gpt-4.1-nano"
        const val GPT_4O = "gpt-4o"
        const val GPT_4O_MINI = "gpt-4o-mini"
        const val GPT_4_0125_PREVIEW = "gpt-4-0125-preview"
        const val GPT_4_VISION_PREVIEW = "gpt-4-vision-preview"
        const val GPT_5 = "gpt-5"
        const val GPT_5_MINI = "gpt-5-mini"
        const val GPT_5_CODEX = "gpt-5-codex"

        // Anthropic Models
        const val CLAUDE_OPUS_4_20250514 = "claude-opus-4-20250514"
        const val CLAUDE_SONNET_4_20250514 = "claude-sonnet-4-20250514"

        // Google Models
        const val GEMINI_2_0_FLASH = "gemini-2.0-flash"

        // Mistral Models
        const val MISTRAL_LARGE_2411 = "mistral-large-2411"
        const val DEVSTRAL_MEDIUM_2507 = "devstral-medium-2507"
        const val CODESTRAL_LATEST = "codestral-latest"

        // Ollama default models
        const val LLAMA_3_2 = "llama3.2"

        // Llama.cpp default models
        const val LLAMA_3_2_3B_INSTRUCT = "llama-3.2-3b-instruct"

        const val MERCURY_CODER = "mercury-coder"

        @JvmStatic
        fun getInstance(): ModelRegistry {
            return ApplicationManager.getApplication().getService(ModelRegistry::class.java)
        }
    }
}
