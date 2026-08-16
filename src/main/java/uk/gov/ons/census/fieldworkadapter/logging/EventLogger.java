package uk.gov.ons.census.fieldworkadapter.logging;

import static uk.gov.ons.census.fieldworkadapter.utils.MessageDateHelper.getMessageTimeStamp;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.common.model.entity.Event;
import uk.gov.ons.census.common.model.entity.EventType;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventHeaderDTO;
import uk.gov.ons.census.fieldworkadapter.model.repository.EventRepository;
import uk.gov.ons.census.fieldworkadapter.utils.JsonHelper;
import uk.gov.ons.census.fieldworkadapter.utils.RedactHelper;

@Component
public class EventLogger {

  private final EventRepository eventRepository;

  public EventLogger(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  public void logEvent(
      String eventDescription,
      EventType eventType,
      EventDTO event,
      Object eventPayload,
      OffsetDateTime messageTimestamp) {

    EventHeaderDTO eventHeader = event.getHeader();
    OffsetDateTime eventDate = eventHeader.getDateTime();

    Event loggedEvent =
        buildEvent(
            eventDate,
            eventDescription,
            eventType,
            eventHeader,
            RedactHelper.redact(eventPayload),
            messageTimestamp);

    eventRepository.save(loggedEvent);
  }

  public void logEvent(
      String eventDescription,
      EventType eventType,
      EventDTO event,
      Object eventPayload,
      Message<byte[]> message) {

    OffsetDateTime messageTimestamp = getMessageTimeStamp(message);

    logEvent(eventDescription, eventType, event, eventPayload, messageTimestamp);
  }

  private Event buildEvent(
      OffsetDateTime eventDate,
      String eventDescription,
      EventType eventType,
      EventHeaderDTO eventHeader,
      Object eventPayload,
      OffsetDateTime messageTimestamp) {
    Event loggedEvent = new Event();

    loggedEvent.setId(UUID.randomUUID());
    loggedEvent.setDateTime(eventDate);
    loggedEvent.setProcessedAt(OffsetDateTime.now());
    loggedEvent.setDescription(eventDescription);
    loggedEvent.setType(eventType);
    loggedEvent.setChannel(eventHeader.getChannel());
    loggedEvent.setSource(eventHeader.getSource());
    loggedEvent.setMessageId(eventHeader.getMessageId());
    loggedEvent.setMessageTimestamp(messageTimestamp);
    loggedEvent.setCreatedBy(eventHeader.getOriginatingUser());
    loggedEvent.setCorrelationId(eventHeader.getCorrelationId());

    loggedEvent.setPayload(JsonHelper.convertObjectToJson(eventPayload));

    return loggedEvent;
  }
}
