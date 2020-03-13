/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.common.operator.resource;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.KafkaUserList;
import io.strimzi.api.kafka.model.DoneableKafkaUser;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserBuilder;
import io.strimzi.api.kafka.model.status.Condition;
import io.strimzi.api.kafka.model.status.ConditionBuilder;
import io.strimzi.operator.KubernetesVersion;
import io.strimzi.operator.PlatformFeaturesAvailability;
import io.strimzi.test.k8s.KubeClusterResource;
import io.strimzi.test.k8s.cluster.KubeCluster;
import io.strimzi.test.k8s.exceptions.NoClusterException;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static io.strimzi.test.k8s.KubeClusterResource.cmdKubeClient;
import static io.strimzi.test.k8s.KubeClusterResource.kubeClient;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The main purpose of the Integration Tests for the operators is to test them against a real Kubernetes cluster.
 * Real Kubernetes cluster has often some quirks such as some fields being immutable, some fields in the spec section
 * being created by the Kubernetes API etc. These things are hard to test with mocks. These IT tests make it easy to
 * test them against real clusters.
 */
@ExtendWith(VertxExtension.class)
public class KafkaUserCrdOperatorIT extends AbstractCustomResourceOperatorIT {
    protected static final Logger log = LogManager.getLogger(KafkaUserCrdOperatorIT.class);

    @Override
    CrdOperator operator() {
        return new CrdOperator(vertx, client, KafkaUser.class, KafkaUserList.class, DoneableKafkaUser.class);
    }

    @Override
    CustomResourceDefinition getCrd() {
        return Crds.kafkaUser();
    }

    protected KafkaUser getResource() {
        return new KafkaUserBuilder()
                .withApiVersion(KafkaUser.RESOURCE_GROUP + "/" + KafkaUser.V1BETA1)
                .withNewMetadata()
                .withName(RESOURCE_NAME)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .withNewStatus()
                .endStatus()
                .build();
    }

    @Override
    KafkaUser getResourceWithModifications(CustomResource resourceInCluster) {
        return new KafkaUserBuilder((KafkaUser) resourceInCluster)
                .editSpec()
                .withNewKafkaUserTlsClientAuthentication().endKafkaUserTlsClientAuthentication()
                .endSpec()
                .build();
    }

    @Override
    KafkaUser getResourceWithNewReadyStatus(CustomResource resourceInCluster) {
        return new KafkaUserBuilder((KafkaUser) resourceInCluster)
                .withNewStatus()
                .withConditions(readyCondition)
                .endStatus()
                .build();
    }

    @Override
    void assertReady(VertxTestContext context, CustomResource modifiedCustomResource) {
        KafkaUser kafkaUser = (KafkaUser) modifiedCustomResource;
        context.verify(() -> assertThat(kafkaUser.getStatus()
                .getConditions()
                .get(0), is(readyCondition)));
    }
}

