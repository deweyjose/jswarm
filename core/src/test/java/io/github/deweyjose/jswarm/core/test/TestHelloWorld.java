package io.github.deweyjose.jswarm.core.test;

import io.github.deweyjose.jswarm.core.annotations.LLMFunction;
import io.github.deweyjose.jswarm.core.model.LLMAgent;

public class TestHelloWorld extends LLMAgent {
  public TestHelloWorld() {
    super("gpt-4o-mini", "test hello world", "use me for hello world");
  }

  @LLMFunction(description = "Returns a hello world message")
  public String helloWorld() {
    return "Hello, World!";
  }
}
