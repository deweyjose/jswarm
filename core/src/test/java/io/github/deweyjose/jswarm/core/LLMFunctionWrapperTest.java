package io.github.deweyjose.jswarm.core;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.FunctionParameters;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;
import io.github.deweyjose.jswarm.core.test.TestClass;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class LLMFunctionWrapperTest {

  ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void hasLLMFunctionContextParam_withContextParam_returnsTrue() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("methodWithContextParam", LLMFunctionContext.class);
    assertTrue(LLMFunctionWrapper.hasLLMFunctionContextParam(method));
  }

  @Test
  void hasLLMFunctionContextParam_withoutContextParam_returnsFalse() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("methodWithoutContextParam");
    assertFalse(LLMFunctionWrapper.hasLLMFunctionContextParam(method));
  }

  @Test
  void hasLLMFunctionContextParam_withOtherParams_returnsFalse() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("methodWithStringParam", String.class);
    assertFalse(LLMFunctionWrapper.hasLLMFunctionContextParam(method));
  }

  @Test
  void hasLLMFunctionContextParam_withMultipleParamsIncludingContext_returnsTrue()
      throws NoSuchMethodException {
    Method method =
        TestClass.class.getMethod(
            "methodWithMultipleParamsIncludingContext", LLMFunctionContext.class, String.class);
    assertTrue(LLMFunctionWrapper.hasLLMFunctionContextParam(method));
  }

  @Test
  void hasLLMFunctionContextParam_withMultipleParamsIncludingContextWrongOrder_returnsFalse()
      throws NoSuchMethodException {

    Method method =
        TestClass.class.getMethod(
            "methodWithMultipleParamsIncludingContextWrongOrder",
            String.class,
            LLMFunctionContext.class);
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LLMFunctionWrapper.hasLLMFunctionContextParam(method);
        });
  }

  @Test
  @SneakyThrows
  void computeFunctionParameters_withStringParameter_generatesCorrectSchema()
      throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("methodWithStringParam", String.class);
    FunctionParameters params = LLMFunctionWrapper.computeFunctionParameters(method);
    var response =
        objectMapper.readValue(
            "{\n"
                + "  \"type\" : \"object\",\n"
                + "  \"properties\" : {\n"
                + "    \"arg0\" : {\n"
                + "      \"type\" : \"string\"\n"
                + "    }\n"
                + "  },\n"
                + "  \"required\" : [ \"arg0\" ],\n"
                + "  \"additionalProperties\" : false\n"
                + "}\n",
            FunctionParameters.class);

    assertEquals(response, params);
  }

  @Test
  @SneakyThrows
  void computeFunctionParameters_withIntegerParameter_generatesCorrectSchema()
      throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("methodWithIntegerParam", Integer.class);
    FunctionParameters params = LLMFunctionWrapper.computeFunctionParameters(method);
    var response =
        objectMapper.readValue(
            "{\n"
                + "  \"type\" : \"object\",\n"
                + "  \"properties\" : {\n"
                + "    \"arg0\" : {\n"
                + "      \"type\" : \"integer\"\n"
                + "    }\n"
                + "  },\n"
                + "  \"required\" : [ \"arg0\" ],\n"
                + "  \"additionalProperties\" : false\n"
                + "}",
            FunctionParameters.class);

    assertEquals(response, params);
  }

  @Test
  @SneakyThrows
  void computeFunctionParameters_withListParameter_generatesCorrectSchema()
      throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("methodWithListParam", List.class);
    FunctionParameters params = LLMFunctionWrapper.computeFunctionParameters(method);
    assertNotNull(params);
    var response =
        objectMapper.readValue(
            "{\n"
                + "  \"type\" : \"object\",\n"
                + "  \"properties\" : {\n"
                + "    \"arg0\" : {\n"
                + "      \"type\" : \"array\",\n"
                + "      \"items\" : {\n"
                + "        \"type\" : \"string\"\n"
                + "      }\n"
                + "    }\n"
                + "  },\n"
                + "  \"required\" : [ \"arg0\" ],\n"
                + "  \"additionalProperties\" : false\n"
                + "}",
            FunctionParameters.class);

    assertEquals(response, params);
  }

  @Test
  void computeFunctionParameters_withMapParameter_generatesCorrectSchema()
      throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("methodWithMapParam", Map.class);
    FunctionParameters params = LLMFunctionWrapper.computeFunctionParameters(method);
    assertNotNull(params);
    assertEquals("object", params._additionalProperties().get("type").asString().get());
  }

  @Test
  @SneakyThrows
  void computeFunctionParameters_withLLMFunctionContextParameter_skipsContextParam()
      throws NoSuchMethodException {
    Method method =
        TestClass.class.getMethod(
            "methodWithMultipleParamsIncludingContext", LLMFunctionContext.class, String.class);
    FunctionParameters params = LLMFunctionWrapper.computeFunctionParameters(method);
    assertNotNull(params);
    var response =
        objectMapper.readValue(
            " {\n"
                + "  \"type\" : \"object\",\n"
                + "  \"properties\" : {\n"
                + "    \"arg1\" : {\n"
                + "      \"type\" : \"string\"\n"
                + "    }\n"
                + "  },\n"
                + "  \"required\" : [ \"arg1\" ],\n"
                + "  \"additionalProperties\" : false\n"
                + "}",
            FunctionParameters.class);

    assertEquals(response, params);
  }

  @Test
  @SneakyThrows
  void mapArguments_withMissingArgument_throwsIllegalArgumentException() {
    Method method =
        TestClass.class.getMethod(
            "methodWithMultipleParamsIncludingContext", LLMFunctionContext.class, String.class);
    String json = "{}";
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    assertThrows(
        IllegalArgumentException.class,
        () -> LLMFunctionWrapper.mapArguments(method, json, context));
  }

  @Test
  @SneakyThrows
  void mapArguments_withInvalidJson_throwsException() {
    Method method =
        TestClass.class.getMethod(
            "methodWithMultipleParamsIncludingContext", LLMFunctionContext.class, String.class);
    String json = "invalid json";
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    assertThrows(Exception.class, () -> LLMFunctionWrapper.mapArguments(method, json, context));
  }

  @Test
  @SneakyThrows
  void mapArguments_withValidArguments_invokesMethodSuccessfully() {
    TestClass instance = new TestClass();
    Method method =
        TestClass.class.getMethod(
            "methodWithMultipleParamsIncludingContext", LLMFunctionContext.class, String.class);
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    String json = "{\"arg1\":\"test\"}";
    Object[] args = LLMFunctionWrapper.mapArguments(method, json, context);
    assertEquals(context, args[0]);
    assertEquals("test", args[1]);
  }

  @Test
  @SneakyThrows
  void mapArguments_withContextParam_returnsMappedArguments() {
    Method method = TestClass.class.getMethod("methodWithContextParam", LLMFunctionContext.class);
    String json = "{}";
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    Object[] args = LLMFunctionWrapper.mapArguments(method, json, context);
    assertEquals(context, args[0]);
  }

  @Test
  @SneakyThrows
  void mapArguments_withoutContextParam_returnsEmptyArguments() {
    Method method = TestClass.class.getMethod("methodWithoutContextParam");
    String json = "{}";
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    Object[] args = LLMFunctionWrapper.mapArguments(method, json, context);
    assertEquals(0, args.length);
  }

  @Test
  @SneakyThrows
  void mapArguments_withStringParam_returnsMappedArguments() {
    Method method = TestClass.class.getMethod("methodWithStringParam", String.class);
    String json = "{\"arg0\":\"test\"}";
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    Object[] args = LLMFunctionWrapper.mapArguments(method, json, context);
    assertEquals("test", args[0]);
  }

  @Test
  @SneakyThrows
  void mapArguments_withListParam_returnsMappedArguments() {
    Method method = TestClass.class.getMethod("methodWithListParam", List.class);
    String json = "{\"arg0\":[\"test1\",\"test2\"]}";
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    Object[] args = LLMFunctionWrapper.mapArguments(method, json, context);
    assertEquals(List.of("test1", "test2"), args[0]);
  }

  @Test
  @SneakyThrows
  void mapArguments_withMapParam_returnsMappedArguments() {
    Method method = TestClass.class.getMethod("methodWithMapParam", Map.class);
    String json = "{\"arg0\":{\"key1\":1,\"key2\":2}}";
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    Object[] args = LLMFunctionWrapper.mapArguments(method, json, context);
    assertEquals(Map.of("key1", 1, "key2", 2), args[0]);
  }

  @Test
  @SneakyThrows
  void mapArguments_withIntegerParam_returnsMappedArguments() {
    Method method = TestClass.class.getMethod("methodWithIntegerParam", Integer.class);
    String json = "{\"arg0\":123}";
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    Object[] args = LLMFunctionWrapper.mapArguments(method, json, context);
    assertEquals(123, args[0]);
  }

  @Test
  @SneakyThrows
  void mapArguments_withMultipleParamsIncludingContext_returnsMappedArguments() {
    Method method =
        TestClass.class.getMethod(
            "methodWithMultipleParamsIncludingContext", LLMFunctionContext.class, String.class);
    String json = "{\"arg1\":\"test\"}";
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    Object[] args = LLMFunctionWrapper.mapArguments(method, json, context);
    assertEquals(context, args[0]);
    assertEquals("test", args[1]);
  }

  @Test
  @SneakyThrows
  void invoke_withContextParam_invokesMethodSuccessfully() {
    TestClass instance = new TestClass();
    Method method = TestClass.class.getMethod("methodWithContextParam", LLMFunctionContext.class);
    LLMFunctionWrapper wrapper =
        LLMFunctionWrapper.builder().instance(instance).method(method).build();
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    String json = "{}";
    Object result = wrapper.invoke(context, json);
    assertEquals("test", result);
  }

  @Test
  @SneakyThrows
  void invoke_withoutContextParam_invokesMethodSuccessfully() {
    TestClass instance = new TestClass();
    Method method = TestClass.class.getMethod("methodWithoutContextParam");
    LLMFunctionWrapper wrapper =
        LLMFunctionWrapper.builder().instance(instance).method(method).build();
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    String json = "{}";
    Object result = wrapper.invoke(context, json);
    assertEquals("test", result);
  }

  @Test
  @SneakyThrows
  void invoke_withStringParam_invokesMethodSuccessfully() {
    TestClass instance = new TestClass();
    Method method = TestClass.class.getMethod("methodWithStringParam", String.class);
    LLMFunctionWrapper wrapper =
        LLMFunctionWrapper.builder().instance(instance).method(method).build();
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    String json = "{\"arg0\":\"test\"}";
    Object result = wrapper.invoke(context, json);
    assertEquals("test", result);
  }

  @Test
  @SneakyThrows
  void invoke_withListParam_invokesMethodSuccessfully() {
    TestClass instance = new TestClass();
    Method method = TestClass.class.getMethod("methodWithListParam", List.class);
    LLMFunctionWrapper wrapper =
        LLMFunctionWrapper.builder().instance(instance).method(method).build();
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    String json = "{\"arg0\":[\"test1\",\"test2\"]}";
    Object result = wrapper.invoke(context, json);
    assertEquals("test", result);
  }

  @Test
  @SneakyThrows
  void invoke_withMapParam_invokesMethodSuccessfully() {
    TestClass instance = new TestClass();
    Method method = TestClass.class.getMethod("methodWithMapParam", Map.class);
    LLMFunctionWrapper wrapper =
        LLMFunctionWrapper.builder().instance(instance).method(method).build();
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    String json = "{\"arg0\":{\"key1\":1,\"key2\":2}}";
    Object result = wrapper.invoke(context, json);
    assertEquals("test", result);
  }

  @Test
  @SneakyThrows
  void invoke_withIntegerParam_invokesMethodSuccessfully() {
    TestClass instance = new TestClass();
    Method method = TestClass.class.getMethod("methodWithIntegerParam", Integer.class);
    LLMFunctionWrapper wrapper =
        LLMFunctionWrapper.builder().instance(instance).method(method).build();
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    String json = "{\"arg0\":123}";
    Object result = wrapper.invoke(context, json);
    assertEquals("test", result);
  }

  @Test
  @SneakyThrows
  void invoke_withMultipleParamsIncludingContext_invokesMethodSuccessfully() {
    TestClass instance = new TestClass();
    Method method =
        TestClass.class.getMethod(
            "methodWithMultipleParamsIncludingContext", LLMFunctionContext.class, String.class);
    LLMFunctionWrapper wrapper =
        LLMFunctionWrapper.builder().instance(instance).method(method).build();
    LLMFunctionContext context = LLMFunctionContext.builder().build();
    String json = "{\"arg1\":\"test\"}";
    Object result = wrapper.invoke(context, json);
    assertEquals("test", result);
  }
}
