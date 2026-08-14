package uk.gov.ons.census.fieldworkadapter.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.census.fieldworkadapter.testutils.MessageConstructor.constructMessage;
import static uk.gov.ons.census.fieldworkadapter.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.common.model.entity.EventType;
import uk.gov.ons.census.fieldworkadapter.logging.EventLogger;
import uk.gov.ons.census.fieldworkadapter.model.dto.Address;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventHeaderDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FieldActionInstruction;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtCancelActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.PayloadDTO;
import uk.gov.ons.census.fieldworkadapter.utils.ActionInstructionMapper;

@ExtendWith(MockitoExtension.class)
class ActionFieldReceiverTest {

  private static final String TEST_TOPIC = "event_fieldwork_action-instruction";

  @Mock private ActionInstructionMapper actionInstructionMapper;
  @Mock private MessageSender messageSender;
  @Mock private EventLogger eventLogger;

  @InjectMocks private ActionFieldReceiver underTest;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(underTest, "fwmtActionInstructionTopic", TEST_TOPIC);
  }

  @Test
  void shouldSwallowWhenFieldActionInstructionIsNull() {
    EventDTO nullInstructionEvent = buildEvent(null, "E", EventType.CASE_UPDATE, true);
    Message<byte[]> message = constructMessage(nullInstructionEvent);

    underTest.receiveMessage(message);

    verify(messageSender, never()).sendMessage(any(), any());
    verify(eventLogger).logEvent(anyString(), any(), any(), any(), any(Message.class));
  }

  @Test
  void shouldPublishCreateInstructionForNonNisraCase() {
    EventDTO event = buildEvent(FieldActionInstruction.CREATE, "E", EventType.CASE_UPDATE, true);
    Message<byte[]> message = constructMessage(event);

    FwmtActionInstructionDTO mapped = new FwmtActionInstructionDTO();
    mapped.setActionInstruction(FieldActionInstruction.CREATE);
    when(actionInstructionMapper.toFwmtActionInstruction(
            event.getPayload().getCaseUpdate(), FieldActionInstruction.CREATE))
        .thenReturn(mapped);

    underTest.receiveMessage(message);

    verify(actionInstructionMapper)
        .toFwmtActionInstruction(event.getPayload().getCaseUpdate(), FieldActionInstruction.CREATE);
    verify(messageSender).sendMessage(TEST_TOPIC, mapped);
    verify(eventLogger).logEvent(anyString(), any(), any(), any(), any(Message.class));
  }

  @Test
  void shouldPublishUpdateInstructionForNonNisraCase() {
    EventDTO event = buildEvent(FieldActionInstruction.UPDATE, "E", EventType.CASE_UPDATE, true);
    Message<byte[]> message = constructMessage(event);

    FwmtActionInstructionDTO mapped = new FwmtActionInstructionDTO();
    mapped.setActionInstruction(FieldActionInstruction.UPDATE);
    when(actionInstructionMapper.toFwmtActionInstruction(
            event.getPayload().getCaseUpdate(), FieldActionInstruction.UPDATE))
        .thenReturn(mapped);

    underTest.receiveMessage(message);

    verify(actionInstructionMapper)
        .toFwmtActionInstruction(event.getPayload().getCaseUpdate(), FieldActionInstruction.UPDATE);
    verify(messageSender).sendMessage(TEST_TOPIC, mapped);
    verify(eventLogger).logEvent(anyString(), any(), any(), any(), any(Message.class));
  }

  @Test
  void shouldPublishCancelInstructionForNonNisraCase() {
    EventDTO event = buildEvent(FieldActionInstruction.CANCEL, "E", EventType.CASE_UPDATE, true);
    Message<byte[]> message = constructMessage(event);

    FwmtCancelActionInstructionDTO mapped = new FwmtCancelActionInstructionDTO();
    mapped.setActionInstruction(FieldActionInstruction.CANCEL);
    when(actionInstructionMapper.toFwmtCancelActionInstruction(event.getPayload().getCaseUpdate()))
        .thenReturn(mapped);

    underTest.receiveMessage(message);

    verify(actionInstructionMapper)
        .toFwmtCancelActionInstruction(event.getPayload().getCaseUpdate());
    verify(messageSender).sendMessage(TEST_TOPIC, mapped);
    verify(eventLogger).logEvent(anyString(), any(), any(), any(), any(Message.class));
  }

  @Test
  void shouldNotPublishAnyInstructionForNisraCase() {
    EventDTO createEvent =
        buildEvent(FieldActionInstruction.CREATE, "N", EventType.CASE_UPDATE, true);
    EventDTO updateEvent =
        buildEvent(FieldActionInstruction.UPDATE, "N", EventType.CASE_UPDATE, true);
    EventDTO cancelEvent =
        buildEvent(FieldActionInstruction.CANCEL, "N", EventType.CASE_UPDATE, true);

    underTest.receiveMessage(constructMessage(createEvent));
    underTest.receiveMessage(constructMessage(updateEvent));
    underTest.receiveMessage(constructMessage(cancelEvent));

    verify(messageSender, never()).sendMessage(any(), any());
    verify(actionInstructionMapper, never()).toFwmtActionInstruction(any(), any());
    verify(actionInstructionMapper, never()).toFwmtCancelActionInstruction(any());
    verify(eventLogger, times(3)).logEvent(anyString(), any(), any(), any(), any(Message.class));
  }

  @Test
  void shouldNotPublishForKnownNiRegionContractSamples() {
    // "N" across all instructions is owned by shouldNotPublishAnyInstructionForNisraCase;
    // this test focuses purely on region format detection: case-insensitive, prefix, and trim.
    EventDTO niLowerCaseRegionOnly =
        buildEvent(FieldActionInstruction.CREATE, "n", EventType.CASE_UPDATE, true);
    EventDTO niOnsCode =
        buildEvent(FieldActionInstruction.UPDATE, "N92000002", EventType.CASE_UPDATE, true);
    EventDTO niLowerCaseOnsCode =
        buildEvent(FieldActionInstruction.CANCEL, "n92000002", EventType.CASE_UPDATE, true);
    EventDTO niCodeWithWhitespace =
        buildEvent(FieldActionInstruction.CREATE, " N92000002 ", EventType.CASE_UPDATE, true);

    underTest.receiveMessage(constructMessage(niLowerCaseRegionOnly));
    underTest.receiveMessage(constructMessage(niOnsCode));
    underTest.receiveMessage(constructMessage(niLowerCaseOnsCode));
    underTest.receiveMessage(constructMessage(niCodeWithWhitespace));

    verify(messageSender, never()).sendMessage(any(), any());
    verify(actionInstructionMapper, never()).toFwmtActionInstruction(any(), any());
    verify(actionInstructionMapper, never()).toFwmtCancelActionInstruction(any());
    verify(eventLogger, times(4)).logEvent(anyString(), any(), any(), any(), any(Message.class));
  }

  @Test
  void shouldPublishForNonNiRegionContractControlsAcrossInstructionAndRegionFormats() {
    FwmtActionInstructionDTO mappedForward = new FwmtActionInstructionDTO();
    FwmtCancelActionInstructionDTO mappedCancel = new FwmtCancelActionInstructionDTO();
    when(actionInstructionMapper.toFwmtActionInstruction(any(), any())).thenReturn(mappedForward);
    when(actionInstructionMapper.toFwmtCancelActionInstruction(any())).thenReturn(mappedCancel);

    EventDTO createUpper =
        buildEvent(FieldActionInstruction.CREATE, "E92000001", EventType.CASE_UPDATE, true);
    EventDTO createLower =
        buildEvent(FieldActionInstruction.CREATE, "w92000004", EventType.CASE_UPDATE, true);
    EventDTO createTrimmed =
        buildEvent(FieldActionInstruction.CREATE, " S92000003 ", EventType.CASE_UPDATE, true);

    EventDTO updateUpper =
        buildEvent(FieldActionInstruction.UPDATE, "S92000003", EventType.CASE_UPDATE, true);
    EventDTO updateLower =
        buildEvent(FieldActionInstruction.UPDATE, "e92000001", EventType.CASE_UPDATE, true);
    EventDTO updateTrimmed =
        buildEvent(FieldActionInstruction.UPDATE, " W92000004 ", EventType.CASE_UPDATE, true);

    EventDTO cancelUpper =
        buildEvent(FieldActionInstruction.CANCEL, "W92000004", EventType.CASE_UPDATE, true);
    EventDTO cancelLower =
        buildEvent(FieldActionInstruction.CANCEL, "s92000003", EventType.CASE_UPDATE, true);
    EventDTO cancelTrimmed =
        buildEvent(FieldActionInstruction.CANCEL, " E92000001 ", EventType.CASE_UPDATE, true);

    underTest.receiveMessage(constructMessage(createUpper));
    underTest.receiveMessage(constructMessage(createLower));
    underTest.receiveMessage(constructMessage(createTrimmed));
    underTest.receiveMessage(constructMessage(updateUpper));
    underTest.receiveMessage(constructMessage(updateLower));
    underTest.receiveMessage(constructMessage(updateTrimmed));
    underTest.receiveMessage(constructMessage(cancelUpper));
    underTest.receiveMessage(constructMessage(cancelLower));
    underTest.receiveMessage(constructMessage(cancelTrimmed));

    verify(actionInstructionMapper, times(6)).toFwmtActionInstruction(any(), any());
    verify(actionInstructionMapper, times(3)).toFwmtCancelActionInstruction(any());
    verify(messageSender, times(6)).sendMessage(TEST_TOPIC, mappedForward);
    verify(messageSender, times(3)).sendMessage(TEST_TOPIC, mappedCancel);
    verify(eventLogger, times(9)).logEvent(anyString(), any(), any(), any(), any(Message.class));
  }

  @Test
  void shouldThrowForWrongMessageType() {
    EventDTO event = buildEvent(FieldActionInstruction.UPDATE, "E", EventType.NEW_CASE, true);

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> underTest.receiveMessage(constructMessage(event)));

    org.assertj.core.api.Assertions.assertThat(thrown.getMessage())
        .contains("Event Type 'NEW_CASE' is invalid on this topic");
  }

  @Test
  void shouldThrowWhenCaseUpdatePayloadMissing() {
    EventDTO event = buildEvent(FieldActionInstruction.UPDATE, "E", EventType.CASE_UPDATE, false);

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> underTest.receiveMessage(constructMessage(event)));

    org.assertj.core.api.Assertions.assertThat(thrown.getMessage())
        .isEqualTo("Invalid CASE_UPDATE event: payload.caseUpdate is missing");
  }

  private EventDTO buildEvent(
      FieldActionInstruction instruction,
      String region,
      EventType messageType,
      boolean includeCaseUpdate) {
    EventHeaderDTO header = new EventHeaderDTO();
    header.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    header.setFieldActionInstruction(instruction);
    header.setMessageType(messageType);

    PayloadDTO payload = new PayloadDTO();
    if (includeCaseUpdate) {
      CaseUpdateDTO caseUpdate = new CaseUpdateDTO();
      caseUpdate.setCaseId(UUID.randomUUID());

      Address address = new Address();
      address.setRegion(region);
      address.setAddressType("HH");
      address.setAddressLevel("U");
      address.setPostcode("AB1 2CD");
      caseUpdate.setAddress(address);

      payload.setCaseUpdate(caseUpdate);
    }

    EventDTO event = new EventDTO();
    event.setHeader(header);
    event.setPayload(payload);
    return event;
  }
}
