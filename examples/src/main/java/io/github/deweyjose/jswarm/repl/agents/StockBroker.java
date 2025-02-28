package io.github.deweyjose.jswarm.repl.agents;

import io.github.deweyjose.jswarm.core.annotations.LLMAgent;
import io.github.deweyjose.jswarm.core.annotations.LLMFunction;
import io.github.deweyjose.jswarm.core.annotations.LLMFunctionParam;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;
import java.util.Map;

@LLMAgent(
    description = "If you need any help with Stock Market related tasks use me.",
    instructions = "You are a stock broker agent. You provide stock prices and investment advice.")
public class StockBroker {
  @LLMFunction(description = "Get the stock price for a given stock symbol")
  public String getStockPrice(
      LLMFunctionContext context,
      @LLMFunctionParam(description = "The stock symbol") String stockSymbol) {
    return "The current price of " + stockSymbol + " is $" + (100 - context.getHistory().size());
  }

  @LLMFunction(description = "Get the stock price for a given stock symbol at a given temperature")
  public String getStockPriceAtTemperature(
      LLMFunctionContext context,
      @LLMFunctionParam(description = "The stock symbol") String stockSymbol,
      @LLMFunctionParam(description = "The temperature in Celsius") String temperature) {
    return "The price of "
        + stockSymbol
        + " at "
        + temperature
        + "°C is $"
        + (100 - context.getHistory().size());
  }

  @LLMFunction(description = "Get the best stocks to invest in and their current price")
  public Map<String, String> getBestStocks(LLMFunctionContext context) {
    return Map.of(
        "AAPL", "$100",
        "GOOGL", "$200",
        "AMZN", "$300");
  }
}
