plugins {
    id(libs.plugins.kotlin.kapt.get().pluginId)
}

dependencies {
    compileOnly(libs.minecraft.proxy.velocity)
    kapt(libs.minecraft.proxy.velocity)
}

tasks {
    kapt {
        this.useBuildCache = true
    }
}