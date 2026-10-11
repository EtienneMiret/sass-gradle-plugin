import org.gradle.plugin.compatibility.compatibility

plugins {
  `kotlin-dsl`
  id("com.gradle.plugin-publish") version "2.2.1"
}

repositories {
  mavenCentral()
}

val javaVersion = JavaLanguageVersion.of(21)

java {
  toolchain {
    languageVersion = javaVersion
  }
}

val functionalTest = sourceSets.create("functionalTest")

val functionalTestImplementation =
  configurations.named(functionalTest.implementationConfigurationName) {
    extendsFrom(configurations.testImplementation.get())
  }
val functionalTestRuntimeOnly =
  configurations.named(functionalTest.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.testRuntimeOnly.get())
  }

dependencies {
  implementation("de.undercouch:gradle-download-task:5.7.0")

  testImplementation("org.assertj:assertj-core:3.27.7")
  testImplementation(platform("org.junit:junit-bom:5.14.4"))
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  functionalTestImplementation("org.wiremock:wiremock:3.13.2")
  functionalTestImplementation("org.apache.commons:commons-compress:1.28.0")
}

tasks.test {
  useJUnitPlatform()
}

version = "2.0.0-SNAPSHOT"
group = "io.miret.etienne.gradle"

gradlePlugin {
  website = "https://github.com/EtienneMiret/sass-gradle-plugin"
  vcsUrl = "https://github.com/EtienneMiret/sass-gradle-plugin"

  plugins {
    create("sass") {
      id = "io.miret.etienne.sass"
      displayName = "Sass Compile"
      description = "A Gradle plugin to compile scss files using the official Dart Sass compiler."
      implementationClass = "io.miret.etienne.gradle.sass.SassGradlePlugin"
      tags = listOf("sass", "scss")

      compatibility {
        features {
          configurationCache = true
        }
      }
    }
  }
}

gradlePlugin.testSourceSets(functionalTest)

val functionalTests = tasks.register("functionalTest", Test::class) {
  description = "Runs the functional tests."
  group = "verification"
  testClassesDirs = functionalTest.output.classesDirs
  classpath = functionalTest.runtimeClasspath
  useJUnitPlatform()
}

tasks.check {
  dependsOn(functionalTests)
}

tasks.publishPlugins {
  doFirst {
    if (JavaLanguageVersion.of(JavaVersion.current().toString()) != javaVersion) {
      throw GradleException("This build must be run with Java ${javaVersion}.")
    }
  }
}
