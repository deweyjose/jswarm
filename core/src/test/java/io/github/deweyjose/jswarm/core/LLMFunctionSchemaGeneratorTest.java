package io.github.deweyjose.jswarm.core;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;
import io.github.deweyjose.jswarm.core.test.TestClass;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LLMFunctionSchemaGeneratorTest {
  @Test
  void generateSchema_withStringParameter_generatesStringSchema() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("methodWithStringParam", String.class);
    ObjectNode schema = LLMFunctionSchemaGenerator.generateSchema(method);
    JsonNode properties = schema.get("properties");
    assertEquals("string", properties.get("arg0").get("type").asText());
  }

  @Test
  void generateSchema_withIntegerParameter_generatesIntegerSchema() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("methodWithIntegerParam", Integer.class);
    ObjectNode schema = LLMFunctionSchemaGenerator.generateSchema(method);
    JsonNode properties = schema.get("properties");
    assertEquals("integer", properties.get("arg0").get("type").asText());
  }

  @Test
  void generateSchema_withListParameter_generatesArraySchema() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("methodWithListParam", List.class);
    ObjectNode schema = LLMFunctionSchemaGenerator.generateSchema(method);
    JsonNode properties = schema.get("properties");
    assertEquals("array", properties.get("arg0").get("type").asText());
  }

  @Test
  void generateSchema_withMapParameter_generatesObjectSchema() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("methodWithMapParam", Map.class);
    ObjectNode schema = LLMFunctionSchemaGenerator.generateSchema(method);
    JsonNode properties = schema.get("properties");
    assertEquals("object", properties.get("arg0").get("type").asText());
  }

  @Test
  void generateSchema_withLLMFunctionContextParameter_skipsContextParam()
      throws NoSuchMethodException {
    Method method =
        TestClass.class.getMethod(
            "methodWithMultipleParamsIncludingContext", LLMFunctionContext.class, String.class);
    ObjectNode schema = LLMFunctionSchemaGenerator.generateSchema(method);
    JsonNode properties = schema.get("properties");
    assertFalse(properties.has("context"));
    assertEquals("string", properties.get("arg1").get("type").asText());
  }
}
