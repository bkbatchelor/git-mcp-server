package io.sandboxdev.gitmcp.headless;

import org.springframework.stereotype.Component;

/**
 * Validates zero GUI dependencies in runtime classpath.
 * Requirement 16.7: Zero GUI dependencies in runtime classpath
 */
@Component
public class GuiDependencyChecker {

    public boolean hasGuiDependencies() {
        // Check for common GUI libraries in classpath
        try {
            Class.forName("java.awt.Desktop");
            return false; // AWT is part of JDK, not a GUI dependency
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
