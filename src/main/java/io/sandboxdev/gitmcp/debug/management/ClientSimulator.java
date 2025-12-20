package io.sandboxdev.gitmcp.debug.management;

import io.sandboxdev.gitmcp.debug.model.SimulationResult;
import io.sandboxdev.gitmcp.debug.model.TestScenario;

import java.util.Map;

/**
 * Interface for simulating MCP client interactions.
 * Handles tool invocation simulation and test scenario execution.
 */
public interface ClientSimulator {
    
    /**
     * Invoke a tool with the specified parameters.
     * 
     * @param toolName the name of the tool to invoke
     * @param parameters the parameters to pass to the tool
     * @return the result of the tool invocation simulation
     */
    SimulationResult invokeTool(String toolName, Map<String, Object> parameters);
    
    /**
     * Execute a test scenario containing multiple operations.
     * 
     * @param scenario the test scenario to execute
     * @return the result of the scenario execution
     */
    SimulationResult executeScenario(TestScenario scenario);
    
    /**
     * Save a test scenario for later replay.
     * 
     * @param name the name to save the scenario under
     * @param scenario the test scenario to save
     */
    void saveScenario(String name, TestScenario scenario);
    
    /**
     * Load a previously saved test scenario.
     * 
     * @param name the name of the scenario to load
     * @return the loaded test scenario
     */
    TestScenario loadScenario(String name);
}