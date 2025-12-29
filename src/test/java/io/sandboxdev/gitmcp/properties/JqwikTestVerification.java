package io.sandboxdev.gitmcp.properties;

import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test to verify jqwik is working
 */
class JqwikTestVerification {

    @Property
    @DisplayName("Simple jqwik test to verify framework is working")
    void simplePropertyTest(@ForAll int number) {
        assertThat(number + 1).isGreaterThan(number);
    }
}
