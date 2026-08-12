package uk.gov.ons.census.fieldworkadapter.messaging;

import static uk.gov.ons.census.fieldworkadapter.utils.JsonHelper.convertJsonBytesToEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
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

  private final ActionInstructionMapper actionInstructionMapper;
  private final MessageSender messageSender;

  @Value("${queueconfig.fieldwork-action-instruction-topic}")
  private String fwmtActionInstructionTopic;

  public ActionFieldReceiver(
      ActionInstructionMapper actionInstructionMapper, MessageSender messageSender) {
    this.actionInstructionMapper = actionInstructionMapper;
    this.messageSender = messageSender;
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
        if (log.isDebugEnabled()) {
          log.debug(
              "Swallowing CASE_UPDATE with null fieldActionInstruction, caseId={}",
              caseUpdate.getCaseId());
        }
      }
      case UPDATE -> handleForwardableInstruction(caseUpdate, FieldActionInstruction.UPDATE);
      case CREATE -> handleForwardableInstruction(caseUpdate, FieldActionInstruction.CREATE);
      case CANCEL -> handleCancelInstruction(caseUpdate);
      default ->
          throw new RuntimeException(
              String.format(
                  "Unsupported fieldActionInstruction '%s' for caseId=%s",
                  header.getFieldActionInstruction(), caseUpdate.getCaseId()));
    }
  }

  private void handleCancelInstruction(CaseUpdateDTO caseUpdate) {
    if (isNisraCase(caseUpdate)) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Skipping NISRA CASE_UPDATE for CANCEL instruction, caseId={}", caseUpdate.getCaseId());
      }
      return;
    }

    FwmtCancelActionInstructionDTO actionInstruction =
        actionInstructionMapper.toFwmtCancelActionInstruction(caseUpdate);

    messageSender.sendMessage(fwmtActionInstructionTopic, actionInstruction);

    if (log.isInfoEnabled()) {
      log.info(
          "Published CANCEL fieldwork action instruction for caseId={}", caseUpdate.getCaseId());
    }
  }

  private void handleForwardableInstruction(
      CaseUpdateDTO caseUpdate, FieldActionInstruction fieldActionInstruction) {
    if (isNisraCase(caseUpdate)) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Skipping NISRA CASE_UPDATE for {} instruction, caseId={}",
            fieldActionInstruction,
            caseUpdate.getCaseId());
      }
      return;
    }

    FwmtActionInstructionDTO actionInstruction =
        actionInstructionMapper.toFwmtActionInstruction(caseUpdate, fieldActionInstruction);

    messageSender.sendMessage(fwmtActionInstructionTopic, actionInstruction);

    if (log.isInfoEnabled()) {
      log.info(
          "Published {} fieldwork action instruction for caseId={}",
          fieldActionInstruction,
          caseUpdate.getCaseId());
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

  /**
   * Returns {@code true} if the case is a NISRA case (region code "N"). NISRA cases must be
   * excluded from fieldwork for the 2027 test.
   */
  private boolean isNisraCase(CaseUpdateDTO caseUpdate) {
    return caseUpdate.getAddress() != null
        && NISRA_REGION.equals(caseUpdate.getAddress().getRegion());
  }
}
