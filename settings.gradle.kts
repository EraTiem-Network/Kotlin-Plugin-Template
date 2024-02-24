import java.io.File.separator
import java.net.URI
import kotlin.io.OnErrorAction.SKIP

rootProject.name = "Kotlin-Plugin-Template"

projectSettings {

  activeModules {

  }

  serverPluginProperties {
    authors {

    }
    mainClass = "TemplatePlugin"
    description = "A simple Template-Plugin using Kotlin as main language"
    dependencies {
      add("KotlinProvider")
    }
    softDependencies {

    }
  }

  proxyPluginProperties {
    description = "A simple Template-Plugin using Kotlin as main language"
    dependencies {
      add("KotlinProvider")
    }
    softDependencies {

    }
  }
}

// ####################################################################################################################
// ####################################################################################################################
// ####################################################################################################################
// Following code is to get above working or general settings that should not be touched unless you know what you're doing

System.setProperty("kotlin.code.style", "official")
System.setProperty("kotlin.incremental", "true")

dependencyResolutionManagement {
  pluginManagement.repositories {
    maven {
      name = "Bit-Build | Artifactory"
      url = URI("https://artifactory.bit-build.de/artifactory/public")
    }
  }
}

private fun projectSettings(block: ProjectSettings.() -> Unit) {
  ProjectSettings().block()
}

private class ProjectSettings {
  init {
    extra["create-util-lib-jar"] = false
  }

  private val serverPluginProperties = ServerPluginProperties()
  private val proxyPluginProperties = ProxyPluginProperties()

  /**
   * Possible activations:
   * ```kotlin
   * folia()
   * paper()
   * velocity()
   * utils {}
   * ```
   */
  fun activeModules(block: ProjectModules.() -> Unit) {
    ProjectModules().block()
  }

  fun serverPluginProperties(block: ServerPluginProperties.() -> Unit) {
    serverPluginProperties.block()
  }

  fun proxyPluginProperties(block: ProxyPluginProperties.() -> Unit) {
    proxyPluginProperties.block()
  }
}

private class ProjectModules {
  fun folia() = setupProject("folia")
  fun paper() = setupProject("paper")
  fun velocity() = setupProject("velocity")

  /**
   * To build a jar for publication:
   * ```kotlin
   * createUtilLibJar = true
   * ```
   */
  fun util(block: UtilSettings.() -> Unit) {
    UtilSettings().block()
    setupProject("util")
  }

  private fun setupProject(projectName: String) {
    include(":$projectName")
    project(":$projectName").run {
      projectDir = File("modules$separator$projectName")
      projectDir
    }.also { projectDir ->
      if (!projectDir.exists()) projectDir.mkdirs()

      File("gradle${separator}templates$separator$projectName").copyRecursively(projectDir, onError = { _, _ -> SKIP })
      val pluginYml = File("gradle${separator}templates${separator}plugin.yml")
      File("${projectDir.path}${separator}src${separator}main${separator}kotlin").mkdirs()
      File("${projectDir.path}${separator}src${separator}main${separator}resources").also {
        it.mkdirs()
        if (projectName !in listOf("util", "velocity"))
          pluginYml.copyRecursively(it.resolve("plugin.yml"), onError = { _, _ -> SKIP })
      }
      File("${projectDir.path}${separator}src${separator}test${separator}kotlin").mkdirs()
      File("${projectDir.path}${separator}src${separator}test${separator}resources").mkdirs()
    }
  }
}

private class UtilSettings {
  var createUtilLibJar = false
    set(value) {
      extra["create-util-lib-jar"] = value
      field = value
    }
}

private class ServerPluginProperties {
  private val authors: PluginListProperty = PluginListProperty()
  var mainClass: String = ""
    set(value) {
      propsMap()["mainClass"] = value
      field = value
    }
  var description: String = ""
    set(value) {
      propsMap()["description"] = value
      field = value
    }
  private val dependencies: PluginListProperty = PluginListProperty()
  private val softDependencies: PluginListProperty = PluginListProperty()

  init {
    extra["server-plugin-properties"] = mapOf(
      "authors" to "",
      "mainClass" to "",
      "description" to "",
      "dependencies" to "",
      "softDependencies" to ""
    )
  }

  fun authors(block: PluginListProperty.() -> Unit) {
    authors.block()
    propsMap()["authors"] = "$authors"
  }

  fun dependencies(block: PluginListProperty.() -> Unit) {
    dependencies.block()
    propsMap()["dependencies"] = "$dependencies"
  }

  fun softDependencies(block: PluginListProperty.() -> Unit) {
    softDependencies.block()
    propsMap()["softDependencies"] = "$softDependencies"
  }

  private fun propsMap() = extra.getPropertiesMap("server-plugin-properties")
}

private class ProxyPluginProperties {
  var description: String = ""
    set(value) {
      propsMap()["description"] = value
      field = value
    }
  private val dependencies: PluginListProperty = PluginListProperty()
  private val softDependencies: PluginListProperty = PluginListProperty()

  init {
    extra["proxy-plugin-properties"] = mapOf(
      "description" to "",
      "dependencies" to "",
      "softDependencies" to ""
    )
  }

  fun dependencies(block: PluginListProperty.() -> Unit) {
    dependencies.block()
    propsMap()["dependencies"] = "$dependencies"
  }

  fun softDependencies(block: PluginListProperty.() -> Unit) {
    softDependencies.block()
    propsMap()["softDependencies"] = "$softDependencies"
  }

  private fun propsMap() = extra.getPropertiesMap("proxy-plugin-properties")
}

class PluginListProperty : LinkedHashSet<String>() {
  override fun toString(): String = yamlListOf()

  private fun yamlListOf() =
    takeIf { it.isNotEmpty() } // IF
      ?.toList()?.toString()   // DO
      ?: ""                    // ELSE
}

@Suppress("UNCHECKED_CAST")
private fun ExtraPropertiesExtension.getPropertiesMap(key: String): MutableMap<String, String> =
  extra[key]!! as MutableMap<String, String>