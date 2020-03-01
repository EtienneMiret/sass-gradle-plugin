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
  id 'io.miret.etienne.sass' version '1.1.1'
}
```

Optionally, configure it for your needs (default values shown):

```groovy
sass {
  // dart-sass version to use:
  version = '1.24.4'

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

  // Source directory containing sass to compile:
  sourceDir = project.file (${projectDir}/src/main/sass)

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

[1]: https://gradle.org/ 
[2]: https://sass-lang.com/dart-sass
[3]: https://docs.gradle.org/current/userguide/war_plugin.html
