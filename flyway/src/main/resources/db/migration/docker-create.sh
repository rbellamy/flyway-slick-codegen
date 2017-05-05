#!/usr/bin/env bash
PG_DOCKER_CONTAINER_ID=`docker ps -a -f "name=postgres$" -q`
[[ -z "${PG_DOCKER_CONTAINER_ID}" ]] && PG_DOCKER_CONTAINER_ID=`docker run -d --name postgres -p 5432:5432 postgres` || docker start "${PG_DOCKER_CONTAINER_ID}"
docker cp create-role.sql ${PG_DOCKER_CONTAINER_ID}:/
docker cp create-schema.sql ${PG_DOCKER_CONTAINER_ID}:/
docker cp drop-role.sql ${PG_DOCKER_CONTAINER_ID}:/
docker cp create.sh ${PG_DOCKER_CONTAINER_ID}:/
docker exec ${PG_DOCKER_CONTAINER_ID} /bin/bash -c "./create.sh"
