package io.miret.etienne.gradle.sass;

import com.google.common.io.ByteStreams;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.tools.ant.taskdefs.condition.Os;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Utils {

  /**
   * Copies classpath resources to a directory.
   * <p>
   * Each resource will be searched in the classpath from {@code base} and
   * copied to {@code destination}. That is, for {@code base = "/io/miret/"},
   * {@code destination = "/var/tmp"} and a {@code foo/bar.txt} resource,
   * the {@code /io/miret/foo/bar.txt} classpath resource will be copied
   * to {@code /var/tmp/foo/bar.txt}.
   * <p>
   * Intermediate directories will be created as needed.
   *
   * @param base base name for the resources, must include the starting '/'.
   * @param resources the set of file resources to copy, relative to base.
   * @param destination a path that points to an existing directory.
   */
  public static void copy(
      String base,
      Collection<String> resources,
      Path destination
  ) throws IOException {
    if (!base.startsWith("/")) {
      throw new IllegalArgumentException("Base resource name must start with a '/'");
    }
    for (String resourceStr : resources) {
      Path resourcePath = Paths.get(resourceStr);
      if (resourcePath.isAbsolute()) {
        throw new IllegalArgumentException("Absolute path provided: " + resourceStr);
      }
      Path target = destination.resolve(resourcePath);
      Files.createDirectories(target.getParent());
      try (InputStream input = Utils.class.getResourceAsStream(base + "/" + resourceStr)) {
        Objects.requireNonNull(input, "No such resource: " + resourceStr);
        Files.copy(input, target);
      }
    }
  }

  /**
   * Creates an archive with the dummy sass executable from classpath.
   * On Windows, this is a ZIP archive with {@code sass.bat}.
   * Elsewhere, this is a gzipped, tar archive with {@code sass.sh}.
   */
  public static byte[] createArchive() throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream ();
    if (Os.isFamily (Os.FAMILY_WINDOWS)) {
      try (
          ZipOutputStream zip = new ZipOutputStream (bytes);
          InputStream sass = SassGradlePlugin_withWar_FunctionalTest.class.getResourceAsStream ("sass.bat")
      ) {
        assert sass != null;
        zip.putNextEntry (new ZipEntry("dart-sass/sass.bat"));
        ByteStreams.copy (sass, zip);
      }
    } else {
      try (
          GZIPOutputStream gz = new GZIPOutputStream (bytes);
          TarArchiveOutputStream tgz = new TarArchiveOutputStream (gz);
          InputStream sass = SassGradlePlugin_withWar_FunctionalTest.class.getResourceAsStream ("sass.sh")
      ) {
        assert sass != null;
        TarArchiveEntry entry = new TarArchiveEntry ("dart-sass/sass");
        entry.setSize (sass.available ());
        entry.setMode (0755);
        tgz.putArchiveEntry (entry);
        ByteStreams.copy (sass, tgz);
        tgz.closeArchiveEntry ();
      }
    }
    return bytes.toByteArray ();
  }

}
