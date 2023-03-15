import java.net.URL
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  java
  jacoco
  id("org.openstreetmap.josm") version "0.8.2"
}

object Versions {
  const val jacoco = "0.8.8"
  const val junit = "5.9.2"
}

repositories {
  jcenter()
}
dependencies {
  testImplementation("org.openstreetmap.josm:josm-unittest:SNAPSHOT"){isChanging=true}
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
  testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
}

tasks.withType(JavaCompile::class) {
  options.compilerArgs.addAll(
    arrayOf("-Xlint:all", "-Xlint:-serial")
  )
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
  withSourcesJar()
  withJavadocJar()
}

josm {
  manifest {
    oldVersionDownloadLink(17903, "2.1.15", URL("https://github.com/JOSM/pt_assistant/releases/download/v2.1.15/pt_assistant.jar"))
    oldVersionDownloadLink(14149, "2.1.6", URL("https://github.com/JOSM/pt_assistant/releases/download/v2.1.6/pt_assistant.jar"))
    oldVersionDownloadLink(14027, "v2.1.4", URL("https://github.com/JOSM/pt_assistant/releases/download/v2.1.4/pt_assistant.jar"))
    oldVersionDownloadLink(13957, "v2.0.0", URL("https://github.com/JOSM/pt_assistant/releases/download/v2.0.0/pt_assistant.jar"))
  }
  i18n {
    pathTransformer = getPathTransformer(project.projectDir, "github.com/JOSM/pt_assistant/blob")
  }
}

tasks.withType(Test::class) {
  testLogging.exceptionFormat = TestExceptionFormat.FULL
}

sourceSets {
  getByName("test") {
    java {
      setSrcDirs(setOf("test/unit"))
    }
    resources {
      setSrcDirs(setOf("test/data"))
    }
  }
}
tasks.withType(ProcessResources::class).getByName(sourceSets["main"].processResourcesTaskName) {
  from(projectDir) {
    include("images/**")
    include("GPL-*")
    include("README.md")
    include("LICENSE")
  }
}

jacoco {
  toolVersion = Versions.jacoco
}

tasks.jacocoTestReport {
  reports {
    xml.isEnabled = true
    html.isEnabled = true
  }
  dependsOn(tasks.test)
  tasks.check.get().dependsOn(this)
}
