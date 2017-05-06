#!/usr/bin/env bash
while ! dropdb --if-exists -U postgres test; do sleep 5; done
psql -U postgres -f drop-role.sql
psql -U postgres -f create-role.sql
createdb -U postgres test -O test
psql -U postgres -d test -f create-schema.sql