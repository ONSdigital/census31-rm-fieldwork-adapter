package uk.gov.ons.census.fieldworkadapter.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventHeaderDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.PayloadDTO;

class JsonHelperTest {

  @Test
  void convertObjectToJson_serializesEvent() {
    EventDTO event = createEvent("0.5.0");

    String json = JsonHelper.convertObjectToJson(event);

    assertThat(json).contains("\"version\":\"0.5.0\"");
    assertThat(json).contains("\"topic\":\"test-topic\"");
  }

  @Test
  void convertJsonBytesToEvent_deserializesSupportedVersion() {
    EventDTO event = createEvent("0.5.0");

    EventDTO converted =
        JsonHelper.convertJsonBytesToEvent(JsonHelper.convertObjectToJson(event).getBytes(UTF_8));

    assertThat(converted.getHeader().getVersion()).isEqualTo("0.5.0");
    assertThat(converted.getHeader().getTopic()).isEqualTo("test-topic");
  }

  @Test
  void convertJsonBytesToEvent_throwsForUnsupportedVersion() {
    EventDTO event = createEvent("0.1.0");

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () ->
                JsonHelper.convertJsonBytesToEvent(
                    JsonHelper.convertObjectToJson(event).getBytes(UTF_8)));

    assertThat(ex.getMessage()).contains("Unsupported message version");
    assertThat(ex.getMessage()).contains("0.1.0");
  }

  @Test
  void convertJsonBytesToEvent_throwsForInvalidJson() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> JsonHelper.convertJsonBytesToEvent("{".getBytes(UTF_8)));

    assertThat(ex.getCause()).isNotNull();
  }

  private EventDTO createEvent(String version) {
    EventHeaderDTO header =
        EventHelper.createEventDTO("test-topic", UUID.randomUUID(), "FIELDWORK_ADAPTER");
    header.setVersion(version);

    EventDTO event = new EventDTO();
    event.setHeader(header);
    event.setPayload(new PayloadDTO());
    return event;
  }
}
