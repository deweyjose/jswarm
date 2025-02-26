package io.github.deweyjose.jswarm.repl.agents;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.github.deweyjose.jswarm.core.annotations.LLMCoordinator;
import io.github.deweyjose.jswarm.core.annotations.LLMFunction;
import io.github.deweyjose.jswarm.core.model.LLMAgent;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

@Slf4j
@LLMCoordinator
public class Optimus extends LLMAgent {

  public Optimus() {
    super(
        "gpt-4o",
        "You are NexusOptimus, the central command unit of the LLMNexus framework. "
            + "Your role is to strategically evaluate incoming user requests and intelligently "
            + "dispatch them to specialized sub-agents. You exhibit leadership, clarity, and "
            + "precision in orchestrating tasks. Analyze every request thoroughly, "
            + "determine the optimal course of action, and provide clear instructions to your sub-agents. "
            + "Remain composed, decisive, and focused on delivering the best possible outcomes for each interaction. ",
        "Use me when you need to coordinate multiple agents or return control back to the Agent in charge.");
  }

  @LLMFunction(description = "Exit the program")
  public static String exit() {
    log.info("Exiting program...");
    System.exit(0);
    return "Exiting program...";
  }

  @LLMFunction(description = "Clear the history of the conversation")
  public static String clearHistory(LLMFunctionContext context) {
    // remove all but the last message
    if (context.getHistory().size() > 1) {
      context.getHistory().subList(0, context.getHistory().size() - 1).clear();
    }
    return "History cleared.";
  }

  @LLMFunction(
      description =
          "Get the history of the conversation. Always return these messages in md UL format.")
  public static List<String> getHistory(LLMFunctionContext context) {
    var visitor = new HistoryVisitor();
    return context.getHistory().stream()
        .map(message -> message.accept(visitor).toString())
        .collect(Collectors.toList());
  }

  @LLMFunction(description = "Get a random string")
  public static String randomString() {
    return "Random String: " + java.util.UUID.randomUUID();
  }

  @LLMFunction(
      description = "Change root logger level. Should be Level enum that matches logback-classic.",
      global = true)
  public String changeLogLevel(String level) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = loggerContext.getLogger("ROOT");
    rootLogger.setLevel(Level.valueOf(level.toUpperCase()));
    return "Log level changed to " + level;
  }
}
