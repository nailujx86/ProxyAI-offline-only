package ee.carlrobert.codegpt.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import ee.carlrobert.codegpt.codecompletions.CodeCompletionService
import ee.carlrobert.codegpt.settings.service.FeatureType
import ee.carlrobert.codegpt.settings.service.ModelSelectionService
import ee.carlrobert.codegpt.settings.service.ServiceType.*
import ee.carlrobert.codegpt.settings.service.custom.CustomServicesSettings
import ee.carlrobert.codegpt.settings.service.llama.LlamaSettings
import ee.carlrobert.codegpt.settings.service.ollama.OllamaSettings

abstract class CodeCompletionFeatureToggleActions(
    private val enableFeatureAction: Boolean
) : DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val serviceType =
            service<ModelSelectionService>().getServiceForFeature(FeatureType.CODE_COMPLETION)
        when (serviceType) {
            LLAMA_CPP -> {
                LlamaSettings.getCurrentState().isCodeCompletionsEnabled = enableFeatureAction
            }

            OLLAMA -> {
                service<OllamaSettings>().state.codeCompletionsEnabled = enableFeatureAction
            }

            CUSTOM_OPENAI -> {
                service<CustomServicesSettings>()
                    .customServiceStateForFeatureType(FeatureType.CODE_COMPLETION)
                    .codeCompletionSettings.codeCompletionsEnabled = enableFeatureAction
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val selectedService =
            service<ModelSelectionService>().getServiceForFeature(FeatureType.CODE_COMPLETION)
        val codeCompletionEnabled =
            service<CodeCompletionService>().isCodeCompletionsEnabled(selectedService) == true
        e.presentation.isVisible = codeCompletionEnabled != enableFeatureAction
        e.presentation.isEnabled = when (selectedService) {
            CUSTOM_OPENAI,
            LLAMA_CPP,
            OLLAMA -> true
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

class EnableCompletionsAction : CodeCompletionFeatureToggleActions(true)

class DisableCompletionsAction : CodeCompletionFeatureToggleActions(false)
