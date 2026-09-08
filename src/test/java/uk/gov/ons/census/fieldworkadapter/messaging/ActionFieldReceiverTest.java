package uk.gov.ons.census.fieldworkadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.census.fieldworkadapter.testutils.MessageConstructor.constructMessage;
import static uk.gov.ons.census.fieldworkadapter.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.common.model.entity.EventType;
import uk.gov.ons.census.fieldworkadapter.model.dto.Address;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventHeaderDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FieldActionInstruction;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtCancelActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.PayloadDTO;
import uk.gov.ons.census.fieldworkadapter.service.FieldFollowUpFilter;
import uk.gov.ons.census.fieldworkadapter.utils.ActionInstructionMapper;

@ExtendWith(MockitoExtension.class)
class ActionFieldReceiverTest {

  private static final String TEST_TOPIC = "event_fieldwork_action-instruction";
  private static final OffsetDateTime EVENT_TIME = OffsetDateTime.parse("2026-08-25T10:15:30Z");

  @Mock private FieldworkActionPublisher fieldworkActionPublisher;

  private ActionFieldReceiver underTest;

  @BeforeEach
  void setUp() {
    underTest =
        new ActionFieldReceiver(
            new ActionInstructionMapper(),
            fieldworkActionPublisher,
            new FieldFollowUpFilter(),
            TEST_TOPIC);
  }

  @Test
  void shouldIgnoreWhenFieldActionInstructionIsNull() {
    underTest.receiveMessage(
        constructMessage(buildEvent(null, "E92000001", EventType.CASE_UPDATE)));

    verify(fieldworkActionPublisher, never()).sendMessage(anyString(), any(), anyMap());
  }

  @Test
  void shouldPublishCreateInstructionForEligibleCase() {
    EventDTO event = buildEvent(FieldActionInstruction.CREATE, "E92000001", EventType.CASE_UPDATE);

    underTest.receiveMessage(constructMessage(event));

    PublishedMessage publishedMessage = capturePublishedMessage();
    assertThat(publishedMessage.payload()).isInstanceOf(FwmtActionInstructionDTO.class);

    FwmtActionInstructionDTO published = (FwmtActionInstructionDTO) publishedMessage.payload();
    assertThat(published.getActionInstruction()).isEqualTo(FieldActionInstruction.CREATE);
    assertThat(published.getSurveyName()).isEqualTo("Census");
    assertThat(published.getCaseId()).isEqualTo(event.getPayload().getCaseUpdate().getCaseId());
    assertThat(published.getAddressType()).isEqualTo("HH");
    assertThat(published.getCaseRef()).isEqualTo("1000000001");
    assertThat(published.getFieldOfficerId()).isEqualTo("FO12345");
    assertThat(published.getPostcode()).isEqualTo("AB1 2CD");
    assertCommonAttributes(publishedMessage.attributes(), event);
  }

  @Test
  void shouldPublishUpdateInstructionForEligibleCase() {
    EventDTO event = buildEvent(FieldActionInstruction.UPDATE, "W92000004", EventType.CASE_UPDATE);

    underTest.receiveMessage(constructMessage(event));

    PublishedMessage publishedMessage = capturePublishedMessage();
    assertThat(publishedMessage.payload()).isInstanceOf(FwmtActionInstructionDTO.class);

    FwmtActionInstructionDTO published = (FwmtActionInstructionDTO) publishedMessage.payload();
    assertThat(published.getActionInstruction()).isEqualTo(FieldActionInstruction.UPDATE);
    assertThat(published.getSurveyName()).isEqualTo("Census");
    assertThat(published.getCaseId()).isEqualTo(event.getPayload().getCaseUpdate().getCaseId());
    assertThat(published.getAddressType()).isEqualTo("HH");
    assertThat(published.getCaseRef()).isEqualTo("1000000001");
    assertThat(published.getFieldOfficerId()).isEqualTo("FO12345");
    assertThat(published.getPostcode()).isEqualTo("AB1 2CD");
    assertCommonAttributes(publishedMessage.attributes(), event);
  }

  @Test
  void shouldPublishCancelInstructionForEligibleCase() {
    EventDTO event = buildEvent(FieldActionInstruction.CANCEL, "E92000001", EventType.CASE_UPDATE);

    underTest.receiveMessage(constructMessage(event));

    PublishedMessage publishedMessage = capturePublishedMessage();
    assertThat(publishedMessage.payload()).isInstanceOf(FwmtCancelActionInstructionDTO.class);

    FwmtCancelActionInstructionDTO published =
        (FwmtCancelActionInstructionDTO) publishedMessage.payload();
    assertThat(published.getActionInstruction()).isEqualTo(FieldActionInstruction.CANCEL);
    assertThat(published.getSurveyName()).isEqualTo("Census");
    assertThat(published.getCaseId()).isEqualTo(event.getPayload().getCaseUpdate().getCaseId());
    assertThat(published.getAddressType()).isEqualTo("HH");
    assertThat(published.getAddressLevel()).isNull();
    assertCommonAttributes(publishedMessage.attributes(), event);
  }

  @Test
  void shouldSuppressExcludedRegionInstructions() {
    EventDTO northernIrelandCreate =
        buildEvent(FieldActionInstruction.CREATE, "N92000002", EventType.CASE_UPDATE);
    EventDTO scotlandUpdate =
        buildEvent(FieldActionInstruction.UPDATE, " S92000003 ", EventType.CASE_UPDATE);
    EventDTO scotlandCancel =
        buildEvent(FieldActionInstruction.CANCEL, "s92000003", EventType.CASE_UPDATE);

    underTest.receiveMessage(constructMessage(northernIrelandCreate));
    underTest.receiveMessage(constructMessage(scotlandUpdate));
    underTest.receiveMessage(constructMessage(scotlandCancel));

    verify(fieldworkActionPublisher, never()).sendMessage(anyString(), any(), anyMap());
  }

