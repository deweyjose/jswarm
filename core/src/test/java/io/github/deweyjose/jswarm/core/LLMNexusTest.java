package io.github.deweyjose.jswarm.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionMessageParam;
import io.github.deweyjose.jswarm.core.model.LLMResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Slf4j
class LLMNexusTest {

  private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @SneakyThrows
  void runBasic() {
    OpenAIClient client = mock(OpenAIClient.class);
    Mockito.mockStatic(OpenAIOkHttpClient.class);
    OpenAIOkHttpClient.Builder mockBuilder = Mockito.mock(OpenAIOkHttpClient.Builder.class);

    Mockito.when(OpenAIOkHttpClient.builder()).thenReturn(mockBuilder);
    Mockito.when(mockBuilder.apiKey(Mockito.anyString())).thenReturn(mockBuilder);
    Mockito.when(mockBuilder.build()).thenReturn(client);

    String json =
        "{\"id\":\"chatcmpl-B4e4liK9XUX30A3V4FzXBTjdp0VhG\","
            + "\"choices\":[{\"finish_reason\":\"stop\",\"index\":0,\"logprobs\":null,\"message\":{\"content\":\"Hello! How can I assist you today?\",\"refusal\":null,\"role\":\"assistant\"}}],"
            + "\"created\":1740447063,\"model\":\"gpt-4o-2024-08-06\",\"object\":\"chat.completion\",\"service_tier\":\"default\",\"system_fingerprint\":\"fp_f9f4fb6dbf\","
            + "\"usage\":{\"completion_tokens\":11,\"prompt_tokens\":343,\"total_tokens\":354,\"completion_tokens_details\":{\"accepted_prediction_tokens\":0,\"audio_tokens\":0,\"reasoning_tokens\":0,"
            + "\"rejected_prediction_tokens\":0},\"prompt_tokens_details\":{\"audio_tokens\":0,\"cached_tokens\":0}}}";

    System.setProperty("AGENT_PACKAGE", "io.github.deweyjose.jswarm.core.test");
    LLMNexus spyLLMNexus =
        Mockito.spy(new LLMNexus("test", "io.github.deweyjose.jswarm.core.test"));
    Mockito.doReturn(objectMapper.readValue(json, ChatCompletion.class))
        .when(spyLLMNexus)
        .chatCompletion(Mockito.any());

    List<ChatCompletionMessageParam> history = new ArrayList<>();
    LLMResponse response = spyLLMNexus.run("test it", history, Map.of());
    assertEquals("Hello! How can I assist you today?", response.getReply()._content().toString());
  }

