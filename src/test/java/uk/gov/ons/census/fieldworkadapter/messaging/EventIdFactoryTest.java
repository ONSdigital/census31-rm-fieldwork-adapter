package uk.gov.ons.census.fieldworkadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.ons.census.common.model.entity.EventType;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.EventHeaderDTO;

class EventIdFactoryTest {
  private final EventIdFactory underTest = new EventIdFactory();

  @Test
  void shouldReuseInboundMessageIdWhenPresent() {
    EventHeaderDTO header = new EventHeaderDTO();
    UUID messageId = UUID.randomUUID();
    header.setMessageId(messageId);

    String eventId = underTest.createDeterministicId(header, new CaseUpdateDTO(), "CREATE");

    assertThat(eventId).isEqualTo(messageId.toString());
  }

  @Test
  void shouldCreateStableDeterministicIdWhenMessageIdMissing() {
    EventHeaderDTO header = new EventHeaderDTO();
    header.setMessageType(EventType.CASE_UPDATE);
    header.setDateTime(OffsetDateTime.parse("2026-08-25T10:15:30Z"));

    CaseUpdateDTO caseUpdate = new CaseUpdateDTO();
    caseUpdate.setCaseId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    String first = underTest.createDeterministicId(header, caseUpdate, "UPDATE");
    String second = underTest.createDeterministicId(header, caseUpdate, "UPDATE");

    assertThat(first).isEqualTo(second);
  }
}
