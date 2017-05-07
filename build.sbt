import java.io.File

import com.typesafe.config.ConfigFactory
import Dependencies._
import sbt.{Def, _}

val generatedTestFilePath: String = "/com/terradatum/dao/Tables.scala"
val flywayDbName: String = "admin"

val isTestMode = settingKey[Boolean]("Test mode")
val dbConf = settingKey[DbConf]("Typesafe config file with slick settings")
val genTables = taskKey[Seq[File]]("Generate slick code")
val genTablesTest = taskKey[Seq[File]]("Generate slick code in Test")

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

val genTablesTask: (SettingKey[DbConf]) => Def.Initialize[Task[Seq[File]]] = (dbConf: SettingKey[DbConf]) =>
  Def.task {
    val path = generatedTestFilePath
    val outputDir = (sourceManaged in Compile).value.getPath
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
          (dependencyClasspath in Compile).value.files,
          Array(slickProfile, jdbcDriver, url, outputDir, pkg, username, password),
          streams.value.log
        )
      )
    }
    Seq(file(fname))
}

val genTablesTaskCompile: Def.Initialize[Task[Seq[File]]] = genTablesTask(dbConf)

val genTablesTaskTest: Def.Initialize[Task[Seq[File]]] = genTablesTask(dbConf in Test)

def codegen = Def.taskDyn {
  if (isTestMode.value) {
    genTablesTaskTest
  } else {
    genTablesTaskCompile
  }
}

lazy val `flyway-slick-codegen` = (project in file("."))
  .settings(
    Common.settings,
    libraryDependencies ++= serverDependencies,
    isTestMode := false,
    genTables := codegen.value,
    sourceGenerators in Compile += codegen.taskValue
  )
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
    name := "flyway-slick-codegen-flyway",
    description := "Flyway migrations for Test PostgreSQL",
    libraryDependencies ++= flywayDependencies,
    /*
     * flyway
     */
    flywaySchemas := Seq("test"),
    flywayLocations := Seq("classpath:db/migration")
    //flywayLocations := Seq("filesystem:flyway/src/main/resources/db/migration"),
    //(test in Test) := (test in Test).dependsOn((flywayClean in Test).dependsOn(flywayMigrate in Test)).value
    //(compile in Compile) := (compile in Compile).dependsOn(flywayMigrate in Compile).value
  )
