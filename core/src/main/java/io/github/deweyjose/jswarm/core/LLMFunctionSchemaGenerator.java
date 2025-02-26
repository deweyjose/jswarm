package io.github.deweyjose.jswarm.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.deweyjose.jswarm.core.annotations.LLMFunctionParam;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class LLMFunctionSchemaGenerator {

  private static final ObjectMapper mapper = new ObjectMapper();

  public static ObjectNode generateSchema(Method method) {
    ObjectNode schema = mapper.createObjectNode();
    schema.put("type", "object");

    ObjectNode properties = mapper.createObjectNode();
    ArrayNode required = mapper.createArrayNode();

    // Iterate over all parameters in the method
    for (Parameter parameter : method.getParameters()) {
      if (parameter.getType().equals(LLMFunctionContext.class)) {
        // Skip the LLMFunctionContext parameter, LLMs don't need to know this
        continue;
      }
      String paramName =
          parameter.getName(); // You may choose to override with a custom annotation for parameter
      // names.
      JsonNode propertySchema = generateSchemaForType(parameter.getParameterizedType());
      properties.set(paramName, propertySchema);
      // if the Parameter is annotated with LLMFunctionParam add an attribute named description to
      // properties
      if (parameter.isAnnotationPresent(LLMFunctionParam.class)) {
        LLMFunctionParam annotation = parameter.getAnnotation(LLMFunctionParam.class);
        // need to add this to the propertySchema
        ((ObjectNode) propertySchema).put("description", annotation.description());
      }

      required.add(paramName);
    }
    schema.set("properties", properties);
    schema.set("required", required);
    return schema;
  }

  private static JsonNode generateSchemaForType(Type type) {
    // If the type is a Class (i.e. non-parameterized)
    if (type instanceof Class<?>) {
      Class<?> cls = (Class<?>) type;
      if (cls.equals(String.class)) {
        return createTypeNode("string");
      } else if (cls.equals(Integer.class) || cls.equals(int.class)) {
        return createTypeNode("integer");
      } else if (cls.equals(Double.class)
          || cls.equals(double.class)
          || cls.equals(Float.class)
          || cls.equals(float.class)) {
        return createTypeNode("number");
      } else if (cls.equals(Boolean.class) || cls.equals(boolean.class)) {
        return createTypeNode("boolean");
      } else if (List.class.isAssignableFrom(cls)) {
        // Raw List, assume items are of any type (empty schema)
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "array");
        node.set("items", mapper.createObjectNode());
        return node;
      } else if (Map.class.isAssignableFrom(cls)) {
        // Raw Map, assume additionalProperties is an empty schema
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "object");
        node.set("additionalProperties", mapper.createObjectNode());
        return node;
      } else {
        // Fallback for any other object type – treat it as an object.
        return createTypeNode("object");
      }
    }
    // If the type is parameterized (e.g. List<String>, Map<String, Integer>, etc.)
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;
      Type rawType = pType.getRawType();
      if (rawType instanceof Class<?>) {
        Class<?> rawClass = (Class<?>) rawType;
        if (List.class.isAssignableFrom(rawClass)) {
          ObjectNode node = mapper.createObjectNode();
          node.put("type", "array");
          Type[] typeArgs = pType.getActualTypeArguments();
          // Expecting a single generic argument for List
          if (typeArgs.length == 1) {
            node.set("items", generateSchemaForType(typeArgs[0]));
          } else {
            node.set("items", mapper.createObjectNode());
          }
          return node;
        } else if (Map.class.isAssignableFrom(rawClass)) {
          ObjectNode node = mapper.createObjectNode();
          node.put("type", "object");
          Type[] typeArgs = pType.getActualTypeArguments();
          // For Maps, keys are assumed to be strings and values are defined by the second type
          // parameter.
          if (typeArgs.length == 2) {
            node.set("additionalProperties", generateSchemaForType(typeArgs[1]));
          } else {
            node.set("additionalProperties", mapper.createObjectNode());
          }
          return node;
        } else {
          // For any other parameterized type, treat it as an object.
          return createTypeNode("object");
        }
      }
    }
    // Fallback: return an empty object schema
    return mapper.createObjectNode();
  }

  /** Helper method to create a simple JSON Schema node for a primitive type. */
  private static ObjectNode createTypeNode(String typeName) {
    ObjectNode node = mapper.createObjectNode();
    node.put("type", typeName);
    return node;
  }
}
