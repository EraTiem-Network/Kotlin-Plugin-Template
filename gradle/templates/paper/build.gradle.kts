plugins {
  alias(libs.plugins.paperweight.userdev)
  alias(libs.plugins.run.paper)
}

val mcVersionRegex = Regex("^\\d+\\.\\d+\\.\\d+")
val mcApiVersionRegex = Regex("^\\d+\\.\\d+")

dependencies {
  compileOnly(libs.minecraft.server.paper)
  paperweight.paperDevBundle(libs.versions.paper.get())

  findProject(":util")?.let { implementation(it) }
}

tasks {
  runServer {
    minecraftVersion(mcVersionRegex.find(libs.versions.paper.get())!!.value)
  }

  withType<Copy> {
    val props = listOf("authors", "main-class", "description", "dependencies", "soft-dependencies")
      .associate { it.replace('-', '_') to getPluginProperty(it) }
      .toMutableMap()

    props["name"] = rootProject.name
    props["version"] = "${rootProject.version}"
    props["api_version"] = mcApiVersionRegex.find(libs.versions.paper.get())?.value
    props["commands"] = "-"

    filesMatching("plugin.yml") {
      expand(props.map { "plugin_${it.key}" to it.value }.toMap())
    }
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

private fun getPluginProperty(key: String) = System.getProperty("plugin.$key", "")