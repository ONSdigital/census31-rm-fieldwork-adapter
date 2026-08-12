package uk.gov.ons.census.fieldworkadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.census.fieldworkadapter.testutils.MessageConstructor.constructMessage;
import static uk.gov.ons.census.fieldworkadapter.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.census.common.model.entity.EventType;
import uk.gov.ons.census.fieldworkadapter.model.dto.Address;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventHeaderDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FieldActionInstruction;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtCancelActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.PayloadDTO;
import uk.gov.ons.census.fieldworkadapter.utils.ActionInstructionMapper;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
      ActionFieldReceiver.class,
      ActionInstructionMapper.class,
      ActionFieldReceiverIT.TestConfig.class
    })
@TestPropertySource(
    properties = {
      "queueconfig.fieldwork-action-instruction-topic=event_fieldwork_action-instruction"
    })
class ActionFieldReceiverIT {

  @Configuration
  static class TestConfig {
    @Bean
    MessageSender messageSender() {
      return Mockito.mock(MessageSender.class);
    }
  }

  @Autowired private ActionFieldReceiver underTest;
  @Autowired private MessageSender messageSender;

  @BeforeEach
  void resetMocks() {
    Mockito.reset(messageSender);
  }

  @Test
  void shouldPublishCreateActionInstructionThroughSpringWiring() {
    EventDTO event = buildEvent(FieldActionInstruction.CREATE, "E", EventType.CASE_UPDATE);
    Message<byte[]> message = constructMessage(event);

    underTest.receiveMessage(message);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messageSender)
        .sendMessage(eq("event_fieldwork_action-instruction"), payloadCaptor.capture());

    assertThat(payloadCaptor.getValue()).isInstanceOf(FwmtActionInstructionDTO.class);
    FwmtActionInstructionDTO published = (FwmtActionInstructionDTO) payloadCaptor.getValue();
    assertThat(published.getActionInstruction()).isEqualTo(FieldActionInstruction.CREATE);
    assertThat(published.getCaseId()).isEqualTo(event.getPayload().getCaseUpdate().getCaseId());
  }

  @Test
  void shouldPublishCancelActionInstructionThroughSpringWiring() {
    EventDTO event = buildEvent(FieldActionInstruction.CANCEL, "E", EventType.CASE_UPDATE);
    Message<byte[]> message = constructMessage(event);

    underTest.receiveMessage(message);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messageSender)
        .sendMessage(eq("event_fieldwork_action-instruction"), payloadCaptor.capture());

    assertThat(payloadCaptor.getValue()).isInstanceOf(FwmtCancelActionInstructionDTO.class);
    FwmtCancelActionInstructionDTO published =
        (FwmtCancelActionInstructionDTO) payloadCaptor.getValue();
    assertThat(published.getActionInstruction()).isEqualTo(FieldActionInstruction.CANCEL);
    assertThat(published.getCaseId()).isEqualTo(event.getPayload().getCaseUpdate().getCaseId());
  }

  @Test
  void shouldSuppressNisraMessagesThroughSpringWiring() {
    EventDTO event = buildEvent(FieldActionInstruction.UPDATE, "N", EventType.CASE_UPDATE);

    underTest.receiveMessage(constructMessage(event));

    verify(messageSender, never()).sendMessage(any(), any());
  }

  @Test
  void shouldRejectWrongMessageTypeThroughSpringWiring() {
    EventDTO event = buildEvent(FieldActionInstruction.UPDATE, "E", EventType.NEW_CASE);

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> underTest.receiveMessage(constructMessage(event)));

    assertThat(thrown.getMessage()).contains("Event Type 'NEW_CASE' is invalid on this topic");
  }

  private EventDTO buildEvent(
      FieldActionInstruction instruction, String region, EventType messageType) {
    EventHeaderDTO header = new EventHeaderDTO();
    header.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    header.setMessageType(messageType);
    header.setFieldActionInstruction(instruction);

    Address address = new Address();
    address.setRegion(region);
    address.setAddressType("HH");
    address.setAddressLevel("U");
    address.setPostcode("AB1 2CD");
    address.setLatitude("51.5");
    address.setLongitude("-1.2");

    CaseUpdateDTO caseUpdate = new CaseUpdateDTO();
    caseUpdate.setCaseId(UUID.randomUUID());
    caseUpdate.setCaseRef("1000000001");
    caseUpdate.setAddress(address);

    PayloadDTO payload = new PayloadDTO();
    payload.setCaseUpdate(caseUpdate);

    EventDTO event = new EventDTO();
    event.setHeader(header);
    event.setPayload(payload);

    return event;
  }
}
