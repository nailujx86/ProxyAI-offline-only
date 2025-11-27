package ee.carlrobert.codegpt.toolwindow.chat

import com.intellij.openapi.components.service
import com.intellij.testFramework.LightVirtualFile
import ee.carlrobert.codegpt.CodeGPTKeys
import ee.carlrobert.codegpt.EncodingManager
import ee.carlrobert.codegpt.completions.ConversationType
import ee.carlrobert.codegpt.completions.HuggingFaceModel
import ee.carlrobert.codegpt.completions.llama.PromptTemplate.LLAMA
import ee.carlrobert.codegpt.conversations.ConversationService
import ee.carlrobert.codegpt.conversations.message.Message
import ee.carlrobert.codegpt.settings.configuration.ConfigurationSettings
import ee.carlrobert.codegpt.settings.prompts.PromptsSettings
import ee.carlrobert.codegpt.settings.service.llama.LlamaSettings
import ee.carlrobert.llm.client.http.RequestEntity
import ee.carlrobert.llm.client.http.exchange.StreamHttpExchange
import ee.carlrobert.llm.client.util.JSONUtil.*
import org.apache.http.HttpHeaders
import ee.carlrobert.codegpt.util.file.FileUtil.getResourceContent
import org.assertj.core.api.Assertions.assertThat
import testsupport.IntegrationTest
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class ChatToolWindowTabPanelTest : IntegrationTest() {

    fun testSendingLlamaMessage() {
        useLlamaService()
        val configurationState = service<ConfigurationSettings>().state
        service<PromptsSettings>().state.personas.selectedPersona.instructions =
            "TEST_SYSTEM_PROMPT"
        configurationState.maxTokens = 1000
        configurationState.temperature = 0.1f
        val llamaSettings = LlamaSettings.getCurrentState()
        llamaSettings.isUseCustomModel = false
        llamaSettings.huggingFaceModel = HuggingFaceModel.CODE_LLAMA_7B_Q4
        llamaSettings.topK = 30
        llamaSettings.topP = 0.8
        llamaSettings.minP = 0.03
        llamaSettings.repeatPenalty = 1.3
        val message = Message("TEST_PROMPT")
        val conversation = ConversationService.getInstance().startConversation(project)
        val panel = ChatToolWindowTabPanel(project, conversation)
        expectLlama(StreamHttpExchange { request: RequestEntity ->
            assertThat(request.uri.path).isEqualTo("/v1/chat/completions")
            val expectedSystem = getResourceContent("/prompts/persona/psi-navigation-guidelines.txt").let { g ->
                "TEST_SYSTEM_PROMPT\n$g"
            }
            assertThat(request.body)
                .extracting(
                    "model",
                    "messages"
                )
                .containsExactly(
                    HuggingFaceModel.CODE_LLAMA_7B_Q4.code,
                    listOf(
                        mapOf("role" to "system", "content" to expectedSystem),
                        mapOf("role" to "user", "content" to "TEST_PROMPT")
                    )
                )
            listOf(
                jsonMapResponse(
                    "choices",
                    jsonArray(jsonMap("delta", jsonMap("role", "assistant")))
                ),
                jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("content", "Hel")))),
                jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("content", "lo")))),
                jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("content", "!"))))
            )
        })

        panel.sendMessage(message, ConversationType.DEFAULT)

        waitExpecting {
            val messages = conversation.messages
            messages.isNotEmpty() && "Hello!" == messages[0].response
                    && panel.tokenDetails.conversationTokens > 0
        }
        assertThat(panel.conversation)
            .isNotNull()
            .extracting("id", "discardTokenLimit")
            .containsExactly(conversation.id, false)
        val messages = panel.conversation.messages
        assertThat(messages).hasSize(1)
        assertThat(messages[0])
            .extracting("id", "prompt", "response")
            .containsExactly(message.id, message.prompt, message.response)
    }
}
