package uk.gov.ons.census.fieldworkadapter.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.census.fieldworkadapter.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.census.fieldworkadapter.testutils.TestConstants.TEST_ORIGINATING_USER;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import uk.gov.ons.census.common.model.entity.Event;
import uk.gov.ons.census.common.model.entity.EventType;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventHeaderDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FieldActionInstruction;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.model.repository.EventRepository;
import uk.gov.ons.census.fieldworkadapter.utils.EventHelper;

@ExtendWith(MockitoExtension.class)
class EventLoggerTest {
  @Mock EventRepository eventRepository;

  @InjectMocks EventLogger underTest;

  @Test
  void testLogEventSuppliedDateTime() {
    OffsetDateTime eventTime = OffsetDateTime.now();
    OffsetDateTime messageTime = OffsetDateTime.now().minusSeconds(30);
    EventHeaderDTO eventHeader =
        EventHelper.createEventDTO("Test topic", TEST_CORRELATION_ID, TEST_ORIGINATING_USER);
    eventHeader.setSource("Test source");
    eventHeader.setChannel("Test channel");
    eventHeader.setDateTime(eventTime);
    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);

    FwmtActionInstructionDTO actionInstruction = new FwmtActionInstructionDTO();
    actionInstruction.setActionInstruction(FieldActionInstruction.CREATE);

    underTest.logEvent(
        "Published CREATE fieldwork action instruction",
        EventType.CASE_UPDATE,
        event,
        actionInstruction,
        messageTime);

    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    Event actualEvent = eventArgumentCaptor.getValue();
    assertThat(actualEvent.getUacQidLink()).isNull();
    assertThat(actualEvent.getCaze()).isNull();
    assertThat(eventTime).isEqualTo(actualEvent.getDateTime());
    assertThat("Test source").isEqualTo(actualEvent.getSource());
    assertThat("Test channel").isEqualTo(actualEvent.getChannel());
    assertThat(EventType.CASE_UPDATE).isEqualTo(actualEvent.getType());
    assertThat("Published CREATE fieldwork action instruction")
        .isEqualTo(actualEvent.getDescription());
    assertThat(eventHeader.getMessageId()).isEqualTo(actualEvent.getMessageId());
    assertThat(eventHeader.getCorrelationId()).isEqualTo(actualEvent.getCorrelationId());
    assertThat(eventHeader.getOriginatingUser()).isEqualTo(actualEvent.getCreatedBy());
    assertThat(messageTime).isEqualTo(actualEvent.getMessageTimestamp());
    assertThat(actualEvent.getPayload()).isNotBlank();
  }

  @Test
  void testLogEventDateTimeFromMessage() {
    OffsetDateTime eventTime = OffsetDateTime.now();
    EventHeaderDTO eventHeader =
        EventHelper.createEventDTO("Test topic", TEST_CORRELATION_ID, TEST_ORIGINATING_USER);
    eventHeader.setSource("Test source");
    eventHeader.setChannel("Test channel");
    eventHeader.setDateTime(eventTime);
    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);

    FwmtActionInstructionDTO actionInstruction = new FwmtActionInstructionDTO();
    actionInstruction.setActionInstruction(FieldActionInstruction.CREATE);

    @SuppressWarnings("unchecked")
    Message<byte[]> message = (Message<byte[]>) mock(Message.class);

    OffsetDateTime messageTime = OffsetDateTime.now().minusSeconds(3911);
    long timeStamp = messageTime.toInstant().toEpochMilli();

    MessageHeaders messageHeaders = mock(MessageHeaders.class);
    when(message.getHeaders()).thenReturn(messageHeaders);

    when(messageHeaders.getTimestamp()).thenReturn(timeStamp);

    underTest.logEvent(
        "Published CREATE fieldwork action instruction",
        EventType.CASE_UPDATE,
        event,
        actionInstruction,
        message);

    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    Event actualEvent = eventArgumentCaptor.getValue();
    assertThat(actualEvent.getUacQidLink()).isNull();
    assertThat(actualEvent.getCaze()).isNull();
    assertThat(eventTime).isEqualTo(actualEvent.getDateTime());
    assertThat("Test source").isEqualTo(actualEvent.getSource());
    assertThat("Test channel").isEqualTo(actualEvent.getChannel());
    assertThat(EventType.CASE_UPDATE).isEqualTo(actualEvent.getType());
    assertThat("Published CREATE fieldwork action instruction")
        .isEqualTo(actualEvent.getDescription());
    assertThat(eventHeader.getMessageId()).isEqualTo(actualEvent.getMessageId());
    assertThat(eventHeader.getCorrelationId()).isEqualTo(actualEvent.getCorrelationId());
    assertThat(eventHeader.getOriginatingUser()).isEqualTo(actualEvent.getCreatedBy());
    assertThat(timeStamp).isEqualTo(actualEvent.getMessageTimestamp().toInstant().toEpochMilli());
  }
}
