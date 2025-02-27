package io.github.deweyjose.jswarm.repl.agents;

import io.github.deweyjose.jswarm.core.annotations.LLMAgent;
import io.github.deweyjose.jswarm.core.annotations.LLMFunction;
import io.github.deweyjose.jswarm.core.annotations.LLMFunctionParam;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LLMAgent(
    description = "If you need help with the weather use me.",
    instructions = "You are a weather assistant. You provide weather information for any city.")
public class WeatherMan {

  @LLMFunction(description = "Get the weather for a given city")
  public String getWeather(
      @LLMFunctionParam(description = "The city to get the weather for") String city,
      @LLMFunctionParam(description = "The id of the city") Integer id,
      @LLMFunctionParam(description = "The nearby cities") List<String> nearbyCities) {
    return String.format(
        "The weather in %s is sunny with a high of 25°C. "
            + "The id is %d. The nearby cities are %s",
        city, id, String.join(", ", nearbyCities));
  }

  @LLMFunction(description = "Convert Celsius to Fahrenheit")
  public String convertToFahrenheit(
      @LLMFunctionParam(description = "The input temp in Celsius") String celsius) {
    double celsiusValue = Double.parseDouble(celsius);
    double fahrenheitValue = (celsiusValue * 9 / 5) + 32;
    return String.format("%.2f", fahrenheitValue);
  }

  @LLMFunction(description = "Convert Fahrenheit to Celsius")
  public String convertToCelsius(
      @LLMFunctionParam(description = "The input temp in Fahrenheit") String fahrenheit) {
    double fahrenheitValue = Double.parseDouble(fahrenheit);
    double celsiusValue = (fahrenheitValue - 32) * 5 / 9;
    return String.format("%.2f", celsiusValue);
  }
}
