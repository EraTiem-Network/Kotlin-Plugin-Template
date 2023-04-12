rootProject.name = "Kotlin-Plugin-Template"

/**
 * Activate or deactivate modules.
 *
 * Deactivate with caution as it will remove the corresponding directory!!!
 */
val modules: Map<String, Boolean> = mapOf(
  "tools" to false,

  "spigot" to false,
  "paper" to false,

  "bungeecord" to false,
  "waterfall" to false,
  "velocity" to false
)

val modulesWithPluginYAML: List<String> = listOf(
  "spigot", "paper", "bungeecord", "waterfall"
)

val moduleResources =
  File("${rootProject.projectDir.absolutePath}${File.separator}gradle${File.separator}module-resources")
val basePluginYAML = File("${moduleResources.path}${File.separator}plugin.yml")

modules.forEach { (moduleName, active) ->
  val moduleDir = File("${rootProject.projectDir.absolutePath}${File.separator}$moduleName")

  if (active) {
    if (!moduleDir.exists())
      createModuleFolder(moduleName, moduleDir)
    include(moduleName)
  } else {
    if (moduleDir.exists())
      moduleDir.deleteRecursively()
  }
}

fun createModuleFolder(moduleName: String, moduleDir: File) {
  val baseModuleBuildScript = File("${moduleResources.path}${File.separator}$moduleName.kts")

  val moduleBuildScript = File("${moduleDir.path}${File.separator}build.gradle.kts")
  val moduleMainKotlinDir = File("${moduleDir.path}${File.separator}src${File.separator}main${File.separator}kotlin")

  moduleDir.mkdirs()
  moduleMainKotlinDir.mkdirs()
  baseModuleBuildScript.copyTo(moduleBuildScript)

  if (moduleName in modulesWithPluginYAML) {
    val modulePluginYAML =
      File("${moduleMainKotlinDir.parentFile.path}${File.separator}resources${File.separator}plugin.yml")
    modulePluginYAML.parentFile.mkdirs()
    basePluginYAML.copyTo(modulePluginYAML)
  }
}