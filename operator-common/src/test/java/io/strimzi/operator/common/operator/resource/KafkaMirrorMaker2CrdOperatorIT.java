/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.common.operator.resource;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.KafkaMirrorMaker2List;
import io.strimzi.api.kafka.model.DoneableKafkaMirrorMaker2;
import io.strimzi.api.kafka.model.KafkaMirrorMaker2;
import io.strimzi.api.kafka.model.KafkaMirrorMaker2Builder;
import io.strimzi.api.kafka.model.status.ConditionBuilder;
import io.strimzi.operator.KubernetesVersion;
import io.strimzi.operator.PlatformFeaturesAvailability;
import io.strimzi.test.k8s.KubeClusterResource;
import io.strimzi.test.k8s.cluster.KubeCluster;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.strimzi.test.k8s.KubeClusterResource.cmdKubeClient;
import static io.strimzi.test.k8s.KubeClusterResource.kubeClient;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * The main purpose of the Integration Tests for the operators is to test them against a real Kubernetes cluster.
 * Real Kubernetes cluster has often some quirks such as some fields being immutable, some fields in the spec section
 * being created by the Kubernetes API etc. These things are hard to test with mocks. These IT tests make it easy to
 * test them against real clusters.
 */
@ExtendWith(VertxExtension.class)
public class KafkaMirrorMaker2CrdOperatorIT {
    protected static final Logger log = LogManager.getLogger(KafkaMirrorMaker2CrdOperatorIT.class);

    public static final String RESOURCE_NAME = "my-test-resource";
    protected static Vertx vertx;
    protected static KubernetesClient client;
    protected static CrdOperator<KubernetesClient, KafkaMirrorMaker2, KafkaMirrorMaker2List, DoneableKafkaMirrorMaker2> kafkaMirrorMaker2Operator;
    protected static String namespace = "connect-crd-it-namespace";

    private static KubeClusterResource cluster;

    @BeforeAll
    public static void before() {
        cluster = KubeClusterResource.getInstance();
        cluster.setTestNamespace(namespace);

        assertDoesNotThrow(() -> KubeCluster.bootstrap(), "Could not bootstrap server");

        vertx = Vertx.vertx();
        client = new DefaultKubernetesClient();
        kafkaMirrorMaker2Operator = new CrdOperator(vertx, client, KafkaMirrorMaker2.class, KafkaMirrorMaker2List.class, DoneableKafkaMirrorMaker2.class);

        log.info("Preparing namespace");
        if (cluster.getTestNamespace() != null && System.getenv("SKIP_TEARDOWN") == null) {
            log.warn("Namespace {} is already created, going to delete it", namespace);
            kubeClient().deleteNamespace(namespace);
            cmdKubeClient().waitForResourceDeletion("Namespace", namespace);
        }

        log.info("Creating namespace: {}", namespace);
        kubeClient().createNamespace(namespace);
        cmdKubeClient().waitForResourceCreation("Namespace", namespace);

        log.info("Creating CRD");
        client.customResourceDefinitions().create(Crds.kafkaMirrorMaker2());
        log.info("Created CRD");
    }

    @AfterAll
    public static void after() {
        if (client != null) {
            log.info("Deleting CRD");
            client.customResourceDefinitions().delete(Crds.kafkaMirrorMaker2());
        }
        if (kubeClient().getNamespace(namespace) != null && System.getenv("SKIP_TEARDOWN") == null) {
            log.warn("Deleting namespace {} after tests run", namespace);
            kubeClient().deleteNamespace(namespace);
            cmdKubeClient().waitForResourceDeletion("Namespace", namespace);
        }

        if (vertx != null) {
            vertx.close();
        }
    }

