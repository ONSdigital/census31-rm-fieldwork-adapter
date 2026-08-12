package uk.gov.ons.census.fieldworkadapter.messaging;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.common.model.entity.MessageToSend;
import uk.gov.ons.census.fieldworkadapter.model.repository.MessageToSendRepository;
import uk.gov.ons.census.fieldworkadapter.utils.JsonHelper;

@Component
public class MessageSender {
  private final MessageToSendRepository messageToSendRepository;

  public MessageSender(MessageToSendRepository messageToSendRepository) {
    this.messageToSendRepository = messageToSendRepository;
  }

  public void sendMessage(String destinationTopic, Object message) {
    MessageToSend messageToSend = new MessageToSend();
    messageToSend.setId(UUID.randomUUID());
    messageToSend.setDestinationTopic(destinationTopic);
    messageToSend.setMessageBody(JsonHelper.convertObjectToJson(message));

    messageToSendRepository.save(messageToSend);
  }
}
