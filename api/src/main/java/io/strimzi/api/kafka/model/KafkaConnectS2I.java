/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.client.CustomResource;
import io.strimzi.api.kafka.model.status.HasStatus;
import io.strimzi.api.kafka.model.status.KafkaConnectS2IStatus;
import io.strimzi.crdgenerator.annotations.Crd;
import io.strimzi.crdgenerator.annotations.Description;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@Crd(
        apiVersion = Constants.V1BETA1_API_VERSION,
        spec = @Crd.Spec(
                names = @Crd.Spec.Names(
                        kind = KafkaConnectS2I.RESOURCE_KIND,
                        plural = KafkaConnectS2I.RESOURCE_PLURAL,
                        shortNames = {KafkaConnectS2I.SHORT_NAME},
                        categories = {Constants.STRIMZI_CATEGORY}
                ),
                group = Constants.RESOURCE_GROUP,
                scope = Constants.NAMESPACE_SCOPE,
                version = Constants.V1BETA1,
                versions = {
                        @Crd.Spec.Version(
                                name = Constants.V1BETA1,
                                served = true,
                                storage = true
                        ),
                        @Crd.Spec.Version(
                                name = Constants.V1ALPHA1,
                                served = true,
                                storage = false
                        )
                },
                subresources = @Crd.Spec.Subresources(
                        status = @Crd.Spec.Subresources.Status()
                ),
                additionalPrinterColumns = {
                        @Crd.Spec.AdditionalPrinterColumn(
                                name = "Desired replicas",
                                description = "The desired number of Kafka Connect replicas",
                                jsonPath = ".spec.replicas",
                                type = "integer"
                        )
                }
        )
)
@Buildable(
        editableEnabled = false,
        builderPackage = Constants.FABRIC8_KUBERNETES_API,
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"apiVersion", "kind", "metadata", "spec", "status"})
@EqualsAndHashCode
public class KafkaConnectS2I extends CustomResource implements UnknownPropertyPreserving, HasStatus<KafkaConnectS2IStatus> {

    private static final long serialVersionUID = 1L;

    public static final String RESOURCE_KIND = "KafkaConnectS2I";
    public static final String RESOURCE_PLURAL = "kafkaconnects2is";
    public static final String CRD_NAME = RESOURCE_PLURAL + "." + Constants.RESOURCE_GROUP;
    public static final String SHORT_NAME = "kcs2i";

    private KafkaConnectS2ISpec spec;
    private KafkaConnectS2IStatus status;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @Description("The specification of the Kafka Connect Source-to-Image (S2I) cluster.")
    public KafkaConnectS2ISpec getSpec() {
        return spec;
    }

    public void setSpec(KafkaConnectS2ISpec spec) {
        this.spec = spec;
    }

    @Override
    @Description("The status of the Kafka Connect Source-to-Image (S2I) cluster.")
    public KafkaConnectS2IStatus getStatus() {
        return status;
    }

    public void setStatus(KafkaConnectS2IStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        YAMLMapper mapper = new YAMLMapper().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @Override
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
