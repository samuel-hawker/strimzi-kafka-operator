/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster;


import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

public class CustomMatchers {

    private CustomMatchers() { }

    /**
     * hasEntries is a custom matcher that checks that the entries in the provided map
     * are contained within the provided actual map
     *
     * @param entries a map of entries expected in the actual map
     * @return a custom Matcher which iterates through entries and delegates matching to hasEntry
     */
    public static Matcher<Map<String, String>> hasEntries(Map<String, String> entries) {
        return new TypeSafeDiagnosingMatcher<Map<String, String>>() {

            @Override
            public void describeTo(final Description description) {
                description.appendText("Expected Map with entries").appendValue(entries);
            }

            @Override
            protected boolean matchesSafely(Map<String, String> actual, Description mismatchDescription) {
                Map<String, String> misMatchedEntries = entries.entrySet()
                        .stream()
                        .filter(entry -> !hasEntry(entry.getKey(), entry.getValue()).matches(actual))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                mismatchDescription.appendText(" was ").appendValue(actual)
                        .appendText("\nMismatched entries : ").appendValue(misMatchedEntries);
                return misMatchedEntries.isEmpty();
            }
        };
    }

    /**
     * hasOwnerReferenceFor is a custom matcher that checks that the parent resource has a correctly configured
     * OwnerReference for the parent
     *
     * @param parent the resource with a metadata containing an Owner Reference
     *
     * @return a custom Matcher that returns whether the parent has an OwnerReference referring to the supplied parent
     */
    public static TypeSafeDiagnosingMatcher<HasMetadata> hasOwnerReferenceFor(HasMetadata parent) {
        return new TypeSafeDiagnosingMatcher<HasMetadata>() {

            @Override
            public void describeTo(final Description description) {
                description.appendText("Expected Resource to have correct owner reference ").appendValue(parent);
            }

            @Override
            protected boolean matchesSafely(HasMetadata child, Description mismatchDescription) {
                StringBuilder mismatchesBuilder = new StringBuilder();

                // Currently assumes single owner reference, can be trivially extended to check multiple
                List<OwnerReference> ownerReferences = child.getMetadata().getOwnerReferences();
                if (ownerReferences.size() != 1) {
                    mismatchesBuilder.append("Expected 1 owner reference found ").append(ownerReferences.size());
                    return false;
                }

                OwnerReference ownerReference = ownerReferences.get(0);

                if (!is(ownerReference.getApiVersion()).matches(parent.getApiVersion())) {
                    mismatchesBuilder.append("Child owner reference apiVersion does not match parent apiVersion\n")
                            .append("Parent " + parent.getApiVersion() + "\n")
                            .append("Child OwnerReference" + ownerReference.getApiVersion() + "\n");
                }
                if (!is(ownerReference.getKind()).matches(parent.getKind())) {
                    mismatchesBuilder.append("Child owner reference kind does not match parent kind\n")
                            .append("Parent " + parent.getKind() + "\n")
                            .append("Child " + ownerReference.getKind() + "\n");
                }
                if (!is(ownerReference.getName()).matches(parent.getMetadata().getName())) {
                    mismatchesBuilder.append("Child owner reference metdata name does not match parent name\n")
                            .append("Parent " + parent.getMetadata().getName() + "\n")
                            .append("Child " + ownerReference.getName() + "\n");
                }
                if (!is(ownerReference.getUid()).matches(parent.getMetadata().getUid())) {
                    mismatchesBuilder.append("Child owner reference uid does not match parent uid\n")
                            .append("Parent " + parent.getMetadata().getUid() + "\n")
                            .append("Child OwnerReference" + ownerReference.getUid() + "\n");
                }
                String mismatches = mismatchesBuilder.toString();

                mismatchDescription.appendText(" was ").appendValue(child)
                        .appendText("\nMismatches : ").appendValue(mismatches);
                return mismatches.isEmpty();
            }
        };
    }

    private Deployment deploymentWithOwnerReference(OwnerReference ownerReference) {
        return new DeploymentBuilder()
            .withNewMetadata()
                .withOwnerReferences(ownerReference)
            .endMetadata()
            .build();
    }

    @Test
    public void testHasOwnerReference() {
        Kafka resource = new KafkaBuilder()
                .withApiVersion("v1")
                .withNewMetadata()
                    .withName("my-resource")
                    .withUid("123")
                .endMetadata()
                .build();

        OwnerReference correctOwnerReference = new OwnerReferenceBuilder()
                .withName("my-resource")
                .withApiVersion("v1")
                .withKind("Kafka")
                .withUid("123")
                .build();
        assertThat(deploymentWithOwnerReference(correctOwnerReference), hasOwnerReferenceFor(resource));

        OwnerReference wrongName = new OwnerReferenceBuilder(correctOwnerReference)
                .withName("wrong-resource")
                .build();
        assertThat(deploymentWithOwnerReference(wrongName), not(hasOwnerReferenceFor(resource)));

        OwnerReference wrongApiVersion = new OwnerReferenceBuilder(correctOwnerReference)
                .withApiVersion("v1alphawrong")
                .build();
        assertThat(deploymentWithOwnerReference(wrongApiVersion), not(hasOwnerReferenceFor(resource)));

        OwnerReference wrongKind = new OwnerReferenceBuilder(correctOwnerReference)
                .withKind("WrongKind")
                .build();
        assertThat(deploymentWithOwnerReference(wrongKind), not(hasOwnerReferenceFor(resource)));

        OwnerReference wrongUid = new OwnerReferenceBuilder(correctOwnerReference)
                .withUid("wrong-uid")
                .build();
        assertThat(deploymentWithOwnerReference(wrongUid), not(hasOwnerReferenceFor(resource)));
    }

}