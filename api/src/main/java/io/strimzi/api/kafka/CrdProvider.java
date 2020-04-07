/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.Map;

public interface CrdProvider {

    Map<String, CustomResourceDefinition> crds();

    <T extends KubernetesResource & HasMetadata, L extends KubernetesResourceList<T>, D extends Doneable<T>>
            MixedOperation<T, L, D, Resource<T, D>> operation(KubernetesClient client,
                                                              Class<T> cls,
                                                              Class<L> listCls,
                                                              Class<D> doneableCls);
}
