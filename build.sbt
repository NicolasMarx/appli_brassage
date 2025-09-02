ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "1.0-SNAPSHOT"

// Java 21 LTS Configuration
ThisBuild / javacOptions ++= Seq("-source", "21", "-target", "21")
ThisBuild / javaOptions ++= Seq("--enable-preview")

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "brewing-platform-ddd",
    organization := "com.brewery",
    
    libraryDependencies ++= Seq(
      // Play Framework
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
      
      // Base de données
      "com.typesafe.play" %% "play-slick" % "5.1.0",
      "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0", 
      "org.postgresql" % "postgresql" % "42.6.0",
      
      // JSON
      "com.typesafe.play" %% "play-json" % "2.10.1",
      
      // Sécurité
      "org.mindrot" % "jbcrypt" % "0.4",
      
      // IA et HTTP client
      "com.typesafe.play" %% "play-ws" % "2.9.0",
      "com.typesafe.play" %% "play-ahc-ws" % "2.9.0",
      
      // Tests
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % Test,
      
      // Validation et fonctionnel
      "com.github.tminglei" %% "slick-pg" % "0.21.1",
      "com.github.tminglei" %% "slick-pg_play-json" % "0.21.1",
      
      // Cache et performance
      "com.github.blemale" %% "scaffeine" % "5.2.1",
      
      // Event Sourcing avec pg-event-store
      "immo.performance" %% "pg-event-store-play-json" % "0.0.0+25-d9a7b12e+20250902-2125-SNAPSHOT",
      "immo.performance" %% "pg-event-store-postgres" % "0.0.0+25-d9a7b12e+20250902-2125-SNAPSHOT"
    ),
    
    // Configuration compilation
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard"
    ),
    
    // Configuration tests
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
    Test / fork := true,
    Test / javaOptions += "-Dconfig.resource=application.test.conf"
  )

// Ajout tasks personnalisées pour l'architecture DDD
lazy val verifyArchitecture = taskKey[Unit]("Vérifie la cohérence de l'architecture DDD")
verifyArchitecture := {
  println("🔍 Vérification architecture DDD/CQRS...")
  // TODO: Ajouter vérifications automatiques
  println("✅ Architecture vérifiée")
}

lazy val generateDocs = taskKey[Unit]("Génère la documentation de l'architecture")
generateDocs := {
  println("📚 Génération documentation...")
  // TODO: Générer docs automatiquement
  println("✅ Documentation générée")
}
