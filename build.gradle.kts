import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.util.stream.Collectors

plugins {
    val kotlinVersion: String by System.getProperties()
    val shadowVersion: String by System.getProperties()

    kotlin("jvm").version(kotlinVersion)
    kotlin("kapt").version(kotlinVersion)
    id("com.github.johnrengelman.shadow").version(shadowVersion)

    id("maven-publish")
}

group = "net.eratiem"
version = "0.1-SNAPSHOT"

repositories {
    maven {
        url = uri("https://artifactory.bit-build.de/artifactory/public")

        // Only needed for local repos that need authentication (for example snapshot-repos)
        bitBuildCredentials(this)
    }
}

dependencies {
    val kotlinVersion: String by System.getProperties()
    val eraloggerVersion: String by project
    val spigotApiVersion: String? by project
    val paperApiVersion: String? by project
    val bungeeApiVersion: String? by project
    val velocityApiVersion: String? by project

    compileOnly(kotlin("stdlib", kotlinVersion))
    compileOnly("net.eratiem", "eralogger", eraloggerVersion)

    if (!spigotApiVersion.isNullOrBlank()) compileOnly("org.spigotmc", "spigot-api", spigotApiVersion)
    if (!paperApiVersion.isNullOrBlank()) compileOnly("io.papermc.paper", "paper-api", paperApiVersion)
    if (!bungeeApiVersion.isNullOrBlank()) compileOnly("net.md-5", "bungeecord-api", bungeeApiVersion)
    if (!velocityApiVersion.isNullOrBlank()) {
        compileOnly("com.velocitypowered", "velocity-api", velocityApiVersion)
        kapt("com.velocitypowered", "velocity-api", velocityApiVersion)
    }
}

val jarTasks: MutableSet<TaskProvider<ShadowJar>> = mutableSetOf()

tasks {

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


        val apiRegex: Regex = "(\\d+\\.\\d+){1}(\\.\\d+)?".toRegex()
        filesMatching("plugin.yml") {
            var apiVersion =
                (project.properties["paperApiVersion"]?.takeUnless { (it as String).isBlank() }
                    ?: project.properties["spigotApiVersion"]?.takeUnless { (it as String).isBlank() }
                    ?: "") as String
            if (apiRegex.containsMatchIn(apiVersion)) apiVersion = apiRegex.find(apiVersion)?.value ?: ""

            props["plugin_api_version"] = apiVersion

            expand(props)
        }
        filesMatching("bungee.yml") {
            var apiVersion =
                (project.properties["bungeeApiVersion"]?.takeUnless { (it as String).isBlank() } ?: "") as String
            if (apiRegex.containsMatchIn(apiVersion)) apiVersion = apiRegex.find(apiVersion)?.value ?: ""

            props["plugin_api_version"] = apiVersion

            expand(props)
        }
    }

    // Disable standart jar task
    jar {
        enabled = false
    }

    project.configurations.implementation.get().isCanBeResolved = true

    // Register ShadowJar-Tasks with excludes
    getJarTaskExcludes().forEach { (name, excludes) -> registerShadowJarTask(name, excludes) }

    // Add ShadowJar Tasks as dependency to build
    build {
        jarTasks.forEach(this::dependsOn)
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

/**
 * Get Jar-Task excludes to generate clean jars
 */
fun getJarTaskExcludes(): Map<String, Set<String>> {
    val workingPackage = "${project.group.toString().replace('.', '/')}/${
        project.name.toLowerCaseAsciiOnly().replace("""[^\w\d]""".toRegex(), "")
    }"

    val enableSpigot: Boolean = File(projectDir, "src/main/kotlin/$workingPackage/spigot").exists()
    val enablePaper: Boolean = File(projectDir, "src/main/kotlin/$workingPackage/paper").exists()
    val enableBungee: Boolean = File(projectDir, "src/main/kotlin/$workingPackage/bungee").exists()
    val enableVelocity: Boolean = File(projectDir, "src/main/kotlin/$workingPackage/velocity").exists()
    val enableDependency: String by project

    val jarTaskExcludes: MutableMap<String, Set<String>> = mutableMapOf()

    if (enableSpigot) jarTaskExcludes["spigot"] = setOf(
        "$workingPackage/paper/**",
        "$workingPackage/bungee/**",
        "$workingPackage/velociy/**",
        "bungee.yml",
        "velocity-plugin.json"
    )
    if (enablePaper) jarTaskExcludes["paper"] = setOf(
        "$workingPackage/spigot/**",
        "$workingPackage/bungee/**",
        "$workingPackage/velocity/**",
        "bungee.yml",
        "velocity-plugin.json"
    )
    if (enableBungee) jarTaskExcludes["bungee"] = setOf(
        "$workingPackage/spigot/**",
        "$workingPackage/paper/**",
        "$workingPackage/velocity/**",
        "plugin.yml",
        "velocity-plugin.json"
    )
    if (enableVelocity) jarTaskExcludes["velocity"] = setOf(
        "$workingPackage/spigot/**",
        "$workingPackage/paper/**",
        "$workingPackage/bungee/**",
        "plugin.yml",
        "bungee.yml"
    )
    if (enableDependency.toBoolean()) jarTaskExcludes[""] = setOf(
        "$workingPackage/spigot/**",
        "$workingPackage/paper/**",
        "$workingPackage/bungee/**",
        "$workingPackage/velocity/**",
        "plugin.yml",
        "bungee.yml",
        "velocity-plugin.json"
    )

    return jarTaskExcludes
}

publishing {
    publications {
        create<MavenPublication>("maven-java") {
            groupId = project.group.toString()
            artifactId = project.name.toLowerCase()
            version = project.version.toString()

            jarTasks.forEach(this::artifact)
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
        maven {
            url = uri("https://maven.pkg.github.com/EraTiem-Network/${project.name}")

            githubPackageCredentials(this)
        }
    }
}

/**
 * Register ShadowJar-Task with excludes and archive name
 */
fun registerShadowJarTask(classifier: String, excludes: Set<String>) {
    jarTasks.add(tasks.register<ShadowJar>("${classifier}Jar") {
        group = "plugin"
        enabled = true

        archiveClassifier.set("")
        configurations = listOf(project.configurations.implementation.get())

        archiveClassifier.set(classifier)

        from(sourceSets.main.get().output) {
            exclude(excludes)
        }
    })
}


/**
 * parse comma seperated lists to
 */
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

/**
 * Get credentials for Bit-Build's Artifactory (https://artifactory.bit-build.de)
 */
fun bitBuildCredentials(maven: MavenArtifactRepository) {
    maven.credentials {
        username = System.getenv("ARTIFACTORY_USER")
        password = System.getenv("ARTIFACTORY_PASS")
    }
}

/**
 * Get credentials for Github-Packages
 */
fun githubPackageCredentials(maven: MavenArtifactRepository) {
    maven.credentials {
        username = System.getenv("GITHUB_USER")
        password = System.getenv("GITHUB_TOKEN")
    }
}