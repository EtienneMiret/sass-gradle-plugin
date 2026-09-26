package io.miret.etienne.gradle.sass;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;

import java.io.File;
import java.nio.file.Path;

@Getter
@Setter
public class SassGradlePluginExtension {

  private String version;

  /**
   * Where to install dart-sass. Lazy counterpart of {@link #getDirectory()}, which reads and writes through it.
   */
  private final DirectoryProperty installDirectory;

  private String baseUrl;

  private boolean autoCopy;

  public SassGradlePluginExtension (Project project) {
    Path projectPath = project.getRootDir()
        .toPath()
        .relativize(project.getProjectDir().toPath());
    File defaultDirectory = project.getRootDir()
        .toPath ()
        .resolve (".gradle/sass")
        .resolve(projectPath)
        .toFile ();
    this.version = "1.54.0";
    this.installDirectory = project.getObjects ().directoryProperty ()
        .convention (project.getLayout ().dir (project.provider (() -> defaultDirectory)));
    this.baseUrl = "https://github.com/sass/dart-sass/releases/download";
    this.autoCopy = true;
  }

  public File getDirectory () {
    return installDirectory.get ().getAsFile ();
  }

  public void setDirectory (File directory) {
    installDirectory.set (directory);
  }

  public void noAutoCopy () {
    this.autoCopy = false;
  }

}
