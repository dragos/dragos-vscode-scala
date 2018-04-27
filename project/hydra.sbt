credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
resolvers += Resolver.url("Hydra Artifactory Releases",
  url("https://repo.triplequote.com/artifactory/sbt-plugins-release/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.triplequote" % "sbt-hydra" % "0.9.11")
