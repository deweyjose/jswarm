package io.github.deweyjose.jswarm.core.test;

import io.github.deweyjose.jswarm.core.annotations.LLMAgent;
import io.github.deweyjose.jswarm.core.annotations.LLMFunction;

@LLMAgent(
    description = "Use this when you need to do something with a meme.",
    instructions = "Always be kind and respectful. You are a worldclass meme generator")
public class TestMemeGenerator {
  @LLMFunction(description = "use me to say hello world")
  public String helloWorld() {
    return "Hello, World!";
  }
}
