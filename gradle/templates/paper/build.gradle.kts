plugins {
  alias(libs.plugins.paperweight.userdev)
  alias(libs.plugins.run.paper)
}

val mcVersionRegex = Regex("^\\d+\\.\\d+\\.\\d+")

dependencies {
  compileOnly(libs.minecraft.server.paper)
  paperweight.paperDevBundle(libs.versions.paper.get())

  findProject(":util")?.let { implementation(it) }
}

tasks {
  runServer {
    minecraftVersion(mcVersionRegex.find(libs.versions.paper.get())!!.value)
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