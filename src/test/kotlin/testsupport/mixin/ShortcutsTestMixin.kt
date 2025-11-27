package testsupport.mixin

import com.intellij.openapi.components.service
import com.intellij.testFramework.PlatformTestUtil
import ee.carlrobert.codegpt.completions.HuggingFaceModel
import ee.carlrobert.codegpt.credentials.CredentialsStore.CredentialKey.*
import ee.carlrobert.codegpt.credentials.CredentialsStore.setCredential
import ee.carlrobert.codegpt.settings.models.ModelSettings
import ee.carlrobert.codegpt.settings.service.FeatureType
import ee.carlrobert.codegpt.settings.service.ServiceType
import ee.carlrobert.codegpt.settings.service.llama.LlamaSettings
import ee.carlrobert.codegpt.settings.service.ollama.OllamaSettings
import ee.carlrobert.llm.client.google.models.GoogleModel
import java.util.function.BooleanSupplier

interface ShortcutsTestMixin {

    fun useLlamaService(
        codeCompletionsEnabled: Boolean = false,
        role: FeatureType = FeatureType.CHAT
    ) {
        LlamaSettings.getCurrentState().serverPort = null
        LlamaSettings.getCurrentState().isCodeCompletionsEnabled = codeCompletionsEnabled
        LlamaSettings.getCurrentState().huggingFaceModel = HuggingFaceModel.CODE_LLAMA_7B_Q4
        
        val modelSettings = service<ModelSettings>()
        when (role) {
            FeatureType.CHAT -> {
                modelSettings.setModel(FeatureType.CHAT, HuggingFaceModel.CODE_LLAMA_7B_Q4.code, ServiceType.LLAMA_CPP)
            }
            FeatureType.CODE_COMPLETION -> {
                modelSettings.setModel(FeatureType.CODE_COMPLETION, HuggingFaceModel.CODE_LLAMA_7B_Q4.code, ServiceType.LLAMA_CPP)
            }
            else -> {
                modelSettings.setModel(FeatureType.CHAT, HuggingFaceModel.CODE_LLAMA_7B_Q4.code, ServiceType.LLAMA_CPP)
                modelSettings.setModel(FeatureType.CODE_COMPLETION, HuggingFaceModel.CODE_LLAMA_7B_Q4.code, ServiceType.LLAMA_CPP)
            }
        }
    }

    fun useOllamaService(role: FeatureType = FeatureType.CHAT) {
        setCredential(OllamaApikey, "TEST_API_KEY")
        service<OllamaSettings>().state.apply {
            model = HuggingFaceModel.LLAMA_3_8B_Q6_K.code
            codeCompletionsEnabled = true
            fimOverride = false
            host = null
            availableModels = mutableListOf(
                HuggingFaceModel.LLAMA_3_8B_Q6_K.code,
                HuggingFaceModel.CODE_QWEN_2_5_3B_Q4_K_M.code
            )
        }
        
        val modelSettings = service<ModelSettings>()
        when (role) {
            FeatureType.CHAT -> {
                modelSettings.setModel(FeatureType.CHAT, HuggingFaceModel.LLAMA_3_8B_Q6_K.code, ServiceType.OLLAMA)
            }
            FeatureType.CODE_COMPLETION -> {
                modelSettings.setModel(FeatureType.CODE_COMPLETION, HuggingFaceModel.CODE_QWEN_2_5_3B_Q4_K_M.code, ServiceType.OLLAMA)
            }
            else -> {
                modelSettings.setModel(FeatureType.CHAT, HuggingFaceModel.LLAMA_3_8B_Q6_K.code, ServiceType.OLLAMA)
                modelSettings.setModel(FeatureType.CODE_COMPLETION, HuggingFaceModel.CODE_QWEN_2_5_3B_Q4_K_M.code, ServiceType.OLLAMA)
            }
        }
    }

    fun waitExpecting(condition: BooleanSupplier?) {
        PlatformTestUtil.waitWithEventsDispatching(
            "Waiting for message response timed out or did not meet expected conditions",
            condition!!,
            5
        )
    }
}
