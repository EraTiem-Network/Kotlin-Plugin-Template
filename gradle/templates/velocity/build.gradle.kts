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

tasks {
  runVelocity {
    velocityVersion(libs.versions.velocity.get())
  }

  register("replacePluginAnnotation") {
    val srcDir = project.sourceSets.main.get().kotlin.srcDirs.toList()[0]
    val fileNameRegex = Regex(".*Plugin\\.kt")

    val pluginFile = srcDir
      .walk()
      .filter { it.isFile && fileNameRegex.matches(it.name) }
      .first()

    val fileContent = pluginFile.readLines().joinToString("\n")
    val pluginAnnotationRegex = Regex("@Plugin\\((.|\\s)*\\)", RegexOption.MULTILINE)

    pluginFile.writeText(
      fileContent.replace(
        pluginAnnotationRegex,
        "@Plugin(\n${pluginAnnotationContent()}\n)".trimMargin()
      )
    )
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