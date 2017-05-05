# flyway-slick-codegen-test

**NOTE: ALL NAMES HAVE BEEN CHANGED TO PROTECT THE INNOCENT**

This project is designed to illustrate the difficulty working with SBT, Flyway and Slick Codegen within SBT itself.

The problem is predicated on the idea that project needs two databases:
1. `test` - this database is essentially ephemeral, with the schema being created and destroyed frequently (in-memory)
2. `runtime` - this database is persistent, running more-or-less all the time

This is because a developer will be working with more than one version of the database - the "tip" or `HEAD` of the
database, where the current development is taking place, and the "build" or "release" version of the database, where
the artifacts for release are built.

When doing ongoing development, the developer will build Flyway migrations, in conjunction with Slick, and want to do 
development against the `test` database, building unit tests and creating and destroying the schema frequently.

When building artifacts for deployment, the developer will want to target the `runtime` database.

## Setup

Create both the `runtime` and the `test` databases in docker (note that the `test` DB uses `tmpfs` for `PGDATA`).

```bash
cd flyway/src/main/resources/db/migration
./docker-create.sh # runtime in docker
./docker-create-test.sh # test in docker
```

This _should_ create two "user" tables and one Flyway table:
1. AERGO.PROFILE
2. AERGO.MEMBERSHIP
3. AERGO.SCHEMA_VERSIONS

## Desired behavior

There are three environment where this build file will be run:
1. LOCAL developer, working in `test` -> this uses SBT `Test`
2. LOCAL developer, working in `runtime` -> this uses SBT `Compile`
3. CI Server, working in `runtime` -> this uses SBT `Compile`

The following SBT commands should produce the results shown after the fat arrow:
1. `sbt test` => `flywayClean in Test` + `flywayMigrate in Test` + `genTables in Test` + `test`. During `test`, Flyway 
migrations target the `test` database, and then Slick codegen is run against that same `test` database.
2. `sbt compile` => `flywayMigrate` + `genTables` + `compile`. During `runtime`, Flyway migrations target the `runtime` 
database, and then Slick codegen is run against that same `runtime` database.

As an added complication, the Flyway migrations project should ship as a resource JAR with the main project, thus allowing
for runtime migration of databases via code (NOT INCLUDED).

## Attempted solution

Two configuration files, `application.conf` for the `runtime` DB connection, and `test.conf` for the `test` DB.

Each are loaded into a separate instance of the `SettingKey[DbConf]` via the TypeSafe `ConfigFactory.load` run inside
the `build.sbt` - `dbConf` and `dbConf in Test` respectively.

These DB connection objects are then used by both Flyway and Slick for migration and code generation.

There are so many ways in which this has NOT worked, I'm at a loss as to how to even describe the things I've tried to 
make it work. 

# The Problem 

While it took some time to setup the problem here, it's essentially a pretty simple issue - I want to target one DB for 
testing and another for runtime. I need to do this from my build tool.

When the two codegen tasks are wired to `sourceGenerators`, this means they BOTH get run during test.

When the Flyway migrate tasks are wired to various tasks, they both get run, whether during `Compile` or `Test`.

