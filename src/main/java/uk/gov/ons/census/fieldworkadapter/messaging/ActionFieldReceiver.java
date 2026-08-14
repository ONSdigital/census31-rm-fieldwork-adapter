package uk.gov.ons.census.fieldworkadapter.messaging;

import static uk.gov.ons.census.fieldworkadapter.utils.JsonHelper.convertJsonBytesToEvent;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.common.model.entity.EventType;
import uk.gov.ons.census.fieldworkadapter.logging.EventLogger;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventHeaderDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FieldActionInstruction;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtCancelActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.utils.ActionInstructionMapper;

@MessageEndpoint
public class ActionFieldReceiver {

  private static final String NISRA_REGION = "N";

  private final ActionInstructionMapper actionInstructionMapper;
  private final MessageSender messageSender;
  private final EventLogger eventLogger;

  @Value("${queueconfig.fieldwork-action-instruction-topic}")
  private String fwmtActionInstructionTopic;

  public ActionFieldReceiver(
      ActionInstructionMapper actionInstructionMapper,
      MessageSender messageSender,
      EventLogger eventLogger) {
    this.actionInstructionMapper = actionInstructionMapper;
    this.messageSender = messageSender;
    this.eventLogger = eventLogger;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  @ServiceActivator(inputChannel = "actionFieldInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {

    EventDTO event = convertJsonBytesToEvent(message.getPayload());
    validateEvent(event);

    EventHeaderDTO header = event.getHeader();
    CaseUpdateDTO caseUpdate = event.getPayload().getCaseUpdate();

    switch (header.getFieldActionInstruction()) {
      case null -> {
        eventLogger.logEvent(
            "Ignoring CASE_UPDATE with null fieldActionInstruction",
            EventType.CASE_UPDATE,
            event,
            caseUpdate,
            message);
      }
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
    if (isNisraCase(caseUpdate)) {
      eventLogger.logEvent(
          "Skipping NISRA CASE_UPDATE for CANCEL instruction",
          EventType.CASE_UPDATE,
          event,
          caseUpdate,
          message);
      return;
    }

    FwmtCancelActionInstructionDTO actionInstruction =
        actionInstructionMapper.toFwmtCancelActionInstruction(caseUpdate);

    messageSender.sendMessage(fwmtActionInstructionTopic, actionInstruction);
    eventLogger.logEvent(
        "Published CANCEL fieldwork action instruction",
        EventType.CASE_UPDATE,
        event,
        actionInstruction,
        message);
  }

  private void handleForwardableInstruction(
      EventDTO event,
      Message<byte[]> message,
      CaseUpdateDTO caseUpdate,
      FieldActionInstruction fieldActionInstruction) {
    if (isNisraCase(caseUpdate)) {
      eventLogger.logEvent(
          String.format("Skipping NISRA CASE_UPDATE for %s instruction", fieldActionInstruction),
          EventType.CASE_UPDATE,
          event,
          caseUpdate,
          message);
      return;
    }

    FwmtActionInstructionDTO actionInstruction =
        actionInstructionMapper.toFwmtActionInstruction(caseUpdate, fieldActionInstruction);

    messageSender.sendMessage(fwmtActionInstructionTopic, actionInstruction);
    eventLogger.logEvent(
        String.format("Published %s fieldwork action instruction", fieldActionInstruction),
        EventType.CASE_UPDATE,
        event,
        actionInstruction,
        message);
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
