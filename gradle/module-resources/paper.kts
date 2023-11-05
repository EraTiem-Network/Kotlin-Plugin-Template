plugins {
  alias(libs.plugins.run.paper)
}

dependencies {
  compileOnly(libs.minecraft.server.paper)
}

tasks.runServer {
  minecraftVersion("1.20.2")
}