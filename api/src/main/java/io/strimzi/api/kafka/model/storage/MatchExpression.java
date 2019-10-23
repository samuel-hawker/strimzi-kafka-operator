/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka.model.storage;

import static java.util.Collections.emptyMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.strimzi.api.kafka.model.UnknownPropertyPreserving;
import io.strimzi.crdgenerator.annotations.Description;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;


/**
 * Representation for persistent claim-based storage.
 */
@JsonPropertyOrder({"class"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
@EqualsAndHashCode
public class MatchExpression implements Serializable, UnknownPropertyPreserving {
    private String key;
    private List<String> values;
    private String operator;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    public MatchExpression(String key, List<String> values, String operator) {
        this.key = key;
        this.values = values;
        this.operator = operator;
    }

    public MatchExpression(MatchExpression matchExpression) {
        this.key = matchExpression.key;
        this.values = matchExpression.values;
        this.operator = matchExpression.operator;
    }

    @Description("The key the match expressions selects on")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Description("The values accepted for the specified key. " +
            "The values set must be non-empty in the case of the operator value being In or NotIn. ")
    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    @Description("The operator used for the match expression." +
            "Possible values are: In, NotIn, Exists, and DoesNotExist.")
    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties != null ? this.additionalProperties : emptyMap();
    }

    @Override
    public void setAdditionalProperty(String name, Object value) {
        if (this.additionalProperties == null) {
            this.additionalProperties = new HashMap<>();
        }
        this.additionalProperties.put(name, value);
    }

    public boolean isEmpty() {
        return this.key != null &&
            this.values != null &&
            this.operator != null;
    }
}