package ee.carlrobert.codegpt.toolwindow.chat.ui.textarea;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.ui.popup.ListPopup;
import ee.carlrobert.codegpt.Icons;
import ee.carlrobert.codegpt.completions.llama.LlamaModel;
import ee.carlrobert.codegpt.settings.models.ModelRegistry;
import ee.carlrobert.codegpt.settings.models.ModelSettings;
import ee.carlrobert.codegpt.settings.models.ModelSettingsConfigurable;
import ee.carlrobert.codegpt.settings.service.FeatureType;
import ee.carlrobert.codegpt.settings.service.ModelChangeNotifier;
import ee.carlrobert.codegpt.settings.service.ModelChangeNotifierAdapter;
import ee.carlrobert.codegpt.settings.service.ServiceType;
import ee.carlrobert.codegpt.settings.service.custom.CustomServiceSettingsState;
import ee.carlrobert.codegpt.settings.service.custom.CustomServicesSettings;
import ee.carlrobert.codegpt.settings.service.llama.LlamaSettings;
import ee.carlrobert.codegpt.settings.service.ollama.OllamaSettings;
import ee.carlrobert.codegpt.toolwindow.ui.ModelListPopup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static ee.carlrobert.codegpt.settings.service.ServiceType.*;

public class ModelComboBoxAction extends ComboBoxAction {

    private static final Logger LOG = Logger.getInstance(ModelComboBoxAction.class);

    private final Consumer<ServiceType> onModelChange;
    private final Project project;
    private final List<ServiceType> availableProviders;
    private final boolean showConfigureModels;
    private final FeatureType featureType;

    public ModelComboBoxAction(
            Project project,
            Consumer<ServiceType> onModelChange,
            ServiceType selectedService) {
        this(project, onModelChange, selectedService, Arrays.asList(ServiceType.values()), true,
                FeatureType.CHAT);
    }

    public ModelComboBoxAction(
            Project project,
            Consumer<ServiceType> onModelChange,
            ServiceType selectedProvider,
            List<ServiceType> availableProviders,
            boolean showConfigureModels) {
        this(project, onModelChange, selectedProvider, availableProviders, showConfigureModels,
                FeatureType.CHAT);
    }

    public ModelComboBoxAction(
            Project project,
            Consumer<ServiceType> onModelChange,
            ServiceType selectedProvider,
            List<ServiceType> availableProviders,
            boolean showConfigureModels,
            FeatureType featureType) {
        this.project = project;
        this.onModelChange = onModelChange;
        this.availableProviders = availableProviders;
        this.showConfigureModels = showConfigureModels;
        this.featureType = featureType;
        setSmallVariant(true);
        updateTemplatePresentation(selectedProvider);

        var messageBus = ApplicationManager.getApplication().getMessageBus().connect();
        messageBus.subscribe(
                ModelChangeNotifier.getTopic(),
                new ModelChangeNotifierAdapter() {
                    @Override
                    public void modelChanged(@NotNull FeatureType changedFeature,
                                             @NotNull String newModel,
                                             @NotNull ServiceType serviceType) {
                        if (changedFeature == featureType) {
                            updateTemplatePresentation(serviceType);
                        }
                    }
                });
    }

    public JComponent createCustomComponent(@NotNull String place) {
        return createCustomComponent(getTemplatePresentation(), place);
    }

    @NotNull
    @Override
    public JComponent createCustomComponent(
            @NotNull Presentation presentation,
            @NotNull String place) {
        ComboBoxButton button = createComboBoxButton(presentation);
        button.setForeground(
                EditorColorsManager.getInstance().getGlobalScheme().getDefaultForeground());
        button.setBorder(null);
        button.putClientProperty("JButton.backgroundColor", new Color(0, 0, 0, 0));
        return button;
    }

    @Override
    protected JBPopup createActionPopup(DefaultActionGroup group, @NotNull DataContext context,
                                        @Nullable Runnable disposeCallback) {
        ListPopup popup = new ModelListPopup(group, context);
        if (disposeCallback != null) {
            popup.addListener(new JBPopupListener() {
                @Override
                public void onClosed(@NotNull LightweightWindowEvent event) {
                    disposeCallback.run();
                }
            });
        }
        popup.setShowSubmenuOnHover(true);
        return popup;
    }

