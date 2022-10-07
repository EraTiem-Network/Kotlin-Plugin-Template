plugins {
    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("maven-java") {
            groupId = project.group.toString()
            artifactId = rootProject.name.toLowerCase()
            version = project.version.toString()
        }

        repositories {
            add(rootProject.ext["bitBuildArtifactoryPublish"] as MavenArtifactRepository)
            add(rootProject.ext["githubPackagesPublish"] as MavenArtifactRepository)
        }
    }
}