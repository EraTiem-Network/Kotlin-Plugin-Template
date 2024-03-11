import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.run.velocity)
}

dependencies {
  compileOnly(libs.minecraft.proxy.velocity)
  kapt(libs.minecraft.proxy.velocity)

  findProject(":util")?.let { implementation(it) }
}

rootProject.publishing {
  publications {
    val block: MavenPublication.() -> Unit = {
      artifact(tasks.shadowJar) {
        classifier = "velocity"
      }

      artifactId = rootProject.name
    }

    if (findByName("maven") != null) {
      named("maven", block)
    } else {
      create("maven", block)
    }
  }
}

tasks {
  runVelocity {
    velocityVersion(libs.versions.velocity.get())
  }

  register("replacePluginAnnotation") {
    val srcDir = project.sourceSets.main.get().kotlin.srcDirs.toList()[0]
    val fileNameRegex = Regex(".*Plugin\\.kt")

    srcDir
      .walk()
      .filter { it.isFile && fileNameRegex.matches(it.name) }
      .takeIf { it.count() != 0 }
      ?.first()
      ?.let { pluginFile ->

        val fileContent = pluginFile.readLines().joinToString("\n")
        val pluginAnnotationRegex = Regex("@Plugin\\((.|\\s)*\\)\\s*class", RegexOption.MULTILINE)

        pluginFile.writeText(
          fileContent.replaceFirst(
            pluginAnnotationRegex,
            "@Plugin(\n${pluginAnnotationContent()}\n)\nclass".trimMargin()
          )
        )
      }
  }

  withType<KotlinCompile> {
    dependsOn("replacePluginAnnotation")
  }
}

sourceSets {
  main {
    java.setSrcDirs(listOf<String>())
  }
  test {
    java.setSrcDirs(listOf<String>())
  }
}

private fun pluginAnnotationContent(): String = mapOf(
  "id" to "\"${rootProject.name.lowercase()}\"",
  "name" to "\"${rootProject.name}\"",
  "version" to "\"${rootProject.version as String}\"",
  "description" to System.getProperty("proxy-plugin.description", null)?.let { "\"$it\"" },
  "authors" to System.getProperty("plugin.authors", null)
    ?.takeIf { it != "[]" }
    ?.replace("[", "[\n    \"")
    ?.replace(", ", "\",\n    \"")
    ?.replace("]", "\"\n  ]"),
  "dependencies" to System.getProperty("proxy-plugin.dependencies", null)
).filterValues { it != null }
  .map { (key, value) -> "  $key = $value" }
  .joinToString(",\n")