package io.miret.etienne.gradle.sass;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;

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

}
