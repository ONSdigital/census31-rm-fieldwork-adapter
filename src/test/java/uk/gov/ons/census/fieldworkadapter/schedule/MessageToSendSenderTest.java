package uk.gov.ons.census.fieldworkadapter.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import uk.gov.ons.census.common.model.entity.MessageToSend;

@ExtendWith(MockitoExtension.class)
class MessageToSendSenderTest {

  @Mock private PubSubTemplate pubSubTemplate;

  @InjectMocks private MessageToSendSender underTest;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(underTest, "publishTimeout", 1);
  }

  @Test
  void shouldPublishMessageToConfiguredTopic() {
    MessageToSend messageToSend = new MessageToSend();
    messageToSend.setDestinationTopic("topic-a");
    messageToSend.setMessageBody("{\"caseId\":\"1\"}");
    when(pubSubTemplate.publish(eq(messageToSend.getDestinationTopic()), any()))
        .thenReturn(CompletableFuture.completedFuture("message-id"));

    underTest.sendMessage(messageToSend);

    ArgumentCaptor<PubsubMessage> captor = ArgumentCaptor.forClass(PubsubMessage.class);
    verify(pubSubTemplate).publish(eq("topic-a"), captor.capture());
    assertThat(captor.getValue().getData().toStringUtf8()).isEqualTo("{\"caseId\":\"1\"}");
  }

  @Test
  void shouldWrapExecutionExceptionAsRuntimeException() {
    MessageToSend messageToSend = new MessageToSend();
    messageToSend.setDestinationTopic("topic-a");
    messageToSend.setMessageBody("payload");

    CompletableFuture<String> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new IllegalStateException("illegal state exception"));
    when(pubSubTemplate.publish(eq(messageToSend.getDestinationTopic()), any()))
        .thenReturn(failedFuture);

    assertThatThrownBy(() -> underTest.sendMessage(messageToSend))
        .isInstanceOf(RuntimeException.class)
        .hasCauseInstanceOf(ExecutionException.class);
  }

  @Test
  void shouldWrapTimeoutExceptionAsRuntimeException() {
    MessageToSend messageToSend = new MessageToSend();
    messageToSend.setDestinationTopic("topic-a");
    messageToSend.setMessageBody("payload");

    CompletableFuture<String> timeoutFuture =
        new CompletableFuture<>() {
          @Override
          public String get(long timeout, TimeUnit unit) throws TimeoutException {
            throw new TimeoutException("timed out");
          }
        };

    when(pubSubTemplate.publish(eq(messageToSend.getDestinationTopic()), any()))
        .thenReturn(timeoutFuture);

    assertThatThrownBy(() -> underTest.sendMessage(messageToSend))
        .isInstanceOf(RuntimeException.class)
        .hasCauseInstanceOf(TimeoutException.class);
  }
}
