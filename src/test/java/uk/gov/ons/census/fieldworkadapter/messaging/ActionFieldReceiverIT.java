package uk.gov.ons.census.fieldworkadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.census.fieldworkadapter.testutils.MessageConstructor.constructMessage;
import static uk.gov.ons.census.fieldworkadapter.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
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
import uk.gov.ons.census.fieldworkadapter.model.dto.RefusalTypeDTO;
import uk.gov.ons.census.fieldworkadapter.service.FieldFollowUpFilter;
import uk.gov.ons.census.fieldworkadapter.utils.ActionInstructionMapper;
import uk.gov.ons.census.fieldworkadapter.utils.JsonHelper;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
      ActionFieldReceiver.class,
      ActionInstructionMapper.class,
      FieldFollowUpFilter.class,
      ActionFieldReceiverIT.TestConfig.class
    })
@TestPropertySource(
    properties = {
      "queueconfig.fieldwork-action-instruction-topic=event_fieldwork_action-instruction",
      "queueconfig.publishtimeout=5"
    })
class ActionFieldReceiverIT {

  public static final String TEST_TOPIC = "event_fieldwork_action-instruction";

  @Configuration
  static class TestConfig {
    @Bean
    FieldworkActionPublisher fieldworkActionPublisher() {
      return Mockito.mock(FieldworkActionPublisher.class);
    }
  }

  @Autowired private ActionFieldReceiver underTest;
  @Autowired private FieldworkActionPublisher fieldworkActionPublisher;

  @BeforeEach
  void resetMocks() {
    Mockito.reset(fieldworkActionPublisher);
  }

  @Test
  void shouldPublishCreateActionInstruction() {
    EventDTO event = buildEvent(FieldActionInstruction.CREATE, "E", EventType.CASE_UPDATE);
    Message<byte[]> message = constructMessage(event);

    underTest.receiveMessage(message);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(fieldworkActionPublisher)
        .sendMessage(eq(TEST_TOPIC), payloadCaptor.capture(), attributesCaptor.capture());

    assertThat(payloadCaptor.getValue()).isInstanceOf(FwmtActionInstructionDTO.class);
    FwmtActionInstructionDTO published = (FwmtActionInstructionDTO) payloadCaptor.getValue();
    assertThat(published.getActionInstruction()).isEqualTo(FieldActionInstruction.CREATE);
    assertThat(published.getSurveyName()).isEqualTo("Census");
    assertThat(published.getCaseId()).isEqualTo(event.getPayload().getCaseUpdate().getCaseId());
    assertThat(attributesCaptor.getValue())
        .containsEntry("eventId", event.getHeader().getMessageId().toString())
        .containsKeys("correlationId", "caseId", "eventType", "schemaVersion", "occurredAt");
  }

  @Test
  void shouldPublishCancelActionInstruction() {
    EventDTO event = buildEvent(FieldActionInstruction.CANCEL, "E", EventType.CASE_UPDATE);
    Message<byte[]> message = constructMessage(event);

    underTest.receiveMessage(message);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(fieldworkActionPublisher).sendMessage(eq(TEST_TOPIC), payloadCaptor.capture(), any());

    assertThat(payloadCaptor.getValue()).isInstanceOf(FwmtCancelActionInstructionDTO.class);
    FwmtCancelActionInstructionDTO published =
        (FwmtCancelActionInstructionDTO) payloadCaptor.getValue();
    assertThat(published.getActionInstruction()).isEqualTo(FieldActionInstruction.CANCEL);
    assertThat(published.getSurveyName()).isEqualTo("Census");
    assertThat(published.getCaseId()).isEqualTo(event.getPayload().getCaseUpdate().getCaseId());
    verify(fieldworkActionPublisher, times(1)).sendMessage(eq(TEST_TOPIC), any(), any());
  }

