package uk.gov.ons.census.fieldworkadapter.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.ons.census.fieldworkadapter.utils.Constants.ALLOWED_INBOUND_EVENT_SCHEMA_VERSIONS;
import static uk.gov.ons.census.fieldworkadapter.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.ons.census.common.model.entity.EventType;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventHeaderDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FieldActionInstruction;
import uk.gov.ons.census.fieldworkadapter.model.dto.PayloadDTO;

class JsonHelperTest {

  @Test
  void convertObjectToJson_serializesEventWithExpectedWireFormat() {
    EventDTO event = createEvent(OUTBOUND_EVENT_SCHEMA_VERSION);

    String json = JsonHelper.convertObjectToJson(event);

    assertThat(json)
        .isEqualTo(
            "{\"header\":{\"version\":\""
                + OUTBOUND_EVENT_SCHEMA_VERSION
                + "\",\"topic\":\"test-topic\","
                + "\"source\":\"FIELDWORK_ADAPTER\",\"channel\":\"RM\","
                + "\"dateTime\":\"2026-09-15T12:30:00Z\","
                + "\"messageId\":\"11111111-1111-1111-1111-111111111111\","
                + "\"correlationId\":\"22222222-2222-2222-2222-222222222222\","
                + "\"originatingUser\":\"test-user\",\"messageType\":\"CASE_UPDATE\","
                + "\"fieldActionInstruction\":\"CREATE\"},\"payload\":{}}");
  }

  @Test
  void convertJsonBytesToEvent_deserializesSupportedVersion() {
    EventDTO event = createEvent(OUTBOUND_EVENT_SCHEMA_VERSION);

    EventDTO converted =
        JsonHelper.convertJsonBytesToEvent(JsonHelper.convertObjectToJson(event).getBytes(UTF_8));

    assertThat(converted.getHeader().getVersion()).isEqualTo(OUTBOUND_EVENT_SCHEMA_VERSION);
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
    assertThat(ex.getMessage()).contains(String.join(", ", ALLOWED_INBOUND_EVENT_SCHEMA_VERSIONS));
  }

  @Test
  void convertJsonBytesToEvent_throwsForInvalidJson() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> JsonHelper.convertJsonBytesToEvent("{".getBytes(UTF_8)));

    assertThat(ex.getCause()).isNotNull();
  }

  private EventDTO createEvent(String version) {
    EventHeaderDTO header = new EventHeaderDTO();
    header.setVersion(version);
    header.setTopic("test-topic");
    header.setSource("FIELDWORK_ADAPTER");
    header.setChannel("RM");
    header.setDateTime(OffsetDateTime.parse("2026-09-15T12:30:00Z"));
    header.setMessageId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    header.setCorrelationId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    header.setOriginatingUser("test-user");
    header.setMessageType(EventType.CASE_UPDATE);
    header.setFieldActionInstruction(FieldActionInstruction.CREATE);

    EventDTO event = new EventDTO();
    event.setHeader(header);
    event.setPayload(new PayloadDTO());
    return event;
  }
}
