package uk.gov.ons.census.fieldworkadapter.testutils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import uk.gov.ons.census.fieldworkadapter.utils.JsonHelper;

public class MessageConstructor {
  public static Message<byte[]> constructMessage(Object payload) {
    byte[] payloadBytes = JsonHelper.convertObjectToJson(payload).getBytes(StandardCharsets.UTF_8);
    return constructMessage(payloadBytes, Map.of());
  }

  public static Message<byte[]> constructMessage(Object payload, Map<String, Object> headers) {
    byte[] payloadBytes = JsonHelper.convertObjectToJson(payload).getBytes(StandardCharsets.UTF_8);
    return constructMessage(payloadBytes, headers);
  }

  private static Message<byte[]> constructMessage(byte[] payload, Map<String, Object> headers) {
    return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
  }
}
