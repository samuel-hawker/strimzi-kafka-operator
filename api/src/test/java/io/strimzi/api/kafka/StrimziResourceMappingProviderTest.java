/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StrimziResourceMappingProviderTest {


    /**
     * The ServiceLoader can error with a cryptic Exception:
     * java.util.ServiceConfigurationError: io.fabric8.kubernetes.api.KubernetesResourceMappingProvider: Provider io.strimzi.api.kafka.StrimziResourceMappingProvider could not be instantiated
     *
     * This is due to the instantiation of StrimziResourceMappingProvider throwing but the exception isn't passed up the stack trace
     * Asserting that this does not throw in a test can mitigate the chances of this happening at runtime
     */
    @Test
    void testStrimziResourceMappingProvider() {
        assertDoesNotThrow(() -> new StrimziResourceMappingProvider());
    }
}