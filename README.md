# JSWARM Project

## Overview

JSWARM is a Java-based project that leverages OpenAI's API to create a chatbot with various functionalities. The project
is structured using Maven and includes multiple modules such as `core` and `repl`.

## Modules

- **core**: Contains the main logic for interacting with OpenAI's API and managing agents. This module includes:
    - **LLMAgentRegistry**: Manages the registration and retrieval of agents and their functions.
    - **LLMFunctionWrapper**: Wraps methods annotated with `@LLMFunction` to be used as chatbot functions.
    - **Annotations**: Custom annotations like `@LLMCoordinator`, `@LLMFunction`, and `@LLMFunctionParam` to define
      agents and their functions.

- **repl**: Provides a command-line interface for interacting with the chatbot. This module includes:
    - **NexusRepl**: A simple Read-Eval-Print Loop (REPL) interface to interact with the chatbot.

## Annotations

### `@LLMCoordinator`

- **Purpose**: Marks a class as the coordinator agent.
- **Impact**: The coordinator agent is responsible for managing interactions and delegating tasks to other agents. Only
  one class should be annotated with `@LLMCoordinator`.

### `@LLMFunction`

- **Purpose**: Marks a method as a function that can be called by the chatbot.
- **Impact**: Methods annotated with `@LLMFunction` are registered and can be invoked by the chatbot. The `description`
  attribute provides a brief description of the function. The `global` attribute determines the scope of the function:
    - **Global Functions**: Functions with `global = true` or static methods are available to all agents and can be used
      for agent transfers and other global operations.
    - **Agent-Scoped Functions**: Functions with `global = false` (default) are specific to the agent they belong to and
      cannot be accessed by other agents.

### `@LLMFunctionParam`

- **Purpose**: Marks a parameter of a method annotated with `@LLMFunction`.
- **Impact**: Provides a description for the parameter, which helps in understanding the purpose of each parameter when
  the function is invoked.

## `LLMFunctionContext` Class

### Overview

The `LLMFunctionContext` class provides context for functions annotated with `@LLMFunction`. It includes information
about the conversation history and any additional context variables that might be relevant for the function's execution.

### Fields

- **history**: A list of `ChatCompletionMessageParam` objects representing the conversation history.
- **developerContext**: A map of additional context variables that can be used by the function.

### Usage

The `LLMFunctionContext` class is used as a parameter in methods annotated with `@LLMFunction`. It allows the function
to access the conversation history and any additional context provided by the developer.

### Constraints

- The `LLMFunctionContext` parameter must be the first parameter in the method.
- It must be the only parameter of type `LLMFunctionContext` in the method.

### Example

Here is an example of how to use the `LLMFunctionContext` class in a function:

```java
import io.github.deweyjose.jswarm.core.annotations.LLMFunction;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;

public class ExampleAgent extends LLMAgent {

    public ExampleAgent() {
        super("gpt-4o", "Example instructions", "Example description");
    }

    @LLMFunction(description = "Get the conversation history length")
    public Integer getHistory(LLMFunctionContext context) {
        return context.getHistory().size();
    }
}
```

In this example, the `getHistory` method uses the `LLMFunctionContext` parameter to access and return the conversation
history.

## Loading Agents

Agents are loaded by the `LLMAgentRegistry` class. The registry scans the specified package for classes that
extend `LLMAgent` and registers them. The coordinator agent is identified by the `@LLMCoordinator` annotation.

## Environment Variables

### `OPENAI_API_KEY`

- **Description**: Your OpenAI API key.
- **Usage**: Required for authenticating requests to the OpenAI API.

### `AGENT_PACKAGE`

- **Description**: The package where agent classes are located.
- **Usage**: Specifies the package to scan for agent classes.
- **Default Behavior**: If `AGENT_PACKAGE` is not set, the default package `io.github.deweyjose.jswarm.agents` is used.

## Setup

1. **Clone the repository**:
    ```sh
    git clone https://github.com/deweyjose/jswarm.git
    cd jswarm
    ```

2. **Set up environment variables**:
    ```sh
    export OPENAI_API_KEY=your_openai_api_key
    export AGENT_PACKAGE=io.github.deweyjose.jswarm.agents
    ```

3. **Build the project**:
    ```sh
    mvn clean install
    ```

## Running the REPL

To start the REPL (Read-Eval-Print Loop)

```sh
java -cp repl/target/repl-1.0-SNAPSHOT.jar io.github.deweyjose.jswarm.repl.NexusRepl
```

## Creating an Agent

### Coordinator Agent Example

```java
package io.github.deweyjose.jswarm.repl.agents;

import io.github.deweyjose.jswarm.core.annotations.LLMCoordinator;
import io.github.deweyjose.jswarm.core.annotations.LLMFunction;
import io.github.deweyjose.jswarm.core.model.LLMAgent;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LLMCoordinator
public class Optimus extends LLMAgent {

    public Optimus() {
        super(
                "gpt-4o",
                "You are NexusOptimus, the central command unit of the LLMNexus framework...",
                "Use me when you need to coordinate multiple agents or return control back to the Agent in charge.");
    }

    @LLMFunction(description = "Exit the program")
    public static String exit() {
        log.info("Exiting program...");
        System.exit(0);
        return "Exiting program...";
    }

    @LLMFunction(description = "Clear the history of the conversation")
    public static String clearHistory(LLMFunctionContext context) {
        if (context.getHistory().size() > 1) {
            context.getHistory().subList(0, context.getHistory().size() - 1).clear();
        }
        return "History cleared.";
    }
}
```

### Non-Coordinator Agent Example

```java
package io.github.deweyjose.jswarm.repl.agents;

import io.github.deweyjose.jswarm.core.annotations.LLMFunction;
import io.github.deweyjose.jswarm.core.annotations.LLMFunctionParam;
import io.github.deweyjose.jswarm.core.model.LLMAgent;
import io.github.deweyjose.jswarm.core.model.LLMFunctionContext;

import java.util.Map;

public class StockBroker extends LLMAgent {

    public StockBroker() {
        super(
                "gpt-4o",
                "You are a stock broker. You provide stock market analysis and advice and provide quotes.",
                "If you need help with the stock market use me.");
    }

    @LLMFunction(description = "Get the stock price for a given stock symbol")
    public String getStockPrice(
            LLMFunctionContext context,
            @LLMFunctionParam(description = "The stock symbol") String stockSymbol) {
        return "The current price of " + stockSymbol + " is $" + (100 - context.getHistory().size());
    }

    @LLMFunction(description = "Get the best stocks to invest in and their current price")
    public Map<String, String> getBestStocks(LLMFunctionContext context) {
        return Map.of(
                "AAPL", "$100",
                "GOOGL", "$200",
                "AMZN", "$300");
    }
}
```