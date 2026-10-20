/*
 * Demonstrate how to use and configure this plugin with the Gradle Kotlin DSL.
 *
 * Note this is just to show the syntax. Don’t copy these settings verbatim!
 */

plugins {
    war
    id("io.miret.etienne.sass") version "1.6.1"
}

sass {
    version = "1.105.2"
    directory = file("${rootDir}/.gradle/sass-cache")
    baseUrl = "https://github.com/sass/dart-sass/releases/download"
}

tasks.compileSass {
    outputDir = file("${buildDir}/generated-css")
    destPath = "styles"
    sourceDir = file("${rootDir}/src/main/styles")

    entryPoint("main.scss", "styles.css")
    // You can also specify a pair
    entryPoint("secondary.scss" to "other.css")

    loadPath(file("/var/lib/compass"))

    style = expanded

    quiet()

    sourceMapLocation = file
    sourceMapUrls = relative
}
