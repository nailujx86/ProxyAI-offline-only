package ee.carlrobert.codegpt.settings.service

import com.intellij.openapi.components.service
import ee.carlrobert.codegpt.settings.models.ModelRegistry
import ee.carlrobert.codegpt.settings.models.ModelSettings
import ee.carlrobert.codegpt.settings.models.ModelSettingsState
import org.assertj.core.api.Assertions.assertThat
import testsupport.IntegrationTest

class ModelSelectionServiceTest : IntegrationTest() {

    private lateinit var modelSelectionService: ModelSelectionService
    private lateinit var modelSettings: ModelSettings

    override fun setUp() {
        super.setUp()
        modelSelectionService = service<ModelSelectionService>()
        modelSettings = service<ModelSettings>()

        val cleanState = ModelSettingsState()
        cleanState.modelSelections.clear()
        modelSettings.loadState(cleanState)
    }

    fun `test getModelSelectionForFeature with valid feature returns model`() {
        val result = modelSelectionService.getModelSelectionForFeature(FeatureType.CHAT)
        val expected = ModelRegistry.getInstance().getDefaultModelForFeature(FeatureType.CHAT)

        assertThat(result.provider).isEqualTo(expected.provider)
        assertThat(result.model).isEqualTo(expected.model)
        assertThat(result.displayName).isEqualTo(expected.displayName)
    }

//    fun `test getModelSelectionForFeature with pricing plan returns plan-specific model`() {
//        val individualResult = modelSelectionService.getModelSelectionForFeature(
//            FeatureType.CHAT
//        )
//        val freeResult =
//            modelSelectionService.getModelSelectionForFeature(FeatureType.CHAT)
//
//        assertThat(individualResult.model).isEqualTo("claude-sonnet-4-5-thinking")
//        assertThat(freeResult.model).isEqualTo("qwen3-coder")
//    }


    fun `test getModelForFeature with valid feature returns model string`() {
        val result = modelSelectionService.getModelForFeature(FeatureType.CHAT)
        val expected = ModelRegistry.getInstance().getDefaultModelForFeature(FeatureType.CHAT)

        assertThat(result).isEqualTo(expected.model)
    }

//    fun `test getModelForFeature with pricing plan returns plan-specific model`() {
//        val result =
//            modelSelectionService.getModelForFeature(FeatureType.CHAT)
//
//        assertThat(result).isEqualTo("claude-sonnet-4-5-thinking")
//    }
}