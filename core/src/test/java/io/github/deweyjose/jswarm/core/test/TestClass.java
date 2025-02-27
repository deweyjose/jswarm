package io.github.deweyjose.jswarm.core.test;

import io.github.deweyjose.jswarm.core.annotations.LLMCoordinator;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;
import java.util.List;
import java.util.Map;

@LLMCoordinator(
    description = "Use me for coordinating the conversation across various test agents.",
    instructions = "Always be nice.")
public class TestClass {

  public String methodWithContextParam(LLMFunctionContext context) {
    return "test";
  }

  public String methodWithoutContextParam() {
    return "test";
  }

  public String methodWithStringParam(String param) {
    return "test";
  }

  public String methodWithListParam(List<String> param) {
    return "test";
  }

  public String methodWithMapParam(Map<String, Integer> param) {
    return "test";
  }

  public String methodWithIntegerParam(Integer param) {
    return "test";
  }

  public String methodWithMultipleParamsIncludingContext(LLMFunctionContext context, String param) {
    return "test";
  }

  public String methodWithMultipleParamsIncludingContextWrongOrder(
      String param, LLMFunctionContext context) {
    return "test";
  }
}
