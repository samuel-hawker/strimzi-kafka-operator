FROM strimzi/base:latest

# Add strimzi user with UID 1001
# The user is in the group 0 to have access to the mounted volumes and storage
# RUN useradd -r -m -u 1001 -g 0 strimzi

ARG strimzi_version=1.0-SNAPSHOT
ENV STRIMZI_VERSION ${strimzi_version}
ENV STRIMZI_HOME=/opt/strimzi
RUN mkdir -p ${STRIMZI_HOME}/bin
WORKDIR ${STRIMZI_HOME}

COPY scripts/ tmp/bin/ bin/

COPY tmp/lib/ lib/

COPY LICENSE /licenses/LICENSE

LABEL name="strimzi-operator" \
      vendor="CNCF" \
      release="2.3.0" \
      summary="Strimzi Kafka Operator" \
      description="Strimzi Kafka Operator is a high-throughput, fault-tolerant, pub-sub technology for building event-driven applications. It is built on top of Apache Kafka(R)"

USER 1001
