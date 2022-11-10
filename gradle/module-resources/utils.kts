plugins {
    id("maven-publish")
    id("java-library")
}

if (properties["enableToolsMavenDependency"] == "true") {
    publishing {
        publications {
            create<MavenPublication>("Utils") {
                groupId = project.group.toString()
                artifactId = rootProject.name.toLowerCase()
                version = project.version.toString()

                artifact(rootProject.ext["utilsArtifact"] as TaskProvider<*>)
            }

            repositories {
                add(rootProject.ext["bitBuildArtifactoryPublish"] as MavenArtifactRepository)
                add(rootProject.ext["githubPackagesPublish"] as MavenArtifactRepository)
            }
        }
    }
}
