plugins {
  alias(libs.plugins.paperweight.userdev)
  alias(libs.plugins.run.paper)
}

val mcVersionRegex = Regex("^\\d+\\.\\d+\\.\\d+")

dependencies {
  compileOnly(libs.minecraft.server.folia)
  paperweight.foliaDevBundle(libs.versions.folia.get())

  findProject(":util")?.let { implementation(it) }
}

tasks {
  runPaper.folia.registerTask {
    minecraftVersion(mcVersionRegex.find(libs.versions.folia.get())!!.value)
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