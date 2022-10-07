import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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
        compileOnly(rootProject.libs.kotlin.gradleplugin)
        compileOnly(rootProject.libs.kotlin.stdlib)
    }

    tasks {
        project.configurations.implementation.get().isCanBeResolved = true

        register<ShadowJar>("${project.name}Jar") {
            group = "plugin"
            enabled = true

            configurations = listOf(project.configurations.implementation.get())

            archiveBaseName.set(rootProject.name)
            archiveClassifier.set(project.name)
        }

        /**
         * Copy Task to fill plugin.yml and bungee.yml
         */
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

            filesMatching(setOf("plugin.yml", "bungee.yml")) {
                val api = if (this.sourceName.contains("plugin")) "pluginApiVersion" else "bungeeApiVersion"
                props["plugin_api_version"] = (project.properties[api] as String?) ?: ""

                expand(props)
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

    /**
     * Create task to copy plugin to paper server
     */
    create("copyPluginToServer") {
        dependsOn(build)

        group = "plugin"
        enabled = false

        outputs.upToDateWhen { false }

        val serverPath: String by project

        if (serverPath.isNotBlank() && File(serverPath).exists()) {
            val libsDir = File("${project.buildDir.absolutePath}${File.separator}libs")
            val destinationFile =
                File("$serverPath${File.separator}plugins${File.separator}${rootProject.name.toLowerCase()}.jar")
            val jarFiles: List<File>? = libsDir.listFiles()?.filter { it.extension == "jar" }

            if (jarFiles?.size == 1) {
                jarFiles[0].copyTo(
                    destinationFile,
                    true
                )

                enabled = destinationFile.exists()
            }
        }
    }

    /**
     * Create task to run paper server
     */
    create<Copy>("generateIntelliJRunConfig") {
        group = "plugin"
        enabled = false

        from("./runConfigs")
        destinationDir = File("./.idea/runConfigurations")
        include("intellij.xml")

        val serverPath: String? by project

        serverPath?.let { path ->
            if (path.isNotBlank() && File(path).exists()) {

                val paperFile: File? =
                    File(path).listFiles()?.filter { it.name.matches("paper.*\\.jar".toRegex()) }?.get(0)

                if (paperFile != null && paperFile.exists()) {
                    val props: LinkedHashMap<String, String> = linkedMapOf(
                        "server_path" to File(path).absolutePath,
                        "project_dir" to "\$PROJECT_DIR\$"
                    )

                    filesMatching("intellij.xml") {
                        expand(props)
                    }

                    enabled = true
                }
            }
        }
    }
}

fun RepositoryHandler.bitBuildArtifactory(
    useCredentials: Boolean = false, publish: Boolean = false
): MavenArtifactRepository {
    val url = if (publish) {
        "https://artifactory.bit-build.de/artifactory/eratiem${
            if (project.version.toString().contains("SNAPSHOT")) "-snapshots" else ""
        }"
    } else {
        "https://artifactory.bit-build.de/artifactory/public"
    }

    return if (publish || useCredentials) createMavenRepo(url, "ARTIFACTORY_USER", "ARTIFACTORY_PASS")
    else createMavenRepo(url)
}

fun RepositoryHandler.githubPackages(useCredentials: Boolean = true): MavenArtifactRepository {
    val url = "https://maven.pkg.github.com/EraTiem-Network/${project.name}"

    return if (useCredentials) createMavenRepo(url, "GITHUB_USER", "GITHUB_TOKEN")
    else createMavenRepo(url)
}

fun RepositoryHandler.createMavenRepo(url: String) = maven {
    this.url = uri(url)
}

fun RepositoryHandler.createMavenRepo(url: String, userEnv: String, passEnv: String) = maven {
    this.url = uri(url)
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