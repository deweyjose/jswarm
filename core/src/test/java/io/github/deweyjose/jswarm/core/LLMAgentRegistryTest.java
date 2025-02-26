package io.github.deweyjose.jswarm.core;

import static org.junit.jupiter.api.Assertions.*;

import io.github.deweyjose.jswarm.core.model.LLMAgent;
import io.github.deweyjose.jswarm.core.test.TestClass;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class LLMAgentRegistryTest {

  private LLMAgentRegistry llmAgentRegistry;

  public LLMAgentRegistryTest() {
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
    LLMAgent testClass = llmAgentRegistry.getCoordinatorAgent();
    assertNotNull(testClass);
    assertEquals("test instructions", testClass.getInstructions());
    assertEquals("use me for test description", testClass.getDescription());
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

    assertEquals(List.of("use me for hello world", "use me for test description"), descriptions);
    log.info("Functions: {}", functions.keySet());
  }

  @Test
  @SneakyThrows
  void getFunction() {
    var functions = llmAgentRegistry.getFunctions(llmAgentRegistry.getCoordinatorAgent());
    var functionName =
        functions.keySet().stream()
            .filter(f -> f.contains("TestClass_getAgent"))
            .findFirst()
            .orElseThrow();

    var function =
        llmAgentRegistry.getFunction(functionName, llmAgentRegistry.getCoordinatorAgent());

    TestClass agent = (TestClass) function.getMethod().invoke(function.getInstance());
    assertEquals("test", agent.methodWithStringParam("a"));
  }
}
