import Settings_gradle.LoadTime.POSTWORLD
import java.io.File.separator
import java.lang.System.setProperty
import java.net.URI
import kotlin.io.OnErrorAction.SKIP

rootProject.name = "Kotlin-Plugin-Template"

projectSettings {

  activeModules {

  }

  proxyPluginProperties {
    description = "A simple Template-Plugin using Kotlin as main language"
    dependencies {
      add("KotlinProvider")
    }
  }

  serverPluginProperties {
    authors {

    }
    mainClass = "TemplatePlugin"
    description = "A simple Template-Plugin using Kotlin as main language"
    dependencies {
      add("KotlinProvider")
    }
  }
}

// ####################################################################################################################
// ####################################################################################################################
// ####################################################################################################################
// Following code is to get above working or general settings that should not be touched unless you know what you're doing

setProperty("kotlin.code.style", "official")
setProperty("kotlin.incremental", "true")

dependencyResolutionManagement {
  pluginManagement.repositories {
    maven {
      name = "Bit-Build | Artifactory"
      url = URI("https://artifactory.bit-build.de/artifactory/public")
    }
  }
}

// ####################################################################################################################
// Project Settings DSL

private fun projectSettings(block: ProjectSettings.() -> Unit) {
  ProjectSettings().block()
}

private class ProjectSettings {
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
      setProperty("project.create-util-lib-jar", "$value")
      field = value
    }
}

private class ServerPluginProperties {
  init {
    setProperty("plugin.authors", "[]")
    setProperty("plugin.dependencies", "[]")
    setProperty("plugin.soft-dependencies", "[]")
    setProperty("plugin.load", "$POSTWORLD")
    setProperty("plugin.load-before", "[]")
    setProperty("plugin.commands", "{}")
  }

  var mainClass: String = ""
    set(value) {
      setProperty("plugin.main-class", value)
      field = value
    }

  var description: String = ""
    set(value) {
      setProperty("plugin.description", value)
      field = value
    }

  var load: LoadTime = POSTWORLD
    set(value) {
      setProperty("plugin.load", "$value")
      field = value
    }

  fun authors(block: PluginListProperty.() -> Unit) {
    setProperty("plugin.authors", "${PluginListProperty(block)}")
  }

  fun dependencies(block: PluginListProperty.() -> Unit) {
    setProperty("plugin.dependencies", "${PluginListProperty(block)}")
  }

  fun softDependencies(block: PluginListProperty.() -> Unit) {
    setProperty("plugin.soft-dependencies", "${PluginListProperty(block)}")
  }

  fun loadBefore(block: PluginListProperty.() -> Unit) {
    setProperty("plugin.load-before", "${PluginListProperty(block)}")
  }

  fun commands(block: Commands.() -> Unit) {
    setProperty("plugin.commands", "${Commands().apply(block)}")
  }
}

private enum class LoadTime {
  STARTUP,
  POSTWORLD;

  override fun toString(): String = name.uppercase()
}

private class Commands {
  private val commands: MutableMap<String, Command> = mutableMapOf()

  fun add(name: String, block: Command.() -> Unit) {
    commands[name] = Command().apply(block)
  }

  override fun toString(): String = commands.takeIf { it.isNotEmpty() }?.run {
    "\n${map { (name, cmd) -> "  $name:\n$cmd" }.joinToString("\n")}"
  } ?: "{}"
}

private class Command {
  var description: String? = null
  private val aliases = PluginListProperty()
  var permission: String? = null
  var permissionMessage: String? = null
  var usage: String? = null

  fun aliases(block: PluginListProperty.() -> Unit) {
    aliases.block()
  }

  override fun toString(): String = mutableMapOf(
    "description" to description,
    "aliases" to aliases.takeIf(PluginListProperty::isNotEmpty)?.toString(),
    "permission" to permission,
    "permission-message" to permissionMessage,
    "usage" to usage
  ).filterValues { it != null }
    .map { (key, value) -> "    $key: $value" }
    .joinToString("\n")
}

private class ProxyPluginProperties {
  init {
    setProperty("proxy-plugin.dependencies", "[]")
    setProperty("proxy-plugin.soft-dependencies", "[]")
  }

  var description: String = ""
    set(value) {
      setProperty("proxy-plugin.description", value)
      field = value
    }

  fun dependencies(block: ProxyDependenciesProperty.() -> Unit) {
    setProperty("proxy-plugin.dependencies", "${ProxyDependenciesProperty(block)}")
  }
}

private class PluginListProperty(block: PluginListProperty.() -> Unit = {}) : LinkedHashSet<String>() {
  init {
    block()
  }

  override fun toString(): String = yamlListOf()

  private fun yamlListOf() =
    takeIf { it.isNotEmpty() } // IF
      ?.toList()?.toString()   // DO
      ?: "[]"                    // ELSE
}

private class ProxyDependenciesProperty(block: ProxyDependenciesProperty.() -> Unit) :
  LinkedHashMap<String, Boolean>() {
  init {
    block()
  }

  fun add(dependency: String, optional: Boolean = false) {
    put(dependency, optional)
  }

  override fun toString(): String = "[\n" +
    map { (key, value) -> "    Dependency(id = \"${key.lowercase()}\", optional = $value)" }.joinToString(",\n") +
    "\n  ]"
}

private fun <T : Any> T.applyIf(condition: Boolean, block: T.() -> Unit): T = if (condition) {
  apply { block() }
} else {
  this
}