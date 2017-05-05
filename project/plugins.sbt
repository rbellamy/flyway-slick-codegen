logLevel := Level.Warn
resolvers += "Flyway" at "https://flywaydb.org/repo"

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")
addSbtPlugin("net.virtual-void" % "sbt-optimizer" % "0.1.2")
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.1.2")
