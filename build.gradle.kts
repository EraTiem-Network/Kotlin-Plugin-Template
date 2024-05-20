import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.lang.System.getProperty
import java.net.URI

group = "net.eratiem"
version = "0.1-SNAPSHOT"

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.shadow) apply false
  idea
  `maven-publish`
}

// ###############
// # Subprojects #
// ###############

subprojects {
  group = rootProject.group
  version = rootProject.version

  apply {
    plugin(rootProject.libs.plugins.kotlin.jvm.get().pluginId)
    plugin(rootProject.libs.plugins.shadow.get().pluginId)
    plugin("maven-publish")
  }

  tasks {
    named<ShadowJar>("shadowJar") {
      archiveBaseName = rootProject.name
      archiveClassifier = this@subprojects.name.takeIf { it != "util" } ?: ""

      dependencies {
        exclude(dependency(rootProject.libs.kotlin.stdlib.get()))
      }
    }
  }
}


// ##############
// # Publishing #
// ##############

publishing {
  repositories {
    val repoNames = System.getProperties().filterKeys { (it as String).startsWith("project.publish", true) }
      .map { (it.key as String).removePrefix("project.publish.").substringBefore(".") }.toSet()

    repoNames.forEach {
      maven {
        name = it.uppercaseFirstChar()
        url = uri(getProperty("project.publish.$it.url"))

        getProperty("project.publish.$it.auth-type")?.let { authType ->
          when (authType) {
            else -> {
              credentials {
                username = "${properties["project.publish.$it.username"]}"
                password = "${properties["project.publish.$it.access-token"]}"
              }
            }
          }
        }
      }
    }
  }
}


// ################
// # All Projects #
// ################

allprojects {
  apply {
    plugin("org.gradle.idea")
  }

  repositories {
    maven {
      name = "Bit-Build | Artifactory"
      url = URI("https://artifactory.bit-build.de/artifactory/public")
    }
  }

  tasks {
    idea {
      module {
        isDownloadSources = true
        isDownloadJavadoc = true
      }
    }

    jar {
      enabled = false
    }
  }

  kotlin {
    jvmToolchain(21)
  }
}


// ###############
// # Source-Sets #
// ###############

sourceSets {
  main {
    java.setSrcDirs(listOf<String>())
    kotlin.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
  }

  test {
    java.setSrcDirs(listOf<String>())
    kotlin.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
  }
}

fun <T> getPropertyOrNull(key: String, castFun: String.() -> T) = try {
  getProperty(key).castFun()
} catch (ex: Exception) {
  println(ex)
  null
}

fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit) =
  if (condition) {
    apply { block() }
  } else {
    this
  }