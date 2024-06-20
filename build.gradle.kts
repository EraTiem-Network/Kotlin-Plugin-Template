import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.lang.System.getProperty
import java.lang.System.getenv
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

// ###############
// # Maven Repos #
// ###############
buildscript {
  dependencies {
    classpath(libs.kotlinx.serialization.core)
    classpath(libs.kaml)
  }
  repositories.mavenCentral()
}

val repoList = File("maven-repos.yml").takeIf(File::exists)?.inputStream()?.use { stream ->
  Yaml.default.decodeFromStream<List<Map<String, String?>>>(stream).map {
    MavenRepo(
      it["name"]!!,
      URI(it["url"]!!),
      it["username"],
      it["password"],
      it["publish"]?.toBooleanStrictOrNull() ?: false
    )
  }
}


// ##############
// # Publishing #
// ##############

publishing {
  repositories {
    val isSnapshot = version.toString().contains("SNAPSHOT", true)

    repoList?.filter { it.publish }
      ?.filter {
        if (isSnapshot)
          it.name.contains("SNAPSHOT", true)
        else
          !it.name.contains("SNAPSHOT", true)
      }
      ?.forEach { createRepository(it) }
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
    repoList?.forEach { createRepository(it) }
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

fun RepositoryHandler.createRepository(repo: MavenRepo) {
  maven {
    name = repo.name.replace(Regex("[^A-Za-z0-9_\\-. ]"), "").replace(Regex("\\s+"), "_")
    url = repo.url

    val envName = name.replace(Regex("\\."), "").uppercase()
    val user = repo.username ?: getenv("${envName}_USERNAME")
    val pass = repo.password ?: getenv("${envName}_PASSWORD")

    if (!user.isNullOrBlank() || !pass.isNullOrBlank()) {
      credentials {
        if (!user.isNullOrBlank()) username = user
        if (!pass.isNullOrBlank()) password = pass
      }
    }
  }
}

data class MavenRepo(
  val name: String,
  val url: URI,
  val username: String? = null,
  val password: String? = null,
  val publish: Boolean = false
)