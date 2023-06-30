/*
 * Demonstrate how to use and configure this plugin with the Gradle Kotlin DSL.
 *
 * Note this is just to show the syntax. Don’t copy these settings verbatim!
 */

plugins {
    "war"
    id("io.miret.etienne.sass") version "1.4.2"
}

sass {
    version = "1.63.6"
    directory = file("${rootDir}/.gradle/sass-cache")
    baseUrl = "https://github.com/sass/dart-sass/releases/download"
    noAutoCopy()
}

tasks.compileSass {
    outputDir = file("${buildDir}/generated-css")
    destPath = "styles"
    sourceDir = file("${rootDir}/src/main/styles")

    loadPath(file("/var/lib/compass"))

    style = expanded

    quiet()

    sourceMap = file
    sourceMapUrls = relative
}
