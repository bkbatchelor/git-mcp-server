package io.sandboxdev.gitmcpserver.mcp;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class ToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public void registerTool(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public List<Tool> getTools() {
        return new ArrayList<>(tools.values());
    }

    public java.util.Optional<Tool> getTool(String name) {
        return java.util.Optional.ofNullable(tools.get(name));
    }
}
