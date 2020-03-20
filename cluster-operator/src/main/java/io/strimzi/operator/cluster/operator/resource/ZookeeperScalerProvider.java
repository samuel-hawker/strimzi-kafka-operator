/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.resource;

import io.fabric8.kubernetes.api.model.Secret;
import io.vertx.core.Vertx;

/**
 * Helper interface to pass different ZookeeperScaler implementations
 */
public interface ZookeeperScalerProvider {
    /**
     * Creates an instance of ZookeeperScaler
     *
     * @param vertx                         Vertx instance
     * @param zookeeperConnectionString     Connection string to connect to the right Zookeeper
     * @param clusterCaCertSecret           Secret with Kafka cluster CA public key
     * @param coKeySecret                   Secret with Cluster Operator public and private key
     * @param operationTimeoutMs            Operation timeout
     *
     * @return  ZookeeperScaler instance
     */
    ZookeeperScaler createZookeeperScaler(Vertx vertx, String zookeeperConnectionString, Secret clusterCaCertSecret, Secret coKeySecret, long operationTimeoutMs);
}