  @Test
  void shouldPublishUpdateActionInstruction() {
    EventDTO event = buildEvent(FieldActionInstruction.UPDATE, "E", EventType.CASE_UPDATE);
    Message<byte[]> message = constructMessage(event);

    underTest.receiveMessage(message);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(fieldworkActionPublisher).sendMessage(eq(TEST_TOPIC), payloadCaptor.capture(), any());

    assertThat(payloadCaptor.getValue()).isInstanceOf(FwmtActionInstructionDTO.class);
    FwmtActionInstructionDTO published = (FwmtActionInstructionDTO) payloadCaptor.getValue();
    assertThat(published.getActionInstruction()).isEqualTo(FieldActionInstruction.UPDATE);
    assertThat(published.getSurveyName()).isEqualTo("Census");
    assertThat(published.getCaseId()).isEqualTo(event.getPayload().getCaseUpdate().getCaseId());
    verify(fieldworkActionPublisher, times(1)).sendMessage(eq(TEST_TOPIC), any(), any());
  }

  @Test
  void shouldPublishCeUnitCreatePayloadWithDocumentedFields() {
    EventDTO event =
        buildEvent(FieldActionInstruction.CREATE, "E92000001", EventType.CASE_UPDATE, "CE", "U");

    underTest.receiveMessage(constructMessage(event));

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(fieldworkActionPublisher).sendMessage(eq(TEST_TOPIC), payloadCaptor.capture(), any());

    FwmtActionInstructionDTO published = (FwmtActionInstructionDTO) payloadCaptor.getValue();
    String json = JsonHelper.convertObjectToJson(published);

    assertThat(published.getAddressType()).isEqualTo("CE");
    assertThat(published.getAddressLevel()).isEqualTo("U");
    assertThat(published.getFieldCoordinatorId()).isEqualTo("FC001");
    assertThat(published.getUprn()).isEqualTo("10000000000");
    assertThat(published.getEstabUprn()).isEqualTo("20000000000");
    assertThat(published.getCeExpectedCapacity()).isEqualTo(42);
    assertThat(published.getCeActualResponses()).isEqualTo(11);
    assertThat(published.getSecureEstablishment()).isFalse();
    assertThat(published.getUndeliveredAsAddress()).isTrue();
    assertThat(json)
        .contains(
            "\"addressLevel\":\"U\"",
            "\"fieldCoordinatorId\":\"FC001\"",
            "\"estabUprn\":\"20000000000\"",
            "\"ceExpectedCapacity\":42",
            "\"ceActualResponses\":11")
        .doesNotContain("blankFormReturned", "ce1Complete", "handDeliver");
  }

  @Test
  void shouldPublishCeUpdatePayloadWithMinimalDocumentedFields() {
    EventDTO event =
        buildEvent(FieldActionInstruction.UPDATE, "E92000001", EventType.CASE_UPDATE, "CE", "E");

    underTest.receiveMessage(constructMessage(event));

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(fieldworkActionPublisher).sendMessage(eq(TEST_TOPIC), payloadCaptor.capture(), any());

    FwmtActionInstructionDTO published = (FwmtActionInstructionDTO) payloadCaptor.getValue();
    String json = JsonHelper.convertObjectToJson(published);

    assertThat(published.getAddressType()).isEqualTo("CE");
    assertThat(published.getAddressLevel()).isEqualTo("E");
    assertThat(published.getCeExpectedCapacity()).isEqualTo(42);
    assertThat(published.getCeActualResponses()).isEqualTo(11);
    assertThat(json)
        .contains("\"ceExpectedCapacity\":42", "\"ceActualResponses\":11")
        .doesNotContain(
            "caseRef",
            "fieldOfficerId",
            "fieldCoordinatorId",
            "addressLine1",
            "organisationName",
            "uprn",
            "secureEstablishment");
  }

  @Test
  void shouldPublishHouseholdCancelPayloadWithoutAddressLevel() {
    EventDTO event =
        buildEvent(FieldActionInstruction.CANCEL, "E92000001", EventType.CASE_UPDATE, "HH", "U");

    underTest.receiveMessage(constructMessage(event));

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(fieldworkActionPublisher).sendMessage(eq(TEST_TOPIC), payloadCaptor.capture(), any());

    FwmtCancelActionInstructionDTO published =
        (FwmtCancelActionInstructionDTO) payloadCaptor.getValue();
    String json = JsonHelper.convertObjectToJson(published);

    assertThat(published.getAddressType()).isEqualTo("HH");
    assertThat(published.getAddressLevel()).isNull();
    assertThat(json).doesNotContain("addressLevel", "ceExpectedCapacity", "ceActualResponses");
  }

