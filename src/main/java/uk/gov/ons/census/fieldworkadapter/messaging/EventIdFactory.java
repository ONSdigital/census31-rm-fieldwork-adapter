package uk.gov.ons.census.fieldworkadapter.messaging;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventHeaderDTO;

@Component
public class EventIdFactory {
  public String createDeterministicId(
      EventHeaderDTO header, CaseUpdateDTO caseUpdate, String instructionName) {
    if (header != null && header.getMessageId() != null) {
      return header.getMessageId().toString();
    }

    String caseId =
        caseUpdate != null && caseUpdate.getCaseId() != null
            ? caseUpdate.getCaseId().toString()
            : "";
    String eventType =
        header != null && header.getMessageType() != null ? header.getMessageType().name() : "";
    String occurredAt =
        header != null && header.getDateTime() != null
            ? header.getDateTime().toString()
            : OffsetDateTime.MIN.toString();
    String seed = String.join("|", caseId, instructionName, eventType, occurredAt);
    return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
  }
}
