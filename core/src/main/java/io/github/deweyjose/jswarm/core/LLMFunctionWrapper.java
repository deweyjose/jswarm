package io.github.deweyjose.jswarm.core;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Getter
@Builder
@Slf4j
public class LLMFunctionWrapper {

  private static final ObjectMapper mapper = new ObjectMapper();

  private final Object instance;
  private final Method method;
  private final boolean hasContextParam;
  private final FunctionDefinition functionDefinition;

  public static boolean hasLLMFunctionContextParam(Method method) {
    Class<?>[] params = method.getParameterTypes();

    // If there are no parameters, it's valid.
    if (params.length == 0) {
      return false;
    }

    // Check if the first parameter is Foo.
    boolean isFirst = params[0].equals(LLMFunctionContext.class);
    long count =
        Arrays.stream(params).filter(param -> param.equals(LLMFunctionContext.class)).count();

    if (count == 0) {
      return false;
    } else if (count == 1 && isFirst) {
      return true;
    } else {
      throw new IllegalArgumentException(
          "Method can have a single parameter of type LLMFunctionContext, and it must be the first parameter");
    }
  }

  public static FunctionParameters computeFunctionParameters(Method method) {
    var schema = LLMFunctionSchemaGenerator.generateSchema(method);

    log.debug("Generated schema for function {}: {}", method.getName(), schema.toPrettyString());

    return FunctionParameters.builder()
        .putAdditionalProperty("type", JsonValue.from(schema.get("type")))
        .putAdditionalProperty("properties", JsonValue.from(schema.get("properties")))
        .putAdditionalProperty("required", JsonValue.from(schema.get("required")))
        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
        .build();
  }

  @SneakyThrows
  public static Object[] mapArguments(
      Method method, String argumentsJson, LLMFunctionContext context) {

    JsonNode jsonNode = mapper.readTree(argumentsJson);
    Parameter[] parameters = method.getParameters();
    Object[] args = new Object[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      if (parameter.getType().equals(LLMFunctionContext.class)) {
        args[i] = context;
      } else {
        String key = "arg" + i;
        JsonNode valueNode = jsonNode.get(key);
        if (valueNode == null) {
          throw new IllegalArgumentException("Missing argument for parameter: " + key);
        }
        // Build a JavaType from the parameter's generic type to support nested collections.
        JavaType javaType = mapper.getTypeFactory().constructType(parameter.getParameterizedType());
        Object value = mapper.convertValue(valueNode, javaType);
        args[i] = value;
      }
    }
    return args;
  }

  @SuppressWarnings("unchecked")
  public <R> R invoke(LLMFunctionContext functionContext, String arguments) throws Exception {
    var mappedArgs = mapArguments(method, arguments, functionContext);
    return (R) method.invoke(instance, mappedArgs);
  }
}
