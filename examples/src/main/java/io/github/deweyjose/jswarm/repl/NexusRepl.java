package io.github.deweyjose.jswarm.repl;

import com.openai.models.*;
import io.github.deweyjose.jswarm.core.LLMNexus;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NexusRepl {
  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    System.setProperty("AGENT_PACKAGE", "io.github.deweyjose.jswarm.repl.agents");
    var nexus = new LLMNexus();

    log.info("LLMNexus CLI 🐝");

    var history = new ArrayList<ChatCompletionMessageParam>();

    while (true) {
      System.out.print("👱🏻‍: ");
      String userInput = scanner.nextLine();
      var response = nexus.run(userInput, history, Map.of());
      var message = response.getReply();
      System.out.println("🤖: " + message._content());
    }
  }
}
