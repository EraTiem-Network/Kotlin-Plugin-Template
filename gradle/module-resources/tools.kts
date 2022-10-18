plugins {
    id("maven-publish")
}

if (properties["enableToolsMavenDependency"] == "true") {
    publishing {
        publications {
            create<MavenPublication>("Tools") {
                groupId = project.group.toString()
                artifactId = rootProject.name.toLowerCase()
                version = project.version.toString()

                artifact(rootProject.ext["toolsArtifact"] as TaskProvider<*>)
            }

            repositories {
                add(rootProject.ext["bitBuildArtifactoryPublish"] as MavenArtifactRepository)
                add(rootProject.ext["githubPackagesPublish"] as MavenArtifactRepository)
            }
        }
    }
}
