# Sass Compile

Sass Compile is a [Gradle][1] plugin to compile sass or scss files using the
official [Dart Sass][2] compiler.

In its default configuration, it will compile sass files from `src/main/sass`
to `build/sass`.
If the [War Plugin][3] is also applied, the generated CSS will also be added to
the war artifact. 

## Requirements
* Gradle >= 6.0

## Usage

First, apply the plugin:

```groovy
plugins {
  id 'io.miret.etienne.sass' version '1.2.0'
}
```

Optionally, configure it for your needs (default values shown):

```groovy
sass {
  // dart-sass version to use:
  version = '1.49.9'

  // Directory where to install dart-sass:
  directory = file ("${rootDir}/.gradle/sass")

  // Base URL where to download dart-sass from:
  baseUrl = 'https://github.com/sass/dart-sass/releases/download'
}
```

You may also configure the `compileSass` task:

```groovy
compileSass {
  // Directory where to output generated CSS:
  outputDir = project.file ("${buildDir}/sass")

  // Sub path where to copy generated CSS, eg relative to war root:
  destPath = "."

  // Source directory containing sass to compile:
  sourceDir = project.file ("${projectDir}/src/main/sass")

  // Adds a directory to sass load path (default is empty):
  loadPath project.file ('sass-lib')
  loadPath project.file ('/var/lib/compass')

  // Set the output style:
  // Possible values are “expanded” and “compressed”, default is “expanded”.
  style = expanded

  // Don’t emit a @charset for CSS with non-ASCII chars (default to emit):
  noCharset ()

  // When an error occurs, do not emit a stylesheet describing it:
  // (Default to emit)
  noErrorCss ()

  // Watch sass files in sourceDir for changes
  // (Default is to not to watch, compile once and terminate)
  watch ()

  // Source map style:
  //  - file: output source map in a separate file (default)
  //  - embed: embed source map in CSS
  //  - none: do not emit source map.
  sourceMap = file

  // How to link source maps to source files [relative (default) or absolute]:
  sourceMapUrls = relative
}
```

## Samples

### Simple

The easiest way to use this plugin is to apply it, along with the war
plugin, and put your sass files under `src/main/sass`.
See [samples/simple](samples/simple/build.gradle).

### With custom paths

You can customize the path where to pickup the SASS sources (`sourceDir`)
as well as the path inside the war where to put the generated CSS (`destPath`).
See [samples/custom-paths](samples/custom-paths/build.gradle).

Note that the `outputDir` is an intermediate folder where the CSS is output
before being copied to the war. There should be no need to customize it.

### Without the war plugin

If you’re not using the war plugin,
you need to explicitly call the `compileSass` task.
Usually, this is done by making another task depend on it.

Furthermore, the output will probably need to be consumed by that other task.
See [samples/jar](samples/jar/build.gradle) for an example with the `java`
plugin and its `jar` task.

### Watching for changes

The `watch ()` option prevents the `compileSass` task to terminates.
It is therefore better used on a copy of this task that isn’t a dependency
of the `assemble` task.
See [samples/watch](samples/watch/build.gradle).

[1]: https://gradle.org/ 
[2]: https://sass-lang.com/dart-sass
[3]: https://docs.gradle.org/current/userguide/war_plugin.html
