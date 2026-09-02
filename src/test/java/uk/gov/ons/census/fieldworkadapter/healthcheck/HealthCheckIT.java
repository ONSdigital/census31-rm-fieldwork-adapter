package uk.gov.ons.census.fieldworkadapter.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class HealthCheckIT {
  @Test
  public void testHappyPath() throws IOException {
    HealthCheck underTest = new HealthCheck();
    Path tempFile = Files.createTempFile("fieldwork-adapter-healthcheck-it", ".txt");
    ReflectionTestUtils.setField(underTest, "fileName", tempFile.toString());

    underTest.updateFileWithCurrentTimestamp();

    String fileLine = Files.readString(tempFile).trim();
    OffsetDateTime healthCheckTimeStamp = OffsetDateTime.parse(fileLine);
    assertThat(OffsetDateTime.now().toEpochSecond() - healthCheckTimeStamp.toEpochSecond())
        .isLessThan(30);
  }
}
