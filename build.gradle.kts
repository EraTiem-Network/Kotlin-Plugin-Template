import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.util.stream.Collectors

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

group = "net.eratiem"
version = "0.1-SNAPSHOT"

subprojects {
    this.group = rootProject.group
    this.version = rootProject.version

    repositories {
        bitBuildArtifactory()

        rootProject.ext["bitBuildArtifactoryPublish"] = bitBuildArtifactory(useCredentials = true, publish = true)
        rootProject.ext["githubPackagesPublish"] = githubPackages(true)
    }

    apply {
        plugin(rootProject.libs.plugins.kotlin.jvm.get().pluginId)
        plugin(rootProject.libs.plugins.shadow.get().pluginId)
    }

    dependencies {
        val tools: Project? = findProject(":tools")
        if (tools != null && tools != project) implementation(project(":tools"))

        compileOnly(rootProject.libs.kotlin.gradleplugin)
        compileOnly(rootProject.libs.kotlin.stdlib)
    }

    tasks {
        project.configurations.implementation.get().isCanBeResolved = true

        val shadowJarTask = register<ShadowJar>("${project.name}Jar") {
            group = "plugin"
            enabled = true

            configurations = listOf(project.configurations.implementation.get())

            archiveBaseName.set(rootProject.name)
            archiveClassifier.set(project.name)

        }
        if (project.name == "tools")
            rootProject.ext["toolsArtifact"] = shadowJarTask

        build {
            dependsOn(shadowJarTask)
        }

        /**
         * Copy Task to fill plugin.yml
         */
        if (project.name !in arrayOf("tools", "velocity")) {
            withType<Copy> {
                outputs.upToDateWhen { false }

                val mainClass = "${project.group}.${project.name.toLowerCase()}.${project.properties["mainClass"]}"
                val pluginDescription: String by project
                val pluginDependencies = getAsYamlList(project.properties["pluginDependencies"])
                val pluginSoftDependencies = getAsYamlList(project.properties["pluginSoftdependencies"])
                val authors: String = getAsYamlList(project.properties["authors"])

                val props: LinkedHashMap<String, String> = linkedMapOf(
                    "plugin_name" to project.name,
                    "plugin_description" to pluginDescription,
                    "plugin_version" to version.toString(),
                    "plugin_main_class" to mainClass,
                    "plugin_dependencies" to pluginDependencies,
                    "plugin_softdependencies" to pluginSoftDependencies,
                    "plugin_authors" to authors
                )

                filesMatching("plugin.yml") {
                    val api = if (this.sourceName.contains("plugin")) "pluginApiVersion" else "bungeeApiVersion"
                    props["plugin_api_version"] = (project.properties[api] as String?) ?: ""

                    expand(props)
                }
            }
        }

        // Disable standart jar task
        jar {
            enabled = false
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
}

fun getAsYamlList(commaSeparatedList: Any?): String {
    if (commaSeparatedList is String && commaSeparatedList.isNotBlank()) {
        return commaSeparatedList
            .replace(" ", "")
            .split(",")
            .stream()
            .map { "\n  - $it" }
            .collect(Collectors.joining())
    }
    return ""
}

repositories {
    bitBuildArtifactory()
}

tasks {
    // Disable standart jar task
    jar {
        enabled = false
    }
}

fun RepositoryHandler.bitBuildArtifactory(
    useCredentials: Boolean = false, publish: Boolean = false
): MavenArtifactRepository {
    val url: String
    val name: String
    if (publish) {
        val isSnapshot = project.version.toString().toUpperCaseAsciiOnly().contains("SNAPSHOT")
        url = "https://artifactory.bit-build.de/artifactory/eratiem${if (isSnapshot) "-snapshots" else ""}"
        name = "BitBuildArtifactoryEraTiem${if (isSnapshot) "Snapshots" else ""}"

    } else {
        url = "https://artifactory.bit-build.de/artifactory/public"
        name = "BitBuildArtifactoryPublic"
    }

    return if (publish || useCredentials) createMavenRepo(url, name,"ARTIFACTORY_USER", "ARTIFACTORY_TOKEN")
    else createMavenRepo(url, name)
}

fun RepositoryHandler.githubPackages(useCredentials: Boolean = true): MavenArtifactRepository {
    val url = "https://maven.pkg.github.com/EraTiem-Network/${project.name}"
    val name = "GitHub"

    return if (useCredentials) createMavenRepo(url, name, "GITHUB_USER", "GITHUB_TOKEN")
    else createMavenRepo(url, name)
}

fun RepositoryHandler.createMavenRepo(url: String, name: String) = maven {
    this.url = uri(url)
    this.name = name
}

fun RepositoryHandler.createMavenRepo(url: String, name: String, userEnv: String, passEnv: String) = maven {
    this.url = uri(url)
    this.name = name
    val user: String? = System.getenv(userEnv)
    val pass: String? = System.getenv(passEnv)

    if (user == null || pass == null) {
        logger.error("The environment variable $userEnv or $passEnv does not exist or is null!")
    }

    credentials {
        username = System.getenv(userEnv)
        password = System.getenv(passEnv)
    }
}