import java.io.File

import com.typesafe.config.ConfigFactory
import Dependencies._
import sbt.{Def, _}

val generatedFilePath: String = "/com/terradatum/dao/Tables.scala"
val flywayDbName: String = "admin"

val dbConf = settingKey[DbConf]("Typesafe config file with slick settings")
val dbConfName = settingKey[String]("The configuration name for the DbConf.")

inThisBuild(Seq(
  dbConfName in Test := "test.conf",
  dbConfName in Compile := "application.conf"
))

def executedCommandKey = Def.task {
  // A fully-qualified reference to a setting or task looks like {<build-uri>}<project-id>/config:intask::key
  state.value.history.current.takeWhile(c => !c.isWhitespace).split(Array('/', ':')).lastOption.getOrElse("")
}

def createDbConf(dbConfFile: File): DbConf = {
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


def generateTables(conf: DbConf, dependencyClasspath: Seq[Attributed[File]]) = Def.task {
  val outputDir = sourceManaged.value.getPath
  val fname = outputDir + generatedFilePath
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
        dependencyClasspath.files,
        Array(slickProfile, jdbcDriver, url, outputDir, pkg, username, password),
        streams.value.log
      )
    )
  } else ()

  Seq(file(fname))
}

def generatorsSetting: Seq[Setting[_]] =
  sourceGenerators += Def.taskDyn(generateTables((dbConf in flyway).value, dependencyClasspath.value)).taskValue

def allSourceGenerators: Seq[Setting[_]] =
  inConfig(Compile)(generatorsSetting) ++ inConfig(Test)(generatorsSetting)

lazy val `flyway-slick-codegen` = (project in file("."))
  .aggregate(flyway)
  .dependsOn(flyway)
  .settings(
    Common.settings,
    libraryDependencies ++= serverDependencies,
    allSourceGenerators
  )

def flywaySettings = Seq(
  dbConf := createDbConf((resourceDirectory in Compile).value / dbConfName.value),
  flywayUrl := dbConf.value.url,
  flywayUser := dbConf.value.user,
  flywayPassword := dbConf.value.password
)

def scopedFlywaySettings: Seq[Setting[_]] =
  inConfig(Compile)(flywaySettings) ++ inConfig(Test)(flywaySettings)

lazy val flyway = (project in file("flyway"))
  .settings(
    Common.settings ++ scopedFlywaySettings,
    name := "flyway-slick-codegen-flyway",
    description := "Flyway migrations for Test PostgreSQL",
    libraryDependencies ++= flywayDependencies,
    /*
     * flyway
     */
    flywaySchemas := Seq("test"),
    //flywayLocations := Seq("classpath:db/migration"),
    //flywayLocations := Seq("filesystem:flyway/src/main/resources/db/migration"),
    (test in Test) := (test in Test).dependsOn((flywayClean in Test).dependsOn(flywayMigrate in Test)).value
    //(compile in Compile) := (compile in Compile).dependsOn(flywayMigrate in Compile).value
  )
