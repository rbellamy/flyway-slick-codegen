import java.io.File

import com.typesafe.config.ConfigFactory
import Dependencies._
import sbt.{Def, _}

val generatedFilePath: String = "/com/terradatum/dao/Tables.scala"
val flywayDbName: String = "admin"

val dbConf = settingKey[DbConf]("Typesafe config file with slick settings")
val generateTables = taskKey[Seq[File]]("Generate slick code")

def createDbConf(dbConfFile: File): DbConf = {
  println (s"dbConfFile: $dbConfFile")
  val configFactory = ConfigFactory.parseFile(dbConfFile)
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

def generateTablesTask(conf: DbConf) = Def.task {
  val outputDir = (sourceManaged in Compile).value.getPath
  val fname = outputDir + generatedFilePath
  println (s"fname: $fname")
  if (!file(fname).exists()) {
    val generator = "slick.codegen.SourceCodeGenerator"
    val url = conf.url
    val slickProfile = conf.profile.dropRight(1)
    val jdbcDriver = conf.driver
    val pkg = "com.terradatum.dao"
    val username = conf.user
    val password = conf.password
    toError(
      runner.value.run(
        generator,
        (dependencyClasspath in Compile).value.files,
        Array(slickProfile, jdbcDriver, url, outputDir, pkg, username, password),
        streams.value.log
      )
    )
  } else ()

  Seq(file(fname))
}

def dbConfSettings = Seq(
  dbConf in Global := createDbConf((resourceDirectory in Compile).value / "application.conf")
)

lazy val `flyway-slick-codegen` = (project in file("."))
  .aggregate(flyway)
  .dependsOn(flyway)
  .settings(
    Common.settings ++ dbConfSettings,
    libraryDependencies ++= serverDependencies,
    sourceGenerators in Compile += Def.taskDyn(generateTablesTask((dbConf in Global).value)).taskValue
  )

def flywaySettings = Seq(
  flywayUrl := (dbConf in Global).value.url,
  flywayUser := (dbConf in Global).value.user,
  flywayPassword := (dbConf in Global).value.password
)

lazy val flyway = (project in file("flyway"))
  .settings(
    Common.settings ++ flywaySettings,
    name := "flyway-slick-codegen-flyway",
    description := "Flyway migrations for Test PostgreSQL",
    libraryDependencies ++= flywayDependencies,
    /*
     * flyway
     */
    flywayLocations := Seq("classpath:db/migration"),
    flywaySchemas := Seq("test")
  )