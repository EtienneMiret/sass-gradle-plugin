package io.miret.etienne.gradle.sass;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.Project;

import java.io.File;

@Getter
@Setter
public class SassGradlePluginExtension {

  private String version;

  private File directory;

  private String baseUrl;

  public SassGradlePluginExtension (Project project) {
    this.version = "1.23.7";
    this.directory = project.getRootDir ()
        .toPath ()
        .resolve (".gradle/sass")
        .toFile ();
    this.baseUrl = "https://github.com/sass/dart-sass/releases/download";
  }

}
