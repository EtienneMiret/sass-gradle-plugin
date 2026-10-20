# Sass Compile

Sass Compile is a [Gradle][1] plugin to compile sass or scss files using the
official [Dart Sass][2] compiler.

In its default configuration, it will compile sass files from `src/main/sass`
to `build/sass`.
If the [War Plugin][3] is also applied, the generated CSS will also be added to
the war artifact.

It supports the [configuration cache][4].
Its tasks (`downloadSass`, `installSass` and `compileSass`) are also cacheable,
so with the [build cache][5] enabled, they reuse outputs from previous builds
instead of downloading dart-sass or compiling sass again.

## Requirements

### Version 2.x

* Gradle >= 8.5
* Java >= 21

### Version 1.x

* Gradle >= 6.0
* Java >= 1.8

If you use this version,
make sure you read the documentation on [branch 1.x].

[branch 1.x]: https://github.com/EtienneMiret/sass-gradle-plugin/blob/1.x/README.md

## Usage

First, apply the plugin:

```groovy
plugins {
  id 'io.miret.etienne.sass' version '2.0.0'
}
```

Optionally, configure it for your needs (default values shown):

```groovy
sass {
  // dart-sass version to use:
  version = '1.105.2'

  // Directory where to download and install dart-sass
  // (when the plugin is applied to a subproject,
  // the default is ${rootDir}/.gradle/sass/<subproject path>):
  directory = file ("${rootDir}/.gradle/sass")

  // Base URL where to download dart-sass from:
  baseUrl = 'https://github.com/sass/dart-sass/releases/download'

  // Automagically copy generated CSS to the war
  // (set to false, or call noAutoCopy (), to disable):
  autoCopy = true
}
```

You may also configure the `compileSass` task:

```groovy
compileSass {
  // Directory where to output generated CSS:
  outputDir = layout.buildDirectory.dir ('sass')

  // Sub path under outputDir where to write generated CSS,
  // e.g., relative to war root:
  destPath = "."

  // Source directory containing sass to compile:
  sourceDir = file ('src/main/sass')

  // Specify entry points for sass compilation
  // (default is to compile all .scss and .sass files in sourceDir,
  // except partials whose name starts with an underscore,
  // and output a .css file with the same name for each)
  // Those paths are relative to sourceDir and outputDir/destPath respectively.
  entryPoint "main.scss", "styles.css"
  entryPoint "errors.scss", "errors.css"

  // Add a directory to sass load path (default is empty):
  loadPath project.file ('sass-lib')
  loadPath project.file ('node_modules')

  // Set the output style:
  // Possible values are “expanded” and “compressed”, default is “expanded”.
  style = expanded

  // Don’t emit a @charset declaration nor a byte-order mark
  // for CSS with non-ASCII chars (default is to emit them).
  // Same as charset = false:
  noCharset ()

  // When an error occurs, do not emit a stylesheet describing it
  // (default is to emit it).
  // Same as errorCss = false:
  noErrorCss ()

  // Watch sass files in sourceDir for changes
  // (default is not to watch, but to compile once and terminate).
  // Same as watch = true:
  watch ()

  // Prevent sass from emitting warnings
  // (default is to emit them).
  // Same as quiet = true:
  quiet ()

  // Source map location:
  //  - file: output source map in a separate file (default)
  //  - embedded: embed source map in CSS
  //  - none: do not emit source map.
  sourceMapLocation = file

  // How to link source maps to source files [relative (default) or absolute]
  // (ignored when sourceMapLocation is none):
  sourceMapUrls = relative
}
```

## Examples

### Simple

The easiest way to use this plugin is to apply it, along with the war
plugin, and put your sass files under `src/main/sass`.
See [examples/simple](examples/simple/build.gradle).

### With custom paths

You can customize the path where to pick up the SASS sources (`sourceDir`)
as well as the path inside the war where to put the generated CSS (`destPath`).
See [examples/custom-paths](examples/custom-paths/build.gradle).

Note that the `outputDir` is an intermediate folder where the CSS is output
before being copied to the war. There should be no need to customize it.

### Without the war plugin

If you’re not using the war plugin,
you need to explicitly call the `compileSass` task.
Usually, this is done by making another task depend on it.

Furthermore, the output will probably need to be consumed by that other task.
See [examples/jar](examples/jar/build.gradle) for an example with the `java`
plugin and its `jar` task.

### Using the Gradle Kotlin DSL

When writing the build script in Kotlin instead of Groovy,
the syntax is different.
See [examples/kotlin](examples/kotlin/build.gradle.kts).

### In a multi-project build

You can apply this plugin to a multi-project build.
If only one subproject needs it, just apply it to said subproject.
Otherwise, apply it to the root project,
or to a common ancestor of all subprojects that need it.

Applying the plugin to a project registers a `compileSass` task
in that project and in every one of its subprojects,
all sharing the same dart-sass installation and `sass {}` configuration.
In each of those projects, the generated CSS is added to the war artifacts,
unless `noAutoCopy ()` was called.
See [examples/multi-projects](examples/multi-projects/build.gradle).

### Watching for changes

The `watch ()` option prevents the `compileSass` task from terminating.
It is therefore better used on a separate task of type `CompileSass`
that isn’t a dependency of the `assemble` task.
See [examples/watch](examples/watch/build.gradle).

Such an additional task must depend on `installSass`,
so that dart-sass is installed before it runs.
It must also belong to a project where the plugin is applied,
or to one of its subprojects.

[1]: https://gradle.org/
[2]: https://sass-lang.com/dart-sass
[3]: https://docs.gradle.org/current/userguide/war_plugin.html
[4]: https://docs.gradle.org/current/userguide/configuration_cache.html
[5]: https://docs.gradle.org/current/userguide/build_cache.html
