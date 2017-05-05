import java.io.File
import com.typesafe.config.ConfigFactory

import Dependencies._
import sbt._

val generatedAergoFilePath: String = "/com/terradatum/dao/Tables.scala"
val flywayDbName: String = "admin"

lazy val dbConf = settingKey[DbConf]("Typesafe config file with slick settings")
lazy val genTables = taskKey[Seq[File]]("Generate slick code")

dbConf := {
  val configFactory = ConfigFactory.parseFile((resourceDirectory in Compile).value / "application.conf")
  val configPath = s"admin"
  val config = configFactory.getConfig(configPath).resolve
  DbConf(
    config.getString("profile"),
    config.getString("db.driver"),
    config.getString("db.url"),
    config.getString("db.user"),
    config.getString("db.password")
  )
}

dbConf in Test := {
  val configFactory = ConfigFactory.parseFile((resourceDirectory in Compile).value / "test.conf")
  val configPath = s"admin"
  val config = configFactory.getConfig(configPath).resolve
  DbConf(
    config.getString("profile"),
    config.getString("db.driver"),
    config.getString("db.url"),
    config.getString("db.user"),
    config.getString("db.password")
  )
}

dbConf in flyway := dbConf.value
dbConf in flyway in Test := (dbConf in Test).value

lazy val `flyway-slick-codegen-test` = (project in file("."))
  .settings(Common.settings: _*)
  .settings(
    libraryDependencies ++= serverDependencies,
    genTables in Compile := genTablesTask.value,
    genTables in Test := genTablesTaskTest.value,
    sourceGenerators in Compile += genTablesTask.taskValue,
    sourceGenerators in Test += genTablesTaskTest.taskValue
  )
  .aggregate(flyway)
  .dependsOn(flyway)

lazy val flyway = (project in file("flyway"))
  .settings(
    Common.settings,
    name := "flyway-slick-codegen-test-flyway",
    description := "Flyway migrations for Aergo PostgreSQL",
    libraryDependencies ++= flywayDependencies,
    /*
     * flyway
     */
    flywaySchemas := Seq("aergo"),
    flywayLocations := Seq("classpath:db/migration"),
    flywayUrl := dbConf.value.url,
    flywayUser := dbConf.value.user,
    flywayPassword := dbConf.value.password,
    flywaySchemas in Test := Seq("aergo"),
    flywayLocations in Test := Seq("classpath:db/migration"),
    flywayUrl in Test := (dbConf in Test).value.url,
    flywayUser in Test := (dbConf in Test).value.user,
    flywayPassword in Test := (dbConf in Test).value.password
  )

lazy val genTablesTask = Def.task {
  val outputDir = (sourceManaged in Compile).value.getPath
  val fname = outputDir + generatedAergoFilePath
  val cp = (dependencyClasspath in Compile).value
  val r = (runner in Compile).value
  val s = streams.value

  if (!file(fname).exists()) {
    val generator = "slick.codegen.SourceCodeGenerator"
    val url = dbConf.value.url
    val slickProfile = dbConf.value.profile.dropRight(1)
    val jdbcDriver = dbConf.value.driver
    val pkg = "com.terradatum.dao"
    val username = dbConf.value.user
    val password = dbConf.value.password
    toError(
      r.run(generator, cp.files, Array(slickProfile, jdbcDriver, url, outputDir, pkg, username, password), s.log)
    )
  }
  Seq(file(fname))
}

lazy val genTablesTaskTest = Def.task {
  val outputDir = (sourceManaged in Compile).value.getPath
  val fname = outputDir + generatedAergoFilePath
  val cp = (dependencyClasspath in Compile).value
  val r = (runner in Compile).value
  val s = streams.value
  if (!file(fname).exists()) {
    val generator = "slick.codegen.SourceCodeGenerator"
    val url = (dbConf in Test).value.url
    val slickProfile = (dbConf in Test).value.profile.dropRight(1)
    val jdbcDriver = (dbConf in Test).value.driver
    val pkg = "com.terradatum.dao"
    val username = (dbConf in Test).value.user
    val password = (dbConf in Test).value.password
    toError(
      r.run(generator, cp.files, Array(slickProfile, jdbcDriver, url, outputDir, pkg, username, password), s.log)
    )
  }
  Seq(file(fname))
}
