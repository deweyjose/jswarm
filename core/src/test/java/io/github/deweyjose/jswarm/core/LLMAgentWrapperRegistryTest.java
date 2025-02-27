package io.github.deweyjose.jswarm.core;

import static org.junit.jupiter.api.Assertions.*;

import io.github.deweyjose.jswarm.core.model.LLMAgentWrapper;
import io.github.deweyjose.jswarm.core.test.TestClass;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class LLMAgentWrapperRegistryTest {

  private LLMAgentRegistry llmAgentRegistry;

  public LLMAgentWrapperRegistryTest() {
    System.setProperty("AGENT_PACKAGE", "io.github.deweyjose.jswarm.core.test");
    llmAgentRegistry = new LLMAgentRegistry();
  }

  @Test
  void functionName() {
    TestClass testClass = new TestClass();
    String functionName =
        llmAgentRegistry.functionName(testClass.hashCode(), "TestClass", "methodWithContextParam");
    assertEquals(testClass.hashCode() + "_TestClass_methodWithContextParam", functionName);
  }

  @Test
  void getCoordinatorAgent() {
    LLMAgentWrapper testClass = llmAgentRegistry.getCoordinatorAgent();
    assertNotNull(testClass);
    assertEquals("Always be nice.", testClass.getInstructions());
    assertEquals(
        "Use me for coordinating the conversation across various test agents.",
        testClass.getDescription());
  }

  @Test
  void getFunctions() {
    var functions = llmAgentRegistry.getFunctions(llmAgentRegistry.getCoordinatorAgent());
    assertNotNull(functions);
    assertFalse(functions.isEmpty());
    assertEquals(2, functions.size());
    var descriptions =
        functions.values().stream()
            .map(f -> f.getFunctionDefinition().description().get())
            .collect(Collectors.toList());

    Collections.sort(descriptions);

    assertEquals(
        List.of(
            "Use me for coordinating the conversation across various test agents.",
            "Use this when you need to do something with a meme."),
        descriptions);
    log.info("Functions: {}", functions.keySet());
  }

  @Test
  @SneakyThrows
  void getAgentFunction() {
    var functions = llmAgentRegistry.getFunctions(llmAgentRegistry.getCoordinatorAgent());

    assertEquals(2, functions.size());

    for (Map.Entry<String, LLMFunctionWrapper> entry : functions.entrySet()) {
      assertTrue(entry.getKey().endsWith("_getAgent"));
    }
  }
}
