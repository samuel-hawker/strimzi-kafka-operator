/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.operators;

import io.strimzi.systemtest.AbstractST;
import io.strimzi.systemtest.resources.crd.KafkaResource;
import io.strimzi.systemtest.resources.operator.BundleResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static io.strimzi.systemtest.Constants.REGRESSION;
import static io.strimzi.test.k8s.KubeClusterResource.kubeClient;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Tag(REGRESSION)
class RolesOnlyOperatorST extends AbstractST {

    static final String NAMESPACE = "roles-only-cluster-test";
    static final String CLUSTER_NAME = "roles-only-cluster";

    private static final Logger LOGGER = LogManager.getLogger(RolesOnlyOperatorST.class);

    @Test
    void testRolesOnlyDeploysRoles() {
        prepareEnvironment();

        KafkaResource.kafkaEphemeral(CLUSTER_NAME, 3, 3).done();

        // Obviously not right.
        assertThat(kubeClient().listClusterRoles(), is(Collections.emptyList()));
    }

    private void prepareEnvironment() {
        prepareEnvForOperator(NAMESPACE);
//        applyRoles(NAMESPACE);
        applyRoleBindings(NAMESPACE);
        // 060-Deployment
        BundleResource.clusterOperator(NAMESPACE)
                .editOrNewSpec()
                    .editOrNewTemplate()
                        .editOrNewSpec()
                            .editContainer(0)
                                .addNewEnv()
                                    .withName("STRIMZI_ROLES_ONLY")
                                    .withValue("true")
                                .endEnv()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .done();
    }
}
