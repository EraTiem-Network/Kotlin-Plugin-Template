import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.util.stream.Collectors

group = "net.eratiem"
version = "0.1-SNAPSHOT"

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.shadow)

  idea
}

// Load SnakeYaml
buildscript {
  dependencies {
    classpath(libs.snakeyaml)
  }
}

// Command YAMLs
val yamlLoad by lazy { Load(LoadSettings.builder().build()) }
val yamlDump by lazy { Dump(DumpSettings.builder().build()) }
val loadYaml = { it: String ->
  val file = project.file(it)
  if (!file.exists()) file.createNewFile()

  project.file(it).bufferedReader().use { reader ->
    yamlLoad.loadFromReader(reader) as? LinkedHashMap<*, *>
  }
}
val commands by lazy { loadYaml("commands.yml") }
val proxyCommands by lazy { loadYaml("proxyCommands.yml") }


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
    if (tools != null && tools != project) implementation(project(path = ":tools", configuration = "shadow"))

    compileOnly(rootProject.libs.kotlin.gradleplugin)
    compileOnly(rootProject.libs.kotlin.stdlib)

    compileOnly(rootProject.libs.minecraft.plugin.eralogger)
  }

  configurations {
    create("shadowJarDependencies").apply {
      extendsFrom(project.configurations.implementation.get())
      isCanBeResolved = true
    }
  }

  tasks {
    shadowJar {
      group = "plugin"
      enabled = true
      dependsOn(classes)

      configurations = listOf(
        project.configurations.getByName("shadowJarDependencies")
      )

      archiveBaseName.set(rootProject.name)
      archiveClassifier.set(null as String?)
      if (project.name != "tools")
        archiveAppendix.set(project.name)

    }
    if (project.name == "tools")
      rootProject.ext["toolsArtifact"] = shadowJar

    /**
     * Copy Task to fill plugin.yml
     */
    if (project.name !in arrayOf("tools", "velocity")) {
      withType<Copy> {
        outputs.upToDateWhen { false }

        val mainClass = "${project.group}.${rootProject.name.lowercase()}.${project.properties["mainClass"]}"
        val pluginDescription: String by project
        val pluginDependencies = getAsYamlList(project.properties["pluginDependencies"])
        val pluginSoftDependencies = getAsYamlList(project.properties["pluginSoftdependencies"])
        val authors: String = getAsYamlList(project.properties["authors"])
        val commands: String = if (
          (project.properties["splitCommandYaml"] as? Boolean == true) &&
          project.name in arrayOf("bungeecord", "waterfall")
        ) {
          commands?.let { commandsDumpOptimization(yamlDump.dumpToString(it)) } ?: "[]"
        } else {
          proxyCommands?.let { commandsDumpOptimization(yamlDump.dumpToString(it)) } ?: "[]"
        }

        val props: LinkedHashMap<String, String> = linkedMapOf(
          "plugin_name" to rootProject.name,
          "plugin_description" to pluginDescription,
          "plugin_version" to version.toString(),
          "plugin_main_class" to mainClass,
          "plugin_dependencies" to pluginDependencies,
          "plugin_softdependencies" to pluginSoftDependencies,
          "plugin_authors" to authors,
          "plugin_commands" to commands
        )

        filesMatching("plugin.yml") {
          val api = when (project.name) {
            "spigot" -> rootProject.libs.versions.plugin.spigot
            "paper" -> rootProject.libs.versions.plugin.paper
            "folia" -> rootProject.libs.versions.plugin.folia
            "bungeecord" -> rootProject.libs.versions.plugin.bungeecord
            else -> rootProject.libs.versions.plugin.waterfall
          }
          props["plugin_api_version"] = api.get()

          expand(props)
        }
      }
    }

    // Disable standard jar task
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

fun getAsYamlList(commaSeparatedList: Any?): String =
  if (commaSeparatedList is String && commaSeparatedList.isNotBlank()) {
    commaSeparatedList
      .replace(" ", "")
      .split(",")
      .stream()
      .map { "\n  - $it" }
      .collect(Collectors.joining())
  } else ""

fun commandsDumpOptimization(commandsDump: String?) = commandsDump?.let {
  it.split("\n")
    .joinToString("\n", "\n") { line -> "  $line" }
}

repositories {
  bitBuildArtifactory()
}

tasks {
  // Disable standard jar task
  jar {
    enabled = false
  }

  shadowJar {
    enabled = false
  }
}

idea {
  module {
    isDownloadSources = true
    isDownloadJavadoc = true
  }
}

fun RepositoryHandler.bitBuildArtifactory(
  useCredentials: Boolean = false, publish: Boolean = false
): MavenArtifactRepository {
  val url: String
  val name: String
  if (publish) {
    val nonReleaseStrings = listOf("snapshot", "alpha", "beta", "rc")
    val isNonRelease =
      nonReleaseStrings.any { project.version.toString().contains(it, true) }
    url = "https://packages.bit-build.de/maven/p/eratiem-network/eratiem"
    name = "BitBuildSpaceEraTiem"

  } else {
    url = "https://artifactory.bit-build.de/artifactory/public"
    name = "BitBuildArtifactoryPublic"
  }

  return if (publish || useCredentials) createMavenRepo(url, name, "SPACE_USER", "SPACE_TOKEN")
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