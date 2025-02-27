package io.github.deweyjose.jswarm.core.model;

import com.openai.models.ChatCompletionMessage;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LLMToolResponse {
  private final ChatCompletionMessage message;
  private final LLMAgentWrapper agent;
}
