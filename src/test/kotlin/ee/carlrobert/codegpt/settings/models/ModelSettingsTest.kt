package ee.carlrobert.codegpt.settings.models

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.util.messages.MessageBusConnection
import ee.carlrobert.codegpt.settings.service.FeatureType
import ee.carlrobert.codegpt.settings.service.ModelChangeNotifier
import ee.carlrobert.codegpt.settings.service.ServiceType
import org.assertj.core.api.Assertions.assertThat
import testsupport.IntegrationTest
import java.util.concurrent.atomic.AtomicReference

class ModelSettingsTest : IntegrationTest() {

    private lateinit var modelSettings: ModelSettings
    private lateinit var connection: MessageBusConnection
    private val lastNotification = AtomicReference<NotificationData>()

    data class NotificationData(
        val featureType: FeatureType,
        val model: String,
        val serviceType: ServiceType,
        val specificNotification: String
    )

    override fun setUp() {
        super.setUp()
        modelSettings = service<ModelSettings>()
        connection = ApplicationManager.getApplication().messageBus.connect()
        connection.subscribe(ModelChangeNotifier.getTopic(), object : ModelChangeNotifier {
            override fun chatModelChanged(newModel: String, serviceType: ServiceType) {
                lastNotification.set(NotificationData(FeatureType.CHAT, newModel, serviceType, "chat"))
            }
            override fun codeModelChanged(newModel: String, serviceType: ServiceType) {
                lastNotification.set(NotificationData(FeatureType.CODE_COMPLETION, newModel, serviceType, "code"))
            }
            override fun autoApplyModelChanged(newModel: String, serviceType: ServiceType) {
                lastNotification.set(NotificationData(FeatureType.AUTO_APPLY, newModel, serviceType, "autoApply"))
            }
            override fun commitMessageModelChanged(newModel: String, serviceType: ServiceType) {
                lastNotification.set(NotificationData(FeatureType.COMMIT_MESSAGE, newModel, serviceType, "commitMessage"))
            }
            override fun inlineEditModelChanged(newModel: String, serviceType: ServiceType) {
                lastNotification.set(NotificationData(FeatureType.INLINE_EDIT, newModel, serviceType, "inlineEdit"))
            }
            override fun nextEditModelChanged(newModel: String, serviceType: ServiceType) {
                lastNotification.set(NotificationData(FeatureType.NEXT_EDIT, newModel, serviceType, "nextEdit"))
            }
            override fun nameLookupModelChanged(newModel: String, serviceType: ServiceType) {
                lastNotification.set(NotificationData(FeatureType.LOOKUP, newModel, serviceType, "lookup"))
            }
            override fun modelChanged(featureType: FeatureType, newModel: String, serviceType: ServiceType) {
                lastNotification.compareAndSet(null, NotificationData(featureType, newModel, serviceType, "general"))
            }
        })
    }

    override fun tearDown() {
        connection.disconnect()
        super.tearDown()
    }


    fun `test migrateMissingProviderInformation with unknown model keeps model but no provider`() {
        val state = ModelSettingsState()
        val detailsState = ModelDetailsState()
        detailsState.model = "unknown-model"
        detailsState.provider = null
        state.modelSelections["CHAT"] = detailsState

        modelSettings.loadState(state)

        assertThat(modelSettings.getModelForFeature(FeatureType.CHAT)).isEqualTo("unknown-model")
        assertThat(modelSettings.getProviderForFeature(FeatureType.CHAT)).isNull()
    }
}