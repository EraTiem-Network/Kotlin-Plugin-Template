import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.stream.Collectors

plugins {
    val kotlinVersion: String by System.getProperties()
    val shadowVersion: String by System.getProperties()

    kotlin("jvm").version(kotlinVersion)
    id("com.github.johnrengelman.shadow").version(shadowVersion)

    id("maven-publish")
}

group = "net.eratiem"
version = "0.1-SNAPSHOT"

repositories {
    maven {
        url = uri("https://artifactory.bit-build.de/artifactory/all")

        bitBuildCredentials(this)
    }
}

dependencies {
    val kotlinVersion: String by System.getProperties()
    val paperApiVersion: String by project

    compileOnly(kotlin("stdlib", kotlinVersion))
    compileOnly("io.papermc.paper", "paper-api", paperApiVersion)
}

publishing {
    publications {
        create<MavenPublication>("maven-java") {
            groupId = project.group.toString()
            artifactId = project.name.toLowerCase()
            version = project.version.toString()

            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri(
                "https://artifactory.bit-build.de/artifactory/eratiem"
                        + (if (project.version.toString().contains("SNAPSHOT"))
                    "-snapshots" else "")
            )

            bitBuildCredentials(this)
        }
    }
}

tasks {
    // Write Properties into plugin.yml
    withType<Copy> {
        outputs.upToDateWhen { false }

        val mainClass = "${project.group}.${project.name.toLowerCase()}.${project.properties["mainClass"]}"
        val apiVersion =
            "(\\d+\\.\\d+){1}(\\.\\d+)?".toRegex().find(project.properties["paperApiVersion"] as String)!!.value
        val pluginDescription: String by project
        val pluginDependencies =
            "\n  - KotlinProvider${getAsYamlList(project.properties["pluginDependencies"])}"
        val authors: String = getAsYamlList(project.properties["authors"])

        val props: LinkedHashMap<String, String> = linkedMapOf(
            "plugin_name" to project.name,
            "plugin_description" to pluginDescription,
            "plugin_version" to version.toString(),
            "plugin_main_class" to mainClass,
            "plugin_api_version" to apiVersion,
            "plugin_dependencies" to pluginDependencies,
            "plugin_authors" to authors
        )

        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        project.configurations.implementation.get().isCanBeResolved = true
        configurations = listOf(project.configurations.implementation.get())
    }

    build {
        dependsOn(shadowJar)
    }

    //
    create("copyPluginToServer") {
        dependsOn(build)
        group = "plugin"
        enabled = true

        val serverPath: String by project
        enabled = serverPath.isNotBlank()

        outputs.upToDateWhen { false }
        val libsDir = File("${project.buildDir.absolutePath}${File.separator}libs")
        val destinationFile =
            File("$serverPath${File.separator}plugins${File.separator}${rootProject.name.toLowerCase()}.jar")
        val jarFiles: List<File>? = libsDir.listFiles()?.filter { it.extension == "jar" }

        enabled =
            if (jarFiles?.size == 1) {
                jarFiles[0].copyTo(
                    destinationFile,
                    true
                )

                destinationFile.exists()
            } else {
                false
            }
    }

    create<Copy>("generateIntelliJRunConfig") {
        group = "plugin"
        enabled = true

        from("./runConfigs")
        destinationDir = File("./.idea/runConfigurations")
        include("intellij.xml")

        val serverPath: String by project
        enabled = serverPath.isNotBlank()

        val paperFile: File? =
            File(serverPath).listFiles()?.filter { it.name.matches("paper.*\\.jar".toRegex()) }?.get(0)
        enabled = paperFile != null

        if (paperFile != null && paperFile.exists()) {
            val props: LinkedHashMap<String, String> = linkedMapOf(
                "server_path" to File(serverPath).absolutePath,
                "project_dir" to "\$PROJECT_DIR\$"
            )

            filesMatching("intellij.xml") {
                expand(props)
            }
        }
    }

    // Compile Stuff
    val javaVersion = JavaVersion.VERSION_17
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(javaVersion.toString().toInt())
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.toString()))
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersion.toString()
    }
}

fun getAsYamlList(commaSeparatedList: Any?): String {
    if (commaSeparatedList is String && commaSeparatedList.isNotBlank()) {
        return commaSeparatedList
            .split(",")
            .stream()
            .map { "\n  - $it" }
            .collect(Collectors.joining())
    }
    return ""
}

fun bitBuildCredentials(maven: MavenArtifactRepository) {
    maven.credentials {
        val mavenUsr: String by System.getProperties()
        val mavenPsw: String by System.getProperties()

        username = mavenUsr
        password = mavenPsw
    }
}