  @Test
  @SneakyThrows
  void runToolCalls() {
    OpenAIClient client = mock(OpenAIClient.class);
    System.setProperty("AGENT_PACKAGE", "io.github.deweyjose.jswarm.core.test");
    LLMAgentRegistry registry = new LLMAgentRegistry();

    System.setProperty("AGENT_PACKAGE", "io.github.deweyjose.jswarm.core.test");
    LLMNexus spyLLMNexus = Mockito.spy(new LLMNexus(client, registry));

    var functions = registry.getFunctions(registry.getCoordinatorAgent());

    var functionName =
        functions.keySet().stream()
            .filter(p -> p.endsWith("_TestMemeGenerator_getAgent"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No function found"));

    String toolCallJson =
        "{\"id\":\"chatcmpl-B4eDIn9tt6c2KtuPP8bzRa7wltNiW\","
            + "\"choices\":[{\"finish_reason\":\"tool_calls\",\"index\":0,\"logprobs\":null,\"message\":{\"content\":null,\"refusal\":null,\"role\":\"assistant\","
            + "\"tool_calls\":[{\"id\":\"call_HoyIRiaC1NOaXBg2JJe7VEic\",\"function\":{\"arguments\":\"{}\",\"name\":\"HELLO_WORLD_AGENT_FUNCTION_NAME\"},\"type\":\"function\"}]}}],"
            + "\"created\":1740447592,\"model\":\"gpt-4o-2024-08-06\",\"object\":\"chat.completion\",\"service_tier\":\"default\",\"system_fingerprint\":\"fp_f9f4fb6dbf\","
            + "\"usage\":{\"completion_tokens\":18,\"prompt_tokens\":347,\"total_tokens\":365,\"completion_tokens_details\":{\"accepted_prediction_tokens\":0,\"audio_tokens\":0,\"reasoning_tokens\":0,"
            + "\"rejected_prediction_tokens\":0},\"prompt_tokens_details\":{\"audio_tokens\":0,\"cached_tokens\":0}}}";

    toolCallJson = toolCallJson.replace("HELLO_WORLD_AGENT_FUNCTION_NAME", functionName);

    String toolCallResponseJson =
        "{\"id\":\"chatcmpl-B4fH4iChM1B41UwzPjNRdHigAT2Ai\","
            + "\"choices\":[{\"finish_reason\":\"stop\",\"index\":0,\"logprobs\":null,"
            + "\"message\":{\"content\":\"ok\",\"refusal\":null,\"role\":\"assistant\"}}],"
            + "\"created\":1740451670,\"model\":\"gpt-4o-2024-08-06\",\"object\":\"chat.completion\",\"service_tier\":\"default\",\"system_fingerprint\":\"fp_f9f4fb6dbf\","
            + "\"usage\":{\"completion_tokens\":34,\"prompt_tokens\":407,\"total_tokens\":441,\"completion_tokens_details\":{\"accepted_prediction_tokens\":0,\"audio_tokens\":0,"
            + "\"reasoning_tokens\":0,\"rejected_prediction_tokens\":0},\"prompt_tokens_details\":{\"audio_tokens\":0,\"cached_tokens\":0}}}";

    Mockito.doReturn(
            objectMapper.readValue(toolCallJson, ChatCompletion.class),
            objectMapper.readValue(toolCallResponseJson, ChatCompletion.class))
        .when(spyLLMNexus)
        .chatCompletion(Mockito.any());

    List<ChatCompletionMessageParam> history = new ArrayList<>();
    LLMResponse response = spyLLMNexus.run("test it", history, Map.of());
    assertEquals("ok", response.getReply()._content().toString());
  }

  @Test
  @SneakyThrows
  void toolCalls() {
    OpenAIClient client = mock(OpenAIClient.class);
    System.setProperty("AGENT_PACKAGE", "io.github.deweyjose.jswarm.core.test");
    LLMAgentRegistry registry = new LLMAgentRegistry();

    System.setProperty("AGENT_PACKAGE", "io.github.deweyjose.jswarm.core.test");
    LLMNexus spyLLMNexus = Mockito.spy(new LLMNexus(client, registry));

    var functions = registry.getFunctions(registry.getCoordinatorAgent());

    var functionName =
        functions.keySet().stream()
            .filter(p -> p.endsWith("_TestMemeGenerator_getAgent"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No function found"));

    String toolCallJson =
        "{\"id\":\"chatcmpl-B4eDIn9tt6c2KtuPP8bzRa7wltNiW\","
            + "\"choices\":[{\"finish_reason\":\"tool_calls\",\"index\":0,\"logprobs\":null,\"message\":{\"content\":null,\"refusal\":null,\"role\":\"assistant\","
            + "\"tool_calls\":[{\"id\":\"call_HoyIRiaC1NOaXBg2JJe7VEic\",\"function\":{\"arguments\":\"{}\",\"name\":\"HELLO_WORLD_AGENT_FUNCTION_NAME\"},\"type\":\"function\"}]}}],"
            + "\"created\":1740447592,\"model\":\"gpt-4o-2024-08-06\",\"object\":\"chat.completion\",\"service_tier\":\"default\",\"system_fingerprint\":\"fp_f9f4fb6dbf\","
            + "\"usage\":{\"completion_tokens\":18,\"prompt_tokens\":347,\"total_tokens\":365,\"completion_tokens_details\":{\"accepted_prediction_tokens\":0,\"audio_tokens\":0,\"reasoning_tokens\":0,"
            + "\"rejected_prediction_tokens\":0},\"prompt_tokens_details\":{\"audio_tokens\":0,\"cached_tokens\":0}}}";

    String toolCallResponseJson =
        "{\"id\":\"chatcmpl-B4fH4iChM1B41UwzPjNRdHigAT2Ai\","
            + "\"choices\":[{\"finish_reason\":\"stop\",\"index\":0,\"logprobs\":null,"
            + "\"message\":{\"content\":\"missing function, not recover\",\"refusal\":null,\"role\":\"assistant\"}}],"
            + "\"created\":1740451670,\"model\":\"gpt-4o-2024-08-06\",\"object\":\"chat.completion\",\"service_tier\":\"default\",\"system_fingerprint\":\"fp_f9f4fb6dbf\","
            + "\"usage\":{\"completion_tokens\":34,\"prompt_tokens\":407,\"total_tokens\":441,\"completion_tokens_details\":{\"accepted_prediction_tokens\":0,\"audio_tokens\":0,"
            + "\"reasoning_tokens\":0,\"rejected_prediction_tokens\":0},\"prompt_tokens_details\":{\"audio_tokens\":0,\"cached_tokens\":0}}}";

    Mockito.doReturn(
            objectMapper.readValue(toolCallJson, ChatCompletion.class),
            objectMapper.readValue(toolCallResponseJson, ChatCompletion.class))
        .when(spyLLMNexus)
        .chatCompletion(Mockito.any());

    List<ChatCompletionMessageParam> history = new ArrayList<>();
    LLMResponse response = spyLLMNexus.run("test it", history, Map.of());
    assertEquals("missing function, not recover", response.getReply()._content().toString());
  }
}