    protected KafkaMirrorMaker2 getResource() {
        return new KafkaMirrorMaker2Builder()
                .withApiVersion(KafkaMirrorMaker2.RESOURCE_GROUP + "/" + KafkaMirrorMaker2.V1ALPHA1)
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

    @Test
    public void testUpdateStatus(VertxTestContext context) {
        Checkpoint async = context.checkpoint();

        log.info("Getting Kubernetes version");
        PlatformFeaturesAvailability.create(vertx, client)
            .setHandler(context.succeeding(pfa -> context.verify(() -> {
                assertThat("Kubernetes version : " + pfa.getKubernetesVersion() + " is too old",
                        pfa.getKubernetesVersion().compareTo(KubernetesVersion.V1_11), is(not(lessThan(0))));
            })))

            .compose(pfa -> {
                log.info("Creating resource");
                return kafkaMirrorMaker2Operator.reconcile(namespace, RESOURCE_NAME, getResource());
            })
            .setHandler(context.succeeding())
            .compose(rrCreated -> {
                KafkaMirrorMaker2 newStatus = new KafkaMirrorMaker2Builder(kafkaMirrorMaker2Operator.get(namespace, RESOURCE_NAME))
                        .withNewStatus()
                        .withConditions(new ConditionBuilder()
                                .withType("Ready")
                                .withStatus("True")
                                .build())
                        .endStatus()
                        .build();

                log.info("Updating resource status");
                return kafkaMirrorMaker2Operator.updateStatusAsync(newStatus);
            })
            .setHandler(context.succeeding())

            .compose(rrModified -> kafkaMirrorMaker2Operator.getAsync(namespace, RESOURCE_NAME))
            .setHandler(context.succeeding(modifiedKafkaMirrorMaker2 -> context.verify(() -> {
                assertThat(modifiedKafkaMirrorMaker2.getStatus().getConditions().get(0).getType(), is("Ready"));
                assertThat(modifiedKafkaMirrorMaker2.getStatus().getConditions().get(0).getStatus(), is("True"));
            })))

            .compose(rrModified -> {
                log.info("Deleting resource");
                return kafkaMirrorMaker2Operator.reconcile(namespace, RESOURCE_NAME, null);
            })
            .setHandler(context.succeeding(rrDeleted ->  async.flag()));
    }

    /**
     * Tests what happens when the resource is deleted while updating the status
     *
     * @param context
     */
    @Test
    public void testUpdateStatusWhileResourceDeletedThrowsNullPointerException(VertxTestContext context) {
        Checkpoint async = context.checkpoint();

        log.info("Getting Kubernetes version");
        PlatformFeaturesAvailability.create(vertx, client)
            .setHandler(context.succeeding(pfa -> context.verify(() -> {
                assertThat("Kubernetes version : " + pfa.getKubernetesVersion() + " is too old",
                        pfa.getKubernetesVersion().compareTo(KubernetesVersion.V1_11), is(not(lessThan(0))));
            })))
            .compose(pfa -> {
                log.info("Creating resource");
                return kafkaMirrorMaker2Operator.reconcile(namespace, RESOURCE_NAME, getResource());
            })
            .setHandler(context.succeeding())

            .compose(rr -> {
                log.info("Deleting resource");
                return kafkaMirrorMaker2Operator.reconcile(namespace, RESOURCE_NAME, null);
            })
            .setHandler(context.succeeding())

            .compose(v -> {
                KafkaMirrorMaker2 newStatus = new KafkaMirrorMaker2Builder(kafkaMirrorMaker2Operator.get(namespace, RESOURCE_NAME))
                        .withNewStatus()
                            .withConditions(new ConditionBuilder()
                                    .withType("Ready")
                                    .withStatus("True")
                                    .build())
                        .endStatus()
                        .build();

                log.info("Updating resource status");
                return kafkaMirrorMaker2Operator.updateStatusAsync(newStatus);
            })
            .setHandler(context.failing(e -> context.verify(() -> {
                assertThat(e, instanceOf(NullPointerException.class));
                async.flag();
            })));
    }

    /**
     * Tests what happens when the resource is modifed while updating the status
     *
     * @param context
     */
    @Test
    public void testUpdateStatusThrowsKubernetesExceptionIfResourceUpdatedPrior(VertxTestContext context) {
        Checkpoint async = context.checkpoint();

        Promise updateFailed = Promise.promise();

        log.info("Getting Kubernetes version");
        PlatformFeaturesAvailability.create(vertx, client)
            .setHandler(context.succeeding(pfa -> context.verify(() -> {
                assertThat("Kubernetes version : " + pfa.getKubernetesVersion() + " is too old",
                        pfa.getKubernetesVersion().compareTo(KubernetesVersion.V1_11), is(not(lessThan(0))));
            })))
            .compose(pfa -> {
                log.info("Creating resource");
                return kafkaMirrorMaker2Operator.reconcile(namespace, RESOURCE_NAME, getResource());
            })
            .setHandler(context.succeeding())
            .compose(rr -> {
                KafkaMirrorMaker2 currentKafkaMirrorMaker2 = kafkaMirrorMaker2Operator.get(namespace, RESOURCE_NAME);

                KafkaMirrorMaker2 updated = new KafkaMirrorMaker2Builder(currentKafkaMirrorMaker2)
                        .editSpec()
                        .withNewLivenessProbe().withInitialDelaySeconds(14).endLivenessProbe()
                        .endSpec()
                        .build();

                KafkaMirrorMaker2 newStatus = new KafkaMirrorMaker2Builder(currentKafkaMirrorMaker2)
                        .withNewStatus()
                        .withConditions(new ConditionBuilder()
                                .withType("Ready")
                                .withStatus("True")
                                .build())
                        .endStatus()
                        .build();

                log.info("Updating resource (mocking an update due to some other reason)");
                kafkaMirrorMaker2Operator.operation().inNamespace(namespace).withName(RESOURCE_NAME).patch(updated);

                log.info("Updating resource status after underlying resource has changed");
                return kafkaMirrorMaker2Operator.updateStatusAsync(newStatus);
            })
            .setHandler(context.failing(e -> context.verify(() -> {
                assertThat("Exception was not KubernetesClientException, it was : " + e.toString(),
                        e, instanceOf(KubernetesClientException.class));
                updateFailed.complete();
            })));

        updateFailed.future().compose(v -> {
            log.info("Deleting resource");
            return kafkaMirrorMaker2Operator.reconcile(namespace, RESOURCE_NAME, null);
        })
        .setHandler(context.succeeding(v -> async.flag()));

    }
}

