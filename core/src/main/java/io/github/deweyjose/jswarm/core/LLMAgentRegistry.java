package io.github.deweyjose.jswarm.core;

import static io.github.deweyjose.jswarm.core.LLMFunctionWrapper.*;
import static io.github.deweyjose.jswarm.core.LLMFunctionWrapper.computeFunctionParameters;

import com.openai.models.FunctionDefinition;
import io.github.deweyjose.jswarm.core.annotations.LLMCoordinator;
import io.github.deweyjose.jswarm.core.annotations.LLMFunction;
import io.github.deweyjose.jswarm.core.model.LLMAgent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

@Slf4j
public class LLMAgentRegistry {

  private Map<Object, Map<String, LLMFunctionWrapper>> registeredInstanceFunctions =
      new HashMap<>();
  private Map<String, LLMFunctionWrapper> registeredGlobalFunctions = new HashMap<>();

  private LLMAgent coordinatorAgent;

  public LLMAgentRegistry() {
    this(System.getProperty("AGENT_PACKAGE", System.getenv().get("AGENT_PACKAGE")));
  }

  public LLMAgentRegistry(String agentPackage) {
    if (agentPackage == null || agentPackage.isEmpty()) {
      throw new IllegalArgumentException("Agent package cannot be null or empty");
    }
    initialize(agentPackage);
  }

  public static String functionName(int hashCode, String className, String methodName) {
    return String.format("%s_%s_%s", hashCode, className, methodName);
  }

  public LLMAgent getCoordinatorAgent() {
    return coordinatorAgent;
  }

  private void initialize(String agentPackage) {
    Reflections reflections = new Reflections(agentPackage);
    Set<Class<? extends LLMAgent>> annotated =
        reflections.getSubTypesOf(LLMAgent.class).stream()
            .filter(p -> p.isAnnotationPresent(LLMCoordinator.class))
            .collect(Collectors.toSet());

    // make sure we have a coordinator agent
    if (annotated.isEmpty()) {
      throw new IllegalStateException("No LLMCoordinator annotated classes found");
    } else if (annotated.size() > 1) {
      throw new IllegalStateException("Multiple LLMCoordinator annotated classes found");
    } else {
      try {
        coordinatorAgent = annotated.iterator().next().getDeclaredConstructor().newInstance();
        registerAgent(coordinatorAgent);
      } catch (Exception e) {
        throw new IllegalStateException("Error instantiating coordinator agent", e);
      }
    }

    // load all agents
    reflections.getSubTypesOf(LLMAgent.class).stream()
        .filter(p -> !p.isAnnotationPresent(LLMCoordinator.class))
        .forEach(
            clazz -> {
              try {
                LLMAgent agent = clazz.getDeclaredConstructor().newInstance();
                registerAgent(agent);
              } catch (Exception e) {
                log.error("Error Loading agent class: {}", clazz.getSimpleName(), e);
                throw new RuntimeException(e);
              }
            });
  }

  private void registerAgent(LLMAgent agent) {
    Method[] methods = agent.getClass().getDeclaredMethods();
    for (Method method : methods) {
      LLMFunction methodAnnotation = method.getAnnotation(LLMFunction.class);
      if (methodAnnotation != null) {
        if (Modifier.isStatic(method.getModifiers())) {
          registerGlobalFunction(methodAnnotation.description(), method, null);
        } else if (methodAnnotation.global()) {
          registerGlobalFunction(methodAnnotation.description(), method, agent);
        } else {
          registerInstanceFunction(methodAnnotation.description(), method, agent);
        }
      }
    }

    Method agentTransfer =
        Arrays.stream(agent.getClass().getSuperclass().getDeclaredMethods())
            .filter(m -> m.getName().equals("getAgent"))
            .findFirst()
            .orElse(null);

    registerGlobalFunction(agent.getDescription(), agentTransfer, agent);

    log.info("Loaded Agent: {}", agent.getName());
  }

  public void registerInstanceFunction(String description, Method method, LLMAgent agent) {
    String name =
        functionName(agent.hashCode(), agent.getClass().getSimpleName(), method.getName());

    log.debug("Registering instance LLMFunction {}", name);

    registeredInstanceFunctions
        .computeIfAbsent(agent, k -> new HashMap<>())
        .put(
            name,
            builder()
                .instance(agent)
                .method(method)
                .functionDefinition(
                    FunctionDefinition.builder()
                        .name(name)
                        .description(description)
                        .strict(true)
                        .parameters(computeFunctionParameters(method))
                        .build())
                .hasContextParam(hasLLMFunctionContextParam(method))
                .build());
  }

  public void registerGlobalFunction(String description, Method method, LLMAgent agent) {
    // can be static ...
    final String name =
        agent == null
            ? functionName(
                method.hashCode(), method.getDeclaringClass().getSimpleName(), method.getName())
            : functionName(agent.hashCode(), agent.getClass().getSimpleName(), method.getName());

    log.debug("Registering global LLMFunction {}", name);

    if (registeredGlobalFunctions.containsKey(name)) {
      throw new IllegalArgumentException("Global function already registered for " + name);
    }

    registeredGlobalFunctions.put(
        name,
        builder()
            .instance(agent)
            .method(method)
            .functionDefinition(
                FunctionDefinition.builder()
                    .name(name)
                    .description(description)
                    .strict(true)
                    .parameters(computeFunctionParameters(method))
                    .build())
            .hasContextParam(hasLLMFunctionContextParam(method))
            .build());
  }

  public Map<String, LLMFunctionWrapper> getFunctions(LLMAgent agent) {
    Map<String, LLMFunctionWrapper> allFunctions = new HashMap<>(registeredGlobalFunctions);
    if (registeredInstanceFunctions.containsKey(agent)) {
      allFunctions.putAll(registeredInstanceFunctions.get(agent));
    }
    return Collections.unmodifiableMap(allFunctions);
  }

  public LLMFunctionWrapper getFunction(String name, LLMAgent agent) {
    LLMFunctionWrapper function = registeredGlobalFunctions.get(name);
    if (function == null) {
      Map<String, LLMFunctionWrapper> functions = registeredInstanceFunctions.get(agent);
      if (functions != null) {
        function = functions.get(name);
      }
    }
    return function;
  }
}