    @Override
    protected @NotNull DefaultActionGroup createPopupActionGroup(JComponent button) {
        var presentation = ((ComboBoxButton) button).getPresentation();
        var actionGroup = new DefaultActionGroup();

        actionGroup.addSeparator("Cloud");

        if (availableProviders.contains(CUSTOM_OPENAI)) {
            List<CustomServiceSettingsState> services = ApplicationManager.getApplication()
                    .getService(CustomServicesSettings.class)
                    .getState()
                    .getServices();

            var customGroup = DefaultActionGroup.createPopupGroup(() -> "Custom OpenAI");
            customGroup.getTemplatePresentation().setIcon(Icons.OpenAI);
            services.forEach(model ->
                    customGroup.add(createCustomOpenAIModelAction(model, presentation))
            );
            actionGroup.add(customGroup);
        }

        if (availableProviders.contains(LLAMA_CPP) || availableProviders.contains(OLLAMA)) {
            actionGroup.addSeparator("Offline");

            if (availableProviders.contains(LLAMA_CPP)) {
                actionGroup.add(createLlamaModelAction(presentation));
            }

            if (availableProviders.contains(OLLAMA)) {
                var ollamaGroup = DefaultActionGroup.createPopupGroup(() -> "Ollama");
                ollamaGroup.getTemplatePresentation().setIcon(Icons.Ollama);
                ApplicationManager.getApplication()
                        .getService(OllamaSettings.class)
                        .getState()
                        .getAvailableModels()
                        .forEach(model ->
                                ollamaGroup.add(createOllamaModelAction(model, presentation)));
                actionGroup.add(ollamaGroup);
            }
        }

        if (showConfigureModels) {
            actionGroup.addSeparator();
            actionGroup.add(new DumbAwareAction("Configure Models", "", AllIcons.General.Settings) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(
                            e.getProject(),
                            ModelSettingsConfigurable.class
                    );
                }

                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.BGT;
                }
            });
        }

        return actionGroup;
    }

    @Override
    protected boolean shouldShowDisabledActions() {
        return true;
    }

    private void updateTemplatePresentation(ServiceType selectedService) {
        var application = ApplicationManager.getApplication();
        var templatePresentation = getTemplatePresentation();
        var chatModel = application.getService(ModelSettings.class).getState()
                .getModelSelection(featureType);
        var modelCode = chatModel != null ? chatModel.getModel() : null;

        switch (selectedService) {
            case CUSTOM_OPENAI:
                ModelRegistry.getInstance().getCustomOpenAIModels().stream()
                        .filter(it -> it.getModel().equals(modelCode))
                        .findFirst()
                        .ifPresent(selection -> {
                            templatePresentation.setIcon(Icons.OpenAI);
                            templatePresentation.setText(selection.getDisplayName());
                        });
                break;
            case LLAMA_CPP:
                templatePresentation.setText(getLlamaCppPresentationText());
                templatePresentation.setIcon(Icons.Llama);
                break;
            case OLLAMA:
                templatePresentation.setIcon(Icons.Ollama);
                templatePresentation.setText(application.getService(OllamaSettings.class)
                        .getState()
                        .getModel());
                break;
            default:
                break;
        }
    }

    private String getLlamaCppPresentationText() {
        var huggingFaceModel = LlamaSettings.getCurrentState().getHuggingFaceModel();
        var llamaModel = LlamaModel.findByHuggingFaceModel(huggingFaceModel);
        return String.format("%s (%dB)",
                llamaModel.getLabel(),
                huggingFaceModel.getParameterSize());
    }


    private AnAction createModelAction(
            ServiceType serviceType,
            String label,
            Icon icon,
            Presentation comboBoxPresentation,
            Runnable onModelChanged) {
        return new DumbAwareAction(label, "", icon) {
            @Override
            public void update(@NotNull AnActionEvent event) {
                var presentation = event.getPresentation();
                presentation.setEnabled(!presentation.getText().equals(comboBoxPresentation.getText()));
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (onModelChanged != null) {
                    onModelChanged.run();
                }
                handleModelChange(serviceType);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };
    }

    private void handleModelChange(ServiceType serviceType) {
        updateTemplatePresentation(serviceType);
        onModelChange.accept(serviceType);
    }

    private AnAction createOllamaModelAction(String model, Presentation comboBoxPresentation) {
        return createModelAction(OLLAMA, model, Icons.Ollama, comboBoxPresentation,
                () -> {
                    var application = ApplicationManager.getApplication();
                    application
                            .getService(OllamaSettings.class)
                            .getState()
                            .setModel(model);
                    application
                            .getService(ModelSettings.class)
                            .setModel(featureType, model, OLLAMA);
                });
    }

    private AnAction createCustomOpenAIModelAction(
            CustomServiceSettingsState model,
            Presentation comboBoxPresentation) {
        return createModelAction(
                CUSTOM_OPENAI,
                model.getName(),
                Icons.OpenAI,
                comboBoxPresentation,
                () -> ApplicationManager.getApplication().getService(ModelSettings.class)
                        .setModel(featureType, model.getId(), CUSTOM_OPENAI));
    }

    private AnAction createLlamaModelAction(Presentation comboBoxPresentation) {
        return createModelAction(
                LLAMA_CPP,
                getLlamaCppPresentationText(),
                Icons.Llama,
                comboBoxPresentation,
                () -> ApplicationManager.getApplication().getService(ModelSettings.class)
                        .setModel(featureType,
                                LlamaSettings.getCurrentState().getHuggingFaceModel().getCode(), LLAMA_CPP));
    }
}
