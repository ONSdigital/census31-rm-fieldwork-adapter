package uk.gov.ons.census.fieldworkadapter.config;

import static com.google.cloud.spring.pubsub.support.PubSubSubscriptionUtils.toProjectSubscriptionName;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import uk.gov.ons.census.fieldworkadapter.messaging.NonRetryableEventException;

@Configuration
public class MessageConsumerConfig {
  private final PubSubTemplate pubSubTemplate;

  @Value("${spring.cloud.gcp.pubsub.project-id:${spring.cloud.gcp.project-id:our-project}}")
  private String pubsubProject;

  @Value("${queueconfig.case-update-subscription}")
  private String caseUpdateSubscription;

  @Value("${queueconfig.retry.max-attempts:3}")
  private int maxRetryAttempts;

  public MessageConsumerConfig(PubSubTemplate pubSubTemplate) {
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
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(
        new SimpleRetryPolicy(
            maxRetryAttempts, java.util.Map.of(NonRetryableEventException.class, false), true));
    requestHandlerRetryAdvice.setRetryTemplate(retryTemplate);
    return requestHandlerRetryAdvice;
  }
}
