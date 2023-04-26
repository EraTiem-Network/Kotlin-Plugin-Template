plugins {
    alias(libs.plugins.run.paper)
}

dependencies {
    compileOnly(libs.minecraft.server.folia)
}

tasks {
    runPaper.folia.registerTask {
        minecraftVersion("1.19.4")
    }
}