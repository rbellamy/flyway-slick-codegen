import java.io.File
import com.typesafe.config.ConfigFactory

import Dependencies._
import sbt._

val generatedAergoFilePath: String = "/com/terradatum/dao/Tables.scala"
val flywayDbName: String = "admin"

lazy val dbConf = settingKey[DbConf]("Typesafe config file with slick settings")
lazy val genTables = taskKey[Seq[File]]("Generate slick code in Compile")

dbConf := {
  val configFactory = ConfigFactory.parseFile((resourceDirectory in Compile).value / "application.conf")
  val configPath = s"$flywayDbName"
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
  val configPath = s"$flywayDbName"
  val config = configFactory.getConfig(configPath).resolve
  DbConf(
    config.getString("profile"),
    config.getString("db.driver"),
    config.getString("db.url"),
    config.getString("db.user"),
    config.getString("db.password")
  )
}

def genTablesTask(
    dbConf: SettingKey[DbConf],
    path: String,
    sourceManaged: SettingKey[File],
    dependencyClasspath: TaskKey[Keys.Classpath],
    runner: TaskKey[ScalaRun],
    streams: TaskKey[Keys.TaskStreams]
) = Def.task {
  val outputDir = sourceManaged.value.getPath
  val fname = outputDir + path
  if (!file(fname).exists()) {
    val generator = "slick.codegen.SourceCodeGenerator"
    val url = dbConf.value.url
    val slickProfile = dbConf.value.profile.dropRight(1)
    val jdbcDriver = dbConf.value.driver
    val pkg = "com.terradatum.dao"
    val username = dbConf.value.user
    val password = dbConf.value.password
    toError(
      runner.value.run(
        generator,
        dependencyClasspath.value.files,
        Array(slickProfile, jdbcDriver, url, outputDir, pkg, username, password),
        streams.value.log
      )
    )
  }
  Seq(file(fname))
}

val genTablesTaskCompile = genTablesTask(
  dbConf,
  generatedAergoFilePath,
  sourceManaged in Compile,
  dependencyClasspath in Compile,
  runner,
  streams
)

val genTablesTaskTest = genTablesTask(
  dbConf in Test,
  generatedAergoFilePath,
  sourceManaged in Test,
  dependencyClasspath in Test,
  runner,
  streams
)

lazy val `flyway-slick-codegen-test` = (project in file("."))
  .settings(Common.settings: _*)
  .settings(
    libraryDependencies ++= serverDependencies,
    genTables in (Compile, compile) := genTablesTaskCompile.value,
    genTables in (Test, compile) := genTablesTaskTest.value,
    sourceGenerators in (Compile, compile) += genTablesTaskCompile.taskValue,
    sourceGenerators in (Test, compile) += genTablesTaskTest.taskValue
  )
  .aggregate(flyway)
  .dependsOn(flyway)

dbConf in flyway := dbConf.value
dbConf in flyway in Test := (dbConf in Test).value

lazy val flywaySettings = Seq(
  flywayUrl := dbConf.value.url,
  flywayUser := dbConf.value.user,
  flywayPassword := dbConf.value.password
)

lazy val flywaySettingsTest = Seq(
  flywayUrl in Test := (dbConf in Test).value.url,
  flywayUser in Test := (dbConf in Test).value.user,
  flywayPassword in Test := (dbConf in Test).value.password
)

lazy val flyway = (project in file("flyway"))
  .settings(
    Common.settings ++ flywaySettings ++ inConfig(Test)(flywaySettingsTest),
    name := "flyway-slick-codegen-test-flyway",
    description := "Flyway migrations for Aergo PostgreSQL",
    libraryDependencies ++= flywayDependencies,
    /*
     * flyway
     */
    flywaySchemas := Seq("aergo"),
    //flywayLocations := Seq("classpath:db/migration"),
    //flywayLocations := Seq("filesystem:flyway/src/main/resources/db/migration"),
    (test in Test) := (test in Test).dependsOn((flywayClean in Test).dependsOn(flywayMigrate in Test)).value
    //(compile in Compile) := (compile in Compile).dependsOn(flywayMigrate in Compile).value
  )
