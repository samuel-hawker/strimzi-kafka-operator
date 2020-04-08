#!/bin/sh
echo 'Generate Event Streams certificates'
echo '==================================='

scriptDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

ROOT_COMMON_CA="HYC Root CA"
ES_CERT_COMMON_NAME="IBM Event Streams"
SSL_CONFIG_FILE="$scriptDir/openssl.cnf.template"

removeFile() {
  if [ -f $1 ]; then
    echo "Removing $1"
    `rm $1`
  fi
}

if [ -z "$1" ]
  then
    echo "Usage: ./createCerts --ip <cluster-ip> --dns <DNS name> --gen-ca --ca-common-name <Root CA Common Name> --cert-common-name <Event Streams identifier> --ssl-config <openssl config template>" 
    exit 1
fi

while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    --ip)
    CLUSTER_IP="$2"
    echo "Setting Cluster IP: $2"
    shift # past argument
    shift # past value
    ;;
   --dns)
    DNS_NAME="$2"
    echo "Setting DNS Name: $2"
    shift # past argument
    shift # past value
    ;;
    --gen-ca)
    GENERATE_CA="true"
    echo "Setting CA Certificate generation"
    shift # past argument
    ;;
    --ca-common-name)
    ROOT_COMMON_CA="$2"
    echo "Setting Root common name: $2"
    shift # past argument
    shift # past argument
    ;;
    --cert-common-name)
    ES_CERT_COMMON_NAME="$2"
    echo "Setting Certificate common name: $2"
    shift # past argument
    shift # past value
    ;;
    --ssl-config)
    SSL_CONFIG_FILE="$2"
    echo "Setting SSL Config File: $2"
    shift # past argument
    shift # past value
    ;;
    *)    # unknown option
    echo "Unknown property: $1"
    shift # past argument
    ;;
esac
done

echo "IPAddress: $CLUSTER_IP"
echo "DNS Name: $DNS_NAME"
echo "Root CA Common Name: $ROOT_COMMON_CA"
echo "Event Streams cert Common Name: $ES_CERT_COMMON_NAME"
echo "Generate CA Certificate: $GENERATE_CA"

echo "Removing previous keys and config"
removeFile "tls.key"
removeFile "tls.crt"
removeFile "tls.csr"

removeFile "ca.srl"
removeFile "openssl.cnf"

removeFile "tls.pfx"
removeFile "keystore.jks"
removeFile "truststore.jks"

CLUSTER_DNS_NAMES=""
CLUSTER_IP_ADDRESSES=""
SUBJECT_ALTERNATE_NAMES=""

if [ ! -z $DNS_NAME ]; then
  CLUSTER_DNS_NAMES="DNS.1 = $DNS_NAME"
  SUBJECT_ALTERNATE_NAMES="subjectAltName = @alt_names"
fi

if [ ! -z $CLUSTER_IP ]; then
  CLUSTER_IP_ADDRESSES="IP.1 = $CLUSTER_IP"
  SUBJECT_ALTERNATE_NAMES="subjectAltName = @alt_names"
fi

sed -e "s|CLUSTER_DNS_NAMES|${CLUSTER_DNS_NAMES}|g" \
  -e "s|CLUSTER_IP_ADDRESSES|${CLUSTER_IP_ADDRESSES}|g" \
  -e "s|SUBJECT_ALTERNATE_NAMES|${SUBJECT_ALTERNATE_NAMES}|g" \
  "${SSL_CONFIG_FILE}" > openssl.cnf

if [ ! -z $GENERATE_CA ]; then
  removeFile "ca.key"
  removeFile "ca.crt"
  echo "Generating CA Certificate"
  `openssl genrsa -out ca.key 2048`
  `openssl req -key ca.key -nodes -new -x509 -days 3650 -sha256 -out ca.crt -subj "/C=GB/ST=Hampshire/L=Winchester/O=IBM UK Ltd/OU=Hybrid Cloud/CN=$ROOT_COMMON_CA" -config openssl.cnf -extensions v3_ca`
else
  if [ -f "ca.key" ] && [ -f "ca.crt" ]; then
    echo "Using existing CA Certificate to generate cert"
  else
    echo "No ca.key and/or ca.crt found. Unable to generate certs with existing CA"
    exit 1
  fi
fi

`openssl genrsa -out tls.key 2048`

`openssl req -key tls.key -new -sha256 -out tls.csr  -config openssl.cnf -subj "/C=GB/ST=Hampshire/L=Winchester/O=IBM UK Ltd/OU=DUMMY ICP/CN=$ES_CERT_COMMON_NAME"`

`openssl x509 -req -in tls.csr -CA ca.crt -CAkey ca.key -out tls.crt -days 365 -CAcreateserial -extfile openssl.cnf -extensions v3_req`

echo "Creating truststore"
keytool -import -file ca.crt -alias ESCA -keystore truststore.jks -storepass password -keypass password -noprompt -trustcacerts
echo "Creating pkcs12"
openssl pkcs12 -export -passin pass:"password" -passout pass:"password" -inkey tls.key  -in tls.crt -out tls.pfx
echo "Creating keystore"
keytool -importkeystore -srckeystore tls.pfx -srcstoretype pkcs12 -srcstorepass password -destkeystore keystore.jks -deststorepass password


echo "CA certificate"
echo "--------------"
echo "$(openssl base64 -in ca.crt)" | tr -d '\n'
echo "\n"

echo "TLS certificate"
echo "---------------"
echo "$(openssl base64 -in tls.crt)" | tr -d '\n'
echo "\n"

echo "TLS Key"
echo "-------"
echo "$(openssl base64 -in tls.key)" | tr -d '\n'
echo "\n"

echo "TrustStore"
echo "-------"
echo "$(cat truststore.jks | base64)"
echo "\n"

echo "KeyStore"
echo "-------"
echo "$(cat keystore.jks | base64)"
echo "\n"

removeFile "tls.csr"
removeFile "ca.srl"
removeFile "tls.pfx"
removeFile "openssl.cnf"
