package io.github.deweyjose.jswarm.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessage;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionMessageToolCall;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionTool;
import com.openai.models.ChatCompletionToolMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;
import io.github.deweyjose.jswarm.core.model.LLMAgentWrapper;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;
import io.github.deweyjose.jswarm.core.model.LLMResponse;
import io.github.deweyjose.jswarm.core.model.LLMToolResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LLMNexus {

  private final LLMAgentRegistry functionRegistry;
  private final OpenAIClient openAIClient;

  public LLMNexus(String apiKey, String functionRegistryPackage) {
    this.openAIClient = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
    this.functionRegistry = new LLMAgentRegistry(functionRegistryPackage);
  }

  public LLMNexus() {
    this.openAIClient = OpenAIOkHttpClient.fromEnv();
    this.functionRegistry = new LLMAgentRegistry();
  }

  public LLMNexus(OpenAIClient openAIClient, LLMAgentRegistry functionRegistry) {
    this.openAIClient = openAIClient;
    this.functionRegistry = functionRegistry;
  }

  public LLMNexus(OpenAIClient openAIClient, String functionRegistryPackage) {
    this.openAIClient = openAIClient;
    this.functionRegistry = new LLMAgentRegistry(functionRegistryPackage);
  }

  @SneakyThrows
  private ChatCompletion getChatCompletion(
      LLMAgentWrapper agent, List<ChatCompletionMessageParam> history) {

    List<ChatCompletionMessageParam> messages = new ArrayList<>();
    messages.add(
        ChatCompletionMessageParam.ofSystem(
            ChatCompletionSystemMessageParam.builder().content(agent.getInstructions()).build()));

    log.debug("Adding system message: {}", agent.getInstructions());

    messages.addAll(history);

    var paramsBuilder =
        ChatCompletionCreateParams.builder()
            .model(agent.getModel())
            .addSystemMessage(agent.getInstructions())
            .messages(messages);

    for (var entry : functionRegistry.getFunctions(agent).entrySet()) {
      var name = entry.getKey();
      var function = entry.getValue();
      paramsBuilder.addTool(
          ChatCompletionTool.builder().function(function.getFunctionDefinition()).build());
      log.debug(
          "Adding function {}, {} to chat completion",
          name,
          function.getFunctionDefinition().description().get());
    }

    log.debug("History:");
    for (var message : history) {
      log.debug("  {}", message);
    }

    return chatCompletion(paramsBuilder.build());
  }

  @SneakyThrows
  ChatCompletion chatCompletion(ChatCompletionCreateParams params) {
    var completion = openAIClient.chat().completions().create(params);
    log.debug("Chat completion: {}", completion);
    log.debug("Chat completion json: {}", (new ObjectMapper()).writeValueAsString(completion));
    return completion;
  }

  @SneakyThrows
  private LLMToolResponse handleToolCall(
      ChatCompletionMessageToolCall toolCall,
      LLMAgentWrapper agent,
      LLMFunctionContext functionContext) {
    var function = toolCall.function();
    var functionName = function.name();
    var functionArgs = function.arguments();
    log.debug("Function name: {}", functionName);
    log.debug("Function arguments: {}", functionArgs);
    log.debug("Tool call id: {}", toolCall.id());

    var functionWrapper = functionRegistry.getFunction(functionName, agent);

    if (functionWrapper == null) {
      log.error(
          "No global function or instance function {} not found for agent {}",
          functionName,
          agent.getName());
      return LLMToolResponse.builder()
          .message(
              ChatCompletionMessage.builder()
                  .role(JsonValue.from("tool"))
                  .content("Function not found: " + functionName)
                  .toolCalls(List.of(toolCall))
                  .refusal("Function not found: " + functionName) // TODO: add refusal message
                  .build())
          .build();
    }

    Object result = functionWrapper.invoke(functionContext, functionArgs);

    var responseBuilder = LLMToolResponse.builder();
    var completionMessageBuilder =
        ChatCompletionMessage.builder()
            .role(JsonValue.from("tool"))
            .toolCalls(
                List.of(
                    ChatCompletionMessageToolCall.builder()
                        .id(toolCall.id())
                        .function(function)
                        .build()))
            .refusal(Optional.empty());

    if (result instanceof LLMAgentWrapper) {
      completionMessageBuilder.content(
          String.format(
              "Transferred to agent %s. Adopt this persona immediately.",
              ((LLMAgentWrapper) result).getName()));
      responseBuilder.agent((LLMAgentWrapper) result);
    } else {
      completionMessageBuilder.content(result.toString());
    }

    return responseBuilder.message(completionMessageBuilder.build()).build();
  }

  public LLMResponse run(
      String prompt, List<ChatCompletionMessageParam> history, Map<String, Object> context) {

    history.add(
        ChatCompletionMessageParam.ofUser(
            ChatCompletionUserMessageParam.builder().content(prompt).build()));

    ChatCompletionMessage message;
    boolean hasToolCalls;
    // we start off with the coordinator
    LLMAgentWrapper agent = functionRegistry.getCoordinatorAgent();

    do {
      var completion = getChatCompletion(agent, history);
      message = completion.choices().get(0).message();

      history.add(ChatCompletionMessageParam.ofAssistant(message.toParam()));

      // we only loop if there are tool calls
      hasToolCalls = message.toolCalls().map(toolCalls -> !toolCalls.isEmpty()).orElse(false);

      if (hasToolCalls) {
        for (var toolCall : message.toolCalls().get()) {
          var toolResponse =
              handleToolCall(
                  toolCall,
                  agent,
                  LLMFunctionContext.builder().history(history).developerContext(context).build());

          history.add(
              ChatCompletionMessageParam.ofTool(
                  ChatCompletionToolMessageParam.builder()
                      .toolCallId(toolResponse.getMessage().toolCalls().get().get(0).id())
                      .content(toolResponse.getMessage().content().get())
                      .build()));

          // if the tool response is an agent, we switch to that agent
          // we then let the new agent handle conversation which might contain more tool calls
          agent = Optional.ofNullable(toolResponse.getAgent()).orElse(agent);
        }
      }
    } while (hasToolCalls);

    log.debug("User: {}", prompt);
    log.debug("Assistant: {}", message.content().get());

    return LLMResponse.builder().history(history).reply(message).contextVariables(context).build();
  }
}
