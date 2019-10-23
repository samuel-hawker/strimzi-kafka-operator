FROM registry.access.redhat.com/ubi8/ubi-minimal
ARG JAVA_VERSION=1.8.0

RUN microdnf -y update \
    && microdnf -y install java-${JAVA_VERSION}-openjdk-headless openssl \
    && microdnf -y clean all
