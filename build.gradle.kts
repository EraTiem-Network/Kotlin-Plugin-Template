import java.net.URI

group = "net.eratiem"
version = "0.1-SNAPSHOT"

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.shadow) apply false
  idea
}

subprojects {
  apply {
    plugin(rootProject.libs.plugins.kotlin.jvm.get().pluginId)
    plugin(rootProject.libs.plugins.shadow.get().pluginId)
  }
}

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
    jvmToolchain(17)
  }
}

fun <T> getPropertyOrNull(key: String, castFun: String.() -> T) = try {
  System.getProperty(key).castFun()
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