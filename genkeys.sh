#!/bin/bash

mkdir data

dd if=/dev/urandom of=data/file.bin bs=64M count=2 iflag=fullblock

openssl req \
-subj "/C=NO/ST=Oslo/L=Oslo/CN=nav.no" \
-newkey rsa:8192 -x509 \
-keyout data/private_key.pem \
-out data/public_key.crt \
-days 36500 -passout pass:changeit

openssl dgst -sha1 -sign data/private_key.pem -out data/file.bin.sha1 -passin pass:changeit data/file.bin

openssl pkcs12 -export -name perftest-private -inkey data/private_key.pem -out data/keystore.p12 -name perftest-public -in data/public_key.crt -out data/keystore.p12 -passin pass:changeit -passout pass:changeit
