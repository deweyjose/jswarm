package io.github.deweyjose.jswarm.core.test;

import io.github.deweyjose.jswarm.core.annotations.LLMCoordinator;
import io.github.deweyjose.jswarm.core.model.LLMAgent;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;
import java.util.List;
import java.util.Map;

@LLMCoordinator
public class TestClass extends LLMAgent {

  public TestClass() {
    super("gpt-4o-mini", "test instructions", "use me for test description");
  }

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
