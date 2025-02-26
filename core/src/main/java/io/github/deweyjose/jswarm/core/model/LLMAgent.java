package io.github.deweyjose.jswarm.core.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LLMAgent {
  private final String model;
  private final String instructions;
  private final String description;

  public LLMAgent(String model, String instructions, String description) {
    this.model = model;
    this.instructions = instructions;
    this.description = description;
  }

  public String getName() {
    return getClass().getSimpleName();
  }

  public LLMAgent getAgent() {
    return this;
  }
}
