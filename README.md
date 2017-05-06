# flyway-slick-codegen

# TL;DR:

I wanna:

* `compile` => run flyway migrations and generate slick code from `localhost:5432`
* `test:compile` => run flyway migrations and generate slick code from `localhost:5434`

## Commentary

SBT seems to fight me the whole way. I've spent days on this. I'm very frustrated.

While I am by no means the best developer on the planet, I felt like I had a pretty firm grasp of the in's and out's of 
SBT. What I thought I knew was garnered through extensive work trying to understand SBT, including building and 
maintaining a couple of SBT plugins as well as contributing to OSS SBT plugins:
* [sbt-docker-helper][1]
* [sbt-native-packager][2]

The level of attention required to solve apparently simple problems seems tremendously high.

Said another way: _Using SBT is really hard. Using SBT should be really easy._

# Overview
**NOTE: ALL NAMES HAVE BEEN CHANGED TO PROTECT THE INNOCENT**

This project is designed to illustrate the difficulty working with SBT, Flyway and Slick Codegen within SBT itself.

The problem is predicated on the idea that project needs two databases for development:
1. `test` - this database is essentially ephemeral, with the schema being created and destroyed frequently (in-memory)
2. `runtime` - this database is persistent, running more-or-less all the time

This is because a developer will be working with more than one version of the database - the "tip" or `HEAD` of the
database, where the current development is taking place, and the "build" or "release" version of the database, where
the artifacts for release are built.

When doing ongoing development, the developer will build Flyway migrations, in conjunction with Slick, and want to do 
development against the `test` database, building unit tests and creating and destroying the schema frequently.

When building artifacts for deployment, the developer will want to target the `runtime` database.

This is _very_ similar to the concept in Play of "Dev" vs "Prod" runtime behavior of SBT. I've therefore inspected and
tried to emulate various methods for treating this project in a similar way, without much luck.

# Project & Work

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

When the Flyway migrate tasks are wired to various tasks, they both get run, whether during `compile` or `test` tasks.

**NOTE: Unfortunately, when the Flyway tasks are wired up, this hits a deadlock problem, so the reproduction can't
really do a good job of illustrating this.**

 [1]: https://github.com/terradatum/sbt-docker-helper
 [2]: https://github.com/sbt/sbt-native-packager/pulls?q=is%3Apr+is%3Aclosed+author%3Arbellamy