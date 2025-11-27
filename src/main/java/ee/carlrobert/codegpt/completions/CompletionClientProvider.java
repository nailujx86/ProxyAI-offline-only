package ee.carlrobert.codegpt.completions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.net.ssl.CertificateManager;
import ee.carlrobert.codegpt.credentials.CredentialsStore.CredentialKey;
import ee.carlrobert.codegpt.settings.advanced.AdvancedSettings;
import ee.carlrobert.codegpt.settings.configuration.ConfigurationSettings;
import ee.carlrobert.codegpt.settings.service.llama.LlamaSettings;
import ee.carlrobert.codegpt.settings.service.ollama.OllamaSettings;
import ee.carlrobert.llm.client.llama.LlamaClient;
import ee.carlrobert.llm.client.ollama.OllamaClient;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import static ee.carlrobert.codegpt.credentials.CredentialsStore.getCredential;

public class CompletionClientProvider {

  public static LlamaClient getLlamaClient() {
    var llamaSettings = LlamaSettings.getCurrentState();
    return new LlamaClient.Builder()
            .setPort(llamaSettings.getServerPort())
            .build(getDefaultClientBuilder());
  }

  public static OllamaClient getOllamaClient() {
    var host = ApplicationManager.getApplication()
            .getService(OllamaSettings.class)
            .getState()
            .getHost();
    var builder = new OllamaClient.Builder()
            .setHost(host);

    String apiKey = getCredential(CredentialKey.OllamaApikey.INSTANCE);
    if (apiKey != null && !apiKey.isBlank()) {
      builder.setApiKey(apiKey);
    }
    return builder.build(getDefaultClientBuilder());
  }

  public static OkHttpClient.Builder getDefaultClientBuilder() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    CertificateManager certificateManager = CertificateManager.getInstance();
    X509TrustManager trustManager = certificateManager.getTrustManager();
    builder.sslSocketFactory(certificateManager.getSslContext().getSocketFactory(), trustManager);
    var advancedSettings = AdvancedSettings.getCurrentState();
    var proxyHost = advancedSettings.getProxyHost();
    var proxyPort = advancedSettings.getProxyPort();
    if (!proxyHost.isEmpty() && proxyPort != 0) {
      builder.proxy(
              new Proxy(advancedSettings.getProxyType(), new InetSocketAddress(proxyHost, proxyPort)));
      if (advancedSettings.isProxyAuthSelected()) {
        builder.proxyAuthenticator((route, response) ->
                response.request()
                        .newBuilder()
                        .header("Proxy-Authorization", Credentials.basic(
                                advancedSettings.getProxyUsername(),
                                advancedSettings.getProxyPassword()))
                        .build());
      }
    }

    if (ConfigurationSettings.getState().getDebugModeEnabled()) {
      var ideLogger = Logger.getInstance(CompletionClientProvider.class);
      var httpLogger = new HttpLoggingInterceptor(message -> ideLogger.info("[HTTP] " + message));
      httpLogger.setLevel(HttpLoggingInterceptor.Level.BODY);
      httpLogger.redactHeader("Authorization");
      httpLogger.redactHeader("X-API-Key");
      httpLogger.redactHeader("Api-Key");
      builder.addInterceptor(httpLogger);
    }

    return builder
            .connectTimeout(advancedSettings.getConnectTimeout(), TimeUnit.SECONDS)
            .readTimeout(advancedSettings.getReadTimeout(), TimeUnit.SECONDS);
  }
}
