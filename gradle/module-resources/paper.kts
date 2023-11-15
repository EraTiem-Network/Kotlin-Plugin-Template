plugins {
  alias(libs.plugins.run.paper)
}

dependencies {
  compileOnly(libs.minecraft.server.paper)

  paperweight.paperDevBundle("1.20.2-R0.1-SNAPSHOT")
}

tasks.runServer {
  minecraftVersion("1.20.2")
}