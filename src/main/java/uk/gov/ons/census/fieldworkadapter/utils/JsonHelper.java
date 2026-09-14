package uk.gov.ons.census.fieldworkadapter.utils;

import static uk.gov.ons.census.fieldworkadapter.utils.Constants.ALLOWED_INBOUND_EVENT_SCHEMA_VERSIONS;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventDTO;

public class JsonHelper {
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  public static String convertObjectToJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JacksonException e) {
      throw new RuntimeException("Failed converting Object To Json", e);
    }
  }

  public static EventDTO convertJsonBytesToEvent(byte[] bytes) {
    EventDTO event;
    try {
      event = objectMapper.readValue(bytes, EventDTO.class);
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }

    if (!ALLOWED_INBOUND_EVENT_SCHEMA_VERSIONS.contains((event.getHeader().getVersion()))) {
      throw new RuntimeException(
          String.format(
              "Unsupported message version. Got %s but RM only supports %s",
              event.getHeader().getVersion(),
              String.join(", ", ALLOWED_INBOUND_EVENT_SCHEMA_VERSIONS)));
    }

    return event;
  }
}
