package uk.gov.ons.census.fieldworkadapter.messaging;

import static uk.gov.ons.census.fieldworkadapter.utils.JsonHelper.convertJsonBytesToEvent;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import java.util.LinkedHashMap;
import java.util.Locale;
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
import uk.gov.ons.census.fieldworkadapter.utils.ActionInstructionMapper;

@MessageEndpoint
public class ActionFieldReceiver {
  private static final Logger log = LoggerFactory.getLogger(ActionFieldReceiver.class);
  private static final String NISRA_REGION = "N";
  private static final String EVENT_TYPE = "CASE_UPDATE";
  private static final String SCHEMA_VERSION_UNKNOWN = "unknown";

  private final ActionInstructionMapper actionInstructionMapper;
  private final FieldworkActionPublisher fieldworkActionPublisher;
  private final EventIdFactory eventIdFactory;

  @Value("${queueconfig.fieldwork-action-instruction-topic}")
  private String fwmtActionInstructionTopic;

  public ActionFieldReceiver(
      ActionInstructionMapper actionInstructionMapper,
      FieldworkActionPublisher fieldworkActionPublisher,
      EventIdFactory eventIdFactory) {
    this.actionInstructionMapper = actionInstructionMapper;
    this.fieldworkActionPublisher = fieldworkActionPublisher;
    this.eventIdFactory = eventIdFactory;
  }

  @ServiceActivator(inputChannel = "actionFieldInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {

    EventDTO event = convertJsonBytesToEvent(message.getPayload());
    validateEvent(event);

    EventHeaderDTO header = event.getHeader();
    CaseUpdateDTO caseUpdate = event.getPayload().getCaseUpdate();

    switch (header.getFieldActionInstruction()) {
      case null -> {
        logOutcome("IGNORED_NO_INSTRUCTION", event, caseUpdate, null, message, null);
      }
      case UPDATE ->
          handleForwardableInstruction(event, message, caseUpdate, FieldActionInstruction.UPDATE);
      case CREATE ->
          handleForwardableInstruction(event, message, caseUpdate, FieldActionInstruction.CREATE);
      case CANCEL -> handleCancelInstruction(event, message, caseUpdate);
      default ->
          throw new NonRetryableEventException(
              String.format(
                  "Unsupported fieldActionInstruction '%s' for caseId=%s",
                  header.getFieldActionInstruction(), caseUpdate.getCaseId()));
    }
  }

  private void handleCancelInstruction(
      EventDTO event, Message<byte[]> message, CaseUpdateDTO caseUpdate) {
    if (isNisraCase(caseUpdate)) {
      logOutcome(
          "SUPPRESSED_NISRA", event, caseUpdate, FieldActionInstruction.CANCEL, message, null);
      return;
    }

    FwmtCancelActionInstructionDTO actionInstruction =
        actionInstructionMapper.toFwmtCancelActionInstruction(caseUpdate);

    Map<String, String> attributes =
        buildAttributes(event, caseUpdate, FieldActionInstruction.CANCEL, message);
    try {
      String pubSubMessageId =
          fieldworkActionPublisher.sendMessage(
              fwmtActionInstructionTopic, actionInstruction, attributes);
      logOutcome(
          "PUBLISHED", event, caseUpdate, FieldActionInstruction.CANCEL, message, pubSubMessageId);
    } catch (PublishFailedException ex) {
      logOutcome("PUBLISH_FAILED", event, caseUpdate, FieldActionInstruction.CANCEL, message, null);
      throw ex;
    }
  }

  private void handleForwardableInstruction(
      EventDTO event,
      Message<byte[]> message,
      CaseUpdateDTO caseUpdate,
      FieldActionInstruction fieldActionInstruction) {
    if (isNisraCase(caseUpdate)) {
      logOutcome("SUPPRESSED_NISRA", event, caseUpdate, fieldActionInstruction, message, null);
      return;
    }

    FwmtActionInstructionDTO actionInstruction =
        actionInstructionMapper.toFwmtActionInstruction(caseUpdate, fieldActionInstruction);

    Map<String, String> attributes =
        buildAttributes(event, caseUpdate, fieldActionInstruction, message);
    try {
      String pubSubMessageId =
          fieldworkActionPublisher.sendMessage(
              fwmtActionInstructionTopic, actionInstruction, attributes);
      logOutcome("PUBLISHED", event, caseUpdate, fieldActionInstruction, message, pubSubMessageId);
    } catch (PublishFailedException ex) {
      logOutcome("PUBLISH_FAILED", event, caseUpdate, fieldActionInstruction, message, null);
      throw ex;
    }
  }

  private void validateEvent(EventDTO event) {
    if (event == null || event.getHeader() == null || event.getPayload() == null) {
      throw new NonRetryableEventException(
          "Invalid CASE_UPDATE event: missing header and/or payload");
    }

    if (event.getHeader().getMessageType() != EventType.CASE_UPDATE) {
      throw new NonRetryableEventException(
          String.format(
              "Event Type '%s' is invalid on this topic", event.getHeader().getMessageType()));
    }

    if (event.getPayload().getCaseUpdate() == null) {
      throw new NonRetryableEventException(
          "Invalid CASE_UPDATE event: payload.caseUpdate is missing");
    }
  }

  private Map<String, String> buildAttributes(
      EventDTO event,
      CaseUpdateDTO caseUpdate,
      FieldActionInstruction instruction,
      Message<byte[]> message) {
    EventHeaderDTO header = event.getHeader();
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put(
        "eventId", eventIdFactory.createDeterministicId(header, caseUpdate, instruction.name()));
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
    attributes.put("traceparent", extractTraceparent(message));
    return attributes;
  }

  private void logOutcome(
      String outcome,
      EventDTO event,
      CaseUpdateDTO caseUpdate,
      FieldActionInstruction instruction,
      Message<byte[]> message,
      String pubSubMessageId) {
    EventHeaderDTO header = event.getHeader();
    log.atInfo()
        .setMessage("Fieldwork action processing outcome")
        .addKeyValue("outcome", outcome)
        .addKeyValue("caseId", caseUpdate.getCaseId())
        .addKeyValue(
            "eventId",
            eventIdFactory.createDeterministicId(
                header, caseUpdate, instruction == null ? "" : instruction.name()))
        .addKeyValue("fieldActionInstruction", instruction)
        .addKeyValue("inboundMessageId", extractInboundMessageId(message, header))
        .addKeyValue("pubsubMessageId", pubSubMessageId)
        .addKeyValue("traceparent", extractTraceparent(message))
        .log();
  }

  private String extractTraceparent(Message<byte[]> message) {
    Object traceparent = message.getHeaders().get("traceparent");
    return traceparent == null ? "" : traceparent.toString();
  }

  private String extractInboundMessageId(Message<byte[]> message, EventHeaderDTO header) {
    if (header.getMessageId() != null) {
      return header.getMessageId().toString();
    }

    Object original = message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE);
    if (original instanceof BasicAcknowledgeablePubsubMessage basicMessage) {
      return basicMessage.getPubsubMessage().getMessageId();
    }

    return "";
  }

  /**
   * Returns {@code true} if the case is a NISRA case (region begins with "N", case-insensitive).
   * NISRA cases must be excluded from fieldwork for the 2027 test.
   */
  private boolean isNisraCase(CaseUpdateDTO caseUpdate) {
    if (caseUpdate.getAddress() == null || caseUpdate.getAddress().getRegion() == null) {
      return false;
    }

    String region = caseUpdate.getAddress().getRegion().trim();
    return !region.isEmpty() && region.toUpperCase(Locale.ROOT).startsWith(NISRA_REGION);
  }
}
