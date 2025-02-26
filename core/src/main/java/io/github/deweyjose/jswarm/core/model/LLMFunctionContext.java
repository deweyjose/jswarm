package io.github.deweyjose.jswarm.core.model;

import com.openai.models.ChatCompletionMessageParam;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LLMFunctionContext {
  private final List<ChatCompletionMessageParam> history;
  private final Map<String, Object> developerContext;
}