  @Test
  void shouldSuppressNisraMessages() {
    EventDTO createEvent = buildEvent(FieldActionInstruction.CREATE, "N", EventType.CASE_UPDATE);
    EventDTO updateEvent = buildEvent(FieldActionInstruction.UPDATE, "N", EventType.CASE_UPDATE);
    EventDTO cancelEvent = buildEvent(FieldActionInstruction.CANCEL, "N", EventType.CASE_UPDATE);

    underTest.receiveMessage(constructMessage(createEvent));
    underTest.receiveMessage(constructMessage(updateEvent));
    underTest.receiveMessage(constructMessage(cancelEvent));

    verify(fieldworkActionPublisher, never()).sendMessage(any(), any(), any());
  }

  @Test
  void shouldSuppressKnownNiRegionContractSamples() {
    EventDTO niLowerCaseRegionOnly =
        buildEvent(FieldActionInstruction.CREATE, "n", EventType.CASE_UPDATE);
    EventDTO niOnsCode =
        buildEvent(FieldActionInstruction.UPDATE, "N92000002", EventType.CASE_UPDATE);
    EventDTO niLowerCaseOnsCode =
        buildEvent(FieldActionInstruction.CANCEL, "n92000002", EventType.CASE_UPDATE);
    EventDTO niCodeWithWhitespace =
        buildEvent(FieldActionInstruction.CREATE, " N92000002 ", EventType.CASE_UPDATE);

    underTest.receiveMessage(constructMessage(niLowerCaseRegionOnly));
    underTest.receiveMessage(constructMessage(niOnsCode));
    underTest.receiveMessage(constructMessage(niLowerCaseOnsCode));
    underTest.receiveMessage(constructMessage(niCodeWithWhitespace));

    verify(fieldworkActionPublisher, never()).sendMessage(any(), any(), any());
  }

  @Test
  void shouldSuppressCancelForCn80FieldFollowUpExclusions() {
    // CN-80: CANCEL messages only exclude NISRA (N region) cases.
    // Other CN-80 exclusion rules (invalid, refusal, HI case, treatment codes, Scotland)
    // do NOT apply to CANCEL messages - they are still sent to fieldwork.

    // These CANCEL messages should be SENT despite having CN-80 exclusion attributes,
    // because they are not in NISRA (N region)
    EventDTO invalidCancel =
        buildEvent(
            FieldActionInstruction.CANCEL,
            "E92000001",
            EventType.CASE_UPDATE,
            caseUpdate -> caseUpdate.setInvalid(true));
    EventDTO refusalCancel =
        buildEvent(
            FieldActionInstruction.CANCEL,
            "E92000001",
            EventType.CASE_UPDATE,
            caseUpdate -> caseUpdate.setRefusalReceived(RefusalTypeDTO.HARD_REFUSAL));
    EventDTO hiCaseCancel =
        buildEvent(
            FieldActionInstruction.CANCEL,
            "E92000001",
            EventType.CASE_UPDATE,
            caseUpdate -> caseUpdate.setCaseType("HI"));
    EventDTO onlineOnlyCancel =
        buildEvent(
            FieldActionInstruction.CANCEL,
            "E92000001",
            EventType.CASE_UPDATE,
            caseUpdate -> caseUpdate.setTreatmentCode("HH_ONE"));
    EventDTO scottishRegionCancel =
        buildEvent(FieldActionInstruction.CANCEL, "S92000003", EventType.CASE_UPDATE);

    underTest.receiveMessage(constructMessage(invalidCancel));
    underTest.receiveMessage(constructMessage(refusalCancel));
    underTest.receiveMessage(constructMessage(hiCaseCancel));
    underTest.receiveMessage(constructMessage(onlineOnlyCancel));
    underTest.receiveMessage(constructMessage(scottishRegionCancel));

    // All 5 messages should be published (sent to fieldwork) because none are NISRA
    verify(fieldworkActionPublisher, times(5)).sendMessage(any(), any(), any());
  }

