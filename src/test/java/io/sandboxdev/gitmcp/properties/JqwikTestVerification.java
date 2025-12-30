package io.sandboxdev.gitmcp.properties;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

/**
     Simple test to verify jqwik is working
 */
class JqwikTestVerification {

    @Property
    void simplePropertyTest(@ForAll @IntRange(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE - 1) int number) {
        assertThat(number + 1).isGreaterThan(number);
    }
}
