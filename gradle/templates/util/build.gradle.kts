import java.lang.System.getProperty

plugins {

}

dependencies {

}

if (getProperty("project.create-util-lib-jar", "false").toBoolean()) {
  rootProject.publishing {
    publications {
      val block: MavenPublication.() -> Unit = {
        artifact(tasks.shadowJar) {
          classifier = ""
        }

        artifactId = rootProject.name
      }

      if (findByName("maven") != null) {
        named("maven", block)
      } else {
        create("maven", block)
      }
    }
  }
}

tasks {
  jar {
    enabled = true
  }
}

sourceSets {
  main {
    java.setSrcDirs(listOf<String>())
  }
  test {
    java.setSrcDirs(listOf<String>())
  }
}