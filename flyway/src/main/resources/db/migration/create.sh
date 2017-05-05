#!/usr/bin/env bash
while ! dropdb --if-exists -U postgres aergo; do sleep 5; done
psql -U postgres -f drop-role.sql
psql -U postgres -f create-role.sql
createdb -U postgres aergo -O aergo
psql -U postgres -d aergo -f create-schema.sql