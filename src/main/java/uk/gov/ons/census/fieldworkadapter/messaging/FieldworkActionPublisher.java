package uk.gov.ons.census.fieldworkadapter.messaging;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fieldworkadapter.utils.JsonHelper;

@Component
public class FieldworkActionPublisher {
  private final PubSubTemplate pubSubTemplate;

  @Value("${queueconfig.publishtimeout}")
  private int publishTimeout;

  public FieldworkActionPublisher(PubSubTemplate pubSubTemplate) {
    this.pubSubTemplate = pubSubTemplate;
  }

  public String sendMessage(
      String destinationTopic, Object message, Map<String, String> attributes) {
    PubsubMessage.Builder builder =
        PubsubMessage.newBuilder()
            .setData(ByteString.copyFromUtf8(JsonHelper.convertObjectToJson(message)));
    if (attributes != null && !attributes.isEmpty()) {
      builder.putAllAttributes(attributes);
    }

    PubsubMessage pubsubMessage = builder.build();

    CompletableFuture<String> future = pubSubTemplate.publish(destinationTopic, pubsubMessage);

    try {
      return future.get(publishTimeout, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new PublishFailedException(
          String.format("Failed to publish fieldwork message to topic '%s'", destinationTopic), e);
    }
  }
}
