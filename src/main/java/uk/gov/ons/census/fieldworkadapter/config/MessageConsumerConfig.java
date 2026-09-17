package uk.gov.ons.census.fieldworkadapter.config;

import static com.google.cloud.spring.pubsub.support.PubSubSubscriptionUtils.toProjectSubscriptionName;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.MessageChannel;
import uk.gov.ons.census.fieldworkadapter.messaging.ManagedMessageRecoverer;

@Configuration
public class MessageConsumerConfig {
  // Spring core retry defaults to maxRetries = 3, i.e. 3 retries after the initial call
  // (4 total invocations). Pin this to 3 total invocations to preserve the pre-migration
  // behaviour.
  private static final int MESSAGE_TOTAL_ATTEMPTS = 3;

  private final ManagedMessageRecoverer managedMessageRecoverer;
  private final PubSubTemplate pubSubTemplate;

  @Value("${spring.cloud.gcp.pubsub.project-id}")
  private String pubsubProject;

  @Value("${queueconfig.case-update-subscription}")
  private String caseUpdateSubscription;

  public MessageConsumerConfig(
      ManagedMessageRecoverer managedMessageRecoverer, PubSubTemplate pubSubTemplate) {
    this.managedMessageRecoverer = managedMessageRecoverer;
    this.pubSubTemplate = pubSubTemplate;
  }

  @Bean
  public MessageChannel actionFieldInputChannel() {
    return new DirectChannel();
  }

  @Bean
  PubSubInboundChannelAdapter caseUpdateInbound(
      @Qualifier("actionFieldInputChannel") MessageChannel channel) {
    String subscription =
        toProjectSubscriptionName(caseUpdateSubscription, pubsubProject).toString();
    return makeAdapter(channel, subscription);
  }

  private PubSubInboundChannelAdapter makeAdapter(MessageChannel channel, String subscriptionName) {
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
    adapter.setOutputChannel(channel);
    adapter.setAckMode(AckMode.AUTO);
    return adapter;
  }

  @Bean
  public RequestHandlerRetryAdvice retryAdvice() {
    RequestHandlerRetryAdvice requestHandlerRetryAdvice = new RequestHandlerRetryAdvice();
    requestHandlerRetryAdvice.setRetryPolicy(
        RetryPolicy.builder().maxRetries(MESSAGE_TOTAL_ATTEMPTS - 1).delay(Duration.ZERO).build());
    requestHandlerRetryAdvice.setRecoveryCallback(managedMessageRecoverer);
    return requestHandlerRetryAdvice;
  }

  @Bean
  public RetryListener retryListener() {
    RetryListener retryListener = new DefaultListenerSupport();

    return retryListener;
  }
}
