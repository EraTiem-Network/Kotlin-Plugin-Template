plugins {
  alias(libs.plugins.run.waterfall)
}

dependencies {
  compileOnly(libs.minecraft.proxy.waterfall)
}

tasks.runWaterfall {
  waterfallVersion("1.19")
}