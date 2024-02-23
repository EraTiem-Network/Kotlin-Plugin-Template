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
}

sourceSets {
  main {
    java.setSrcDirs(listOf<String>())
  }
  test {
    java.setSrcDirs(listOf<String>())
  }
}