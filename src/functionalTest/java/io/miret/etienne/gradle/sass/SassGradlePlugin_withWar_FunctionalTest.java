package io.miret.etienne.gradle.sass;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class SassGradlePlugin_withWar_FunctionalTest {

  @TempDir
  Path projectDir;

  private WireMockServer server;

  @BeforeEach
  void startAndSetupServer () throws IOException {
    server = new WireMockServer (options ().dynamicPort ());
    server.start ();
    server.stubFor (get (urlMatching ("/1.23.7/dart-sass-.*"))
        .willReturn (aResponse ()
            .withStatus (200)
            .withBody (createArchive ())
        ));
  }

  @AfterEach
  void stopServer () {
    server.stop ();
  }

  @BeforeEach
  void setupProject () throws IOException {
    Files.createDirectories (projectDir.resolve ("src/main/sass"));
    try (var input = SassGradlePlugin_withWar_FunctionalTest.class.getResourceAsStream ("settings-with-war.gradle")) {
      Files.copy (input, projectDir.resolve ("settings.gradle"));
    }
    try (var input = SassGradlePlugin_withWar_FunctionalTest.class.getResourceAsStream ("build-with-war.gradle")) {
      Files.copy (input, projectDir.resolve ("build.gradle"));
    }
  }

  @Test
  void should_include_css_in_war () throws IOException {
    GradleRunner runner = GradleRunner.create ();
    runner.withPluginClasspath ();
    runner.withEnvironment (Map.of ("URL", server.baseUrl ()));
    runner.withArguments ("assemble");
    runner.withProjectDir (projectDir.toFile ());
    runner.build ();

    try (
        var input = Files.newInputStream (projectDir.resolve ("build/libs/cool-webapp-1.0.0.war"));
        var zip = new ZipInputStream (input)
    ) {
      var entries = new ArrayList<ZipEntry> ();
      for (var entry = zip.getNextEntry (); entry != null; entry = zip.getNextEntry ()) {
        entries.add (entry);
      }
      assertThat (entries)
          .extracting (ZipEntry::getName)
          .contains ("style.css");
    }
  }

  private byte[] createArchive () throws IOException {
    var bytes = new ByteArrayOutputStream ();
    if (Os.isFamily (Os.FAMILY_WINDOWS)) {
      try (
          var zip = new ZipOutputStream (bytes);
          var sass = SassGradlePlugin_withWar_FunctionalTest.class.getResourceAsStream ("sass.bat")
      ) {
        zip.putNextEntry (new ZipEntry ("dart-sass/sass.bat"));
        sass.transferTo (zip);
      }
    } else {
      try (
          var gz = new GZIPOutputStream (bytes);
          var tgz = new TarArchiveOutputStream (gz);
          var sass = SassGradlePlugin_withWar_FunctionalTest.class.getResourceAsStream ("sass.sh")
      ) {
        var entry = new TarArchiveEntry ("dart-sass/sass");
        entry.setSize (sass.available ());
        entry.setMode (0755);
        tgz.putArchiveEntry (entry);
        sass.transferTo (tgz);
        tgz.closeArchiveEntry ();
      }
    }
    return bytes.toByteArray ();
  }

}
