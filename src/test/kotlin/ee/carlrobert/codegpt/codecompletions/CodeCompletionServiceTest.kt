package ee.carlrobert.codegpt.codecompletions

import com.intellij.codeInsight.inline.completion.session.InlineCompletionSession.Companion.getOrNull
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.PlatformTestUtil
import ee.carlrobert.codegpt.CodeGPTKeys.REMAINING_EDITOR_COMPLETION
import ee.carlrobert.codegpt.completions.HuggingFaceModel
import ee.carlrobert.codegpt.settings.service.FeatureType
import ee.carlrobert.codegpt.util.file.FileUtil
import ee.carlrobert.llm.client.http.RequestEntity
import ee.carlrobert.llm.client.http.exchange.StreamHttpExchange
import ee.carlrobert.llm.client.util.JSONUtil.*
import org.assertj.core.api.Assertions.assertThat
import testsupport.IntegrationTest

class CodeCompletionServiceTest : IntegrationTest() {

    fun `test code completion with Ollama provider and separate model settings`() {
        useOllamaService(FeatureType.CODE_COMPLETION)
        myFixture.configureByText(
            "CompletionTest.txt",
            FileUtil.getResourceContent("/codecompletions/code-completion-file.txt")
        )
        myFixture.editor.caretModel.moveToVisualPosition(VisualPosition(3, 0))
        project.service<CodeCompletionCacheService>().clear()
        val prefix = """
             xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
             zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz
             [INPUT]
             p
             """.trimIndent()
        val suffix = """
             
             [\INPUT]
             zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz
             xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
             """.trimIndent()
        expectOllama(StreamHttpExchange { request: RequestEntity ->
            assertThat(request.uri.path).isEqualTo("/api/generate")
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.body)
                .extracting("model", "prompt", "suffix")
                .containsExactly(HuggingFaceModel.CODE_QWEN_2_5_3B_Q4_K_M.code, prefix, suffix)
            listOf(
                jsonMapResponse(
                    e("model", HuggingFaceModel.CODE_QWEN_2_5_3B_Q4_K_M.code),
                    e("created_at", "2023-08-04T08:52:19.385406455-07:00"),
                    e("response", "rivate void main"),
                    e("done", true),
                ),
            )
        })

        myFixture.type('p')

        assertInlineSuggestion("Failed to display initial inline suggestion.") {
            "rivate void main" == it
        }
    }

    private fun assertRemainingCompletion(
        errorMessage: String = "Failed to assert remaining suggestion",
        onAssert: (String) -> Boolean
    ) {
        PlatformTestUtil.waitWithEventsDispatching(
            errorMessage,
            {
                val remainingCompletion = REMAINING_EDITOR_COMPLETION.get(myFixture.editor)
                    ?: return@waitWithEventsDispatching false
                onAssert(remainingCompletion)
            },
            5
        )
    }

    private fun assertInlineSuggestion(
        errorMessage: String = "Failed to assert inline suggestion",
        onAssert: (String) -> Boolean
    ) {
        PlatformTestUtil.waitWithEventsDispatching(
            errorMessage,
            {
                val session = getOrNull(myFixture.editor) ?: return@waitWithEventsDispatching false
                onAssert(session.context.textToInsert())
            },
            5
        )
    }
}
