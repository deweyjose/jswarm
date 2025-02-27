package io.github.deweyjose.jswarm.core;

import static io.github.deweyjose.jswarm.core.LLMFunctionWrapper.*;
import static io.github.deweyjose.jswarm.core.LLMFunctionWrapper.computeFunctionParameters;

import com.openai.models.FunctionDefinition;
import io.github.deweyjose.jswarm.core.annotations.LLMAgent;
import io.github.deweyjose.jswarm.core.annotations.LLMCoordinator;
import io.github.deweyjose.jswarm.core.annotations.LLMFunction;
import io.github.deweyjose.jswarm.core.model.LLMAgentWrapper;
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

  private LLMAgentWrapper coordinatorAgent;

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

  public LLMAgentWrapper getCoordinatorAgent() {
    return coordinatorAgent;
  }

  private void initialize(String agentPackage) {
    Reflections reflections = new Reflections(agentPackage);
    Set<Class<?>> annotated =
        reflections.getTypesAnnotatedWith(LLMCoordinator.class).stream()
            .collect(Collectors.toSet());

    // make sure we have a coordinator agent
    if (annotated.isEmpty()) {
      throw new IllegalStateException("No LLMCoordinator annotated classes found");
    } else if (annotated.size() > 1) {
      throw new IllegalStateException("Multiple LLMCoordinator annotated classes found");
    } else {
      try {
        Object instance = annotated.iterator().next().getDeclaredConstructor().newInstance();
        LLMCoordinator annotation = instance.getClass().getAnnotation(LLMCoordinator.class);
        coordinatorAgent =
            LLMAgentWrapper.builder()
                .agent(instance)
                .model(annotation.model())
                .description(annotation.description())
                .instructions(annotation.instructions())
                .build();
        registerAgent(coordinatorAgent);
      } catch (Exception e) {
        throw new IllegalStateException("Error instantiating coordinator agent", e);
      }
    }

    reflections
        .getTypesAnnotatedWith(LLMAgent.class)
        .forEach(
            clazz -> {
              try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                LLMAgent annotation = instance.getClass().getAnnotation(LLMAgent.class);
                LLMAgentWrapper wrapper =
                    LLMAgentWrapper.builder()
                        .agent(instance)
                        .model(annotation.model())
                        .description(annotation.description())
                        .instructions(annotation.instructions())
                        .build();
                registerAgent(wrapper);
              } catch (Exception e) {
                log.error("Error Loading agent class: {}", clazz.getSimpleName(), e);
                throw new RuntimeException(e);
              }
            });
  }

  private void registerAgent(LLMAgentWrapper wrapper) {
    Method[] methods = wrapper.getAgent().getClass().getDeclaredMethods();
    for (Method method : methods) {
      LLMFunction methodAnnotation = method.getAnnotation(LLMFunction.class);
      if (methodAnnotation != null) {
        if (Modifier.isStatic(method.getModifiers())) {
          registerGlobalFunction(
              methodAnnotation.description(),
              method,
              null,
              functionName(
                  method.hashCode(), method.getDeclaringClass().getSimpleName(), method.getName()));
        } else {
          var name =
              functionName(
                  wrapper.getAgent().hashCode(),
                  wrapper.getAgent().getClass().getSimpleName(),
                  method.getName());
          if (methodAnnotation.global()) {
            registerGlobalFunction(
                methodAnnotation.description(), method, wrapper.getAgent(), name);
          } else {
            registerInstanceFunction(
                methodAnnotation.description(), method, wrapper.getAgent(), name);
          }
        }
      }
    }

    Method agentTransfer =
        Arrays.stream(wrapper.getClass().getDeclaredMethods())
            .filter(m -> m.getName().equals("getWrapper"))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No getAgent method found in agent class: "
                            + wrapper.getClass().getSimpleName()));

    registerGlobalFunction(
        wrapper.getDescription(),
        agentTransfer,
        wrapper,
        functionName(
            wrapper.getAgent().hashCode(),
            wrapper.getAgent().getClass().getSimpleName(),
            "getAgent"));

    log.debug("Loaded Agent: {}", wrapper.getName());
  }

  private void registerInstanceFunction(
      String description, Method method, Object agent, String name) {
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

  private void registerGlobalFunction(
      String description, Method method, Object agent, String name) {
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

  public Map<String, LLMFunctionWrapper> getFunctions(LLMAgentWrapper wrapper) {
    Map<String, LLMFunctionWrapper> allFunctions = new HashMap<>(registeredGlobalFunctions);
    if (registeredInstanceFunctions.containsKey(wrapper.getAgent())) {
      allFunctions.putAll(registeredInstanceFunctions.get(wrapper.getAgent()));
    }
    return Collections.unmodifiableMap(allFunctions);
  }

  public LLMFunctionWrapper getFunction(String name, LLMAgentWrapper wrapper) {
    LLMFunctionWrapper function = registeredGlobalFunctions.get(name);
    if (function == null) {
      Map<String, LLMFunctionWrapper> functions =
          registeredInstanceFunctions.get(wrapper.getAgent());
      if (functions != null) {
        function = functions.get(name);
      }
    }
    return function;
  }
}
