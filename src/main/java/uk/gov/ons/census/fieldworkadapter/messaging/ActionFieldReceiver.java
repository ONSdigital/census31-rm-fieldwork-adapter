package uk.gov.ons.census.fieldworkadapter.messaging;

import static uk.gov.ons.census.fieldworkadapter.utils.JsonHelper.convertJsonBytesToEvent;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import uk.gov.ons.census.common.model.entity.EventType;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventHeaderDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FieldActionInstruction;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtCancelActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.service.FieldFollowUpFilter;
import uk.gov.ons.census.fieldworkadapter.service.FieldFollowUpFilter.Exclusion;
import uk.gov.ons.census.fieldworkadapter.utils.ActionInstructionMapper;

@MessageEndpoint
public class ActionFieldReceiver {
  private static final Logger log = LoggerFactory.getLogger(ActionFieldReceiver.class);
  private static final String EVENT_TYPE = "CASE_UPDATE";
  private static final String SCHEMA_VERSION_UNKNOWN = "unknown";

  private final ActionInstructionMapper actionInstructionMapper;
  private final FieldworkActionPublisher fieldworkActionPublisher;
  private final FieldFollowUpFilter fieldFollowUpFilter;
  private final String fwmtActionInstructionTopic;

  public ActionFieldReceiver(
      ActionInstructionMapper actionInstructionMapper,
      FieldworkActionPublisher fieldworkActionPublisher,
      FieldFollowUpFilter fieldFollowUpFilter,
      @Value("${queueconfig.fieldwork-action-instruction-topic}")
          String fwmtActionInstructionTopic) {
    this.actionInstructionMapper = actionInstructionMapper;
    this.fieldworkActionPublisher = fieldworkActionPublisher;
    this.fieldFollowUpFilter = fieldFollowUpFilter;
    this.fwmtActionInstructionTopic = fwmtActionInstructionTopic;
  }

  @ServiceActivator(inputChannel = "actionFieldInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {

    EventDTO event = convertJsonBytesToEvent(message.getPayload());
    validateEvent(event);

    EventHeaderDTO header = event.getHeader();
    CaseUpdateDTO caseUpdate = event.getPayload().getCaseUpdate();

    switch (header.getFieldActionInstruction()) {
      case null -> logOutcome("IGNORED_NO_INSTRUCTION", caseUpdate, null, null);
      case UPDATE ->
          handleForwardableInstruction(event, message, caseUpdate, FieldActionInstruction.UPDATE);
      case CREATE ->
          handleForwardableInstruction(event, message, caseUpdate, FieldActionInstruction.CREATE);
      case CANCEL -> handleCancelInstruction(event, message, caseUpdate);
      default ->
          throw new RuntimeException(
              String.format(
                  "Unsupported fieldActionInstruction '%s' for caseId=%s",
                  header.getFieldActionInstruction(), caseUpdate.getCaseId()));
    }
  }

  private void handleCancelInstruction(
      EventDTO event, Message<byte[]> message, CaseUpdateDTO caseUpdate) {
    var exclusion = fieldFollowUpFilter.exclusionFor(caseUpdate);
    if (exclusion.isPresent()) {
      logOutcome(
          "SUPPRESSED_INVALID_FOR_FIELD_FOLLOWUP",
          caseUpdate,
          FieldActionInstruction.CANCEL,
          exclusion.get());
      return;
    }

    FwmtCancelActionInstructionDTO actionInstruction =
        actionInstructionMapper.toFwmtCancelActionInstruction(caseUpdate);

    Map<String, String> attributes = buildAttributes(event, caseUpdate, message);
    try {
      fieldworkActionPublisher.sendMessage(
          fwmtActionInstructionTopic, actionInstruction, attributes);
      logOutcome("PUBLISHED", caseUpdate, FieldActionInstruction.CANCEL, null);
    } catch (RuntimeException ex) {
      logOutcome("PUBLISH_FAILED", caseUpdate, FieldActionInstruction.CANCEL, null);
      throw ex;
    }
  }

  private void handleForwardableInstruction(
      EventDTO event,
      Message<byte[]> message,
      CaseUpdateDTO caseUpdate,
      FieldActionInstruction fieldActionInstruction) {
    var exclusion = fieldFollowUpFilter.exclusionFor(caseUpdate);
    if (exclusion.isPresent()) {
      logOutcome(
          "SUPPRESSED_INVALID_FOR_FIELD_FOLLOWUP",
          caseUpdate,
          fieldActionInstruction,
          exclusion.get());
      return;
    }

    FwmtActionInstructionDTO actionInstruction =
        actionInstructionMapper.toFwmtActionInstruction(caseUpdate, fieldActionInstruction);

    Map<String, String> attributes = buildAttributes(event, caseUpdate, message);
    try {
      fieldworkActionPublisher.sendMessage(
          fwmtActionInstructionTopic, actionInstruction, attributes);
      logOutcome("PUBLISHED", caseUpdate, fieldActionInstruction, null);
    } catch (RuntimeException ex) {
      logOutcome("PUBLISH_FAILED", caseUpdate, fieldActionInstruction, null);
      throw ex;
    }
  }

  private void validateEvent(EventDTO event) {
    if (event == null || event.getHeader() == null || event.getPayload() == null) {
      throw new RuntimeException("Invalid CASE_UPDATE event: missing header and/or payload");
    }

    if (event.getHeader().getMessageType() != EventType.CASE_UPDATE) {
      throw new RuntimeException(
          String.format(
              "Event Type '%s' is invalid on this topic", event.getHeader().getMessageType()));
    }

    if (event.getPayload().getCaseUpdate() == null) {
      throw new RuntimeException("Invalid CASE_UPDATE event: payload.caseUpdate is missing");
    }
  }

  private Map<String, String> buildAttributes(
      EventDTO event, CaseUpdateDTO caseUpdate, Message<byte[]> message) {
    EventHeaderDTO header = event.getHeader();
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("eventId", extractEventId(message, header));
    attributes.put(
        "correlationId",
        header.getCorrelationId() == null ? "" : header.getCorrelationId().toString());
    attributes.put(
        "caseId", caseUpdate.getCaseId() == null ? "" : caseUpdate.getCaseId().toString());
    attributes.put(
        "eventType", header.getMessageType() == null ? EVENT_TYPE : header.getMessageType().name());
    attributes.put(
        "schemaVersion",
        header.getVersion() == null || header.getVersion().isBlank()
            ? SCHEMA_VERSION_UNKNOWN
            : header.getVersion());
    attributes.put(
        "occurredAt", header.getDateTime() == null ? "" : header.getDateTime().toString());
    return attributes;
  }

  private void logOutcome(
      String outcome,
      CaseUpdateDTO caseUpdate,
      FieldActionInstruction instruction,
      Exclusion exclusion) {
    var logBuilder =
        log.atInfo()
            .setMessage("Fieldwork action processing outcome")
            .addKeyValue("outcome", outcome)
            .addKeyValue("caseId", caseUpdate.getCaseId())
            .addKeyValue("fieldActionInstruction", instruction);

    if (exclusion != null) {
      logBuilder.addKeyValue("exclusionReason", exclusion.name());
    }

    logBuilder.log();
  }

  private String extractEventId(Message<byte[]> message, EventHeaderDTO header) {
    if (header.getMessageId() != null) {
      return header.getMessageId().toString();
    }

    Object original = message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE);
    if (original instanceof BasicAcknowledgeablePubsubMessage basicMessage) {
      return basicMessage.getPubsubMessage().getMessageId();
    }

    return "";
  }
}
