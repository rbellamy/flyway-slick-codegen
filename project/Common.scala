import sbt.Def
import sbt.Keys._

object Common {
  val settings: Seq[Def.Setting[String]] = Seq(
    organizationName := "Terradatum",
    organization := "com.terradatum",
    name := "aergo-server",
    description := "Aergo API",
    version := "0.5.0-SNAPSHOT",
    scalaVersion := "2.11.8"
  )
}