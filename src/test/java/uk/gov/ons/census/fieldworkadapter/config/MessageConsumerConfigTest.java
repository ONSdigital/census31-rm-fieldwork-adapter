package uk.gov.ons.census.fieldworkadapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RetryListener;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.fieldworkadapter.messaging.ManagedMessageRecoverer;

@ExtendWith(MockitoExtension.class)
class MessageConsumerConfigTest {

  @Mock private ManagedMessageRecoverer managedMessageRecoverer;

  @Mock private PubSubTemplate pubSubTemplate;

  private MessageConsumerConfig underTest;

  @BeforeEach
  void setUp() {
    underTest = new MessageConsumerConfig(managedMessageRecoverer, pubSubTemplate);
    ReflectionTestUtils.setField(underTest, "pubsubProject", "project-a");
    ReflectionTestUtils.setField(underTest, "caseUpdateSubscription", "subscription-a");
  }

  @Test
  void shouldBuildInboundAdapterWithAutoAck() {
    MessageChannel channel = new DirectChannel();

    PubSubInboundChannelAdapter adapter = underTest.caseUpdateInbound(channel);

    assertThat(adapter.getAckMode()).isEqualTo(AckMode.AUTO);
    assertThat(adapter.getOutputChannel()).isEqualTo(channel);
  }

  @Test
  void shouldCreateRetryAdviceUsingRecovererCallback() {
    RequestHandlerRetryAdvice retryAdvice = underTest.retryAdvice();

    assertThat(ReflectionTestUtils.getField(retryAdvice, "recoveryCallback"))
        .isEqualTo(managedMessageRecoverer);
  }

  @Test
  void shouldCreateRetryListener() {
    RetryListener retryListener = underTest.retryListener();
    assertThat(retryListener).isInstanceOf(DefaultListenerSupport.class);
  }
}