  @Test
  void shouldSuppressCancelForNisraRegionOnly() {
    // CN-80: CANCEL messages with NISRA (N region) should be suppressed
    EventDTO nisraCancel =
        buildEvent(FieldActionInstruction.CANCEL, "N92000002", EventType.CASE_UPDATE);
    EventDTO nisraCancelLowercase =
        buildEvent(FieldActionInstruction.CANCEL, "n92000002", EventType.CASE_UPDATE);

    underTest.receiveMessage(constructMessage(nisraCancel));
    underTest.receiveMessage(constructMessage(nisraCancelLowercase));

    // Neither NISRA CANCEL should be published
    verify(fieldworkActionPublisher, never()).sendMessage(any(), any(), any());
  }

  @Test
  void shouldPublishForNonNiRegionContractControls() {
    EventDTO englandCreate =
        buildEvent(FieldActionInstruction.CREATE, "E92000001", EventType.CASE_UPDATE);
    EventDTO walesUpdate =
        buildEvent(FieldActionInstruction.UPDATE, "W92000004", EventType.CASE_UPDATE);
    EventDTO englandCancel =
        buildEvent(FieldActionInstruction.CANCEL, "E12000004", EventType.CASE_UPDATE);

    underTest.receiveMessage(constructMessage(englandCreate));
    underTest.receiveMessage(constructMessage(walesUpdate));
    underTest.receiveMessage(constructMessage(englandCancel));

    verify(fieldworkActionPublisher, times(3)).sendMessage(eq(TEST_TOPIC), any(), any());
  }

  @Test
  void shouldRejectWrongMessageType() {
    EventDTO event = buildEvent(FieldActionInstruction.UPDATE, "E", EventType.NEW_CASE);

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> underTest.receiveMessage(constructMessage(event)));

    assertThat(thrown.getMessage()).contains("Event Type 'NEW_CASE' is invalid on this topic");
  }

  private EventDTO buildEvent(
      FieldActionInstruction instruction, String region, EventType messageType) {
    return buildEvent(instruction, region, messageType, "HH", "U");
  }

  private EventDTO buildEvent(
      FieldActionInstruction instruction,
      String region,
      EventType messageType,
      Consumer<CaseUpdateDTO> caseUpdateMutator) {
    return buildEvent(instruction, region, messageType, "HH", "U", caseUpdateMutator);
  }

  private EventDTO buildEvent(
      FieldActionInstruction instruction,
      String region,
      EventType messageType,
      String addressType,
      String addressLevel) {
    return buildEvent(
        instruction, region, messageType, addressType, addressLevel, caseUpdate -> {});
  }

  private EventDTO buildEvent(
      FieldActionInstruction instruction,
      String region,
      EventType messageType,
      String addressType,
      String addressLevel,
      Consumer<CaseUpdateDTO> caseUpdateMutator) {
    EventHeaderDTO header = new EventHeaderDTO();
    header.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    header.setMessageType(messageType);
    header.setFieldActionInstruction(instruction);
    header.setMessageId(UUID.randomUUID());
    header.setCorrelationId(UUID.randomUUID());
    header.setDateTime(OffsetDateTime.parse("2026-08-25T10:15:30Z"));

    Address address = new Address();
    address.setRegion(region);
    address.setAddressType(addressType);
    address.setAddressLevel(addressLevel);
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
    address.setEstabUprn("20000000000");

    CaseUpdateDTO caseUpdate = new CaseUpdateDTO();
    caseUpdate.setCaseId(UUID.randomUUID());
    caseUpdate.setCaseRef("1000000001");
    caseUpdate.setCaseType("HH");
    caseUpdate.setFieldOfficerId("FO12345");
    caseUpdate.setFieldCoordinatorId("FC001");
    caseUpdate.setOa("E00000001");
    caseUpdate.setCeExpectedCapacity(42);
    caseUpdate.setCeActualResponses(11);
    caseUpdate.setUndeliveredAsAddress(true);
    caseUpdate.setBlankFormReturned(false);
    caseUpdate.setSecureEstablishment(false);
    caseUpdate.setAddress(address);
    caseUpdateMutator.accept(caseUpdate);

    PayloadDTO payload = new PayloadDTO();
    payload.setCaseUpdate(caseUpdate);

    EventDTO event = new EventDTO();
    event.setHeader(header);
    event.setPayload(payload);

    return event;
  }
}
