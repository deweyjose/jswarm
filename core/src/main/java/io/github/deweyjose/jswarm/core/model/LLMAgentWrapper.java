package io.github.deweyjose.jswarm.core.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class LLMAgentWrapper {
  private final String model;
  private final String instructions;
  private final String description;
  private final Object agent;

  public LLMAgentWrapper(String model, String instructions, String description, Object agent) {
    this.model = model;
    this.instructions = instructions;
    this.description = description;
    this.agent = agent;
  }

  public String getName() {
    return agent.getClass().getSimpleName();
  }

  public Object getAgent() {
    return agent;
  }

  public LLMAgentWrapper getWrapper() {
    return this;
  }
}
