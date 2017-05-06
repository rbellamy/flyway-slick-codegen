import sbt.Keys.libraryDependencies
import sbt._

object Dependencies {

  val jdbcDependencies: Seq[ModuleID] = Seq(
    "org.postgresql" % "postgresql" % "42.0.0"
  )

  val slick: ModuleID = "com.typesafe.slick" %% "slick" % "3.2.0"

  val flywayDependencies: Seq[ModuleID] = Seq(
    slick,
    "org.scaldi"         %% "scaldi"        % "0.5.8",
    "org.flywaydb"       % "flyway-core"    % "3.1",
    "com.typesafe.slick" %% "slick-codegen" % "3.2.0",
    "com.typesafe"       % "config"         % "1.3.1"
  ) ++ jdbcDependencies

  val serverDependencies: Seq[ModuleID] = Seq(slick) ++ jdbcDependencies
}