  @Test
  void shouldPublishIncludedRegionControls() {
    EventDTO englandCreate =
        buildEvent(FieldActionInstruction.CREATE, " E92000001 ", EventType.CASE_UPDATE);
    EventDTO walesUpdate =
        buildEvent(FieldActionInstruction.UPDATE, "w92000004", EventType.CASE_UPDATE);
    EventDTO englandCancel =
        buildEvent(FieldActionInstruction.CANCEL, "E12000004", EventType.CASE_UPDATE);

    underTest.receiveMessage(constructMessage(englandCreate));
    underTest.receiveMessage(constructMessage(walesUpdate));
    underTest.receiveMessage(constructMessage(englandCancel));

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(fieldworkActionPublisher, times(3))
        .sendMessage(eq(TEST_TOPIC), payloadCaptor.capture(), anyMap());

    assertThat(payloadCaptor.getAllValues())
        .hasSize(3)
        .extracting(
            payload ->
                payload instanceof FwmtActionInstructionDTO actionInstruction
                    ? actionInstruction.getActionInstruction()
                    : ((FwmtCancelActionInstructionDTO) payload).getActionInstruction())
        .containsExactly(
            FieldActionInstruction.CREATE,
            FieldActionInstruction.UPDATE,
            FieldActionInstruction.CANCEL);
  }

  @Test
  void shouldThrowForWrongMessageType() {
    EventDTO event = buildEvent(FieldActionInstruction.UPDATE, "E92000001", EventType.NEW_CASE);

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> underTest.receiveMessage(constructMessage(event)));

    assertThat(thrown.getMessage()).contains("Event Type 'NEW_CASE' is invalid on this topic");
  }

  @Test
  void shouldThrowWhenCaseUpdatePayloadMissing() {
    EventDTO event =
        buildEvent(FieldActionInstruction.UPDATE, "E92000001", EventType.CASE_UPDATE, false);

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> underTest.receiveMessage(constructMessage(event)));

    assertThat(thrown.getMessage())
        .isEqualTo("Invalid CASE_UPDATE event: payload.caseUpdate is missing");
  }

  @Test
  void shouldPropagatePublishFailures() {
    EventDTO event = buildEvent(FieldActionInstruction.CREATE, "E92000001", EventType.CASE_UPDATE);
    doThrow(new RuntimeException("publish failed"))
        .when(fieldworkActionPublisher)
        .sendMessage(eq(TEST_TOPIC), any(FwmtActionInstructionDTO.class), anyMap());

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> underTest.receiveMessage(constructMessage(event)));

    assertThat(thrown.getMessage()).isEqualTo("publish failed");
  }

  private PublishedMessage capturePublishedMessage() {
    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> attributesCaptor = ArgumentCaptor.forClass(Map.class);

    verify(fieldworkActionPublisher)
        .sendMessage(eq(TEST_TOPIC), payloadCaptor.capture(), attributesCaptor.capture());

    return new PublishedMessage(payloadCaptor.getValue(), attributesCaptor.getValue());
  }

  private void assertCommonAttributes(Map<String, String> attributes, EventDTO event) {
    assertThat(attributes)
        .containsEntry("eventId", event.getHeader().getMessageId().toString())
        .containsEntry("correlationId", event.getHeader().getCorrelationId().toString())
        .containsEntry("caseId", event.getPayload().getCaseUpdate().getCaseId().toString())
        .containsEntry("eventType", event.getHeader().getMessageType().name())
        .containsEntry("schemaVersion", OUTBOUND_EVENT_SCHEMA_VERSION)
        .containsEntry("occurredAt", EVENT_TIME.toString());
  }

  private EventDTO buildEvent(
      FieldActionInstruction instruction, String region, EventType messageType) {
    return buildEvent(instruction, region, messageType, true);
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
    header.setCorrelationId(UUID.randomUUID());
    header.setMessageId(UUID.randomUUID());
    header.setDateTime(EVENT_TIME);

    PayloadDTO payload = new PayloadDTO();
    if (includeCaseUpdate) {
      Address address = new Address();
      address.setRegion(region);
      address.setAddressType("HH");
      address.setAddressLevel("U");
      address.setEstabType("Residential Property");
      address.setOrganisationName("Example Organisation Ltd");
      address.setAddressLine1("1 High Street");
      address.setAddressLine2("Business Park");
      address.setAddressLine3("Unit 5");
      address.setTownName("Newport");
      address.setPostcode("AB1 2CD");
      address.setLatitude("51.5");
      address.setLongitude("-1.2");
      address.setUprn("10000000000");

      CaseUpdateDTO caseUpdate = new CaseUpdateDTO();
      caseUpdate.setCaseId(UUID.randomUUID());
      caseUpdate.setCaseRef("1000000001");
      caseUpdate.setCaseType("HH");
      caseUpdate.setFieldOfficerId("FO12345");
      caseUpdate.setFieldCoordinatorId("FC001");
      caseUpdate.setOa("E00000001");
      caseUpdate.setUndeliveredAsAddress(true);
      caseUpdate.setBlankFormReturned(false);
      caseUpdate.setAddress(address);

      payload.setCaseUpdate(caseUpdate);
    }

    EventDTO event = new EventDTO();
    event.setHeader(header);
    event.setPayload(payload);
    return event;
  }

  private record PublishedMessage(Object payload, Map<String, String> attributes) {}
}
