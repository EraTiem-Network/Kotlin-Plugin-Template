plugins {
    alias(libs.plugins.run.paper)
}

dependencies {
    compileOnly(libs.minecraft.server.folia)

    paperweight.paperDevBundle("1.20.2-R0.1-SNAPSHOT")
}

tasks {
    runPaper.folia.registerTask {
        minecraftVersion("1.20.2")
    }
}