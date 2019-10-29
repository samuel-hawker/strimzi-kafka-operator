FROM registry.access.redhat.com/ubi8/ubi-minimal AS builder

ARG JAVA_VARIANT=jre
ARG B_ARCH=s390x
ARG JAVA_VERSION=1.8.0_sr5fp40
RUN microdnf install -y gzip tar; \
    set -eux; \
    JAVA_ARCH=${B_ARCH}; \
    if [ "${JAVA_ARCH}" = "amd64" ] ; then JAVA_ARCH='x86_64'; fi; \
    declare -A ESUMS; \
    ESUMS[jre,x86_64]='cbe9d719973ec8e9ce98977aefa9992be1a3b0a6a6e8a4e3397d5fd891f744f4'; \
    ESUMS[jre,ppc64le]='8cfd888fac4706b93fa720b610290fec4fd3601f00d8ad9279659003e6d8de4b'; \
    ESUMS[jre,s390x]='9188fb5a9ec74fb502dc0ee402a629e601338eedd8c1818244c0c12a9cdf5693'; \
    ESUMS[sdk,x86_64]='bc53faf476655e565f965dab3db37f9258bfc16bb8c5352c93d43d53860b79d3'; \
    ESUMS[sdk,ppc64le]='d70cf419442a446aa85eb09740a79386a80e962c790057371f5f07ddc7ef3ad5'; \
    ESUMS[sdk,s390x]='a42335e8633fc35214410402891de40773cfb75f451c59c689bf5abd3c78576a'; \
    YML_FILE="${JAVA_VARIANT}/linux/${JAVA_ARCH}/index.yml"; \
    BASE_URL="https://public.dhe.ibm.com/ibmdl/export/pub/systems/cloud/runtimes/java/meta/"; \
    curl -A UA_IBM_JAVA_Docker -o /tmp/index.yml "${BASE_URL}/${YML_FILE}"; \
    cat /tmp/index.yml; \
    JAVA_URL=$(sed -n '/^'${JAVA_VERSION}.*:'/{n;s/\s*uri:\s//p}'< /tmp/index.yml); \
    curl -A UA_IBM_JAVA_Docker -o /tmp/ibm-java.bin "${JAVA_URL}"; \
    echo "${ESUMS[${JAVA_VARIANT},${JAVA_ARCH}]} /tmp/ibm-java.bin" | sha256sum -c -; \
    echo "INSTALLER_UI=silent" > /tmp/response.properties; \
    echo "USER_INSTALL_DIR=/java" >> /tmp/response.properties; \
    echo "LICENSE_ACCEPTED=TRUE" >> /tmp/response.properties; \
    chmod +x /tmp/ibm-java.bin; \
    /tmp/ibm-java.bin -i silent -f /tmp/response.properties;

FROM registry.access.redhat.com/ubi8/ubi-minimal
ARG JAVA_VERSION

COPY --from=builder /java/ /opt/ibm/java/

RUN microdnf -y update \
    && microdnf -y install hostname openssl tar vi which \
    && microdnf -y clean all

ENV JAVA_HOME=/opt/ibm/java/jre \
    PATH=/opt/ibm/java/jre/bin:/opt/ibm/java/bin:$PATH \
    IBM_JAVA_OPTIONS="-XX:+UseContainerSupport"

LABEL JAVA_VERSION=${JAVA_VERSION}
