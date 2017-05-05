#!/usr/bin/env bash
PG_DOCKER_CONTAINER_ID=`docker ps -a -f "name=postgres-test$" -q`
[[ -z "${PG_DOCKER_CONTAINER_ID}" ]] && PG_DOCKER_CONTAINER_ID=`docker run -d --name postgres-test --tmpfs=/pgtmpfs:rw,size=50m,mode=1777 -p 5434:5432 -e PGDATA=/pgtmpfs postgres` || docker start "${PG_DOCKER_CONTAINER_ID}"
docker cp create-role.sql ${PG_DOCKER_CONTAINER_ID}:/
docker cp create-schema.sql ${PG_DOCKER_CONTAINER_ID}:/
docker cp drop-role.sql ${PG_DOCKER_CONTAINER_ID}:/
docker cp create.sh ${PG_DOCKER_CONTAINER_ID}:/
docker exec ${PG_DOCKER_CONTAINER_ID} /bin/bash -c "./create.sh"