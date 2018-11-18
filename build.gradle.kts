import java.net.URL
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet

plugins {
  java
  jacoco
  id("org.openstreetmap.josm") version "0.5.3"
  id("com.github.ben-manes.versions") version "0.20.0"
}

object Versions {
  const val awaitility = "3.1.2"
  const val jacoco = "0.8.2"
  const val gradleJosmPlugin = "0.5.3"
  const val junit = "5.3.1"
  const val wiremock = "2.19.0"
}

repositories {
  jcenter()
}
dependencies {
  testImplementation("org.openstreetmap.josm:josm-unittest:SNAPSHOT"){isChanging=true}
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
  testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
  testImplementation("org.junit.vintage:junit-vintage-engine:${Versions.junit}")
  testImplementation("com.github.tomakehurst:wiremock:${Versions.wiremock}")
  testImplementation("org.awaitility:awaitility:${Versions.awaitility}")
}

tasks.withType(JavaCompile::class) {
  options.compilerArgs.addAll(
    arrayOf("-Xlint:all", "-Xlint:-serial")
  )
}

josm {
  versionWithoutLeadingV = true
  manifest {
    oldVersionDownloadLink(14149, "2.1.6", URL("https://github.com/JOSM/pt_assistant/releases/download/v2.1.6/pt_assistant.jar"))
    oldVersionDownloadLink(14027, "v2.1.4", URL("https://github.com/JOSM/pt_assistant/releases/download/v2.1.4/pt_assistant.jar"))
    oldVersionDownloadLink(13957, "v2.0.0", URL("https://github.com/JOSM/pt_assistant/releases/download/v2.0.0/pt_assistant.jar"))
  }
  i18n {
    pathTransformer = getPathTransformer("github.com/JOSM/pt_assistant/blob")
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
    include("README")
    include("LICENSE")
  }
}

jacoco {
  toolVersion = Versions.jacoco
}

val jacocoTestReport: JacocoReport by tasks
jacocoTestReport.apply {
  reports {
    xml.isEnabled = true
    html.isEnabled = true
  }
}
tasks["check"].dependsOn(jacocoTestReport)
jacocoTestReport.dependsOn(tasks["test"])
