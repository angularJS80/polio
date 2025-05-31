#!/bin/bash

docker run -d \
  --name keycloak \
  -p 8089:8089 \
  -e KEYCLOAK_ADMIN=keycloakadmin \
  -e KEYCLOAK_ADMIN_PASSWORD=keycloakadmin \
  -e KC_DB=mysql \
  -e KC_DB_URL_HOST=host.docker.internal \
  -e KC_DB_URL_PORT=3306 \
  -e KC_DB_URL_DATABASE=keycloak \
  -e KC_DB_USERNAME=keycloak \
  -e KC_DB_PASSWORD=keycloak \
  -v /Users/admin/docker-volume/keycloak-data:/opt/keycloak/data \
  quay.io/keycloak/keycloak:24.0.3 \
  start-dev
