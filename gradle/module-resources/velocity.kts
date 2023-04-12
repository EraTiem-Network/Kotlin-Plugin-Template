plugins {
  alias(libs.plugins.run.velocity)
  id(libs.plugins.kotlin.kapt.get().pluginId)
}

dependencies {
  compileOnly(libs.minecraft.proxy.velocity)
  kapt(libs.minecraft.proxy.velocity)
}

tasks.runVelocity {
  velocityVersion(libs.versions.velocity.get())
}