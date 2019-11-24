package io.miret.etienne.gradle.sass;

import de.undercouch.gradle.tasks.download.Download;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;

public class SassGradlePlugin implements Plugin<Project> {

  public void apply (Project project) {
    SassGradlePluginExtension extension = project.getExtensions ()
        .create ("sass", SassGradlePluginExtension.class, project);

    String archiveName = archiveName (extension.getVersion ());
    File archive = extension.getDirectory ()
        .toPath ()
        .resolve ("archive")
        .resolve (archiveName)
        .toFile ();
    final FileTree downloadedFiles = Os.isFamily (Os.FAMILY_WINDOWS)
        ? project.zipTree (archive)
        : project.tarTree (archive);
    TaskProvider<Download> downloadSass = project.getTasks ()
        .register ("downloadSass", Download.class, task -> {
          task.setDescription ("Download a sass archive.");
          task.src (String.format ("%s/%s/%s", extension.getBaseUrl (), extension.getVersion (), archiveName));
          task.dest (archive);
          task.overwrite (false);
        });
    project.getTasks ()
        .register ("installSass", Copy.class, task -> {
          task.setDescription ("Unpack and install a sass archive.");
          task.dependsOn (downloadSass);
          task.from (downloadedFiles);
          task.into (extension.getDirectory ());
        });
  }

  private String archiveName (String version) {
    return String.format ("dart-sass-%s-%s-%s.%s",
        version, buildOperatingSystem (), buildArchitecture (), archiveExtension ());
  }

  private String buildOperatingSystem () {
    if (Os.isFamily (Os.FAMILY_WINDOWS)) {
      return "windows";
    } else if (Os.isFamily (Os.FAMILY_MAC)) {
      return "macos";
    } else {
      return "linux";
    }
  }

  private String buildArchitecture () {
    return System.getProperty ("os.arch").contains ("64") ? "x64" : "ia32";
  }

  private String archiveExtension () {
    return Os.isFamily (Os.FAMILY_WINDOWS) ? "zip" : "tar.gz";
  }

}
