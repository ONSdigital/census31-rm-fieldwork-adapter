package uk.gov.ons.census.fieldworkadapter.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.ons.census.common.model.entity.FulfilmentToProcess;

class RedactHelperTest {
  @Test
  void testRedactWorksForMap() {
    // GIVEN
    FulfilmentToProcess fulfilmentToProcess = new FulfilmentToProcess();

    fulfilmentToProcess.setPersonalisation(Map.of("PHONE_NUMBER", "999999"));

    // WHEN
    // Cast the object back to it's original type, just for the test
    FulfilmentToProcess fulfilmentToProcessRedacted =
        (FulfilmentToProcess) RedactHelper.redact(fulfilmentToProcess);

    // THEN
    assertThat(fulfilmentToProcessRedacted.getPersonalisation())
        .isEqualTo(Map.of("PHONE_NUMBER", "REDACTED"));

    // Extra check to make sure the original object wasn't accidentally mutated
    assertThat(fulfilmentToProcess.getPersonalisation())
        .isEqualTo(Map.of("PHONE_NUMBER", "999999"));
  }
}
