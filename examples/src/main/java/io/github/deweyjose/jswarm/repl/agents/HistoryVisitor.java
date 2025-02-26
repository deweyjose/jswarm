package io.github.deweyjose.jswarm.repl.agents;

import com.openai.models.ChatCompletionAssistantMessageParam;
import com.openai.models.ChatCompletionDeveloperMessageParam;
import com.openai.models.ChatCompletionFunctionMessageParam;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionToolMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;
import org.jetbrains.annotations.NotNull;

public class HistoryVisitor implements ChatCompletionMessageParam.Visitor {
  @Override
  public Object visitUser(@NotNull ChatCompletionUserMessageParam chatCompletionUserMessageParam) {
    return "  👱🏻‍: " + chatCompletionUserMessageParam._content();
  }

  @Override
  public Object visitTool(@NotNull ChatCompletionToolMessageParam chatCompletionToolMessageParam) {
    return "  🛠: " + chatCompletionToolMessageParam._content();
  }

  @Override
  public Object visitSystem(
      @NotNull ChatCompletionSystemMessageParam chatCompletionSystemMessageParam) {
    return " 💻: " + chatCompletionSystemMessageParam._content();
  }

  @Override
  public Object visitFunction(
      @NotNull ChatCompletionFunctionMessageParam chatCompletionFunctionMessageParam) {
    return "  👨🏻‍💻: " + chatCompletionFunctionMessageParam._content();
  }

  @Override
  public Object visitDeveloper(
      @NotNull ChatCompletionDeveloperMessageParam chatCompletionDeveloperMessageParam) {
    return "  👨🏻‍💻: " + chatCompletionDeveloperMessageParam._content();
  }

  @Override
  public Object visitAssistant(
      @NotNull ChatCompletionAssistantMessageParam chatCompletionAssistantMessageParam) {
    if (chatCompletionAssistantMessageParam
        .toolCalls()
        .map(toolCalls -> !toolCalls.isEmpty())
        .orElse(false)) {
      return "  🤖: " + chatCompletionAssistantMessageParam._toolCalls();
    } else {
      return "  🤖: " + chatCompletionAssistantMessageParam._content();
    }
  }
}
