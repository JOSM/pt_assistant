import java.net.URL
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.openstreetmap.josm.gradle.plugin.i18n.I18nSourceSet

plugins {
  java
  id("org.openstreetmap.josm") version "0.5.3"
  id("com.github.ben-manes.versions") version "0.20.0"
}

repositories {
  jcenter()
}
dependencies {
  testImplementation("org.openstreetmap.josm:josm-unittest:SNAPSHOT"){isChanging=true}
  val junitVersion = "5.3.1"
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
  testImplementation("org.junit.vintage:junit-vintage-engine:$junitVersion")
  testImplementation("com.github.tomakehurst:wiremock:2.19.0")
  testImplementation("org.awaitility:awaitility:3.1.2")
}

tasks.withType(JavaCompile::class) {
  options.compilerArgs.addAll(
    arrayOf("-Xlint:all", "-Xlint:-serial")
  )
}

josm {
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
  getByName("main") {
    java {
      setSrcDirs(setOf("$projectDir/src"))
    }
    withConvention(I18nSourceSet::class) {
      po {
        setSrcDirs(setOf("$projectDir/poSrc"))
      }
    }
  }
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
