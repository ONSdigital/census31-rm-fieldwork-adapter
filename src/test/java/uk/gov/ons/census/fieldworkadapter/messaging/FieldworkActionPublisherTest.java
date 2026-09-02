package uk.gov.ons.census.fieldworkadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FieldworkActionPublisherTest {

  @Mock private PubSubTemplate pubSubTemplate;

  @InjectMocks private FieldworkActionPublisher underTest;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(underTest, "publishTimeout", 1);
  }

  @Test
  void shouldPublishMessageToConfiguredTopic() {
    when(pubSubTemplate.publish(eq("topic-a"), any()))
        .thenReturn(CompletableFuture.completedFuture("message-id"));

    underTest.sendMessage("topic-a", Map.of("caseId", "1"), Map.of("eventId", "e-1"));

    ArgumentCaptor<PubsubMessage> captor = ArgumentCaptor.forClass(PubsubMessage.class);
    verify(pubSubTemplate).publish(eq("topic-a"), captor.capture());
    assertThat(captor.getValue().getData().toStringUtf8()).isEqualTo("{\"caseId\":\"1\"}");
    assertThat(captor.getValue().getAttributesMap()).containsEntry("eventId", "e-1");
  }

  @Test
  void shouldWrapExecutionExceptionAsRuntimeException() {
    CompletableFuture<String> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new IllegalStateException("illegal state exception"));
    when(pubSubTemplate.publish(eq("topic-a"), any())).thenReturn(failedFuture);

    assertThatThrownBy(() -> underTest.sendMessage("topic-a", "payload", Map.of()))
        .isInstanceOf(RuntimeException.class)
        .hasCauseInstanceOf(java.util.concurrent.ExecutionException.class);
  }

  @Test
  void shouldWrapTimeoutExceptionAsRuntimeException() {
    CompletableFuture<String> timeoutFuture =
        new CompletableFuture<>() {
          @Override
          public String get(long timeout, TimeUnit unit) throws TimeoutException {
            throw new TimeoutException("timed out");
          }
        };

    when(pubSubTemplate.publish(eq("topic-a"), any())).thenReturn(timeoutFuture);

    assertThatThrownBy(() -> underTest.sendMessage("topic-a", "payload", Map.of()))
        .isInstanceOf(RuntimeException.class)
        .hasCauseInstanceOf(TimeoutException.class);
  }

  @Test
  void shouldWrapInterruptedExceptionAsRuntimeException() {
    CompletableFuture<String> interruptedFuture =
        new CompletableFuture<>() {
          @Override
          public String get(long timeout, TimeUnit unit) throws InterruptedException {
            throw new InterruptedException("interrupted");
          }
        };

    when(pubSubTemplate.publish(eq("topic-a"), any())).thenReturn(interruptedFuture);

    assertThatThrownBy(() -> underTest.sendMessage("topic-a", "payload", Map.of()))
        .isInstanceOf(RuntimeException.class)
        .hasCauseInstanceOf(InterruptedException.class);
  }
}
