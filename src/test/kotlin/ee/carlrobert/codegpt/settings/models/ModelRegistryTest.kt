package ee.carlrobert.codegpt.settings.models

import ee.carlrobert.codegpt.settings.service.FeatureType
import ee.carlrobert.codegpt.settings.service.ServiceType
import ee.carlrobert.llm.client.codegpt.PricingPlan
import org.assertj.core.api.Assertions.assertThat
import testsupport.IntegrationTest

class ModelRegistryTest : IntegrationTest() {

    private lateinit var modelRegistry: ModelRegistry

    override fun setUp() {
        super.setUp()
        modelRegistry = ModelRegistry.getInstance()
    }

    fun `test getProvidersForFeature with chat returns all chat providers`() {
        val result = modelRegistry.getProvidersForFeature(FeatureType.CHAT)

        assertThat(result).containsExactlyInAnyOrder(
            ServiceType.OLLAMA,
            ServiceType.LLAMA_CPP,
            ServiceType.CUSTOM_OPENAI
        )
    }

    fun `test getProvidersForFeature with code completion returns code providers only`() {
        val result = modelRegistry.getProvidersForFeature(FeatureType.CODE_COMPLETION)

        assertThat(result).containsExactlyInAnyOrder(
            ServiceType.OLLAMA,
            ServiceType.LLAMA_CPP,
            ServiceType.CUSTOM_OPENAI
        )
    }
}
